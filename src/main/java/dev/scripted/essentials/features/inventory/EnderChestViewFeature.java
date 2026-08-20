package dev.scripted.essentials.features.inventory;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /echest} — your own ender chest, or somebody else's. */
public final class EnderChestViewFeature extends Feature {

    public EnderChestViewFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("enderchest")
                .name("Ender Chest View")
                .description("Opens your ender chest, or another player's.")
                .icon(Material.ENDER_CHEST)
                .category(FeatureCategory.INVENTORY)
                .controls("/echest")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "echest", "enderchest", "ec") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player viewer = asPlayer(sender);
                Player target = resolveTarget(sender, args, 0);

                if (target != viewer && !viewer.hasPermission("scriptedessentials.enderchest.others")) {
                    throw fail("no-permission",
                            Text.placeholder("permission", "scriptedessentials.enderchest.others"));
                }
                viewer.openInventory(target.getEnderChest());
                if (target != viewer) {
                    plugin.messages().send(sender, "echest-opened",
                            Text.placeholder("player", target.getName()));
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.playerOnly().describe("Open an ender chest.", "/echest [player]"));
    }
}
