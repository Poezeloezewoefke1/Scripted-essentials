package dev.scripted.essentials.features.player;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /totem} — hands out totems, and optionally keeps one in the off hand automatically.
 *
 * <p>With auto-totem on, a hit that would be fatal moves a spare totem from the inventory into
 * the off hand before the damage resolves, so the vanilla totem check finds it.
 */
public final class TotemFeature extends Feature {

    private final Set<UUID> autoTotem = new HashSet<>();

    public TotemFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("totem")
                .name("Totem")
                .description("Gives totems of undying, and can auto-swap one into your off hand.")
                .icon(Material.TOTEM_OF_UNDYING)
                .category(FeatureCategory.PLAYER)
                .controls("/totem", "/totem auto")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "totem") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length > 0 && args[0].equalsIgnoreCase("auto")) {
                    Player player = sender instanceof Player p ? p : null;
                    if (player == null) {
                        throw fail("player-only");
                    }
                    boolean enabled = !autoTotem.remove(player.getUniqueId());
                    if (enabled) {
                        autoTotem.add(player.getUniqueId());
                    }
                    plugin.messages().send(sender, enabled ? "totem-auto-on" : "totem-auto-off");
                    return;
                }

                Player target = resolveTarget(sender, args, 0);
                target.getInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING)).values()
                        .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
                plugin.messages().send(sender, "totem-given",
                        Text.placeholder("player", target.getName()));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    List<String> options = new java.util.ArrayList<>(onlineNames(sender, args[0]));
                    options.addAll(filter(args[0], List.of("auto")));
                    return options;
                }
                return List.of();
            }
        }.describe("Give a totem, or toggle auto-totem.", "/totem [player|auto]"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onDamage(EntityDamageEvent event) {
                if (!isEnabled() || !(event.getEntity() instanceof Player player)) {
                    return;
                }
                if (!autoTotem.contains(player.getUniqueId())) {
                    return;
                }
                if (player.getHealth() - event.getFinalDamage() > 0) {
                    return;
                }
                swapInTotem(player);
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                autoTotem.remove(event.getPlayer().getUniqueId());
            }
        });
    }

    @Override
    protected void onDisable() {
        autoTotem.clear();
    }

    private void swapInTotem(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (inventory.getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
            return;
        }
        int slot = inventory.first(Material.TOTEM_OF_UNDYING);
        if (slot < 0) {
            return;
        }
        ItemStack totem = inventory.getItem(slot);
        if (totem == null) {
            return;
        }
        ItemStack single = totem.clone();
        single.setAmount(1);

        if (totem.getAmount() > 1) {
            totem.setAmount(totem.getAmount() - 1);
            inventory.setItem(slot, totem);
        } else {
            inventory.setItem(slot, null);
        }
        ItemStack displaced = inventory.getItemInOffHand();
        inventory.setItemInOffHand(single);
        if (displaced.getType() != Material.AIR) {
            inventory.addItem(displaced).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }
}
