package edn.stratodonut.trackwork.tracks.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import edn.stratodonut.trackwork.TrackworkConfigs;
import edn.stratodonut.trackwork.client.TrackworkPartialModels;
import edn.stratodonut.trackwork.tracks.blocks.WheelBlock;
import edn.stratodonut.trackwork.tracks.blocks.WheelBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;

public class SimpleWheelRenderer extends KineticBlockEntityRenderer<WheelBlockEntity> {

    public SimpleWheelRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(WheelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {

        BlockState state = be.getBlockState();
        Direction.Axis rotationAxis = getRotationAxisOf(be);
        BlockPos visualPos = be.getBlockPos();
        float angleForBE = SimpleWheelRenderer.getAngleForBE(be, visualPos, rotationAxis, partialTicks);

        Direction trackDir = state.getValue(HORIZONTAL_FACING);

        float axisMult = (trackDir.getAxis() == Direction.Axis.X) ? 1 : -1;

        float horizontalOffset = be.getPointHorizontalOffset();
        float axialOffset = be.getPointAxialOffset();

        boolean springFlip = trackDir.getAxisDirection() == Direction.AxisDirection.NEGATIVE;
        if (!springFlip)
            angleForBE *= -1;
        else {
            horizontalOffset *= -1;
            axialOffset *= -1;
        }

        float yRot = state.getValue(HORIZONTAL_FACING).toYRot();
        float wheelTravel = be.getWheelTravel(partialTicks) - be.getWheelRadius();
        double wheelTuck = Math.sqrt(2.25 - Math.min(1, wheelTravel * wheelTravel)) - 1.5 - axialOffset;

        // always render lower rib (unless NO_LOWER_RIB variant)
        if (state.hasProperty(WheelBlock.VISUAL_VARIANT)
                && state.getValue(WheelBlock.VISUAL_VARIANT) != WheelBlock.VisualVariant.NO_LOWER_RIB) {
            ribTransform(CachedBufferer.partial(TrackworkPartialModels.SIMPLE_WHEEL_RIB, state), yRot, wheelTravel, ms, buffer, light,
                    new Vec3(horizontalOffset * -axisMult, 0, axialOffset), springFlip);
        }

        // render upper rib (unless NO_SPRING or NO_LOWER_RIB variant)
        if (state.hasProperty(WheelBlock.VISUAL_VARIANT)
                && state.getValue(WheelBlock.VISUAL_VARIANT) != WheelBlock.VisualVariant.NO_SPRING
                && state.getValue(WheelBlock.VISUAL_VARIANT) != WheelBlock.VisualVariant.NO_LOWER_RIB) {
            ribTransform(CachedBufferer.partial(TrackworkPartialModels.SIMPLE_WHEEL_RIB_UPPER, state), yRot, wheelTravel, ms, buffer, light,
                    new Vec3(horizontalOffset * -axisMult, -9/16f, axialOffset), false);
        }

        // spring transform helper
        final BiConsumer<SuperByteBuffer, Float> springTransform = (spring, angle) ->
                spring
                        .translate(0, 14 / 16f, 6 / 16f)
                        .translate(0, -7 / 16f, 12 / 16f)
                        .rotateX(angle)
                        .translate(0, -7 / 16f, -12 / 16f);

        // spring math (shared)
        float springBaseLength = 4 / 16f;
        float springEndPointLength = 12 / 16f;
        double diagonalLength = Math.sqrt(springEndPointLength * springEndPointLength + springBaseLength * springBaseLength);
        double diagonalRibOffset = -Math.atan(springBaseLength);
        double diagonalSpringOffset = (Math.PI / 2 - Math.abs(diagonalRibOffset));

        double ribAngle = Math.atan((wheelTravel + 0.3f) / 1.1);
        double scaleLength = Math.sqrt(springEndPointLength * springEndPointLength + diagonalLength * diagonalLength
                - 2 * springEndPointLength * diagonalLength * Math.cos(Math.PI / 2 + ribAngle - diagonalRibOffset));

        //I Am The Yuggilator.
        if (state.hasProperty(WheelBlock.VISUAL_VARIANT)
                && state.getValue(WheelBlock.VISUAL_VARIANT) != WheelBlock.VisualVariant.NO_SPRING
                && state.getValue(WheelBlock.VISUAL_VARIANT) != WheelBlock.VisualVariant.NO_LOWER_RIB) {

            // upper rib (already rendered above, but this is redundant - keeping for spring math dependency)

            // spring angle (depends on math you calculated above)
            float springAngle = (float) (Math.toDegrees(Math.asin((springEndPointLength * Math.sin(Math.PI/2 + ribAngle
                    - diagonalRibOffset)) / scaleLength) + diagonalSpringOffset - Math.PI/2));

            // spring base
            SuperByteBuffer springBase = CachedBufferer.partial(TrackworkPartialModels.SIMPLE_WHEEL_SPRING_BASE, state);
            springBase.centre().rotateY(-yRot);
            springTransform.accept(springBase, 0f);
            springBase.translate((springFlip ? 12/16f : 0) + horizontalOffset * -axisMult, 0, axialOffset)
                    .unCentre();
            springBase.renderInto(ms, buffer.getBuffer(RenderType.solid()));

            // spring coil
            SuperByteBuffer springCoil = CachedBufferer.partial(TrackworkPartialModels.SIMPLE_WHEEL_SPRING_COIL, state);
            springCoil.centre().rotateY(-yRot);
            springTransform.accept(springCoil, springAngle);
            springCoil
                    .translate((springFlip ? 12/16f : 0) + horizontalOffset * -axisMult, 7/16f, axialOffset)
                    .scale(1, (float) scaleLength / (17f/16), 1)
                    .translate(0, -7/16f, 0)
                    .unCentre();
            springCoil.renderInto(ms, buffer.getBuffer(RenderType.cutout()));
        }


        // wheel mesh
        {
            SuperByteBuffer wheels = be.getWheelRadius() > 1f
                    ? CachedBufferer.partial(TrackworkPartialModels.LARGE_SIMPLE_WHEEL, state)
                    : be.getWheelRadius() > 0.8f
                    ? CachedBufferer.partial(TrackworkPartialModels.SIMPLE_WHEEL, state)
                    : be.getWheelRadius() > 0.5f
                    ? CachedBufferer.partial(TrackworkPartialModels.MED_SIMPLE_WHEEL, state)
                    : CachedBufferer.partial(TrackworkPartialModels.SMALL_SIMPLE_WHEEL, state);

            wheels.centre()
                    .rotateY(-yRot + be.getSteeringValue() * 30)
                    .translate(horizontalOffset * -axisMult, -wheelTravel - 0.5, -wheelTuck)
                    .rotateZ(-angleForBE)
                    .unCentre();

            wheels.light(light)
                    .renderInto(ms, buffer.getBuffer(RenderType.cutout()));
        }
    }

    public static void ribTransform(SuperByteBuffer rib, float yRot, float wheelTravel, PoseStack ms, MultiBufferSource buffer, int light,
                                    Vec3 offset, boolean flip) {
        rib.centre()
                .rotateY(-yRot)
                .translate(offset.x, -8 / 16f - offset.y, 1.5 + offset.z)
                .rotateX(-Math.toDegrees(Math.atan((wheelTravel + 0.3f) / 1.1)))
                .translate(0, 8 / 16f + offset.y, -1.375)
                .unCentre();
        rib.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    public static float getAngleForBE(WheelBlockEntity be, final BlockPos pos, Direction.Axis axis, float partialTick) {
        if (be.isFreespin) return be.getFreeWheelAngle(partialTick);
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float offset = getRotationOffsetForPosition(be, pos, axis);
        return (time * be.getWheelSpeed() * 3f / 10 + offset) % 360;
    }

    @Override
    protected BlockState getRenderedBlockState(WheelBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }

    @Override
    public int getViewDistance() {
        return TrackworkConfigs.client().trackRenderDist.get();
    }
}
