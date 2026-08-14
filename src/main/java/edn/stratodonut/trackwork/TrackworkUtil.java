package edn.stratodonut.trackwork;

import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlockEntity;
import edn.stratodonut.trackwork.tracks.blocks.WheelBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.bodies.properties.BodyKinematics;
import org.valkyrienskies.core.api.physics.RayCastResult;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.api.world.PhysLevel;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toMinecraft;

public class TrackworkUtil {
    public static final Vector3dc ZERO = new Vector3d();

    public static Vector3dc accumulatedVelocity(ShipTransform t, BodyKinematics pose, Vector3dc worldPosition) {
        return pose.getVelocity().add(pose.getAngularVelocity().cross(worldPosition.sub(t.getPositionInWorld(), new Vector3d()), new Vector3d()), new Vector3d());
    }

    public record ReducedRayCastResult(double distance, @Nonnull Vector3dc velocity) {
        public static final ReducedRayCastResult ZERO = new ReducedRayCastResult(0, new Vector3d());
    }

    public record ClipResult(@Nonnull Vector3dc trackTangent, @Nullable Vector3dc suspensionLength, @Nullable Vector3dc groundVelocity) {
        public static final ClipResult MISS = new ClipResult(new Vector3d(), null, null);
    }

    // TODO: Terrain dynamics
    // Ground pressure?
    public static @Nonnull ClipResult clipAndResolvePhys(PhysLevel physLevel, PhysShip ship, Vector3dc steeringAxis,
                                                     Vector3dc start, Vector3dc clipVector, double wheelRadius, int order,
                                                     long... ignoreWheelIds) {
        Vector3dc worldSpaceAxis = ship.getTransform().getShipToWorldRotation().transform(steeringAxis, new Vector3d());
        Vector3dc normal = clipVector.normalize(new Vector3d());
        Vector3dc tangent = worldSpaceAxis.cross(normal, new Vector3d());

        ReducedRayCastResult bResult;
        Stream<Vector3dc> points;
        if (order == 0) {
            // Single point
            points = Stream.of(start);
        } else if (order == 1) {
            // 3 point line
            double contactWidth = 0.5;
            points = Stream.of(tangent.mul(-contactWidth, new Vector3d()).add(start), start, tangent.mul(contactWidth, new Vector3d()).add(start));
        } else if (order == 2) {
            // 3 point circle
            // sqrt(2)/2 = 0.707106781187
            double contactWidth = 0.707106781187 * wheelRadius;
            double heightOverArc = wheelRadius * (1 - 0.707106781187);
            points = Stream.of(
                    tangent.mul(-contactWidth, new Vector3d()).add(start).sub(normal.mul(heightOverArc, new Vector3d())),
                    start,
                    tangent.mul(contactWidth, new Vector3d()).add(start).sub(normal.mul(heightOverArc, new Vector3d()))
            );
        } else {
            throw new IllegalArgumentException(String.format("Invalid clip order. Must be 0, 1 or 2, received %d", order));
        }

        long[] ignoredIds = Arrays.copyOf(ignoreWheelIds, ignoreWheelIds.length + 1);
        ignoredIds[ignoredIds.length - 1] = ship.getId();

        Optional<ReducedRayCastResult> accumResult = points
                .map(p -> physLevel.rayCast(p, normal, clipVector.length(), ignoredIds))
                .filter(Objects::nonNull)
                .filter(result -> result.getHitBody().getId() != ship.getId())
                .map(result -> new ReducedRayCastResult(result.getDistance(), result.getVelocity()))
                .reduce((a,b) -> new ReducedRayCastResult(
                        Math.min(a.distance, b.distance),
                        a.velocity.add(b.velocity, new Vector3d())
                ));
        if (accumResult.isEmpty()) {
            return ClipResult.MISS;
        }

        bResult = new ReducedRayCastResult(accumResult.get().distance, accumResult.get().velocity);
        Vector3dc worldSpacehitExact = normal.normalize(bResult.distance, new Vector3d()).add(start);
        Vector3dc forceNormal = start.sub(worldSpacehitExact, new Vector3d());
        return new ClipResult(
                normal.cross(worldSpaceAxis, new Vector3d()).normalize(),
                forceNormal,
                bResult.velocity()
        );
    }

    public static double roundTowardZero(double val) {
        if (val < 0) {
            return Math.ceil(val);
        }
        return Math.floor(val);
    }

    public static Direction.Axis around(Direction.Axis axis) {
        if (axis.isVertical()) return axis;
        return (axis == Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
    }

    public static Vec3 getActionNormal(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vec3(0, -1, 0);
            case Y -> new Vec3(0,0, 0);
            case Z -> new Vec3(0, -1, 0);
        };
    }

    public static Vector3d getAxisAsVec(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vector3d(1, 0, 0);
            case Y -> new Vector3d(0,1, 0);
            case Z -> new Vector3d(0, 0, 1);
        };
    }
    
    public static Vector3d getForwardVec3d(Direction.Axis axis, float length) {
        return switch (axis) {
            case X -> new Vector3d(0, 0, length);
            case Y -> new Vector3d(0,0, 0);
            case Z -> new Vector3d(length, 0, 0);
        };
    }
}
