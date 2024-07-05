package edn.stratodonut.trackwork.tracks.blocks.variants;

import edn.stratodonut.trackwork.TrackBlockEntityTypes;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlock;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class LargeSuspensionTrackBlock extends SuspensionTrackBlock {

    public LargeSuspensionTrackBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends SuspensionTrackBlockEntity> getBlockEntityType() {
        return TrackBlockEntityTypes.LARGE_SUSPENSION_TRACK.get();
    }
}
