package dev.scripted.forgotten.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lets each system add its own operator subcommands under /forgotten admin without the
 * command class knowing about every system.
 */
public final class AdminExtensions {
    private static final List<Consumer<LiteralArgumentBuilder<ServerCommandSource>>> EXTENSIONS = new ArrayList<>();

    private AdminExtensions() {
    }

    public static void add(Consumer<LiteralArgumentBuilder<ServerCommandSource>> extension) {
        EXTENSIONS.add(extension);
    }

    static void register(LiteralArgumentBuilder<ServerCommandSource> admin) {
        for (Consumer<LiteralArgumentBuilder<ServerCommandSource>> e : EXTENSIONS) {
            e.accept(admin);
        }
    }
}
