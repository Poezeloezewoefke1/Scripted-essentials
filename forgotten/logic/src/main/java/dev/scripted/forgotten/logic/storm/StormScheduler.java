package dev.scripted.forgotten.logic.storm;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** Decides when the next Reality Storm comes and what it is. */
public final class StormScheduler {
    /** Storms only begin once the world is half aware. */
    public static final double MIN_LUCIDITY = 30.0;

    private StormScheduler() {
    }

    /** Ticks until the next storm after one ends: three days when half aware, one when ascending. */
    public static long interval(double lucidity, RandomGenerator random) {
        double t = Math.max(0, Math.min(1, (lucidity - MIN_LUCIDITY) / 60.0));
        long days = Math.round(3.0 - 2.0 * t);
        return Math.max(1, days) * 24000L + random.nextInt(12000);
    }

    public static int duration(RandomGenerator random) {
        return 2400 + random.nextInt(2400);
    }

    public static List<StormKind> pick(RandomGenerator random, int players, double lucidity) {
        List<StormKind> pool = new ArrayList<>(List.of(StormKind.values()));
        if (players < 2) {
            pool.remove(StormKind.PLAYER_SWAP);
        }
        List<StormKind> out = new ArrayList<>();
        // A "total" storm stacks several failures at once. Only in a nearly awake world.
        int count = lucidity >= 80 && random.nextDouble() < 0.25 ? 3 : 1;
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            out.add(pool.remove(random.nextInt(pool.size())));
        }
        return out;
    }
}
