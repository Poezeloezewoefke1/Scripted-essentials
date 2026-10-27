package dev.scripted.forgotten.logic.bio;

/**
 * The body a program drives. The mod implements this against a real mob; tests implement it
 * against a fake. Every action is called once per VM tick while its instruction is current and
 * reports whether the instruction has finished.
 */
public interface Agent {
    enum Status {
        /** Still working; call again next tick. */
        RUNNING,
        /** Done. */
        DONE,
        /** Could not be done (no target, nothing to harvest). Advances, and sets the FAILED flag. */
        FAILED
    }

    Status move(Target target);

    Status harvest();

    Status deposit(String filter);

    Status pickup();

    Status attack();

    Status shoot();

    Status follow();

    boolean test(Condition condition);

    void emit(int pitch);
}
