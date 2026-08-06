package edn.stratodonut.trackwork.tracks.forces;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mojang.datafixers.util.Pair;
import edn.stratodonut.trackwork.TrackworkUtil;
import edn.stratodonut.trackwork.tracks.blocks.WheelBlockEntity;
import edn.stratodonut.trackwork.tracks.data.SimpleWheelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.bodies.properties.BodyKinematics;
import org.valkyrienskies.core.api.physics.RayCastResult;
import org.valkyrienskies.core.api.ships.*;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static edn.stratodonut.trackwork.TrackworkUtil.accumulatedVelocity;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toMinecraft;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY
)
public final class SimpleWheelController implements ShipPhysicsListener {
    @JsonIgnore
    public static final double RPM_TO_RADS = 0.10471975512;
    @JsonIgnore
    public static final double MAXIMUM_SLIP = 10;
    @JsonIgnore
    public static final double MAXIMUM_SLIP_LATERAL = MAXIMUM_SLIP * 1.5;
    @JsonIgnore
    public static final double MAX_FREESPIN_SLIP = 0.07;
    @JsonIgnore
    public static final double MAXIMUM_G = 98.1*5;
    public static final Vector3dc UP = new Vector3d(0, 1, 0);
    private final HashMap<Long, SimpleWheelData> trackData = new HashMap<>();
    // steering (float) and axis (direction)
    private final HashMap<Long, SimpleWheelData.ExtraWheelData> steeringData = new HashMap<>();
    @JsonIgnore
    private final ConcurrentHashMap<Long, TrackworkUtil.ClipResult> suspensionData = new ConcurrentHashMap<>();

    @JsonIgnore
    private final ConcurrentLinkedQueue<Pair<Long, SimpleWheelData.SimpleWheelCreateData>> createdTrackData = new ConcurrentLinkedQueue<>();
    @JsonIgnore
    private final ConcurrentHashMap<Long, SimpleWheelData.SimpleWheelUpdateData> trackUpdateData = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Long> removedTracks = new ConcurrentLinkedQueue<>();
    @Deprecated(forRemoval = true)
    private int nextBearingID = 0;

    private volatile Vector3dc suspensionAdjust = new Vector3d(0, 1, 0);
    private volatile float suspensionStiffness = 1.0f;

    public SimpleWheelController() {}

    public static SimpleWheelController getOrCreate(LoadedServerShip ship) {
        if (ship.getAttachment(SimpleWheelController.class) == null) {
            ship.setAttachment(SimpleWheelController.class, new SimpleWheelController());
        }

        return ship.getAttachment(SimpleWheelController.class);
    }

    private float debugTick = 0;

    @Override
    public void physTick(@NotNull PhysShip physShip, @NotNull PhysLevel physLevel) {
        while(!this.createdTrackData.isEmpty()) {
            Pair<Long, SimpleWheelData.SimpleWheelCreateData> createData = this.createdTrackData.remove();
            this.trackData.put(createData.getFirst(), SimpleWheelData.from(createData.getSecond()));
        }

        this.trackUpdateData.forEach((id, data) -> {
            SimpleWheelData old = this.trackData.get(id);
            if (old != null) {
                this.trackData.put(id, old.updateWith(data));
                this.steeringData.put(id, SimpleWheelData.ExtraWheelData.from(data));
            }
        });
        this.trackUpdateData.clear();

        // Idk why, but sometimes removing a block can send an update in the same tick(?), so this is last.
        while(!removedTracks.isEmpty()) {
            Long removeId = this.removedTracks.remove();
            this.trackData.remove(removeId);
        }

        if (this.trackData.isEmpty()) return;

        Vector3d netLinearForce = new Vector3d(0);
        Vector3d netTorque = new Vector3d(0);

        double coefficientOfPower = Math.min(2.0d, 3d / this.trackData.size());
        this.trackData.forEach((id, data) -> {
            ComputeResult computeResult = this.computeForce(data, ((PhysShipImpl) physShip), coefficientOfPower, physLevel, this.steeringData.getOrDefault(id, SimpleWheelData.ExtraWheelData.empty()));
            suspensionData.put(id, computeResult.clipResult);

            if (computeResult.linearForce.isFinite()) {
                netLinearForce.add(computeResult.linearForce);
                netTorque.add(computeResult.torque);
            }
        });

        if (netLinearForce.isFinite() && netLinearForce.length()/((PhysShipImpl) physShip).getMass() < MAXIMUM_G) {
            physShip.applyInvariantForce(netLinearForce);
            if (netTorque.isFinite()) physShip.applyInvariantTorque(netTorque);
        }
    }

    private record ComputeResult(Vector3dc linearForce, Vector3dc torque, TrackworkUtil.ClipResult clipResult) {}

