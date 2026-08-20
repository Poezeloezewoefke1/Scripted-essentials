package dev.scripted.essentials.features.inventory;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Periodically clears dropped items from the ground, with a warning first. */
public final class AutoClearFeature extends Feature {

    private BukkitTask task;

    public AutoClearFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("autoclear")
                .name("Auto Clear")
                .description("Sweeps dropped items off the ground on a timer, with a countdown warning.")
                .icon(Material.HOPPER)
                .category(FeatureCategory.WORLD)
                .controls("/autoclear")
                .disabledByDefault()
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "autoclear", "clearlag") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                int removed = sweep();
                plugin.messages().send(sender, "autoclear-manual",
                        Text.placeholder("count", String.valueOf(removed)));
            }
        }.describe("Clear dropped items now.", "/autoclear"));
    }

    @Override
    protected void onEnable() {
        int minutes = Math.max(1, plugin.getConfig().getInt("auto-clear.interval-minutes", 10));
        int warning = Math.max(0, plugin.getConfig().getInt("auto-clear.warning-seconds", 10));
        long periodTicks = minutes * 60L * 20L;

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (warning > 0) {
                Bukkit.broadcast(plugin.messages().get("autoclear-warning",
                        Text.placeholder("seconds", String.valueOf(warning))));
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    int removed = sweep();
                    Bukkit.broadcast(plugin.messages().get("autoclear-done",
                            Text.placeholder("count", String.valueOf(removed))));
                }, warning * 20L);
            } else {
                int removed = sweep();
                Bukkit.broadcast(plugin.messages().get("autoclear-done",
                        Text.placeholder("count", String.valueOf(removed))));
            }
        }, periodTicks, periodTicks);
    }

    @Override
    protected void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Removes every unprotected ground item and returns how many went. */
    private int sweep() {
        Set<Material> protectedItems = protectedMaterials();
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item drop
                        && !protectedItems.contains(drop.getItemStack().getType())) {
                    drop.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    private Set<Material> protectedMaterials() {
        Set<Material> materials = new HashSet<>();
        List<String> names = plugin.getConfig().getStringList("auto-clear.protected-items");
        for (String name : names) {
            Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
            if (material != null) {
                materials.add(material);
            }
        }
        return materials;
    }
}
