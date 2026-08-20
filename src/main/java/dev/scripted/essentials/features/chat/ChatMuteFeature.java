package dev.scripted.essentials.features.chat;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Numbers;
import dev.scripted.essentials.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * Global chat lock plus per-player mutes.
 *
 * <p>Mutes are stored by UUID with an expiry timestamp, so they outlive a reconnect and a restart
 * and lift themselves when the time is up.
 */
public final class ChatMuteFeature extends Feature {

    private static final String BYPASS = "scriptedessentials.chatmute.bypass";

    private DataFile store;
    private boolean chatLocked;

    public ChatMuteFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("chatmute")
                .name("Chat Mute")
                .description("Lock global chat, or mute one player for a set time.")
                .icon(Material.PAPER)
                .category(FeatureCategory.CHAT)
                .controls("/chatmute", "/mute", "/unmute")
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/mutes.yml");

        command(new SECommand(plugin, "chatmute", "mutechat") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                chatLocked = !chatLocked;
                Bukkit.broadcast(plugin.messages().get(
                        chatLocked ? "chatmute-locked" : "chatmute-unlocked",
                        Text.placeholder("player", sender.getName())));
            }
        }.describe("Lock or unlock global chat.", "/chatmute"));

        command(new SECommand(plugin, "mute") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/mute <player> [duration]"));
                }
                Player target = requireOnline(args[0]);
                int seconds = args.length > 1 ? Numbers.parseDuration(args[1]).orElse(0) : 0;
                long until = seconds <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + seconds * 1000L;

                store.get().set(target.getUniqueId() + ".name", target.getName());
                store.get().set(target.getUniqueId() + ".until", until);
                store.save();

                String duration = seconds <= 0 ? "permanently" : "for " + Numbers.formatDuration(seconds);
                plugin.messages().send(sender, "mute-applied",
                        Text.placeholder("player", target.getName()),
                        Text.placeholder("duration", duration));
                plugin.messages().send(target, "mute-target",
                        Text.placeholder("duration", duration));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return onlineNames(sender, args[0]);
                }
                return args.length == 2 ? filter(args[1], List.of("10m", "1h", "1d", "permanent")) : List.of();
            }
        }.describe("Mute a player.", "/mute <player> [duration]"));

        command(new SECommand(plugin, "unmute") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/unmute <player>"));
                }
                Player target = requireOnline(args[0]);
                if (!store.get().contains(target.getUniqueId().toString())) {
                    throw fail("unmute-not-muted", Text.placeholder("player", target.getName()));
                }
                store.get().set(target.getUniqueId().toString(), null);
                store.save();

                plugin.messages().send(sender, "unmute-applied",
                        Text.placeholder("player", target.getName()));
                plugin.messages().send(target, "unmute-target");
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Unmute a player.", "/unmute <player>"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
            public void onChat(AsyncChatEvent event) {
                if (!isEnabled()) {
                    return;
                }
                Player player = event.getPlayer();
                if (player.hasPermission(BYPASS)) {
                    return;
                }
                if (chatLocked) {
                    event.setCancelled(true);
                    plugin.messages().send(player, "chatmute-blocked");
                    return;
                }
                long remaining = remainingMute(player);
                if (remaining != 0) {
                    event.setCancelled(true);
                    plugin.messages().send(player, "mute-blocked", Text.placeholder("time",
                            remaining < 0 ? "never" : Numbers.formatDuration(remaining)));
                }
            }
        });
    }

    @Override
    protected void onDisable() {
        chatLocked = false;
    }

    /** Zero if not muted, -1 if muted permanently, otherwise seconds remaining. */
    public long remainingMute(Player player) {
        String key = player.getUniqueId() + ".until";
        if (!store.get().contains(key)) {
            return 0;
        }
        long until = store.get().getLong(key, 0L);
        if (until == Long.MAX_VALUE) {
            return -1;
        }
        long remaining = (until - System.currentTimeMillis()) / 1000L;
        if (remaining <= 0) {
            store.get().set(player.getUniqueId().toString(), null);
            store.save();
            return 0;
        }
        return remaining;
    }

    public boolean isChatLocked() {
        return chatLocked;
    }
}
