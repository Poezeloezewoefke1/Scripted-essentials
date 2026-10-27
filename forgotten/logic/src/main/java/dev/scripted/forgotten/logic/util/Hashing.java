package dev.scripted.forgotten.logic.util;

/**
 * Deterministic hashing for "the world decided this" choices: which identity an Unremembered
 * block has, which word a formation spells, which nature a name resolves to. The same inputs
 * must always give the same answer so the world stays consistent with itself across restarts.
 */
public final class Hashing {
    private Hashing() {
    }

    /** SplitMix64 finaliser. */
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    public static long position(long seed, int x, int y, int z) {
        long h = mix(seed);
        h = mix(h ^ (x * 0x9E3779B97F4A7C15L));
        h = mix(h ^ (y * 0xC2B2AE3D27D4EB4FL));
        return mix(h ^ (z * 0x165667B19E3779F9L));
    }

    public static long string(String s) {
        long h = 0x84222325CBF29CE4L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001B3L;
        }
        return mix(h);
    }

    /** A value in [0, bound) derived from a hash. */
    public static int bounded(long hash, int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        return (int) Math.floorMod(hash, (long) bound);
    }

    /** A value in [0, 1) derived from a hash. */
    public static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }
}
