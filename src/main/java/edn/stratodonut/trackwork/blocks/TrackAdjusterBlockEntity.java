package edn.stratodonut.trackwork.blocks;

import com.mojang.math.Vector3f;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import edn.stratodonut.trackwork.tracks.forces.PhysicsTrackController;
import edn.stratodonut.trackwork.tracks.forces.SimpleWheelController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class TrackAdjusterBlockEntity extends KineticBlockEntity {
    public TrackAdjusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void destroy() {
        super.destroy();

        if (this.level.isClientSide) return;
        ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel)this.level, this.getBlockPos());
        if (ship != null) {
            PhysicsTrackController controller = PhysicsTrackController.getOrCreate(ship);
            controller.resetSuspension();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level.isClientSide) return;
        ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel)this.level, this.getBlockPos());
        if (ship != null) {
            Direction.Axis axis = this.getBlockState().getValue(RotatedPillarKineticBlock.AXIS);
            Vector3f vec = Direction.get(Direction.AxisDirection.POSITIVE, axis).step();
            vec.mul(this.getSpeed() / 20000f);

            PhysicsTrackController controller = PhysicsTrackController.getOrCreate(ship);
            controller.adjustSuspension(vec);

            SimpleWheelController controller2 = SimpleWheelController.getOrCreate(ship);
            controller2.adjustSuspension(vec);
        }
    }
}
