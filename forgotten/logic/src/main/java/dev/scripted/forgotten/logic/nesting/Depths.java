package dev.scripted.forgotten.logic.nesting;

/**
 * The Matryoshka layers. Depth 0 is the dimension you placed your first Frame Anchor in; each
 * Frame Anchor opens the next depth down. Time runs faster the deeper you go, and each depth
 * is a copy of the world with more copy-errors than the last.
 */
public final class Depths {
    public static final int MAX = 5;
    /** Width of the region one Frame Anchor owns inside a depth dimension. */
    public static final int REGION = 2048;

    private Depths() {
    }

    /** Time multiplier at a depth: 1, 2, 4, 8, 16, 32. */
    public static int timeFactor(int depth) {
        return 1 << Math.max(0, Math.min(MAX, depth));
    }

    /**
     * Extra block-entity ticks to run per game tick at a depth. Equal to the time factor minus
     * the tick the game already runs, capped so a depth-5 furnace bank cannot stall a server.
     */
    public static int extraBlockEntityTicks(int depth, int cap) {
        return Math.min(cap, timeFactor(depth) - 1);
    }

    /** Extra random ticks per chunk section per game tick, on top of the vanilla three. */
    public static int extraRandomTicks(int depth) {
        return 3 * (timeFactor(depth) - 1);
    }

    /** Centre of the region a Frame Anchor with this id opens into. */
    public static int regionCenterX(int anchorId) {
        return anchorId * REGION;
    }

    /** What has gone wrong in the copy at this depth. */
    public static String copyError(int depth) {
        return switch (depth) {
            case 0 -> "None. This is the original.";
            case 1 -> "Almost perfect. Some blocks have swapped places with their neighbours.";
            case 2 -> "Names are reversed. Trees have forgotten some of their leaves.";
            case 3 -> "Blocks from other biomes appear out of place. Columns repeat.";
            case 4 -> "Materials substitute for one another. Water forgets to be water.";
            default -> "The Scratch Layer. Generation has given up: tinted-glass deserts, oceans of beds.";
        };
    }

    /** Probability that any given surface block in a fresh chunk is altered by copy-error. */
    public static double errorRate(int depth) {
        return switch (depth) {
            case 0 -> 0.0;
            case 1 -> 0.01;
            case 2 -> 0.03;
            case 3 -> 0.06;
            case 4 -> 0.12;
            default -> 0.35;
        };
    }
}
