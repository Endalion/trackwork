package edn.stratodonut.trackwork.tracks.blocks.variants;

import edn.stratodonut.trackwork.TrackBlockEntityTypes;
import edn.stratodonut.trackwork.tracks.blocks.PhysEntityTrackBlock;
import edn.stratodonut.trackwork.tracks.blocks.PhysEntityTrackBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class LargePhysEntityTrackBlock extends PhysEntityTrackBlock {
    public LargePhysEntityTrackBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<PhysEntityTrackBlockEntity> getBlockEntityType() {
        return TrackBlockEntityTypes.LARGE_PHYS_TRACK.get();
    }
}
