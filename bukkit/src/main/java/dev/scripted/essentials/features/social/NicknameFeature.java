package dev.scripted.essentials.features.social;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.Locale;

/**
 * {@code /nick} — display names for chat and the tab list.
 *
 * <p>Paper's default chat renderer uses the player's display name, so setting it here is all that
 * is needed for nicknames to show up in chat as well as above the player list.
 */
public final class NicknameFeature extends Feature {

    private DataFile store;

    public NicknameFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("nicknames")
                .name("Nickname System")
                .description("Display names in chat and the tab list, with length and impersonation checks.")
                .icon(Material.NAME_TAG)
                .category(FeatureCategory.SYSTEMS)
                .controls("/nick", "/nickother")
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/nicknames.yml");

        command(new SECommand(plugin, "nick", "nickname") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/nick <nickname|off>"));
                }
                // Every argument is part of the nickname, so multi-word and MiniMessage
                // nicknames both survive. Nicking somebody else is /nickother.
                set(sender, asPlayer(sender), String.join(" ", args));
            }
        }.playerOnly().describe("Set your own nickname.", "/nick <nickname|off>"));

        command(new SECommand(plugin, "nickother", "nickplayer") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length < 2) {
                    throw fail("usage", Text.placeholder("usage", "/nickother <player> <nickname|off>"));
                }
                Player target = requireOnline(args[0]);
                set(sender, target, String.join(" ",
                        java.util.Arrays.copyOfRange(args, 1, args.length)));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.permission("scriptedessentials.nicknames.others")
                .describe("Set another player's nickname.", "/nickother <player> <nickname|off>"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.LOW)
            public void onJoin(PlayerJoinEvent event) {
                if (!isEnabled()) {
                    return;
                }
                String stored = store.get().getString(event.getPlayer().getUniqueId() + ".nickname");
                if (stored != null) {
                    show(event.getPlayer(), stored);
                }
            }
        });
    }

    @Override
    protected void onDisable() {
        // Put everyone's real name back rather than leaving stale nicknames on screen.
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.displayName(Component.text(player.getName()));
            player.playerListName(Component.text(player.getName()));
        }
    }

    /** Applies or clears a nickname, whichever the input asks for. */
    private void set(CommandSender sender, Player target, String requested) {
        if (requested.equalsIgnoreCase("off") || requested.equalsIgnoreCase("reset")) {
            clear(target);
            plugin.messages().send(sender, "nick-cleared", Text.placeholder("player", target.getName()));
            return;
        }
        apply(sender, target, requested);
    }

    private void apply(CommandSender sender, Player target, String requested) {
        boolean allowColours = plugin.config().getBoolean("nicknames.allow-colours", true);
        String nickname = allowColours ? requested : Text.plain(Text.parse(requested));

        String visible = Text.plain(Text.parse(nickname));
        int maxLength = plugin.config().getInt("nicknames.max-length", 16);
        if (visible.length() > maxLength) {
            throw new dev.scripted.essentials.command.CommandException(
                    plugin.messages().raw("nick-too-long", Text.placeholder("max", String.valueOf(maxLength))));
        }
        if (plugin.config().getBoolean("nicknames.require-real-name", false)
                && !visible.toLowerCase(Locale.ROOT).contains(target.getName().toLowerCase(Locale.ROOT))) {
            throw new dev.scripted.essentials.command.CommandException(
                    plugin.messages().raw("nick-must-contain-name"));
        }
        if (isTaken(target, visible)) {
            throw new dev.scripted.essentials.command.CommandException(
                    plugin.messages().raw("nick-taken", Text.placeholder("nickname", visible)));
        }

        store.get().set(target.getUniqueId() + ".nickname", nickname);
        store.get().set(target.getUniqueId() + ".name", target.getName());
        store.save();
        show(target, nickname);

        plugin.messages().send(sender, "nick-set",
                Text.placeholder("player", target.getName()),
                Text.placeholder("nickname", visible));
    }

    private void clear(Player target) {
        store.get().set(target.getUniqueId().toString(), null);
        store.save();
        target.displayName(Component.text(target.getName()));
        target.playerListName(Component.text(target.getName()));
    }

    private void show(Player target, String nickname) {
        Component rendered = Text.parse(nickname);
        target.displayName(rendered);
        target.playerListName(rendered);
    }

    /** Stops two players ending up with the same visible name. */
    private boolean isTaken(Player claimant, String visible) {
        for (String key : store.get().getKeys()) {
            if (key.equals(claimant.getUniqueId().toString())) {
                continue;
            }
            String other = store.get().getString(key + ".nickname");
            if (other != null && Text.plain(Text.parse(other)).equalsIgnoreCase(visible)) {
                return true;
            }
        }
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.equals(claimant) && online.getName().equalsIgnoreCase(visible)) {
                return true;
            }
        }
        return false;
    }
}
