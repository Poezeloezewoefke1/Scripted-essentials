package dev.scripted.forgotten.logic.storm;

/** Reality Storms: server-wide events where random rules of the world fail at once. */
public enum StormKind {
    INVERTED_GRAVITY("Inverted Gravity", "Players and mobs fall upward."),
    BLOCK_AMNESIA("Block Amnesia", "Placed blocks slowly forget what they are."),
    MOB_FUSION("Mob Fusion", "Nearby creatures merge into hybrids."),
    TIME_STUTTER("Time Stutter", "The world rewinds five seconds."),
    CHUNK_MUTATION("Chunk Mutation", "A chunk is replaced by a corrupted alternate of itself."),
    PLAYER_SWAP("Player Swap", "Players trade places."),
    ADMIN_GLITCH("The Admin Glitch", "Someone who is not an operator starts issuing commands.");

    private final String title;
    private final String description;

    StormKind(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }
}
