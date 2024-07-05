package edn.stratodonut.trackwork.tracks.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.render.SpriteShiftEntry;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import edn.stratodonut.trackwork.TrackworkPartialModels;
import edn.stratodonut.trackwork.TrackworkSpriteShifts;
import edn.stratodonut.trackwork.tracks.ITrackPointProvider;
import edn.stratodonut.trackwork.tracks.blocks.TrackBaseBlock;
import edn.stratodonut.trackwork.tracks.blocks.TrackBaseBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static edn.stratodonut.trackwork.tracks.ITrackPointProvider.PointType;

public class TrackBeltRenderer {
    public static void renderBelt(TrackBaseBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                                  int light, ScalableScroll scroll) {

//        if (Backend.canUseInstancing(be.getLevel())) return;
        if (be.isDetracked()) return;
        BlockState state = be.getBlockState();
        float yRot = getYRotFromState(state);
        renderLink(be, partialTicks, state, yRot, light, ms, buffer, scroll);
    }

    private static void renderLink(ITrackPointProvider fromTrack, float partialTicks,
                                   BlockState state, float yRot, int light, PoseStack ms, MultiBufferSource buf,
                                   ScalableScroll scroll) {
        boolean isLarge = fromTrack.isBeltLarge();
        float largeScale = isLarge ? 2 : 2*fromTrack.getWheelRadius();
        SuperByteBuffer topLink;
        TrackBaseBlock.TrackPart part = state.getValue(TrackBaseBlock.PART);
        if (part == TrackBaseBlock.TrackPart.MIDDLE) {
            topLink = getLink(state);
            topLink.centre()
                    .rotateY(yRot)
                    .rotateX(180)
                    .translate(0, (-0.5)*(17/16f)*largeScale, -0.5)
                    .scale(1, largeScale, 1)
                    .shiftUVScrolling(TrackworkSpriteShifts.BELT, scroll.getAtScale(1.0f))
                    .unCentre();
            topLink.light(light).renderInto(ms, buf.getBuffer(RenderType.solid()));

            SuperByteBuffer flatlink = getLink(state);
            flatlink.centre()
                    .rotateY(yRot)
                    .translate(0, -0.5f, -0.25)
                    .translate(0, -fromTrack.getPointDownwardOffset(partialTicks), fromTrack.getPointHorizontalOffset())
                    .scale(1, largeScale, 0.5f)
                    .shiftUVScrolling(TrackworkSpriteShifts.BELT, scroll.getAtScale(0.5f))
                    .unCentre();
            flatlink.light(light).renderInto(ms, buf.getBuffer(RenderType.solid()));
        } else if (fromTrack.getTrackPointType() == PointType.WRAP) {
            float flip = (part == TrackBaseBlock.TrackPart.END) ? -1 : 1;
            topLink = getLink(state);
            topLink.centre()
                    .rotateY(yRot)
                    .rotateX(180)
                    .translate(0, (-0.5)*(17/16f)*largeScale, -0.5)
                    .scale(1, largeScale, 12/16f + (largeScale/16f))
                    .shiftUVScrolling(TrackworkSpriteShifts.BELT, scroll.getAtScale(flip))
                    .unCentre();
            topLink.light(light).renderInto(ms, buf.getBuffer(RenderType.solid()));

            SuperByteBuffer wrapLink = CachedBufferer.partial(TrackworkPartialModels.TRACK_WRAP, state);
            wrapLink.centre()
                    .rotateY(yRot)
                    .scale(1, largeScale,largeScale)
                    .translate(0, (0.5) + 1/16f, fromTrack.getWheelRadius() > 0.667 ? 0.5/16f : -1/16f)
                    .shiftUVScrolling(TrackworkSpriteShifts.BELT, scroll.getAtScale(flip))
                    .unCentre();
            wrapLink.light(light).renderInto(ms, buf.getBuffer(RenderType.solid()));
        }

        if (fromTrack.getNextPoint() != PointType.NONE) {
            Vec3 offset = fromTrack.getTrackPointSlope(partialTicks);
            float opposite = (float) offset.y;
            float adjacent = 1 + (float) offset.z;
            // Slope Link
            SuperByteBuffer link = getLink(state);
            if (fromTrack.getNextPoint() == fromTrack.getTrackPointType()) {        // Middle
                float cut_adjacent = 8/16f + (float) offset.z;
                float length = (float) Math.sqrt(opposite*opposite + cut_adjacent*cut_adjacent);
                float angleOffset = (float) (Math.atan2(opposite, cut_adjacent));
                link.centre()
                        .rotateY(yRot)
                        .translate(0, -0.5f, 4 / 16f)
                        .translate(0, -fromTrack.getPointDownwardOffset(partialTicks), fromTrack.getPointHorizontalOffset())
                        .rotateX(angleOffset * 180f / Math.PI)
                        .scale(1, largeScale, length)
                        .shiftUVScrolling(TrackworkSpriteShifts.BELT, scroll.getAtScale(length))
                        .unCentre();
                link.light(light).renderInto(ms, buf.getBuffer(RenderType.solid()));
            } else {                                                                // Ends
                float length = (float) Math.sqrt(opposite*opposite + adjacent*adjacent + (isLarge ? 4/16f : 0));
                float flip = (part == TrackBaseBlock.TrackPart.START) ? -1 : 1;
                float angleOffset = (float) (Math.atan2(opposite, adjacent + (isLarge ? 2/16f : 0)));
                link.centre()
                        .rotateY(yRot)
                        .translate(0, -0.5f, (5 / 16f + ((isLarge && fromTrack.getTrackPointType() == PointType.WRAP) ? 2/16f : 0)) * flip)
                        .translate(0, -fromTrack.getPointDownwardOffset(partialTicks), fromTrack.getPointHorizontalOffset())
                        .rotateX(angleOffset * 180f / Math.PI)
                        .scale(1, largeScale, length)
                        .shiftUVScrolling(TrackworkSpriteShifts.BELT, scroll.getAtScale(length))
                        .unCentre();
                link.light(light).renderInto(ms, buf.getBuffer(RenderType.solid()));
            }
        }
    }

