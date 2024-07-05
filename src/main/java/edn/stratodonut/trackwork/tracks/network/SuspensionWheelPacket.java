package edn.stratodonut.trackwork.tracks.network;

import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlockEntity;
import com.simibubi.create.foundation.networking.BlockEntityDataPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server to client
 */
public final class SuspensionWheelPacket extends BlockEntityDataPacket<SuspensionTrackBlockEntity> {
    public final float wheelTravel;

    public SuspensionWheelPacket(FriendlyByteBuf buffer) {
        super(buffer);
        this.wheelTravel = buffer.readFloat();
    }

    public SuspensionWheelPacket(BlockPos pos, float wheelTravel) {
        super(pos);
        this.wheelTravel = wheelTravel;
    }

    @Override
    protected void writeData(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.wheelTravel);
    }

    @Override
    protected void handlePacket(SuspensionTrackBlockEntity blockEntity) {
        blockEntity.handlePacket(this);
    }
}
