package edn.stratodonut.trackwork.tracks.forces;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mojang.datafixers.util.Pair;
import edn.stratodonut.trackwork.TrackworkUtil;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlockEntity;
import edn.stratodonut.trackwork.tracks.data.PhysTrackData;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static edn.stratodonut.trackwork.TrackworkUtil.accumulatedVelocity;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toMinecraft;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY
)
public final class PhysicsTrackController implements ShipPhysicsListener {
    @JsonIgnore
    public static final double RPM_TO_RADS = 0.10471975512;

    @JsonIgnore
    public static final double MAXIMUM_SLIP = 10;
    @JsonIgnore
    public static final double MAXIMUM_G = 98.1 * 5;

    public static final Vector3dc UP = new Vector3d(0, 1, 0);

    @Deprecated
    // For Backwards compatibility
    private final HashMap<Integer, PhysTrackData> trackData = new HashMap<>();
    private final HashMap<Long, PhysTrackData> trackData2 = new HashMap<>();
    @JsonIgnore
    private final HashMap<Long, TrackworkUtil.ClipResult> suspensionData = new HashMap<>();

    @JsonIgnore
    private final ConcurrentLinkedQueue<Pair<Long, PhysTrackData.PhysTrackCreateData>> createdTrackData = new ConcurrentLinkedQueue<>();
    @JsonIgnore
    private final ConcurrentHashMap<Long, PhysTrackData.PhysTrackUpdateData> trackUpdateData = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Long> removedTracks = new ConcurrentLinkedQueue<>();
    private int nextBearingID = 0;

    private volatile Vector3dc suspensionAdjust = new Vector3d(0, 1, 0);
    private volatile float suspensionStiffness = 1.0f;
    @JsonIgnore
    @Deprecated(forRemoval = true)
    private volatile float suspensionDampening = 1.2f;

    public PhysicsTrackController () {}

    public static PhysicsTrackController getOrCreate(LoadedServerShip ship) {
        if (ship.getAttachment(PhysicsTrackController.class) == null) {
            ship.setAttachment(PhysicsTrackController.class, new PhysicsTrackController());
        }
        return ship.getAttachment(PhysicsTrackController.class);
    }

    @JsonIgnore
    @Deprecated(forRemoval = true)
    private float debugTick = 0;

    @Override
    public void physTick(@NotNull PhysShip physShip, @NotNull PhysLevel physLevel) {
        if (!this.trackData.isEmpty()) {
            this.trackData.forEach((id, data) ->
                    this.trackData2.put(data.blockPos, data)
            );
            this.trackData.clear();
        }

        while(!this.createdTrackData.isEmpty()) {
            Pair<Long, PhysTrackData.PhysTrackCreateData> createData = this.createdTrackData.remove();
            this.trackData2.put(createData.getFirst(), PhysTrackData.from(createData.getSecond()));
        }

        this.trackUpdateData.forEach((id, data) -> {
            PhysTrackData old = this.trackData2.get(id);
            if (old != null) {
                this.trackData2.put(id, old.updateWith(data));
            }
        });
        this.trackUpdateData.clear();

        // Sometimes removing a block sends an update in the same tick
        while(!removedTracks.isEmpty()) {
            Long removeId = this.removedTracks.remove();
            if (removeId != null) this.trackData2.remove(removeId);
        }

        if (this.trackData2.isEmpty()) return;

        Vector3d netLinearForce = new Vector3d(0);
        Vector3d netTorque = new Vector3d(0);

        // Thanks java
        long[] ignoreIds = PhysEntityTrackController.getWheelIds(physShip.getId())
                .stream().mapToLong(l -> l).toArray();

        double coefficientOfPower = Math.min(2.0d, 14d / this.trackData2.size());
        this.trackData2.forEach((id, data) -> {
            ComputeResult computeResult = this.computeForce(data, (PhysShipImpl) physShip, physLevel,
                    coefficientOfPower, ignoreIds);
            suspensionData.put(id, computeResult.clipResult);
            if (computeResult.linearForce.isFinite()) {
                netLinearForce.add(computeResult.linearForce);
                netTorque.add(computeResult.torque);
            }
        });

        // Clamp total acceleration to avoid physics explosions
        if (netLinearForce.isFinite() && netLinearForce.length()/ physShip.getMass() < MAXIMUM_G) {
            physShip.applyWorldForce(netLinearForce, physShip.getKinematics().getPosition());
            if (netTorque.isFinite()) physShip.applyWorldTorque(netTorque);
        }
    }

