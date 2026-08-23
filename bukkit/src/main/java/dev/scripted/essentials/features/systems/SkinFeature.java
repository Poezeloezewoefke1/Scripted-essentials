package dev.scripted.essentials.features.systems;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.gui.ItemBuilder;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * A named skin library, applied to head items.
 *
 * <p><strong>Scope note.</strong> Changing the skin worn by a live player is not possible through
 * the Bukkit or Paper API: the client only learns a player's texture from the login profile, so
 * swapping it mid-session needs packet-level access (ProtocolLib or an NMS bridge), which this
 * plugin deliberately does not take on. What is possible without that — storing named skins by
 * player name or texture URL and stamping them onto heads — is what this feature does.
 */
public final class SkinFeature extends Feature {

    private DataFile store;

    public SkinFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("skins")
                .name("Skin Library")
                .description("Stores named skins and puts them on head items. Live player skins need packet access.")
                .icon(Material.LEATHER_CHESTPLATE)
                .category(FeatureCategory.SYSTEMS)
                .controls("/skin")
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/skins.yml");

        command(new SECommand(plugin, "skin") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/skin <add|remove|list|get> ..."));
                }
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "add" -> add(sender, args);
                    case "remove" -> remove(sender, args);
                    case "list" -> plugin.messages().send(sender, "skin-list",
                            Text.placeholder("skins", names().isEmpty() ? "none" : String.join(", ", names())));
                    case "get" -> get(sender, args);
                    default -> throw fail("usage",
                            Text.placeholder("usage", "/skin <add|remove|list|get> ..."));
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(args[0], List.of("add", "remove", "list", "get"));
                }
                if (args.length == 2 && !args[0].equalsIgnoreCase("add")) {
                    return filter(args[1], names());
                }
                return List.of();
            }
        }.describe("Manage the skin library.", "/skin <add|remove|list|get>"));
    }

    private void add(CommandSender sender, String[] args) {
        if (args.length < 3) {
            throw abort("usage", Text.placeholder("usage", "/skin add <name> <player|texture-url>"));
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        String source = args[2];
        boolean isUrl = source.startsWith("http://") || source.startsWith("https://");

        store.get().set("skins." + name + ".type", isUrl ? "url" : "player");
        store.get().set("skins." + name + ".value", source);
        store.save();

        plugin.messages().send(sender, "skin-added", Text.placeholder("skin", name));
    }

    private void remove(CommandSender sender, String[] args) {
        String name = requireSkin(args, 1);
        store.get().set("skins." + name, null);
        store.save();
        plugin.messages().send(sender, "skin-removed", Text.placeholder("skin", name));
    }

    private void get(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            throw abort("player-only");
        }
        String name = requireSkin(args, 1);
        ItemStack head = head(name);
        if (head == null) {
            throw abort("skin-invalid", Text.placeholder("skin", name));
        }
        player.getInventory().addItem(head).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        plugin.messages().send(sender, "skin-given", Text.placeholder("skin", name));
    }

    /** Builds a player head wearing the named skin, or null if the entry is unusable. */
    public ItemStack head(String name) {
        String type = store.get().getString("skins." + name + ".type", "player");
        String value = store.get().getString("skins." + name + ".value");
        if (value == null) {
            return null;
        }
        ItemBuilder builder = ItemBuilder.of(Material.PLAYER_HEAD).name("<yellow>" + Text.prettify(name));

        if ("player".equals(type)) {
            return builder.skull(Bukkit.getOfflinePlayer(value)).build();
        }
        URL texture = parseUrl(value);
        if (texture == null) {
            return null;
        }
        // A profile needs an identity even when only the texture matters, so give it a stable one
        // derived from the skin name; reusing a real player's UUID here would be wrong.
        PlayerProfile profile = Bukkit.createPlayerProfile(
                UUID.nameUUIDFromBytes(("scriptedessentials:" + name).getBytes()), name);
        profile.getTextures().setSkin(texture);

        return builder.meta(meta -> {
            if (meta instanceof SkullMeta skullMeta) {
                skullMeta.setOwnerProfile(profile);
            }
        }).build();
    }

    public List<String> names() {
        return new ArrayList<>(store.get().getKeys("skins"));
    }

    private URL parseUrl(String value) {
        try {
            return URI.create(value).toURL();
        } catch (IllegalArgumentException | java.net.MalformedURLException e) {
            return null;
        }
    }

    private String requireSkin(String[] args, int index) {
        if (args.length <= index) {
            throw abort("usage", Text.placeholder("usage", "/skin " + args[0] + " <name>"));
        }
        String name = args[index].toLowerCase(Locale.ROOT);
        if (!store.get().contains("skins." + name)) {
            throw abort("skin-unknown", Text.placeholder("skin", args[index]));
        }
        return name;
    }

    private dev.scripted.essentials.command.CommandException abort(
            String key, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
        return new dev.scripted.essentials.command.CommandException(plugin.messages().raw(key, resolvers));
    }
}
