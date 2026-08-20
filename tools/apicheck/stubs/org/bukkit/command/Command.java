package org.bukkit.command;

import java.util.List;

public abstract class Command {
    protected Command(String name) { }
    protected Command(String name, String description, String usageMessage, List<String> aliases) { }
    public abstract boolean execute(CommandSender sender, String commandLabel, String[] args);
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) { throw new UnsupportedOperationException(); }
    public String getName() { throw new UnsupportedOperationException(); }
    public String getLabel() { throw new UnsupportedOperationException(); }
    public List<String> getAliases() { throw new UnsupportedOperationException(); }
    public String getPermission() { throw new UnsupportedOperationException(); }
    public void setPermission(String permission) { }
    public String getDescription() { throw new UnsupportedOperationException(); }
    public String getUsage() { throw new UnsupportedOperationException(); }
    public Command setAliases(List<String> aliases) { throw new UnsupportedOperationException(); }
    public Command setDescription(String description) { throw new UnsupportedOperationException(); }
    public Command setUsage(String usage) { throw new UnsupportedOperationException(); }
    public boolean unregister(CommandMap commandMap) { throw new UnsupportedOperationException(); }
}
