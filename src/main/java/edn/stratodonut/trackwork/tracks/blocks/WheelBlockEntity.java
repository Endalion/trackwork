package edn.stratodonut.trackwork.tracks.blocks;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.infrastructure.config.AllConfigs;
import edn.stratodonut.trackwork.TrackPackets;
import edn.stratodonut.trackwork.TrackworkConfigs;
import edn.stratodonut.trackwork.TrackworkUtil;
import edn.stratodonut.trackwork.ducks.MSGPLIDuck;
import edn.stratodonut.trackwork.tracks.data.SimpleWheelData;
import edn.stratodonut.trackwork.tracks.forces.SimpleWheelController;
import edn.stratodonut.trackwork.tracks.network.SimpleWheelPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;
import static edn.stratodonut.trackwork.TrackSounds.SUSPENSION_CREAK;
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
    private float prevFreeWheelAngle;
    private float horizontalOffset;
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
        setLazyTickRate(40);
    }

    @Override
    public void remove() {
        super.remove();

        if (this.level != null && !this.level.isClientSide && this.assembled) {
            ServerShip ship = (ServerShip)this.ship.get();
            if (ship != null) {
                SimpleWheelController controller = SimpleWheelController.getOrCreate(ship);
                controller.removeTrackBlock(this.getBlockPos());
            }
        }
    }

    private void assemble() {
        if (!WheelBlock.isValid(this.getBlockState().getValue(HORIZONTAL_FACING))) return;
        if (this.level != null && !this.level.isClientSide) {
            ServerShip ship = (ServerShip)this.ship.get();
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
        if (this.level.isClientSide && this.ship.get() != null && Math.abs(this.getSpeed()) > 64) {
            Vector3d pos = toJOML(Vec3.atBottomCenterOf(this.getBlockPos()));
            Vector3dc ground = VSGameUtilsKt.getWorldCoordinates(this.level, this.getBlockPos(), pos.sub(UP.mul(this.wheelTravel * 1.2, new Vector3d())));
            BlockPos blockpos = BlockPos.containing(toMinecraft(ground));
            BlockState blockstate = this.level.getBlockState(blockpos);
            // Is this safe without calling BlockState::addRunningEffects?
            if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
                Vector3dc speed = this.ship.get().getShipTransform().getShipToWorldRotation().transform(TrackBaseBlockEntity.getActionVec3d(this.getBlockState().getValue(HORIZONTAL_FACING).getAxis(), this.getSpeed()));
                this.level.addParticle(new BlockParticleOption(
                                ParticleTypes.BLOCK, blockstate).setPos(blockpos),
                        pos.x + (this.random.nextDouble() - 0.5D),
                        pos.y + 0.25D,
                        pos.z + (this.random.nextDouble() - 0.5D) * this.wheelRadius,
                        speed.x() * -1.0D, 10.5D, speed.z() * -1.0D
                );
            }
        }

        // Freespin check
        Direction dir = this.getBlockState().getValue(HORIZONTAL_FACING);
        BlockPos innerBlock = this.getBlockPos().relative(dir);
        BlockState innerState = this.level.getBlockState(innerBlock);
        if (innerState.getBlock() instanceof KineticBlock ke && ke.hasShaftTowards(level, this.getBlockPos(), innerState, dir.getOpposite())) {
            isFreespin = false;
        } else {
            isFreespin = true;
            if (this.level.isClientSide) {
                this.prevFreeWheelAngle += this.getWheelSpeed() * 3f / 10;
            }
        }

        if (this.level.isClientSide) return;
        if (this.assembled) {
            Vec3 start = Vec3.atCenterOf(this.getBlockPos());
            Direction.Axis axis = dir.getAxis();
            double restOffset = this.wheelRadius - 0.5f;
            float trackRPM = this.getSpeed();
            double susScaled = this.suspensionTravel * this.suspensionScale;
            ServerShip ship = (ServerShip)this.ship.get();
            if (ship != null) {
                Vec3 worldSpaceNormal = toMinecraft(ship.getTransform().getShipToWorldRotation().transform(toJOML(this.getActionNormal(axis)), new Vector3d()).mul(susScaled + 0.5));
                Vec3 worldSpaceStart = toMinecraft(ship.getShipToWorld().transformPosition(toJOML(start.add(0, -restOffset, 0))));

//                 Steering Control
                int bestSignal = this.level.getBestNeighborSignal(this.getBlockPos());
                this.steeringValue = bestSignal / 15f * ((dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1));

                this.onLinkedWheel(wbe -> wbe.linkedSteeringValue = this.steeringValue);

                Vector3dc worldSpaceForward = ship.getTransform().getShipToWorldRotation().transform(getActionVec3d(axis, 1), new Vector3d());
                float horizontalOffset = this.getPointHorizontalOffset();
                Vec3 worldSpaceFutureOffset = toMinecraft(
                        worldSpaceForward.normalize(Math.clamp(-0.4 - horizontalOffset, 0.4 - horizontalOffset, 0.05 * ship.getVelocity().dot(worldSpaceForward)), new Vector3d())
                );

                Vec3 worldSpaceHorizontalOffset = toMinecraft(
                        ship.getTransform().getShipToWorldRotation().transform(getForwardVec3d(axis, 1), new Vector3d()).mul(horizontalOffset, new Vector3d())
                );

                Vector3dc forceVec;
                ClipResult clipResult = clipAndResolve(ship, axis, worldSpaceStart.add(worldSpaceHorizontalOffset).add(worldSpaceFutureOffset), worldSpaceNormal);

                forceVec = clipResult.trackTangent.mul(this.wheelRadius / 0.5, new Vector3d());
                if (forceVec.lengthSquared() == 0) {
                    BlockState b = this.level.getBlockState(BlockPos.containing(worldSpaceStart));
                    if (b.getFluidState().is(FluidTags.WATER)) {
                        forceVec = ship.getTransform().getShipToWorldRotation().transform(getActionVec3d(axis, 1)).mul(this.wheelRadius / 0.5).mul(0.2);
                    }
                }

                double suspensionTravel = clipResult.suspensionLength.lengthSqr() == 0 ? susScaled : clipResult.suspensionLength.length() - 0.5;
                Vector3dc suspensionForce = toJOML(worldSpaceNormal.scale( (susScaled - suspensionTravel))).negate();

                SimpleWheelController controller = SimpleWheelController.getOrCreate(ship);
                SimpleWheelData.SimpleWheelUpdateData data = new SimpleWheelData.SimpleWheelUpdateData(
                        toJOML(worldSpaceStart.add(worldSpaceHorizontalOffset)),
                        forceVec,
                        toJOML(worldSpaceNormal),
                        suspensionForce,
                        isFreespin,
                        clipResult.groundShipId,
                        clipResult.suspensionLength.lengthSqr() != 0,
                        trackRPM
                );
                this.suspensionScale = controller.updateTrackBlock(this.getBlockPos(), data);
                float newWheelTravel = (float) (suspensionTravel + restOffset);
                float delta = newWheelTravel - wheelTravel;
                if (delta < -0.667) {
                    this.level.playSound(null, this.getBlockPos(), SUSPENSION_CREAK.get(), SoundSource.BLOCKS, Math.max(1.0f, Math.abs(delta * (this.getWheelSpeed() / 256))), 0.8F + 0.4F * this.random.nextFloat());
                }

                this.prevWheelTravel = this.wheelTravel;
                this.wheelTravel = newWheelTravel;
                TrackPackets.getChannel().send(packetTarget(), new SimpleWheelPacket(this.getBlockPos(), this.wheelTravel, this.getSteeringValue(), this.horizontalOffset));

                // Entity Damage
                // TODO: Players don't get pushed, why?
                List<LivingEntity> hits = this.level.getEntitiesOfClass(LivingEntity.class, new AABB(this.getBlockPos().below(), this.getBlockPos()));
                Vec3 worldPos = toMinecraft(ship.getShipToWorld().transformPosition(toJOML(Vec3.atCenterOf(this.getBlockPos()))));;
                for (LivingEntity e : hits) {
//                    if (e instanceof ItemEntity)
//                        continue;
//                    if (e instanceof AbstractContraptionEntity)
//                        continue;
                    SuspensionTrackBlockEntity.push(e, worldPos);
                    if (e instanceof ServerPlayer p) {
                        ((MSGPLIDuck) p.connection).tallyho$setAboveGroundTickCount(0);
                    }
                    Vec3 relPos = e.position().subtract(worldPos);
                    if (relPos.horizontalDistanceSqr() < 0.25*0.25 || Math.abs(this.getSpeed()) > 1) e.hurt(CreateDamageSources.crush(this.level), 3 * AllConfigs.server().kinetics.crushingDamage.get());
                }
            }
        }
    }

    public record ClipResult(Vector3dc trackTangent, Vec3 suspensionLength, @Nullable Long groundShipId) { ; }

    // TODO: Terrain dynamics
    // Ground pressure?
    private @NotNull ClipResult clipAndResolve(ServerShip ship, Direction.Axis axis, Vec3 start, Vec3 dir) {
        BlockHitResult bResult = this.level.clip(new ClipContext(start, start.add(dir), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        if (bResult.isInside()) {
            // TODO: what to do if the wheel is inside?
        }
        if (bResult.getType() != HitResult.Type.BLOCK) {
            return new ClipResult(new Vector3d(0), Vec3.ZERO, null);
        }
        Ship hitShip = VSGameUtilsKt.getShipObjectManagingPos(this.level, bResult.getBlockPos());
        Long hitShipId = null;
        if (hitShip != null) {
            if (hitShip.equals(ship)) return new ClipResult(new Vector3d(0), Vec3.ZERO, null);
            hitShipId = hitShip.getId();
        }

        Vec3 worldSpacehitExact = bResult.getLocation();
        Vec3 forceNormal = start.subtract(worldSpacehitExact);
        Vec3 worldSpaceAxis = toMinecraft(ship.getTransform().getShipToWorldRotation().transform(
                getAxisAsVec(axis).rotateAxis(this.getSteeringValue() * Math.toRadians(30), 0, 1, 0)
        ));
        return new ClipResult(
                toJOML(worldSpaceAxis.cross(forceNormal)).normalize(),
                forceNormal,
                hitShipId
        );
    }

    protected void onLinkedWheel(Consumer<WheelBlockEntity> action) {
        Direction dir = this.getBlockState().getValue(HORIZONTAL_FACING);
        for (int i = 1; i <= 6; i++) {
            BlockPos bpos = this.getBlockPos().relative(dir, i);
            BlockEntity be = this.level.getBlockEntity(bpos);
            if (be instanceof WheelBlockEntity wbe) {
                action.accept(wbe);
                break;
            }
        }
    }

    protected static Vec3 getActionNormal(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vec3(0, -1, 0);
            case Y -> new Vec3(0,0, 0);
            case Z -> new Vec3(0, -1, 0);
        };
    }

    protected Vector3d getAxisAsVec(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vector3d(1, 0, 0);
            case Y -> new Vector3d(0,1, 0);
            case Z -> new Vector3d(0, 0, 1);
        };
    }

    /*
        This includes steering!
     */
    public Vector3d getActionVec3d(Direction.Axis axis, float length) {
        return getForwardVec3d(axis, length)
                .rotateAxis(this.getSteeringValue() * Math.toRadians(30), 0, 1, 0);
    }

    public Vector3d getForwardVec3d(Direction.Axis axis, float length) {
        return switch (axis) {
            case X -> new Vector3d(0, 0, length);
            case Y -> new Vector3d(0,0, 0);
            case Z -> new Vector3d(length, 0, 0);
        };
    }

    public float getFreeWheelAngle(float partialTick) {
        return (this.prevFreeWheelAngle + this.getWheelSpeed()*partialTick* 3f/10) % 360;
    }

    // To future dev, this does not take wheel radius into account
    public float getWheelSpeed() {
        if (this.isFreespin) {
            Ship s = this.ship.get();
            if (s != null) {
                Vector3d vel = s.getVelocity().add(s.getOmega().cross(s.getShipToWorld().transformPosition(
                        toJOML(Vec3.atBottomCenterOf(this.getBlockPos())), new Vector3d()).sub(
                        s.getTransform().getPositionInWorld(), new Vector3d()), new Vector3d()), new Vector3d()
                );
                Direction.Axis axis = this.getBlockState().getValue(HORIZONTAL_FACING).getAxis();
                int sign = axis == Direction.Axis.X ? 1 : -1;
                return sign * (float) TrackworkUtil.roundTowardZero(vel.dot(s.getShipToWorld()
                        .transformDirection(this.getActionVec3d(axis, 1))) * 9.3f);
            }
        }
        return this.getSpeed();
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putBoolean("Assembled", this.assembled);
        compound.putFloat("WheelTravel", this.wheelTravel);
        compound.putFloat("HorizontalOffset", this.horizontalOffset);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        this.assembled = compound.getBoolean("Assembled");
        this.wheelTravel = compound.getFloat("WheelTravel");
        this.horizontalOffset = compound.getFloat("HorizontalOffset");
        this.prevWheelTravel = this.wheelTravel;
        super.read(compound, clientPacket);
    }

    public float getWheelRadius() {
        return this.wheelRadius;
    }

    public float getWheelTravel(float partialTicks) {
        return Mth.lerp(partialTicks, prevWheelTravel, wheelTravel);
    }

    public void setSteeringValue(float value) {
        this.steeringValue = value;
    }

    public float getSteeringValue() {
        return Math.abs(linkedSteeringValue) > Math.abs(steeringValue) ? linkedSteeringValue : steeringValue;
    }

    public void setHorizontalOffset(Vector3dc offset) {
        Direction.Axis axis = this.getBlockState().getValue(HORIZONTAL_FACING).getAxis();
        double factor = offset.dot(getActionVec3d(axis, 1));
        this.horizontalOffset = Math.clamp(-0.4f, 0.4f, Math.round(factor * 8.0f) / 8.0f);
        this.onLinkedWheel(wbe -> wbe.horizontalOffset = this.horizontalOffset);
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

    public void handlePacket(SimpleWheelPacket p) {
        this.prevWheelTravel = this.wheelTravel;
        this.wheelTravel = p.wheelTravel;
        this.steeringValue = p.steeringValue;
        this.horizontalOffset = p.horizontalOffset;
    }
}
