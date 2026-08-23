package dev.scripted.essentials.command;

import dev.scripted.essentials.ScriptedEssentials;
import org.bukkit.command.CommandMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers commands into the live command map at runtime.
 *
 * <p>Commands are declared in code next to the feature that owns them rather than in plugin.yml,
 * so adding a feature never means editing two files. Every command is registered under the
 * {@code scriptedessentials} fallback prefix, which means a conflicting plugin can still be
 * reached explicitly as {@code /scriptedessentials:heal}.
 */
public final class CommandRegistry {

    private static final String FALLBACK_PREFIX = "scriptedessentials";

    private final ScriptedEssentials plugin;
    private final CommandMap commandMap;
    private final List<SECommand> registered = new ArrayList<>();

    public CommandRegistry(ScriptedEssentials plugin) {
        this.plugin = plugin;
        this.commandMap = resolveCommandMap(plugin);
    }

    /**
     * {@code Server#getCommandMap()} is Paper API. On Spigot the call is missing entirely and the
     * JVM raises an Error rather than an Exception, which would surface as an unreadable stack
     * trace at startup. Catching it lets the plugin say what is actually wrong.
     */
    private static CommandMap resolveCommandMap(ScriptedEssentials plugin) {
        try {
            return plugin.getServer().getCommandMap();
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            throw new IllegalStateException(
                    "ScriptedEssentials needs Paper (or a Paper fork such as Purpur). "
                            + "This server does not provide Server#getCommandMap().", e);
        }
    }

    public void register(SECommand command) {
        commandMap.register(FALLBACK_PREFIX, command);
        registered.add(command);
    }

    public List<SECommand> registered() {
        return List.copyOf(registered);
    }

    public ScriptedEssentials plugin() {
        return plugin;
    }
}
