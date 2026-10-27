package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.genesis.Ending;
import dev.scripted.forgotten.logic.genesis.Palimpsest;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import dev.scripted.forgotten.world.WorldMemory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Memory that survives the world. When a run ends, the world writes down where its players
 * lived, what that place was made of, and what they said, into a file outside any one save.
 * Worlds created later on the same installation read it and grow echoes of the old run: a
 * half-deleted foundation at the same coordinates, creatures repeating old words.
 */
public final class Palimpsests {
    private static Palimpsest loaded = new Palimpsest();

    private Palimpsests() {
    }

    public static Path file() {
        return FabricLoader.getInstance().getGameDir().resolve("forgotten").resolve("palimpsest.txt");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) {
            loaded = new Palimpsest();
            return;
        }
        try (Reader r = Files.newBufferedReader(f)) {
            loaded = Palimpsest.read(r);
            Forgotten.LOG.info("The world remembers {} earlier run(s)", loaded.runs().size());
        } catch (IOException e) {
            Forgotten.LOG.warn("Could not read the palimpsest", e);
            loaded = new Palimpsest();
        }
    }

    public static Palimpsest loaded() {
        return loaded;
    }

    /** Writes this run into the palimpsest. Called by every ending. */
    public static void record(MinecraftServer server, Ending ending) {
        WorldMemory memory = Reality.memory(server);
        List<String> players = new ArrayList<>();
        List<Palimpsest.Echo> echoes = new ArrayList<>();
        ServerWorld overworld = server.getOverworld();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            players.add(p.getName().getString());
            long bed = Reality.player(p).bed;
            BlockPos home = bed != Long.MIN_VALUE ? BlockPos.fromLong(bed) : p.getBlockPos();
            if (p.getEntityWorld() == overworld || bed != Long.MIN_VALUE) {
                echoes.add(snapshot(overworld, home));
            }
        }
        String worldName = server.getSavePath(WorldSavePath.ROOT).normalize().getFileName().toString();
        Palimpsest p = new Palimpsest();
        for (Palimpsest.Run old : loaded.runs()) {
            p.add(old);
        }
        p.add(new Palimpsest.Run(worldName, ending.name(), players, echoes, memory.lines));
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file())) {
                p.write(w);
            }
            loaded = p;
        } catch (IOException e) {
            Forgotten.LOG.warn("Could not write the palimpsest", e);
        }
    }

    /** What a player built around a place: their placed blocks within a 16-block box. */
    static Palimpsest.Echo snapshot(ServerWorld world, BlockPos center) {
        RegionMemory region = Reality.region(world);
        List<String> cells = new ArrayList<>();
        for (BlockPos p : BlockPos.iterate(center.add(-8, -3, -8), center.add(8, 8, 8))) {
            if (region.wasPlaced(p)) {
                BlockState s = world.getBlockState(p);
                if (!s.isAir()) {
                    cells.add((p.getX() - center.getX()) + "," + (p.getY() - center.getY()) + "," + (p.getZ() - center.getZ())
                            + "=" + Registries.BLOCK.getId(s.getBlock()));
                }
            }
        }
        return new Palimpsest.Echo(center.getX(), center.getY(), center.getZ(), cells);
    }

    /** Every five seconds per player: grow echoes of earlier runs near them. */
    public static void tick(ServerWorld world, ServerPlayerEntity player) {
        if (loaded.isEmpty() || world.getRegistryKey() != World.OVERWORLD) {
            return;
        }
        WorldMemory memory = Reality.memory(world.getServer());
        List<Palimpsest.Run> runs = loaded.runs();
        for (int r = 0; r < runs.size(); r++) {
            List<Palimpsest.Echo> echoes = runs.get(r).echoes();
            for (int e = 0; e < echoes.size(); e++) {
                Palimpsest.Echo echo = echoes.get(e);
                double dx = player.getX() - echo.x();
                double dz = player.getZ() - echo.z();
                if (dx * dx + dz * dz > 64 * 64 || !world.isChunkLoaded(ChunkPos.toLong(echo.x() >> 4, echo.z() >> 4))) {
                    continue;
                }
                if (memory.setFlag("palimpsest_" + r + "_" + e)) {
                    grow(world, echo, world.getRandom());
                    Texts.tell(player, Texts.world("Someone built here once. Not in this world. In the one before it."));
                }
            }
        }
    }

    static void grow(ServerWorld world, Palimpsest.Echo echo, Random random) {
        BlockPos origin = new BlockPos(echo.x(), echo.y(), echo.z());
        for (String cell : echo.blocks()) {
            String[] kv = cell.split("=");
            String[] xyz = kv[0].split(",");
            if (random.nextFloat() > 0.6f) {
                continue;
            }
            BlockPos p = origin.add(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
            BlockState here = world.getBlockState(p);
            if (!here.isAir() && !here.isReplaceable()) {
                continue;
            }
            Identifier id = Identifier.tryParse(kv[1]);
            BlockState state = switch (random.nextInt(4)) {
                case 0 -> ModBlocks.MEMORY_BLOCK.getDefaultState();
                case 1 -> Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
                default -> id != null && Registries.BLOCK.containsId(id) ? Registries.BLOCK.get(id).getDefaultState() : Blocks.COBWEB.getDefaultState();
            };
            world.setBlockState(p, state, Block.NOTIFY_LISTENERS);
        }
    }

    /** A line someone said in an earlier run, for glitched creatures to repeat. */
    public static String randomLine(Random random) {
        List<String> lines = loaded.allLines();
        return lines.isEmpty() ? null : lines.get(random.nextInt(lines.size()));
    }

    public static List<String> oldNames() {
        return loaded.allPlayers();
    }
}
