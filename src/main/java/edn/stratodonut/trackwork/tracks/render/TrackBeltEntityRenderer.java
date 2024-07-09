package edn.stratodonut.trackwork.tracks.render;

import edn.stratodonut.trackwork.tracks.ITrackPointProvider;
import edn.stratodonut.trackwork.tracks.TrackBeltEntity;
import edn.stratodonut.trackwork.tracks.blocks.TrackBaseBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

// TODO: Do I go the entity way and suffer?
@Deprecated
public class TrackBeltEntityRenderer extends EntityRenderer<TrackBeltEntity> {
    public TrackBeltEntityRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    @Override
    public void render(TrackBeltEntity e, float f, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
        super.render(e, f, partialTick, poseStack, multiBufferSource, i);

        BlockPos parent = e.getParentPos();
        if (parent == null) return;
        if (!(e.level().getBlockEntity(parent) instanceof ITrackPointProvider)) return;
        Direction iterator = iterateOverBelt(e, parent);
        BlockPos bp = parent;
        BlockEntity be = e.level().getBlockEntity(parent);
        while (be instanceof ITrackPointProvider t) {
            List<SuperByteBuffer> buf = drawTrackPoint(t, be.getBlockState(), partialTick);
            BlockPos finalBp = bp;
            buf.forEach(b -> {
                b.translate(finalBp.subtract(parent));
                b.translate(0, 0.5, 0.0);
                b.light(i).renderInto(poseStack, multiBufferSource.getBuffer(RenderType.solid()));
            });

            bp = bp.relative(iterator);
            be = e.level().getBlockEntity(bp);
        }
    }

    public static List<SuperByteBuffer> drawTrackPoint(ITrackPointProvider track, BlockState state, float partialTicks) {
        List<SuperByteBuffer> result = new ArrayList<>();
        SuperByteBuffer base = CachedBufferer.partial(AllPartialModels.BELT_MIDDLE, state);
        result.add(base);

        switch (track.getTrackPointType()) {
            case WRAP -> {
//                SuperByteBuffer cogs = CachedBufferer.partial(AllPartialModels.BELT_MIDDLE, state);
//                cogs.translate(0, track.getTrackPointOffset(), 0);
//                result.add(cogs);
            }
            case GROUND -> {
                SuperByteBuffer cogs = CachedBufferer.partial(AllPartialModels.BELT_MIDDLE_BOTTOM, state);
                cogs.translate(0, -1 - track.getPointDownwardOffset(partialTicks), 0);
                result.add(cogs);
            }
            default -> {}
        }

        return result;
    }

    public static Direction iterateOverBelt(TrackBeltEntity e, BlockPos start) {
        BlockState state = e.level().getBlockState(start);
        TrackBaseBlock.TrackPart part = state.getValue(TrackBaseBlock.PART);
        return Direction.get(part == TrackBaseBlock.TrackPart.START ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE, state.getValue(TrackBaseBlock.AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
    }

    @Override
    public ResourceLocation getTextureLocation(TrackBeltEntity p_114482_) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
