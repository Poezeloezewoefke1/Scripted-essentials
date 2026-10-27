package dev.scripted.forgotten.logic.bio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A compiled Bio-Node program. Lines are addressed by their source line number, so a jump to
 * line 3 means "the third row of the chest", even if rows above it were blank.
 */
public final class Program {
    private final TreeMap<Integer, Instruction> lines;
    private final boolean tightLoop;

    public Program(List<Instruction> instructions, boolean tightLoop) {
        this.lines = new TreeMap<>();
        for (Instruction i : instructions) {
            lines.put(i.line(), i);
        }
        this.tightLoop = tightLoop;
    }

    /**
     * Whether the program contains a loop that never lets time pass. Such a program is valid but
     * pathological; compiled onto a cartridge it becomes an Infinite Loop, which is what opens
     * the way to the Stack.
     */
    public boolean tightLoop() {
        return tightLoop;
    }

    public Instruction at(int line) {
        return lines.get(line);
    }

    public int first() {
        return lines.isEmpty() ? -1 : lines.firstKey();
    }

    /** The next line after {@code line}, or -1 at the end (programs end; they do not wrap). */
    public int next(int line) {
        Integer n = lines.higherKey(line);
        return n == null ? -1 : n;
    }

    public boolean has(int line) {
        return lines.containsKey(line);
    }

    public List<Instruction> instructions() {
        return new ArrayList<>(lines.values());
    }

    public int size() {
        return lines.size();
    }

    /** Text form stored on cartridges and parsed back by {@link #parse(String)}. */
    public String serialize() {
        StringBuilder sb = new StringBuilder(tightLoop ? "L" : "N");
        for (Map.Entry<Integer, Instruction> e : lines.entrySet()) {
            Instruction i = e.getValue();
            sb.append(';').append(i.line())
                    .append(',').append(i.opcode().name())
                    .append(',').append(i.target() == null ? "" : i.target().name())
                    .append(',').append(i.condition() == null ? "" : i.condition().name())
                    .append(',').append(i.filter())
                    .append(',').append(i.number())
                    .append(',').append(i.then());
        }
        return sb.toString();
    }

    public static Program parse(String text) {
        if (text == null || text.isEmpty()) {
            return new Program(List.of(), false);
        }
        String[] parts = text.split(";");
        boolean tight = parts[0].equals("L");
        List<Instruction> out = new ArrayList<>();
        for (int p = 1; p < parts.length; p++) {
            String[] f = parts[p].split(",", -1);
            out.add(new Instruction(
                    Integer.parseInt(f[0]),
                    Opcode.valueOf(f[1]),
                    f[2].isEmpty() ? null : Target.valueOf(f[2]),
                    f[3].isEmpty() ? null : Condition.valueOf(f[3]),
                    f[4],
                    Integer.parseInt(f[5]),
                    f.length > 6 ? Integer.parseInt(f[6]) : 0));
        }
        return new Program(out, tight);
    }

    public List<String> listing() {
        return lines.values().stream().map(Instruction::render).toList();
    }
}
