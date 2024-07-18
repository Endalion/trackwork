package edn.stratodonut.trackwork.wheel;

import com.simibubi.create.AllBlocks;
import kotlin.TypeCastException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.ShipObjectClientWorld;
import org.valkyrienskies.mod.common.IShipObjectWorldClientProvider;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.Random;
import java.util.Objects;


// TODO: Do I want this?
@Deprecated
public final class WheelEntityRenderer extends EntityRenderer<WheelEntity> {
    public WheelEntityRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    @Override
    public void render(WheelEntity e, float f, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
        BlockState blockState = AllBlocks.COGWHEEL.getDefaultState();
        if (blockState.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        Level level = e.level();
        if (blockState.equals(level.getBlockState(
                e.blockPosition()
        )) || blockState.getRenderShape() == RenderShape.INVISIBLE
        ) {
            return;
        }

        // Java is trash
        ShipTransform renderTransform;
        try {
            renderTransform = e.getRenderTransform(
                    (ShipObjectClientWorld) (Objects.requireNonNull(((IShipObjectWorldClientProvider) Minecraft.getInstance()).getShipObjectWorld()))
            );
        } catch (NullPointerException | TypeCastException eee) {
            return;
        }

        double expectedX = e.xo + (e.getX() - e.xo) * partialTick;
        double expectedY = e.yo + (e.getY() - e.yo) * partialTick;
        double expectedZ = e.zo + (e.getZ() - e.zo) * partialTick;

        // Replace the default transform applied by mc with these offsets
        double offsetX = renderTransform.getPositionInWorld().x() - expectedX;
        double offsetY = renderTransform.getPositionInWorld().y() - expectedY;
        double offsetZ = renderTransform.getPositionInWorld().z() - expectedZ;

        poseStack.pushPose();
        BlockPos blockPos = BlockPos.containing(e.getX(), e.getBoundingBox().maxY, e.getZ());

        poseStack.translate(offsetX, offsetY, offsetZ);
        poseStack.mulPose(VectorConversionsMCKt.toFloat(renderTransform.getShipToWorldRotation()));
        poseStack.translate(-0.5, -0.5, -0.5);
        BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        blockRenderDispatcher.getModelRenderer().tesselateBlock(
                level, blockRenderDispatcher.getBlockModel(blockState), blockState, blockPos, poseStack,
                multiBufferSource.getBuffer(
                        ItemBlockRenderTypes.getMovingBlockRenderType(blockState)
                ), false, RandomSource.create(), blockState.getSeed(BlockPos.ZERO), OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
        super.render(e, f, partialTick, poseStack, multiBufferSource, i);
    }

    @Override
    public ResourceLocation getTextureLocation(WheelEntity p_114482_) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
