package dev.scripted.essentials.features.chat;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * An audit trail for staff commands.
 *
 * <p>Only commands run by players who hold {@code scriptedessentials.admin} are reported, so this
 * is a log of moderator actions rather than a firehose of everything typed on the server.
 */
public final class CommandFeedbackFeature extends Feature {

    private static final String SEE_PERMISSION = "scriptedessentials.commandfeedback.see";

    public CommandFeedbackFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("commandfeedback")
                .name("Command Feedback")
                .description("Reports staff command use to other staff and to the console.")
                .icon(Material.COMMAND_BLOCK)
                .category(FeatureCategory.CHAT)
                .disabledByDefault()
                .build());
    }

    @Override
    protected void onRegister() {
        listener(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onCommand(PlayerCommandPreprocessEvent event) {
                if (!isEnabled() || !event.getPlayer().hasPermission("scriptedessentials.admin")) {
                    return;
                }
                String command = event.getMessage();
                plugin.getLogger().info("[cmd] " + event.getPlayer().getName() + ": " + command);

                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission(SEE_PERMISSION) && !staff.equals(event.getPlayer())) {
                        plugin.messages().send(staff, "commandfeedback-notice",
                                Text.placeholder("player", event.getPlayer().getName()),
                                Text.placeholder("command", command));
                    }
                }
            }
        });
    }
}
