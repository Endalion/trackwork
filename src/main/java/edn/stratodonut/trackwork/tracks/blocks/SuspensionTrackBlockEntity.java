package edn.stratodonut.trackwork.tracks.blocks;

import com.simibubi.create.infrastructure.config.AllConfigs;
import edn.stratodonut.trackwork.TrackPackets;
import edn.stratodonut.trackwork.TrackworkConfigs;
import edn.stratodonut.trackwork.ducks.MSGPLIDuck;
import edn.stratodonut.trackwork.tracks.ITrackPointProvider;
import edn.stratodonut.trackwork.tracks.data.PhysTrackData;
import edn.stratodonut.trackwork.tracks.forces.PhysicsTrackController;
import edn.stratodonut.trackwork.tracks.network.SuspensionWheelPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.RenderShape;
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
import java.util.function.Supplier;

import static com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS;
import static edn.stratodonut.trackwork.tracks.forces.PhysicsTrackController.UP;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toMinecraft;

public class SuspensionTrackBlockEntity extends TrackBaseBlockEntity implements ITrackPointProvider {
    private float wheelRadius;
    private float suspensionTravel = 1.5f;
    protected final Random random = new Random();
    @NotNull
    protected final Supplier<Ship> ship;
    private Integer trackID;
    public boolean assembled;
    public boolean assembleNextTick = true;
    private float wheelTravel;
    private float prevWheelTravel;
    private double suspensionScale = 1.0;
    private float horizontalOffset;

