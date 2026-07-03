package dev.mostlyharmless.malisisdoorsreborn.hbm.item;

import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmScrewdriverDoorTarget;
import dev.mostlyharmless.malisisdoorsreborn.item.TooltipItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class HbmScrewdriverItem extends TooltipItem {

    private static final String MODE_TAG = "Mode";

    public HbmScrewdriverItem(final Properties props, final String tooltipKey) {
        super(props, tooltipKey);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull final UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        final Level level = context.getLevel();
        final BlockState clickedState = level.getBlockState(context.getClickedPos());
        if (!(clickedState.getBlock() instanceof final HbmScrewdriverDoorTarget target)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        return target.applyHbmScrewdriverMode(
                level,
                context.getClickedPos(),
                clickedState,
                player,
                modeFromStack(context.getItemInHand())
        );
    }

    public static @NotNull HbmScrewdriverMode modeFromStack(@NotNull final net.minecraft.world.item.ItemStack stack) {
        final CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(MODE_TAG)) return HbmScrewdriverMode.CYCLE_DOOR_SKIN;
        return HbmScrewdriverMode.values()[Math.floorMod(tag.getInt(MODE_TAG), HbmScrewdriverMode.values().length)];
    }

    public static @NotNull HbmScrewdriverMode cycleMode(@NotNull final net.minecraft.world.item.ItemStack stack) {
        final HbmScrewdriverMode mode = modeFromStack(stack).next();
        stack.getOrCreateTag().putInt(MODE_TAG, mode.ordinal());
        return mode;
    }

    public static void showMode(@NotNull final Player player,
                                @NotNull final HbmScrewdriverMode mode) {
        player.displayClientMessage(mode.actionbarComponent(), true);
    }
}
