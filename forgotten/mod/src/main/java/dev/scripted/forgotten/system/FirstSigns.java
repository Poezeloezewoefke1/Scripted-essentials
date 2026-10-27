package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Scheduler;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;


import java.util.ArrayList;
import java.util.List;

/**
 * The first impossible thing: a perfect cube, 32 blocks wide, hovering above the ground, made
 * of every kind of block arranged at random. No entrance. When you come close, every sound
 * stops, a line appears, and the cube is gone. The mod has officially begun.
 */
public final class FirstSigns {
    public static final int SIZE = 32;
    public static final double REVEAL_DISTANCE = 30;
    public static final long LIFETIME = 3600;

    private static final BlockState[] PALETTE = {
            Blocks.GRASS_BLOCK.getDefaultState(), Blocks.STONE.getDefaultState(), Blocks.OAK_PLANKS.getDefaultState(),
            Blocks.OAK_LOG.getDefaultState(), Blocks.SAND.getDefaultState(), Blocks.BLUE_STAINED_GLASS.getDefaultState(),
            Blocks.COBBLESTONE.getDefaultState(), Blocks.BRICKS.getDefaultState(), Blocks.DIRT.getDefaultState(),
            Blocks.SNOW_BLOCK.getDefaultState(), Blocks.NETHERRACK.getDefaultState(), Blocks.END_STONE.getDefaultState(),
            Blocks.PRISMARINE.getDefaultState(), Blocks.MOSS_BLOCK.getDefaultState(), Blocks.AMETHYST_BLOCK.getDefaultState(),
            Blocks.GOLD_BLOCK.getDefaultState(), Blocks.WHITE_WOOL.getDefaultState(), Blocks.TERRACOTTA.getDefaultState(),
            Blocks.DEEPSLATE.getDefaultState(), Blocks.BOOKSHELF.getDefaultState(), Blocks.OBSIDIAN.getDefaultState(),
            Blocks.MAGMA_BLOCK.getDefaultState(), Blocks.SCULK.getDefaultState(), Blocks.HONEY_BLOCK.getDefaultState()};

    private static BlockPos cube;
    private static long cubeBorn;
    private static final List<BlockPos> PLACED = new ArrayList<>();

    private FirstSigns() {
    }

    public static boolean active() {
        return cube != null;
    }

    public static void tick(ServerWorld overworld, long now) {
        WorldMemory memory = Reality.memory(overworld.getServer());
        if (cube != null) {
            Vec3d center = Vec3d.ofCenter(cube).add(SIZE / 2.0, SIZE / 2.0, SIZE / 2.0);
            boolean revealed = false;
            for (ServerPlayerEntity player : overworld.getPlayers()) {
                if (player.getEntityPos().distanceTo(center) < REVEAL_DISTANCE + SIZE / 2.0) {
                    reveal(overworld, player);
                    revealed = true;
                }
            }
            if (revealed || now - cubeBorn > LIFETIME) {
                if (revealed) {
                    memory.setFlag("first_cube");
                }
                vanish(overworld);
            }
            return;
        }
        // Waits until the world has begun to stir, then shows itself rarely: the first time is
        // the start of everything, later times are for players who missed it.
        if (memory.lucidity.lucidity() < 10) {
            return;
        }
        boolean first = !memory.flag("first_cube");
        if (overworld.getRandom().nextInt(first ? 60 : 2000) != 0) {
            return;
        }
        List<ServerPlayerEntity> players = overworld.getPlayers(p -> !p.isSpectator() && overworld.isSkyVisible(p.getBlockPos())
                && (first || !Reality.player(p).flag("saw_cube")));
        if (players.isEmpty()) {
            return;
        }
        ServerPlayerEntity player = players.get(overworld.getRandom().nextInt(players.size()));
        spawn(overworld, player, now);
    }

    public static void spawn(ServerWorld world, ServerPlayerEntity player, long now) {
        Vec3d look = player.getRotationVector().multiply(1, 0, 1).normalize();
        BlockPos ahead = BlockPos.ofFloored(player.getEntityPos().add(look.multiply(70)));
        BlockPos ground = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, ahead);
        BlockPos corner = ground.add(-SIZE / 2, 16, -SIZE / 2);
        if (corner.getY() + SIZE >= world.getTopYInclusive()) {
            return;
        }
        cube = corner;
        cubeBorn = now;
        PLACED.clear();
        var random = world.getRandom();
        // Built one layer per tick so it never stalls the server.
        for (int y = 0; y < SIZE; y++) {
            int layer = y;
            Scheduler.after(1 + y, () -> {
                if (cube == null) {
                    return;
                }
                for (int x = 0; x < SIZE; x++) {
                    for (int z = 0; z < SIZE; z++) {
                        boolean shell = layer == 0 || layer == SIZE - 1 || x == 0 || x == SIZE - 1 || z == 0 || z == SIZE - 1;
                        if (!shell) {
                            continue;
                        }
                        BlockPos p = corner.add(x, layer, z);
                        if (world.getBlockState(p).isAir()) {
                            world.setBlockState(p, PALETTE[random.nextInt(PALETTE.length)], Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                            PLACED.add(p);
                        }
                    }
                }
            });
        }
    }

    static void reveal(ServerWorld world, ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new StopSoundS2CPacket(null, null));
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 80, 30));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("MEMORY FRAGMENT DISCOVERED").formatted(Formatting.LIGHT_PURPLE)));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("THE FIRST WORLD").formatted(Formatting.GRAY, Formatting.ITALIC)));
        if (Reality.player(player).setFlag("saw_cube")) {
            ItemStack fragment = new ItemStack(ModItems.MEMORY_FRAGMENT_FIRST_WORLD);
            if (!player.giveItemStack(fragment)) {
                player.dropItem(fragment, false);
            }
            Reality.insight(player, Insight.MEMORY_FRAGMENT);
            Reality.advance(player, "first_world_fragment");
            Texts.tell(player, Texts.world("Every sound in the world stops. Then the cube is gone."));
        }
    }

    static void vanish(ServerWorld world) {
        for (BlockPos p : PLACED) {
            world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }
        PLACED.clear();
        cube = null;
    }

    public static void reset() {
        cube = null;
        PLACED.clear();
    }

}
