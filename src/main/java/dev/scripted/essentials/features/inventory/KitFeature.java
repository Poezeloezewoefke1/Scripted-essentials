package dev.scripted.essentials.features.inventory;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Items;
import dev.scripted.essentials.util.Numbers;
import dev.scripted.essentials.util.Sounds;
import dev.scripted.essentials.util.Text;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** {@code /kit} — saved loadouts with per-kit cooldowns, permissions and a browsable menu. */
public final class KitFeature extends Feature {

    private DataFile kits;
    private DataFile cooldowns;

    public KitFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("kits")
                .name("Kit System")
                .description("Saved loadouts with cooldowns, permissions and a kit menu.")
                .icon(Material.DIAMOND_CHESTPLATE)
                .category(FeatureCategory.INVENTORY)
                .controls("/kit", "/kits", "/createkit", "/delkit")
                .build());
    }

    @Override
    protected void onRegister() {
        this.kits = data("data/kits.yml");
        this.cooldowns = data("data/kit-cooldowns.yml");

        command(new SECommand(plugin, "kit") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                if (args.length == 0) {
                    new KitMenu(plugin, player, KitFeature.this).open();
                    return;
                }
                claim(player, args[0]);
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], available(sender)) : List.of();
            }
        }.playerOnly().describe("Claim a kit.", "/kit [name]"));

        command(new SECommand(plugin, "kits", "kitlist") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                List<String> names = available(sender);
                if (names.isEmpty()) {
                    throw fail("kit-none");
                }
                if (sender instanceof Player player) {
                    new KitMenu(plugin, player, KitFeature.this).open();
                    return;
                }
                plugin.messages().send(sender, "kit-list",
                        Text.placeholder("kits", String.join(", ", names)));
            }
        }.describe("List the kits you can claim.", "/kits"));

        command(new SECommand(plugin, "createkit", "savekit") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                if (args.length == 0) {
                    throw fail("usage",
                            Text.placeholder("usage", "/createkit <name> [cooldown] [permission]"));
                }
                String name = normalise(args[0]);
                int cooldown = args.length > 1
                        ? Numbers.parseDuration(args[1]).orElse(defaultCooldown())
                        : defaultCooldown();

                kits.get().set("kits." + name + ".cooldown", cooldown);
                kits.get().set("kits." + name + ".permission", args.length > 2 ? args[2] : "");
                kits.get().set("kits." + name + ".icon",
                        firstMeaningfulIcon(player).name());
                Items.writeArray(kits.get(), "kits." + name + ".contents",
                        player.getInventory().getContents());
                Items.writeArray(kits.get(), "kits." + name + ".armor",
                        player.getInventory().getArmorContents());
                kits.save();

                plugin.messages().send(sender, "kit-created",
                        Text.placeholder("kit", name),
                        Text.placeholder("cooldown", Numbers.formatDuration(cooldown)));
            }
        }.playerOnly().permission("scriptedessentials.kits.manage")
                .describe("Save your inventory as a kit.", "/createkit <name> [cooldown] [permission]"));

        command(new SECommand(plugin, "delkit", "deletekit") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/delkit <name>"));
                }
                String name = normalise(args[0]);
                if (!kits.get().contains("kits." + name)) {
                    throw fail("kit-unknown", Text.placeholder("kit", args[0]));
                }
                kits.get().set("kits." + name, null);
                kits.save();
                plugin.messages().send(sender, "kit-deleted", Text.placeholder("kit", name));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], kitNames()) : List.of();
            }
        }.permission("scriptedessentials.kits.manage").describe("Delete a kit.", "/delkit <name>"));
    }

    /** Gives a kit to a player, reporting the reason if they cannot have it yet. */
    public void claim(Player player, String rawName) {
        String name = normalise(rawName);
        if (!kits.get().contains("kits." + name)) {
            plugin.messages().send(player, "kit-unknown", Text.placeholder("kit", rawName));
            return;
        }
        String permission = permissionFor(name);
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            plugin.messages().send(player, "kit-no-access", Text.placeholder("kit", name));
            return;
        }
        long remaining = remainingCooldown(player, name);
        if (remaining > 0) {
            plugin.messages().send(player, "kit-cooldown",
                    Text.placeholder("kit", name),
                    Text.placeholder("time", Numbers.formatDuration(remaining)));
            return;
        }

        for (ItemStack item : Items.readArray(kits.get(), "kits." + name + ".contents", 41)) {
            if (!Items.isEmpty(item)) {
                player.getInventory().addItem(item.clone()).values().forEach(left ->
                        player.getWorld().dropItemNaturally(player.getLocation(), left));
            }
        }
        ItemStack[] armor = Items.readArray(kits.get(), "kits." + name + ".armor", 4);
        ItemStack[] current = player.getInventory().getArmorContents();
        for (int slot = 0; slot < Math.min(armor.length, current.length); slot++) {
            // Only fill empty armour slots, so a kit never destroys what is already worn.
            if (!Items.isEmpty(armor[slot]) && Items.isEmpty(current[slot])) {
                current[slot] = armor[slot].clone();
            } else if (!Items.isEmpty(armor[slot])) {
                player.getInventory().addItem(armor[slot].clone());
            }
        }
        player.getInventory().setArmorContents(current);
        player.updateInventory();

        cooldowns.get().set(player.getUniqueId() + "." + name, System.currentTimeMillis());
        cooldowns.save();

        Sounds.play(plugin, player, Sounds.EQUIP, 0.8f, 1.2f);
        plugin.messages().send(player, "kit-claimed", Text.placeholder("kit", name));
    }

    public List<String> kitNames() {
        ConfigurationSection section = kits.get().getConfigurationSection("kits");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    /** Kit names the sender holds the permission for. */
    public List<String> available(CommandSender sender) {
        List<String> names = new ArrayList<>();
        for (String name : kitNames()) {
            String permission = permissionFor(name);
            if (permission.isEmpty() || sender.hasPermission(permission)) {
                names.add(name);
            }
        }
        return names;
    }

    public String permissionFor(String name) {
        return kits.get().getString("kits." + name + ".permission", "");
    }

    public Material iconFor(String name) {
        Material icon = Material.matchMaterial(
                kits.get().getString("kits." + name + ".icon", "CHEST"));
        return icon == null ? Material.CHEST : icon;
    }

    public int cooldownOf(String name) {
        return kits.get().getInt("kits." + name + ".cooldown", defaultCooldown());
    }

    /** Seconds left before the player may claim this kit again; zero when it is ready. */
    public long remainingCooldown(Player player, String name) {
        int cooldown = cooldownOf(name);
        if (cooldown <= 0 || player.hasPermission("scriptedessentials.kits.nocooldown")) {
            return 0;
        }
        long last = cooldowns.get().getLong(player.getUniqueId() + "." + name, 0L);
        long elapsed = (System.currentTimeMillis() - last) / 1000L;
        return Math.max(0, cooldown - elapsed);
    }

    private int defaultCooldown() {
        return plugin.getConfig().getInt("kits.default-cooldown-seconds", 3600);
    }

    /** Picks a sensible menu icon from what the creator was carrying. */
    private Material firstMeaningfulIcon(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (!Items.isEmpty(item)) {
                return item.getType();
            }
        }
        return Material.CHEST;
    }

    private static String normalise(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
