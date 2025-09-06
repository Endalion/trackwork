package edn.stratodonut.trackwork.tracks.forces;

import org.joml.Vector3f;
import edn.stratodonut.trackwork.tracks.data.PhysTrackData;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mojang.datafixers.util.Pair;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.physics_api.PoseVel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY
)
public class PhysicsTrackController implements ShipForcesInducer {
    @JsonIgnore
    public static final double RPM_TO_RADS = 0.10471975512;

    // Friction/slip model
    @JsonIgnore
    public static final double MAXIMUM_SLIP = 10;
    @JsonIgnore
    public static final double MAXIMUM_SLIP_LATERAL = MAXIMUM_SLIP * 0.75;
    @JsonIgnore
    public static final double LATERAL_FRICTION_MULTIPLIER = 0.6;
    @JsonIgnore
    public static final double LONGITUDINAL_FRICTION_MULTIPLIER = 0.4; // NEW: lower than 1.0 for less forward/back traction
    @JsonIgnore
    public static final double MAXIMUM_G = 98.1 * 5;

    public static final Vector3dc UP = new Vector3d(0, 1, 0);

    private final HashMap<Integer, PhysTrackData> trackData = new HashMap<>();

    @JsonIgnore
    private final ConcurrentLinkedQueue<Pair<Integer, PhysTrackData.PhysTrackCreateData>> createdTrackData = new ConcurrentLinkedQueue<>();
    @JsonIgnore
    private final ConcurrentHashMap<Integer, PhysTrackData.PhysTrackUpdateData> trackUpdateData = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Integer> removedTracks = new ConcurrentLinkedQueue<>();
    private int nextBearingID = 0;

    private volatile Vector3dc suspensionAdjust = new Vector3d(0, 1, 0);
    private volatile float suspensionStiffness = 1.0f;
    private volatile float suspensionDampening = 1.2f;

    public PhysicsTrackController () {}

    public static PhysicsTrackController getOrCreate(ServerShip ship) {
        if (ship.getAttachment(PhysicsTrackController.class) == null) {
            ship.saveAttachment(PhysicsTrackController.class, new PhysicsTrackController());
        }
        return ship.getAttachment(PhysicsTrackController.class);
    }

    private float debugTick = 0;

