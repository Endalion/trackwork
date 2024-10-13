package edn.stratodonut.trackwork.tracks.forces;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mojang.datafixers.util.Pair;
import org.joml.Vector3f;
import edn.stratodonut.trackwork.tracks.data.SimpleWheelData;
import kotlin.jvm.functions.Function1;
import net.minecraft.core.BlockPos;
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
public class SimpleWheelController implements ShipForcesInducer {
    @JsonIgnore
    public static final double RPM_TO_RADS = 0.10471975512;
    @JsonIgnore
    public static final double MAXIMUM_SLIP = 10;
    @JsonIgnore
    public static final double MAXIMUM_SLIP_LATERAL = MAXIMUM_SLIP * 1.5;
    @JsonIgnore
    public static final double MAXIMUM_G = 98.1*5;
    public static final Vector3dc UP = new Vector3d(0, 1, 0);
    private final HashMap<Long, SimpleWheelData> trackData = new HashMap<>();

    @JsonIgnore
    private final ConcurrentLinkedQueue<Pair<Long, SimpleWheelData.SimpleWheelCreateData>> createdTrackData = new ConcurrentLinkedQueue<>();
    @JsonIgnore
    private final ConcurrentHashMap<Long, SimpleWheelData.SimpleWheelUpdateData> trackUpdateData = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Long> removedTracks = new ConcurrentLinkedQueue<>();
    private int nextBearingID = 0;

    private volatile Vector3dc suspensionAdjust = new Vector3d(0, 1, 0);
    private volatile float suspensionStiffness = 1.0f;

    public SimpleWheelController() {}

    public static SimpleWheelController getOrCreate(ServerShip ship) {
        if (ship.getAttachment(SimpleWheelController.class) == null) {
            ship.saveAttachment(SimpleWheelController.class, new SimpleWheelController());
        }

        return ship.getAttachment(SimpleWheelController.class);
    }

    private float debugTick = 0;

    @Override
    public void applyForcesAndLookupPhysShips(@NotNull PhysShip physShip, @NotNull Function1<? super Long, ? extends PhysShip> lookupPhysShip) {
        while(!this.createdTrackData.isEmpty()) {
            Pair<Long, SimpleWheelData.SimpleWheelCreateData> createData = this.createdTrackData.remove();
            this.trackData.put(createData.getFirst(), SimpleWheelData.from(createData.getSecond()));
        }

        this.trackUpdateData.forEach((id, data) -> {
            SimpleWheelData old = this.trackData.get(id);
            if (old != null) {
                this.trackData.put(id, old.updateWith(data));
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
            Pair<Vector3dc, Vector3dc> forces = this.computeForce(data, ((PhysShipImpl) physShip), coefficientOfPower, lookupPhysShip);
            if (forces.getFirst().isFinite()) {
                netLinearForce.add(forces.getFirst());
                netTorque.add(forces.getSecond());
            }
        });

        if (netLinearForce.isFinite() && netLinearForce.length()/((PhysShipImpl) physShip).getInertia().getShipMass() < MAXIMUM_G) {
            physShip.applyInvariantForce(netLinearForce);
            if (netTorque.isFinite()) physShip.applyInvariantTorque(netTorque);
        }
    }

    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        // DO NOTHING
    }

