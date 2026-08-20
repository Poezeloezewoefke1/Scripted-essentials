package dev.scripted.essentials.features.chat;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Replaces the vanilla join and quit lines with configurable ones. */
public final class JoinLeaveMessagesFeature extends Feature {

    public JoinLeaveMessagesFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("joinleavemessages")
                .name("Join & Leave Messages")
                .description("Custom join, first-join and leave broadcasts.")
                .icon(Material.OAK_DOOR)
                .category(FeatureCategory.CHAT)
                .build());
    }

    @Override
    protected void onRegister() {
        listener(new Listener() {
            @EventHandler(priority = EventPriority.NORMAL)
            public void onJoin(PlayerJoinEvent event) {
                if (!isEnabled()) {
                    return;
                }
                String key = event.getPlayer().hasPlayedBefore()
                        ? "join-leave-messages.join"
                        : "join-leave-messages.first-join";
                String format = plugin.getConfig().getString(key, "");
                event.joinMessage(format.isBlank() ? null
                        : Text.parse(format, Text.placeholder("player", event.getPlayer().getName())));
            }

            @EventHandler(priority = EventPriority.NORMAL)
            public void onQuit(PlayerQuitEvent event) {
                if (!isEnabled()) {
                    return;
                }
                String format = plugin.getConfig().getString("join-leave-messages.leave", "");
                event.quitMessage(format.isBlank() ? null
                        : Text.parse(format, Text.placeholder("player", event.getPlayer().getName())));
            }
        });
    }
}