    private ComputeResult computeForce(SimpleWheelData data, PhysShipImpl ship, double coefficientOfPower, PhysLevel physLevel, SimpleWheelData.ExtraWheelData steeringInfo) {
        Direction.Axis axis = steeringInfo.wheelAxis();
        float steeringValue = steeringInfo.steeringValue();
        float axialOffset = steeringInfo.axialOffset();
        float horizontalOffset = steeringInfo.horizontalOffset();
        double susScaled = steeringInfo.susScaled();
        double restOffset = steeringInfo.wheelRadius() - 0.5;
        BodyKinematics pose = ship.getKinematics();
        ShipTransform shipTransform = ship.getTransform();
        double m =  ship.getMass();
        Vector3dc localUp = shipTransform.getShipToWorldRotation().transform(UP, new Vector3d());
        double gravity_factor = Math.max(0.3, localUp.dot(UP));
        Vec3 start = toMinecraft(data.wheelOriginPosition);

        Vec3 worldSpaceNormal = toMinecraft(ship.getTransform().getShipToWorldRotation().transform(toJOML(TrackworkUtil.getActionNormal(axis)), new Vector3d()).mul(susScaled + 0.5));
        Vec3 worldSpaceStart = toMinecraft(ship.getShipToWorld().transformPosition(toJOML(start.add(0, -restOffset, 0))));

        Vec3 worldSpaceOffset = toMinecraft(
                ship.getTransform().getShipToWorldRotation().transform(
                        TrackworkUtil.getForwardVec3d(axis, 1).mul(horizontalOffset)
                                .add(TrackworkUtil.getAxisAsVec(axis).mul(axialOffset)), new Vector3d()));

        Vector3dc forceVec;
        TrackworkUtil.ClipResult clipResult = TrackworkUtil.clipAndResolvePhys(physLevel, ship,
                TrackworkUtil.getAxisAsVec(axis).rotateAxis(steeringValue * Math.toRadians(30), 0, 1, 0),
                toJOML(worldSpaceStart.add(worldSpaceOffset)), toJOML(worldSpaceNormal),
                steeringInfo.wheelRadius(), 2, ship.getId());
        forceVec = clipResult.trackTangent().mul(steeringInfo.wheelRadius() / 0.5, new Vector3d());

        double suspensionTravel = clipResult.equals(TrackworkUtil.ClipResult.MISS) ? susScaled : clipResult.suspensionLength().length() - 0.5;
        Vector3dc suspensionForce = toJOML(worldSpaceNormal.scale( (susScaled - suspensionTravel))).negate();
        boolean isOnGround = !clipResult.equals(TrackworkUtil.ClipResult.MISS);

        Vector3dc wheelContactPosition = toJOML(worldSpaceStart.add(worldSpaceOffset));
        Vector3dc wheelNormal = toJOML(worldSpaceNormal);

        Vector3dc trackRelPosShip = data.wheelOriginPosition.sub(shipTransform.getPositionInShip(), new Vector3d());
//            Vector3dc worldSpaceTrackOrigin = shipTransform.getShipToWorld().transformPosition(data.trackOriginPosition.get(new Vector3d()));
        Vector3d tForce = new Vector3d(); //data.trackSpeed;
        Vector3dc trackNormal = wheelNormal.normalize(new Vector3d());
        Vector3dc trackSurface = forceVec.mul(data.wheelRPM * RPM_TO_RADS * 0.5, new Vector3d());
        Vector3dc velocityAtPosition = accumulatedVelocity(shipTransform, pose, wheelContactPosition);
        if (isOnGround) {
            velocityAtPosition = velocityAtPosition.sub(clipResult.groundVelocity(), new Vector3d());
        }

        double suspensionCompressionDelta = 0f;
        if (data.lastSuspensionForce != null) {
            suspensionCompressionDelta = suspensionForce.sub(data.lastSuspensionForce, new Vector3d()).dot(trackNormal);
        }
        data.lastSuspensionForce = suspensionForce;

        // Suspension
        if (isOnGround) {
            double suspensionDelta = velocityAtPosition.dot(trackNormal) + suspensionCompressionDelta;
            double tilt = 1 + this.tilt(trackRelPosShip);

            // Spring force (stiffness) - apply in world coordinates but calculated relative to local up
            Vector3dc springForce = suspensionForce.mul(m * 4.0 * coefficientOfPower * this.suspensionStiffness * tilt, new Vector3d());
            tForce.add(springForce);

            // Damper force (dampening) - apply in world coordinates but calculated relative to local up
            Vector3dc damperForce = trackNormal.mul(m * 1.2 * -suspensionDelta * coefficientOfPower * this.suspensionStiffness, new Vector3d());
            tForce.add(damperForce);
            // Really half-assed antislip when the spring is stronger than friction (what?)
            if (data.wheelRPM == 0) {
                tForce = new Vector3d(0, tForce.y(), 0);
            }
        }

        if (isOnGround || trackSurface.lengthSquared() > 0) {
            // Torque
            Vector3dc surfaceVelocity = velocityAtPosition.sub(trackNormal.mul(velocityAtPosition.dot(trackNormal), new Vector3d()), new Vector3d());
            Vector3dc slipVelocity = trackSurface.sub(surfaceVelocity, new Vector3d());

            // driveForceVector can be zero!
            Vector3dc driveDir = forceVec.normalize(new Vector3d());
            Vector3dc driveSlip = driveDir.mul(driveDir.dot(slipVelocity), new Vector3d());
            Vector3dc lateralSlip = slipVelocity.sub(driveSlip, new Vector3d());

            // TODO: A better Tyre model like Pacoianowfa 98?
            if (isOnGround) {
                if (data.isFreespin) {
                    slipVelocity = driveSlip.normalize(Math.min(driveSlip.length(), MAX_FREESPIN_SLIP), new Vector3d())
                            .add(lateralSlip.normalize(Math.min(lateralSlip.length(), MAXIMUM_SLIP_LATERAL), new Vector3d()));
                } else {
                    slipVelocity = driveSlip.normalize(Math.min(driveSlip.length(), MAXIMUM_SLIP), new Vector3d())
                            .add(lateralSlip.normalize(Math.min(lateralSlip.length(), MAXIMUM_SLIP_LATERAL), new Vector3d()), new Vector3d());
                }
                tForce.add(slipVelocity.mul(1.0 * m * coefficientOfPower * gravity_factor, new Vector3d()));
            } else if (!data.isFreespin && forceVec.length() != 0) {
                slipVelocity = driveSlip.normalize(Math.min(driveSlip.length(), MAXIMUM_SLIP), new Vector3d());
                tForce.add(slipVelocity.mul(1.0 * m * coefficientOfPower * gravity_factor, new Vector3d()));
            }
        }

        Vector3dc trackRelPos = shipTransform.getShipToWorldRotation().transform(trackRelPosShip, new Vector3d());//worldSpaceTrackOrigin.sub(shipTransform.getPositionInWorld(), new Vector3d());
        Vector3dc torque = trackRelPos.cross(tForce, new Vector3d());
        return new ComputeResult(tForce, torque, clipResult);
    }

