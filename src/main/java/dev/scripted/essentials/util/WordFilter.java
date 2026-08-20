package dev.scripted.essentials.util;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Word matching for the chat and sign filters.
 *
 * <p>Deliberately free of any Bukkit or config dependency: this is the part with the actual
 * logic in it, so it is kept plain enough to test directly.
 *
 * <p>Matching works on a folded form of the text — lowercased, common letter substitutions
 * undone, everything that is not a letter dropped, and runs of the same letter collapsed. That
 * is what makes {@code f-r-e-e   m0ney} match {@code money}, which is the entire point of a
 * filter. The cost is that folding is lossy and cannot be reversed, so censoring can only strike
 * out literal spellings; see {@link #censor}.
 */
public final class WordFilter {

    private static final Map<Character, Character> SUBSTITUTIONS = Map.of(
            '0', 'o', '1', 'i', '3', 'e', '4', 'a', '5', 's', '7', 't', '@', 'a', '$', 's');

    private WordFilter() {
    }

    /** Normalises text so obfuscated spellings collapse onto their plain form. */
    public static String fold(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        char previous = 0;
        for (char raw : input.toLowerCase(Locale.ROOT).toCharArray()) {
            char letter = SUBSTITUTIONS.getOrDefault(raw, raw);
            if (!Character.isLetter(letter)) {
                continue;
            }
            if (letter != previous) {
                out.append(letter);
                previous = letter;
            }
        }
        return out.toString();
    }

    /** The first blocked word the text contains, or null when it is clean. */
    public static String firstMatch(String text, Collection<String> blocked) {
        String folded = fold(text);
        for (String word : blocked) {
            String target = fold(word);
            if (!target.isEmpty() && folded.contains(target)) {
                return word;
            }
        }
        return null;
    }

    /**
     * Replaces every literal occurrence of a blocked word with asterisks.
     *
     * <p>This only catches the spelling as written. When {@link #firstMatch} flags text that this
     * leaves untouched — the reader wrote {@code m0ney}, and there is no {@code money} to strike
     * out — the caller must block the message rather than let it through uncensored.
     */
    public static String censor(String text, Collection<String> blocked) {
        String result = text;
        for (String word : blocked) {
            if (word == null || word.isBlank()) {
                continue;
            }
            result = result.replaceAll("(?i)" + Pattern.quote(word), "*".repeat(word.length()));
        }
        return result;
    }
}
