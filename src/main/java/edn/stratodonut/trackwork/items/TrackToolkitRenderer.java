package edn.stratodonut.trackwork.items;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import edn.stratodonut.trackwork.TrackworkMod;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class TrackToolkitRenderer extends CustomRenderedItemModelRenderer {
    protected static final PartialModel OFFSET_WRENCH = PartialModel.of(TrackworkMod.getResource("item/kit/power_wrench"));
    protected static final PartialModel SOCKET = PartialModel.of(TrackworkMod.getResource("item/kit/socket"));
    protected static final PartialModel STIFFNESS_WRENCH = PartialModel.of(TrackworkMod.getResource("item/kit/stiff_tool"));

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        CompoundTag nbt = stack.getOrCreateTag();
        if (transformType == ItemDisplayContext.GUI) {
            renderer.render(model.getOriginalModel(), light);

        } else if (nbt.contains("Tool")) {
            ms.translate(1/16f, 1/16f, -1/16f);
            TrackToolkit.TOOL type = TrackToolkit.TOOL.from(nbt.getInt("Tool"));
            
            BakedModel toolModel;
            switch (type) {
                case OFFSET -> {
                    float yOffset = 0.5f/16;
                    ms.pushPose();
                    ms.translate(0, -yOffset, 0);
                    ms.mulPose(Axis.XP.rotationDegrees(AnimationTickHolder.getRenderTime() * 15f));
                    ms.translate(0, yOffset, 0);
                    renderer.render(SOCKET.get(), light);
                    ms.popPose();

                    toolModel = OFFSET_WRENCH.get();
                }
                case STIFFNESS -> toolModel = STIFFNESS_WRENCH.get();
                default -> toolModel = model.getOriginalModel();
            }
            renderer.render(toolModel, light);

        } else {
            renderer.render(model.getOriginalModel(), light);
        }
    }
}
