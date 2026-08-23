package dev.scripted.essentials.features.world;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Stops players entering nominated worlds, by portal or by teleport. */
public final class DimensionLockFeature extends Feature {

    private static final String BYPASS = "scriptedessentials.bypass";

    public DimensionLockFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("dimensionlock")
                .name("Dimension Lock")
                .description("Closes chosen worlds. Portals and teleports into them are refused.")
                .icon(Material.END_PORTAL_FRAME)
                .category(FeatureCategory.WORLD)
                .controls("/dimlock")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "dimlock", "dimensionlock") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                List<String> locked = lockedWorlds();
                if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                    plugin.messages().send(sender, "dimlock-list", Text.placeholder("worlds",
                            locked.isEmpty() ? "none" : String.join(", ", locked)));
                    return;
                }
                World world = Bukkit.getWorld(args[0]);
                if (world == null) {
                    throw fail("dimlock-unknown-world", Text.placeholder("world", args[0]));
                }
                String name = world.getName().toLowerCase(Locale.ROOT);

                boolean nowLocked = !locked.remove(name);
                if (nowLocked) {
                    locked.add(name);
                }
                plugin.config().set("dimension-lock.locked-worlds", locked);
                plugin.saveConfigDocument();

                plugin.messages().send(sender, nowLocked ? "dimlock-locked" : "dimlock-unlocked",
                        Text.placeholder("world", world.getName()));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length != 1) {
                    return List.of();
                }
                List<String> names = new ArrayList<>(List.of("list"));
                Bukkit.getWorlds().forEach(world -> names.add(world.getName()));
                return filter(args[0], names);
            }
        }.describe("Lock or unlock a world.", "/dimlock <world|list>"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
            public void onTeleport(PlayerTeleportEvent event) {
                refuseIfLocked(event.getPlayer(), event.getTo(), event::setCancelled);
            }

            @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
            public void onPortal(PlayerPortalEvent event) {
                refuseIfLocked(event.getPlayer(), event.getTo(), event::setCancelled);
            }
        });
    }

    private void refuseIfLocked(Player player, Location destination,
                                java.util.function.Consumer<Boolean> cancel) {
        if (!isEnabled() || destination == null || destination.getWorld() == null) {
            return;
        }
        if (player.hasPermission(BYPASS)) {
            return;
        }
        if (!isLocked(destination.getWorld())) {
            return;
        }
        cancel.accept(true);
        player.sendMessage(Text.parse(plugin.config().getString("dimension-lock.message",
                "<red>That dimension is currently closed.")));
    }

    public boolean isLocked(World world) {
        return lockedWorlds().contains(world.getName().toLowerCase(Locale.ROOT));
    }

    private List<String> lockedWorlds() {
        List<String> names = new ArrayList<>();
        for (String raw : plugin.config().getStringList("dimension-lock.locked-worlds")) {
            names.add(raw.toLowerCase(Locale.ROOT));
        }
        return names;
    }
}
