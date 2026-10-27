package dev.scripted.forgotten.logic.boss;

import dev.scripted.forgotten.logic.chronicle.Chronicle;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;

import java.util.List;

/**
 * The Curator is the consciousness the world's memories assemble into, so it looks different in
 * every world. Its body is made of the blocks placed most often, its servants are the creatures
 * killed most often, and its strength scales with how much history there is to draw on.
 *
 * @param bodyBlocks  blocks its body is built from, most placed first
 * @param servant     entity type it summons
 * @param health      maximum health
 * @param deaths      how many player deaths it can force you to relive
 */
public record CuratorProfile(List<String> bodyBlocks, String servant, float health, int deaths) {
    public static final List<String> DEFAULT_BODY = List.of("minecraft:cobblestone", "minecraft:oak_planks", "minecraft:dirt");

    public static CuratorProfile from(Chronicle chronicle) {
        List<String> body = chronicle.top(ChronicleEvent.BLOCK_PLACED, 3);
        if (body.isEmpty()) {
            body = DEFAULT_BODY;
        }
        String servant = chronicle.mostFrequent(ChronicleEvent.MOB_KILLED).orElse("minecraft:zombie");
        long history = chronicle.size();
        float health = (float) Math.min(1200.0, 300.0 + history * 0.5);
        int deaths = (int) Math.min(12, chronicle.total(ChronicleEvent.PLAYER_DIED));
        return new CuratorProfile(body, servant, health, deaths);
    }
}
