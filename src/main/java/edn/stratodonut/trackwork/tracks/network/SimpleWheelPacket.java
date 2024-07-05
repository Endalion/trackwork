package edn.stratodonut.trackwork.tracks.network;

import com.simibubi.create.foundation.networking.BlockEntityDataPacket;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlockEntity;
import edn.stratodonut.trackwork.tracks.blocks.WheelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server to client
 */
public final class SimpleWheelPacket extends BlockEntityDataPacket<WheelBlockEntity> {
    public final float wheelTravel;
    public final float steeringValue;
    public final float horizontalOffset;

    public SimpleWheelPacket(FriendlyByteBuf buffer) {
        super(buffer);
        this.wheelTravel = buffer.readFloat();
        this.steeringValue = buffer.readFloat();
        this.horizontalOffset = buffer.readFloat();
    }

    public SimpleWheelPacket(BlockPos pos, float wheelTravel, float steeringValue, float horizontalOffset) {
        super(pos);
        this.wheelTravel = wheelTravel;
        this.steeringValue = steeringValue;
        this.horizontalOffset = horizontalOffset;
    }

    @Override
    protected void writeData(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.wheelTravel);
        buffer.writeFloat(this.steeringValue);
        buffer.writeFloat(this.horizontalOffset);
    }

    @Override
    protected void handlePacket(WheelBlockEntity blockEntity) {
        blockEntity.handlePacket(this);
    }
}
