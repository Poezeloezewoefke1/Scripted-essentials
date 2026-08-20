package dev.scripted.essentials.features.player;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Effects;
import dev.scripted.essentials.util.Text;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** {@code /potion} — applies a named bundle of effects defined in config.yml. */
public final class PotionPresetsFeature extends Feature {

    public PotionPresetsFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("potionpresets")
                .name("Potion Presets")
                .description("Applies a named set of potion effects configured in config.yml.")
                .icon(Material.BREWING_STAND)
                .category(FeatureCategory.PLAYER)
                .controls("/potion")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "potion", "preset") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                    Set<String> names = presetNames();
                    if (names.isEmpty()) {
                        throw fail("potion-no-presets");
                    }
                    plugin.messages().send(sender, "potion-list",
                            Text.placeholder("presets", String.join(", ", names)));
                    return;
                }

                String preset = args[0].toLowerCase(Locale.ROOT);
                List<String> specs = plugin.getConfig().getStringList("potion-presets." + preset);
                if (specs.isEmpty()) {
                    throw fail("potion-unknown", Text.placeholder("input", args[0]));
                }
                Player target = resolveTarget(sender, args, 1);

                int applied = 0;
                for (String spec : specs) {
                    PotionEffect effect = Effects.parse(spec);
                    if (effect == null) {
                        plugin.getLogger().warning("Potion preset '" + preset
                                + "' has an unknown effect: " + spec);
                        continue;
                    }
                    target.addPotionEffect(effect);
                    applied++;
                }

                plugin.messages().send(sender, "potion-applied",
                        Text.placeholder("preset", preset),
                        Text.placeholder("player", target.getName()),
                        Text.placeholder("count", String.valueOf(applied)));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    List<String> options = new ArrayList<>(presetNames());
                    options.add("list");
                    return filter(args[0], options);
                }
                return args.length == 2 ? onlineNames(sender, args[1]) : List.of();
            }
        }.describe("Apply a potion preset.", "/potion <preset> [player]"));
    }

    private Set<String> presetNames() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("potion-presets");
        return section == null ? Set.of() : section.getKeys(false);
    }
}
