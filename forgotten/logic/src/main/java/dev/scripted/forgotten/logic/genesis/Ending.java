package dev.scripted.forgotten.logic.genesis;

/**
 * The final choice. Five source ideas each ended with a choice, and they turned out to be the
 * same few choices seen from different sides, so each ending here answers several of them:
 *
 * <ul>
 *   <li>PRESERVE - Preserve the World / Repair Reality / Reboot / the Waking World's Refuse.</li>
 *   <li>RESTORE - Restore the First World / Fork.</li>
 *   <li>AUTHOR - Become the Author / Become the Engine.</li>
 *   <li>MERGE - the Waking World's Merge: become part of the terrain.</li>
 *   <li>TERMINATE - Delete the World / Terminate and Run Again.</li>
 * </ul>
 */
public enum Ending {
    PRESERVE("Preserve the World",
            "Restore the constraints. The world stabilises; reality manipulation is heavily restricted; "
                    + "anomalies become rare. One place of your choosing becomes a memorial the Witness visits each night."),
    RESTORE("Restore the First World",
            "Release what was removed. Dimensions bleed into each other permanently and the world keeps changing. "
                    + "You can phase between the unravelling world and the stable First World at will."),
    AUTHOR("Become the Author",
            "Take the system that decides which rules exist. Write laws for players, creatures and the world."),
    MERGE("Merge",
            "Become a permanent part of the world: a heartblock bearing your name that future players will find."),
    TERMINATE("Run Again",
            "End this run. The world keeps a palimpsest of it: the next world you make remembers fragments of this one.");

    private final String title;
    private final String description;

    Ending(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    /** MERGE is the waking world's choice and needs the world itself to be ascending. */
    public boolean available(double lucidity, boolean beginningRevealed) {
        if (!beginningRevealed) {
            return false;
        }
        return this != MERGE || lucidity >= 90.0;
    }
}
