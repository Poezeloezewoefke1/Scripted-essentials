package dev.scripted.essentials.features.player;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Feedback;
import dev.scripted.essentials.util.Sounds;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.List;

/** {@code /heal} — full health, fed, no fire, no lingering effects. */
public final class HealFeature extends Feature {

    public HealFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("heal")
                .name("Heal")
                .description("Restores health and hunger, puts out fire and clears potion effects.")
                .icon(Material.GOLDEN_APPLE)
                .category(FeatureCategory.PLAYER)
                .controls("/heal")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "heal") {
            @Override
            @SuppressWarnings("deprecation") // getMaxHealth is the version-stable spelling
            protected void run(CommandSender sender, String[] args) {
                Player target = resolveTarget(sender, args, 0);
                target.setHealth(target.getMaxHealth());
                target.setFoodLevel(20);
                target.setSaturation(20f);
                target.setFireTicks(0);
                for (PotionEffect effect : target.getActivePotionEffects()) {
                    target.removePotionEffect(effect.getType());
                }
                Sounds.play(plugin, target, Sounds.LEVEL_UP, 0.6f, 1.4f);
                Feedback.report(plugin, sender, target, "heal-self", "heal-other", "heal-received");
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Fully restore a player.", "/heal [player]"));
    }
}
