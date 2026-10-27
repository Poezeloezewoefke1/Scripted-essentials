package dev.scripted.forgotten.logic.language;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Finds a word in a block of materials. The mod reads the blocks around where a player is
 * speaking into a grid and asks this which word, if any, was built there.
 */
public final class FormationMatcher {
    private FormationMatcher() {
    }

    /** Read access to a box of materials. Positions outside the box return null. */
    @FunctionalInterface
    public interface Grid {
        Material at(int x, int y, int z);
    }

    public record Match(Word word, int x, int y, int z, int rotation) {
    }

    /**
     * Searches every origin inside [min, max] (inclusive) and every rotation. Larger words are
     * tried first so a word that contains a smaller one is not misread as it.
     */
    public static Optional<Match> find(Grid grid, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        List<Word> words = List.of(Word.values()).stream()
                .sorted(Comparator.comparingInt((Word w) -> w.cells().size()).reversed())
                .toList();
        for (Word word : words) {
            for (int rot = 0; rot < 4; rot++) {
                List<Word.Cell> cells = word.rotated(rot);
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            if (fits(grid, cells, x, y, z)) {
                                return Optional.of(new Match(word, x, y, z, rot));
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    static boolean fits(Grid grid, List<Word.Cell> cells, int ox, int oy, int oz) {
        for (Word.Cell c : cells) {
            if (grid.at(ox + c.x(), oy + c.y(), oz + c.z()) != c.material()) {
                return false;
            }
        }
        return true;
    }
}
