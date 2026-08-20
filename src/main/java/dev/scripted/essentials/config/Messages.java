package dev.scripted.essentials.config;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Loads messages.yml and turns keys into components, falling back to the bundled defaults. */
public final class Messages {

    private final ScriptedEssentials plugin;
    private YamlConfiguration configuration;
    private Component prefix = Component.empty();

    public Messages(ScriptedEssentials plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveResource("messages.yml", false);
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.configuration = YamlConfiguration.loadConfiguration(file);

        InputStream bundled = plugin.getResource("messages.yml");
        if (bundled != null) {
            configuration.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(bundled, StandardCharsets.UTF_8)));
        }
        this.prefix = Text.parse(configuration.getString("prefix", "<gray>[<aqua>SE</aqua>]</gray> "));
    }

    public Component prefix() {
        return prefix;
    }

    /** Resolves a key into a prefixed component. Unknown keys render as the key itself. */
    public Component get(String key, TagResolver... resolvers) {
        String raw = configuration.getString(key);
        if (raw == null) {
            return Component.text("<missing message: " + key + ">");
        }
        return prefix.append(Text.parse(raw, resolvers));
    }

    /** Resolves a key with no prefix, for menu text and multi-line output. */
    public Component raw(String key, TagResolver... resolvers) {
        String value = configuration.getString(key);
        return value == null ? Component.text("<missing message: " + key + ">") : Text.parse(value, resolvers);
    }

    public List<String> rawList(String key) {
        return configuration.getStringList(key);
    }

    public void send(CommandSender target, String key, TagResolver... resolvers) {
        target.sendMessage(get(key, resolvers));
    }
}