    private Pair<Vector3dc, Vector3dc> computeForce(SimpleWheelData data, PhysShipImpl ship, double coefficientOfPower, @NotNull Function1<? super Long, ? extends PhysShip> lookupPhysShip) {
        PoseVel pose = ship.getPoseVel();
        ShipTransform shipTransform = ship.getTransform();
        double m =  ship.getInertia().getShipMass();
        double gravity_factor = Math.max(0, shipTransform.getShipToWorldRotation().transform(UP, new Vector3d()).dot(UP));
        Vector3dc trackRelPosShip = data.wheelOriginPosition.sub(shipTransform.getPositionInShip(), new Vector3d());
//            Vector3dc worldSpaceTrackOrigin = shipTransform.getShipToWorld().transformPosition(data.trackOriginPosition.get(new Vector3d()));
        Vector3d tForce = new Vector3d(); //data.trackSpeed;
        Vector3dc trackNormal = data.wheelNormal.normalize(new Vector3d());
        Vector3dc trackSurface = data.driveForceVector.mul(data.wheelRPM * RPM_TO_RADS * 0.5, new Vector3d());
        Vector3dc velocityAtPosition = accumulatedVelocity(shipTransform, pose, data.wheelContactPosition);
        if (data.isWheelGrounded && data.groundShipId != null) {
            PhysShipImpl ground = (PhysShipImpl) lookupPhysShip.invoke(data.groundShipId);
            Vector3dc groundShipVelocity = accumulatedVelocity(ground.getTransform(), ground.getPoseVel(), data.wheelContactPosition);
            velocityAtPosition = velocityAtPosition.sub(groundShipVelocity, new Vector3d());
        }

        // Suspension
        if (data.isWheelGrounded) {
            double suspensionDelta = velocityAtPosition.dot(trackNormal) + data.getSuspensionCompressionDelta().length();
            double tilt = 1 + this.tilt(trackRelPosShip);
            tForce.add(data.suspensionCompression.mul(m * 4.0 * coefficientOfPower * this.suspensionStiffness * tilt, new Vector3d()));
            tForce.add(trackNormal.mul(m * 1.2 * -suspensionDelta * coefficientOfPower * this.suspensionStiffness, new Vector3d()));
            // Really half-assed antislip when the spring is stronger than friction (what?)
            if (data.wheelRPM == 0) {
                tForce = new Vector3d(0, tForce.y(), 0);
            }
        }

        if (data.isWheelGrounded || trackSurface.lengthSquared() > 0) {
            // Torque
            Vector3dc surfaceVelocity = velocityAtPosition.sub(trackNormal.mul(velocityAtPosition.dot(trackNormal), new Vector3d()), new Vector3d());
            Vector3dc slipVelocity = trackSurface.sub(surfaceVelocity, new Vector3d());

            // driveForceVector can be zero!
            Vector3dc driveDir = data.driveForceVector.normalize(new Vector3d());
            Vector3dc driveSlip = driveDir.mul(driveDir.dot(slipVelocity), new Vector3d());
            Vector3dc lateralSlip = slipVelocity.sub(driveSlip, new Vector3d());

            // TODO: A better Tyre model like Pacoianowfa 98?
            if (data.isWheelGrounded) {
                if (data.isFreespin) {
                    slipVelocity = lateralSlip.normalize(Math.min(lateralSlip.length(), MAXIMUM_SLIP_LATERAL), new Vector3d());
                } else {
                    slipVelocity = driveSlip.normalize(Math.min(driveSlip.length(), MAXIMUM_SLIP), new Vector3d())
                            .add(lateralSlip.normalize(Math.min(lateralSlip.length(), MAXIMUM_SLIP_LATERAL), new Vector3d()), new Vector3d());
                }
                tForce.add(slipVelocity.mul(1.0 * m * coefficientOfPower * gravity_factor, new Vector3d()));
            } else if (!data.isFreespin && data.driveForceVector.length() != 0) {
                slipVelocity = driveSlip.normalize(Math.min(driveSlip.length(), MAXIMUM_SLIP), new Vector3d());
                tForce.add(slipVelocity.mul(1.0 * m * coefficientOfPower * gravity_factor, new Vector3d()));
            }
        }

        Vector3dc trackRelPos = shipTransform.getShipToWorldRotation().transform(trackRelPosShip, new Vector3d());//worldSpaceTrackOrigin.sub(shipTransform.getPositionInWorld(), new Vector3d());
        Vector3dc torque = trackRelPos.cross(tForce, new Vector3d());
        return new Pair<>(tForce, torque);
    }

    public static Vector3dc accumulatedVelocity(ShipTransform t, PoseVel pose, Vector3dc worldPosition) {
        return pose.getVel().add(pose.getOmega().cross(worldPosition.sub(t.getPositionInWorld(), new Vector3d()), new Vector3d()), new Vector3d());
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

    public static <T> boolean areQueuesEqual(Queue<T> left, Queue<T> right) {
        return Arrays.equals(left.toArray(), right.toArray());
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof SimpleWheelController otherController)) {
            return false;
        } else {
            return Objects.equals(this.trackData, otherController.trackData) && Objects.equals(this.trackUpdateData, otherController.trackUpdateData) && areQueuesEqual(this.createdTrackData, otherController.createdTrackData) && areQueuesEqual(this.removedTracks, otherController.removedTracks) && this.nextBearingID == otherController.nextBearingID;
        }
    }
}
