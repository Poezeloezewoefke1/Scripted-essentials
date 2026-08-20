package org.bukkit.command;

public interface CommandMap {
    boolean register(String fallbackPrefix, Command command);
    boolean register(String label, String fallbackPrefix, Command command);
    Command getCommand(String name);
}
