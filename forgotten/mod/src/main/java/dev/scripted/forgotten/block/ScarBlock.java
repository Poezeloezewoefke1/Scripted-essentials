package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * The Nether remembers pain. Where something violent happened, the ground scars, and the scar
 * keeps replaying the event: smoke, ash, the sound of a blast that ended long ago.
 */
public class ScarBlock extends Block {
    public static final MapCodec<ScarBlock> CODEC = createCodec(ScarBlock::new);

    public ScarBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        world.spawnParticles(ParticleTypes.ASH, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.01);
        if (random.nextInt(12) == 0) {
            world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.02);
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.AMBIENT, 0.4f, 0.5f);
        }
    }
}