    public SuspensionTrackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.assembled = false;
        this.wheelRadius = 0.5f;
        this.suspensionTravel = 1.5f;
        this.ship = () -> VSGameUtilsKt.getShipObjectManagingPos(this.level, pos);
        setLazyTickRate(40);
    }

    public static SuspensionTrackBlockEntity large(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        SuspensionTrackBlockEntity be = new SuspensionTrackBlockEntity(type, pos, state);
        be.wheelRadius = 1.0f;
        be.suspensionTravel = 2.0f;
        return be;
    }

    public static SuspensionTrackBlockEntity med(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        SuspensionTrackBlockEntity be = new SuspensionTrackBlockEntity(type, pos, state);
        be.wheelRadius = 0.75f;
        be.suspensionTravel = 1.5f;
        return be;
    }

    @Override
    public void onLoad() {
        super.onLoad();

//        if (this.getBlockState().getBlock() )
    }

    @Override
    public void remove() {
        super.remove();

        if (this.level != null && !this.level.isClientSide && this.assembled) {
            ServerShip ship = (ServerShip)this.ship.get();
            if (ship != null) {
                PhysicsTrackController controller = PhysicsTrackController.getOrCreate(ship);
                controller.removeTrackBlock(this.trackID);
            }
        }
    }

    private void assemble() {
        if (!TrackBaseBlock.isValidAxis(this.getBlockState().getValue(AXIS))) return;
        if (this.level != null && !this.level.isClientSide) {
            ServerShip ship = (ServerShip)this.ship.get();
            if (ship != null && Math.abs(1.0 - ship.getTransform().getShipToWorldScaling().length()) > 0.01) {
                this.assembled = true;
                PhysicsTrackController controller = PhysicsTrackController.getOrCreate(ship);
                PhysTrackData.PhysTrackCreateData data = new PhysTrackData.PhysTrackCreateData(toJOML(Vec3.atCenterOf(this.getBlockPos())));
                this.trackID = controller.addTrackBlock(data);
                this.sendData();
                if (this.trackID != null) return;
            }
        }
    }

    public void disassemble() {

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
            BlockPos blockpos = new BlockPos(toMinecraft(ground));
            BlockState blockstate = this.level.getBlockState(blockpos);
            // Is this safe without calling BlockState::addRunningEffects?
            if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
                Vector3dc speed = this.ship.get().getShipTransform().getShipToWorldRotation().transform(getActionVec3d(this.getBlockState().getValue(AXIS), this.getSpeed()));
                this.level.addParticle(new BlockParticleOption(
                        ParticleTypes.BLOCK, blockstate).setPos(blockpos),
                        pos.x + (this.random.nextDouble() - 0.5D),
                        pos.y + 0.25D,
                        pos.z + (this.random.nextDouble() - 0.5D) * this.wheelRadius,
                        speed.x() * -1.0D, 10.5D, speed.z() * -1.0D
                );
            }
        }

        // TODO: degrass + de-snowlayer

        if (this.level.isClientSide) return;
        if (this.assembled) {
            Vec3 start = Vec3.atCenterOf(this.getBlockPos());
            Direction.Axis axis = this.getBlockState().getValue(AXIS);
            double restOffset = this.wheelRadius - 0.5f;
            float trackRPM = this.getSpeed();
            double susScaled = this.suspensionTravel * this.suspensionScale;
            ServerShip ship = (ServerShip)this.ship.get();
            if (ship != null) {
                Vec3 worldSpaceNormal = toMinecraft(ship.getTransform().getShipToWorldRotation().transform(toJOML(this.getActionNormal(axis)), new Vector3d()).mul(susScaled + 0.5));
                Vec3 worldSpaceStart = toMinecraft(ship.getShipToWorld().transformPosition(toJOML(start.add(0, -restOffset, 0))));
                Vector3dc worldSpaceForward = ship.getTransform().getShipToWorldRotation().transform(getActionVec3d(axis, 1), new Vector3d());
                Vec3 worldSpaceFutureOffset = toMinecraft(
                        worldSpaceForward.mul(0.1 * ship.getVelocity().dot(worldSpaceForward), new Vector3d())
                );
                Vec3 worldSpaceHorizontalOffset = toMinecraft(
                        worldSpaceForward.mul(this.getPointHorizontalOffset(), new Vector3d())
                );

                Vector3dc forceVec;
                ClipResult clipResult = clipAndResolve(ship, axis, worldSpaceStart.add(worldSpaceFutureOffset).add(worldSpaceHorizontalOffset), worldSpaceNormal);

                forceVec = clipResult.trackTangent.mul(this.wheelRadius / 0.5, new Vector3d());
                if (forceVec.lengthSquared() == 0) {
                    BlockState b = this.level.getBlockState(new BlockPos(worldSpaceStart));
                    if (b.getFluidState().is(FluidTags.WATER)) {
                        forceVec = ship.getTransform().getShipToWorldRotation().transform(getActionVec3d(axis, 1)).mul(this.wheelRadius / 0.5).mul(0.2);
                    }
                }

                double suspensionTravel = clipResult.suspensionLength.lengthSqr() == 0 ? susScaled : clipResult.suspensionLength.length() - 0.5;
                Vector3dc suspensionForce = toJOML(worldSpaceNormal.scale( (susScaled - suspensionTravel))).negate();

                PhysicsTrackController controller = PhysicsTrackController.getOrCreate(ship);
                if (this.trackID == null) {return;}
                PhysTrackData.PhysTrackUpdateData data = new PhysTrackData.PhysTrackUpdateData(
                        toJOML(worldSpaceStart),
                        forceVec,
                        toJOML(worldSpaceNormal),
                        suspensionForce,
                        clipResult.groundShipId,
                        clipResult.suspensionLength.lengthSqr() != 0,
                        trackRPM
                );
                this.suspensionScale = controller.updateTrackBlock(this.trackID, data);
                this.prevWheelTravel = this.wheelTravel;
                this.wheelTravel = (float) (suspensionTravel + restOffset);
                TrackPackets.getChannel().send(packetTarget(), new SuspensionWheelPacket(this.getBlockPos(), this.wheelTravel));

                // Entity Damage
                // TODO: Players don't get pushed, why?
                List<LivingEntity> hits = this.level.getEntitiesOfClass(LivingEntity.class, new AABB(this.getBlockPos()).expandTowards(0, -1, 0).deflate(0.5));
                Vec3 worldPos = toMinecraft(ship.getShipToWorld().transformPosition(toJOML(Vec3.atCenterOf(this.getBlockPos()))));;
                for (LivingEntity e : hits) {
//                    if (e instanceof ItemEntity)
//                        continue;
//                    if (e instanceof AbstractContraptionEntity)
//                        continue;
                    this.push(e, worldPos);
                    if (e instanceof ServerPlayer p) {
                        ((MSGPLIDuck) p.connection).tallyho$setAboveGroundTickCount(0);
                    }
                    Vec3 relPos = e.position().subtract(worldPos);
                    float speed = Math.abs(this.getSpeed());
                    if (speed > 1) e.hurt(SuspensionTrackBlock.damageSourceTrack, (speed / 8f) * AllConfigs.server().kinetics.crushingDamage.get());
                }
            }
        }
    }

    public record ClipResult(Vector3dc trackTangent, Vec3 suspensionLength, @Nullable Long groundShipId) { ; }

    private @NotNull ClipResult clipAndResolve(ServerShip ship, Direction.Axis axis, Vec3 start, Vec3 dir) {
        BlockHitResult bResult = this.level.clip(new ClipContext(start, start.add(dir), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
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
        Vec3 worldSpaceAxis = toMinecraft(ship.getTransform().getShipToWorldRotation().transform(getAxisAsVec(axis)));
        return new ClipResult(
                toJOML(worldSpaceAxis.cross(forceNormal)).normalize(),
                forceNormal,
                hitShipId
        );
    }

    public void setHorizontalOffset(Vector3dc offset) {
        Direction.Axis axis = this.getBlockState().getValue(AXIS);
        double factor = offset.dot(getActionVec3d(axis, 1));
        this.horizontalOffset = Math.clamp(-0.5f, 0.5f, Math.round(factor * 8.0f) / 8.0f);
        this.setChanged();
    }

    @Override
    public float getPointDownwardOffset(float partialTicks) {
        return this.getWheelTravel(partialTicks);
    }

    @Override
    public float getPointHorizontalOffset() {
        return this.horizontalOffset;
    }

    public boolean isBeltLarge() {
        return this.wheelRadius > 0.75;
    }

    @Override
    public Vec3 getTrackPointSlope(float partialTicks) {
        return new Vec3(0,
                Mth.lerp(partialTicks, this.nextPointVerticalOffset.getFirst(), this.nextPointVerticalOffset.getSecond()) - this.getWheelTravel(partialTicks),
                this.nextPointHorizontalOffset - this.horizontalOffset
        );
    }

    @Override
    public @NotNull PointType getTrackPointType() {
//        if (this.getBlockState().hasProperty(WHEEL_VARIANT) &&
//                this.getBlockState().getValue(WHEEL_VARIANT) == SuspensionTrackBlock.TrackVariant.BLANK) return PointType.BLANK;
        return PointType.GROUND;
    }

    @Override
    public float getWheelRadius() {
        return this.wheelRadius;
    }

    @Override
    public float getSpeed() {
        if (!assembled) return 0;
        return Math.clamp(-TrackworkConfigs.server().maxRPM.get(), TrackworkConfigs.server().maxRPM.get(), super.getSpeed());
    }

    public static void push(Entity entity, Vec3 worldPos) {
        if (!entity.noPhysics) {
            double d0 = entity.getX() - worldPos.x;
            double d1 = entity.getZ() - worldPos.z;
            double d2 = Mth.absMax(d0, d1);
            if (d2 >= (double)0.01F) {
                d2 = java.lang.Math.sqrt(d2);
                d0 /= d2;
                d1 /= d2;
                double d3 = 1.0D / d2;
                if (d3 > 1.0D) {
                    d3 = 1.0D;
                }

                d0 *= d3;
                d1 *= d3;
                d0 *= (double)0.2F;
                d1 *= (double)0.2F;

                if (!entity.isVehicle()) {
                    entity.push(d0, 0.0D, d1);
                }
            }
        }
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putBoolean("Assembled", this.assembled);
        if (this.trackID != null) compound.putInt("trackBlockID", this.trackID);
        compound.putFloat("WheelTravel", this.wheelTravel);
        compound.putFloat("horizontalOffset", this.horizontalOffset);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        this.assembled = compound.getBoolean("Assembled");
        if (this.trackID == null && compound.contains("trackBlockID")) this.trackID = compound.getInt("trackBlockID");
        this.wheelTravel = compound.getFloat("WheelTravel");
        if (compound.contains("horizontalOffset")) this.horizontalOffset = compound.getFloat("horizontalOffset");
        this.prevWheelTravel = this.wheelTravel;
        super.read(compound, clientPacket);
    }

    public float getWheelTravel() {
        return this.wheelTravel;
    }

    public float getWheelTravel(float partialTicks) {
        return Mth.lerp(partialTicks, prevWheelTravel, wheelTravel);
    }

    public void handlePacket(SuspensionWheelPacket p) {
        this.prevWheelTravel = this.wheelTravel;
        this.wheelTravel = p.wheelTravel;
    }
}
