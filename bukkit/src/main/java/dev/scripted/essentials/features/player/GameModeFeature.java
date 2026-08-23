package dev.scripted.essentials.features.player;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** {@code /gm} and the four shorthands. */
public final class GameModeFeature extends Feature {

    private static final Map<String, GameMode> ALIASES = Map.ofEntries(
            Map.entry("c", GameMode.CREATIVE), Map.entry("1", GameMode.CREATIVE),
            Map.entry("creative", GameMode.CREATIVE),
            Map.entry("s", GameMode.SURVIVAL), Map.entry("0", GameMode.SURVIVAL),
            Map.entry("survival", GameMode.SURVIVAL),
            Map.entry("a", GameMode.ADVENTURE), Map.entry("2", GameMode.ADVENTURE),
            Map.entry("adventure", GameMode.ADVENTURE),
            Map.entry("sp", GameMode.SPECTATOR), Map.entry("3", GameMode.SPECTATOR),
            Map.entry("spectator", GameMode.SPECTATOR));

    public GameModeFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("gamemode")
                .name("Gamemodes")
                .description("Switch game mode, with a shorthand command for each mode.")
                .icon(Material.GRASS_BLOCK)
                .category(FeatureCategory.PLAYER)
                .controls("/gm", "/gmc", "/gms", "/gma", "/gmsp")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "gamemode", "gm") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/gm <mode> [player]"));
                }
                GameMode mode = ALIASES.get(args[0].toLowerCase(Locale.ROOT));
                if (mode == null) {
                    throw fail("gamemode-unknown", Text.placeholder("input", args[0]));
                }
                apply(sender, resolveTarget(sender, args, 1), mode);
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(args[0], List.of("survival", "creative", "adventure", "spectator"));
                }
                return args.length == 2 ? onlineNames(sender, args[1]) : List.of();
            }
        }.describe("Change game mode.", "/gm <mode> [player]"));

        shorthand("gmc", GameMode.CREATIVE);
        shorthand("gms", GameMode.SURVIVAL);
        shorthand("gma", GameMode.ADVENTURE);
        shorthand("gmsp", GameMode.SPECTATOR);
    }

    private void shorthand(String name, GameMode mode) {
        command(new SECommand(plugin, name) {
            @Override
            protected void run(CommandSender sender, String[] args) {
                apply(sender, resolveTarget(sender, args, 0), mode);
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Switch to " + mode.name().toLowerCase(Locale.ROOT) + " mode.", "/" + name + " [player]"));
    }

    private void apply(CommandSender sender, Player target, GameMode mode) {
        target.setGameMode(mode);
        String pretty = Text.prettify(mode.name());
        if (sender == target) {
            plugin.messages().send(sender, "gamemode-set-self", Text.placeholder("mode", pretty));
        } else {
            plugin.messages().send(sender, "gamemode-set-other",
                    Text.placeholder("player", target.getName()), Text.placeholder("mode", pretty));
            plugin.messages().send(target, "gamemode-set-self", Text.placeholder("mode", pretty));
        }
    }
}
