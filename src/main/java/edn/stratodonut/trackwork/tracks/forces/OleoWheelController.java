package edn.stratodonut.trackwork.tracks.forces;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edn.stratodonut.trackwork.TrackworkUtil;
import edn.stratodonut.trackwork.tracks.data.OleoWheelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.bodies.properties.BodyKinematics;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ShipPhysicsListener;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.api.world.PhysLevel;

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
public final class OleoWheelController implements ShipPhysicsListener {
    @JsonIgnore
    public static double RPM_TO_RADS = 0.10471975512;
    @JsonIgnore
    public static double MAXIMUM_SLIP = 10;
    @JsonIgnore
    public static double MAXIMUM_SLIP_LATERAL = MAXIMUM_SLIP * 1.5;
    @JsonIgnore
    public static double MAX_FREESPIN_SLIP = 0.07;
    @JsonIgnore
    public static double MAXIMUM_G = 98.1*5;
    public static Vector3dc UP = new Vector3d(0, 1, 0);
    private HashMap<Long, OleoWheelData> wheelData = new HashMap<>();
    @JsonIgnore
    private final ConcurrentHashMap<Long, TrackworkUtil.ClipResult> suspensionData = new ConcurrentHashMap<>();

    @JsonIgnore
    private ConcurrentHashMap<Long, OleoWheelData> wheelUpdateData = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<Long> removedWheels = new ConcurrentLinkedQueue<>();

    private volatile Vector3dc suspensionAdjust = new Vector3d(0, 1, 0);
    private volatile float suspensionStiffness = 1.0f;
    private volatile float suspensionDampening = 1.2f;

    public OleoWheelController() {}

    public static OleoWheelController getOrCreate(LoadedServerShip ship) {
        return ship.getOrPutAttachment(OleoWheelController.class, OleoWheelController::new);
    }

    @Override
    public void physTick(@NotNull PhysShip physShip, @NotNull PhysLevel physLevel) {
        this.wheelUpdateData.forEach((id, data) -> {
            this.wheelData.merge(id, data, OleoWheelData::updateWith);
        });
        this.wheelUpdateData.clear();

        // Idk why, but sometimes removing a block can send an update in the same tick(?), so this is last.
        while(!removedWheels.isEmpty()) {
            Long removeId = this.removedWheels.remove();
            this.wheelData.remove(removeId);
        }

        if (this.wheelData.isEmpty()) return;

        double coefficientOfPower = Math.min(2.0d, 3d / this.wheelData.size());

        this.wheelData.forEach((id, data) -> {
            ComputeResult result = computeForce(data, physShip, physLevel, coefficientOfPower);
            this.suspensionData.put(id, result.clipResult);
            if (result.linearForce.isFinite()) physShip.applyWorldForce(result.linearForce, physShip.getTransform().getPositionInWorld());
            if (result.torque.isFinite()) physShip.applyWorldTorque(result.torque);
        });
    }

    private record ComputeResult(Vector3dc linearForce, Vector3dc torque, TrackworkUtil.ClipResult clipResult) {}

