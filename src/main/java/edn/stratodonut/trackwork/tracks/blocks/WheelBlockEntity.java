package edn.stratodonut.trackwork.tracks.blocks;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.collision.OrientedBB;
import com.simibubi.create.infrastructure.config.AllConfigs;
import edn.stratodonut.trackwork.*;
import edn.stratodonut.trackwork.sounds.TrackSoundScapes;
import edn.stratodonut.trackwork.tracks.data.SimpleWheelData;
import edn.stratodonut.trackwork.tracks.forces.SimpleWheelController;
import edn.stratodonut.trackwork.tracks.network.SimpleWheelPacket;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Intersectiond;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.impl.bodies.properties.BodyKinematicsImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.physics_api.PoseVel;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;
import static edn.stratodonut.trackwork.TrackSounds.SUSPENSION_CREAK;
import static edn.stratodonut.trackwork.TrackworkUtil.accumulatedVelocity;
import static edn.stratodonut.trackwork.tracks.forces.SimpleWheelController.UP;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toMinecraft;

public class WheelBlockEntity extends KineticBlockEntity {
    private float wheelRadius;
    private float suspensionTravel = 1.5f;
    private double suspensionScale = 1.0f;
    private float steeringValue = 0.0f;
    private float linkedSteeringValue = 0.0f;
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
    @NotNull
    protected final Supplier<Ship> ship;

    public boolean isFreespin = true;
    public boolean assembled;
    public boolean assembleNextTick = true;