    public Vector3d getActionVec3d(Direction.Axis axis, float length, float steeringValue) {
        return TrackworkUtil.getForwardVec3d(axis, length)
                .rotateAxis(steeringValue * Math.toRadians(30), 0, 1, 0);
    }

    public final void addTrackBlock(BlockPos pos, SimpleWheelData.SimpleWheelCreateData data) {
        this.createdTrackData.add(new Pair<>(pos.asLong(), data));
    }

    public final double updateTrackBlock(BlockPos pos, SimpleWheelData.SimpleWheelUpdateData data) {
        this.trackUpdateData.put(pos.asLong(), data);
        return Math.round(this.suspensionAdjust.y()*16) / 16. * ((9+1/(this.suspensionStiffness*2 - 1))/10);
    }

    public final void removeTrackBlock(BlockPos pos) {
        this.removedTracks.add(pos.asLong());
    }

    public final float setDamperCoefficient(float delta) {
        this.suspensionStiffness = Math.clamp(1.0f, 4.0f, this.suspensionStiffness + delta);
        return this.suspensionStiffness;
    }

    public final void adjustSuspension(Vector3f delta) {
        Vector3dc old = this.suspensionAdjust;
        this.suspensionAdjust = new Vector3d(
                Math.clamp(-0.5, 0.5, old.x() + delta.x()*5),
                Math.clamp(0.1, 1, old.y() + delta.y()),
                Math.clamp(-0.5, 0.5, old.z() + delta.z()*5)
        );
    }

    public final void resetSuspension() {
        double y = this.suspensionAdjust.y();
        this.suspensionAdjust = new Vector3d(0, y,0);
    }

    private double tilt(Vector3dc relPos) {
        return Math.signum(relPos.x()) * this.suspensionAdjust.z() + Math.signum(relPos.z()) * this.suspensionAdjust.x();
    }

    public @Nonnull TrackworkUtil.ClipResult getSuspensionData(BlockPos pos) {
        return suspensionData.getOrDefault(pos.asLong(), TrackworkUtil.ClipResult.MISS);
    }

    public static <T> boolean areQueuesEqual(Queue<T> left, Queue<T> right) {
        return Arrays.equals(left.toArray(), right.toArray());
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof SimpleWheelController otherController)) {
            return false;
        } else {
            return Objects.equals(this.trackData, otherController.trackData) &&
                    Objects.equals(this.trackUpdateData, otherController.trackUpdateData) &&
                    areQueuesEqual(this.createdTrackData, otherController.createdTrackData) &&
                    areQueuesEqual(this.removedTracks, otherController.removedTracks);
        }
    }
}
