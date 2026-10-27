package dev.scripted.forgotten.world;

import com.mojang.serialization.Codec;
import dev.scripted.forgotten.logic.chronicle.Chronicle;
import dev.scripted.forgotten.logic.chronicle.ChronicleEntry;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.genesis.Ending;
import dev.scripted.forgotten.logic.genesis.GenesisRule;
import dev.scripted.forgotten.logic.genesis.Law;
import dev.scripted.forgotten.logic.storm.StormKind;
import dev.scripted.forgotten.logic.waking.LucidityLedger;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everything the world remembers about itself as a whole: how awake it is, its chronicle, the
 * rules and laws it has been given, where its Frame Anchors are, how it ended. One per server,
 * stored with the Overworld.
 */
public final class WorldMemory extends PersistentState {
    public static final Codec<WorldMemory> CODEC = NbtCompound.CODEC.xmap(WorldMemory::fromNbt, WorldMemory::toNbt);
    public static final PersistentStateType<WorldMemory> TYPE =
            new PersistentStateType<>("forgotten_world_memory", WorldMemory::new, CODEC, null);

    public final LucidityLedger lucidity;
    public final Chronicle chronicle;
    private final Set<String> flags = new LinkedHashSet<>();
    private final EnumSet<GenesisRule> rules = EnumSet.noneOf(GenesisRule.class);
    private final Set<String> laws = new LinkedHashSet<>();
    private Ending ending;
    private UUID author;
    /** World time the next Reality Storm begins, or 0 when not yet scheduled. */
    public long nextStorm;
    /** World time the current storm ends; 0 when calm. */
    public long stormEnds;
    public final List<StormKind> storm = new ArrayList<>();
    /** Frame Anchors by id. */
    public final Map<Integer, FrameRecord> frames = new HashMap<>();
    public int nextFrameId = 1;
    /** Where the Archive assembled itself, or null. */
    public long archivePos = Long.MIN_VALUE;
    public String archiveDimension = "";
    /** Recent things players said, kept for the palimpsest. */
    public final List<String> lines = new ArrayList<>();

    public WorldMemory() {
        this.lucidity = new LucidityLedger();
        this.chronicle = new Chronicle();
    }

    private WorldMemory(LucidityLedger lucidity, Chronicle chronicle) {
        this.lucidity = lucidity;
        this.chronicle = chronicle;
    }

