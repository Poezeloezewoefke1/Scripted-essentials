package dev.scripted.forgotten.logic.boss;

import dev.scripted.forgotten.logic.concept.Concept;

/**
 * The Author does not have health. It has conviction: its certainty that every change you made
 * was a deviation from the design. It works through your abilities one at a time, trying to
 * return you to the state you entered the world in. Each step can be answered with the concept
 * that resists it; answering correctly shakes its conviction far more than any sword.
 *
 * <p>When its conviction is gone it stops, and asks its question.
 */
public final class AuthorTrial {
    public static final int CONVICTION = 100;

    public enum Step {
        /** "You have changed too much." Your hotbar stops answering you. */
        UNMAKE_INVENTORY(Concept.IDENTITY, "Your inventory stops recognising you."),
        /** Your tools forget their enchantments. */
        UNMAKE_ENCHANTMENTS(Concept.MEMORY, "Your tools forget what they were taught."),
        /** Your reality edits go dormant. */
        UNMAKE_EDITS(Concept.FINALITY, "Everything you changed begins to change back."),
        /** It rewinds you and turns your blows aside. */
        UNMAKE_CAUSES(Concept.CAUSALITY, "Your actions stop having consequences."),
        /** Your way back from death closes. */
        UNMAKE_RETURN(Concept.POSSIBILITY, "There is only one way this ends."),
        /** It has stopped. It is waiting for your answer. */
        QUESTION(null, "If the world can become anything, why must it remain this?");

        private final Concept answer;
        private final String line;

        Step(Concept answer, String line) {
            this.answer = answer;
            this.line = line;
        }

        public Concept answer() {
            return answer;
        }

        public String line() {
            return line;
        }
    }

    private int conviction;
    private Step step;

    public AuthorTrial() {
        this(CONVICTION, Step.UNMAKE_INVENTORY);
    }

    public AuthorTrial(int conviction, Step step) {
        this.conviction = conviction;
        this.step = step;
    }

    public int conviction() {
        return conviction;
    }

    public Step step() {
        return step;
    }

    public boolean asking() {
        return step == Step.QUESTION;
    }

    /** A plain blow. Barely matters. */
    public Step strike() {
        return shake(1);
    }

    /** A concept used against the current step. The right one is devastating. */
    public Step answer(Concept concept) {
        if (asking()) {
            return step;
        }
        boolean right = concept == step.answer();
        Step before = step;
        shake(right ? 20 : 4);
        if (right && step == before && step != Step.QUESTION) {
            advance();
        }
        return step;
    }

    private Step shake(int amount) {
        if (asking()) {
            return step;
        }
        conviction = Math.max(0, conviction - amount);
        // Conviction thresholds also move it on: it will try every method before it gives up.
        int expected = Math.min(Step.UNMAKE_RETURN.ordinal(), (CONVICTION - conviction) / 20);
        while (step.ordinal() < expected) {
            advance();
        }
        if (conviction == 0) {
            step = Step.QUESTION;
        }
        return step;
    }

    private void advance() {
        Step[] all = Step.values();
        if (step.ordinal() < Step.UNMAKE_RETURN.ordinal()) {
            step = all[step.ordinal() + 1];
        }
    }
}
