package dev.scripted.forgotten.logic.genesis;

/**
 * Global rules the Genesis Interface can set for the whole world once the Author has asked its
 * question. Each is implemented by the mod as an ongoing change to how the world behaves.
 */
public enum GenesisRule {
    GRAVITY_OPTIONAL("Gravity is optional.",
            "Sneak in mid-air to hover. Sand and gravel stay where they are put. Some creatures float."),
    WORLDS_CONNECTED("The Nether and Overworld are connected.",
            "Nether ground breaks through in the Overworld; Piglins keep their minds here; trees take root in the Nether."),
    RUINS_GROW("Every abandoned structure can grow.",
            "Things that were demolished slowly rebuild themselves from the world's memory of them."),
    ITEMS_REMEMBER("Items remember their owners.",
            "Tools keep a history of their use and grow stronger with it."),
    DEATH_IS_A_DOOR("Death is no longer the end of a journey.",
            "The dead return where they fell, after walking for a while as a ghost."),
    WORLD_DREAMS("The world can dream.",
            "Sleep carries you into the Underdream, where the world rebuilds the places you have been."),
    REWRITE_THE_BEGINNING("Rewrite the beginning.",
            "Opens the final choice. Requires the deepest secret of the First World.");

    private final String statement;
    private final String effect;

    GenesisRule(String statement, String effect) {
        this.statement = statement;
        this.effect = effect;
    }

    public String statement() {
        return statement;
    }

    public String effect() {
        return effect;
    }
}
