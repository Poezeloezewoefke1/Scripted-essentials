package dev.scripted.essentials.util;

import java.util.Locale;

/** Normalises a typed command into the bare name used for comparisons. */
public final class CommandNames {

    private CommandNames() {
    }

    /**
     * Strips the leading slash, any {@code plugin:} prefix and any arguments, and lowercases.
     *
     * <p>The prefix matters: dropping it is what makes blocking {@code /pl} also block
     * {@code /bukkit:pl}, which is the usual way a naive blocker is walked around.
     */
    public static String normalise(String command) {
        if (command == null) {
            return "";
        }
        String value = command.trim().toLowerCase(Locale.ROOT);
        int space = value.indexOf(' ');
        if (space >= 0) {
            value = value.substring(0, space);
        }
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(colon + 1);
        }
        return value;
    }
}