    @Override
    public void applyForcesAndLookupPhysShips(@NotNull PhysShip physShip, @NotNull Function1<? super Long, ? extends PhysShip> lookupPhysShip) {
        while(!this.createdTrackData.isEmpty()) {
            Pair<Integer, PhysTrackData.PhysTrackCreateData> createData = this.createdTrackData.remove();
            this.trackData.put(createData.getFirst(), PhysTrackData.from(createData.getSecond()));
        }

        this.trackUpdateData.forEach((id, data) -> {
            PhysTrackData old = this.trackData.get(id);
            if (old != null) {
                this.trackData.put(id, old.updateWith(data));
            }
        });
        this.trackUpdateData.clear();

        // Sometimes removing a block sends an update in the same tick
        while(!removedTracks.isEmpty()) {
            Integer removeId = this.removedTracks.remove();
            this.trackData.remove(removeId);
        }

        if (this.trackData.isEmpty()) return;

        Vector3d netLinearForce = new Vector3d(0);
        Vector3d netTorque = new Vector3d(0);

        double coefficientOfPower = Math.min(2.0d, 14d / this.trackData.size());
        this.trackData.forEach((id, data) -> {
            Pair<Vector3dc, Vector3dc> forces = this.computeForce(data, ((PhysShipImpl) physShip), coefficientOfPower, lookupPhysShip);
            if (forces.getFirst().isFinite()) {
                netLinearForce.add(forces.getFirst());
                netTorque.add(forces.getSecond());
            }
        });

        // Clamp total acceleration to avoid physics explosions
        if (netLinearForce.isFinite() && netLinearForce.length()/((PhysShipImpl) physShip).getInertia().getShipMass() < MAXIMUM_G) {
            physShip.applyInvariantForce(netLinearForce);
            if (netTorque.isFinite()) physShip.applyInvariantTorque(netTorque);
        }
    }

    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        // DO NOTHING
    }

    private Pair<Vector3dc, Vector3dc> computeForce(PhysTrackData data, PhysShipImpl ship, double coefficientOfPower, @NotNull Function1<? super Long, ? extends PhysShip> lookupPhysShip) {
        PoseVel pose = ship.getPoseVel();
        ShipTransform shipTransform = ship.getTransform();
        double m =  ship.getInertia().getShipMass();
        Vector3dc trackRelPosShip = data.trackOriginPosition.sub(shipTransform.getPositionInShip(), new Vector3d());

        Vector3d tForce = new Vector3d();
        Vector3dc trackNormal = data.trackNormal.normalize(new Vector3d());
        Vector3dc trackSurface = data.trackSpeed.mul(data.trackRPM * RPM_TO_RADS * 0.5, new Vector3d());
        Vector3dc velocityAtPosition = accumulatedVelocity(shipTransform, pose, data.trackContactPosition);

        if (data.istrackGrounded && data.groundShipId != null) {
            PhysShipImpl ground = (PhysShipImpl) lookupPhysShip.invoke(data.groundShipId);
            Vector3dc groundShipVelocity = accumulatedVelocity(ground.getTransform(), ground.getPoseVel(), data.trackContactPosition);
            velocityAtPosition = velocityAtPosition.sub(groundShipVelocity, new Vector3d());
        }

        // Suspension
        if (data.istrackGrounded) {
            double suspensionDelta = velocityAtPosition.dot(trackNormal) + data.getSuspensionCompressionDelta().length();
            double tilt = 1 + this.tilt(trackRelPosShip);

            // Spring force
            tForce.add(data.suspensionCompression.mul(m * 4.0 * coefficientOfPower * this.suspensionStiffness * tilt, new Vector3d()));

            // Damper force
            tForce.add(trackNormal.mul(m * -suspensionDelta * coefficientOfPower * this.suspensionDampening, new Vector3d()));

            if (data.trackRPM == 0) {
                tForce = new Vector3d(0, tForce.y(), 0);
            }
        }

        // Slip/friction
        if (data.istrackGrounded || trackSurface.lengthSquared() > 0) {
            Vector3dc surfaceVelocity = velocityAtPosition.sub(trackNormal.mul(velocityAtPosition.dot(trackNormal), new Vector3d()), new Vector3d());
            Vector3dc slipVelocity = trackSurface.sub(surfaceVelocity, new Vector3d());

            Vector3dc driveDir = data.trackSpeed.normalize(new Vector3d());
            Vector3dc driveSlip = driveDir.mul(driveDir.dot(slipVelocity), new Vector3d());
            Vector3dc lateralSlip = slipVelocity.sub(driveSlip, new Vector3d());

            if (data.istrackGrounded) {
                // Apply multipliers separately
                Vector3dc driveForce = driveSlip.normalize(Math.min(driveSlip.length(), MAXIMUM_SLIP), new Vector3d())
                        .mul(LONGITUDINAL_FRICTION_MULTIPLIER, new Vector3d()); // less traction forward/back
                Vector3dc lateralForce = lateralSlip.normalize(Math.min(lateralSlip.length(), MAXIMUM_SLIP_LATERAL), new Vector3d())
                        .mul(LATERAL_FRICTION_MULTIPLIER, new Vector3d());

                Vector3dc combinedSlip = driveForce.add(lateralForce, new Vector3d());
                tForce.add(combinedSlip.mul(1.0 * m * coefficientOfPower, new Vector3d()));
            } else {
                if (data.trackSpeed.lengthSquared() > 0) {
                    slipVelocity = driveSlip.normalize(Math.min(driveSlip.length(), MAXIMUM_SLIP), new Vector3d())
                            .mul(LONGITUDINAL_FRICTION_MULTIPLIER, new Vector3d());
                    tForce.add(slipVelocity.mul(1.0 * m * coefficientOfPower, new Vector3d()));
                }
            }
        }

        Vector3dc trackRelPos = shipTransform.getShipToWorldRotation().transform(trackRelPosShip, new Vector3d());
        Vector3dc torque = trackRelPos.cross(tForce, new Vector3d());
        return new Pair<>(tForce, torque);
    }

    private static Vector3dc accumulatedVelocity(ShipTransform t, PoseVel pose, Vector3dc worldPosition) {
        return pose.getVel().add(pose.getOmega().cross(worldPosition.sub(t.getPositionInWorld(), new Vector3d()), new Vector3d()), new Vector3d());
    }

    public final int addTrackBlock(PhysTrackData.PhysTrackCreateData data) {
        this.createdTrackData.add(new Pair<>(++nextBearingID, data));
        return this.nextBearingID;
    }

    public final double updateTrackBlock(Integer id, PhysTrackData.PhysTrackUpdateData data) {
        this.trackUpdateData.put(id, data);
        return Math.round(this.suspensionAdjust.y()*16) / 16. * ((9+1/(this.suspensionStiffness*2 - 1))/10);
    }

    public final void removeTrackBlock(int id) {
        this.removedTracks.add(id);
    }

    public final float setSuspensionStiffness(float delta) {
        this.suspensionStiffness = Math.clamp(1.0f, 10.0f, this.suspensionStiffness + delta);
        return this.suspensionStiffness;
    }

    public final float setSuspensionDampening(float delta) {
        this.suspensionDampening = Math.clamp(5f, 10.0f, this.suspensionDampening + delta);
        return this.suspensionDampening;
    }

    @Deprecated
    public final float setDamperCoefficient(float delta) {
        return setSuspensionDampening(delta);
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

    public static <T> boolean areQueuesEqual(Queue<T> left, Queue<T> right) {
        return Arrays.equals(left.toArray(), right.toArray());
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof PhysicsTrackController otherController)) {
            return false;
        } else {
            return Objects.equals(this.trackData, otherController.trackData) &&
                    Objects.equals(this.trackUpdateData, otherController.trackUpdateData) &&
                    areQueuesEqual(this.createdTrackData, otherController.createdTrackData) &&
                    areQueuesEqual(this.removedTracks, otherController.removedTracks) &&
                    this.nextBearingID == otherController.nextBearingID &&
                    Float.compare(this.suspensionStiffness, otherController.suspensionStiffness) == 0 &&
                    Float.compare(this.suspensionDampening, otherController.suspensionDampening) == 0;
        }
    }
}
