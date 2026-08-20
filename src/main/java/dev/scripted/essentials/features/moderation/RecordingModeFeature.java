package dev.scripted.essentials.features.moderation;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * A session mode for recording video: no chat noise, no accidental commands.
 *
 * <p>What it actually does is set per server in config.yml, so a server that only wants chat
 * hidden is not forced to also block commands.
 */
public final class RecordingModeFeature extends Feature {

    private static final Set<String> ALWAYS_ALLOWED = Set.of("recording", "se");

    private final Set<UUID> recording = new HashSet<>();

    public RecordingModeFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("recordingmode")
                .name("Recording Mode")
                .description("Hides chat, blocks stray commands and can hide other players while filming.")
                .icon(Material.SPYGLASS)
                .category(FeatureCategory.MODERATION)
                .controls("/recording")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "recording", "recordingmode") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                boolean nowRecording = !recording.remove(player.getUniqueId());
                if (nowRecording) {
                    recording.add(player.getUniqueId());
                }
                applyPlayerVisibility(player, nowRecording);
                plugin.messages().send(sender, nowRecording ? "recording-on" : "recording-off");
            }
        }.playerOnly().describe("Toggle recording mode.", "/recording"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.HIGH)
            public void onChat(AsyncChatEvent event) {
                if (!isEnabled() || !plugin.getConfig().getBoolean("recording-mode.mute-chat", true)) {
                    return;
                }
                // Drop the message for anyone recording rather than cancelling it for everybody.
                for (UUID id : recording) {
                    Player viewer = Bukkit.getPlayer(id);
                    if (viewer != null) {
                        event.viewers().remove(viewer);
                    }
                }
            }

            @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
            public void onCommand(PlayerCommandPreprocessEvent event) {
                if (!isEnabled()
                        || !plugin.getConfig().getBoolean("recording-mode.block-commands", true)
                        || !recording.contains(event.getPlayer().getUniqueId())) {
                    return;
                }
                String typed = event.getMessage().split(" ")[0]
                        .replaceFirst("^/", "").toLowerCase(Locale.ROOT);
                if (ALWAYS_ALLOWED.contains(typed)) {
                    return;
                }
                event.setCancelled(true);
                plugin.messages().send(event.getPlayer(), "recording-command-blocked");
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                recording.remove(event.getPlayer().getUniqueId());
            }
        });
    }

    @Override
    protected void onDisable() {
        for (UUID id : Set.copyOf(recording)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                applyPlayerVisibility(player, false);
            }
        }
        recording.clear();
    }

    public boolean isRecording(Player player) {
        return recording.contains(player.getUniqueId());
    }

    private void applyPlayerVisibility(Player player, boolean recordingNow) {
        if (!plugin.getConfig().getBoolean("recording-mode.hide-other-players", false)) {
            return;
        }
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) {
                continue;
            }
            if (recordingNow) {
                player.hidePlayer(plugin, other);
            } else {
                player.showPlayer(plugin, other);
            }
        }
    }
}