    public static class ScalableScroll {
        private final float trueSpeed;
        private final float time;
        private final float spriteSize;
        private final float scrollMult;

        public ScalableScroll(KineticBlockEntity be, final float speed, Direction.Axis axis) {
            this.trueSpeed = (axis == Direction.Axis.X) ? speed : -speed;
            this.time = AnimationTickHolder.getRenderTime(be.getLevel()) * 1;

            this.scrollMult = 0.5f;
            SpriteShiftEntry spriteShift = TrackworkSpriteShifts.BELT;
            this.spriteSize = spriteShift.getTarget().getV1() - spriteShift.getTarget().getV0();
        }

        public float getAtScale(float scale) {
            float speed = this.trueSpeed / scale;

            if (speed != 0) {
                double scroll = speed * this.time / (31.5 * 16);
                scroll = scroll - Math.floor(scroll);
                scroll = scroll * this.spriteSize * this.scrollMult;

                return (float) scroll;
            }
            return 0;
        }
    }

    private static SuperByteBuffer getLink(BlockState state) {
        return CachedBufferer.partial(TrackworkPartialModels.TRACK_LINK, state);
    }

    public static Direction getAlong(BlockState state) {
        return state.getValue(RotatedPillarKineticBlock.AXIS) == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
    }

    public static float getYRotFromState(BlockState state) {
        Direction.Axis trackAxis = state.getValue(RotatedPillarKineticBlock.AXIS);
        boolean flip = state.getValue(TrackBaseBlock.PART) == TrackBaseBlock.TrackPart.END;
        return ((trackAxis == Direction.Axis.X) ? 0 : 90) + (flip ? 180 : 0);
    }
}
