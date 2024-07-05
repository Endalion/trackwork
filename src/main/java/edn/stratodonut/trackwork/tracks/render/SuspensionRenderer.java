package edn.stratodonut.trackwork.tracks.render;

import edn.stratodonut.trackwork.TrackworkConfigs;
import edn.stratodonut.trackwork.TrackworkPartialModels;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlock;
import edn.stratodonut.trackwork.tracks.blocks.TrackBaseBlock;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class SuspensionRenderer extends KineticBlockEntityRenderer<SuspensionTrackBlockEntity> {

    public SuspensionRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SuspensionTrackBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
//        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

//        if (Backend.canUseInstancing(be.getLevel())) return;

        BlockState state = be.getBlockState();
//        Boolean alongFirst = state.getValue(GantryCarriageBlock.AXIS_ALONG_FIRST_COORDINATE);
        Direction.Axis rotationAxis = getRotationAxisOf(be);
        BlockPos visualPos = be.getBlockPos();
        float angleForBE = SuspensionRenderer.getAngleForBE(be, visualPos, rotationAxis);
//
        Direction.Axis trackAxis = state.getValue(TrackBaseBlock.AXIS);
//        for (Direction.Axis axis : Iterate.axes)
//            if (axis != rotationAxis && axis != facing.getAxis())
//                trackAxis = axis;

        if (trackAxis == Direction.Axis.X)
            angleForBE *= -1;

//        if (trackAxis == Direction.Axis.Y)
//            if (facing == Direction.NORTH || facing == Direction.EAST)
//                angleForBE *= -1;
        float yRot = (trackAxis == Direction.Axis.X) ? 0 : 90;

        if (state.hasProperty(SuspensionTrackBlock.WHEEL_VARIANT)
                && state.getValue(SuspensionTrackBlock.WHEEL_VARIANT) != SuspensionTrackBlock.TrackVariant.BLANK) {
//            SuperByteBuffer wheels = CachedBufferer.partial(TrackworkPartialModels.SUSPENSION_WHEEL, state) ;
            SuperByteBuffer wheels = be.getWheelRadius() < 0.6f
                    ? CachedBufferer.partial(TrackworkPartialModels.SUSPENSION_WHEEL, state) :
                    be.getWheelRadius() > 0.8f ? CachedBufferer.partial(TrackworkPartialModels.LARGE_SUSPENSION_WHEEL, state) :
                    CachedBufferer.partial(TrackworkPartialModels.MED_SUSPENSION_WHEEL, state);
            wheels.centre()
                    .rotateY(yRot)
                    .translate(0, be.getWheelRadius() - 0.5, 0)
                    .translate(0, -be.getWheelTravel(partialTicks), be.getPointHorizontalOffset())
                    .rotateX(-angleForBE)
//                    .scale(1, be.getWheelRadius() / 0.5f, be.getWheelRadius() / 0.5f)
                    .translate(0, 9 / 16f, 0)
                    .unCentre();

            wheels.light(light)
                    .renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }

        if (be.assembled) TrackBeltRenderer.renderBelt(be, partialTicks, ms, buffer, light, new TrackBeltRenderer.ScalableScroll(be, (float) (be.getSpeed() * (be.getWheelRadius() / 0.5)), trackAxis));
    }

    public static float getAngleForBE(KineticBlockEntity be, final BlockPos pos, Direction.Axis axis) {
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float offset = getRotationOffsetForPosition(be, pos, axis);
        return (time * be.getSpeed() * 3f / 10 + offset) % 360;
    }

    @Override
    protected BlockState getRenderedBlockState(SuspensionTrackBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }

    @Override
    public int getViewDistance() {
        return TrackworkConfigs.client().trackRenderDist.get();
    }
}
