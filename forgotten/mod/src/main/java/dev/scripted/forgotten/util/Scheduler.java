package dev.scripted.forgotten.util;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Runs things a number of ticks from now. Anomalies use it to undo themselves: the tree that
 * became a sheep becomes a tree again, the gravity flip wears off.
 */
public final class Scheduler {
    private record Task(long at, long order, Runnable action) {
    }

    private static final PriorityQueue<Task> QUEUE = new PriorityQueue<>((a, b) ->
            a.at != b.at ? Long.compare(a.at, b.at) : Long.compare(a.order, b.order));
    private static long ticks;
    private static long counter;

    private Scheduler() {
    }

    public static void after(int delay, Runnable action) {
        QUEUE.add(new Task(ticks + Math.max(1, delay), counter++, action));
    }

    public static void tick() {
        ticks++;
        List<Task> due = new ArrayList<>();
        while (!QUEUE.isEmpty() && QUEUE.peek().at <= ticks) {
            due.add(QUEUE.poll());
        }
        for (Task t : due) {
            try {
                t.action.run();
            } catch (RuntimeException e) {
                dev.scripted.forgotten.Forgotten.LOG.warn("Scheduled task failed", e);
            }
        }
    }

    /** Drops everything pending, when the server stops. */
    public static void clear() {
        QUEUE.clear();
    }
}
