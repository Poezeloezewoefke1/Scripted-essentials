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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** {@code /durability} — sets the held item's remaining durability as a percentage. */
public final class DurabilityFeature extends Feature {

    public DurabilityFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("durability")
                .name("Durability")
                .description("Sets the held item's remaining durability to an exact percentage.")
                .icon(Material.IRON_PICKAXE)
                .category(FeatureCategory.PLAYER)
                .controls("/durability")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "durability", "dura") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/durability <0-100|max>"));
                }
                ItemStack item = player.getInventory().getItemInMainHand();
                short maxDurability = (short) item.getType().getMaxDurability();
                if (item.getType() == Material.AIR || maxDurability <= 0) {
                    throw fail("durability-not-damageable");
                }

                int percent = args[0].equalsIgnoreCase("max") ? 100
                        : Numbers.parseInt(args[0])
                        .orElseThrow(() -> fail("invalid-number", Text.placeholder("input", args[0])));
                percent = Numbers.clamp(percent, 1, 100);

                ItemMeta meta = item.getItemMeta();
                if (!(meta instanceof Damageable damageable)) {
                    throw fail("durability-not-damageable");
                }
                damageable.setDamage(maxDurability - (maxDurability * percent / 100));
                item.setItemMeta(meta);
                player.updateInventory();

                plugin.messages().send(sender, "durability-set",
                        Text.placeholder("value", String.valueOf(percent)));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], List.of("25", "50", "75", "100", "max")) : List.of();
            }
        }.playerOnly().describe("Set the held item's durability.", "/durability <0-100|max>"));
    }
}
