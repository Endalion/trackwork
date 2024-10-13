package edn.stratodonut.trackwork.tracks.blocks;

import com.mojang.datafixers.util.Pair;
import com.simibubi.create.foundation.utility.Lang;
import edn.stratodonut.trackwork.TrackEntityTypes;
import edn.stratodonut.trackwork.TrackPonders;
import edn.stratodonut.trackwork.TrackworkConfigs;
import edn.stratodonut.trackwork.TrackworkMod;
import edn.stratodonut.trackwork.tracks.ITrackPointProvider;
import edn.stratodonut.trackwork.tracks.TrackBeltEntity;
import edn.stratodonut.trackwork.tracks.data.PhysEntityTrackData;
import edn.stratodonut.trackwork.tracks.forces.PhysEntityTrackController;
import edn.stratodonut.trackwork.tracks.render.TrackBeltRenderer;
import edn.stratodonut.trackwork.wheel.WheelEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint;
import org.valkyrienskies.core.apigame.constraints.VSConstraintAndId;
import org.valkyrienskies.core.apigame.constraints.VSHingeOrientationConstraint;
import org.valkyrienskies.core.apigame.physics.PhysicsEntityData;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import static com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS;
import static edn.stratodonut.trackwork.tracks.blocks.TrackBaseBlock.*;
import static net.minecraft.ChatFormatting.GRAY;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;

public class PhysEntityTrackBlockEntity extends TrackBaseBlockEntity implements ITrackPointProvider {
    private float wheelRadius;
    protected final Supplier<Ship> ship;
    private Integer trackID;
    private UUID wheelID;
    @NotNull
    private WeakReference<WheelEntity> wheel;
    public boolean assembled;
    public boolean assembleNextTick = true;

