package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.client.render.HbmQeContainmentDoorBlockEntityRenderer;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmQeContainmentDoorBlockItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public class HbmQeContainmentDoorItemRenderer implements SpecialModelRenderer<Integer> {

    public static final Identifier TYPE = Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "hbm_qe_containment_door");

    @Override
    public @Nullable Integer extractArgument(@NotNull final ItemStack stack) {
        return HbmQeContainmentDoorBlockItem.skinIndexForRender(stack);
    }

    @Override
    public void submit(@Nullable final Integer skinIndex,
                       @NotNull final ItemDisplayContext displayContext,
                       @NotNull final PoseStack poseStack,
                       @NotNull final SubmitNodeCollector collector,
                       final int packedLight,
                       final int packedOverlay,
                       final boolean hasFoil,
                       final int outlineColor) {
        HbmQeContainmentDoorBlockEntityRenderer.submitItem(poseStack, collector, packedLight, packedOverlay, skinIndex == null ? 0 : skinIndex);
    }

    @Override
    public void getExtents(@NotNull final Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(-1.0F, 0.0F, 0.25F));
        consumer.accept(new Vector3f(2.0F, 3.0F, 0.75F));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public @NotNull MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(@NotNull final SpecialModelRenderer.BakingContext context) {
            return new HbmQeContainmentDoorItemRenderer();
        }
    }
}
