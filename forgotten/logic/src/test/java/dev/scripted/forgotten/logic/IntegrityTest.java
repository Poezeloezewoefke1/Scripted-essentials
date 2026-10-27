package dev.scripted.forgotten.logic;

import dev.scripted.forgotten.logic.integrity.IntegrityModel;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class IntegrityTest {
    @Test
    void stagesCoverEveryValue() {
        assertEquals(IntegrityStage.STABLE, IntegrityStage.of(100));
        assertEquals(IntegrityStage.STABLE, IntegrityStage.of(85));
        assertEquals(IntegrityStage.DEJA_VU, IntegrityStage.of(84));
        assertEquals(IntegrityStage.CONTRADICTION, IntegrityStage.of(45));
        assertEquals(IntegrityStage.FRAGMENTATION, IntegrityStage.of(44));
        assertEquals(IntegrityStage.AMNESIA, IntegrityStage.of(8));
        assertEquals(IntegrityStage.UNWRITTEN, IntegrityStage.of(7));
        assertEquals(IntegrityStage.UNWRITTEN, IntegrityStage.of(-50));
        assertEquals(IntegrityStage.STABLE, IntegrityStage.of(500));
    }

    @Test
    void spawnIsRememberedBest() {
        assertEquals(100, IntegrityModel.baseline(0));
        assertEquals(100, IntegrityModel.baseline(256));
        assertTrue(IntegrityModel.baseline(5000) < 100);
        assertEquals(86, IntegrityModel.baseline(1_000_000));
    }

    @Test
    void sleepingWorldDoesNotForget() {
        SplittableRandom r = new SplittableRandom(1);
        int v = 100;
        for (int i = 0; i < 10_000; i++) {
            v = IntegrityModel.step(v, 100, 5, 0, 100, false, 0, r);
        }
        assertEquals(100, v);
    }

    @Test
    void awakeWorldForgetsButAnchorsHold() {
        SplittableRandom r = new SplittableRandom(2);
        int v = 100;
        for (int i = 0; i < 2_000; i++) {
            v = IntegrityModel.step(v, 100, 95, 0, 100, false, 0, r);
        }
        assertTrue(v < 85, "an awake world should forget: " + v);
        assertEquals(100, IntegrityModel.step(3, 100, 95, 10, 0, true, 0, r));
    }

    @Test
    void paradoxCostsIntegrityAndFloorHolds() {
        SplittableRandom r = new SplittableRandom(3);
        assertEquals(70, IntegrityModel.step(90, 100, 0, 20, 100, false, 0, r));
        assertEquals(60, IntegrityModel.step(90, 100, 0, 80, 100, false, 60, r));
    }

    @Test
    void unwrittenSpreads() {
        SplittableRandom r = new SplittableRandom(4);
        int v = 80;
        for (int i = 0; i < 200; i++) {
            v = IntegrityModel.step(v, 100, 0, 0, 0, false, 0, r);
        }
        assertTrue(v < 40, "Unmade neighbours should eat this chunk: " + v);
    }
}
