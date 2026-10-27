package dev.scripted.forgotten.logic.boss;

import java.util.EnumMap;

/**
 * The Compiler has no health. It has Stack Depth: a count of the errors you have introduced.
 * Ordinary attacks are expected behaviour and do nothing. Doing things the runtime does not
 * expect - null weapons, glitch grenades, destabilising its arena, hitting it with things that
 * are not weapons - pushes the stack deeper. At the end of each phase it crashes, drops part of
 * itself, and reboots harder.
 *
 * <p>The same error repeated becomes expected: each repeat of a kind is worth less.
 */
public final class StackDepth {
    public enum Phase {
        /** Spawns glitched mobs and rewrites the arena. */
        PARSE,
        /** Undefined behaviour: gravity swaps, teleports into walls, inverted controls. */
        EXECUTE,
        /** Tries to delete you. A countdown runs; interrupt it with enough errors. */
        COMPILE
    }

    public enum ErrorKind {
        NULL_WEAPON(3),
        GLITCH_GRENADE(4),
        DESTABILIZE(3),
        WRONG_TOOL(1),
        PARADOX(2),
        CONTRADICTORY_STATE(2);

        private final int weight;

        ErrorKind(int weight) {
            this.weight = weight;
        }

        public int weight() {
            return weight;
        }
    }

    public static final int PHASE_SIZE = 12;
    public static final int COMPILE_COUNTDOWN = 600;
    public static final int CRASHES_TO_DEFEAT = 3;

    private int depth;
    private int crashes;
    private int countdown = -1;
    private final EnumMap<ErrorKind, Integer> repeats = new EnumMap<>(ErrorKind.class);

    public StackDepth() {
    }

    public StackDepth(int depth, int crashes, int countdown) {
        this.depth = depth;
        this.crashes = crashes;
        this.countdown = countdown;
    }

    public int depth() {
        return depth;
    }

    public int crashes() {
        return crashes;
    }

    public int countdown() {
        return countdown;
    }

    /** Errors needed per phase grow with every reboot. */
    public int phaseSize() {
        return PHASE_SIZE + 4 * crashes;
    }

    public Phase phase() {
        int size = phaseSize();
        if (depth < size) {
            return Phase.PARSE;
        }
        if (depth < size * 2) {
            return Phase.EXECUTE;
        }
        return Phase.COMPILE;
    }

    public boolean defeated() {
        return crashes >= CRASHES_TO_DEFEAT;
    }

    /** Introduces an error. Returns what happened. */
    public Event error(ErrorKind kind) {
        if (defeated()) {
            return Event.NOTHING;
        }
        int n = repeats.merge(kind, 1, Integer::sum);
        int gained = Math.max(0, kind.weight() - (n - 1) / 3);
        if (gained == 0) {
            return Event.EXPECTED;
        }
        Phase before = phase();
        depth += gained;
        if (depth >= phaseSize() * 3) {
            return crash();
        }
        Phase after = phase();
        if (after != before) {
            if (after == Phase.COMPILE) {
                countdown = COMPILE_COUNTDOWN;
            }
            return Event.PHASE_CHANGED;
        }
        return Event.DEEPER;
    }

    /** Advances the compile countdown. Returns true when it reaches zero: the delete fires. */
    public boolean tickCountdown() {
        if (phase() != Phase.COMPILE || countdown < 0) {
            return false;
        }
        countdown--;
        if (countdown <= 0) {
            // It deletes, then falls back to the start of its compile phase and tries again.
            depth = phaseSize() * 2;
            countdown = COMPILE_COUNTDOWN;
            return true;
        }
        return false;
    }

    private Event crash() {
        crashes++;
        depth = 0;
        countdown = -1;
        repeats.clear();
        return defeated() ? Event.TERMINATED : Event.CRASHED;
    }

    public enum Event {
        NOTHING, EXPECTED, DEEPER, PHASE_CHANGED, CRASHED, TERMINATED
    }
}
