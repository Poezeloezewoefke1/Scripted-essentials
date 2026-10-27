package dev.scripted.forgotten.logic.genesis;

import dev.scripted.forgotten.logic.concept.Concept;

/**
 * A law of nature written by whoever became the Author: a subject and a concept. Every pairing
 * means something; the mod implements all thirty-six.
 */
public record Law(LawSubject subject, Concept concept) {
    public String key() {
        return subject.name() + ":" + concept.name();
    }

    public static Law parse(String key) {
        String[] p = key.split(":");
        return new Law(LawSubject.valueOf(p[0]), Concept.valueOf(p[1]));
    }

    public String describe() {
        String who = switch (subject) {
            case PLAYERS -> "Players";
            case CREATURES -> "Creatures";
            case WORLD -> "The world";
        };
        if (subject == LawSubject.WORLD) {
            return who + " " + switch (concept) {
                case GRAVITY -> "lets sand and gravel hang where they are.";
                case MEMORY -> "regrows the stone and earth that is mined from it.";
                case DISTANCE -> "throws thrown things further.";
                case IDENTITY -> "never forgets itself past Contradiction.";
                case CAUSALITY -> "undoes explosions after they happen.";
                case HUNGER -> "grows crops three times as fast.";
                case SILENCE -> "silences hostile creatures.";
                case POSSIBILITY -> "sometimes gives twice what is mined.";
                case FINALITY -> "cannot be broken by explosions.";
                case RECURSION -> "gives animals twins.";
                case ABSENCE -> "spawns half as many monsters.";
                case WITNESS -> "lets every player see in the dark.";
            };
        }
        return who + " " + switch (concept) {
            case GRAVITY -> "fall at half speed.";
            case MEMORY -> "slowly remember being whole.";
            case DISTANCE -> "move faster.";
            case IDENTITY -> "cannot be transformed by amnesia.";
            case CAUSALITY -> "return a third of all harm done to them.";
            case HUNGER -> "do not hunger.";
            case SILENCE -> "make no sound.";
            case POSSIBILITY -> "sometimes avoid harm entirely.";
            case FINALITY -> "cannot be knocked back.";
            case RECURSION -> "sometimes come back once after dying.";
            case ABSENCE -> "are unseen while crouching or in darkness.";
            case WITNESS -> "glow so that everything sees them.";
        };
    }
}
