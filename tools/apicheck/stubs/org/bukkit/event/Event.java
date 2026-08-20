package org.bukkit.event;

public abstract class Event {
    public Event() { }
    public Event(boolean isAsync) { }
    public abstract HandlerList getHandlers();
    public String getEventName() { throw new UnsupportedOperationException(); }
    public boolean isAsynchronous() { throw new UnsupportedOperationException(); }
}
