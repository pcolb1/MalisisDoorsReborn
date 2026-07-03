package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.mostlyharmless.malisisdoorsreborn.hbm.client.render.HbmVehicleDoorBlockEntityRenderer;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmVehicleDoorBlockItem;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class HbmVehicleDoorItemRenderer extends BlockEntityWithoutLevelRenderer {

    public HbmVehicleDoorItemRenderer(final BlockEntityRenderDispatcher dispatcher,
                                   final EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(@NotNull final ItemStack stack,
                             @NotNull final ItemDisplayContext displayContext,
                             @NotNull final PoseStack poseStack,
                             @NotNull final MultiBufferSource buffer,
                             final int packedLight,
                             final int packedOverlay) {
        if (stack.isEmpty()) return;

        HbmVehicleDoorBlockEntityRenderer.renderItem(
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                HbmVehicleDoorBlockItem.skinIndexForRender(stack)
        );
    }
}
