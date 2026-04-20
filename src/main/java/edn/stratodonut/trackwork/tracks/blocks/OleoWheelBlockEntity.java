package edn.stratodonut.trackwork.tracks.blocks;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import edn.stratodonut.trackwork.*;
import edn.stratodonut.trackwork.sounds.TrackSoundScapes;
import edn.stratodonut.trackwork.tracks.data.OleoWheelData;
import edn.stratodonut.trackwork.tracks.forces.OleoWheelController;
import edn.stratodonut.trackwork.tracks.network.OleoWheelPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.impl.bodies.properties.BodyKinematicsImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static edn.stratodonut.trackwork.TrackSounds.SUSPENSION_CREAK;
import static edn.stratodonut.trackwork.TrackworkUtil.accumulatedVelocity;
import static edn.stratodonut.trackwork.tracks.forces.OleoWheelController.UP;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toMinecraft;

public class OleoWheelBlockEntity extends SmartBlockEntity {
    private float wheelRadius;
    private float suspensionTravel;
    private double suspensionScale;
    private float steeringValue = 0.0f;

    protected final Random random = new Random();
    private float wheelTravel;
    private float prevWheelTravel;
    private float serverTargetWheelTravel;
    // Server-side only
    private float lastSyncedWheelTravel;
    private static final float COMPRESS_ALPHA = 0.667f;
    private static final float REBOUND_ALPHA = 0.394f;

    private float prevFreeWheelAngle;
    private float horizontalOffset;
    private float axialOffset;

    private boolean isFreespin = true;

    @NotNull
    protected final Supplier<Ship> ship;

