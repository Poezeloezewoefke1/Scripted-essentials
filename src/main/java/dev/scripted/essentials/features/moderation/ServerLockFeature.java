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
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.List;

/**
 * Closes the server to everyone without {@code scriptedessentials.bypass}.
 *
 * <p>Turning the lock on also kicks anyone already connected who does not hold the bypass, so
 * enabling it mid-session actually empties the server.
 */
public final class ServerLockFeature extends Feature {

    private static final String BYPASS = "scriptedessentials.bypass";

    public ServerLockFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("serverlock")
                .name("Server Lock")
                .description("Only staff may join, and the server list shows a locked message.")
                .icon(Material.IRON_DOOR)
                .category(FeatureCategory.MODERATION)
                .controls("/serverlock")
                .disabledByDefault()
                .build());
    }

    @Override
    protected void onRegister() {
        commandUnbound(new SECommand(plugin, "serverlock", "lockserver") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                boolean target = args.length > 0
                        ? args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("true")
                        : !isEnabled();
                setFeatureEnabled(target);
                plugin.messages().send(sender, target ? "serverlock-on" : "serverlock-off");
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], List.of("on", "off")) : List.of();
            }
        }.permission(definition().permission()).describe("Lock or unlock the server.", "/serverlock [on|off]"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.HIGH)
            public void onLogin(PlayerLoginEvent event) {
                if (isEnabled() && !event.getPlayer().hasPermission(BYPASS)) {
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Text.parse(
                            plugin.getConfig().getString("server-lock.kick-message",
                                    "<red>The server is locked. Try again later.")));
                }
            }

            @EventHandler
            public void onPing(ServerListPingEvent event) {
                if (isEnabled()) {
                    event.motd(Text.parse(plugin.getConfig().getString("server-lock.motd",
                            "<red>The server is currently locked.")));
                }
            }
        });
    }

    @Override
    protected void onEnable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission(BYPASS)) {
                player.kick(Text.parse(plugin.getConfig().getString("server-lock.kick-message",
                        "<red>The server is locked. Try again later.")));
            }
        }
    }
}
