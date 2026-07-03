package dev.mostlyharmless.malisisdoorsreborn.hbm.block;

import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmScrewdriverMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public interface HbmScrewdriverDoorTarget {

    @NotNull InteractionResult applyHbmScrewdriverMode(@NotNull Level level,
                                                       @NotNull BlockPos pos,
                                                       @NotNull BlockState state,
                                                       @NotNull Player player,
                                                       @NotNull HbmScrewdriverMode mode);
}
