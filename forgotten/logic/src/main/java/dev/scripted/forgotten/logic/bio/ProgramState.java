package dev.scripted.forgotten.logic.bio;

/** Execution state of a program on one node. Mutable; stored on the mob between ticks. */
public final class ProgramState {
    public int pc = -1;
    public int timer;
    public int elapsed;
    public boolean failed;
    public boolean halted;

    public ProgramState() {
    }

    public ProgramState(int pc, int timer, int elapsed, boolean failed, boolean halted) {
        this.pc = pc;
        this.timer = timer;
        this.elapsed = elapsed;
        this.failed = failed;
        this.halted = halted;
    }

    public void reset(Program program) {
        pc = program.first();
        timer = 0;
        elapsed = 0;
        failed = false;
        halted = pc < 0;
    }
}
