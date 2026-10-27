package dev.scripted.forgotten.logic.nesting;

/**
 * Going deep too fast without stabilising the layers with anchors makes the nesting unstable.
 * Symptoms escalate until the Unwrapper comes for the first Frame Anchor.
 */
public final class NestingInstability {
    public static final int ECHO_STEPS = 20;
    public static final int MIRROR_BLOCKS = 45;
    public static final int UNWRAPPER = 80;

    private NestingInstability() {
    }

    /** Instability added by descending into a depth. Anchored layers add nothing. */
    public static int onDescend(int depth, boolean anchored, long ticksSinceLastDescent) {
        if (anchored) {
            return 0;
        }
        int base = 6 * depth;
        // Rushing: descending again within five minutes doubles the cost.
        return ticksSinceLastDescent < 6000 ? base * 2 : base;
    }

    public static Symptom symptom(int instability) {
        if (instability >= UNWRAPPER) {
            return Symptom.UNWRAPPER;
        }
        if (instability >= MIRROR_BLOCKS) {
            return Symptom.MIRRORED_BLOCKS;
        }
        if (instability >= ECHO_STEPS) {
            return Symptom.ECHO_STEPS;
        }
        return Symptom.NONE;
    }

    /** Mass the Unwrapper must be fed before it is too heavy to move. */
    public static double unwrapperMass(int depthReached) {
        return 600.0 + 200.0 * depthReached;
    }

    public enum Symptom {
        NONE,
        /** You hear your own footsteps from ten seconds ago. */
        ECHO_STEPS,
        /** A block placed in a deep layer also appears in a shallower one. */
        MIRRORED_BLOCKS,
        /** It walks toward the first Frame Anchor. */
        UNWRAPPER
    }
}
