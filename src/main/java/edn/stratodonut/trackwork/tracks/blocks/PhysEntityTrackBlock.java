package edn.stratodonut.trackwork.tracks.blocks;

import edn.stratodonut.trackwork.TrackBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

public class PhysEntityTrackBlock extends TrackBaseBlock<PhysEntityTrackBlockEntity> {
    public PhysEntityTrackBlock(Properties properties) {
        super(properties);
    }

//    @Override
//    public InteractionResult onBlockEntityUse(BlockGetter world, BlockPos pos, Function<PhysEntityTrackBlockEntity, InteractionResult> action) {
//        BlockEntity be = world.getBlockEntity(pos);
//        if (be instanceof PhysEntityTrackBlockEntity pe) {
//            if (pe.summonBelt()) return InteractionResult.CONSUME;
//        }
//
//        return IBE.super.onBlockEntityUse(world, pos, action);
//    }


    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    // TODO: Detracking
    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public Class<PhysEntityTrackBlockEntity> getBlockEntityClass() {
        return PhysEntityTrackBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PhysEntityTrackBlockEntity> getBlockEntityType() {
        return TrackBlockEntityTypes.PHYS_TRACK.get();
    }
}
