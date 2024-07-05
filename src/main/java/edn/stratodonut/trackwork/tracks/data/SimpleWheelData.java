package edn.stratodonut.trackwork.tracks.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY
)
public class SimpleWheelData {
    public final Vector3dc wheelOriginPosition;
    public final Vector3dc wheelContactPosition;
    public final Vector3dc driveForceVector;
    public final Vector3dc wheelNormal;
    public final Vector3dc suspensionCompression;
    public final boolean isFreespin;
    @Nullable
    public Long groundShipId;
    private Vector3dc suspensionCompressionDelta;
    public final boolean isWheelGrounded;
    public float wheelRPM;

    public float trackSU;

    // For Jackson serialisation
    private SimpleWheelData() {
        this(null);
    }

    private SimpleWheelData(Vector3dc wheelOriginPosition) {
        this.wheelOriginPosition = wheelOriginPosition;
        this.wheelContactPosition = new Vector3d(0);
        this.driveForceVector = new Vector3d(0);
        this.wheelNormal = new Vector3d(0, -1, 0);
        this.suspensionCompression = new Vector3d(0);
        this.suspensionCompressionDelta = new Vector3d(0);
        this.isFreespin = true;
        this.isWheelGrounded = false;
        this.wheelRPM = 0;
    }

    public SimpleWheelData(Vector3dc wheelOriginPosition, Vector3dc wheelContactPosition, Vector3dc driveForceVector,
                           Vector3dc wheelNormal, Vector3dc suspensionCompression, Vector3dc suspensionCompressionDelta,
                           boolean isFreespin, @Nullable Long groundShipId, boolean isWheelGrounded, float wheelRPM) {
        this.wheelOriginPosition = wheelOriginPosition;
        this.wheelContactPosition = wheelContactPosition;
        this.driveForceVector = driveForceVector;
        this.wheelNormal = wheelNormal;
        this.suspensionCompression = suspensionCompression;
        this.suspensionCompressionDelta = suspensionCompressionDelta;
        this.isFreespin = isFreespin;
        this.groundShipId = groundShipId;
        this.isWheelGrounded = isWheelGrounded;
        this.wheelRPM = wheelRPM;
    }

    public final SimpleWheelData updateWith(SimpleWheelUpdateData update) {
        return new SimpleWheelData(
                this.wheelOriginPosition,
                update.trackContactPosition,
                update.trackSpeed,
                update.trackNormal,
                update.suspensionCompression,
                update.suspensionCompression.sub(this.suspensionCompression, new Vector3d()).div(20, new Vector3d()),
                update.isFreespin,
                update.groundShipId,
                update.trackHit,
                update.trackRPM
        );
    }

    public static SimpleWheelData from(SimpleWheelCreateData data) {
        return new SimpleWheelData(data.trackOriginPosition);
    }

    @Nonnull
    public Vector3dc getSuspensionCompressionDelta() {
        return suspensionCompressionDelta;
    }

    public void setSuspensionCompressionDelta(Vector3dc suspensionCompressionDelta) {
        if (suspensionCompressionDelta == null) throw new NullPointerException();
        this.suspensionCompressionDelta = suspensionCompressionDelta;
    }

    public record SimpleWheelUpdateData(Vector3dc trackContactPosition, Vector3dc trackSpeed, Vector3dc trackNormal,
                                        Vector3dc suspensionCompression, boolean isFreespin, Long groundShipId, boolean trackHit, float trackRPM) {
    }

    public record SimpleWheelCreateData(Vector3dc trackOriginPosition) {}
}
