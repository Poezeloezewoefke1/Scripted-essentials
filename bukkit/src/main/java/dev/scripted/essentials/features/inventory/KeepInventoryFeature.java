package dev.scripted.essentials.features.inventory;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;

/**
 * Keeps items and levels through death while switched on.
 *
 * <p>The feature toggle <em>is</em> the setting, so {@code /keepinv} flips the feature itself and
 * has to stay reachable while it is off.
 */
public final class KeepInventoryFeature extends Feature {

    public KeepInventoryFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("keepinventory")
                .name("Keep Inventory")
                .description("Players keep their items and experience when they die.")
                .icon(Material.SHULKER_BOX)
                .category(FeatureCategory.INVENTORY)
                .controls("/keepinv")
                .disabledByDefault()
                .build());
    }

    @Override
    protected void onRegister() {
        commandUnbound(new SECommand(plugin, "keepinv", "keepinventory") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                boolean target = args.length > 0
                        ? args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("true")
                        : !isEnabled();
                setFeatureEnabled(target);
                plugin.messages().send(sender, target ? "keepinv-on" : "keepinv-off");
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], List.of("on", "off")) : List.of();
            }
        }.permission(definition().permission()).describe("Toggle keep inventory.", "/keepinv [on|off]"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.HIGH)
            public void onDeath(PlayerDeathEvent event) {
                if (!isEnabled()) {
                    return;
                }
                event.setKeepInventory(true);
                event.setKeepLevel(true);
                event.getDrops().clear();
                event.setDroppedExp(0);
            }
        });
    }
}
