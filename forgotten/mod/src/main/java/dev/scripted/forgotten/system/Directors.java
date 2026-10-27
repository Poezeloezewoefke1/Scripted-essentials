package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.integrity.IntegrityModel;
import dev.scripted.forgotten.logic.paradox.CausalDebt;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.util.Scheduler;
import dev.scripted.forgotten.world.RegionMemory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.SplittableRandom;

/**
 * The tick loop. Each system runs on its own cadence so the per-tick cost stays small:
 * glitched mobs every tick (each acting every fifth), integrity and anomalies every two
 * seconds, slower world-scale processes every few seconds.
 */
public final class Directors {
    private static final SplittableRandom RANDOM = new SplittableRandom();

    private Directors() {
    }

    public static void tick(MinecraftServer server) {
        Scheduler.tick();
        long now = Reality.now(server);
        Glitches.tick(now);
        Puppets.tick(server);
        BioNodes.tick(now);
        Unraveling.tick(server, now);
        SunDogs.tick(now);
        Creatures.tick(now);
        Bosses.tick(server, now);
        Genesis.tick(server, now);
        Eaters.tick(server);
        Guilds.explosions(server);
        Guilds.tick(server, now);
        Architecture.tick(server, now);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Hooks.trail(player);
            Rifts.tickPlayer(player, now);
        }
        for (ServerWorld world : server.getWorlds()) {
            Nesting.dilate(world);
        }
        if (now % 10 == 0) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ObserverDirector.tick(Reality.world(player), player, now);
            }
        }
        if (now % 20 == 0) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                perPlayer(player, now);
            }
        }
        if (now % 40 == 0) {
            for (ServerWorld world : server.getWorlds()) {
                if (world.getPlayers().isEmpty()) {
                    continue;
                }
                integrity(world);
                for (ServerPlayerEntity player : world.getPlayers()) {
                    Anomalies.roll(world, player);
                    WakingDirector.tick(world, player, now);
                    DebtDirector.tick(world, player, now);
                    Draft.roll(world, player);
                    SunDogs.roll(world, player);
                    Creatures.roll(world, player, now);
                }
                Anomalies.thaw(world, now);
            }
        }
        if (now % 100 == 0) {
            FirstSigns.tick(server.getOverworld(), now);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                MemoryBleed.tick(Reality.world(player), player, now);
                Palimpsests.tick(Reality.world(player), player);
                Anachronisms.tick(Reality.world(player), player, now);
            }
        }
        StormDirector.tick(server, now);
    }

    static void perPlayer(ServerPlayerEntity player, long now) {
        PlayerMemory memory = Reality.player(player);
        ServerWorld world = Reality.world(player);
        Rifts.stackGravity(player);
        Rifts.checkLoops(world, player);
        Nesting.copyErrors(world, player);
        Puppets.applyHollowCurse(player);
        memory.debt = CausalDebt.afterTime(memory.debt, memory.lastEdit, now, 20);
        if (now % 600 == 0) {
            BlockPos pos = player.getBlockPos();
            memory.visit(Reality.place(world, pos.withY(pos.getY())));
            memory.observer.observe(ChunkPos.toLong(pos));
        }
        if (now - memory.lastSnapshot >= 24000 || memory.snapshots.isEmpty()) {
            NbtCompound snapshot = new NbtCompound();
            snapshot.putLong("tick", now);
            snapshot.put("items", Snapshots.write(player.getInventory(), world.getRegistryManager()));
            memory.snapshots.add(snapshot);
            while (memory.snapshots.size() > PlayerMemory.SNAPSHOT_LIMIT) {
                memory.snapshots.remove(0);
            }
            memory.lastSnapshot = now;
        }
    }

    /** Updates the integrity of the chunks around every player in a world. */
    static void integrity(ServerWorld world) {
        RegionMemory region = Reality.region(world);
        double lucidity = Reality.lucidity(world.getServer());
        int floor = Reality.floor(world.getServer());
        java.util.Set<Long> done = new java.util.HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            ChunkPos center = player.getChunkPos();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    long key = ChunkPos.toLong(center.x + dx, center.z + dz);
                    if (!done.add(key)) {
                        continue;
                    }
                    int current = region.integrity(world, key);
                    RegionMemory.ChunkRecord record = region.peek(key);
                    int paradox = record == null ? 0 : record.paradox;
                    int lowest = 100;
                    for (long n : new long[]{ChunkPos.toLong(center.x + dx + 1, center.z + dz), ChunkPos.toLong(center.x + dx - 1, center.z + dz),
                            ChunkPos.toLong(center.x + dx, center.z + dz + 1), ChunkPos.toLong(center.x + dx, center.z + dz - 1)}) {
                        lowest = Math.min(lowest, region.integrity(world, n));
                    }
                    int next = IntegrityModel.step(current, RegionMemory.baseline(world, key), (int) lucidity, paradox, lowest,
                            region.anchored(key), floor, RANDOM);
                    if (record != null && record.paradox != 0) {
                        record.paradox = 0;
                        region.markDirty();
                    }
                    if (next != current) {
                        region.setIntegrity(key, next);
                    }
                }
            }
        }
    }
}
