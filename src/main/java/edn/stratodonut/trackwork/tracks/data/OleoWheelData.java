package edn.stratodonut.trackwork.tracks.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.util.PhysTickOnly;

import javax.annotation.Nullable;

public class OleoWheelData {
    public final long blockPos;
    public float steeringValue;
    public double susScaled;
    public Direction.Axis wheelAxis;
    public float axialOffset;
    public float horizontalOffset;
    public double wheelRadius;
    public float wheelRPM;
    public boolean isFreespin;

    @PhysTickOnly
    @Nullable
    public Vector3dc lastSuspensionForce;

    public OleoWheelData() {
        this(BlockPos.ZERO);
    }

    public OleoWheelData(BlockPos pos) {
        this.blockPos = pos.asLong();
        this.wheelAxis = Direction.Axis.X;
    }

    public OleoWheelData(long pos, float steeringValue, double susScaled, Direction.Axis wheelAxis,
                         float axialOffset, float horizontalOffset, double wheelRadius, float wheelRPM,
                         boolean isFreespin, Vector3dc lastSuspensionForce) {
        this.blockPos = pos;
        this.isFreespin = isFreespin;
        this.susScaled = susScaled;
        this.wheelRadius = wheelRadius;
        this.axialOffset = axialOffset;
        this.horizontalOffset = horizontalOffset;
        this.wheelRPM = wheelRPM;
        this.steeringValue = steeringValue;
        this.wheelAxis = wheelAxis;
        this.lastSuspensionForce = lastSuspensionForce;
    }

    public final OleoWheelData updateWith(OleoWheelData update) {
        return new OleoWheelData(
                this.blockPos,
                update.steeringValue,
                update.susScaled,
                update.wheelAxis,
                update.axialOffset,
                update.horizontalOffset,
                update.wheelRadius,
                update.wheelRPM,
                update.isFreespin,
                this.lastSuspensionForce
        );
    }
}
