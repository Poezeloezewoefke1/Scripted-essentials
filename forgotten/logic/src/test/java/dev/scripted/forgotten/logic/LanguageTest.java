package dev.scripted.forgotten.logic;

import dev.scripted.forgotten.logic.language.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LanguageTest {
    static Map<String, Material> build(Word word, int rot, int ox, int oy, int oz) {
        Map<String, Material> m = new HashMap<>();
        for (Word.Cell c : word.rotated(rot)) {
            m.put((ox + c.x()) + "," + (oy + c.y()) + "," + (oz + c.z()), c.material());
        }
        return m;
    }

    @Test
    void everyWordIsFoundInEveryRotation() {
        for (Word w : Word.values()) {
            for (int rot = 0; rot < 4; rot++) {
                Map<String, Material> m = build(w, rot, 0, 0, 0);
                var match = FormationMatcher.find((x, y, z) -> m.get(x + "," + y + "," + z), -3, -1, -3, 3, 1, 3);
                assertTrue(match.isPresent(), w + " rot " + rot);
                assertEquals(w, match.get().word(), "rot " + rot);
            }
        }
    }

    @Test
    void wordsAreDistinct() {
        Set<String> shapes = new HashSet<>();
        for (Word w : Word.values()) {
            assertTrue(shapes.add(w.cells().toString()), "duplicate shape " + w);
        }
    }

    @Test
    void theFirstWordIsThreeDirtTwoStoneAndASapling() {
        Map<Material, Integer> count = new HashMap<>();
        for (Word.Cell c : Word.YOU.cells()) {
            count.merge(c.material(), 1, Integer::sum);
        }
        assertEquals(3, count.get(Material.EARTH));
        assertEquals(2, count.get(Material.STONE));
        assertEquals(1, count.get(Material.PLANT));
    }

    @Test
    void materialsClassify() {
        assertEquals(Material.WOOD, Material.of("minecraft:dark_oak_log"));
        assertEquals(Material.EARTH, Material.of("minecraft:grass_block"));
        assertEquals(Material.PLANT, Material.of("minecraft:birch_sapling"));
        assertEquals(Material.LIGHT, Material.of("minecraft:lantern"));
        assertNull(Material.of("minecraft:diamond_ore"));
        for (Material mat : Material.values()) {
            assertEquals(mat, Material.of(mat.canonicalBlock()), mat.toString());
        }
    }

    @Test
    void journalDecodesAfterThreeStudies() {
        Journal j = new Journal();
        assertEquals("???", j.render(Word.RAIN));
        assertFalse(j.study(Word.RAIN));
        assertFalse(j.study(Word.RAIN));
        assertTrue(j.study(Word.RAIN));
        assertFalse(j.study(Word.RAIN));
        assertTrue(j.decoded(Word.RAIN));
        assertTrue(j.render(Word.RAIN).startsWith("RAIN"));
        assertEquals(1, j.decodedCount());
    }
}