    private record ComputeResult(Vector3dc linearForce, Vector3dc torque, TrackworkUtil.ClipResult clipResult) {}

    private ComputeResult computeForce(PhysTrackData data, PhysShipImpl ship, @NotNull PhysLevel physLevel,
                                                    double coefficientOfPower, long[] ignoreIds) {
        Vec3 start = Vec3.atCenterOf(BlockPos.of(data.blockPos));
        Direction.Axis axis = data.wheelAxis;
        double restOffset = data.wheelRadius - 0.5f;
        double suspensionRestPosition = data.effectiveSuspensionTravel;

        Vec3 worldSpaceNormal = toMinecraft(ship.getTransform().getShipToWorldRotation().transform(
                toJOML(TrackworkUtil.getActionNormal(data.wheelAxis)), new Vector3d()).mul(data.effectiveSuspensionTravel + 0.5));
        Vec3 worldSpaceStart = toMinecraft(ship.getShipToWorld().transformPosition(toJOML(start.add(0, -restOffset, 0))));
        Vector3dc worldSpaceForward = ship.getTransform().getShipToWorldRotation().transform(TrackworkUtil.getForwardVec3d(axis, 1), new Vector3d());
        Vec3 worldSpaceHorizontalOffset = toMinecraft(
                worldSpaceForward.mul(data.horizontalOffset, new Vector3d())
        );

        Vector3dc trackTangentForce;
        TrackworkUtil.ClipResult clipResult = TrackworkUtil.clipAndResolvePhys(
                physLevel, ship, TrackworkUtil.getAxisAsVec(axis),
                toJOML(worldSpaceStart.add(worldSpaceHorizontalOffset)), toJOML(worldSpaceNormal),
                data.wheelRadius, 1, ignoreIds);
                // Apply forces at the center of the block to reduce pitching moment from acceleration
        Vector3dc trackContactPosition = toJOML(worldSpaceStart);
        trackTangentForce = clipResult.trackTangent().mul(data.wheelRadius / 0.5, new Vector3d());
        if (data.inWater) {
            trackTangentForce = ship.getTransform().getShipToWorldRotation().transform(
                    TrackworkUtil.getForwardVec3d(axis, 1)).mul(data.wheelRadius / 0.5).mul(0.2);
        }

        double suspensionTravel = clipResult.equals(TrackworkUtil.ClipResult.MISS) ? suspensionRestPosition : clipResult.suspensionLength().length() - 0.5;
        Vector3dc suspensionForce = toJOML(worldSpaceNormal.scale( (suspensionRestPosition - suspensionTravel))).negate();

        BodyKinematics pose = ship.getKinematics();
        ShipTransform shipTransform = ship.getTransform();
        double m =  ship.getMass();
        Vector3dc trackRelPosShip = toJOML(start).sub(shipTransform.getPositionInShip(), new Vector3d());

        Vector3d tForce = new Vector3d();
        Vector3dc trackNormal = toJOML(worldSpaceNormal).normalize(new Vector3d());

        double suspensionCompressionDelta = 0;
        if (data.lastSuspensionForce != null) {
            suspensionCompressionDelta = suspensionForce.sub(data.lastSuspensionForce, new Vector3d()).length();
        }
        data.lastSuspensionForce = suspensionForce;
        Vector3dc trackSurface = trackTangentForce.mul(data.trackRPM * RPM_TO_RADS * 0.5, new Vector3d());
        Vector3dc velocityAtPosition = accumulatedVelocity(shipTransform, pose, trackContactPosition);

        boolean istrackGrounded = !clipResult.equals(TrackworkUtil.ClipResult.MISS);

        if (istrackGrounded) {
            velocityAtPosition = velocityAtPosition.sub(clipResult.groundVelocity(), new Vector3d());
        }

        // Suspension
        if (istrackGrounded) {
            double suspensionDelta = velocityAtPosition.dot(trackNormal) + suspensionCompressionDelta;
            double tilt = 1 + this.tilt(trackRelPosShip);

            // Spring force
            tForce.add(suspensionForce.mul(m * 1.0 * coefficientOfPower * this.suspensionStiffness * tilt, new Vector3d()));

            // Damper force
            tForce.add(trackNormal.mul(m * 0.6 * -suspensionDelta * coefficientOfPower * this.suspensionStiffness, new Vector3d()));

            // Really half-assed antislip when the spring is stronger than friction (what?)
            if (data.trackRPM == 0) {
                tForce = new Vector3d(0, tForce.y(), 0);
            }
        }

        // Slip/friction
        if (istrackGrounded || trackSurface.lengthSquared() > 0) {
            Vector3dc surfaceVelocity = velocityAtPosition.sub(trackNormal.mul(velocityAtPosition.dot(trackNormal), new Vector3d()), new Vector3d());
            Vector3dc slipVelocity = trackSurface.sub(surfaceVelocity, new Vector3d());
            // TODO: Do I want to use real friction here?
            if (!istrackGrounded) {
                slipVelocity = surfaceVelocity.normalize(new Vector3d())
                        .mul(slipVelocity.dot(surfaceVelocity.normalize(new Vector3d())), new Vector3d());
            }
            tForce = tForce.add(slipVelocity.normalize(Math.min(slipVelocity.length(), MAXIMUM_SLIP), new Vector3d())
                    .mul(1.0 * m * coefficientOfPower), new Vector3d());
        }

        Vector3dc trackRelPos = shipTransform.getShipToWorldRotation().transform(trackRelPosShip, new Vector3d());
        Vector3dc torque = trackRelPos.cross(tForce, new Vector3d());
        return new ComputeResult(tForce, torque, clipResult);
    }

