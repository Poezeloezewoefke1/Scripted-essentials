package dev.scripted.forgotten.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Message styles used across the mod, so the world's voice sounds the same everywhere. */
public final class Texts {
    private Texts() {
    }

    /** The world speaking. Italic, grey. */
    public static MutableText world(String s) {
        return Text.literal(s).formatted(Formatting.GRAY, Formatting.ITALIC);
    }

    /** A discovery. */
    public static MutableText discovery(String s) {
        return Text.literal(s).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
    }

    /** Mechanics feedback. */
    public static MutableText info(String s) {
        return Text.literal(s).formatted(Formatting.AQUA);
    }

    public static MutableText warn(String s) {
        return Text.literal(s).formatted(Formatting.RED);
    }

    public static MutableText glitch(String s) {
        return Text.literal(s).formatted(Formatting.DARK_PURPLE, Formatting.OBFUSCATED);
    }

    public static void tell(ServerPlayerEntity player, Text text) {
        player.sendMessage(text, false);
    }

    public static void bar(ServerPlayerEntity player, Text text) {
        player.sendMessage(text, true);
    }

    /** Scrambles some characters of a string, the way names scramble under Reality Bleed. */
    public static String scramble(String s, net.minecraft.util.math.random.Random random, float amount) {
        char[] chars = s.toCharArray();
        String glyphs = "#$%&@?!*~^";
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != ' ' && random.nextFloat() < amount) {
                chars[i] = glyphs.charAt(random.nextInt(glyphs.length()));
            }
        }
        return new String(chars);
    }
}
