package dev.scripted.essentials.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Pattern;

/** Text helpers. All user-facing strings are MiniMessage, with a legacy {@code &} fallback. */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern LEGACY = Pattern.compile("[&§][0-9a-fk-orA-FK-OR]");

    private Text() {
    }

    /**
     * Parses a configured string into a component.
     *
     * <p>Strings containing a MiniMessage tag are parsed as MiniMessage. Strings that use only the
     * legacy {@code &a} style codes are converted first, so both styles work in config files
     * without the server owner having to care which one this plugin prefers.
     */
    public static Component parse(String input, TagResolver... resolvers) {
        if (input == null) {
            return Component.empty();
        }
        if (!input.contains("<") && LEGACY.matcher(input).find()) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
        }
        return MM.deserialize(input, resolvers);
    }

    /** Parses a string for use as an item name or lore line, cancelling Minecraft's default italics. */
    public static Component item(String input, TagResolver... resolvers) {
        return parse(input, resolvers).decoration(TextDecoration.ITALIC, false);
    }

    /** Flattens a component to plain text, for logging and for string comparisons. */
    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Shorthand for a MiniMessage {@code <name>} placeholder holding untrusted text. */
    public static TagResolver placeholder(String key, String value) {
        return Placeholder.unparsed(key, value == null ? "" : value);
    }

    /** Shorthand for a MiniMessage {@code <name>} placeholder holding a component. */
    public static TagResolver placeholder(String key, Component value) {
        return Placeholder.component(key, value);
    }

    /** Capitalises {@code SOME_ENUM_NAME} into {@code Some Enum Name}. */
    public static String prettify(String constant) {
        String[] words = constant.toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
