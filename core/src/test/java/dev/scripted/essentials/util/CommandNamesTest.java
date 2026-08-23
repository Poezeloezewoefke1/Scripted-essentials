package dev.scripted.essentials.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandNamesTest {

    @Test
    void stripsTheLeadingSlash() {
        assertEquals("heal", CommandNames.normalise("/heal"));
        assertEquals("heal", CommandNames.normalise("heal"));
    }

    @Test
    void stripsArguments() {
        assertEquals("heal", CommandNames.normalise("/heal Notch"));
    }

    @Test
    void stripsThePluginPrefix() {
        // Blocking /pl has to block /bukkit:pl too, or the blocker is trivially bypassed.
        assertEquals("pl", CommandNames.normalise("/bukkit:pl"));
        assertEquals("pl", CommandNames.normalise("bukkit:pl"));
        assertEquals("se", CommandNames.normalise("/scriptedessentials:se reload"));
    }

    @Test
    void lowercasesAndTrims() {
        assertEquals("gamemode", CommandNames.normalise("  /GameMode  "));
    }

    @Test
    void handlesNullAndEmpty() {
        assertEquals("", CommandNames.normalise(null));
        assertEquals("", CommandNames.normalise(""));
        assertEquals("", CommandNames.normalise("/"));
    }
}
