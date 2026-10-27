package dev.scripted.forgotten.logic.concept;

import dev.scripted.forgotten.logic.util.Hashing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which concept a memory distils into when it is contradicted by a Paradox Fragment. Memories
 * of falling things give Gravity, memories of the End give Finality, and so on; anything else
 * gives whatever its name hashes to, so every memory yields something and always the same thing.
 */
public final class ConceptDistiller {
    private static final Map<Concept, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put(Concept.GRAVITY, List.of("sand", "gravel", "anvil", "feather", "phantom", "elytra", "shulker", "pointed_dripstone"));
        KEYWORDS.put(Concept.FINALITY, List.of("end_stone", "ender", "dragon", "chorus", "purpur", "obsidian", "bedrock"));
        KEYWORDS.put(Concept.RECURSION, List.of("spider", "silverfish", "slime", "magma_cube", "vine", "kelp", "bamboo", "recursion"));
        KEYWORDS.put(Concept.SILENCE, List.of("wool", "sculk", "warden", "carpet", "snow", "quiet"));
        KEYWORDS.put(Concept.HUNGER, List.of("zombie", "husk", "wheat", "bread", "pig", "cow", "rot", "fermented", "glutton"));
        KEYWORDS.put(Concept.MEMORY, List.of("book", "bookshelf", "lectern", "villager", "map", "echo", "amethyst", "sky"));
        KEYWORDS.put(Concept.DISTANCE, List.of("portal", "pearl", "compass", "rail", "boat", "horse", "root"));
        KEYWORDS.put(Concept.IDENTITY, List.of("name_tag", "player", "mannequin", "armor_stand", "head", "skull", "allay"));
        KEYWORDS.put(Concept.CAUSALITY, List.of("redstone", "piston", "tnt", "creeper", "observer", "repeater", "comparator"));
        KEYWORDS.put(Concept.POSSIBILITY, List.of("sapling", "seeds", "egg", "spawner", "flower", "bee", "honey"));
        KEYWORDS.put(Concept.ABSENCE, List.of("air", "void", "glass", "barrier", "light", "ghast", "vex"));
        KEYWORDS.put(Concept.WITNESS, List.of("eye", "spyglass", "observer_", "lantern", "enderman", "watcher"));
    }

    private ConceptDistiller() {
    }

    public static Concept conceptFor(String source) {
        String path = source.contains(":") ? source.substring(source.indexOf(':') + 1) : source;
        for (Map.Entry<Concept, List<String>> e : KEYWORDS.entrySet()) {
            for (String keyword : e.getValue()) {
                if (path.contains(keyword)) {
                    return e.getKey();
                }
            }
        }
        Concept[] all = Concept.values();
        return all[Hashing.bounded(Hashing.string(source), all.length)];
    }
}
