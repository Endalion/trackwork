package edn.stratodonut.trackwork.tracks.blocks.variants;

import edn.stratodonut.trackwork.TrackBlockEntityTypes;
import edn.stratodonut.trackwork.tracks.blocks.PhysEntityTrackBlock;
import edn.stratodonut.trackwork.tracks.blocks.PhysEntityTrackBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class MedPhysEntityTrackBlock extends PhysEntityTrackBlock {
    public MedPhysEntityTrackBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<PhysEntityTrackBlockEntity> getBlockEntityType() {
        return TrackBlockEntityTypes.MED_PHYS_TRACK.get();
    }
}
