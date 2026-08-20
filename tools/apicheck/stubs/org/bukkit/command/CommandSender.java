package org.bukkit.command;

import net.kyori.adventure.audience.Audience;
import org.bukkit.Server;
import org.bukkit.permissions.Permissible;

public interface CommandSender extends Audience, Permissible {
    void sendMessage(String message);
    String getName();
    Server getServer();
}
