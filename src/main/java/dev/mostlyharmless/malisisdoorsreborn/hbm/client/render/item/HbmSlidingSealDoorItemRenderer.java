package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.client.render.HbmSlidingSealDoorBlockEntityRenderer;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmSlidingSealDoorBlockItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public class HbmSlidingSealDoorItemRenderer implements SpecialModelRenderer<Integer> {

    public static final Identifier TYPE = Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "hbm_sliding_seal_door");

    @Override
    public @Nullable Integer extractArgument(@NotNull final ItemStack stack) {
        return HbmSlidingSealDoorBlockItem.skinIndexForRender(stack);
    }

    @Override
    public void submit(@Nullable final Integer skinIndex,
                       @NotNull final PoseStack poseStack,
                       @NotNull final SubmitNodeCollector collector,
                       final int packedLight,
                       final int packedOverlay,
                       final boolean hasFoil,
                       final int outlineColor) {
        HbmSlidingSealDoorBlockEntityRenderer.submitItem(poseStack, collector, packedLight, packedOverlay, skinIndex == null ? 0 : skinIndex);
    }

    @Override
    public void getExtents(@NotNull final Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(-0.5F, -0.5F, -0.5F));
        consumer.accept(new Vector3f(1.5F, 2.0F, 0.5F));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Integer> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public @NotNull MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public @NotNull SpecialModelRenderer<Integer> bake(@NotNull final SpecialModelRenderer.BakingContext context) {
            return new HbmSlidingSealDoorItemRenderer();
        }
    }
}