    public static WorldMemory get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        return overworld.getPersistentStateManager().getOrCreate(TYPE);
    }

    // ---- flags, rules, laws ----

    public boolean flag(String name) {
        return flags.contains(name);
    }

    /** Sets a flag. Returns true if it was not already set: the first time something happens. */
    public boolean setFlag(String name) {
        boolean added = flags.add(name);
        if (added) {
            markDirty();
        }
        return added;
    }

    public void clearFlag(String name) {
        if (flags.remove(name)) {
            markDirty();
        }
    }

    public Set<String> flags() {
        return Set.copyOf(flags);
    }

    public boolean rule(GenesisRule rule) {
        return rules.contains(rule);
    }

    public void setRule(GenesisRule rule, boolean on) {
        if (on ? rules.add(rule) : rules.remove(rule)) {
            markDirty();
        }
    }

    public Set<GenesisRule> rules() {
        return EnumSet.copyOf(rules.isEmpty() ? EnumSet.noneOf(GenesisRule.class) : rules);
    }

    public boolean law(Law law) {
        return laws.contains(law.key());
    }

    public boolean toggleLaw(Law law) {
        boolean on = !laws.remove(law.key());
        if (on) {
            laws.add(law.key());
        }
        markDirty();
        return on;
    }

    public List<Law> laws() {
        return laws.stream().map(Law::parse).toList();
    }

    public Ending ending() {
        return ending;
    }

    public UUID author() {
        return author;
    }

    public void end(Ending ending, UUID chooser) {
        this.ending = ending;
        if (ending == Ending.AUTHOR) {
            this.author = chooser;
        }
        markDirty();
    }

    public void resetEnding() {
        this.ending = null;
        this.author = null;
        markDirty();
    }

    // ---- history ----

    public void record(ChronicleEntry entry, long now) {
        chronicle.record(entry);
        lucidity.add(entry.event().pressure(), now);
        markDirty();
    }

    public void count(ChronicleEvent event, String subject, long now) {
        chronicle.count(event, subject);
        lucidity.add(event.pressure(), now);
        markDirty();
    }

    public void said(String line) {
        lines.add(line);
        while (lines.size() > 40) {
            lines.remove(0);
        }
        markDirty();
    }

    public boolean storming(long now) {
        return now < stormEnds;
    }

    // ---- persistence ----

    public record FrameRecord(int id, String dimension, long pos, int depth) {
    }

    private NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putDouble("pressure", lucidity.pressure());
        nbt.putDouble("penalty", lucidity.penalty());
        nbt.putLong("lockedUntil", lucidity.lockedUntil());
        nbt.putBoolean("eyesOpen", lucidity.eyesOpen());

        NbtCompound counters = new NbtCompound();
        chronicle.counters().forEach(counters::putLong);
        nbt.put("counters", counters);
        NbtList entries = new NbtList();
        for (ChronicleEntry e : chronicle.entries()) {
            NbtCompound c = new NbtCompound();
            c.putString("e", e.event().name());
            c.putLong("t", e.tick());
            c.putString("d", e.dimension());
            c.putInt("x", e.x());
            c.putInt("y", e.y());
            c.putInt("z", e.z());
            c.putString("a", e.actor());
            c.putString("s", e.detail());
            entries.add(c);
        }
        nbt.put("entries", entries);

        nbt.put("flags", strings(flags));
        nbt.put("rules", strings(rules.stream().map(Enum::name).toList()));
        nbt.put("laws", strings(laws));
        if (ending != null) {
            nbt.putString("ending", ending.name());
        }
        if (author != null) {
            nbt.putString("author", author.toString());
        }
        nbt.putLong("nextStorm", nextStorm);
        nbt.putLong("stormEnds", stormEnds);
        nbt.put("storm", strings(storm.stream().map(Enum::name).toList()));
        NbtList frameList = new NbtList();
        for (FrameRecord f : frames.values()) {
            NbtCompound c = new NbtCompound();
            c.putInt("id", f.id());
            c.putString("dim", f.dimension());
            c.putLong("pos", f.pos());
            c.putInt("depth", f.depth());
            frameList.add(c);
        }
        nbt.put("frames", frameList);
        nbt.putInt("nextFrameId", nextFrameId);
        nbt.putLong("archivePos", archivePos);
        nbt.putString("archiveDim", archiveDimension);
        nbt.put("lines", strings(lines));
        return nbt;
    }

    private static WorldMemory fromNbt(NbtCompound nbt) {
        LucidityLedger ledger = new LucidityLedger(nbt.getDouble("pressure", 0), nbt.getDouble("penalty", 0),
                nbt.getLong("lockedUntil", 0), nbt.getBoolean("eyesOpen", false));
        Chronicle chronicle = new Chronicle();
        NbtCompound counters = nbt.getCompoundOrEmpty("counters");
        for (String key : counters.getKeys()) {
            chronicle.restoreCounter(key, counters.getLong(key, 0));
        }
        for (NbtElement el : nbt.getListOrEmpty("entries")) {
            if (el instanceof NbtCompound c) {
                try {
                    chronicle.restoreEntry(new ChronicleEntry(ChronicleEvent.valueOf(c.getString("e", "")),
                            c.getLong("t", 0), c.getString("d", ""), c.getInt("x", 0), c.getInt("y", 0), c.getInt("z", 0),
                            c.getString("a", ""), c.getString("s", "")));
                } catch (IllegalArgumentException ignored) {
                    // An event kind from a newer version; skip it.
                }
            }
        }
        WorldMemory m = new WorldMemory(ledger, chronicle);
        m.flags.addAll(readStrings(nbt, "flags"));
        for (String r : readStrings(nbt, "rules")) {
            try {
                m.rules.add(GenesisRule.valueOf(r));
            } catch (IllegalArgumentException ignored) {
                // removed rule
            }
        }
        m.laws.addAll(readStrings(nbt, "laws"));
        String ending = nbt.getString("ending", "");
        if (!ending.isEmpty()) {
            m.ending = Ending.valueOf(ending);
        }
        String author = nbt.getString("author", "");
        if (!author.isEmpty()) {
            m.author = UUID.fromString(author);
        }
        m.nextStorm = nbt.getLong("nextStorm", 0);
        m.stormEnds = nbt.getLong("stormEnds", 0);
        for (String s : readStrings(nbt, "storm")) {
            m.storm.add(StormKind.valueOf(s));
        }
        for (NbtElement el : nbt.getListOrEmpty("frames")) {
            if (el instanceof NbtCompound c) {
                FrameRecord f = new FrameRecord(c.getInt("id", 0), c.getString("dim", ""), c.getLong("pos", 0), c.getInt("depth", 0));
                m.frames.put(f.id(), f);
            }
        }
        m.nextFrameId = nbt.getInt("nextFrameId", 1);
        m.archivePos = nbt.getLong("archivePos", Long.MIN_VALUE);
        m.archiveDimension = nbt.getString("archiveDim", "");
        m.lines.addAll(readStrings(nbt, "lines"));
        return m;
    }

    static NbtList strings(Iterable<String> values) {
        NbtList list = new NbtList();
        for (String v : values) {
            list.add(NbtString.of(v));
        }
        return list;
    }

    static List<String> readStrings(NbtCompound nbt, String key) {
        List<String> out = new ArrayList<>();
        NbtList list = nbt.getListOrEmpty(key);
        for (int i = 0; i < list.size(); i++) {
            list.getString(i).ifPresent(out::add);
        }
        return out;
    }
}
