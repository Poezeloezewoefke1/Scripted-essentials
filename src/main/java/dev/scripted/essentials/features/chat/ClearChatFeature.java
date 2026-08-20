package dev.scripted.essentials.features.chat;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /clearchat} — scrolls chat away for everyone who cannot see through it. */
public final class ClearChatFeature extends Feature {

    private static final int BLANK_LINES = 100;
    private static final String BYPASS = "scriptedessentials.clearchat.bypass";

    public ClearChatFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("clearchat")
                .name("Clear Chat")
                .description("Wipes the chat window for everyone without the bypass permission.")
                .icon(Material.BOOK)
                .category(FeatureCategory.CHAT)
                .controls("/clearchat")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "clearchat", "cc") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission(BYPASS)) {
                        continue;
                    }
                    for (int line = 0; line < BLANK_LINES; line++) {
                        player.sendMessage(Component.empty());
                    }
                }
                Bukkit.broadcast(plugin.messages().get("clearchat-done",
                        Text.placeholder("player", sender.getName())));
            }
        }.describe("Clear the chat for everyone.", "/clearchat"));
    }
}
