package dev.scripted.essentials.util;

import dev.scripted.essentials.ScriptedEssentials;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Asks a player to type a value in chat and hands the answer to a callback.
 *
 * <p>Used by the menus that need free text — renaming an item, naming a kit, writing an NPC
 * greeting. The reply is swallowed rather than broadcast, and the callback runs back on the main
 * thread because chat arrives asynchronously.
 */
public final class ChatPrompt implements Listener {

    private static final String CANCEL_WORD = "cancel";

    private final ScriptedEssentials plugin;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public ChatPrompt(ScriptedEssentials plugin) {
        this.plugin = plugin;
    }

    /**
     * Waits for the player's next chat message. Typing {@code cancel} aborts and the callback is
     * never run.
     */
    public void await(Player player, String promptMessageKey, Consumer<String> onInput) {
        pending.put(player.getUniqueId(), onInput);
        // Deferred a tick: await() is normally reached from a menu click, and closing an
        // inventory inside that event fights the client's in-progress transaction.
        // An explicit lambda, not a method reference: closeInventory() is overloaded, which
        // makes the reference inexact and ambiguous between the scheduler's Runnable and
        // Consumer<BukkitTask> overloads.
        plugin.getServer().getScheduler().runTask(plugin, () -> player.closeInventory());
        plugin.messages().send(player, promptMessageKey);
        plugin.messages().send(player, "prompt-cancel-hint");
    }

    public void cancel(Player player) {
        pending.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Consumer<String> callback = pending.remove(event.getPlayer().getUniqueId());
        if (callback == null) {
            return;
        }
        event.setCancelled(true);

        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (input.equalsIgnoreCase(CANCEL_WORD)) {
            plugin.messages().send(event.getPlayer(), "prompt-cancelled");
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(input));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
