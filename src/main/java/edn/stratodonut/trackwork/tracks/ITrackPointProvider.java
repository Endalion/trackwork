package edn.stratodonut.trackwork.tracks;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public interface ITrackPointProvider {
    enum PointType {
        WRAP,
        GROUND,
        BLANK,
        NONE
    }

    float getPointDownwardOffset(float partialTicks);

    boolean isBeltLarge();

    float getPointHorizontalOffset();

    Vec3 getTrackPointSlope(float partialTicks);

    @NotNull PointType getTrackPointType();

    @NotNull PointType getNextPoint();

    float getWheelRadius();
}
