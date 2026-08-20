package org.bukkit.scheduler;

import org.bukkit.plugin.Plugin;

public abstract class BukkitRunnable implements Runnable {
    public BukkitTask runTask(Plugin plugin) { throw new UnsupportedOperationException(); }
    public BukkitTask runTaskLater(Plugin plugin, long delay) { throw new UnsupportedOperationException(); }
    public BukkitTask runTaskTimer(Plugin plugin, long delay, long period) { throw new UnsupportedOperationException(); }
    public BukkitTask runTaskAsynchronously(Plugin plugin) { throw new UnsupportedOperationException(); }
    public void cancel() { }
    public boolean isCancelled() { throw new UnsupportedOperationException(); }
    public int getTaskId() { throw new UnsupportedOperationException(); }
}
