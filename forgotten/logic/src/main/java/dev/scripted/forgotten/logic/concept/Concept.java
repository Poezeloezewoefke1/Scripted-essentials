package dev.scripted.forgotten.logic.concept;

import java.util.Locale;

/**
 * Conceptual Engineering replaces spells: these are the fundamental ideas hidden in the world.
 * Each one exists as an item. Combining them in the Paradox Engine makes phenomena.
 *
 * <p>Each concept also names where it is most often found, which is how the concepts tie the
 * dimensions and systems together: Finality comes from the End because the End is finished,
 * Recursion from the Stack because that is where loops live, and so on.
 */
public enum Concept {
    GRAVITY("The relationship between objects and falling", "Stage II gravity flips, the Backside"),
    MEMORY("The persistence of information", "Memory bleeds, the Archive, the Nether"),
    DISTANCE("The separation between locations", "The Backside's World Roots, the Cartographer"),
    IDENTITY("What makes something itself", "Stage IV amnesia, Unremembered Things"),
    CAUSALITY("The relationship between actions and consequences", "Reality storms, Corrections"),
    HUNGER("The consumption of one thing by another", "The Unwritten, Potential Creatures"),
    SILENCE("The absence of sound", "The Underdream's Quiet, staring at the Observer"),
    POSSIBILITY("The existence of outcomes that haven't occurred", "The Unwritten, the Room of Futures"),
    FINALITY("The resistance to change", "The End"),
    RECURSION("Something acting upon itself repeatedly", "The Stack, Recursion Spiders"),
    ABSENCE("The influence of something that isn't there", "Unmade chunks, the Unrendered"),
    WITNESS("The relationship between observation and existence", "The Observer");

    private final String meaning;
    private final String source;

    Concept(String meaning, String source) {
        this.meaning = meaning;
        this.source = source;
    }

    public String meaning() {
        return meaning;
    }

    public String source() {
        return source;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String itemId() {
        return "concept_" + id();
    }

    public static Concept byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }
}
