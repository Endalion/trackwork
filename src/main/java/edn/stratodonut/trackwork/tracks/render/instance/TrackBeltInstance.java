package edn.stratodonut.trackwork.tracks.render.instance;

import com.jozufozu.flywheel.api.struct.Instanced;
import com.jozufozu.flywheel.api.struct.StructWriter;
import com.jozufozu.flywheel.backend.gl.buffer.VecBuffer;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.layout.BufferLayout;
import com.jozufozu.flywheel.core.materials.BasicData;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.render.AllProgramSpecs;
import edn.stratodonut.trackwork.TrackworkPartialModels;
import edn.stratodonut.trackwork.tracks.ITrackPointProvider;
import edn.stratodonut.trackwork.tracks.blocks.TrackBaseBlock;
import edn.stratodonut.trackwork.tracks.render.TrackBeltRenderer;
import edn.stratodonut.trackwork.tracks.render.TrackBeltRenderer.ScalableScroll;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

public class TrackBeltInstance {
    TrackBaseBlock.TrackPart part;
    ITrackPointProvider track;
    ScalableScroll scroll;
    float yRot;
    private HashMap<String, TrackPartData> parts;

    public TrackBeltInstance(KineticBlockEntity be, ScalableScroll scroll) {
        if (!(be instanceof ITrackPointProvider te)) return;
        BlockState state = be.getBlockState();
        this.yRot = TrackBeltRenderer.getYRotFromState(state);
        this.part = be.getBlockState().getValue(TrackBaseBlock.PART);
        this.track = (ITrackPointProvider)be;

        parts = new HashMap<>();

        //        SuperByteBuffer topLink;
//        TrackBaseBlock.Part part = state.getValue(TrackBaseBlock.PART);
        if (part == TrackBaseBlock.TrackPart.MIDDLE) {
            PartialModel link = TrackworkPartialModels.TRACK_LINK;
//            parts.add()
//            topLink = getLink(state);
//            topLink.centre()
//                    .rotateY(yRot)
//                    .rotateX(180)
//                    .translate(0, -0.5/16f-0.5, -0.5)
//                    .shiftUVScrolling(AllSpriteShifts.BELT, scroll.getAtScale(1.0f))
//                    .unCentre();
//            topLink.light(light).renderInto(ms, buf.getBuffer(RenderType.solid()));
//
//            SuperByteBuffer flatlink = getLink(state);
//            flatlink.centre()
//                    .rotateY(yRot)
//                    .translate(0, -0.5, -0.25)
//                    .translate(0, -fromTrack.getTrackPointOffset(partialTicks), 0)
//                    .scale(1, 1, 0.5f)
//                    .shiftUVScrolling(AllSpriteShifts.BELT, scroll.getAtScale(0.5f))
//                    .unCentre();
//            flatlink.light(light).renderInto(ms, buf.getBuffer(RenderType.solid()));
        } else if (track.getTrackPointType() == ITrackPointProvider.PointType.WRAP) {
//            boolean flip = (part == TrackBaseBlock.Part.END);
//            topLink = CachedBufferer.partial(TrackworkPartialModels.TRACK_WRAP, state);
//            topLink.centre()
//                    .rotateY(yRot)
//                    .translate(0, 0.5 + 1/16f, 0)
//                    .shiftUVScrolling(AllSpriteShifts.BELT, scroll.getAtScale(flip ? -1.0f : 1.0f))
//                    .unCentre();
//            topLink.light(light).renderInto(ms, buf.getBuffer(RenderType.solid()));
        }
//
//        if (fromTrack.getNextPoint() != ITrackPointProvider.PointType.NONE) {
//            float opposite = fromTrack.getTrackPointSlope(partialTicks);
//            // Slope Link
//            SuperByteBuffer link = getLink(state);
//            if (fromTrack.getNextPoint() == fromTrack.getTrackPointType()) {
//                float scale = (float) Math.sqrt(opposite * opposite + 4 / 16f);
//                float angleOffset = (float) (Math.atan2(opposite, 0.5));
//                link.centre()
//                        .rotateY(yRot)
//                        .translate(0, -0.5, 4 / 16f)
//                        .translate(0, -fromTrack.getTrackPointOffset(partialTicks), 0)
//                        .rotateX(angleOffset * 180f / Math.PI)
//                        .scale(1, 1, scale)
//                        .shiftUVScrolling(AllSpriteShifts.BELT, scroll.getAtScale(scale))
//                        .unCentre();
//                link.light(light).renderInto(ms, buf.getBuffer(RenderType.solid()));
//            } else {
//                float scale = (float) Math.sqrt(opposite * opposite + 16 / 16f);
//                float angleOffset = (float) (Math.atan2(opposite, 1.0));
//                link.centre()
//                        .rotateY(yRot)
//                        .translate(0, -0.5, 4 / 16f * (part == TrackBaseBlock.Part.START ? -1 : 1))
//                        .translate(0, -fromTrack.getTrackPointOffset(partialTicks), 0)
//                        .rotateX(angleOffset * 180f / Math.PI)
//                        .scale(1, 1, scale)
//                        .shiftUVScrolling(AllSpriteShifts.BELT, scroll.getAtScale(scale))
//                        .unCentre();
//                link.light(light).renderInto(ms, buf.getBuffer(RenderType.solid()));
//            }
//        }
    }

    public void update() {

    }

    private TrackPartData initPart(TrackPartData d, Vec3 origin, float rotY)
    {

        return d;
    }

    public final class TrackPartData extends BasicData {
        byte flip;
        float x;
        float y;
        float z;
        float xRot;
        float speed;
        float trackPointOffset;
        float length;

        public void update(float trackOffset, float length, float speed, float xRot) {
            this.trackPointOffset = trackOffset;
            this.length = length;
            this.speed = speed;
            this.xRot = xRot;
            markDirty();
        }
    }

    private class TrackType implements Instanced<TrackPartData>{
        @Override
        public TrackPartData create() {
            return new TrackPartData();
        }
        @Override
        public BufferLayout getLayout() {
            return TrackworkInstanceFormats.TRACK;
        }

        @Override
        public StructWriter<TrackPartData> getWriter(VecBuffer backing) {
            return new TrackWriterUnsafe(backing, this);
        }

        @Override
        public ResourceLocation getProgramSpec() {
            return AllProgramSpecs.BELT;
        }
    }
}
