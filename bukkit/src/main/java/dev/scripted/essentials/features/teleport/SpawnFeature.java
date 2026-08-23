package dev.scripted.essentials.features.teleport;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Locations;
import dev.scripted.essentials.util.Sounds;
import dev.scripted.essentials.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;

/** {@code /spawn} and {@code /setspawn} — a plugin-owned spawn point, independent of the world's. */
public final class SpawnFeature extends Feature {

    private DataFile store;

    public SpawnFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("spawn")
                .name("Custom Spawn")
                .description("A server spawn point, optionally used on join and on respawn.")
                .icon(Material.BEACON)
                .category(FeatureCategory.TELEPORTATION)
                .controls("/spawn", "/setspawn")
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/spawn.yml");

        command(new SECommand(plugin, "setspawn") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                Locations.write(store.get(), "spawn", player.getLocation());
                store.save();
                plugin.messages().send(sender, "spawn-set",
                        Text.placeholder("location", Locations.describe(player.getLocation())));
            }
        }.playerOnly().permission("scriptedessentials.setspawn")
                .describe("Set the server spawn point.", "/setspawn"));

        command(new SECommand(plugin, "spawn") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Location spawn = spawnLocation();
                if (spawn == null) {
                    throw fail("spawn-not-set");
                }
                Player target = resolveTarget(sender, args, 0);
                target.teleport(spawn);
                Sounds.play(plugin, target, Sounds.TELEPORT, 0.5f, 1.2f);

                if (sender == target) {
                    plugin.messages().send(sender, "spawn-teleported");
                } else {
                    plugin.messages().send(sender, "spawn-teleported-other",
                            Text.placeholder("player", target.getName()));
                    plugin.messages().send(target, "spawn-teleported");
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Teleport to the server spawn.", "/spawn [player]"));

        listener(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                if (!isEnabled() || !plugin.config().getBoolean("spawn.teleport-on-join", false)) {
                    return;
                }
                Location spawn = spawnLocation();
                if (spawn != null) {
                    event.getPlayer().teleport(spawn);
                }
            }

            @EventHandler
            public void onRespawn(PlayerRespawnEvent event) {
                if (!isEnabled() || !plugin.config().getBoolean("spawn.teleport-on-respawn", false)) {
                    return;
                }
                Location spawn = spawnLocation();
                if (spawn != null) {
                    event.setRespawnLocation(spawn);
                }
            }
        });
    }

    public Location spawnLocation() {
        return Locations.read(store.get(), "spawn");
    }
}
