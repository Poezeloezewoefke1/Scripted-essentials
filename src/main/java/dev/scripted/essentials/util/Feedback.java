package dev.scripted.essentials.util;

import dev.scripted.essentials.ScriptedEssentials;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Confirms an action to whoever ran it, and tells the target when it was someone else. */
public final class Feedback {

    private Feedback() {
    }

    public static void report(ScriptedEssentials plugin, CommandSender actor, Player target,
                              String selfKey, String actorKey, String targetKey) {
        if (actor == target) {
            plugin.messages().send(actor, selfKey);
        } else {
            plugin.messages().send(actor, actorKey, Text.placeholder("player", target.getName()));
            plugin.messages().send(target, targetKey, Text.placeholder("player", actor.getName()));
        }
    }
}
