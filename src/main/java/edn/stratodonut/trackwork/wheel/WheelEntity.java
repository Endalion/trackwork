package edn.stratodonut.trackwork.wheel;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.properties.ShipInertiaData;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.apigame.physics.PhysicsEntityData;
import org.valkyrienskies.core.apigame.physics.VSWheelCollisionShapeData;
import org.valkyrienskies.core.impl.game.ships.ShipInertiaDataImpl;
import org.valkyrienskies.mod.common.entity.VSPhysicsEntity;

public class WheelEntity extends VSPhysicsEntity {

    @SuppressWarnings("unchecked")
    public WheelEntity(@NotNull EntityType<? extends VSPhysicsEntity> type, @NotNull Level level) {
        super((EntityType<VSPhysicsEntity>) type, level);
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose p_19975_) {
        return new EntityDimensions(0.01F, 0.01F, false);
    }

    int timeout = 0;
    @Override
    public void tick() {
        super.tick();

        // Debug particles
//        BlockPos p_51253_ = this.blockPosition();
//        this.level.addParticle(ParticleTypes.SMOKE, (double)p_51253_.getX() + 0.5D + random.nextDouble() / 4.0D * (double)(random.nextBoolean() ? 1 : -1), (double)p_51253_.getY() + 0.4D, (double)p_51253_.getZ() + 0.5D + random.nextDouble() / 4.0D * (double)(random.nextBoolean() ? 1 : -1), 0.0D, 0.005D, 0.0D);

        if (this.level().isClientSide) return;

        timeout++;

        if (timeout > 60) {
            this.kill();
        }
    }

    public long getShipId() {
        if (this.getPhysicsEntityData() == null) return -1L;
        return this.getPhysicsEntityData().getShipId();
    }

    public void keepAlive() {
        this.timeout = 0;
    }

    public static final class DataBuilder {
        private DataBuilder() {
        }

        @NotNull
        public static PhysicsEntityData createBasicData(long shipId, @NotNull ShipTransform transform, double radius, double mass) {
            double inertia = 0.4 * mass * radius * radius;
            ShipInertiaData inertiaData = new ShipInertiaDataImpl(new Vector3d(), mass * radius, new Matrix3d().scale(inertia));
            VSWheelCollisionShapeData collisionShapeData = new VSWheelCollisionShapeData(radius, 0.45, (int)(11 * radius));
            return new PhysicsEntityData(
                    shipId,
                    transform,
                    inertiaData,
                    new Vector3d(),
                    new Vector3d(),
                    collisionShapeData,
                    -1,
                    0.8,
                    0.6,
                    0.6,
                    false

            );
        }
    }
}
