package dev.scripted.forgotten;

import dev.scripted.forgotten.logic.chronicle.ChronicleEntry;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.genesis.Ending;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.logic.paradox.ArchitectRank;
import dev.scripted.forgotten.logic.paradox.CausalDebt;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.logic.waking.LucidityPhase;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The one place every system goes through to read or change the state of reality. Keeping
 * this narrow is what lets the systems interact: a Paradox Engine edit, a Glitch Grenade and a
 * Reality Storm all lower integrity the same way, so everything that reacts to integrity reacts
 * to all of them.
 */
public final class Reality {
    private Reality() {
    }

    /** The world a player is in. */
    public static ServerWorld world(ServerPlayerEntity player) {
        return (ServerWorld) player.getEntityWorld();
    }

    public static WorldMemory memory(MinecraftServer server) {
        return WorldMemory.get(server);
    }

    public static RegionMemory region(ServerWorld world) {
        return RegionMemory.get(world);
    }

    public static PlayerMemory player(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(ModAttachments.PLAYER);
    }

    /** World time on the Overworld: the clock everything is measured against. */
    public static long now(MinecraftServer server) {
        return server.getOverworld().getTime();
    }

    public static double lucidity(MinecraftServer server) {
        return memory(server).lucidity.lucidity();
    }

    public static LucidityPhase phase(MinecraftServer server) {
        return memory(server).lucidity.phase();
    }

    public static boolean storming(MinecraftServer server) {
        return memory(server).storming(now(server));
    }

    public static Ending ending(MinecraftServer server) {
        return memory(server).ending();
    }

    /** Lowest integrity any chunk can fall to, set by the endings. */
    public static int floor(MinecraftServer server) {
        Ending e = ending(server);
        if (e == Ending.PRESERVE) {
            return 85;
        }
        if (memory(server).law(new dev.scripted.forgotten.logic.genesis.Law(
                dev.scripted.forgotten.logic.genesis.LawSubject.WORLD, dev.scripted.forgotten.logic.concept.Concept.IDENTITY))) {
            return dev.scripted.forgotten.logic.integrity.IntegrityStage.CONTRADICTION.floor();
        }
        return 0;
    }

    public static int integrity(ServerWorld world, BlockPos pos) {
        int value = region(world).integrity(world, pos);
        return Math.max(value, floor(world.getServer()));
    }

    public static IntegrityStage stage(ServerWorld world, BlockPos pos) {
        return IntegrityStage.of(integrity(world, pos));
    }

    /** Applies paradox at a place: it will cost the chunk that much integrity. */
    public static void paradox(ServerWorld world, BlockPos pos, int amount) {
        if (amount > 0) {
            region(world).addParadox(pos, amount);
        }
    }

    /** Restores integrity directly (Patch Notes, the Stitcher, Mender actions). */
    public static void mend(ServerWorld world, BlockPos pos, int amount) {
        RegionMemory region = region(world);
        long key = net.minecraft.util.math.ChunkPos.toLong(pos);
        region.setIntegrity(key, Math.min(region.integrity(world, key) + amount, IntegrityStage.MAX));
    }

    public static void setIntegrity(ServerWorld world, BlockPos pos, int value) {
        region(world).setIntegrity(net.minecraft.util.math.ChunkPos.toLong(pos), value);
    }

    /** Charges causal debt for contradicting reality. The End's Finality makes it dearer. */
    public static void debt(ServerPlayerEntity player, double amount) {
        if (amount <= 0) {
            return;
        }
        ServerWorld world = Reality.world(player);
        double cost = amount * CausalDebt.finalityMultiplier(world.getRegistryKey() == World.END)
                * CausalDebt.endingMultiplier(ending(world.getServer()) == Ending.PRESERVE);
        player(player).owe(cost, now(world.getServer()));
    }

    public static void insight(ServerPlayerEntity player, Insight kind) {
        ArchitectRank next = player(player).gain(kind, now(Reality.world(player).getServer()));
        if (next != null) {
            Texts.tell(player, Texts.discovery("ARCHITECT RANK " + next.level() + ": " + next.title().toUpperCase()));
            Texts.tell(player, Texts.world(next.power()));
            Reality.world(player).playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.8f, 1.4f);
            advance(player, "rank_" + next.level());
        }
    }

    public static ArchitectRank rank(ServerPlayerEntity player) {
        return player(player).rank(now(Reality.world(player).getServer()));
    }

    /** Records a significant event in the world's chronicle; it also wakes the world. */
    public static void chronicle(ServerWorld world, ChronicleEvent event, BlockPos pos, String actor, String detail) {
        MinecraftServer server = world.getServer();
        memory(server).record(new ChronicleEntry(event, now(server), world.getRegistryKey().getValue().toString(),
                pos.getX(), pos.getY(), pos.getZ(), actor, detail), now(server));
    }

    /** Counts an event too small to keep individually. */
    public static void count(ServerWorld world, ChronicleEvent event, String subject) {
        MinecraftServer server = world.getServer();
        memory(server).count(event, subject, now(server));
    }

    /** Grants one of the mod's advancements (they use impossible criteria named "done"). */
    public static void advance(ServerPlayerEntity player, String name) {
        MinecraftServer server = Reality.world(player).getServer();
        AdvancementEntry entry = server.getAdvancementLoader().get(Forgotten.id(name));
        if (entry != null) {
            player.getAdvancementTracker().grantCriterion(entry, "done");
        }
    }

    public static String place(ServerWorld world, BlockPos pos) {
        return world.getRegistryKey().getValue() + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }
}
