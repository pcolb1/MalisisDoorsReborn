package dev.mostlyharmless.malisisdoorsreborn.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class HbmCollisionHelper {

    private HbmCollisionHelper() {}

    static Boolean rootOpen(final BlockGetter level, final BlockPos pos, final Block expectedBlock) {
        final BlockState root = level.getBlockState(pos);
        //noinspection ConstantValue
        if (root == null || root.getBlock() != expectedBlock) return null;
        return root.getValue(BlockStateProperties.OPEN);
    }
}
