package dev.scripted.essentials.features.moderation;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hides chosen commands from players without the bypass permission.
 *
 * <p>Matching normalises the plugin prefix, so blocking {@code /pl} also blocks
 * {@code /bukkit:pl}, which is the usual way people get around a naive blocker.
 */
public final class CommandBlockerFeature extends Feature {

    private static final String BYPASS = "scriptedessentials.bypass";

    public CommandBlockerFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("commandblocker")
                .name("Command Blocker")
                .description("Blocks listed commands, including their plugin-prefixed forms.")
                .icon(Material.BARRIER)
                .category(FeatureCategory.MODERATION)
                .controls("/cmdblock")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "cmdblock", "commandblock") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                List<String> blocked = blockedCommands();
                if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                    plugin.messages().send(sender, "cmdblock-list", Text.placeholder("commands",
                            blocked.isEmpty() ? "none" : String.join(", ", blocked)));
                    return;
                }
                if (args.length < 2) {
                    throw fail("usage", Text.placeholder("usage", "/cmdblock <add|remove|list> [command]"));
                }
                String target = normalise(args[1]);

                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "add" -> {
                        if (blocked.contains(target)) {
                            throw fail("cmdblock-already", Text.placeholder("command", target));
                        }
                        blocked.add(target);
                        save(blocked);
                        plugin.messages().send(sender, "cmdblock-added",
                                Text.placeholder("command", target));
                    }
                    case "remove" -> {
                        if (!blocked.remove(target)) {
                            throw fail("cmdblock-missing", Text.placeholder("command", target));
                        }
                        save(blocked);
                        plugin.messages().send(sender, "cmdblock-removed",
                                Text.placeholder("command", target));
                    }
                    default -> throw fail("usage",
                            Text.placeholder("usage", "/cmdblock <add|remove|list> [command]"));
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(args[0], List.of("add", "remove", "list"));
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                    return filter(args[1], blockedCommands());
                }
                return List.of();
            }
        }.describe("Manage the blocked command list.", "/cmdblock <add|remove|list> [command]"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onCommand(PlayerCommandPreprocessEvent event) {
                if (!isEnabled() || event.getPlayer().hasPermission(BYPASS)) {
                    return;
                }
                String typed = normalise(event.getMessage().split(" ")[0]);
                if (!blockedCommands().contains(typed)) {
                    return;
                }
                event.setCancelled(true);
                event.getPlayer().sendMessage(Text.parse(
                        plugin.getConfig().getString("command-blocker.message",
                                "<red>Unknown command.")));
            }
        });
    }

    private void save(List<String> blocked) {
        plugin.getConfig().set("command-blocker.blocked", blocked);
        plugin.saveConfig();
    }

    private List<String> blockedCommands() {
        List<String> normalised = new ArrayList<>();
        for (String raw : plugin.getConfig().getStringList("command-blocker.blocked")) {
            normalised.add(normalise(raw));
        }
        return normalised;
    }

    /** Strips the leading slash and any {@code plugin:} prefix, and lowercases. */
    private static String normalise(String command) {
        String value = command.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(colon + 1);
        }
        return value;
    }
}
