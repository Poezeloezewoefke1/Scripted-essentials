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

/** {@code /invsee} — opens another player's live inventory. */
public final class InvseeFeature extends Feature {

    public InvseeFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("invsee")
                .name("Inventory See")
                .description("Opens another player's inventory. Edits apply to them immediately.")
                .icon(Material.CHEST)
                .category(FeatureCategory.INVENTORY)
                .controls("/invsee")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "invsee", "inv") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player viewer = asPlayer(sender);
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/invsee <player>"));
                }
                Player target = requireOnline(args[0]);
                if (target == viewer) {
                    throw fail("invsee-self");
                }
                viewer.openInventory(target.getInventory());
                plugin.messages().send(sender, "invsee-opened",
                        Text.placeholder("player", target.getName()));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.playerOnly().describe("View a player's inventory.", "/invsee <player>"));
    }
}
