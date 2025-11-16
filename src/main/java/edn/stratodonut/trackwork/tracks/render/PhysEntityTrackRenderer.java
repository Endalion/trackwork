package edn.stratodonut.trackwork.tracks.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import edn.stratodonut.trackwork.TrackworkConfigs;
import edn.stratodonut.trackwork.client.TrackworkPartialModels;
import edn.stratodonut.trackwork.tracks.blocks.PhysEntityTrackBlockEntity;
import edn.stratodonut.trackwork.tracks.blocks.TrackBaseBlock;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class PhysEntityTrackRenderer extends KineticBlockEntityRenderer<PhysEntityTrackBlockEntity> {

    public PhysEntityTrackRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    protected void renderSafe(PhysEntityTrackBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        //if (BackendManager.canUseInstancing(be.getLevel())) {
        //    BlockState state = getRenderedBlockState(be);
        //    RenderType type = getRenderType(be, state);
        //    if (type != null)
        //        renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(type), light);
        //}

        BlockState state = be.getBlockState();
//        Boolean alongFirst = state.getValue(GantryCarriageBlock.AXIS_ALONG_FIRST_COORDINATE);
        Direction.Axis rotationAxis = getRotationAxisOf(be);
        BlockPos visualPos = be.getBlockPos();
        float angleForBE = getAngleForBE(be, visualPos, rotationAxis);
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

//        SuperByteBuffer cogs = CachedBufferer.partial(TrackworkPartialModels.COGS, state);
        SuperByteBuffer cogs = be.getWheelRadius() < 0.6f ?
                CachedBuffers.partial(TrackworkPartialModels.COGS, state) :
                be.getWheelRadius() > 0.8f ? CachedBuffers.partial(TrackworkPartialModels.LARGE_COGS, state) :
                    CachedBuffers.partial(TrackworkPartialModels.MED_COGS, state);
        cogs.center()
                .rotateYDegrees(trackAxis == Direction.Axis.X ? 0 : 90)
                .rotateXDegrees(-angleForBE)
//                .scale(1, be.getWheelRadius() / 0.5f, be.getWheelRadius() / 0.5f)
                .translate(0, 9 / 16f, 0)
                .uncenter();

        cogs.light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));

        if (be.assembled) TrackBeltRenderer.renderBelt(be, partialTicks, ms, buffer, light, new TrackBeltRenderer.ScalableScroll(be, (float) (be.getSpeed() * (be.getWheelRadius() / 0.5)), trackAxis));
    }

    public static float getAngleForBE(KineticBlockEntity be, final BlockPos pos, Direction.Axis axis) {
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float offset = getRotationOffsetForPosition(be, pos, axis);
        return (time * be.getSpeed() * 3f / 10 + offset) % 360;
    }

    @Override
    protected BlockState getRenderedBlockState(PhysEntityTrackBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }

    @Override
    public int getViewDistance() {
        return TrackworkConfigs.client().trackRenderDist.get();
    }
}