    public void addTrackBlock(PhysTrackData.PhysTrackCreateData data) {
        this.createdTrackData.add(new Pair<>(data.blockPos().asLong(), data));
    }

    public double updateTrackBlock(BlockPos pos, PhysTrackData.PhysTrackUpdateData data) {
        this.trackUpdateData.put(pos.asLong(), data);
        return Math.round(this.suspensionAdjust.y()*16) / 16. * ((9+1/(this.suspensionStiffness*2 - 1))/10);
    }

    public void removeTrackBlock(long id) {
        this.removedTracks.add(id);
    }

    public float setSuspensionDampening(float delta) {
        this.suspensionStiffness = Math.clamp(1f, 4.0f, this.suspensionStiffness + delta);
        return this.suspensionStiffness;
    }

    @Deprecated
    public float setDamperCoefficient(float delta) {
        return setSuspensionDampening(delta);
    }

    public void adjustSuspension(Vector3f delta) {
        Vector3dc old = this.suspensionAdjust;
        this.suspensionAdjust = new Vector3d(
                Math.clamp(-0.5, 0.5, old.x() + delta.x()*5),
                Math.clamp(0.1, 1, old.y() + delta.y()),
                Math.clamp(-0.5, 0.5, old.z() + delta.z()*5)
        );
    }

    public void resetSuspension() {
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
        } else if (!(other instanceof PhysicsTrackController otherController)) {
            return false;
        } else {
            return Objects.equals(this.trackData2, otherController.trackData2) &&
                    Objects.equals(this.trackUpdateData, otherController.trackUpdateData) &&
                    areQueuesEqual(this.createdTrackData, otherController.createdTrackData) &&
                    areQueuesEqual(this.removedTracks, otherController.removedTracks) &&
                    this.nextBearingID == otherController.nextBearingID &&
                    Float.compare(this.suspensionStiffness, otherController.suspensionStiffness) == 0;
        }
    }
}
