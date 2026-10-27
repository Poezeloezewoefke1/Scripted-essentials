package dev.scripted.forgotten.player;

import com.mojang.serialization.Codec;
import dev.scripted.forgotten.logic.language.Journal;
import dev.scripted.forgotten.logic.language.Word;
import dev.scripted.forgotten.logic.nesting.NestingInstability;
import dev.scripted.forgotten.logic.observer.ObserverMind;
import dev.scripted.forgotten.logic.paradox.ArchitectRank;
import dev.scripted.forgotten.logic.paradox.DebtTier;
import dev.scripted.forgotten.logic.paradox.Insight;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the mod remembers about one player. Stored as a persistent attachment that
 * survives death: the universe does not forget what you owe it just because you died.
 */
public final class PlayerMemory {
    public static final Codec<PlayerMemory> CODEC = NbtCompound.CODEC.xmap(PlayerMemory::fromNbt, PlayerMemory::toNbt);
    public static final int EDIT_LIMIT = 32;
    public static final int PLACES_LIMIT = 24;
    public static final int SNAPSHOT_LIMIT = 4;

    public int insight;
    public double debt;
    public long lastEdit;
    public final Set<String> flags = new LinkedHashSet<>();
    public final Journal journal;
    public final ObserverMind observer;
    public final List<RealityEdit> edits = new ArrayList<>();
    /** Distinct places the player has spent time, newest last: "dim|x|y|z". */
    public final List<String> places = new ArrayList<>();
    /** Return points up through the nesting: "dim|x|y|z" per depth entered. */
    public final List<String> depthStack = new ArrayList<>();
    public int instability;
    public long lastDescent;
    /** Old inventories, for the Curator to throw back at you: tick and the stacks as NBT. */
    public final List<NbtCompound> snapshots = new ArrayList<>();
    public long lastSnapshot;
    /** Borrowed rank from a stolen Codex page, and when it expires. */
    public int borrowedRank;
    public long borrowedUntil;
    /** Ticks until the Hollow's curse lifts (cleared by sleeping). */
    public boolean hollowCursed;
    public long deathlessUntil;
    public long mercyUntil;
    public int deaths;
    public long lastSpoke = Long.MIN_VALUE / 2;

    public PlayerMemory() {
        this(new Journal(), new ObserverMind());
    }

    private PlayerMemory(Journal journal, ObserverMind observer) {
        this.journal = journal;
        this.observer = observer;
    }

    public ArchitectRank rank(long now) {
        ArchitectRank own = ArchitectRank.of(insight);
        if (now < borrowedUntil && borrowedRank > own.level()) {
            return ArchitectRank.ofLevel(borrowedRank);
        }
        return own;
    }

    /** Adds insight. Returns the new rank if this crossed into one, else null. */
    public ArchitectRank gain(Insight kind, long now) {
        ArchitectRank before = ArchitectRank.of(insight);
        insight += kind.value();
        ArchitectRank after = ArchitectRank.of(insight);
        return after != before ? after : null;
    }

    public DebtTier tier() {
        return DebtTier.of(debt);
    }

    public void owe(double amount, long now) {
        debt += amount;
        lastEdit = now;
    }

    public boolean flag(String f) {
        return flags.contains(f);
    }

    /** Returns true the first time. */
    public boolean setFlag(String f) {
        return flags.add(f);
    }

    public void addEdit(RealityEdit edit) {
        edits.add(edit);
        while (edits.size() > EDIT_LIMIT) {
            edits.remove(0);
        }
    }

    public List<RealityEdit> activeEdits() {
        return edits.stream().filter(RealityEdit::active).toList();
    }

    public void visit(String place) {
        places.remove(place);
        places.add(place);
        while (places.size() > PLACES_LIMIT) {
            places.remove(0);
        }
    }

    public int depth() {
        return depthStack.size();
    }

    public NestingInstability.Symptom symptom() {
        return NestingInstability.symptom(instability);
    }

    // ---- persistence ----

    private NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("insight", insight);
        nbt.putDouble("debt", debt);
        nbt.putLong("lastEdit", lastEdit);
        nbt.put("flags", strings(flags));
        NbtCompound words = new NbtCompound();
        journal.progress().forEach((w, n) -> words.putInt(w.name(), n));
        nbt.put("journal", words);

        NbtCompound obs = new NbtCompound();
        NbtList habits = new NbtList();
        observer.habits().forEach((k, v) -> {
            NbtCompound h = new NbtCompound();
            h.putLong("k", k);
            h.putDouble("v", v);
            habits.add(h);
        });
        obs.put("habits", habits);
        obs.putInt("appearances", observer.appearances());
        obs.putInt("trust", observer.trust());
        obs.putBoolean("hostile", observer.hostile());
        obs.putLong("firstDeath", observer.firstDeath());
        nbt.put("observer", obs);

