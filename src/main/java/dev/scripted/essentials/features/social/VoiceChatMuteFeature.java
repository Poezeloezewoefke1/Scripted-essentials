package dev.scripted.essentials.features.social;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Text;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Voice chat muting, done through permissions rather than an API dependency.
 *
 * <p>Simple Voice Chat decides who may talk from the {@code voicechat.speak} permission, so a
 * mute here is a permission attachment that sets that node to false. Nothing links against the
 * voice chat plugin, which means this loads whether or not it is installed — and works with any
 * voice plugin that gates speaking on a permission, by changing the node in config.
 */
public final class VoiceChatMuteFeature extends Feature {

    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private DataFile store;

    public VoiceChatMuteFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("voicechatmute")
                .name("Voice Chat Mute")
                .description("Mutes a player in voice chat by revoking their speak permission.")
                .icon(Material.JUKEBOX)
                .category(FeatureCategory.CHAT)
                .controls("/vcmute", "/vcunmute")
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/voice-mutes.yml");

        command(new SECommand(plugin, "vcmute", "voicemute") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/vcmute <player>"));
                }
                Player target = requireOnline(args[0]);
                store.get().set(target.getUniqueId() + ".name", target.getName());
                store.get().set(target.getUniqueId() + ".muted", true);
                store.save();
                apply(target);

                plugin.messages().send(sender, "vcmute-applied",
                        Text.placeholder("player", target.getName()));
                plugin.messages().send(target, "vcmute-target");
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Mute a player in voice chat.", "/vcmute <player>"));

        command(new SECommand(plugin, "vcunmute", "voiceunmute") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/vcunmute <player>"));
                }
                Player target = requireOnline(args[0]);
                store.get().set(target.getUniqueId().toString(), null);
                store.save();
                apply(target);

                plugin.messages().send(sender, "vcmute-lifted",
                        Text.placeholder("player", target.getName()));
                plugin.messages().send(target, "vcmute-target-lifted");
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Unmute a player in voice chat.", "/vcunmute <player>"));

        listener(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                if (isEnabled()) {
                    apply(event.getPlayer());
                }
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                PermissionAttachment attachment = attachments.remove(event.getPlayer().getUniqueId());
                if (attachment != null) {
                    attachment.remove();
                }
            }
        });
    }

    @Override
    protected void onEnable() {
        plugin.getServer().getOnlinePlayers().forEach(this::apply);
    }

    @Override
    protected void onDisable() {
        attachments.values().forEach(PermissionAttachment::remove);
        attachments.clear();
    }

    public boolean isMuted(Player player) {
        return store.get().getBoolean(player.getUniqueId() + ".muted", false);
    }

    /** Adds or removes the negative permission for one player. */
    private void apply(Player player) {
        PermissionAttachment existing = attachments.remove(player.getUniqueId());
        if (existing != null) {
            existing.remove();
        }
        if (!isMuted(player)) {
            return;
        }
        String node = plugin.getConfig().getString("voice-chat.speak-permission", "voicechat.speak");
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission(node, false);
        player.recalculatePermissions();
        attachments.put(player.getUniqueId(), attachment);
    }
}
