package edn.stratodonut.trackwork.tracks.blocks.variants;

import edn.stratodonut.trackwork.TrackBlockEntityTypes;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlock;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class MedSuspensionTrackBlock extends SuspensionTrackBlock {
    public MedSuspensionTrackBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<SuspensionTrackBlockEntity> getBlockEntityType() {
        return TrackBlockEntityTypes.MED_SUSPENSION_TRACK.get();
    }
}