        NbtList editList = new NbtList();
        for (RealityEdit e : edits) {
            NbtCompound c = new NbtCompound();
            c.putString("k", e.kind().name());
            c.putString("d", e.dimension());
            c.putLong("p", e.pos());
            c.putInt("x", e.data());
            c.putBoolean("a", e.active());
            editList.add(c);
        }
        nbt.put("edits", editList);
        nbt.put("places", strings(places));
        nbt.put("depthStack", strings(depthStack));
        nbt.putInt("instability", instability);
        nbt.putLong("lastDescent", lastDescent);
        NbtList snaps = new NbtList();
        snaps.addAll(snapshots);
        nbt.put("snapshots", snaps);
        nbt.putLong("lastSnapshot", lastSnapshot);
        nbt.putInt("borrowedRank", borrowedRank);
        nbt.putLong("borrowedUntil", borrowedUntil);
        nbt.putBoolean("hollow", hollowCursed);
        nbt.putLong("deathlessUntil", deathlessUntil);
        nbt.putLong("mercyUntil", mercyUntil);
        nbt.putInt("deaths", deaths);
        nbt.putLong("lastSpoke", lastSpoke);
        return nbt;
    }

    private static PlayerMemory fromNbt(NbtCompound nbt) {
        Map<Word, Integer> progress = new EnumMap<>(Word.class);
        NbtCompound words = nbt.getCompoundOrEmpty("journal");
        for (String key : words.getKeys()) {
            try {
                progress.put(Word.valueOf(key), words.getInt(key, 0));
            } catch (IllegalArgumentException ignored) {
                // a word that no longer exists
            }
        }
        NbtCompound obs = nbt.getCompoundOrEmpty("observer");
        Map<Long, Double> habits = new HashMap<>();
        for (NbtElement el : obs.getListOrEmpty("habits")) {
            if (el instanceof NbtCompound h) {
                habits.put(h.getLong("k", 0), h.getDouble("v", 0));
            }
        }
        ObserverMind mind = new ObserverMind(habits, obs.getInt("appearances", 0), obs.getInt("trust", 0),
                obs.getBoolean("hostile", false), obs.getLong("firstDeath", Long.MIN_VALUE));
        PlayerMemory m = new PlayerMemory(new Journal(progress), mind);
        m.insight = nbt.getInt("insight", 0);
        m.debt = nbt.getDouble("debt", 0);
        m.lastEdit = nbt.getLong("lastEdit", 0);
        m.flags.addAll(readStrings(nbt.getListOrEmpty("flags")));
        for (NbtElement el : nbt.getListOrEmpty("edits")) {
            if (el instanceof NbtCompound c) {
                try {
                    m.edits.add(new RealityEdit(RealityEdit.Kind.valueOf(c.getString("k", "")), c.getString("d", ""),
                            c.getLong("p", 0), c.getInt("x", 0), c.getBoolean("a", false)));
                } catch (IllegalArgumentException ignored) {
                    // unknown edit kind
                }
            }
        }
        m.places.addAll(readStrings(nbt.getListOrEmpty("places")));
        m.depthStack.addAll(readStrings(nbt.getListOrEmpty("depthStack")));
        m.instability = nbt.getInt("instability", 0);
        m.lastDescent = nbt.getLong("lastDescent", 0);
        for (NbtElement el : nbt.getListOrEmpty("snapshots")) {
            if (el instanceof NbtCompound c) {
                m.snapshots.add(c);
            }
        }
        m.lastSnapshot = nbt.getLong("lastSnapshot", 0);
        m.borrowedRank = nbt.getInt("borrowedRank", 0);
        m.borrowedUntil = nbt.getLong("borrowedUntil", 0);
        m.hollowCursed = nbt.getBoolean("hollow", false);
        m.deathlessUntil = nbt.getLong("deathlessUntil", 0);
        m.mercyUntil = nbt.getLong("mercyUntil", 0);
        m.deaths = nbt.getInt("deaths", 0);
        m.lastSpoke = nbt.getLong("lastSpoke", Long.MIN_VALUE / 2);
        return m;
    }

    private static NbtList strings(Iterable<String> values) {
        NbtList list = new NbtList();
        for (String v : values) {
            list.add(net.minecraft.nbt.NbtString.of(v));
        }
        return list;
    }

    private static List<String> readStrings(NbtList list) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            list.getString(i).ifPresent(out::add);
        }
        return out;
    }
}
