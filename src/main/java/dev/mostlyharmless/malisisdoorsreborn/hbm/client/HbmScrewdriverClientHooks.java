package dev.mostlyharmless.malisisdoorsreborn.hbm.client;

import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmScrewdriverItem;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmScrewdriverMode;
import dev.mostlyharmless.malisisdoorsreborn.network.MdrNetwork;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MdrDefaults.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HbmScrewdriverClientHooks {

    private HbmScrewdriverClientHooks() {}

    @SubscribeEvent
    public static void onMouseScroll(final InputEvent.MouseScrollingEvent event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) return;
        final Player player = minecraft.player;
        if (player == null) return;
        if (!player.isShiftKeyDown()) return;
        final InteractionHand hand = screwdriverHand(player);
        if (hand == null) return;

        final ItemStack stack = player.getItemInHand(hand);
        final HbmScrewdriverMode mode = HbmScrewdriverItem.cycleMode(stack);
        HbmScrewdriverItem.showMode(player, mode);
        MdrNetwork.sendHbmScrewdriverModeCycle(hand);
        event.setCanceled(true);
    }

    private static InteractionHand screwdriverHand(final Player player) {
        if (player.getItemInHand(InteractionHand.MAIN_HAND).is(MdrItems.HBM_SCREWDRIVER.get())) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getItemInHand(InteractionHand.OFF_HAND).is(MdrItems.HBM_SCREWDRIVER.get())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }
}
