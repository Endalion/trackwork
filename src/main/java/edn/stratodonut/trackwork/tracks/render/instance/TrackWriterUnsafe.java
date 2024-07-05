package edn.stratodonut.trackwork.tracks.render.instance;

import com.jozufozu.flywheel.api.struct.StructType;
import com.jozufozu.flywheel.backend.gl.buffer.VecBuffer;
import com.jozufozu.flywheel.core.materials.BasicData;
import com.jozufozu.flywheel.core.materials.BasicWriterUnsafe;
import edn.stratodonut.trackwork.tracks.render.instance.TrackBeltInstance.TrackPartData;
import org.lwjgl.system.MemoryUtil;

public class TrackWriterUnsafe extends BasicWriterUnsafe<TrackPartData> {

    public TrackWriterUnsafe(VecBuffer backingBuffer, StructType<TrackPartData> vertexType) {
        super(backingBuffer, vertexType);
    }

    @Override
    protected void writeInternal(TrackPartData d) {
        super.writeInternal(d);
        long addr = writePointer;
        MemoryUtil.memPutByte(addr + 1, d.flip);
        MemoryUtil.memPutFloat(addr + 5, d.x);
        MemoryUtil.memPutFloat(addr + 9, d.y);
        MemoryUtil.memPutFloat(addr + 13, d.z);
        MemoryUtil.memPutFloat(addr + 17, d.xRot);
        MemoryUtil.memPutFloat(addr + 21, d.speed);
        MemoryUtil.memPutFloat(addr + 25, d.trackPointOffset);
        MemoryUtil.memPutFloat(addr + 29, d.length);
    }
}
