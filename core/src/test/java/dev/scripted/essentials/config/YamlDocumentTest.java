package dev.scripted.essentials.config;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlDocumentTest {

    private static YamlDocument parse(String yaml) {
        return YamlDocument.load(new StringReader(yaml));
    }

    @Test
    void readsNestedPaths() {
        YamlDocument doc = parse("""
                chat-filter:
                  mode: censor
                  cooldown-seconds: 3
                  filter-signs: true
                """);
        assertEquals("censor", doc.getString("chat-filter.mode"));
        assertEquals(3, doc.getInt("chat-filter.cooldown-seconds", 0));
        assertTrue(doc.getBoolean("chat-filter.filter-signs", false));
    }

    @Test
    void missingPathsFallBackToTheGivenDefault() {
        YamlDocument doc = parse("a: 1");
        assertEquals("fallback", doc.getString("nope.not.here", "fallback"));
        assertEquals(7, doc.getInt("nope", 7));
        assertTrue(doc.getBoolean("nope", true));
        assertNull(doc.getString("nope"));
    }

    @Test
    void wrongTypeFallsBackRatherThanThrowing() {
        // A hand-edited file can put a string where a number belongs; that must not crash.
        YamlDocument doc = parse("count: not-a-number");
        assertEquals(5, doc.getInt("count", 5));
    }

    @Test
    void bundledDefaultsFillTheGaps() {
        YamlDocument doc = parse("prefix: mine");
        doc.setDefaults(parse("prefix: theirs\nextra: from-defaults"));
        assertEquals("mine", doc.getString("prefix"));
        assertEquals("from-defaults", doc.getString("extra"));
        assertTrue(doc.contains("extra"));
    }

    @Test
    void setCreatesIntermediateSections() {
        YamlDocument doc = YamlDocument.empty();
        doc.set("warps.hub.world", "world");
        doc.set("warps.hub.x", 12.5);
        assertEquals("world", doc.getString("warps.hub.world"));
        assertEquals(12.5, doc.getDouble("warps.hub.x", 0), 0.0001);
        assertEquals(List.of("hub"), List.copyOf(doc.getKeys("warps")));
    }

    @Test
    void settingNullRemoves() {
        YamlDocument doc = parse("warps:\n  hub:\n    world: world\n  spawn:\n    world: world\n");
        doc.set("warps.hub", null);
        assertFalse(doc.contains("warps.hub"));
        assertEquals(List.of("spawn"), List.copyOf(doc.getKeys("warps")));
    }

    @Test
    void removingSomethingThatDoesNotExistIsHarmless() {
        YamlDocument doc = YamlDocument.empty();
        doc.set("nothing.here.at.all", null);
        assertTrue(doc.getKeys().isEmpty());
    }

    @Test
    void keysOfAMissingOrScalarSectionAreEmpty() {
        YamlDocument doc = parse("scalar: 3");
        assertTrue(doc.getKeys("scalar").isEmpty());
        assertTrue(doc.getKeys("absent").isEmpty());
    }

    @Test
    void readsStringLists() {
        YamlDocument doc = parse("blocked:\n  - one\n  - two\n");
        assertEquals(List.of("one", "two"), doc.getStringList("blocked"));
        assertEquals(List.of(), doc.getStringList("absent"));
    }

    @Test
    void survivesASaveAndLoadRoundTrip() throws Exception {
        Path file = Files.createTempFile("se-test", ".yml");
        try {
            YamlDocument doc = YamlDocument.empty();
            doc.set("kits.starter.cooldown", 3600);
            doc.set("kits.starter.permission", "");
            doc.set("kits.starter.items", List.of("encoded-a", "encoded-b"));
            doc.save(file);

            YamlDocument reloaded = YamlDocument.load(file);
            assertEquals(3600, reloaded.getInt("kits.starter.cooldown", 0));
            assertEquals("", reloaded.getString("kits.starter.permission"));
            assertEquals(List.of("encoded-a", "encoded-b"), reloaded.getStringList("kits.starter.items"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void loadingAMissingFileGivesAnEmptyDocument() throws Exception {
        YamlDocument doc = YamlDocument.load(Path.of("/definitely/not/here.yml"));
        assertTrue(doc.getKeys().isEmpty());
    }
}
