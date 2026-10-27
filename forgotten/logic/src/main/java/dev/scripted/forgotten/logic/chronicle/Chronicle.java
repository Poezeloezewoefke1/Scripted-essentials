package dev.scripted.forgotten.logic.chronicle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The World Chronicle: a bounded history of everything that meaningfully changed a world, plus
 * running totals of everything that did not merit an entry of its own.
 *
 * <p>Read by: memory bleeding (ghost replays), the Nether's death echoes, the Archive's rooms,
 * the Curator's body, the Second You, the Palimpsest written at an ending, and lucidity.
 */
public final class Chronicle {
    public static final int CAPACITY = 4096;

    private final Deque<ChronicleEntry> entries = new ArrayDeque<>();
    /** Counters keyed by "event:subject", e.g. "BLOCK_PLACED:minecraft:stone_bricks". */
    private final Map<String, Long> counters = new HashMap<>();

    public void record(ChronicleEntry entry) {
        count(entry.event(), entry.detail());
        if (entry.event().significant()) {
            entries.addLast(entry);
            while (entries.size() > CAPACITY) {
                evictLeastSignificant();
            }
        }
    }

    /** Counts an event without keeping an entry. */
    public void count(ChronicleEvent event, String subject) {
        counters.merge(event.name() + ":" + (subject == null ? "" : subject), 1L, Long::sum);
        counters.merge(event.name(), 1L, Long::sum);
    }

    public void restoreCounter(String key, long value) {
        counters.put(key, value);
    }

    public void restoreEntry(ChronicleEntry entry) {
        entries.addLast(entry);
    }

    public long total(ChronicleEvent event) {
        return counters.getOrDefault(event.name(), 0L);
    }

    public long countOf(ChronicleEvent event, String subject) {
        return counters.getOrDefault(event.name() + ":" + subject, 0L);
    }

    public Map<String, Long> counters() {
        return Collections.unmodifiableMap(counters);
    }

    public List<ChronicleEntry> entries() {
        return List.copyOf(entries);
    }

    public int size() {
        return entries.size();
    }

    /** The subject most often seen with this event, e.g. the most placed block. */
    public Optional<String> mostFrequent(ChronicleEvent event) {
        String prefix = event.name() + ":";
        return counters.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix) && e.getKey().length() > prefix.length())
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> e.getKey().substring(prefix.length()));
    }

    /** The top {@code n} subjects for an event, most frequent first. */
    public List<String> top(ChronicleEvent event, int n) {
        String prefix = event.name() + ":";
        return counters.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix) && e.getKey().length() > prefix.length())
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(n)
                .map(e -> e.getKey().substring(prefix.length()))
                .toList();
    }

    public List<ChronicleEntry> find(Predicate<ChronicleEntry> filter) {
        List<ChronicleEntry> out = new ArrayList<>();
        for (ChronicleEntry entry : entries) {
            if (filter.test(entry)) {
                out.add(entry);
            }
        }
        return out;
    }

    public List<ChronicleEntry> near(String dimension, int x, int y, int z, double radius, ChronicleEvent event) {
        double r2 = radius * radius;
        return find(e -> (event == null || e.event() == event) && e.dimension().equals(dimension)
                && e.distanceSq(x, y, z) <= r2);
    }

    public List<ChronicleEntry> latest(int n) {
        List<ChronicleEntry> all = new ArrayList<>(entries);
        Collections.reverse(all);
        return all.subList(0, Math.min(n, all.size()));
    }

    /** The most significant entries overall: boss kills and endings first, then by recency. */
    public List<ChronicleEntry> highlights(int n) {
        return entries.stream()
                .sorted(Comparator.comparingDouble((ChronicleEntry e) -> e.event().pressure()).reversed()
                        .thenComparing(Comparator.comparingLong(ChronicleEntry::tick).reversed()))
                .limit(n)
                .toList();
    }

    private void evictLeastSignificant() {
        // Drop the oldest of the least important kind still present, so boss kills and endings
        // survive long after ten thousand explosions have been forgotten.
        ChronicleEntry victim = null;
        for (ChronicleEntry entry : entries) {
            if (victim == null || entry.event().pressure() < victim.event().pressure()) {
                victim = entry;
            }
        }
        entries.remove(victim);
    }
}
