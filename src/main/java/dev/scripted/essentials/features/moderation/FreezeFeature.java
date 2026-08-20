package dev.scripted.essentials.features.moderation;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /freeze} — pins a player in place for a staff conversation.
 *
 * <p>Looking around still works, since only a change of block position is blocked. Frozen players
 * cannot run commands or take damage, and logging out while frozen is announced to staff.
 */
public final class FreezeFeature extends Feature {

    private final Set<UUID> frozen = new HashSet<>();
    private BukkitTask reminderTask;

    public FreezeFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("freeze")
                .name("Freeze")
                .description("Locks a player in place, blocks their commands, and warns staff if they log out.")
                .icon(Material.PACKED_ICE)
                .category(FeatureCategory.MODERATION)
                .controls("/freeze")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "freeze") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/freeze <player>"));
                }
                Player target = requireOnline(args[0]);
                boolean nowFrozen = !frozen.remove(target.getUniqueId());
                if (nowFrozen) {
                    frozen.add(target.getUniqueId());
                }

                plugin.messages().send(sender, nowFrozen ? "freeze-on" : "freeze-off",
                        Text.placeholder("player", target.getName()));
                plugin.messages().send(target, nowFrozen ? "freeze-target-on" : "freeze-target-off");
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Freeze or unfreeze a player.", "/freeze <player>"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
            public void onMove(PlayerMoveEvent event) {
                if (!isEnabled() || !frozen.contains(event.getPlayer().getUniqueId())) {
                    return;
                }
                Location from = event.getFrom();
                Location to = event.getTo();
                if (to == null) {
                    return;
                }
                // Only block movement between blocks; turning the head is fine.
                if (from.getBlockX() != to.getBlockX()
                        || from.getBlockY() != to.getBlockY()
                        || from.getBlockZ() != to.getBlockZ()) {
                    event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(),
                            to.getYaw(), to.getPitch()));
                }
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onCommand(PlayerCommandPreprocessEvent event) {
                if (isEnabled() && frozen.contains(event.getPlayer().getUniqueId())) {
                    event.setCancelled(true);
                    plugin.messages().send(event.getPlayer(), "freeze-command-blocked");
                }
            }

            @EventHandler(ignoreCancelled = true)
            public void onDamage(EntityDamageEvent event) {
                if (isEnabled() && event.getEntity() instanceof Player player
                        && frozen.contains(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                if (!isEnabled() || !frozen.contains(event.getPlayer().getUniqueId())) {
                    return;
                }
                // Keep them frozen for when they come back, and tell staff they left.
                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission(definition().permission())) {
                        plugin.messages().send(staff, "freeze-logout",
                                Text.placeholder("player", event.getPlayer().getName()));
                    }
                }
            }
        });
    }

    @Override
    protected void onEnable() {
        int interval = Math.max(1, plugin.getConfig().getInt("freeze.reminder-interval", 5));
        reminderTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (frozen.isEmpty()) {
                return;
            }
            String reminder = plugin.getConfig().getString("freeze.reminder",
                    "<red>You are frozen. Do not log out.");
            for (UUID id : frozen) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    player.sendMessage(Text.parse(reminder));
                }
            }
        }, interval * 20L, interval * 20L);
    }

    @Override
    protected void onDisable() {
        if (reminderTask != null) {
            reminderTask.cancel();
            reminderTask = null;
        }
        frozen.clear();
    }

    public boolean isFrozen(Player player) {
        return frozen.contains(player.getUniqueId());
    }
}
