package edn.stratodonut.trackwork.blocks;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import edn.stratodonut.trackwork.TrackBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TrackAdjusterBlock extends RotatedPillarKineticBlock implements IBE<TrackAdjusterBlockEntity> {
    public TrackAdjusterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Class<TrackAdjusterBlockEntity> getBlockEntityClass() {
        return TrackAdjusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TrackAdjusterBlockEntity> getBlockEntityType() {
        return TrackBlockEntityTypes.TRACK_LEVEL_CONTROLLER.get();
    }
}
