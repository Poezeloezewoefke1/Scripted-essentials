package dev.scripted.forgotten.logic.bio;

/**
 * One compiled line.
 *
 * @param line      1-based source line (row of the Compiler Chest)
 * @param opcode    the operation
 * @param target    MOVE target, or null
 * @param condition IF condition, or null
 * @param filter    DEPOSIT item filter, or empty
 * @param number    WAIT seconds, JUMP/IF target line, EMIT pitch, FOLLOW seconds
 * @param then      line to continue at afterwards, from a trailing repeater; 0 to fall through
 */
public record Instruction(int line, Opcode opcode, Target target, Condition condition, String filter, int number, int then) {
    public Instruction {
        filter = filter == null ? "" : filter;
    }

    public Instruction(int line, Opcode opcode, Target target, Condition condition, String filter, int number) {
        this(line, opcode, target, condition, filter, number, 0);
    }

    public Instruction withThen(int line) {
        return new Instruction(this.line, opcode, target, condition, filter, number, line);
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02d %s", line, opcode));
        if (target != null) {
            sb.append(' ').append(target);
        }
        if (condition != null) {
            sb.append(' ').append(condition);
        }
        if (!filter.isEmpty()) {
            sb.append(' ').append(filter);
        }
        if (opcode == Opcode.JUMP || opcode == Opcode.IF) {
            sb.append(" -> ").append(number);
        } else if (number != 0) {
            sb.append(' ').append(number);
        }
        if (then > 0) {
            sb.append(" then ").append(then);
        }
        return sb.toString();
    }
}