    public WheelBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.wheelRadius = 1.0f;
        this.suspensionTravel = 1.5f;
        this.ship = () -> VSGameUtilsKt.getShipObjectManagingPos(this.level, pos);
        this.setLazyTickRate(10);
    }

    public static WheelBlockEntity med(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        WheelBlockEntity be = new WheelBlockEntity(type, pos, state);
        be.wheelRadius = 0.75f;
        be.suspensionTravel = 1.5f;
        return be;
    }

    public static WheelBlockEntity large(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        WheelBlockEntity be = new WheelBlockEntity(type, pos, state);
        be.wheelRadius = 1.5f;
        be.suspensionTravel = 2f;
        return be;
    }

    public static WheelBlockEntity small(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        WheelBlockEntity be = new WheelBlockEntity(type, pos, state);
        be.wheelRadius = 0.5f;
        be.suspensionTravel = 1f;
        return be;
    }

    @Override
    public void remove() {
        super.remove();

        if (this.level != null && !this.level.isClientSide && this.assembled) {
            LoadedServerShip ship = (LoadedServerShip)this.ship.get();
            if (ship != null) {
                SimpleWheelController controller = SimpleWheelController.getOrCreate(ship);
                controller.removeTrackBlock(this.getBlockPos());
            }
        }
    }

    private void assemble() {
        if (!WheelBlock.isValid(this.getBlockState().getValue(HORIZONTAL_FACING))) return;
        if (this.level != null && !this.level.isClientSide) {
            LoadedServerShip ship = (LoadedServerShip)this.ship.get();
            if (ship != null && Math.abs(1.0 - ship.getTransform().getShipToWorldScaling().length()) > 0.01) {
                this.assembled = true;
                SimpleWheelController controller = SimpleWheelController.getOrCreate(ship);
                SimpleWheelData.SimpleWheelCreateData data = new SimpleWheelData.SimpleWheelCreateData(toJOML(Vec3.atCenterOf(this.getBlockPos())));
                controller.addTrackBlock(this.getBlockPos(), data);
                this.sendData();
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.ship.get() != null && this.assembleNextTick && !this.assembled && this.level != null) {
            this.assemble();
            this.assembleNextTick = false;
            return;
        }

        // Ground particles
        if (this.level.isClientSide && this.ship.get() != null) {
            Vector3d pos = toJOML(Vec3.atBottomCenterOf(this.getBlockPos()));
            Vector3dc ground = VSGameUtilsKt.getWorldCoordinates(this.level, this.getBlockPos(), pos.sub(UP.mul(this.wheelTravel * 1.2, new Vector3d())));
            BlockPos blockpos = BlockPos.containing(toMinecraft(ground));
            BlockState blockstate = this.level.getBlockState(blockpos);
            if (blockstate.isSolid()) {
                Ship s = this.ship.get();
                Vector3dc reversedWheelVel = s.getShipTransform().getShipToWorldRotation().transform(TrackworkUtil.getForwardVec3d(this.getBlockState().getValue(HORIZONTAL_FACING).getAxis(), this.getWheelSpeed()));
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

        // Freespin check
        Direction dir = this.getBlockState().getValue(HORIZONTAL_FACING);
        BlockPos innerBlock = this.getBlockPos().relative(dir);
        BlockState innerState = this.level.getBlockState(innerBlock);
        if (innerState.getBlock() instanceof KineticBlock ke && ke.hasShaftTowards(level, innerBlock, innerState, dir.getOpposite())) {
            isFreespin = false;
        } else {
            isFreespin = true;
            if (this.level.isClientSide) {
                this.prevFreeWheelAngle += this.getWheelSpeed() * 3f / 10;
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
            return;
        }
        if (this.assembled) {
            Vec3 start = Vec3.atCenterOf(this.getBlockPos());
            Direction.Axis axis = dir.getAxis();
            double restOffset = this.wheelRadius - 0.5f;
            float trackRPM = this.getDrivenSpeed();
            double susScaled = this.suspensionTravel * this.suspensionScale;
            LoadedServerShip ship = (LoadedServerShip)this.ship.get();
            if (ship != null) {
//                 Steering Control
                int bestSignal = this.level.getBestNeighborSignal(this.getBlockPos());
                float targetSteeringValue = bestSignal / 15f * ((dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1));
                float oldSteeringValue = this.steeringValue;

                // Smooth steering interpolation
                float steeringSpeed = 0.5f; // Adjust this value to control steering speed (0.1 = slower, 0.3 = faster)
                this.steeringValue = Mth.lerp(steeringSpeed, this.steeringValue, targetSteeringValue);

                float deltaSteeringValue = oldSteeringValue - this.steeringValue;

                // Flush linked val & propagate when pair target empty
                if (bestSignal > 0) this.linkedSteeringValue = 0f;
                this.onLinkedWheel(wbe -> {
                    int linkedSignal = this.level.getBestNeighborSignal(wbe.getBlockPos());
                    if (linkedSignal == 0) {
                        wbe.setLinkedSteeringValue(this.steeringValue);
                    }
                });

                SimpleWheelController controller = SimpleWheelController.getOrCreate(ship);
                SimpleWheelData.SimpleWheelUpdateData data = new SimpleWheelData.SimpleWheelUpdateData(
                        this.getSteeringValue(),
                        trackRPM,
                        axis,
                        this.getPointAxialOffset(),
                        this.getPointHorizontalOffset(),
                        susScaled,
                        this.wheelRadius,
                        isFreespin
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
                AABB wheelAabb = new AABB(this.getBlockPos())
                        .deflate(0.25)
                        .expandTowards(0, -1.5, 0);
                List<LivingEntity> hits = this.level.getEntitiesOfClass(LivingEntity.class, wheelAabb);
                Vec3 worldPos = toMinecraft(ship.getShipToWorld().transformPosition(toJOML(Vec3.atCenterOf(this.getBlockPos()))));

                Vector3d b0c = ship.getShipToWorld().transformPosition(toJOML(wheelAabb.getCenter()));
                Vector3d b0hs = new Vector3d(wheelAabb.getXsize() / 2.0, wheelAabb.getYsize() / 2.0,
                        wheelAabb.getZsize() / 2.0);
                Vector3d b0uX = ship.getTransform().getShipToWorldRotation().transform(new Vector3d(1, 0, 0));
                Vector3d b0uY = ship.getTransform().getShipToWorldRotation().transform(new Vector3d(0, 1, 0));
                Vector3d b0uZ = ship.getTransform().getShipToWorldRotation().transform(new Vector3d(0, 0, 1));

                for (LivingEntity e : hits) {
                    Ship mountedShip = VSGameUtilsKt.getShipMountedTo(e);
                    if (mountedShip != null && mountedShip.getId() == ship.getId()) continue;
                    Vector3d b1c = toJOML(e.getBoundingBox().getCenter());
                    Vector3d b1hs = new Vector3d(e.getBbWidth() / 2.0, e.getBbHeight() / 2.0, e.getBbWidth() / 2.0);
                    Vector3d b1uX = new Vector3d(1, 0, 0);
                    Vector3d b1uY = new Vector3d(0, 1, 0);
                    Vector3d b1uZ = new Vector3d(0, 0, 1);

                    if (mountedShip != null) {
                        b1uX = mountedShip.getTransform().getShipToWorldRotation().transform(b1uX);
                        b1uY = mountedShip.getTransform().getShipToWorldRotation().transform(b1uY);
                        b1uZ = mountedShip.getTransform().getShipToWorldRotation().transform(b1uZ);
                    }

                    if (!Intersectiond.testObOb(b0c, b0uX, b0uY, b0uZ, b0hs, b1c, b1uX, b1uY, b1uZ, b1hs)) {
                        continue;
                    }

                    SuspensionTrackBlockEntity.push(e, worldPos);
                    float speed = Math.abs(trackRPM);
                    if (speed > 1) e.hurt(TrackDamageSources.runOver(this.level), (speed / 16f) * AllConfigs.server().kinetics.crushingDamage.get());
                    if (e instanceof ServerPlayer p) p.connection.send(new ClientboundSetEntityMotionPacket(p));
                }

                if (delta < -0.3) {
                    this.level.playSound(null, this.getBlockPos(), SUSPENSION_CREAK.get(), SoundSource.BLOCKS,
                            Math.clamp(0.0f, 2.0f, Math.abs(delta * 3 * (this.getSpeed() / 256))*0.5f),
                            Math.lerp(1.2f, 0.8f, -delta) + 0.4F * this.random.nextFloat()
                    );
                }
                if (isOnGround && this.random.nextFloat() < Math.abs(this.getSpeed() / 256)*0.1) {
                    this.level.playSound(null, this.getBlockPos(),
                            TrackSounds.WHEEL_ROCKTOSS.get(), SoundSource.BLOCKS,
                            Math.max(0.2f, Math.abs(this.getSpeed() / 256)*0.5f),
                            0.8F + 0.4F * this.random.nextFloat());
                }
            }
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (this.assembled && !this.level.isClientSide && this.ship.get() != null) this.syncToClient();
    }

    protected void onLinkedWheel(Consumer<WheelBlockEntity> action) {
        Direction dir = this.getBlockState().getValue(HORIZONTAL_FACING);
        for (int i = 1; i <= TrackworkConfigs.server().wheelPairDist.get() + 1; i++) {
            BlockPos bpos = this.getBlockPos().relative(dir, i);
            BlockEntity be = this.level.getBlockEntity(bpos);
            if (be instanceof WheelBlockEntity wbe) {
                action.accept(wbe);
                break;
            }
        }
    }

    public void setLinkedSteeringValue(float v) {
        float old = this.getSteeringValue();
        this.linkedSteeringValue = v;
        float delta = this.getSteeringValue() - old;
        if (Math.abs(delta) > 0.05f) this.syncToClient();
    }

    protected void syncToClient() {
        if (!this.level.isClientSide) TrackPackets.getChannel().send(packetTarget(),
                new SimpleWheelPacket(this.getBlockPos(), this.wheelTravel, this.getSteeringValue(), this.horizontalOffset));
    }

    /*
        This includes steering!
     */
    public Vector3d getActionVec3d(Direction.Axis axis, float length) {
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
                Direction.Axis axis = this.getBlockState().getValue(HORIZONTAL_FACING).getAxis();
                int sign = axis == Direction.Axis.X ? 1 : -1;
                return sign * (float) TrackworkUtil.roundTowardZero(vel.dot(s.getShipToWorld()
                        .transformDirection(this.getActionVec3d(axis, 1))) * 9.3f * 1/wheelRadius);
            }
        }
        return this.getDrivenSpeed();
    }
    
    public float getDrivenSpeed() {
        return this.getSpeed() * 1/this.wheelRadius;
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putBoolean("Assembled", this.assembled);
        compound.putFloat("WheelTravel", this.wheelTravel);
        compound.putFloat("HorizontalOffset", this.horizontalOffset);
        compound.putFloat("AxialOffset", this.axialOffset);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        this.assembled = compound.getBoolean("Assembled");
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
        return Math.abs(linkedSteeringValue) > Math.abs(steeringValue) ? linkedSteeringValue : steeringValue;
    }
    
    public void setOffset(Vector3dc offset, Direction face) {
        Direction.Axis axis = this.getBlockState().getValue(HORIZONTAL_FACING).getAxis();
        if (face.getAxis() == axis) {
            setHorizontalOffset(offset, axis);
        } else {
            setAxialOffset(offset, axis);
        }
    }

    public void setAxialOffset(Vector3dc offset, Direction.Axis axis) {
        double factor = offset.dot(TrackworkUtil.getAxisAsVec(axis));
        this.axialOffset = Math.clamp(-0.4f, 0.4f, Math.round(factor * 8.0f) / 8.0f);
        this.onLinkedWheel(wbe -> {
            wbe.axialOffset = -this.axialOffset;
            wbe.syncToClient();
        });
        this.syncToClient();
    }

    public float getPointAxialOffset() {
        return this.axialOffset;
    }

    public void setHorizontalOffset(Vector3dc offset, Direction.Axis axis) {
        double factor = offset.dot(getActionVec3d(axis, 1));
        this.horizontalOffset = Math.clamp(-0.4f, 0.4f, Math.round(factor * 8.0f) / 8.0f);
        this.onLinkedWheel(wbe -> {
            wbe.horizontalOffset = this.horizontalOffset;
            wbe.syncToClient();
        });
        this.syncToClient();
    }

    public float getPointHorizontalOffset() {
        return this.horizontalOffset;
    }

    @Override
    public float calculateStressApplied() {
        if (this.level.isClientSide || !TrackworkConfigs.server().enableStress.get() || !this.assembled)
            return super.calculateStressApplied();

        Ship ship = this.ship.get();
        if (ship == null) return super.calculateStressApplied();
        double mass = ((ServerShip) ship).getInertiaData().getMass();
        float impact = this.calculateStressApplied((float) mass);
        this.lastStressApplied = impact;
        return impact;
    }

    public float calculateStressApplied(float mass) {
        double impact = (mass / 1000) * TrackworkConfigs.server().stressMult.get() * (2.0f * this.wheelRadius);
        if (impact < 0) {
            impact = 0;
        }
        return (float) impact;
    }

    protected boolean isNoisy() {
        return false;
    }

    public void handlePacket(SimpleWheelPacket p) {
        this.serverTargetWheelTravel = p.wheelTravel;
        this.steeringValue = p.steeringValue;
        this.horizontalOffset = p.horizontalOffset;
    }
}
