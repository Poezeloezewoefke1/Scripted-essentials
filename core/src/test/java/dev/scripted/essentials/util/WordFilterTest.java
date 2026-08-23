package dev.scripted.essentials.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class WordFilterTest {

    private static final List<String> BLOCKED = List.of("money", "spam");

    @Nested
    @DisplayName("fold")
    class Fold {

        @Test
        void dropsCaseSeparatorsAndPunctuation() {
            assertEquals("money", WordFilter.fold("M-O-N-E-Y"));
            assertEquals("money", WordFilter.fold("  m o n e y!  "));
        }

        @Test
        void undoesCommonLetterSubstitutions() {
            assertEquals("money", WordFilter.fold("m0n3y"));
            assertEquals("spam", WordFilter.fold("$p4m"));
        }

        @Test
        void collapsesRepeatedLetters() {
            assertEquals("money", WordFilter.fold("mmmooonnneeeyyy"));
        }

        @Test
        void handlesNullAndEmpty() {
            assertEquals("", WordFilter.fold(null));
            assertEquals("", WordFilter.fold("   "));
        }

        @Test
        void collapsingDoesNotMergeDistinctWords() {
            // "sp" + "am" must still read as one run; the point is that folding never
            // invents a match that is not there.
            assertEquals("spamis", WordFilter.fold("spam is"));
        }
    }

    @Nested
    @DisplayName("firstMatch")
    class FirstMatch {

        @Test
        void findsPlainOccurrences() {
            assertEquals("money", WordFilter.firstMatch("free money here", BLOCKED));
        }

        @Test
        void findsObfuscatedOccurrences() {
            assertNotNull(WordFilter.firstMatch("free m0ney here", BLOCKED));
            assertNotNull(WordFilter.firstMatch("f r e e  m-o-n-e-y", BLOCKED));
        }

        @Test
        void passesCleanText() {
            assertNull(WordFilter.firstMatch("hello world", BLOCKED));
        }

        @Test
        void ignoresBlankEntriesInTheBlockList() {
            // A blank entry folds to "", which every string contains; it must not match everything.
            assertNull(WordFilter.firstMatch("hello world", List.of("", "   ")));
        }
    }

    @Nested
    @DisplayName("censor")
    class Censor {

        @Test
        void starsOutLiteralSpellings() {
            assertEquals("free ***** here", WordFilter.censor("free money here", BLOCKED));
        }

        @Test
        void isCaseInsensitive() {
            assertEquals("*****", WordFilter.censor("MoNeY", BLOCKED));
        }

        @Test
        void leavesObfuscatedSpellingsUntouched() {
            // This is the documented limitation that forces the caller to block instead:
            // firstMatch catches it, censor cannot.
            String text = "free m0ney here";
            assertNotNull(WordFilter.firstMatch(text, BLOCKED));
            assertEquals(text, WordFilter.censor(text, BLOCKED));
        }

        @Test
        void treatsBlockedWordsAsLiteralsNotPatterns() {
            // A regex metacharacter in the block list must not blow up or match wildly.
            assertEquals("a*b", WordFilter.censor("a.b", List.of(".")));
            assertEquals("safe", WordFilter.censor("safe", List.of("(")));
        }
    }
}
