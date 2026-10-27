package dev.scripted.forgotten.system;

import java.util.Locale;

/**
 * Identities a vanilla creature can take on when the world forgets what it is. These are not
 * new mobs: the cow is still a cow, it just remembers being something else.
 */
public enum GlitchKind {
    /** The Herd: a passive mob with a creeper's memory. It sneaks up, then the moo explodes. */
    HERD("remembers being a creeper"),
    /** An Enderman's memory: it teleports behind you. */
    BLINKER("remembers being an enderman"),
    /** It forgets that walls are solid and steps through them. */
    WALLWALKER("has forgotten that walls are solid"),
    /** It stops and stares, as if it remembers something about you that has not happened yet. */
    STARER("remembers something about you"),
    /** The Archivist: a villager with an enderman's restlessness, leaving books about the world. */
    ARCHIVIST("is trying to remember what the world is"),
    /** It forgot which way is down. */
    FALLING_UP("has forgotten which way is down"),
    /** A hostile mob with a passive mob's mind. It wanders and grazes. */
    GENTLE("has forgotten how to hate");

    private final String reading;

    GlitchKind(String reading) {
        this.reading = reading;
    }

    /** What the Lens shows when it looks at a creature with this identity. */
    public String reading() {
        return reading;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static GlitchKind byId(String id) {
        try {
            return valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
