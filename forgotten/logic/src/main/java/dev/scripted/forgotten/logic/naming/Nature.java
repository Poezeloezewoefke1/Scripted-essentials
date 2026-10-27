package dev.scripted.forgotten.logic.naming;

import java.util.List;

/**
 * What a Potential Creature becomes when it is given a name. The name is a direction, not a
 * command: each nature has an ordinary reading and an overzealous one it drifts into when its
 * experiences push it far enough.
 */
public enum Nature {
    HUNGER(List.of("hunger", "hungry", "appetite", "famine", "maw", "glutton", "devour", "eat", "feast", "greed"),
            "Consumes nearby stone and earth to sustain itself, growing as it eats.",
            "Has begun eating the things you built."),
    SHELTER(List.of("shelter", "shield", "guard", "protect", "refuge", "haven", "wall", "fortress", "safety", "keep"),
            "Raises walls around you when hostile things come near.",
            "Protects you best by imprisoning you inside itself."),
    SILENCE(List.of("silence", "quiet", "hush", "mute", "calm", "still", "peace", "whisper", "shh", "void"),
            "Absorbs the sound of everything around it.",
            "Has started silencing living things too."),
    HOME(List.of("home", "house", "hearth", "nest", "dwelling", "family", "belong", "comfort", "return", "base"),
            "Follows you across dimensions, wearing the blocks of the places you feel safest.",
            "Believes a house should be alive, and is turning yours into an organism."),
    FREEDOM(List.of("freedom", "free", "liberty", "escape", "open", "wild", "unbound", "release", "sky", "wind"),
            "Moves where it likes and helps you move: it leaps and carries.",
            "Destroys anything that restricts movement: walls, doors, bridges."),
    LIGHT(List.of("light", "lamp", "sun", "glow", "dawn", "bright", "star", "spark", "lumen", "candle"),
            "Lights the dark around it so nothing hostile can spawn.",
            "Burns what it decides is darkness."),
    STORM(List.of("storm", "thunder", "lightning", "rain", "tempest", "fury", "rage", "chaos", "gale", "war"),
            "Calls weather and strikes at what threatens you.",
            "Strikes at anything that moves."),
    GARDEN(List.of("garden", "grow", "bloom", "flower", "seed", "life", "green", "forest", "tree", "spring"),
            "Makes crops and saplings around it grow.",
            "Grows over everything, including you.");

    private final List<String> words;
    private final String reading;
    private final String overreading;

    Nature(List<String> words, String reading, String overreading) {
        this.words = words;
        this.reading = reading;
        this.overreading = overreading;
    }

    public List<String> words() {
        return words;
    }

    public String reading() {
        return reading;
    }

    public String overreading() {
        return overreading;
    }
}
