package dev.scripted.essentials.features.social;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Text;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Named teams with colours, chat prefixes and optional friendly fire.
 *
 * <p>Teams are mirrored onto the server scoreboard, which is what colours name tags above players
 * and in the tab list; the plugin's own file stays the source of truth so membership survives a
 * scoreboard reset.
 */
public final class TeamFeature extends Feature {

    private DataFile store;

    public TeamFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("teams")
                .name("Custom Team System")
                .description("Coloured teams with prefixes, name tag colours and friendly fire control.")
                .icon(Material.SHIELD)
                .category(FeatureCategory.SYSTEMS)
                .controls("/team")
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/teams.yml");

        command(new SECommand(plugin, "team") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage",
                            "/team <create|delete|join|leave|list|info>"));
                }
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "create" -> create(sender, args);
                    case "delete" -> delete(sender, args);
                    case "join" -> join(sender, args);
                    case "leave" -> leave(sender, args);
                    case "list" -> list(sender);
                    case "info" -> info(sender, args);
                    default -> throw fail("usage", Text.placeholder("usage",
                            "/team <create|delete|join|leave|list|info>"));
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(args[0], List.of("create", "delete", "join", "leave", "list", "info"));
                }
                if (args.length == 2 && !args[0].equalsIgnoreCase("create")) {
                    return filter(args[1], teamNames());
                }
                if (args.length == 2) {
                    return List.of();
                }
                if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
                    return filter(args[2], new ArrayList<>(NamedTextColor.NAMES.keys()));
                }
                if (args.length == 3 && args[0].equalsIgnoreCase("join")) {
                    return onlineNames(sender, args[2]);
                }
                return List.of();
            }
        }.describe("Manage teams.", "/team <create|delete|join|leave|list|info>"));

        listener(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                if (isEnabled()) {
                    syncPlayer(event.getPlayer());
                }
            }

            @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
            public void onDamage(EntityDamageByEntityEvent event) {
                if (!isEnabled()) {
                    return;
                }
                if (!(event.getEntity() instanceof Player victim)
                        || !(event.getDamager() instanceof Player attacker)) {
                    return;
                }
                String team = teamOf(victim);
                if (team == null || !team.equals(teamOf(attacker))) {
                    return;
                }
                if (!store.get().getBoolean("teams." + team + ".friendly-fire", false)) {
                    event.setCancelled(true);
                    plugin.messages().send(attacker, "team-friendly-fire",
                            Text.placeholder("team", team));
                }
            }
        });
    }

    @Override
    protected void onEnable() {
        syncScoreboard();
    }

    // ---- subcommands ------------------------------------------------------------------------

    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            throw new dev.scripted.essentials.command.CommandException(
                    plugin.messages().raw("usage", Text.placeholder("usage",
                            "/team create <name> [colour]")));
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        NamedTextColor colour = args.length > 2
                ? NamedTextColor.NAMES.value(args[2].toLowerCase(Locale.ROOT))
                : NamedTextColor.WHITE;
        if (colour == null) {
            colour = NamedTextColor.WHITE;
        }
        store.get().set("teams." + name + ".color", colour.toString());
        store.get().set("teams." + name + ".prefix", "<" + colour + ">[" + name + "] ");
        store.get().set("teams." + name + ".friendly-fire", false);
        if (!store.get().contains("teams." + name + ".members")) {
            store.get().set("teams." + name + ".members", new ArrayList<String>());
        }
        store.save();
        syncScoreboard();

        plugin.messages().send(sender, "team-created", Text.placeholder("team", name));
    }

    private void delete(CommandSender sender, String[] args) {
        String name = requireTeam(args, 1);
        Scoreboard scoreboard = scoreboard();
        if (scoreboard != null && scoreboard.getTeam(name) != null) {
            scoreboard.getTeam(name).unregister();
        }
        store.get().set("teams." + name, null);
        store.save();
        plugin.messages().send(sender, "team-deleted", Text.placeholder("team", name));
    }

    private void join(CommandSender sender, String[] args) {
        String name = requireTeam(args, 1);
        Player target = args.length > 2 ? Bukkit.getPlayerExact(args[2])
                : (sender instanceof Player player ? player : null);
        if (target == null) {
            throw new dev.scripted.essentials.command.CommandException(
                    plugin.messages().raw("console-needs-target"));
        }
        removeFromAllTeams(target.getUniqueId());
        List<String> members = membersOf(name);
        members.add(target.getUniqueId().toString());
        store.get().set("teams." + name + ".members", members);
        // Remembering the name here is what keeps the scoreboard sync off the profile lookup path.
        store.get().set("teams." + name + ".member-names." + target.getUniqueId(), target.getName());
        store.save();
        syncScoreboard();

        plugin.messages().send(sender, "team-joined",
                Text.placeholder("player", target.getName()), Text.placeholder("team", name));
    }

    private void leave(CommandSender sender, String[] args) {
        Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1])
                : (sender instanceof Player player ? player : null);
        if (target == null) {
            throw new dev.scripted.essentials.command.CommandException(
                    plugin.messages().raw("console-needs-target"));
        }
        removeFromAllTeams(target.getUniqueId());
        store.save();
        syncScoreboard();
        plugin.messages().send(sender, "team-left", Text.placeholder("player", target.getName()));
    }

    private void list(CommandSender sender) {
        List<String> names = teamNames();
        plugin.messages().send(sender, "team-list", Text.placeholder("teams",
                names.isEmpty() ? "none" : String.join(", ", names)));
    }

    private void info(CommandSender sender, String[] args) {
        String name = requireTeam(args, 1);
        List<String> memberNames = new ArrayList<>();
        for (String raw : membersOf(name)) {
            String memberName = nameOf(name, raw);
            // Show the raw id for a hand-edited entry rather than dropping the member silently.
            memberNames.add(memberName == null ? raw : memberName);
        }
        plugin.messages().send(sender, "team-info",
                Text.placeholder("team", name),
                Text.placeholder("color", store.get().getString("teams." + name + ".color", "white")),
                Text.placeholder("members", memberNames.isEmpty() ? "none" : String.join(", ", memberNames)));
    }

    // ---- data -------------------------------------------------------------------------------

    public List<String> teamNames() {
        ConfigurationSection section = store.get().getConfigurationSection("teams");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    public List<String> membersOf(String team) {
        return new ArrayList<>(store.get().getStringList("teams." + team + ".members"));
    }

    /** The team a player belongs to, or null. */
    public String teamOf(Player player) {
        String id = player.getUniqueId().toString();
        for (String name : teamNames()) {
            if (store.get().getStringList("teams." + name + ".members").contains(id)) {
                return name;
            }
        }
        return null;
    }

    public String prefixOf(String team) {
        return store.get().getString("teams." + team + ".prefix", "");
    }

    private void removeFromAllTeams(UUID id) {
        for (String name : teamNames()) {
            List<String> members = membersOf(name);
            if (members.remove(id.toString())) {
                String previousName = nameOf(name, id.toString());
                store.get().set("teams." + name + ".members", members);
                store.get().set("teams." + name + ".member-names." + id, null);

                org.bukkit.scoreboard.Team team = scoreboard() == null ? null : scoreboard().getTeam(name);
                if (team != null && previousName != null) {
                    team.removeEntry(previousName);
                }
            }
        }
    }

    private String requireTeam(String[] args, int index) {
        if (args.length <= index) {
            throw new dev.scripted.essentials.command.CommandException(
                    plugin.messages().raw("usage", Text.placeholder("usage", "/team " + args[0] + " <team>")));
        }
        String name = args[index].toLowerCase(Locale.ROOT);
        if (!store.get().contains("teams." + name)) {
            throw new dev.scripted.essentials.command.CommandException(
                    plugin.messages().raw("team-unknown", Text.placeholder("team", args[index])));
        }
        return name;
    }

    /** Mirrors the stored teams onto the scoreboard so name tags pick up the colours. */
    private void syncScoreboard() {
        Scoreboard scoreboard = scoreboard();
        if (scoreboard == null) {
            return;
        }
        for (String name : teamNames()) {
            org.bukkit.scoreboard.Team team = scoreboard.getTeam(name);
            if (team == null) {
                team = scoreboard.registerNewTeam(name);
            }
            NamedTextColor colour = NamedTextColor.NAMES.value(
                    store.get().getString("teams." + name + ".color", "white"));
            if (colour != null) {
                team.color(colour);
            }
            team.prefix(Text.parse(prefixOf(name)));
            team.setAllowFriendlyFire(store.get().getBoolean("teams." + name + ".friendly-fire", false));

            for (String raw : membersOf(name)) {
                String memberName = nameOf(name, raw);
                if (memberName != null && !team.hasEntry(memberName)) {
                    team.addEntry(memberName);
                }
            }
        }
    }

    /**
     * Puts one player onto their scoreboard team.
     *
     * <p>Used on join instead of a full re-sync: a whole-server sync on every login is work
     * proportional to total membership, for one player who changed.
     */
    private void syncPlayer(Player player) {
        String teamName = teamOf(player);
        if (teamName == null) {
            return;
        }
        // Refresh the stored name in case they changed it since they joined the team.
        store.get().set("teams." + teamName + ".member-names." + player.getUniqueId(), player.getName());

        org.bukkit.scoreboard.Team team = ensureTeam(teamName);
        if (team != null && !team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    private org.bukkit.scoreboard.Team ensureTeam(String name) {
        Scoreboard scoreboard = scoreboard();
        if (scoreboard == null) {
            return null;
        }
        org.bukkit.scoreboard.Team team = scoreboard.getTeam(name);
        return team == null ? scoreboard.registerNewTeam(name) : team;
    }

    /** The name recorded for a member when they joined, or null if it was never stored. */
    private String nameOf(String team, String memberId) {
        return store.get().getString("teams." + team + ".member-names." + memberId);
    }

    private Scoreboard scoreboard() {
        return Bukkit.getScoreboardManager() == null
                ? null
                : Bukkit.getScoreboardManager().getMainScoreboard();
    }
}