    public PhysEntityTrackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.assembled = false;
        this.wheelRadius = 0.5f;
        this.ship = () -> VSGameUtilsKt.getShipObjectManagingPos(this.level, pos);
        this.wheel = new WeakReference<>(null);
    }

    public static PhysEntityTrackBlockEntity large(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        PhysEntityTrackBlockEntity be = new PhysEntityTrackBlockEntity(type, pos, state);
        be.wheelRadius = 1.0f;
        return be;
    }

    public static PhysEntityTrackBlockEntity med(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        PhysEntityTrackBlockEntity be = new PhysEntityTrackBlockEntity(type, pos, state);
        be.wheelRadius = 0.75f;
        return be;
    }

    @Override
    public void destroy() {
        super.destroy();

        if (this.level != null && !this.level.isClientSide && this.assembled) {
            ServerShip ship = (ServerShip) this.ship.get();
            if (ship != null) {
                PhysEntityTrackController controller = PhysEntityTrackController.getOrCreate(ship);
                controller.removeTrackBlock((ServerLevel) this.level, this.trackID);
                Objects.requireNonNull(this.wheel.get()).kill();
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (!this.level.isClientSide && this.assembled) {
            Entity e = ((ServerLevel) this.level).getEntity(this.wheelID);
            ServerShip ship = (ServerShip) this.ship.get();
            if (ship != null) {
                if (e instanceof WheelEntity wheel) {
                    if (this.constrainWheel(ship, wheel.getShipId(), toJOML(Vec3.atCenterOf(this.getBlockPos()))) != null)
                        return;
                    this.wheel = new WeakReference<>(wheel);
                } else {
                    this.assemble();
                    return;
                }
            }

            this.assembled = false;
            this.assembleNextTick = true;
        }
    }

    @Deprecated
    public boolean summonBelt() {
        if (!this.level.isClientSide) {
            TrackBeltEntity e = TrackBeltEntity.create(this.level, this.getBlockPos());
            e.setPos(Vec3.atLowerCornerOf(this.getBlockPos()));
            this.level.addFreshEntity(e);
        }

        return true;
    }

    private void assemble() {
        if (this.level != null && !this.level.isClientSide) {
            if (!isValidAxis(this.getBlockState().getValue(AXIS))) return;
            ServerLevel slevel = (ServerLevel) this.level;
            ServerShip ship = (ServerShip) this.ship.get();
            if (ship != null) {
                PhysEntityTrackController controller = PhysEntityTrackController.getOrCreate(ship);
                if (this.assembled) {
                    controller.removeTrackBlock((ServerLevel) this.level, this.trackID);
                }
                this.assembled = true;
                Vector3dc trackLocalPos = toJOML(Vec3.atCenterOf(this.getBlockPos()));

                WheelEntity wheel = TrackEntityTypes.WHEEL.create(slevel);
                long wheelId = VSGameUtilsKt.getShipObjectWorld(slevel).allocateShipId(VSGameUtilsKt.getDimensionId(slevel));
                double wheelRadius = this.wheelRadius;
//                Vector3dc wheelOffset = ship.getTransform().getShipToWorldRotation().transform(UP.negate(new Vector3d()));
                Vector3dc wheelGlobalPos = ship.getTransform().getShipToWorld().transformPosition(trackLocalPos, new Vector3d());

                ShipTransform transform = ShipTransformImpl.Companion.create(wheelGlobalPos, new Vector3d());
                PhysicsEntityData wheelData = WheelEntity.DataBuilder.createBasicData(wheelId, transform, wheelRadius, 1000);
                wheel.setPhysicsEntityData(wheelData);
                wheel.setPos(VectorConversionsMCKt.toMinecraft(wheelGlobalPos));
                slevel.addFreshEntity(wheel);

                PhysEntityTrackData.CreateData createData = this.constrainWheel(ship, wheelId, trackLocalPos);
                this.trackID = controller.addTrackBlock(createData);
                this.wheelID = wheel.getUUID();
                this.wheel = new WeakReference<>(wheel);
                this.sendData();
            }
        }
    }

    private PhysEntityTrackData.CreateData constrainWheel(ServerShip ship, long wheelId, Vector3dc trackLocalPos) {
        ServerLevel slevel = (ServerLevel) this.level;
        double attachCompliance = 1e-8;
        double attachMaxForce = 1e150;
        double hingeMaxForce = 1e75;
        Vector3dc axis = getAxisAsVec(this.getBlockState().getValue(AXIS));
//                VSSlideConstraint slider = new VSSlideConstraint(
//                        ship.getId(),
//                        wheelId,
//                        attachCompliance,
//                        trackLocalPos,
//                        new Vector3d(0, 0, 0),
//                        attachMaxForce,
//                        UP,
//                        SUSPENSION_TRAVEL
//                );
        VSAttachmentConstraint slider = new VSAttachmentConstraint(
                ship.getId(),
                wheelId,
                attachCompliance,
                trackLocalPos,
                new Vector3d(0, 0, 0),
                attachMaxForce,
                0.0
        );
        VSHingeOrientationConstraint axle = new VSHingeOrientationConstraint(
                ship.getId(),
                wheelId,
                attachCompliance,
                new Quaterniond().fromAxisAngleDeg(axis, 0),
                new Quaterniond().fromAxisAngleDeg(new Vector3d(0, 0, 1), 0),
                hingeMaxForce
        );


        Integer sliderId = VSGameUtilsKt.getShipObjectWorld(slevel).createNewConstraint(slider);
        Integer axleId = VSGameUtilsKt.getShipObjectWorld(slevel).createNewConstraint(axle);
        if (sliderId == null || axleId == null) return null;

        PhysEntityTrackData.CreateData trackData = new PhysEntityTrackData.CreateData(
                trackLocalPos,
                axis,
                wheelId,
                0,
                0,
                new VSConstraintAndId(sliderId, slider),
                new VSConstraintAndId(axleId, axle),
                this.getSpeed()
        );
        return trackData;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.ship.get() != null && this.assembleNextTick && !this.assembled && this.level != null) {
            this.assemble();
            this.assembleNextTick = false;
            return;
        }

        if (this.level == null) {
            TrackworkMod.warn("Level is null????");
            return;
        }
        if (this.assembled && !this.level.isClientSide) {
            ServerShip ship = (ServerShip) this.ship.get();
            if (ship != null) {
                WheelEntity wheel = this.wheel.get();
                if (wheel == null || !wheel.isAlive() || wheel.isRemoved()) {
                    this.assemble();
                    wheel = this.wheel.get();
                } else {
                    double distance = ship.getShipToWorld().transformPosition(toJOML(Vec3.atCenterOf(this.getBlockPos())))
                            .distance(toJOML(wheel.position()));
                    if (distance > 8f) {
                        this.assemble();
                        wheel = this.wheel.get();
                    };
                }
                if (wheel == null) {
                    // TODO: Figure out why this is happening
                    TrackworkMod.warn("Wheel is NULL after assembly! At %s", this.getBlockPos().toString());
                    return;
                }
                wheel.keepAlive();

                PhysEntityTrackController controller = PhysEntityTrackController.getOrCreate(ship);
                PhysEntityTrackData.UpdateData data = new PhysEntityTrackData.UpdateData(
                        0,
                        0,
                        this.getSpeed()
                );
                controller.updateTrackBlock(this.trackID, data);

//                shipMass = (float) ship.getInertiaData().getMass();
            }
        }
    }


//    @Override
//    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
//        if (!TrackworkConfigs.server().enableStress.get()) return false;
//        Ship ship = this.ship.get();
//        if (!this.assembled || ship == null) return false;
//
//        addStressImpactStats(tooltip, calculateStressApplied(this.shipMass));
////        addMassStats(tooltip, this.shipMass);
//
//        return true;
//    }

    public void addMassStats(List<Component> tooltip, float mass) {
        Lang.text("Total Mass")
                .style(GRAY)
                .forGoggles(tooltip);

        Lang.number(mass)
                .text(" kg")
                .style(ChatFormatting.WHITE)
//                .space()
//                .add(Lang.translate("gui.goggles.at_current_speed")
//                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);
    }

    @Override
    public float getPointDownwardOffset(float partialTicks) {
        return (float) (this.wheelRadius - 0.5);
    }

    @Override
    public float getPointHorizontalOffset() {
        return 0.0f;
    }

    public boolean isBeltLarge() {
        return this.wheelRadius > 0.75;
    }

    @Override
    public Vec3 getTrackPointSlope(float partialTicks) {
        return new Vec3(0,
                Mth.lerp(partialTicks, this.nextPointVerticalOffset.getFirst(), this.nextPointVerticalOffset.getSecond()) - this.getPointDownwardOffset(partialTicks),
                this.nextPointHorizontalOffset
        );
    }

    @Override
    public @NotNull PointType getTrackPointType() {
        return PointType.WRAP;
    }

    @Override
    public float getWheelRadius() {
        return this.wheelRadius;
    }

    @Override
    public float getSpeed() {
        if (!assembled) return 0;
        return Math.min(super.getSpeed(), TrackworkConfigs.server().maxRPM.get());
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putBoolean("Assembled", this.assembled);
        if (this.trackID != null) compound.putInt("trackBlockID", this.trackID);
        if (this.wheelID != null) compound.putUUID("wheelID", this.wheelID);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        this.assembled = compound.getBoolean("Assembled");
        if (this.trackID == null && compound.contains("trackBlockID")) this.trackID = compound.getInt("trackBlockID");
        if (this.wheelID == null && compound.hasUUID("wheelID")) this.wheelID = compound.getUUID("wheelID");
        super.read(compound, clientPacket);
    }

    @Override
    public float calculateStressApplied() {
        if (this.level.isClientSide || !TrackworkConfigs.server().enableStress.get() ||
                !this.assembled || this.getBlockState().getValue(PART) != TrackPart.START) return super.calculateStressApplied();

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
}
