package edn.stratodonut.trackwork.tracks.blocks;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import edn.stratodonut.trackwork.TrackBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.NotNull;

public class WheelBlock extends HorizontalKineticBlock implements IBE<WheelBlockEntity> {
    public WheelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredHorizontalFacing(context);
        if ((context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown()) || preferred == null)
            return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
        return defaultBlockState().setValue(HORIZONTAL_FACING, preferred);
    }

    public static boolean isValid(Direction facing) {
        return !facing.getAxis().isVertical();
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction(@NotNull BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public Class<WheelBlockEntity> getBlockEntityClass() {
        return WheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<WheelBlockEntity> getBlockEntityType() {
        return TrackBlockEntityTypes.SIMPLE_WHEEL.get();
    }
}
