package dev.mostlyharmless.malisisdoorsreborn.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public final class HbmBreakingOverlayHelper {

    private static final AABB DEFAULT_BOUNDS = new AABB(
            0.0D, 0.0D, 0.0D,
            1.0D, 1.0D, 1.0D
    );

    private HbmBreakingOverlayHelper() {
    }

    public static AABB fullDoorBounds(@NotNull final BlockGetter level,
                                      @NotNull final BlockPos rootPos,
                                      @NotNull final Direction facing,
                                      @NotNull final Block block,
                                      final int width,
                                      final int height,
                                      @NotNull final PartPosResolver partPosResolver) {
        AABB bounds = null;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final BlockPos partPos = partPosResolver.resolve(rootPos, facing, x, y);
                final BlockState partState = level.getBlockState(partPos);
                if (partState.getBlock() != block) continue;

                final double minX = partPos.getX() - rootPos.getX();
                final double minY = partPos.getY() - rootPos.getY();
                final double minZ = partPos.getZ() - rootPos.getZ();
                final AABB partBounds = new AABB(
                        minX, minY, minZ,
                        minX + 1.0D, minY + 1.0D, minZ + 1.0D
                );
                bounds = bounds == null ? partBounds : bounds.minmax(partBounds);
            }
        }
        return bounds == null ? DEFAULT_BOUNDS : bounds;
    }

    public static AABB fullBounds(final double width,
                                  final double height,
                                  final double depth) {
        return new AABB(0.0D, 0.0D, 0.0D, width, height, depth);
    }

    public static AABB centredSlab(@NotNull final AABB bounds,
                                   @NotNull final Direction facing,
                                   final double minDepth,
                                   final double maxDepth) {
        return facing.getAxis() == Direction.Axis.Z
                ? new AABB(bounds.minX, bounds.minY, minDepth, bounds.maxX, bounds.maxY, maxDepth)
                : new AABB(minDepth, bounds.minY, bounds.minZ, maxDepth, bounds.maxY, bounds.maxZ);
    }

    public static AABB directionalSlab(@NotNull final AABB bounds,
                                       @NotNull final Direction facing,
                                       final double thickness) {
        return switch (facing) {
            case NORTH -> new AABB(bounds.minX, bounds.minY, 0.0D, bounds.maxX, bounds.maxY, thickness);
            case SOUTH -> new AABB(bounds.minX, bounds.minY, 1.0D - thickness, bounds.maxX, bounds.maxY, 1.0D);
            case WEST -> new AABB(0.0D, bounds.minY, bounds.minZ, thickness, bounds.maxY, bounds.maxZ);
            case EAST -> new AABB(1.0D - thickness, bounds.minY, bounds.minZ, 1.0D, bounds.maxY, bounds.maxZ);
            default -> bounds;
        };
    }

    @FunctionalInterface
    public interface PartPosResolver {
        BlockPos resolve(BlockPos rootPos, Direction facing, int x, int y);
    }
}
