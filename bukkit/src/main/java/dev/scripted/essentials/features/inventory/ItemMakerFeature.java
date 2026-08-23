package dev.scripted.essentials.features.inventory;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /itemmaker} — a menu for editing the item in your hand. */
public final class ItemMakerFeature extends Feature {

    public ItemMakerFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("itemmaker")
                .name("Custom Item Maker")
                .description("Rename items, write lore, add enchantments and set flags from a menu.")
                .icon(Material.CRAFTING_TABLE)
                .category(FeatureCategory.INVENTORY)
                .controls("/itemmaker")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "itemmaker", "im", "itemedit") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                    throw fail("itemmaker-empty-hand");
                }
                new ItemMakerMenu(plugin, player).open();
            }
        }.playerOnly().describe("Edit the item in your hand.", "/itemmaker"));
    }
}
