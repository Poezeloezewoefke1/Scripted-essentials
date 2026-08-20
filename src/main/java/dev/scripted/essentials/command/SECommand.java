package dev.scripted.essentials.command;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Base class for every command in the plugin.
 *
 * <p>Handles the four checks each command would otherwise repeat: is the owning feature switched
 * on, does the sender hold the permission, is this console-safe, and did the body throw.
 */
public abstract class SECommand extends Command implements PluginIdentifiableCommand {

    protected final ScriptedEssentials plugin;
    private Feature owner;
    private boolean playerOnly;

    protected SECommand(ScriptedEssentials plugin, String name, String... aliases) {
        // Two things matter here.
        //
        // The aliases go through the superclass constructor rather than setAliases(), which would
        // publish a partly built `this` to Bukkit before the subclass has finished initialising.
        //
        // The list must be mutable. When an alias collides with a command another plugin already
        // owns, CraftBukkit's SimpleCommandMap drops it by calling iterator.remove() on this very
        // list, so an immutable List.of(...) would throw and take the whole plugin down at
        // startup. Aliases like /v, /cc and /ec collide often enough for that to be a matter of
        // time rather than bad luck.
        super(name, "", "/" + name, new ArrayList<>(Arrays.asList(aliases)));
        this.plugin = plugin;
    }

    /** Binds this command to a feature, so it is refused while that feature is off. */
    public SECommand owner(Feature feature) {
        this.owner = feature;
        if (getPermission() == null) {
            setPermission(feature.definition().permission());
        }
        return this;
    }

    public SECommand playerOnly() {
        this.playerOnly = true;
        return this;
    }

    public SECommand permission(String permission) {
        setPermission(permission);
        return this;
    }

    public SECommand describe(String description, String usage) {
        setDescription(description);
        setUsage(usage);
        return this;
    }

    @Override
    public final boolean execute(CommandSender sender, String label, String[] args) {
        if (owner != null && !owner.isEnabled()) {
            plugin.messages().send(sender, "feature-disabled",
                    Text.placeholder("feature", owner.definition().displayName()));
            return true;
        }
        String permission = getPermission();
        if (permission != null && !sender.hasPermission(permission)) {
            plugin.messages().send(sender, "no-permission", Text.placeholder("permission", permission));
            return true;
        }
        if (playerOnly && !(sender instanceof Player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        try {
            run(sender, args);
        } catch (CommandException e) {
            sender.sendMessage(plugin.messages().prefix().append(e.reason()));
        } catch (RuntimeException e) {
            plugin.messages().send(sender, "command-error");
            plugin.getLogger().log(Level.SEVERE, "Error running /" + getName(), e);
        }
        return true;
    }

    @Override
    public final List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (owner != null && !owner.isEnabled()) {
            return List.of();
        }
        String permission = getPermission();
        if (permission != null && !sender.hasPermission(permission)) {
            return List.of();
        }
        return complete(sender, args);
    }

    /** The body of the command. Throw {@link CommandException} to show the sender a message. */
    protected abstract void run(CommandSender sender, String[] args);

    protected List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    // ---- helpers available to every command ------------------------------------------------

    /** Aborts the command and shows the sender a message from messages.yml. */
    protected CommandException fail(String messageKey,
                                    net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
        return new CommandException(plugin.messages().raw(messageKey, resolvers));
    }

    /** The sender as a player. Only call this on a {@link #playerOnly()} command. */
    protected Player asPlayer(CommandSender sender) {
        return (Player) sender;
    }

    /** Resolves an online player by name, or aborts with {@code player-not-found}. */
    protected Player requireOnline(String name) {
        Player found = Bukkit.getPlayerExact(name);
        if (found == null) {
            throw fail("player-not-found", Text.placeholder("player", name));
        }
        return found;
    }

    /**
     * Resolves the target of a command that takes an optional trailing player name: the named
     * player if one was given, otherwise the sender.
     */
    protected Player resolveTarget(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            return requireOnline(args[index]);
        }
        if (sender instanceof Player player) {
            return player;
        }
        throw fail("console-needs-target");
    }

    protected static List<String> filter(String prefix, Collection<String> options) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }

    /** Online player names the sender is allowed to see, filtered by the argument being typed. */
    protected List<String> onlineNames(CommandSender sender, String prefix) {
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player viewer && !viewer.canSee(online)) {
                continue;
            }
            names.add(online.getName());
        }
        return filter(prefix, names);
    }
}
