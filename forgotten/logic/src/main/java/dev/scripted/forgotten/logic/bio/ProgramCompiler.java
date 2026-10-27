package dev.scripted.forgotten.logic.bio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compiles a Compiler Chest. The chest is read row by row; each non-empty row is a line.
 */
public final class ProgramCompiler {
    private ProgramCompiler() {
    }

    /**
     * @param items  item ids per slot, rows of equal width; empty string for an empty slot
     * @param counts stack sizes per slot
     */
    public static Result compile(String[][] items, int[][] counts) {
        List<Instruction> out = new ArrayList<>();
        Set<Integer> present = new HashSet<>();
        List<int[]> jumps = new ArrayList<>(); // line, target line, column

        for (int row = 0; row < items.length; row++) {
            int line = row + 1;
            String[] r = items[row];
            int col = firstFilled(r, 0);
            if (col < 0) {
                continue;
            }
            Opcode op = Opcode.forItem(r[col]);
            if (op == null) {
                return Result.error(new CompileError(CompileError.Kind.UNKNOWN_OPCODE, line, col + 1,
                        "'" + r[col] + "' is not an instruction"));
            }
            // A repeater at the end of a row that is not itself a jump means "then go to line N".
            int thenCol = -1;
            if (op != Opcode.JUMP && op != Opcode.IF && op != Opcode.HALT) {
                int last = lastFilled(r);
                if (last > col && "minecraft:repeater".equals(r[last])) {
                    thenCol = last;
                    r = java.util.Arrays.copyOf(r, last);
                }
            }
            int argCol = firstFilled(r, col + 1);
            String arg = argCol < 0 ? "" : r[argCol];
            Instruction ins;
            switch (op) {
                case MOVE -> {
                    if (argCol < 0) {
                        return Result.error(missing(line, col, "MOVE needs a target"));
                    }
                    Target t = Target.forItem(arg);
                    if (t == null) {
                        return Result.error(bad(line, argCol, "'" + arg + "' is not something to move to"));
                    }
                    ins = new Instruction(line, op, t, null, "", 0);
                }
                case DEPOSIT -> ins = new Instruction(line, op, null, null, arg, 0);
                case WAIT -> ins = new Instruction(line, op, null, null, "", Math.max(1, counts[row][col]));
                case FOLLOW -> {
                    int seconds = argCol >= 0 && "minecraft:clock".equals(arg) ? counts[row][argCol] : 10;
                    ins = new Instruction(line, op, null, null, "", seconds);
                }
                case EMIT -> ins = new Instruction(line, op, null, null, "", Math.min(24, counts[row][col] - 1));
                case JUMP -> {
                    int target = counts[row][col];
                    jumps.add(new int[]{line, target, col + 1});
                    ins = new Instruction(line, op, null, null, "", target);
                }
                case IF -> {
                    if (argCol < 0) {
                        return Result.error(missing(line, col, "IF needs a condition"));
                    }
                    Condition c = Condition.forItem(arg);
                    if (c == null) {
                        return Result.error(bad(line, argCol, "'" + arg + "' is not a condition"));
                    }
                    int jumpCol = firstFilled(r, argCol + 1);
                    if (jumpCol < 0 || !"minecraft:repeater".equals(r[jumpCol])) {
                        return Result.error(missing(line, argCol, "IF needs a repeater saying where to jump"));
                    }
                    int target = counts[row][jumpCol];
                    jumps.add(new int[]{line, target, jumpCol + 1});
                    ins = new Instruction(line, op, null, c, "", target);
                }
                default -> ins = new Instruction(line, op, null, null, "", 0);
            }
            if (thenCol >= 0) {
                int target = counts[row][thenCol];
                jumps.add(new int[]{line, target, thenCol + 1});
                ins = ins.withThen(target);
            }
            out.add(ins);
            present.add(line);
        }

        if (out.isEmpty()) {
            return Result.error(new CompileError(CompileError.Kind.EMPTY_PROGRAM, 0, 0, "nothing to compile"));
        }
        for (int[] j : jumps) {
            if (!present.contains(j[1])) {
                return Result.error(new CompileError(CompileError.Kind.BAD_JUMP, j[0], j[2],
                        "jump to line " + j[1] + ", which is empty"));
            }
        }
        Program program = new Program(out, false);
        boolean tight = hasTightLoop(program);
        return Result.ok(tight ? new Program(out, true) : program);
    }

    /**
     * Detects a reachable cycle in the control-flow graph made only of instructions that take
     * no time. Such a loop would spin forever without the world advancing.
     */
    static boolean hasTightLoop(Program program) {
        Set<Integer> reachable = new HashSet<>();
        List<Integer> stack = new ArrayList<>();
        if (program.first() >= 0) {
            stack.add(program.first());
        }
        while (!stack.isEmpty()) {
            int line = stack.remove(stack.size() - 1);
            if (line < 0 || !reachable.add(line)) {
                continue;
            }
            stack.addAll(successors(program, line));
        }
        for (int start : reachable) {
            if (program.at(start).opcode().takesTime()) {
                continue;
            }
            // DFS through timeless instructions only, looking for a way back to start.
            Set<Integer> seen = new HashSet<>();
            List<Integer> work = new ArrayList<>(successors(program, start));
            while (!work.isEmpty()) {
                int line = work.remove(work.size() - 1);
                if (line == start) {
                    return true;
                }
                if (line < 0 || !seen.add(line)) {
                    continue;
                }
                Instruction i = program.at(line);
                if (i == null || i.opcode().takesTime()) {
                    continue;
                }
                work.addAll(successors(program, line));
            }
        }
        return false;
    }

    private static List<Integer> successors(Program program, int line) {
        Instruction i = program.at(line);
        if (i == null) {
            return List.of();
        }
        return switch (i.opcode()) {
            case HALT -> List.of();
            case JUMP -> List.of(i.number());
            case IF -> List.of(i.number(), program.next(line));
            default -> List.of(i.then() > 0 ? i.then() : program.next(line));
        };
    }

    private static int firstFilled(String[] row, int from) {
        for (int c = from; c < row.length; c++) {
            if (row[c] != null && !row[c].isEmpty() && !row[c].equals("minecraft:air")) {
                return c;
            }
        }
        return -1;
    }

    private static int lastFilled(String[] row) {
        for (int c = row.length - 1; c >= 0; c--) {
            if (row[c] != null && !row[c].isEmpty() && !row[c].equals("minecraft:air")) {
                return c;
            }
        }
        return -1;
    }

    private static CompileError missing(int line, int col, String msg) {
        return new CompileError(CompileError.Kind.MISSING_ARGUMENT, line, col + 1, msg);
    }

    private static CompileError bad(int line, int col, String msg) {
        return new CompileError(CompileError.Kind.BAD_ARGUMENT, line, col + 1, msg);
    }

    public record Result(Program program, CompileError error) {
        static Result ok(Program p) {
            return new Result(p, null);
        }

        static Result error(CompileError e) {
            return new Result(null, e);
        }

        public boolean ok() {
            return error == null;
        }
    }
}
