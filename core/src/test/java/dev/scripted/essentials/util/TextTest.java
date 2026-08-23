package dev.scripted.essentials.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TextTest {

    @Test
    void parsesMiniMessage() {
        Component parsed = Text.parse("<red>danger</red>");
        assertEquals("danger", Text.plain(parsed));
        assertEquals(NamedTextColor.RED, parsed.color());
    }

    @Test
    void parsesLegacyAmpersandCodes() {
        // Servers migrating from older plugins keep &-codes in their config files.
        Component parsed = Text.parse("&cdanger");
        assertEquals("danger", Text.plain(parsed));
    }

    @Test
    void prefersMiniMessageWhenBothStylesCouldApply() {
        // A string containing a tag is MiniMessage, so a stray & stays literal text
        // instead of the two styles fighting over it.
        assertEquals("a & b", Text.plain(Text.parse("<gray>a & b")));
    }

    @Test
    void resolvesPlaceholders() {
        Component parsed = Text.parse("hello <name>", Text.placeholder("name", "Notch"));
        assertEquals("hello Notch", Text.plain(parsed));
    }

    @Test
    void placeholderContentIsNotReparsedAsMarkup() {
        // A player-supplied name must never be able to inject formatting.
        Component parsed = Text.parse("hi <name>", Text.placeholder("name", "<red>evil</red>"));
        assertEquals("hi <red>evil</red>", Text.plain(parsed));
    }

    @Test
    void handlesNullInput() {
        assertEquals("", Text.plain(Text.parse(null)));
    }

    @Test
    void itemTextCancelsMinecraftDefaultItalics() {
        Component item = Text.item("<aqua>Sword");
        assertEquals(TextDecoration.State.FALSE, item.decoration(TextDecoration.ITALIC));
    }

    @Test
    void prettifiesEnumConstants() {
        assertEquals("Golden Apple", Text.prettify("GOLDEN_APPLE"));
        assertEquals("Survival", Text.prettify("SURVIVAL"));
        assertEquals("", Text.prettify(""));
        assertEquals("A B", Text.prettify("A__B"));
    }

    @Test
    void plainTextSerialisationIsNeverNull() {
        assertNotNull(Text.plain(Component.empty()));
    }
}
