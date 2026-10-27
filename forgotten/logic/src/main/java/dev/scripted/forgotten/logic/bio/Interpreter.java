package dev.scripted.forgotten.logic.bio;

/**
 * Runs a program one VM tick at a time. Instructions that take no time run back to back, up to
 * a budget, so a tight loop costs at most {@link #BUDGET} instructions per tick and can never
 * hang the server.
 */
public final class Interpreter {
    public static final int BUDGET = 16;
    /** VM ticks per second of WAIT. The mod steps nodes every 10 game ticks, so 2. */
    public static final int TICKS_PER_SECOND = 2;
    /** An action that runs this many VM ticks without finishing is abandoned as failed. */
    public static final int TIMEOUT = 60;

    private Interpreter() {
    }

    /** Executes one VM tick. Returns the number of instructions executed. */
    public static int tick(Program program, ProgramState state, Agent agent) {
        if (state.halted || state.pc < 0) {
            state.halted = true;
            return 0;
        }
        int executed = 0;
        while (executed < BUDGET && !state.halted) {
            Instruction ins = program.at(state.pc);
            if (ins == null) {
                state.halted = true;
                break;
            }
            executed++;
            switch (ins.opcode()) {
                case HALT -> state.halted = true;
                case JUMP -> jump(program, state, ins.number());
                case IF -> {
                    boolean holds = ins.condition() == Condition.FAILED ? state.failed : agent.test(ins.condition());
                    if (holds) {
                        jump(program, state, ins.number());
                    } else {
                        advance(program, state);
                    }
                }
                case EMIT -> {
                    agent.emit(ins.number());
                    advance(program, state);
                }
                case WAIT -> {
                    if (state.timer == 0) {
                        state.timer = ins.number() * TICKS_PER_SECOND;
                    }
                    state.timer--;
                    if (state.timer <= 0) {
                        state.timer = 0;
                        advance(program, state);
                    }
                    return executed;
                }
                case FOLLOW -> {
                    if (state.timer == 0) {
                        state.timer = ins.number() * TICKS_PER_SECOND;
                    }
                    agent.follow();
                    state.timer--;
                    if (state.timer <= 0) {
                        state.timer = 0;
                        advance(program, state);
                    }
                    return executed;
                }
                default -> {
                    Agent.Status status = switch (ins.opcode()) {
                        case MOVE -> agent.move(ins.target());
                        case HARVEST -> agent.harvest();
                        case DEPOSIT -> agent.deposit(ins.filter());
                        case PICKUP -> agent.pickup();
                        case ATTACK -> agent.attack();
                        case SHOOT -> agent.shoot();
                        default -> Agent.Status.FAILED;
                    };
                    state.elapsed++;
                    if (status == Agent.Status.RUNNING && state.elapsed < TIMEOUT) {
                        return executed;
                    }
                    state.failed = status != Agent.Status.DONE;
                    state.elapsed = 0;
                    advance(program, state);
                    return executed;
                }
            }
        }
        return executed;
    }

    private static void jump(Program program, ProgramState state, int line) {
        state.pc = program.has(line) ? line : -1;
        state.halted = state.pc < 0;
    }

    private static void advance(Program program, ProgramState state) {
        Instruction current = program.at(state.pc);
        if (current != null && current.then() > 0) {
            jump(program, state, current.then());
            return;
        }
        state.pc = program.next(state.pc);
        state.halted = state.pc < 0;
    }
}
