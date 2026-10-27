package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.TransparentBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Marrowglass, from the Underdream: a building material that knows what shape it should be.
 * It closes one-block gaps between pieces of itself and extends short lines it is part of, so a
 * rough sketch of a wall becomes a wall.
 */
public class MarrowglassBlock extends TransparentBlock {
    public static final MapCodec<MarrowglassBlock> CODEC = createCodec(MarrowglassBlock::new);
    public static final int MAX_RUN = 7;

    public MarrowglassBlock(Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<? extends TransparentBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Direction dir = Direction.random(random);
        BlockPos gap = pos.offset(dir);
        if (!world.getBlockState(gap).isAir()) {
            return;
        }
        // Close a one-block gap.
        if (world.getBlockState(gap.offset(dir)).isOf(this)) {
            world.setBlockState(gap, getDefaultState());
            return;
        }
        // Extend a short line of itself in the direction it is already going.
        int run = 0;
        BlockPos back = pos;
        while (run < MAX_RUN && world.getBlockState(back.offset(dir.getOpposite())).isOf(this)) {
            back = back.offset(dir.getOpposite());
            run++;
        }
        if (run >= 1 && run < MAX_RUN - 1) {
            world.setBlockState(gap, getDefaultState());
        }
    }
}
