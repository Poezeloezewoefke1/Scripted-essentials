package io.papermc.paper.event.player;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;

import java.util.Set;

public class AsyncChatEvent extends PlayerEvent implements Cancellable {
    public AsyncChatEvent(Player who) { super(who, true); }
    public Component message() { throw new UnsupportedOperationException(); }
    public void message(Component message) { }
    public Component originalMessage() { throw new UnsupportedOperationException(); }
    public Set<Audience> viewers() { throw new UnsupportedOperationException(); }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
}
