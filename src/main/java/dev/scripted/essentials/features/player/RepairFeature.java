package dev.scripted.essentials.features.player;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Sounds;
import dev.scripted.essentials.util.Text;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** {@code /repair} — restores durability on the held item, or on everything carried. */
public final class RepairFeature extends Feature {

    public RepairFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("repair")
                .name("Repair")
                .description("Repairs the item in hand, or every damaged item you are carrying.")
                .icon(Material.ANVIL)
                .category(FeatureCategory.PLAYER)
                .controls("/repair", "/repair all")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "repair", "fix") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                boolean all = args.length > 0 && args[0].equalsIgnoreCase("all");

                int repaired = 0;
                if (all) {
                    for (ItemStack item : player.getInventory().getContents()) {
                        repaired += repair(item) ? 1 : 0;
                    }
                    for (ItemStack item : player.getInventory().getArmorContents()) {
                        repaired += repair(item) ? 1 : 0;
                    }
                } else {
                    repaired += repair(player.getInventory().getItemInMainHand()) ? 1 : 0;
                }

                if (repaired == 0) {
                    throw fail("repair-nothing");
                }
                player.updateInventory();
                Sounds.play(plugin, player, "block.anvil.use", 0.6f, 1.4f);
                plugin.messages().send(sender, "repair-done",
                        Text.placeholder("count", String.valueOf(repaired)));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], List.of("all")) : List.of();
            }
        }.playerOnly().describe("Repair your items.", "/repair [all]"));
    }

    /** Returns true if the item was damaged and has now been restored. */
    private boolean repair(ItemStack item) {
        if (item == null || item.getType().getMaxDurability() <= 0) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable) || !damageable.hasDamage()) {
            return false;
        }
        damageable.setDamage(0);
        item.setItemMeta(meta);
        return true;
    }
}
