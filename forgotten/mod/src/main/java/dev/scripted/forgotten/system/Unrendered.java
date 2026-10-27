package dev.scripted.forgotten.system;

import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

/**
 * The Unrendered: a creature that only exists where nobody is looking - in the chunks at the
 * edge of what is loaded. It is not an entity at all. You never see it, only what it does:
 * footprints crossing the ground, plants and leaves that are suddenly gone, and a line in chat.
 */
public final class Unrendered {
    private Unrendered() {
    }

    public static void stir(ServerWorld world, ServerPlayerEntity player, Random random) {
        double angle = random.nextDouble() * Math.PI * 2;
        Vec3d dir = new Vec3d(Math.cos(angle), 0, Math.sin(angle));
        Vec3d start = player.getEntityPos().add(dir.multiply(40));
        // Footprints walking toward the player, then stopping just short.
        for (int i = 0; i < 12; i++) {
            Vec3d p = start.subtract(dir.multiply(i * 2.5));
            BlockPos ground = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, BlockPos.ofFloored(p));
            if (!world.isChunkLoaded(ground)) {
                continue;
            }
            BlockState below = world.getBlockState(ground.down());
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, below), p.x, ground.getY() + 0.05, p.z, 6, 0.15, 0, 0.15, 0);
            // What it walks through stops being there.
            BlockState at = world.getBlockState(ground);
            if (at.isIn(BlockTags.FLOWERS) || at.isIn(BlockTags.REPLACEABLE_BY_TREES) || at.isOf(Blocks.SHORT_GRASS)) {
                world.setBlockState(ground, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
        if (random.nextInt(4) == 0) {
            world.getServer().getPlayerManager().broadcast(Text.literal("Player Unrendered joined the game").formatted(Formatting.YELLOW), false);
        }
        if (random.nextInt(10) == 0) {
            Block.dropStack(world, BlockPos.ofFloored(start.subtract(dir.multiply(30))), new ItemStack(ModItems.concept(Concept.ABSENCE)));
            Texts.bar(player, Texts.world("Something stopped walking, just out of sight."));
        }
    }
}
