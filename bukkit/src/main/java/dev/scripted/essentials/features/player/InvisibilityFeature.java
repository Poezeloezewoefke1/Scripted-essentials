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
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/** {@code /invis} — a permanent invisibility effect that survives milk and death. */
public final class InvisibilityFeature extends Feature {

    public InvisibilityFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("invisibility")
                .name("Invisibility")
                .description("Toggles an endless, particle-free invisibility effect.")
                .icon(Material.GLASS)
                .category(FeatureCategory.PLAYER)
                .controls("/invis")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "invis", "invisible") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player target = resolveTarget(sender, args, 0);
                PotionEffectType invisibility = Effects.byKey("invisibility");
                if (invisibility == null) {
                    throw fail("effect-unavailable", Text.placeholder("effect", "invisibility"));
                }

                boolean enable = !target.hasPotionEffect(invisibility);
                if (enable) {
                    target.addPotionEffect(
                            new PotionEffect(invisibility, Effects.INFINITE, 0, false, false, false));
                } else {
                    target.removePotionEffect(invisibility);
                }

                if (sender == target) {
                    plugin.messages().send(sender, enable ? "invis-on" : "invis-off");
                } else {
                    plugin.messages().send(sender, enable ? "invis-on-other" : "invis-off-other",
                            Text.placeholder("player", target.getName()));
                    plugin.messages().send(target, enable ? "invis-on" : "invis-off");
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Toggle invisibility.", "/invis [player]"));
    }
}
