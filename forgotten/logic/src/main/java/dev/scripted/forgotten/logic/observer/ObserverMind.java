package dev.scripted.forgotten.logic.observer;

import dev.scripted.forgotten.logic.util.Hashing;

import java.util.HashMap;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * What the Observer knows about one player. The Observer (also the Witness of the waking-world
 * idea: the same tall silhouette that only stands where you are looking) learns your habits and
 * appears near the places you return to. It never approaches closer than 40 blocks until it
 * trusts you. Staring at it stops the world for a moment. Attacking it makes the world resent
 * you; befriending it opens the way into the Underdream.
 */
public final class ObserverMind {
    public static final int HABIT_LIMIT = 64;
    public static final int STARE_TICKS = 100;
    public static final int TRUST_FOR_PASSAGE = 60;
    public static final double FAR = 72.0;
    public static final double NEAR = 40.0;
    public static final double TRUSTED_NEAR = 12.0;

    /** chunk key -> how often the player has been there (decays). */
    private final Map<Long, Double> habits = new HashMap<>();
    private int appearances;
    private int stare;
    private int trust;
    private boolean hostile;
    private long firstDeath = Long.MIN_VALUE;

    public ObserverMind() {
    }

    public ObserverMind(Map<Long, Double> habits, int appearances, int trust, boolean hostile, long firstDeath) {
        this.habits.putAll(habits);
        this.appearances = appearances;
        this.trust = trust;
        this.hostile = hostile;
        this.firstDeath = firstDeath;
    }

    public Map<Long, Double> habits() {
        return Map.copyOf(habits);
    }

    public int appearances() {
        return appearances;
    }

    public int trust() {
        return trust;
    }

    public boolean hostile() {
        return hostile;
    }

    public long firstDeath() {
        return firstDeath;
    }

    public boolean grantsPassage() {
        return !hostile && trust >= TRUST_FOR_PASSAGE;
    }

    /** Called periodically with the player's current chunk. */
    public void observe(long chunkKey) {
        habits.merge(chunkKey, 1.0, Double::sum);
        if (habits.size() > HABIT_LIMIT) {
            // Forget the least visited place and let everything else fade a little.
            Long weakest = null;
            double low = Double.MAX_VALUE;
            for (Map.Entry<Long, Double> e : habits.entrySet()) {
                if (e.getValue() < low) {
                    low = e.getValue();
                    weakest = e.getKey();
                }
            }
            habits.remove(weakest);
            habits.replaceAll((k, v) -> v * 0.98);
        }
    }

    public void died(long chunkKey) {
        if (firstDeath == Long.MIN_VALUE) {
            firstDeath = chunkKey;
        }
    }

    /** How far away it stands. It drifts closer with each appearance, never inside its limit. */
    public double distance() {
        double limit = trust >= TRUST_FOR_PASSAGE ? TRUSTED_NEAR : NEAR;
        return Math.max(limit, FAR - appearances * 1.5);
    }

    /** Chance per check that it appears at all. Rare, and only once the world is stirring. */
    public double appearanceChance(double lucidity, boolean alone) {
        if (hostile || lucidity < 20) {
            return 0.0;
        }
        double base = 0.004 + (lucidity - 20) * 0.0002;
        return alone ? base * 2 : base;
    }

    /** Picks which of the player's familiar places it will stand near. */
    public long chooseHaunt(RandomGenerator random, long fallback) {
        if (firstDeath != Long.MIN_VALUE && random.nextDouble() < 0.15) {
            return firstDeath;
        }
        double total = 0;
        for (double w : habits.values()) {
            total += w;
        }
        if (total <= 0) {
            return fallback;
        }
        double pick = random.nextDouble() * total;
        for (Map.Entry<Long, Double> e : habits.entrySet()) {
            pick -= e.getValue();
            if (pick <= 0) {
                return e.getKey();
            }
        }
        return fallback;
    }

    public void appeared() {
        appearances++;
    }

    /** Accumulates direct looking. Returns true on the tick the world freezes. */
    public boolean stare(boolean looking) {
        if (!looking) {
            stare = Math.max(0, stare - 4);
            return false;
        }
        stare++;
        if (stare >= STARE_TICKS) {
            stare = 0;
            return true;
        }
        return false;
    }

    /** A gift it has never seen before earns trust; ones it has seen earn nothing. */
    public int gift(String item, long seed) {
        if (hostile) {
            return 0;
        }
        int gain = 4 + Hashing.bounded(Hashing.string(item) ^ seed, 5);
        trust = Math.min(100, trust + gain);
        return gain;
    }

    public void spokenTo() {
        if (!hostile) {
            trust = Math.min(100, trust + 15);
        }
    }

    public void attacked() {
        hostile = true;
        trust = 0;
    }
}
