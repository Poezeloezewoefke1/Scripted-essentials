package dev.scripted.essentials.features.moderation;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /vanish} — hides a player from everyone without the see-vanished permission.
 *
 * <p>Hiding through {@code hidePlayer} also removes the player from the tab list, and the join
 * and quit broadcasts are suppressed so vanishing looks like a normal logout to everyone else.
 */
public final class VanishFeature extends Feature {

    private static final String SEE_PERMISSION = "scriptedessentials.vanish.see";

    private final Set<UUID> vanished = new HashSet<>();

    public VanishFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("vanish")
                .name("Vanish")
                .description("Hides you from the world and the tab list, with silent join and quit.")
                .icon(Material.POTION)
                .category(FeatureCategory.MODERATION)
                .controls("/vanish")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "vanish", "v") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player target = resolveTarget(sender, args, 0);
                boolean nowVanished = !isVanished(target);
                setVanished(target, nowVanished);

                if (sender == target) {
                    plugin.messages().send(sender, nowVanished ? "vanish-on" : "vanish-off");
                } else {
                    plugin.messages().send(sender, nowVanished ? "vanish-on-other" : "vanish-off-other",
                            Text.placeholder("player", target.getName()));
                    plugin.messages().send(target, nowVanished ? "vanish-on" : "vanish-off");
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Toggle vanish.", "/vanish [player]"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.LOW)
            public void onJoin(PlayerJoinEvent event) {
                if (!isEnabled()) {
                    return;
                }
                // Someone joining must not be able to see anybody already vanished.
                for (UUID id : vanished) {
                    Player hidden = Bukkit.getPlayer(id);
                    if (hidden != null && !event.getPlayer().hasPermission(SEE_PERMISSION)) {
                        event.getPlayer().hidePlayer(plugin, hidden);
                    }
                }
                if (isVanished(event.getPlayer()) && silentJoinLeave()) {
                    event.joinMessage(null);
                    applyVisibility(event.getPlayer(), true);
                }
            }

            @EventHandler(priority = EventPriority.LOW)
            public void onQuit(PlayerQuitEvent event) {
                if (isEnabled() && isVanished(event.getPlayer()) && silentJoinLeave()) {
                    event.quitMessage(null);
                }
                // Keep the vanished set out of sync with reality no longer than the session.
                vanished.remove(event.getPlayer().getUniqueId());
            }

            @EventHandler(ignoreCancelled = true)
            public void onPickup(EntityPickupItemEvent event) {
                if (isEnabled() && event.getEntity() instanceof Player player && isVanished(player)) {
                    event.setCancelled(true);
                }
            }
        });
    }

    @Override
    protected void onDisable() {
        for (UUID id : Set.copyOf(vanished)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                setVanished(player, false);
            }
        }
        vanished.clear();
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public void setVanished(Player player, boolean vanish) {
        if (vanish) {
            vanished.add(player.getUniqueId());
        } else {
            vanished.remove(player.getUniqueId());
        }
        applyVisibility(player, vanish);
    }

    private void applyVisibility(Player player, boolean hidden) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) {
                continue;
            }
            if (hidden && !other.hasPermission(SEE_PERMISSION)) {
                other.hidePlayer(plugin, player);
            } else {
                other.showPlayer(plugin, player);
            }
        }
    }

    private boolean silentJoinLeave() {
        return plugin.getConfig().getBoolean("vanish.silent-join-leave", true);
    }
}
