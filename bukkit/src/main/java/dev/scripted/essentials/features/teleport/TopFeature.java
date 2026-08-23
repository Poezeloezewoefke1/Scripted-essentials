package dev.scripted.essentials.features.teleport;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Sounds;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /top} — teleports straight up to the highest solid block. */
public final class TopFeature extends Feature {

    public TopFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("top")
                .name("Top Teleport")
                .description("Teleports you to the highest block directly above you.")
                .icon(Material.LADDER)
                .category(FeatureCategory.TELEPORTATION)
                .controls("/top")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "top") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                Location from = player.getLocation();
                int highest = player.getWorld().getHighestBlockYAt(from);

                if (highest + 1 <= from.getBlockY()) {
                    throw fail("top-already");
                }
                Location destination = new Location(player.getWorld(),
                        from.getBlockX() + 0.5, highest + 1, from.getBlockZ() + 0.5,
                        from.getYaw(), from.getPitch());

                player.teleport(destination);
                Sounds.play(plugin, player, Sounds.TELEPORT, 0.5f, 1.4f);
                plugin.messages().send(sender, "top-teleported");
            }
        }.playerOnly().describe("Teleport to the surface above you.", "/top"));
    }
}