    private ComputeResult computeForce(OleoWheelData data, PhysShip ship, PhysLevel physLevel, double coefficientOfPower) {
        if (data.susScaled == 0) {
            data.lastSuspensionForce = null;
            return new ComputeResult(new Vector3d(), new Vector3d(), TrackworkUtil.ClipResult.MISS);
        }

        Direction.Axis axis = data.wheelAxis;
        float steeringValue = data.steeringValue;
        float axialOffset = data.axialOffset;
        float horizontalOffset = data.horizontalOffset;
        double susScaled = data.susScaled;
        double restOffset = data.wheelRadius - 0.5;
        BodyKinematics pose = ship.getKinematics();
        ShipTransform shipTransform = ship.getTransform();
        double m =  ship.getMass();
        Vector3dc localUp = shipTransform.getShipToWorldRotation().transform(UP, new Vector3d());
        double gravity_factor = org.joml.Math.max(0.3, localUp.dot(UP));
        Vec3 start = BlockPos.of(data.blockPos).getCenter();

        Vec3 worldSpaceNormal = toMinecraft(ship.getTransform().getShipToWorldRotation().transform(toJOML(TrackworkUtil.getActionNormal(axis)), new Vector3d()).mul(susScaled + 0.5));
        Vec3 worldSpaceStart = toMinecraft(ship.getShipToWorld().transformPosition(toJOML(start.add(0, -restOffset, 0))));

        Vec3 worldSpaceOffset = toMinecraft(
                ship.getTransform().getShipToWorldRotation().transform(
                        TrackworkUtil.getForwardVec3d(axis, 1).mul(horizontalOffset)
                                .add(TrackworkUtil.getAxisAsVec(axis).mul(axialOffset)), new Vector3d()));

        Vector3dc forceVec;
        TrackworkUtil.ClipResult clipResult = TrackworkUtil.clipAndResolvePhys(physLevel, ship,
                TrackworkUtil.getAxisAsVec(axis).rotateAxis(steeringValue * org.joml.Math.toRadians(30), 0, 1, 0),
                toJOML(worldSpaceStart.add(worldSpaceOffset)), toJOML(worldSpaceNormal),
                data.wheelRadius, 2, ship.getId());
        forceVec = clipResult.trackTangent().mul(data.wheelRadius / 0.5, new Vector3d());

        double suspensionTravel = clipResult.equals(TrackworkUtil.ClipResult.MISS) ? susScaled : clipResult.suspensionLength().length() - 0.5;
        Vector3dc suspensionForce = toJOML(worldSpaceNormal.scale( (susScaled - suspensionTravel))).negate();
        boolean isOnGround = !clipResult.equals(TrackworkUtil.ClipResult.MISS);

        Vector3dc wheelContactPosition = toJOML(worldSpaceStart.add(worldSpaceOffset));
        Vector3dc wheelNormal = toJOML(worldSpaceNormal);

        Vector3dc trackRelPosShip = toJOML(start).sub(shipTransform.getPositionInShip(), new Vector3d());
        Vector3d tForce = new Vector3d(); //data.trackSpeed;
        Vector3dc trackNormal = wheelNormal.normalize(new Vector3d());
        Vector3dc trackSurface = forceVec.mul(data.wheelRPM * RPM_TO_RADS * 0.5, new Vector3d());
        Vector3dc velocityAtPosition = accumulatedVelocity(shipTransform, pose, wheelContactPosition);
        if (isOnGround) {
            velocityAtPosition = velocityAtPosition.sub(clipResult.groundVelocity(), new Vector3d());
        }

        double suspensionCompressionDelta = 0f;
        if (data.lastSuspensionForce != null) {
            suspensionCompressionDelta = suspensionForce.sub(data.lastSuspensionForce, new Vector3d()).length();
        }
        data.lastSuspensionForce = suspensionForce;

        // Suspension
        if (isOnGround) {
            double suspensionDelta = velocityAtPosition.dot(trackNormal) + suspensionCompressionDelta;
            double tilt = 1 + this.tilt(trackRelPosShip);

            // Spring force (stiffness) - apply in world coordinates but calculated relative to local up
            Vector3dc springForce = suspensionForce.mul(m * 4.0 * coefficientOfPower * this.suspensionStiffness * tilt, new Vector3d());
            tForce.add(springForce);

            // Less damper downward
            double damperMagnitude = m * -suspensionDelta * coefficientOfPower * this.suspensionDampening;
            if (damperMagnitude > 0) {
                damperMagnitude *= 0.5;
            }

            // Damper force (dampening) - apply in world coordinates but calculated relative to local up
            Vector3dc damperForce = trackNormal.mul(damperMagnitude, new Vector3d());
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
                    slipVelocity = driveSlip.normalize(org.joml.Math.min(driveSlip.length(), MAX_FREESPIN_SLIP), new Vector3d())
                            .add(lateralSlip.normalize(org.joml.Math.min(lateralSlip.length(), MAXIMUM_SLIP_LATERAL), new Vector3d()));
                } else {
                    slipVelocity = driveSlip.normalize(org.joml.Math.min(driveSlip.length(), MAXIMUM_SLIP), new Vector3d())
                            .add(lateralSlip.normalize(org.joml.Math.min(lateralSlip.length(), MAXIMUM_SLIP_LATERAL), new Vector3d()), new Vector3d());
                }
                tForce.add(slipVelocity.mul(1.0 * m * coefficientOfPower * gravity_factor, new Vector3d()));
            } else if (!data.isFreespin && forceVec.length() != 0) {
                slipVelocity = driveSlip.normalize(org.joml.Math.min(driveSlip.length(), MAXIMUM_SLIP), new Vector3d());
                tForce.add(slipVelocity.mul(1.0 * m * coefficientOfPower * gravity_factor, new Vector3d()));
            }
        }

        Vector3dc trackRelPos = shipTransform.getShipToWorldRotation().transform(trackRelPosShip, new Vector3d());//worldSpaceTrackOrigin.sub(shipTransform.getPositionInWorld(), new Vector3d());
        Vector3dc torque = trackRelPos.cross(tForce, new Vector3d());
        return new ComputeResult(tForce, torque, clipResult);
    }

    public double updateTrackBlock(BlockPos pos, OleoWheelData data) {
        this.wheelUpdateData.put(pos.asLong(), data);
        return org.joml.Math.round(this.suspensionAdjust.y()*16) / 16. * ((9+1/(this.suspensionStiffness*2 - 1))/10);
    }

    public void removeTrackBlock(BlockPos pos) {
        this.removedWheels.add(pos.asLong());
    }

    public float setDamperCoefficient(float delta) {
        this.suspensionStiffness = org.joml.Math.clamp(1.0f, 4.0f, this.suspensionStiffness + delta);
        return this.suspensionStiffness;
    }

    public void adjustSuspension(Vector3f delta) {
        Vector3dc old = this.suspensionAdjust;
        this.suspensionAdjust = new Vector3d(
                org.joml.Math.clamp(-0.5, 0.5, old.x() + delta.x()*5),
                org.joml.Math.clamp(0.1, 1, old.y() + delta.y()),
                org.joml.Math.clamp(-0.5, 0.5, old.z() + delta.z()*5)
        );
    }

    public void resetSuspension() {
        double y = this.suspensionAdjust.y();
        this.suspensionAdjust = new Vector3d(0, y,0);
    }

    private double tilt(Vector3dc relPos) {
        return org.joml.Math.signum(relPos.x()) * this.suspensionAdjust.z() + org.joml.Math.signum(relPos.z()) * this.suspensionAdjust.x();
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
        } else if (!(other instanceof OleoWheelController otherController)) {
            return false;
        } else {
            return Objects.equals(this.wheelData, otherController.wheelData) &&
                    Objects.equals(this.wheelUpdateData, otherController.wheelUpdateData) &&
                    areQueuesEqual(this.removedWheels, otherController.removedWheels);
        }
    }
}
