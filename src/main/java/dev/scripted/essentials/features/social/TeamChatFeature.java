package dev.scripted.essentials.features.social;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /tc} — chat that only your own team sees.
 *
 * <p>With no message it latches on, so everything typed afterwards goes to the team until it is
 * switched back off.
 */
public final class TeamChatFeature extends Feature {

    private final TeamFeature teams;
    private final Set<UUID> latched = new HashSet<>();

    public TeamChatFeature(ScriptedEssentials plugin, TeamFeature teams) {
        super(plugin, FeatureDefinition.builder("teamchat")
                .name("Team Chat")
                .description("A private chat channel for each team, with a latching toggle.")
                .icon(Material.GREEN_DYE)
                .category(FeatureCategory.CHAT)
                .controls("/tc")
                .build());
        this.teams = teams;
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "tc", "teamchat") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                String team = teams.teamOf(player);
                if (team == null) {
                    throw fail("teamchat-no-team");
                }
                if (args.length == 0) {
                    boolean nowLatched = !latched.remove(player.getUniqueId());
                    if (nowLatched) {
                        latched.add(player.getUniqueId());
                    }
                    plugin.messages().send(sender, nowLatched ? "teamchat-on" : "teamchat-off");
                    return;
                }
                broadcast(player, team, String.join(" ", args));
            }
        }.playerOnly().describe("Send a message to your team.", "/tc [message]"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
            public void onChat(AsyncChatEvent event) {
                if (!isEnabled() || !latched.contains(event.getPlayer().getUniqueId())) {
                    return;
                }
                String team = teams.teamOf(event.getPlayer());
                if (team == null) {
                    return;
                }
                event.setCancelled(true);
                String message = PlainTextComponentSerializer.plainText().serialize(event.message());
                // Chat arrives off the main thread; hop back before touching plugin state.
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> broadcast(event.getPlayer(), team, message));
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                latched.remove(event.getPlayer().getUniqueId());
            }
        });
    }

    @Override
    protected void onDisable() {
        latched.clear();
    }

    private void broadcast(Player from, String team, String message) {
        for (String raw : teams.membersOf(team)) {
            Player member = playerOf(raw);
            if (member != null) {
                member.sendMessage(plugin.messages().raw("teamchat-format",
                        Text.placeholder("team", team),
                        Text.placeholder("player", from.getName()),
                        Text.placeholder("message", message)));
            }
        }
    }

    private Player playerOf(String rawUuid) {
        try {
            return Bukkit.getPlayer(UUID.fromString(rawUuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
