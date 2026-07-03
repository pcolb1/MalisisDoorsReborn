package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.client.render.HbmFireDoorBlockEntityRenderer;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmFireDoorBlockItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public class HbmFireDoorItemRenderer implements SpecialModelRenderer<Integer> {

    public static final Identifier TYPE = Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "hbm_fire_door");

    @Override
    public @Nullable Integer extractArgument(@NotNull final ItemStack stack) {
        return HbmFireDoorBlockItem.skinIndexForRender(stack);
    }

    @Override
    public void submit(@Nullable final Integer skinIndex,
                       @NotNull final PoseStack poseStack,
                       @NotNull final SubmitNodeCollector collector,
                       final int packedLight,
                       final int packedOverlay,
                       final boolean hasFoil,
                       final int outlineColor) {
        HbmFireDoorBlockEntityRenderer.submitItem(poseStack, collector, packedLight, packedOverlay, skinIndex == null ? 0 : skinIndex);
    }

    @Override
    public void getExtents(@NotNull final Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(-1.0F, 0.0F, 0.375F));
        consumer.accept(new Vector3f(2.0F, 2.0F, 0.625F));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Integer> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public @NotNull MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public @NotNull SpecialModelRenderer<Integer> bake(@NotNull final SpecialModelRenderer.BakingContext context) {
            return new HbmFireDoorItemRenderer();
        }
    }
}
