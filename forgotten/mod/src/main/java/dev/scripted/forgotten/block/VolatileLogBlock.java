package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

/**
 * The trunk of a tree grown by a Bloom Creeper. It still remembers being a creeper: break it
 * and the whole tree goes off, and a forest of them goes off in a chain.
 */
public class VolatileLogBlock extends PillarBlock {
    public static final MapCodec<VolatileLogBlock> CODEC = createCodec(VolatileLogBlock::new);

    public VolatileLogBlock(Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<? extends PillarBlock> getCodec() {
        return CODEC;
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        super.afterBreak(world, player, pos, state, blockEntity, tool);
        if (world instanceof ServerWorld server) {
            server.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0f, World.ExplosionSourceType.BLOCK);
        }
    }

    @Override
    public void onDestroyedByExplosion(ServerWorld world, BlockPos pos, Explosion explosion) {
        super.onDestroyedByExplosion(world, pos, explosion);
        world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.5f, World.ExplosionSourceType.BLOCK);
    }
}
