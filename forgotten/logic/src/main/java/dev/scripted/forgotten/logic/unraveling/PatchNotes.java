package dev.scripted.forgotten.logic.unraveling;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Patch Notes are the Mender's consumable: each one restores integrity around where it is
 * read, and each one is a different, procedurally named fix the world applied to itself.
 */
public final class PatchNotes {
    private static final List<String> SUBJECTS = List.of(
            "Creepers", "Sheep", "Rivers", "The moon", "Villagers", "Shadows", "Furnaces", "Gravity",
            "Doors", "Chickens", "Torches", "Clouds", "Bedrock", "Skeletons", "Cows", "Lava", "Bees",
            "The sun", "Chests", "Ladders", "Wolves", "Snow", "Mountains", "Compasses", "Beds");
    private static final List<String> BUGS = List.of(
            "no longer photosynthesize", "remember their own deaths less often", "now fall downward again",
            "no longer rise in the west", "stop claiming to be 300 years old", "point away from light again",
            "no longer smelt items before they are inserted", "apply to sand again",
            "open only when approached", "no longer teleport behind players", "cast light instead of absorbing it",
            "no longer contain caves", "are solid again", "no longer hum the same song", "moo without exploding",
            "flow downhill", "stop dreaming during the day", "rise in the east", "contain items, not doors",
            "lead up and down instead of elsewhere", "stop following the previous owner", "melt when warm",
            "stay still", "point to a place that exists", "can be slept in without waking somewhere else");

    private PatchNotes() {
    }

    public record Note(String version, String fix, int strength) {
        public String title() {
            return "Patch Notes v" + version;
        }

        public String line() {
            return "Fixed: " + fix;
        }
    }

    public static Note generate(RandomGenerator random) {
        String version = (1 + random.nextInt(2)) + "." + random.nextInt(22) + "." + random.nextInt(10);
        String subject = SUBJECTS.get(random.nextInt(SUBJECTS.size()));
        String bug = BUGS.get(random.nextInt(BUGS.size()));
        int strength = 15 + random.nextInt(16);
        return new Note(version, subject + " " + bug, strength);
    }
}
