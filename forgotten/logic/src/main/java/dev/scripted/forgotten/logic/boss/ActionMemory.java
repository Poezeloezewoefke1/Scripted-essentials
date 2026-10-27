package dev.scripted.forgotten.logic.boss;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * The King of Yesterday remembers everything you do in its arena and replays it against you.
 * It also refuses to be hurt the same way twice: repeating an attack makes it weaker each time
 * until the King is immune to it. You cannot win by repeating a strategy.
 */
public final class ActionMemory {
    public static final int CAPACITY = 64;
    public static final int IMMUNE_AFTER = 4;

    public enum Kind {
        MELEE, PROJECTILE, BLOCK_PLACED, POTION, PARADOX
    }

    /**
     * @param kind      what kind of action
     * @param signature what distinguishes it: weapon id, block id, effect id
     * @param x         where (for blocks and echoes)
     * @param y         where
     * @param z         where
     * @param magnitude damage dealt, amplifier, etc.
     */
    public record Action(Kind kind, String signature, int x, int y, int z, float magnitude) {
        public String key() {
            return kind + ":" + signature;
        }
    }

    private final Deque<Action> remembered = new ArrayDeque<>();
    private final Map<String, Integer> uses = new HashMap<>();

    /** Records an action and returns the damage multiplier it earns against the King. */
    public float remember(Action action) {
        remembered.addLast(action);
        while (remembered.size() > CAPACITY) {
            remembered.removeFirst();
        }
        int n = uses.merge(action.key(), 1, Integer::sum);
        return multiplier(n);
    }

    /** First use is rewarded; each repeat is worth less; then nothing. */
    public static float multiplier(int nthUse) {
        if (nthUse >= IMMUNE_AFTER) {
            return 0.0f;
        }
        return switch (nthUse) {
            case 1 -> 1.5f;
            case 2 -> 0.8f;
            default -> 0.35f;
        };
    }

    public int uses(Kind kind, String signature) {
        return uses.getOrDefault(kind + ":" + signature, 0);
    }

    public int size() {
        return remembered.size();
    }

    public List<Action> all() {
        return List.copyOf(remembered);
    }

    /** Picks a remembered action to replay, favouring recent ones. */
    public Action replay(RandomGenerator random) {
        if (remembered.isEmpty()) {
            return null;
        }
        List<Action> all = List.copyOf(remembered);
        int n = all.size();
        // Triangular distribution toward the end of the list.
        int a = random.nextInt(n);
        int b = random.nextInt(n);
        return all.get(Math.max(a, b));
    }
}
