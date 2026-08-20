package dev.scripted.essentials.command;

import net.kyori.adventure.text.Component;

/** Thrown by a command body to abort with a message for the sender. Carries no stack trace. */
public final class CommandException extends RuntimeException {

    private final transient Component reason;

    public CommandException(Component reason) {
        super(null, null, false, false);
        this.reason = reason;
    }

    public Component reason() {
        return reason;
    }
}
