package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;

/**
 * Sleep matters in a world that is itself asleep. Where you sleep often, Lullaby ore forms in
 * the rock below. In a forgetful place, you may wake to the sun rising in the west. And
 * sometimes you dream about the thing that watches you.
 */
public final class Waking {
    public static final int SLEEPS_FOR_LULLABY = 3;

    private Waking() {
    }

    public static void onWake(ServerWorld world, ServerPlayerEntity player, BlockPos bed) {
        RegionMemory region = Reality.region(world);
        RegionMemory.ChunkRecord chunk = region.chunk(ChunkPos.toLong(bed));
        chunk.slept++;
        region.markDirty();
        Random random = world.getRandom();
        if (chunk.slept >= SLEEPS_FOR_LULLABY) {
            seedLullaby(world, bed, random, 1 + chunk.slept / SLEEPS_FOR_LULLABY);
        }
        IntegrityStage stage = Reality.stage(world, bed);
        if (stage.atLeast(IntegrityStage.DEJA_VU) && random.nextInt(4) == 0 && world.isDay()) {
            long day = world.getTimeOfDay() / 24000L;
            world.setTimeOfDay(day * 24000L + 11500);
            Texts.tell(player, Texts.world("You wake in the same bed. The sun is low in the west."));
        }
        PlayerMemory memory = Reality.player(player);
        if (memory.observer.appearances() > 0 && random.nextInt(3) == 0) {
            Texts.tell(player, Texts.world("You dreamed of a tall shape standing where you sleep. It was not facing you. It was facing the same way you were."));
        }
    }

    static void seedLullaby(ServerWorld world, BlockPos bed, Random random, int veins) {
        for (int v = 0; v < veins; v++) {
            BlockPos start = bed.add(random.nextInt(16) - 8, -(6 + random.nextInt(24)), random.nextInt(16) - 8);
            for (int i = 0; i < 5; i++) {
                BlockPos p = start.add(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
                BlockState s = world.getBlockState(p);
                if (s.isIn(BlockTags.STONE_ORE_REPLACEABLES)) {
                    world.setBlockState(p, ModBlocks.LULLABY_ORE.getDefaultState());
                } else if (s.isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
                    world.setBlockState(p, ModBlocks.DEEPSLATE_LULLABY_ORE.getDefaultState());
                }
            }
        }
    }
}
