package dev.mostlyharmless.malisisdoorsreborn.hbm.item;

import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmDoorRedstoneMode;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessHelper;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessLevel;

import dev.mostlyharmless.malisisdoorsreborn.hbm.client.render.item.HbmFireDoorItemRenderer;
import dev.mostlyharmless.malisisdoorsreborn.item.TooltipBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HbmFireDoorBlockItem extends TooltipBlockItem {

    public static final String SKIN_TAG = "SkinIndex";
    public static final String REDSTONE_MODE_TAG = "RedstoneMode";
    private static final String SKIN_PREVIEW_CYCLE_TAG = "SkinPreviewCycle";
    public static final int SKIN_COUNT = 5;

    private static final String[] SKIN_TRANSLATION_KEYS = {
            "tooltip.malisisdoorsreborn.hbm_fire_door.skin.default",
            "tooltip.malisisdoorsreborn.hbm_fire_door.skin.black",
            "tooltip.malisisdoorsreborn.hbm_fire_door.skin.orange",
            "tooltip.malisisdoorsreborn.hbm_fire_door.skin.yellow",
            "tooltip.malisisdoorsreborn.hbm_fire_door.skin.trefoil"
    };

    public HbmFireDoorBlockItem(final Supplier<? extends Block> block,
                                final Properties properties,
                                final String tooltipKey) {
        super(block, properties, tooltipKey);
    }

    public static int skinIndexFromStack(@NotNull final ItemStack stack) {
        final CompoundTag tag = tagCopy(stack);
        if (tag == null || !tag.contains(SKIN_TAG)) return 0;
        return Math.floorMod(tag.getInt(SKIN_TAG), SKIN_COUNT);
    }

    public static ItemStack stackWithSkinRedstoneAccess(@NotNull final Item item,
                                                        final int skinIndex,
                                                        @NotNull final HbmDoorRedstoneMode redstoneMode,
                                                        @NotNull final DoorAccessLevel accessLevel) {
        final ItemStack stack = new ItemStack(item);
        setSkinIndex(stack, skinIndex);
        setRedstoneMode(stack, redstoneMode);
        setAccessLevel(stack, accessLevel);
        return stack;
    }


    public static ItemStack stackWithSkinPreviewCycle(@NotNull final Item item) {
        final ItemStack stack = new ItemStack(item);
        if (SKIN_COUNT > 1) {
            final CompoundTag tag = tagCopyOrCreate(stack);
            tag.putBoolean(SKIN_PREVIEW_CYCLE_TAG, true);
            writeTag(stack, tag);
        }
        return stack;
    }

    public static int skinIndexForRender(@NotNull final ItemStack stack) {
        if (hasSkinPreviewCycle(stack) && SKIN_COUNT > 1) {
            return (int) ((Util.getMillis() / 1000L) % SKIN_COUNT);
        }
        return skinIndexFromStack(stack);
    }

    private static boolean hasSkinPreviewCycle(@NotNull final ItemStack stack) {
        final CompoundTag tag = tagCopy(stack);
        return tag != null && tag.getBoolean(SKIN_PREVIEW_CYCLE_TAG);
    }

    private static void clearSkinPreviewCycle(@NotNull final ItemStack stack) {
        final CompoundTag tag = tagCopy(stack);
        if (tag == null || !tag.contains(SKIN_PREVIEW_CYCLE_TAG)) return;
        tag.remove(SKIN_PREVIEW_CYCLE_TAG);
        writeTag(stack, tag);
    }

    public static void setSkinIndex(@NotNull final ItemStack stack, final int skinIndex) {
        final int normalisedSkin = Math.floorMod(skinIndex, SKIN_COUNT);
        if (normalisedSkin == 0) {
            final CompoundTag tag = tagCopy(stack);
            if (tag != null) {
                tag.remove(SKIN_TAG);
                writeTag(stack, tag);
            }
            return;
        }
        final CompoundTag tag = tagCopyOrCreate(stack);
        tag.putInt(SKIN_TAG, normalisedSkin);
        writeTag(stack, tag);
    }

    public static Component skinName(final int skinIndex) {
        return Component.translatable(SKIN_TRANSLATION_KEYS[Math.floorMod(skinIndex, SKIN_TRANSLATION_KEYS.length)]);
    }

    public static HbmDoorRedstoneMode redstoneModeFromStack(@NotNull final ItemStack stack) {
        final CompoundTag tag = tagCopy(stack);
        if (tag == null || !tag.contains(REDSTONE_MODE_TAG)) return HbmDoorRedstoneMode.DEFAULT;
        return HbmDoorRedstoneMode.fromOrdinal(tag.getInt(REDSTONE_MODE_TAG));
    }

    public static void setRedstoneMode(@NotNull final ItemStack stack,
                                       @NotNull final HbmDoorRedstoneMode redstoneMode) {
        if (redstoneMode == HbmDoorRedstoneMode.DEFAULT) {
            final CompoundTag tag = tagCopy(stack);
            if (tag != null) {
                tag.remove(REDSTONE_MODE_TAG);
                writeTag(stack, tag);
            }
            return;
        }
        final CompoundTag tag = tagCopyOrCreate(stack);
        tag.putInt(REDSTONE_MODE_TAG, redstoneMode.ordinal());
        writeTag(stack, tag);
    }

    public static Component redstoneModeName(@NotNull final HbmDoorRedstoneMode redstoneMode) {
        return redstoneMode.displayName();
    }

    public static DoorAccessLevel accessLevelFromStack(@NotNull final ItemStack stack) {
        return DoorAccessHelper.accessLevelFromStack(stack);
    }

    public static void setAccessLevel(@NotNull final ItemStack stack,
                                      @NotNull final DoorAccessLevel accessLevel) {
        DoorAccessHelper.setAccessLevel(stack, accessLevel);
    }

    public static Component accessLevelName(@NotNull final DoorAccessLevel accessLevel) {
        return DoorAccessHelper.accessLevelName(accessLevel);
    }

    private static CompoundTag tagCopy(@NotNull final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    private static CompoundTag tagCopyOrCreate(@NotNull final ItemStack stack) {
        final CompoundTag tag = tagCopy(stack);
        return tag == null ? new CompoundTag() : tag;
    }

    private static void writeTag(@NotNull final ItemStack stack,
                                 @NotNull final CompoundTag tag) {
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }


    @Override
    public void inventoryTick(@NotNull final ItemStack stack,
                              @NotNull final Level level,
                              @NotNull final Entity entity,
                              final int slotId,
                              final boolean isSelected) {
        clearSkinPreviewCycle(stack);
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public void appendHoverText(@NotNull final ItemStack stack,
                                @NotNull final Item.TooltipContext context,
                                @NotNull final List<Component> tooltip,
                                @NotNull final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        final DoorAccessLevel accessLevel = accessLevelFromStack(stack);
        final MutableComponent redstoneTooltip = Component.translatable("tooltip.malisisdoorsreborn.label.redstone", redstoneModeName(redstoneModeFromStack(stack))).withStyle(ChatFormatting.WHITE);
        if (accessLevel != DoorAccessLevel.DEFAULT) {
            redstoneTooltip.append(Component.literal(" ")).append(Component.translatable("tooltip.malisisdoorsreborn.tooltip_suffix.overridden").withStyle(ChatFormatting.YELLOW));
        }

        tooltip.add(Component.translatable("tooltip.malisisdoorsreborn.label.skin", skinName(skinIndexFromStack(stack))).withStyle(ChatFormatting.WHITE));
        tooltip.add(redstoneTooltip);
        tooltip.add(Component.translatable("tooltip.malisisdoorsreborn.label.access", accessLevelName(accessLevel)).withStyle(ChatFormatting.WHITE));
    }

    @SuppressWarnings("removal")
    @Override
    public void initializeClient(@NotNull final Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HbmFireDoorItemRenderer renderer;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    final Minecraft minecraft = Minecraft.getInstance();
                    renderer = new HbmFireDoorItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(),
                            minecraft.getEntityModels()
                    );
                }
                return renderer;
            }
        });
    }
}
