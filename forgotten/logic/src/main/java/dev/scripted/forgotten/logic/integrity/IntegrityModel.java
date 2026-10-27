package dev.scripted.forgotten.logic.integrity;

import java.util.random.RandomGenerator;

/**
 * How a chunk's integrity changes over time. Called for chunks near players on a slow cadence;
 * everything here is pure so the rules can be tested and tuned without a server.
 *
 * <p>The forces, in order of strength:
 * <ul>
 *   <li>An anchor (Reality Anchor, Reality Core, a Final block) pins the chunk at 100.</li>
 *   <li>Paradox done nearby (Paradox Engine edits, Unmaker tools) pushes integrity down hard.</li>
 *   <li>An Unwritten neighbour drags the chunk toward itself: Unmade chunks spread.</li>
 *   <li>The world's lucidity sets a slow ambient decay. A sleeping world does not forget.</li>
 *   <li>The original spawn is the world's strongest memory and resists decay; distance from it
 *       lowers the level the chunk would otherwise drift back toward.</li>
 * </ul>
 */
public final class IntegrityModel {
    /** Radius, in blocks, within which spawn fully protects integrity. */
    public static final int SPAWN_MEMORY_RADIUS = 256;

    private IntegrityModel() {
    }

    /** The integrity an untouched chunk starts at, given its distance from world spawn. */
    public static int baseline(double distanceFromSpawn) {
        if (distanceFromSpawn <= SPAWN_MEMORY_RADIUS) {
            return IntegrityStage.MAX;
        }
        // Loses one point per 400 blocks beyond the spawn radius, never below 86: the far lands
        // are still stable when a world begins, just less firmly so.
        int loss = (int) ((distanceFromSpawn - SPAWN_MEMORY_RADIUS) / 400.0);
        return Math.max(86, IntegrityStage.MAX - loss);
    }

    /**
     * One update step.
     *
     * @param current       the chunk's integrity now
     * @param baseline      {@link #baseline(double)} for this chunk
     * @param lucidity      the world's lucidity, 0-100
     * @param localParadox  paradox accumulated in this chunk since the last step (edits, tools)
     * @param lowestNeighbour lowest integrity among the four neighbouring chunks
     * @param anchored      whether an anchor covers this chunk
     * @param floor         global floor set by an ending (0 when none)
     * @param random        randomness source
     */
    public static int step(int current, int baseline, int lucidity, int localParadox, int lowestNeighbour,
                           boolean anchored, int floor, RandomGenerator random) {
        if (anchored) {
            return IntegrityStage.MAX;
        }
        int value = current;

        // Paradox is immediate. Each point of paradox costs one point of integrity.
        value -= localParadox;

        // Spreading: an Unwritten neighbour eats this chunk.
        if (lowestNeighbour <= IntegrityStage.AMNESIA.floor() && value > lowestNeighbour) {
            if (random.nextDouble() < 0.35) {
                value -= 1 + random.nextInt(3);
            }
        }

        // Ambient forgetting, driven by how awake the world is. Below lucidity 10 nothing decays.
        if (lucidity >= 10) {
            double chance = (lucidity - 9) / 400.0; // 0.0025 at 10, ~0.23 at 100
            if (random.nextDouble() < chance) {
                value -= 1;
            }
        }

        // Healing: a chunk that has not been pushed recently remembers itself, slowly, back
        // toward its baseline. Unwritten chunks never heal on their own.
        if (localParadox == 0 && value > IntegrityStage.UNWRITTEN.floor() + 7 && value < baseline) {
            double heal = lucidity >= 70 ? 0.02 : 0.08;
            if (random.nextDouble() < heal) {
                value += 1;
            }
        }

        return Math.max(floor, IntegrityStage.clamp(value));
    }

    /** Chance per anomaly roll that something happens, for a chunk at this integrity. */
    public static double anomalyChance(int integrity) {
        IntegrityStage stage = IntegrityStage.of(integrity);
        return switch (stage) {
            case STABLE -> 0.0;
            case DEJA_VU -> 0.04;
            case CONTRADICTION -> 0.08;
            case FRAGMENTATION -> 0.12;
            case AMNESIA -> 0.16;
            case UNWRITTEN -> 0.22;
        };
    }
}
