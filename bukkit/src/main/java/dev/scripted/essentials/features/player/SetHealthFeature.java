package dev.scripted.essentials.features.player;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Numbers;
import dev.scripted.essentials.util.Text;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /sethealth} — sets health in half-hearts, clamped to the player's maximum. */
public final class SetHealthFeature extends Feature {

    public SetHealthFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("sethealth")
                .name("Set Health")
                .description("Sets a player's health to an exact number of half-hearts.")
                .icon(Material.RED_DYE)
                .category(FeatureCategory.PLAYER)
                .controls("/sethealth")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "sethealth", "health") {
            @Override
            @SuppressWarnings("deprecation")
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/sethealth <amount> [player]"));
                }
                double amount = Numbers.parseDouble(args[0])
                        .orElseThrow(() -> fail("invalid-number", Text.placeholder("input", args[0])));
                Player target = resolveTarget(sender, args, 1);

                double clamped = Numbers.clamp(amount, 0.5, target.getMaxHealth());
                target.setHealth(clamped);
                plugin.messages().send(sender, "health-set",
                        Text.placeholder("player", target.getName()),
                        Text.placeholder("value", String.valueOf(clamped)));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(args[0], List.of("1", "5", "10", "20"));
                }
                return args.length == 2 ? onlineNames(sender, args[1]) : List.of();
            }
        }.describe("Set a player's health.", "/sethealth <amount> [player]"));
    }
}
