package dev.scripted.essentials.features.systems;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.gui.ItemBuilder;
import dev.scripted.essentials.util.Effects;
import dev.scripted.essentials.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Locale;

/**
 * Runs a configured list of actions whenever a player dies.
 *
 * <p>Each entry is {@code type:value}. Supported types are {@code message}, {@code command} (run
 * as the player), {@code console}, {@code broadcast}, {@code effect}, {@code lightning} and
 * {@code drop-head}. Placeholders {@code <player>}, {@code <killer>}, {@code <world>},
 * {@code <x>}, {@code <y>} and {@code <z>} are filled in first.
 */
public final class DeathActionsFeature extends Feature {

    public DeathActionsFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("deathactions")
                .name("On-Death Actions")
                .description("Runs commands, effects and messages of your choosing when a player dies.")
                .icon(Material.SKELETON_SKULL)
                .category(FeatureCategory.SYSTEMS)
                .controls("/deathactions")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "deathactions") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                List<String> actions = plugin.getConfig().getStringList("death-actions.actions");
                plugin.messages().send(sender, "deathactions-list", Text.placeholder("actions",
                        actions.isEmpty() ? "none" : String.join(" | ", actions)));
            }
        }.describe("Show the configured death actions.", "/deathactions"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onDeath(PlayerDeathEvent event) {
                if (!isEnabled()) {
                    return;
                }
                Player victim = event.getEntity();
                Player killer = victim.getKiller();
                Location where = victim.getLocation();

                for (String raw : plugin.getConfig().getStringList("death-actions.actions")) {
                    try {
                        run(raw, victim, killer, where, event);
                    } catch (RuntimeException e) {
                        plugin.getLogger().warning("Death action '" + raw + "' failed: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void run(String raw, Player victim, Player killer, Location where, PlayerDeathEvent event) {
        int split = raw.indexOf(':');
        String type = (split < 0 ? raw : raw.substring(0, split)).toLowerCase(Locale.ROOT).trim();
        String value = split < 0 ? "" : fill(raw.substring(split + 1), victim, killer, where);

        switch (type) {
            case "message" -> victim.sendMessage(Text.parse(value));
            case "broadcast" -> Bukkit.broadcast(Text.parse(value));
            case "command" -> victim.performCommand(value);
            case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value);
            case "lightning" -> where.getWorld().strikeLightningEffect(where);
            case "drop-head" -> event.getDrops().add(ItemBuilder.of(Material.PLAYER_HEAD)
                    .skull(victim)
                    .name("<yellow>" + victim.getName() + "<gray>'s Head")
                    .build());
            case "effect" -> {
                PotionEffect effect = Effects.parse(value);
                if (effect != null) {
                    // Applied a tick later: effects set during the death event are wiped on respawn.
                    plugin.getServer().getScheduler().runTaskLater(plugin,
                            () -> victim.addPotionEffect(effect), 2L);
                }
            }
            default -> plugin.getLogger().warning("Unknown death action type: " + type);
        }
    }

    private String fill(String value, Player victim, Player killer, Location where) {
        return value.replace("<player>", victim.getName())
                .replace("<killer>", killer == null ? "the world" : killer.getName())
                .replace("<world>", where.getWorld().getName())
                .replace("<x>", String.valueOf(where.getBlockX()))
                .replace("<y>", String.valueOf(where.getBlockY()))
                .replace("<z>", String.valueOf(where.getBlockZ()));
    }
}
