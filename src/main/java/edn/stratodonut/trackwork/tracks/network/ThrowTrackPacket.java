package edn.stratodonut.trackwork.tracks.network;

import com.simibubi.create.foundation.networking.BlockEntityDataPacket;
import edn.stratodonut.trackwork.tracks.blocks.TrackBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class ThrowTrackPacket extends BlockEntityDataPacket<TrackBaseBlockEntity> {
    public final boolean detracked;

    public ThrowTrackPacket(FriendlyByteBuf buffer) {
        super(buffer);
        this.detracked = buffer.readBoolean();
    }

    public ThrowTrackPacket(BlockPos pos, boolean detracked) {
        super(pos);
        this.detracked = detracked;
    }

    @Override
    protected void writeData(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.detracked);
    }

    @Override
    protected void handlePacket(TrackBaseBlockEntity blockEntity) {
        blockEntity.handlePacket(this);
    }
}
