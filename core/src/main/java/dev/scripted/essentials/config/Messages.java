package dev.scripted.essentials.config;

import dev.scripted.essentials.util.Text;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Player-facing strings.
 *
 * <p>Targets Adventure's {@link Audience} rather than any platform's sender type, which is what
 * lets the same message file drive both the Paper plugin and the Fabric mod.
 */
public final class Messages {

    private YamlDocument document = YamlDocument.empty();
    private Component prefix = Component.empty();

    /** Loads the file, falling back to the copy bundled in the jar for any missing key. */
    public void load(Path file, InputStream bundledDefaults) throws IOException {
        this.document = YamlDocument.load(file);
        if (bundledDefaults != null) {
            try (Reader reader = new InputStreamReader(bundledDefaults, StandardCharsets.UTF_8)) {
                document.setDefaults(YamlDocument.load(reader));
            }
        }
        this.prefix = Text.parse(document.getString("prefix", ""));
    }

    public Component prefix() {
        return prefix;
    }

    /** A message with the prefix in front. Unknown keys render visibly rather than silently. */
    public Component get(String key, TagResolver... resolvers) {
        String raw = document.getString(key);
        if (raw == null) {
            return Component.text("<missing message: " + key + ">");
        }
        return prefix.append(Text.parse(raw, resolvers));
    }

    /** A message with no prefix, for menu text and multi-line output. */
    public Component raw(String key, TagResolver... resolvers) {
        String raw = document.getString(key);
        return raw == null ? Component.text("<missing message: " + key + ">") : Text.parse(raw, resolvers);
    }

    public List<String> rawList(String key) {
        return document.getStringList(key);
    }

    public void send(Audience target, String key, TagResolver... resolvers) {
        target.sendMessage(get(key, resolvers));
    }
}
