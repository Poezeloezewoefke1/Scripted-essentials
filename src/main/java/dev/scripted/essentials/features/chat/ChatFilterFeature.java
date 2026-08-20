package dev.scripted.essentials.features.chat;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import dev.scripted.essentials.util.WordFilter;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Word filtering for chat and signs, plus repeat and rate limiting.
 *
 * <p>Before matching, text is folded down: case is dropped, common letter substitutions are
 * undone ({@code 4} to {@code a}, {@code $} to {@code s}), separators are stripped and runs of
 * the same letter are collapsed. That means {@code f-r-e-e   m0ney} still matches {@code money},
 * which is the whole point of a filter.
 */
public final class ChatFilterFeature extends Feature {

    private static final String BYPASS = "scriptedessentials.bypass";
    // Chat fires off the main thread, so these are touched by several threads at once.
    private final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessageAt = new ConcurrentHashMap<>();

    public ChatFilterFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("chatfilter")
                .name("Chat & Sign Filters")
                .description("Blocks or censors listed words in chat and on signs, and stops spam.")
                .icon(Material.OAK_SIGN)
                .category(FeatureCategory.CHAT)
                .controls("/chatfilter")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "chatfilter") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                List<String> words = plugin.getConfig().getStringList("chat-filter.blocked-words");
                if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                    plugin.messages().send(sender, "chatfilter-list", Text.placeholder("words",
                            words.isEmpty() ? "none" : String.join(", ", words)));
                    return;
                }
                if (args.length < 2) {
                    throw fail("usage", Text.placeholder("usage", "/chatfilter <add|remove|list> [word]"));
                }
                String word = args[1].toLowerCase(Locale.ROOT);
                List<String> updated = new ArrayList<>(words);

                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "add" -> {
                        if (updated.contains(word)) {
                            throw fail("chatfilter-already", Text.placeholder("word", word));
                        }
                        updated.add(word);
                        plugin.messages().send(sender, "chatfilter-added", Text.placeholder("word", word));
                    }
                    case "remove" -> {
                        if (!updated.remove(word)) {
                            throw fail("chatfilter-missing", Text.placeholder("word", word));
                        }
                        plugin.messages().send(sender, "chatfilter-removed", Text.placeholder("word", word));
                    }
                    default -> throw fail("usage",
                            Text.placeholder("usage", "/chatfilter <add|remove|list> [word]"));
                }
                plugin.getConfig().set("chat-filter.blocked-words", updated);
                plugin.saveConfig();
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(args[0], List.of("add", "remove", "list"));
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                    return filter(args[1], plugin.getConfig().getStringList("chat-filter.blocked-words"));
                }
                return List.of();
            }
        }.describe("Manage the word filter.", "/chatfilter <add|remove|list> [word]"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
            public void onChat(AsyncChatEvent event) {
                if (!isEnabled() || event.getPlayer().hasPermission(BYPASS)) {
                    return;
                }
                Player player = event.getPlayer();
                String plain = PlainTextComponentSerializer.plainText().serialize(event.message());

                if (isSpam(player, plain)) {
                    event.setCancelled(true);
                    return;
                }
                String offending = WordFilter.firstMatch(plain, blockedWords());
                if (offending == null) {
                    return;
                }
                boolean blockMode = "block".equalsIgnoreCase(
                        plugin.getConfig().getString("chat-filter.mode", "censor"));
                String censored = WordFilter.censor(plain, blockedWords());

                // Detection folds leetspeak, but censoring can only strike out the literal
                // spelling. When the two disagree - "m0ney" is caught, yet there is no "money"
                // to star out - censoring would let the message through untouched, so block it.
                if (blockMode || censored.equals(plain)) {
                    event.setCancelled(true);
                    plugin.messages().send(player, "chatfilter-blocked");
                } else {
                    event.message(Component.text(censored));
                }
            }

            @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
            public void onSign(SignChangeEvent event) {
                if (!isEnabled()
                        || !plugin.getConfig().getBoolean("chat-filter.filter-signs", true)
                        || event.getPlayer().hasPermission(BYPASS)) {
                    return;
                }
                List<Component> lines = event.lines();
                for (int index = 0; index < lines.size(); index++) {
                    String plain = PlainTextComponentSerializer.plainText().serialize(lines.get(index));
                    if (WordFilter.firstMatch(plain, blockedWords()) != null) {
                        event.line(index, Component.text(WordFilter.censor(plain, blockedWords())));
                    }
                }
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                lastMessage.remove(event.getPlayer().getUniqueId());
                lastMessageAt.remove(event.getPlayer().getUniqueId());
            }
        });
    }

    private List<String> blockedWords() {
        return plugin.getConfig().getStringList("chat-filter.blocked-words");
    }

    private boolean isSpam(Player player, String message) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        int cooldown = plugin.getConfig().getInt("chat-filter.cooldown-seconds", 0);
        if (cooldown > 0) {
            Long previous = lastMessageAt.get(id);
            if (previous != null && now - previous < cooldown * 1000L) {
                plugin.messages().send(player, "chatfilter-cooldown");
                return true;
            }
        }
        if (plugin.getConfig().getBoolean("chat-filter.block-repeated-messages", true)
                && message.equalsIgnoreCase(lastMessage.get(id))) {
            plugin.messages().send(player, "chatfilter-repeat");
            return true;
        }
        lastMessage.put(id, message);
        lastMessageAt.put(id, now);
        return false;
    }

}
