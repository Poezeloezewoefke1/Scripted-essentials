package dev.scripted.forgotten.logic.naming;

import dev.scripted.forgotten.logic.util.Hashing;

import java.util.Locale;

/**
 * Reads a name the way a Potential Creature would. A name that means something it recognises
 * steers it directly; a name it half-recognises steers it loosely; a name that means nothing to
 * it still becomes something, chosen by the shape of the word itself, so every name works.
 */
public final class NameInterpreter {
    private NameInterpreter() {
    }

    public enum Confidence {
        /** The name is one of the words the nature knows. */
        EXACT,
        /** The name contains a known word, or is a near-miss spelling of one. */
        NEAR,
        /** The creature made up its own mind. */
        FREE
    }

    public record Reading(Nature nature, Confidence confidence, String normalized) {
    }

    public static Reading read(String name) {
        String n = normalize(name);
        if (n.isEmpty()) {
            return new Reading(Nature.values()[0], Confidence.FREE, n);
        }
        for (Nature nature : Nature.values()) {
            if (nature.words().contains(n)) {
                return new Reading(nature, Confidence.EXACT, n);
            }
        }
        // Containment: "Hungry Bob", "MyHome". Longest word wins so "seed" does not beat "freedom".
        Nature best = null;
        int bestLength = 0;
        for (Nature nature : Nature.values()) {
            for (String word : nature.words()) {
                if (word.length() >= 3 && n.contains(word) && word.length() > bestLength) {
                    best = nature;
                    bestLength = word.length();
                }
            }
        }
        if (best != null) {
            return new Reading(best, Confidence.NEAR, n);
        }
        // Near misses: "shleter", "silense".
        for (Nature nature : Nature.values()) {
            for (String word : nature.words()) {
                if (word.length() >= 4 && Math.abs(word.length() - n.length()) <= 2 && distance(word, n) <= 2) {
                    return new Reading(nature, Confidence.NEAR, n);
                }
            }
        }
        Nature[] all = Nature.values();
        return new Reading(all[Hashing.bounded(Hashing.string(n), all.length)], Confidence.FREE, n);
    }

    static String normalize(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : name.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }
}