    public OleoWheelBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.ship = () -> VSGameUtilsKt.getLoadedShipManagingPos(this.level, pos);
        this.setLazyTickRate(10);
    }

    public static BlockEntityBuilder.BlockEntityFactory<OleoWheelBlockEntity> factory(
            float wheelRadius, float suspensionTravel) {
        return (t,p,s) -> {
            OleoWheelBlockEntity be = new OleoWheelBlockEntity(t,p,s);
            be.wheelRadius = wheelRadius;
            be.suspensionTravel = suspensionTravel;
            return be;
        };
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // DO NOTHING
    }

    @Override
    public void remove() {
        super.remove();

        if (this.level != null && !this.level.isClientSide) {
            LoadedServerShip ship = (LoadedServerShip)this.ship.get();
            if (ship != null) {
                OleoWheelController controller = OleoWheelController.getOrCreate(ship);
                controller.removeTrackBlock(this.getBlockPos());
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Ground particles
        if (this.level.isClientSide && this.ship.get() != null) {
            Vector3d pos = toJOML(Vec3.atBottomCenterOf(this.getBlockPos()));
            Vector3dc ground = VSGameUtilsKt.getWorldCoordinates(this.level, this.getBlockPos(), pos.sub(UP.mul(this.wheelTravel * 1.2, new Vector3d())));
            BlockPos blockpos = BlockPos.containing(toMinecraft(ground));
            BlockState blockstate = this.level.getBlockState(blockpos);
            if (blockstate.isSolid()) {
                Ship s = this.ship.get();
                Vector3dc reversedWheelVel = s.getShipTransform().getShipToWorldRotation().transform(
                        TrackworkUtil.getForwardVec3d(this.getBlockState().getValue(OleoWheelBlock.AXLE_FACING).getAxis(), this.getWheelSpeed()));
                if (Math.abs(this.getWheelSpeed()) > 64) {
                    // Is this safe without calling BlockState::addRunningEffects?
                    if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
                        this.level.addParticle(new BlockParticleOption(
                                        ParticleTypes.BLOCK, blockstate).setPos(blockpos),
                                pos.x + (this.random.nextDouble() - 0.5D),
                                pos.y + 0.25D,
                                pos.z + (this.random.nextDouble() - 0.5D) * this.wheelRadius,
                                reversedWheelVel.x() * -1.0D, 10.5D, reversedWheelVel.z() * -1.0D
                        );
                    }
                }

                // TODO: Slip sounds
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    float wheelSpeed = getWheelSpeed();
                    float pitch = Mth.clamp((Math.abs(wheelSpeed) / 256f) + .45f, .85f, 3f);
                    if (Math.abs(wheelSpeed) < 8)
                        return;
                    TrackSoundScapes.play(TrackAmbientGroups.WHEEL_GROUND_AMBIENT, worldPosition, pitch);

                    Vector3dc shipSpeed = accumulatedVelocity(s.getTransform(),
                            new BodyKinematicsImpl(
                                    s.getVelocity(),
                                    s.getAngularVelocity(),
                                    s.getTransform()
                            ), ground);
                    float slip = (float) reversedWheelVel.negate(new Vector3d()).sub(shipSpeed).length();
                    pitch = Mth.clamp((Math.abs(slip) / 10f) + .45f, .85f, 3f);
                    TrackSoundScapes.play(TrackAmbientGroups.WHEEL_GROUND_SLIP, worldPosition, pitch);
                });
            }
        }

        if (this.level.isClientSide) {
            this.prevWheelTravel = this.wheelTravel;
            float gap = this.serverTargetWheelTravel - this.wheelTravel;
            if (gap > 1.2f || gap < -1.2f) {
                this.wheelTravel = this.serverTargetWheelTravel;
            } else {
                this.wheelTravel += gap * (gap >= 0 ? COMPRESS_ALPHA : REBOUND_ALPHA);
            }
            this.prevFreeWheelAngle += this.getWheelSpeed() * 3f / 10;
            return;
        }

        Direction axleDir = this.getBlockState().getValue(OleoWheelBlock.AXLE_FACING);
        Direction.Axis axleAxis = axleDir.getAxis();
        Direction strutDir = this.getBlockState().getValue(OleoWheelBlock.STRUT_FACING);
        double restOffset = this.wheelRadius - 0.5f;
        double susScaled = this.suspensionTravel * this.suspensionScale;
        LoadedServerShip ship = (LoadedServerShip)this.ship.get();
        if (ship != null) {
            boolean stowed = (strutDir != Direction.DOWN);
            if (stowed) {
                this.steeringValue = 0;
                OleoWheelData data = new OleoWheelData(
                        this.getBlockPos().asLong(),
                        this.getSteeringValue(),
                        0.0,
                        axleAxis,
                        this.getPointAxialOffset(),
                        this.getPointHorizontalOffset(),
                        (double) this.wheelRadius,
                        0,
                        true,
                        null
                );
                OleoWheelController controller = OleoWheelController.getOrCreate(ship);
                this.suspensionScale = controller.updateTrackBlock(this.getBlockPos(), data);

                this.prevWheelTravel = this.wheelTravel;
                this.wheelTravel = (float) (suspensionTravel + restOffset);
                return;
            }

//                 Steering Control
            int bestSignal = this.level.getSignal(this.getBlockPos().relative(axleDir), axleDir)
                    - this.level.getSignal(this.getBlockPos().relative(axleDir.getOpposite()), axleDir.getOpposite());
            float targetSteeringValue = bestSignal / 15f * ((axleDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1));
            float oldSteeringValue = this.steeringValue;

            Direction axleCw = axleDir.getClockWise();
            isFreespin = !(this.level.hasSignal(this.getBlockPos().relative(axleCw), axleCw) ||
                    this.level.hasSignal(this.getBlockPos().relative(axleCw.getOpposite()), axleCw.getOpposite()));

            // Smooth steering interpolation
            float steeringSpeed = 0.5f; // Adjust this value to control steering speed (0.1 = slower, 0.3 = faster)
            this.steeringValue = Mth.lerp(steeringSpeed, this.steeringValue, targetSteeringValue);

            float deltaSteeringValue = oldSteeringValue - this.steeringValue;
            OleoWheelController controller = OleoWheelController.getOrCreate(ship);
            OleoWheelData data = new OleoWheelData(
                    this.getBlockPos().asLong(),
                    this.getSteeringValue(),
                    susScaled,
                    axleAxis,
                    this.getPointAxialOffset(),
                    this.getPointHorizontalOffset(),
                    (double) this.wheelRadius,
                    0,
                    isFreespin,
                    null
            );

            TrackworkUtil.ClipResult clipResult = controller.getSuspensionData(this.getBlockPos());

            double suspensionTravel = clipResult.equals(TrackworkUtil.ClipResult.MISS) ? susScaled : clipResult.suspensionLength().length() - 0.5;
            boolean isOnGround = !clipResult.equals(TrackworkUtil.ClipResult.MISS);

            this.suspensionScale = controller.updateTrackBlock(this.getBlockPos(), data);
            float newWheelTravel = (float) (suspensionTravel + restOffset);
            float delta = newWheelTravel - wheelTravel;

            this.prevWheelTravel = this.wheelTravel;
            this.wheelTravel = newWheelTravel;
            if (Math.abs(this.wheelTravel - this.lastSyncedWheelTravel) > 0.04f || Math.abs(deltaSteeringValue) > 0.12f) {
                this.syncToClient();
                this.lastSyncedWheelTravel = this.wheelTravel;
            }

            // Entity Damage
            List<LivingEntity> hits = this.level.getEntitiesOfClass(LivingEntity.class, new AABB(this.getBlockPos())
                    .deflate(0.25)
                    .expandTowards(0, -1.5, 0)
            );
            Vec3 worldPos = toMinecraft(ship.getShipToWorld().transformPosition(toJOML(Vec3.atCenterOf(this.getBlockPos()))));
            for (LivingEntity e : hits) {
                SuspensionTrackBlockEntity.push(e, worldPos);
                float speed = Math.abs(this.getWheelSpeed());
                if (speed > 1) e.hurt(TrackDamageSources.runOver(this.level), (speed / 16f) * AllConfigs.server().kinetics.crushingDamage.get());
                if (e instanceof ServerPlayer p) p.connection.send(new ClientboundSetEntityMotionPacket(p));
            }

            if (delta < -0.3) {
                this.level.playSound(null, this.getBlockPos(), SUSPENSION_CREAK.get(), SoundSource.BLOCKS,
                        Math.clamp(0.0f, 2.0f, Math.abs(delta * 3 * (this.getWheelSpeed() / 256))*0.5f),
                        Math.lerp(1.2f, 0.8f, -delta) + 0.4F * this.random.nextFloat()
                );
            }
            if (isOnGround && this.random.nextFloat() < Math.abs(this.getWheelSpeed() / 256)*0.1) {
                this.level.playSound(null, this.getBlockPos(),
                        TrackSounds.WHEEL_ROCKTOSS.get(), SoundSource.BLOCKS,
                        Math.max(0.2f, Math.abs(this.getWheelSpeed() / 256)*0.5f),
                        0.8F + 0.4F * this.random.nextFloat());
            }
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!this.level.isClientSide && this.ship.get() != null) this.syncToClient();
    }

    protected void syncToClient() {
        if (!this.level.isClientSide) TrackPackets.getChannel().send(packetTarget(),
                new OleoWheelPacket(this.getBlockPos(), this.wheelTravel, this.getSteeringValue(), this.horizontalOffset));
    }

    public Vector3d getTangentVecWithSteering(Direction.Axis axis, float length) {
        return TrackworkUtil.getForwardVec3d(axis, length)
                .rotateAxis(this.getSteeringValue() * Math.toRadians(30), 0, 1, 0);
    }

    public float getFreeWheelAngle(float partialTick) {
        return (this.prevFreeWheelAngle + this.getWheelSpeed()*partialTick* 3f/10) % 360;
    }

    public float getWheelSpeed() {
        if (this.isFreespin) {
            Ship s = this.ship.get();
            if (s != null) {
                Vector3d vel = s.getVelocity().add(s.getOmega().cross(s.getShipToWorld().transformPosition(
                        toJOML(Vec3.atBottomCenterOf(this.getBlockPos()))).sub(
                        s.getTransform().getPositionInWorld()), new Vector3d()), new Vector3d()
                );
                Direction.Axis axis = this.getBlockState().getValue(OleoWheelBlock.AXLE_FACING).getAxis();
                int sign = axis == Direction.Axis.X ? 1 : -1;
                return sign * (float) TrackworkUtil.roundTowardZero(vel.dot(s.getShipToWorld()
                        .transformDirection(this.getTangentVecWithSteering(axis, 1))) * 9.3f * 1 / wheelRadius);
            }
        }
        return 0;
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putFloat("WheelTravel", this.wheelTravel);
        compound.putFloat("HorizontalOffset", this.horizontalOffset);
        compound.putFloat("AxialOffset", this.axialOffset);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        if (clientPacket) {
            this.serverTargetWheelTravel = compound.getFloat("WheelTravel");
        } else {
            this.wheelTravel = compound.getFloat("WheelTravel");
            this.prevWheelTravel = this.wheelTravel;
            this.serverTargetWheelTravel = this.wheelTravel;
            this.lastSyncedWheelTravel = this.wheelTravel;
        }
        this.horizontalOffset = compound.getFloat("HorizontalOffset");
        this.axialOffset = compound.getFloat("AxialOffset");
        super.read(compound, clientPacket);
    }

    public float getWheelRadius() {
        return this.wheelRadius;
    }

    public float getWheelTravel(float partialTicks) {
        return Mth.lerp(partialTicks, prevWheelTravel, wheelTravel);
    }

    /**
     For ponder usage only!
     **/
    public void setSteeringValue(float value) {
        this.steeringValue = value;
    }

    public float getSteeringValue() {
        return steeringValue;
    }

    public void setOffset(Vector3dc offset, Direction face) {
        Direction.Axis axis = this.getBlockState().getValue(OleoWheelBlock.AXLE_FACING).getAxis();
        if (face.getAxis() == axis) {
            setHorizontalOffset(offset, axis);
        } else {
            setAxialOffset(offset, axis);
        }
    }

    public void setAxialOffset(Vector3dc offset, Direction.Axis axis) {
        double factor = offset.dot(TrackworkUtil.getAxisAsVec(axis));
        this.axialOffset = Math.clamp(-0.4f, 0.4f, Math.round(factor * 8.0f) / 8.0f);
        this.syncToClient();
    }

    public float getPointAxialOffset() {
        return this.axialOffset;
    }

    public void setHorizontalOffset(Vector3dc offset, Direction.Axis axis) {
        double factor = offset.dot(getTangentVecWithSteering(axis, 1));
        this.horizontalOffset = Math.clamp(-0.4f, 0.4f, Math.round(factor * 8.0f) / 8.0f);
        this.syncToClient();
    }

    public float getPointHorizontalOffset() {
        return this.horizontalOffset;
    }

    public void handlePacket(OleoWheelPacket p) {
        this.serverTargetWheelTravel = p.wheelTravel;
        this.steeringValue = p.steeringValue;
        this.horizontalOffset = p.horizontalOffset;
    }
}
