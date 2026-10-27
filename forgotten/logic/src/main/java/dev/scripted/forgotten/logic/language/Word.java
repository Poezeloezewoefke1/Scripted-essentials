package dev.scripted.forgotten.logic.language;

import java.util.List;

import static dev.scripted.forgotten.logic.language.Material.*;

/**
 * The vocabulary of the world's geometric language. Each word is a small formation of
 * materials. Cells are (x, y, z, material) relative to the formation's origin; y=0 is the
 * ground layer the formation stands on top of.
 *
 * <p>The first word is the one from the original description: three earth, two stone and a
 * sapling.
 */
public enum Word {
    YOU("You. The world is addressing you.",
            cells(0, 0, 0, EARTH, 1, 0, 0, EARTH, 2, 0, 0, EARTH, 0, 1, 0, STONE, 1, 1, 0, STONE, 2, 1, 0, PLANT)),
    WORLD("Itself. The world, speaking of the world.",
            cells(0, 0, 0, STONE, 0, 1, 0, STONE, 0, 2, 0, STONE, 1, 0, 0, EARTH, -1, 0, 0, EARTH)),
    SLEEP("Sleep. Rest. Let me rest.",
            cells(0, 0, 0, WOOD, 1, 0, 0, WOOD, 0, 1, 0, WOOL, 1, 1, 0, WOOL)),
    RAIN("Rain. Water falling.",
            cells(0, 0, 0, GLASS, 0, 1, 0, GLASS, 0, 2, 0, LEAF, 1, 2, 0, LEAF, -1, 2, 0, LEAF)),
    CLEAR("Clear sky. Open.",
            cells(0, 0, 0, SAND, 0, 1, 0, LIGHT, 1, 0, 0, SAND, -1, 0, 0, SAND)),
    DAY("Day. Waking.",
            cells(0, 0, 0, STONE, 0, 1, 0, LIGHT, 0, 2, 0, LIGHT)),
    NIGHT("Night. Dreaming.",
            cells(0, 0, 0, WOOL, 0, 1, 0, WOOL, 0, 2, 0, GLASS)),
    GIFT("Giving. Something for you.",
            cells(0, 0, 0, WOOD, 1, 0, 0, METAL, 0, 0, 1, METAL, 1, 0, 1, WOOD)),
    WITNESS("The one who watches.",
            cells(0, 0, 0, STONE, 0, 1, 0, STONE, 0, 2, 0, GLASS, 0, 3, 0, STONE)),
    PARADOX("A contradiction. Something that is and is not.",
            cells(0, 0, 0, SAND, 0, 1, 0, GLASS, 0, 2, 0, SAND, 1, 0, 0, PLANT)),
    HOME("Home. A place that remembers you.",
            cells(0, 0, 0, WOOD, 1, 0, 0, WOOD, 0, 1, 0, WOOD, 1, 1, 0, LIGHT, 0, 2, 0, LEAF)),
    FORGET("Forgetting. Let it go.",
            cells(0, 0, 0, SAND, 1, 0, 0, EARTH, 2, 0, 0, STONE, 1, 1, 0, PLANT, 1, 2, 0, GLASS));

    private final String meaning;
    private final List<Cell> cells;

    Word(String meaning, List<Cell> cells) {
        this.meaning = meaning;
        this.cells = cells;
    }

    public String meaning() {
        return meaning;
    }

    public List<Cell> cells() {
        return cells;
    }

    /** Cells rotated by quarter turns around the vertical axis. */
    public List<Cell> rotated(int quarterTurns) {
        int r = Math.floorMod(quarterTurns, 4);
        return cells.stream().map(c -> {
            int x = c.x();
            int z = c.z();
            for (int i = 0; i < r; i++) {
                int t = x;
                x = -z;
                z = t;
            }
            return new Cell(x, c.y(), z, c.material());
        }).toList();
    }

    public record Cell(int x, int y, int z, Material material) {
    }

    private static List<Cell> cells(Object... spec) {
        Cell[] out = new Cell[spec.length / 4];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Cell((Integer) spec[i * 4], (Integer) spec[i * 4 + 1], (Integer) spec[i * 4 + 2],
                    (Material) spec[i * 4 + 3]);
        }
        return List.of(out);
    }
}
