package dev.scripted.forgotten.logic.language;

import java.util.EnumMap;
import java.util.Map;

/**
 * The hidden journal that fills itself in as a player studies the world's formations. A word
 * is decoded after it has been studied through the Lens three times; only decoded words can be
 * spoken back.
 */
public final class Journal {
    public static final int TO_DECODE = 3;

    private final EnumMap<Word, Integer> studied = new EnumMap<>(Word.class);

    public Journal() {
    }

    public Journal(Map<Word, Integer> progress) {
        studied.putAll(progress);
    }

    /** Records a sighting. Returns true if this sighting decoded the word. */
    public boolean study(Word word) {
        int before = studied.getOrDefault(word, 0);
        if (before >= TO_DECODE) {
            return false;
        }
        studied.put(word, before + 1);
        return before + 1 == TO_DECODE;
    }

    public boolean decoded(Word word) {
        return studied.getOrDefault(word, 0) >= TO_DECODE;
    }

    public int progress(Word word) {
        return studied.getOrDefault(word, 0);
    }

    public int decodedCount() {
        int n = 0;
        for (Word w : Word.values()) {
            if (decoded(w)) {
                n++;
            }
        }
        return n;
    }

    public Map<Word, Integer> progress() {
        return Map.copyOf(studied);
    }

    /** How a word appears in the journal: its meaning once decoded, fragments before. */
    public String render(Word word) {
        int p = progress(word);
        if (p >= TO_DECODE) {
            return word.name() + " - " + word.meaning();
        }
        if (p == 0) {
            return "???";
        }
        String m = word.meaning();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m.length(); i++) {
            char c = m.charAt(i);
            boolean reveal = c == ' ' || c == '.' || (i * 31 + word.ordinal()) % TO_DECODE < p;
            sb.append(reveal ? c : '_');
        }
        return sb.toString();
    }
}
