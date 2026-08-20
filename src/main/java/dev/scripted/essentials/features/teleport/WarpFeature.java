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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** {@code /warp} — named destinations, with a browsable menu and optional per-warp permissions. */
public final class WarpFeature extends Feature {

    private DataFile store;

    public WarpFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("warps")
                .name("Warp System")
                .description("Named teleport destinations with a menu and per-warp permissions.")
                .icon(Material.COMPASS)
                .category(FeatureCategory.TELEPORTATION)
                .controls("/warp", "/warps", "/setwarp", "/delwarp")
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/warps.yml");

        command(new SECommand(plugin, "warp") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                if (args.length == 0) {
                    new WarpMenu(plugin, player, WarpFeature.this).open();
                    return;
                }
                teleport(player, args[0]);
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], visibleWarps(sender)) : List.of();
            }
        }.playerOnly().describe("Teleport to a warp.", "/warp [name]"));

        command(new SECommand(plugin, "warps", "warplist") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                List<String> names = visibleWarps(sender);
                if (names.isEmpty()) {
                    throw fail("warp-none");
                }
                if (sender instanceof Player player) {
                    new WarpMenu(plugin, player, WarpFeature.this).open();
                    return;
                }
                plugin.messages().send(sender, "warp-list",
                        Text.placeholder("warps", String.join(", ", names)));
            }
        }.describe("List every warp.", "/warps"));

        command(new SECommand(plugin, "setwarp") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/setwarp <name> [icon] [permission]"));
                }
                String name = normalise(args[0]);
                Material icon = args.length > 1 ? Material.matchMaterial(args[1]) : Material.COMPASS;

                store.get().set("warps." + name + ".icon",
                        (icon == null ? Material.COMPASS : icon).name());
                store.get().set("warps." + name + ".permission", args.length > 2 ? args[2] : "");
                Locations.write(store.get(), "warps." + name + ".location", player.getLocation());
                store.save();

                plugin.messages().send(sender, "warp-created", Text.placeholder("warp", name));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], warpNames()) : List.of();
            }
        }.playerOnly().permission("scriptedessentials.setwarp")
                .describe("Create or move a warp.", "/setwarp <name> [icon] [permission]"));

        command(new SECommand(plugin, "delwarp", "removewarp") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/delwarp <name>"));
                }
                String name = normalise(args[0]);
                if (!store.get().contains("warps." + name)) {
                    throw fail("warp-unknown", Text.placeholder("warp", args[0]));
                }
                store.get().set("warps." + name, null);
                store.save();
                plugin.messages().send(sender, "warp-deleted", Text.placeholder("warp", name));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], warpNames()) : List.of();
            }
        }.permission("scriptedessentials.setwarp").describe("Delete a warp.", "/delwarp <name>"));
    }

    /** Teleports a player to a warp, or aborts with the reason they cannot go. */
    public void teleport(Player player, String rawName) {
        String name = normalise(rawName);
        if (!store.get().contains("warps." + name)) {
            plugin.messages().send(player, "warp-unknown", Text.placeholder("warp", rawName));
            return;
        }
        String permission = permissionFor(name);
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            plugin.messages().send(player, "warp-no-access", Text.placeholder("warp", name));
            return;
        }
        Location location = Locations.read(store.get(), "warps." + name + ".location");
        if (location == null) {
            plugin.messages().send(player, "warp-world-missing", Text.placeholder("warp", name));
            return;
        }
        player.teleport(location);
        Sounds.play(plugin, player, Sounds.TELEPORT, 0.5f, 1.2f);
        plugin.messages().send(player, "warp-teleported", Text.placeholder("warp", name));
    }

    public List<String> warpNames() {
        var section = store.get().getConfigurationSection("warps");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    /** Warp names the sender is actually allowed to use. */
    public List<String> visibleWarps(CommandSender sender) {
        List<String> visible = new ArrayList<>();
        for (String name : warpNames()) {
            String permission = permissionFor(name);
            if (permission.isEmpty() || sender.hasPermission(permission)) {
                visible.add(name);
            }
        }
        return visible;
    }

    public String permissionFor(String name) {
        return store.get().getString("warps." + name + ".permission", "");
    }

    public Material iconFor(String name) {
        Material icon = Material.matchMaterial(store.get().getString("warps." + name + ".icon", "COMPASS"));
        return icon == null ? Material.COMPASS : icon;
    }

    public Location locationFor(String name) {
        return Locations.read(store.get(), "warps." + name + ".location");
    }

    private static String normalise(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
