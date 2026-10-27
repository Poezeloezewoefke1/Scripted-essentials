package dev.scripted.forgotten.logic;

import dev.scripted.forgotten.logic.bio.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BioVmTest {
    /** Builds a 6x9 chest from rows of "item*count" tokens. */
    static ProgramCompiler.Result compile(String... rows) {
        String[][] items = new String[6][9];
        int[][] counts = new int[6][9];
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 9; c++) {
                items[r][c] = "";
            }
        }
        for (int r = 0; r < rows.length; r++) {
            if (rows[r].isBlank()) {
                continue;
            }
            String[] toks = rows[r].trim().split("\\s+");
            for (int c = 0; c < toks.length; c++) {
                String[] p = toks[c].split("\\*");
                items[r][c] = p[0].contains(":") ? p[0] : "minecraft:" + p[0];
                counts[r][c] = p.length > 1 ? Integer.parseInt(p[1]) : 1;
            }
        }
        return ProgramCompiler.compile(items, counts);
    }

    static final String FARM = String.join("\n",
            "compass wheat",
            "iron_hoe",
            "comparator chest repeater*5",
            "repeater*1",
            "compass chest",
            "hopper repeater*1");

    @Test
    void farmProgramCompiles() {
        var r = compile(FARM.split("\n"));
        assertTrue(r.ok(), () -> r.error().describe());
        Program p = r.program();
        assertEquals(6, p.size());
        assertFalse(p.tightLoop());
        assertEquals(Opcode.MOVE, p.at(1).opcode());
        assertEquals(Target.MATURE_CROP, p.at(1).target());
        assertEquals(Condition.BUFFER_FULL, p.at(3).condition());
        assertEquals(5, p.at(3).number());
        assertEquals(1, p.at(6).then());
        assertEquals("", p.at(6).filter(), "the trailing repeater is not a deposit filter");
        assertEquals(p.serialize(), Program.parse(p.serialize()).serialize());
    }

    @Test
    void errors() {
        assertEquals(CompileError.Kind.EMPTY_PROGRAM, compile().error().kind());
        assertEquals(CompileError.Kind.UNKNOWN_OPCODE, compile("dirt").error().kind());
        assertEquals(CompileError.Kind.MISSING_ARGUMENT, compile("compass").error().kind());
        assertEquals(CompileError.Kind.BAD_ARGUMENT, compile("compass dirt").error().kind());
        var bad = compile("repeater*4");
        assertEquals(CompileError.Kind.BAD_JUMP, bad.error().kind());
        assertTrue(bad.error().severe());
        assertEquals(CompileError.Kind.MISSING_ARGUMENT, compile("comparator chest").error().kind());
    }

    @Test
    void tightLoopsAreDetected() {
        assertTrue(compile("repeater*1").program().tightLoop(), "10 GOTO 10");
        assertTrue(compile("note_block", "repeater*1").program().tightLoop());
        assertFalse(compile("clock", "repeater*1").program().tightLoop(), "waiting lets time pass");
        assertFalse(compile("compass wheat", "repeater*1").program().tightLoop());
        // Unreachable tight loop does not count.
        assertFalse(compile("barrier", "repeater*2").program().tightLoop());
        assertTrue(compile("note_block repeater*1").program().tightLoop(), "a then-jump can close a tight loop");
        assertEquals(CompileError.Kind.BAD_JUMP, compile("clock repeater*3").error().kind());
    }

    @Test
    void interpreterRunsFarmLoop() {
        Program p = compile(FARM.split("\n")).program();
        FakeAgent agent = new FakeAgent();
        ProgramState s = new ProgramState();
        s.reset(p);
        for (int i = 0; i < 200; i++) {
            Interpreter.tick(p, s, agent);
        }
        assertTrue(agent.log.contains("harvest"));
        assertTrue(agent.log.contains("deposit"), agent.log.toString());
        assertFalse(s.halted, "the farm loops forever");
    }

    @Test
    void tightLoopCannotHang() {
        Program p = compile("repeater*1").program();
        ProgramState s = new ProgramState();
        s.reset(p);
        assertEquals(Interpreter.BUDGET, Interpreter.tick(p, s, new FakeAgent()));
    }

    @Test
    void waitAndHalt() {
        Program p = compile("clock*2", "barrier").program();
        ProgramState s = new ProgramState();
        s.reset(p);
        for (int i = 0; i < 2 * Interpreter.TICKS_PER_SECOND; i++) {
            Interpreter.tick(p, s, new FakeAgent());
            assertFalse(s.halted);
        }
        Interpreter.tick(p, s, new FakeAgent());
        assertTrue(s.halted);
    }

    @Test
    void failedFlagCanBeTested() {
        Program p = compile("compass wheat", "comparator redstone_torch repeater*4", "barrier", "note_block*3", "barrier").program();
        FakeAgent agent = new FakeAgent();
        agent.noCrops = true;
        ProgramState s = new ProgramState();
        s.reset(p);
        for (int i = 0; i < 5; i++) {
            Interpreter.tick(p, s, agent);
        }
        assertTrue(agent.log.contains("emit2"), agent.log.toString());
    }

    static final class FakeAgent implements Agent {
        final List<String> log = new ArrayList<>();
        int buffer;
        int moveTicks;
        boolean noCrops;

        @Override
        public Status move(Target target) {
            if (noCrops && target == Target.MATURE_CROP) {
                return Status.FAILED;
            }
            moveTicks++;
            if (moveTicks < 3) {
                return Status.RUNNING;
            }
            moveTicks = 0;
            log.add("move:" + target);
            return Status.DONE;
        }

        @Override
        public Status harvest() {
            buffer += 4;
            log.add("harvest");
            return Status.DONE;
        }

        @Override
        public Status deposit(String filter) {
            buffer = 0;
            log.add("deposit");
            return Status.DONE;
        }

        @Override
        public Status pickup() {
            return Status.DONE;
        }

        @Override
        public Status attack() {
            return Status.FAILED;
        }

        @Override
        public Status shoot() {
            return Status.FAILED;
        }

        @Override
        public Status follow() {
            return Status.RUNNING;
        }

        @Override
        public boolean test(Condition condition) {
            return condition == Condition.BUFFER_FULL && buffer >= 12;
        }

        @Override
        public void emit(int pitch) {
            log.add("emit" + pitch);
        }
    }
}
