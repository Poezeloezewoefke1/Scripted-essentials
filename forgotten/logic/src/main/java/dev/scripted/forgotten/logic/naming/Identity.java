package dev.scripted.forgotten.logic.naming;

/**
 * The developing identity of a named Potential Creature.
 *
 * <p>Bond grows with meaningful interaction and makes the identity stable; at full bond the
 * creature settles into a permanent form. Zeal grows with the experiences its nature cares
 * about most; at full zeal it starts acting on its overzealous reading. A settled creature
 * whose zeal is also full may decide the name it was given is no longer sufficient.
 *
 * @param nature  what its name made it
 * @param bond    0-100
 * @param zeal    0-100
 * @param settled whether its form is permanent
 */
public record Identity(Nature nature, int bond, int zeal, boolean settled) {
    public static final int FULL = 100;

    public Identity {
        bond = clamp(bond);
        zeal = clamp(zeal);
    }

    public static Identity fresh(Nature nature) {
        return new Identity(nature, 0, 0, false);
    }

    public boolean overzealous() {
        return zeal >= FULL;
    }

    public boolean outgrown() {
        return settled && overzealous();
    }

    public Identity experience(Experience e) {
        int b = bond + e.bond();
        int z = zeal + zealFor(e);
        boolean s = settled || b >= FULL;
        return new Identity(nature, b, z, s);
    }

    /** Takes on a new nature it chose for itself. Keeps the bond; the zeal is spent. */
    public Identity rename(Nature next) {
        return new Identity(next, bond, 0, settled);
    }

    /** Which nature an outgrown creature chooses: the one its obsession points to. */
    public Nature drift() {
        return switch (nature) {
            case SHELTER -> Nature.HOME;
            case HOME -> Nature.GARDEN;
            case GARDEN -> Nature.HUNGER;
            case HUNGER -> Nature.SILENCE;
            case SILENCE -> Nature.LIGHT;
            case LIGHT -> Nature.STORM;
            case STORM -> Nature.FREEDOM;
            case FREEDOM -> Nature.SHELTER;
        };
    }

    private int zealFor(Experience e) {
        return switch (nature) {
            case HUNGER -> e == Experience.FED ? 6 : e == Experience.ALONE ? 2 : 0;
            case SHELTER -> e == Experience.OWNER_HURT ? 8 : 0;
            case SILENCE -> e == Experience.NOISE ? 3 : 0;
            case HOME -> e == Experience.DIMENSION_CHANGE ? 10 : e == Experience.OWNER_NEAR ? 1 : 0;
            case FREEDOM -> e == Experience.BLOCKED ? 5 : 0;
            case LIGHT -> e == Experience.DARKNESS ? 2 : 0;
            case STORM -> e == Experience.KILLED_HOSTILE ? 6 : e == Experience.OWNER_HURT ? 4 : 0;
            case GARDEN -> e == Experience.GREW ? 2 : 0;
        };
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(FULL, v));
    }

    public enum Experience {
        OWNER_NEAR(1),
        FED(4),
        OWNER_HURT(0),
        ALONE(0),
        BLOCKED(0),
        NOISE(0),
        DARKNESS(0),
        KILLED_HOSTILE(2),
        DIMENSION_CHANGE(5),
        GREW(1),
        GIFT(6);

        private final int bond;

        Experience(int bond) {
            this.bond = bond;
        }

        public int bond() {
            return bond;
        }
    }
}
