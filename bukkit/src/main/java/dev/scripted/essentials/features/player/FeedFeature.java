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

import java.util.List;

/** {@code /feed} — refills the hunger and saturation bars. */
public final class FeedFeature extends Feature {

    public FeedFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("feed")
                .name("Feed")
                .description("Refills a player's hunger and saturation.")
                .icon(Material.COOKED_BEEF)
                .category(FeatureCategory.PLAYER)
                .controls("/feed")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "feed") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player target = resolveTarget(sender, args, 0);
                target.setFoodLevel(20);
                target.setSaturation(20f);
                target.setExhaustion(0f);
                Sounds.play(plugin, target, "entity.player.burp", 0.6f, 1.2f);
                Feedback.report(plugin, sender, target, "feed-self", "feed-other", "feed-received");
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Refill a player's hunger bar.", "/feed [player]"));
    }
}
