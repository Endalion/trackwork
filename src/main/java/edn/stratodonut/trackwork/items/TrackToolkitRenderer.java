package edn.stratodonut.trackwork.items;

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import edn.stratodonut.trackwork.TrackworkMod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class TrackToolkitRenderer extends CustomRenderedItemModelRenderer {
    protected static final PartialModel OFFSET_WRENCH = new PartialModel(TrackworkMod.getResource("item/kit/power_wrench"));
    protected static final PartialModel SOCKET = new PartialModel(TrackworkMod.getResource("item/kit/socket"));
    protected static final PartialModel STIFFNESS_WRENCH = new PartialModel(TrackworkMod.getResource("item/kit/stiff_tool"));

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemTransforms.TransformType transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        CompoundTag nbt = stack.getOrCreateTag();
        if (transformType == ItemTransforms.TransformType.GUI) {
            renderer.render(model.getOriginalModel(), light);

        } else if (nbt.contains("Tool")) {
            TrackToolkit.TOOL type = TrackToolkit.TOOL.from(nbt.getInt("Tool"));

            BakedModel toolModel;
            switch (type) {
                case OFFSET -> {
                    float yOffset = 0.5f/16;
                    ms.pushPose();
                    ms.translate(0, -yOffset, 0);
                    ms.mulPose(Vector3f.XP.rotationDegrees(AnimationTickHolder.getRenderTime() * 15f));
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
