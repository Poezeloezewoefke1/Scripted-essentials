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

/** {@code /hunger} — sets the hunger bar to an exact value. */
public final class HungerFeature extends Feature {

    public HungerFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("hunger")
                .name("Hunger")
                .description("Sets a player's hunger bar to an exact value between 0 and 20.")
                .icon(Material.ROTTEN_FLESH)
                .category(FeatureCategory.PLAYER)
                .controls("/hunger")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "hunger") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/hunger <0-20> [player]"));
                }
                int level = Numbers.parseInt(args[0])
                        .orElseThrow(() -> fail("invalid-number", Text.placeholder("input", args[0])));
                Player target = resolveTarget(sender, args, 1);

                target.setFoodLevel(Numbers.clamp(level, 0, 20));
                target.setSaturation(0f);
                plugin.messages().send(sender, "hunger-set",
                        Text.placeholder("player", target.getName()),
                        Text.placeholder("value", String.valueOf(Numbers.clamp(level, 0, 20))));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(args[0], List.of("0", "5", "10", "15", "20"));
                }
                return args.length == 2 ? onlineNames(sender, args[1]) : List.of();
            }
        }.describe("Set a player's hunger level.", "/hunger <0-20> [player]"));
    }
}
