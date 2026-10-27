package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

/**
 * The Possibility Garden. A seed of an unrealised possibility grows into one of the things it
 * might have been: a tree whose crown reaches into other dimensions, a seam of ore, more seeds,
 * or a single bloom of pure Possibility.
 */
public class PossibilitySproutBlock extends Block {
    public static final MapCodec<PossibilitySproutBlock> CODEC = createCodec(PossibilitySproutBlock::new);
    public static final IntProperty AGE = IntProperty.of("age", 0, 3);
    private static final VoxelShape SHAPE = Block.createCuboidShape(4, 0, 4, 12, 10, 12);

    public PossibilitySproutBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(AGE, 0));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.down());
        return below.isOf(Blocks.FARMLAND) || below.isOf(Blocks.DIRT) || below.isOf(Blocks.GRASS_BLOCK) || below.isOf(Blocks.MOSS_BLOCK);
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int age = state.get(AGE);
        if (age < 3) {
            world.setBlockState(pos, state.with(AGE, age + 1), Block.NOTIFY_LISTENERS);
            return;
        }
        realise(world, pos, random);
    }

    /** The possibility decides what it was. */
    public static void realise(ServerWorld world, BlockPos pos, Random random) {
        world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.05);
        switch (random.nextInt(4)) {
            case 0 -> {
                // A tree whose branches extend into nearby dimensions.
                for (int y = 0; y < 5; y++) {
                    world.setBlockState(pos.up(y), Blocks.OAK_LOG.getDefaultState(), Block.NOTIFY_ALL);
                }
                BlockState[] crown = {Blocks.OAK_LEAVES.getDefaultState(), Blocks.END_STONE.getDefaultState(),
                        ModBlocks.MARROWGLASS.getDefaultState(), Blocks.NETHER_WART_BLOCK.getDefaultState()};
                for (BlockPos p : BlockPos.iterate(pos.add(-2, 4, -2), pos.add(2, 6, 2))) {
                    if (world.getBlockState(p).isAir() && random.nextInt(4) > 0) {
                        world.setBlockState(p.toImmutable(), crown[random.nextInt(crown.length)], Block.NOTIFY_ALL);
                    }
                }
            }
            case 1 -> {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                BlockState ore = new BlockState[]{Blocks.DIAMOND_ORE.getDefaultState(), Blocks.EMERALD_ORE.getDefaultState(),
                        Blocks.LAPIS_ORE.getDefaultState()}[random.nextInt(3)];
                world.setBlockState(pos.down(), ore, Block.NOTIFY_ALL);
            }
            case 2 -> {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                Block.dropStack(world, pos, new ItemStack(ModItems.POSSIBILITY_SEED, 2 + random.nextInt(2)));
            }
            default -> {
                world.setBlockState(pos, Blocks.ALLIUM.getDefaultState(), Block.NOTIFY_ALL);
                if (random.nextInt(4) == 0) {
                    Block.dropStack(world, pos, new ItemStack(ModItems.concept(Concept.POSSIBILITY)));
                }
            }
        }
    }
}
