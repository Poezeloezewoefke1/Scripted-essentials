package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import dev.scripted.forgotten.system.Rifts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** A hole where an Infinite Loop fell through the world. Step in to reach the Stack, or to leave it. */
public class RiftBlock extends Block {
    public static final MapCodec<RiftBlock> CODEC = createCodec(RiftBlock::new);

    public RiftBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity,
                                     EntityCollisionHandler handler, boolean intersects) {
        if (world instanceof ServerWorld && entity instanceof ServerPlayerEntity player) {
            Rifts.enterRift(player, pos);
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        world.addParticleClient(ParticleTypes.REVERSE_PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                pos.getZ() + random.nextDouble(), 0.0, 0.05, 0.0);
    }
}
