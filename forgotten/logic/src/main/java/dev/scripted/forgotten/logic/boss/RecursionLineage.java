package dev.scripted.forgotten.logic.boss;

/**
 * Recursion Spiders duplicate when killed. Each generation is weaker, smaller and faster. Kill
 * enough of one lineage in one place and the overlapping copies become the Recursion Queen.
 */
public final class RecursionLineage {
    public static final int MAX_GENERATION = 4;
    public static final int QUEEN_THRESHOLD = 24;

    private RecursionLineage() {
    }

    public static int childrenOnDeath(int generation) {
        return generation >= MAX_GENERATION ? 0 : 2;
    }

    public static double health(int generation) {
        return 16.0 * Math.pow(0.7, generation);
    }

    public static double speedMultiplier(int generation) {
        return Math.pow(1.12, generation);
    }

    public static double scale(int generation) {
        return Math.pow(0.85, generation);
    }

    /** Total spiders a single generation-0 spider eventually becomes if every copy is killed. */
    public static int totalLineage() {
        int total = 0;
        int alive = 1;
        for (int g = 0; g <= MAX_GENERATION; g++) {
            total += alive;
            alive *= childrenOnDeath(g);
        }
        return total;
    }

    public static boolean queenEmerges(int killsInArea) {
        return killsInArea >= QUEEN_THRESHOLD;
    }
}
