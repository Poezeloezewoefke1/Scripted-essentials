package dev.scripted.essentials.features.player;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.gui.ItemBuilder;
import dev.scripted.essentials.util.Enchants;
import dev.scripted.essentials.util.Sounds;
import dev.scripted.essentials.util.Text;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** {@code /fulltools} — hands out a fully enchanted netherite loadout. */
public final class FullToolsFeature extends Feature {

    public FullToolsFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("fulltools")
                .name("Full Tools")
                .description("Gives a fully enchanted, unbreakable netherite tool and armour set.")
                .icon(Material.NETHERITE_PICKAXE)
                .category(FeatureCategory.PLAYER)
                .controls("/fulltools")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "fulltools", "godset") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player target = resolveTarget(sender, args, 0);

                give(target, Material.NETHERITE_SWORD, Map.of(
                        Enchants.SHARPNESS, 5, Enchants.LOOTING, 3, Enchants.FIRE_ASPECT, 2));
                give(target, Material.NETHERITE_PICKAXE, Map.of(
                        Enchants.EFFICIENCY, 5, Enchants.FORTUNE, 3));
                give(target, Material.NETHERITE_AXE, Map.of(Enchants.EFFICIENCY, 5));
                give(target, Material.NETHERITE_SHOVEL, Map.of(Enchants.EFFICIENCY, 5));
                give(target, Material.NETHERITE_HELMET, Map.of(Enchants.PROTECTION, 4));
                give(target, Material.NETHERITE_CHESTPLATE, Map.of(Enchants.PROTECTION, 4));
                give(target, Material.NETHERITE_LEGGINGS, Map.of(Enchants.PROTECTION, 4));
                give(target, Material.NETHERITE_BOOTS, Map.of(
                        Enchants.PROTECTION, 4, Enchants.FEATHER_FALLING, 4, Enchants.DEPTH_STRIDER, 3));

                Sounds.play(plugin, target, Sounds.EQUIP, 0.8f, 1.0f);
                plugin.messages().send(sender, "fulltools-given",
                        Text.placeholder("player", target.getName()));
                if (sender != target) {
                    plugin.messages().send(target, "fulltools-received");
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Give a full enchanted netherite set.", "/fulltools [player]"));
    }

    private void give(Player player, Material material, Map<String, Integer> enchantments) {
        ItemBuilder builder = ItemBuilder.of(material).unbreakable().clean();
        builder.meta(meta -> enchantments.forEach((key, level) -> {
            Enchantment enchantment = Enchants.byKey(key);
            if (enchantment != null) {
                meta.addEnchant(enchantment, level, true);
            }
        }));
        ItemStack item = builder.build();
        // Drop anything that does not fit rather than silently swallowing it.
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
