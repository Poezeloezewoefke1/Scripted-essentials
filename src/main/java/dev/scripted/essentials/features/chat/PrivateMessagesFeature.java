package dev.scripted.essentials.features.chat;

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
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** {@code /msg} and {@code /r}, with a per-player switch for people who would rather not. */
public final class PrivateMessagesFeature extends Feature {

    private final Map<UUID, UUID> lastCorrespondent = new HashMap<>();
    private final Set<UUID> refusing = new HashSet<>();

    public PrivateMessagesFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("privatemessages")
                .name("Private Messages")
                .description("Direct messages with a reply command and an opt-out toggle.")
                .icon(Material.WRITTEN_BOOK)
                .category(FeatureCategory.CHAT)
                .controls("/msg", "/r", "/msgtoggle")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "msg", "tell", "w", "whisper") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player from = asPlayer(sender);
                if (args.length < 2) {
                    throw fail("usage", Text.placeholder("usage", "/msg <player> <message>"));
                }
                Player to = requireOnline(args[0]);
                if (to == from) {
                    throw fail("msg-self");
                }
                send(from, to, String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.playerOnly().describe("Send a private message.", "/msg <player> <message>"));

        command(new SECommand(plugin, "r", "reply") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player from = asPlayer(sender);
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/r <message>"));
                }
                UUID lastId = lastCorrespondent.get(from.getUniqueId());
                Player to = lastId == null ? null : Bukkit.getPlayer(lastId);
                if (to == null) {
                    throw fail("msg-nobody-to-reply-to");
                }
                send(from, to, String.join(" ", args));
            }
        }.playerOnly().describe("Reply to your last message.", "/r <message>"));

        command(new SECommand(plugin, "msgtoggle", "dnd") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                boolean nowRefusing = !refusing.remove(player.getUniqueId());
                if (nowRefusing) {
                    refusing.add(player.getUniqueId());
                }
                plugin.messages().send(sender, nowRefusing ? "msg-toggle-off" : "msg-toggle-on");
            }
        }.playerOnly().describe("Turn private messages on or off for yourself.", "/msgtoggle"));

        listener(new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                UUID id = event.getPlayer().getUniqueId();
                refusing.remove(id);
                lastCorrespondent.remove(id);
                lastCorrespondent.values().removeIf(id::equals);
            }
        });
    }

    private void send(Player from, Player to, String message) {
        if (refusing.contains(to.getUniqueId())
                && !from.hasPermission("scriptedessentials.privatemessages.override")) {
            plugin.messages().send(from, "msg-refused", Text.placeholder("player", to.getName()));
            return;
        }
        from.sendMessage(plugin.messages().raw("msg-format-outgoing",
                Text.placeholder("player", to.getName()),
                Text.placeholder("message", message)));
        to.sendMessage(plugin.messages().raw("msg-format-incoming",
                Text.placeholder("player", from.getName()),
                Text.placeholder("message", message)));

        lastCorrespondent.put(to.getUniqueId(), from.getUniqueId());
        lastCorrespondent.put(from.getUniqueId(), to.getUniqueId());
    }
}
