package dev.scripted.essentials.features.player;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Feedback;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.List;

/** {@code /noeffects} — strips every active potion effect. */
public final class NoEffectsFeature extends Feature {

    public NoEffectsFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("noeffects")
                .name("No Effects")
                .description("Removes every active potion effect from a player.")
                .icon(Material.MILK_BUCKET)
                .category(FeatureCategory.PLAYER)
                .controls("/noeffects")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "noeffects", "cleareffects") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player target = resolveTarget(sender, args, 0);
                for (PotionEffect effect : target.getActivePotionEffects()) {
                    target.removePotionEffect(effect.getType());
                }
                Feedback.report(plugin, sender, target,
                        "effects-cleared-self", "effects-cleared-other", "effects-cleared-received");
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Clear all potion effects.", "/noeffects [player]"));
    }
}
