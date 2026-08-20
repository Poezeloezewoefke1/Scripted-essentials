package dev.scripted.essentials.command;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.gui.FeatureMenu;
import dev.scripted.essentials.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** {@code /se} — the control panel, the toggle commands, and the reload. */
public final class RootCommand extends SECommand {

    private static final List<String> SUBCOMMANDS =
            List.of("gui", "list", "toggle", "enable", "disable", "reload", "version");

    public RootCommand(ScriptedEssentials plugin) {
        super(plugin, "se", "scriptedessentials", "essentials");
        permission("scriptedessentials.admin");
        describe("Control panel for ScriptedEssentials.", "/se [gui|list|toggle|reload]");
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player player)) {
                throw fail("player-only");
            }
            new FeatureMenu(plugin, player).open();
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadEverything();
                plugin.messages().send(sender, "reloaded");
            }
            case "version" -> plugin.messages().send(sender, "version",
                    Text.placeholder("version", plugin.getPluginMeta().getVersion()));
            case "list" -> list(sender);
            case "toggle" -> setState(sender, args, null);
            case "enable" -> setState(sender, args, true);
            case "disable" -> setState(sender, args, false);
            default -> plugin.messages().send(sender, "unknown-subcommand",
                    Text.placeholder("input", args[0]));
        }
    }

    private void list(CommandSender sender) {
        sender.sendMessage(plugin.messages().raw("feature-list-header"));
        for (Feature feature : plugin.features().all()) {
            sender.sendMessage(plugin.messages().raw(
                    feature.isEnabled() ? "feature-list-entry-on" : "feature-list-entry-off",
                    Text.placeholder("id", feature.id()),
                    Text.placeholder("name", feature.definition().displayName())));
        }
    }

    private void setState(CommandSender sender, String[] args, Boolean target) {
        if (args.length < 2) {
            throw fail("usage", Text.placeholder("usage", "/se " + args[0] + " <feature>"));
        }
        Feature feature = plugin.features().get(args[1])
                .orElseThrow(() -> fail("unknown-feature", Text.placeholder("id", args[1])));

        boolean newState = target != null ? target : !feature.isEnabled();
        plugin.features().setEnabled(feature, newState);
        plugin.messages().send(sender, newState ? "feature-enabled" : "feature-disabled-ok",
                Text.placeholder("name", feature.definition().displayName()));
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(args[0], SUBCOMMANDS);
        }
        if (args.length == 2 && List.of("toggle", "enable", "disable").contains(
                args[0].toLowerCase(Locale.ROOT))) {
            List<String> ids = new ArrayList<>();
            plugin.features().all().forEach(feature -> ids.add(feature.id()));
            return filter(args[1], ids);
        }
        return List.of();
    }
}
