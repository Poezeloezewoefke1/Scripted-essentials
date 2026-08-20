package dev.scripted.essentials.features.player;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.gui.ItemBuilder;
import dev.scripted.essentials.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** {@code /head} and {@code /randomhead} — player heads as items. */
public final class RandomHeadFeature extends Feature {

    public RandomHeadFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("randomhead")
                .name("Random Head")
                .description("Gives the head of a named player, or of a random player who is online.")
                .icon(Material.PLAYER_HEAD)
                .category(FeatureCategory.PLAYER)
                .controls("/head", "/randomhead")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "randomhead") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                List<Player> candidates = new ArrayList<>(Bukkit.getOnlinePlayers());
                candidates.removeIf(other -> !player.canSee(other));
                if (candidates.isEmpty()) {
                    throw fail("head-no-candidates");
                }
                Player chosen = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
                giveHead(player, chosen);
            }
        }.playerOnly().describe("Get a random online player's head.", "/randomhead"));

        command(new SECommand(plugin, "head", "playerhead") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                OfflinePlayer owner = args.length > 0
                        ? Bukkit.getOfflinePlayer(args[0])
                        : player;
                if (owner.getName() == null) {
                    throw fail("player-not-found", Text.placeholder("player", args[0]));
                }
                giveHead(player, owner);
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.playerOnly().describe("Get a player's head.", "/head [player]"));
    }

    private void giveHead(Player receiver, OfflinePlayer owner) {
        ItemStack head = ItemBuilder.of(Material.PLAYER_HEAD)
                .skull(owner)
                .name("<yellow>" + owner.getName() + "<gray>'s Head")
                .build();
        receiver.getInventory().addItem(head).values()
                .forEach(left -> receiver.getWorld().dropItemNaturally(receiver.getLocation(), left));
        plugin.messages().send(receiver, "head-given", Text.placeholder("player", owner.getName()));
    }
}
