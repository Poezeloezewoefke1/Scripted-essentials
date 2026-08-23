package dev.scripted.essentials.features.teleport;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.gui.ItemBuilder;
import dev.scripted.essentials.gui.Menu;
import dev.scripted.essentials.util.Locations;
import dev.scripted.essentials.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/** Browsable list of the warps a player may use. */
public final class WarpMenu extends Menu {

    private static final int PER_PAGE = 45;

    private final WarpFeature warps;
    private int page;

    public WarpMenu(ScriptedEssentials plugin, Player viewer, WarpFeature warps) {
        super(plugin, viewer);
        this.warps = warps;
    }

    @Override
    protected Component title() {
        return Text.parse("<dark_gray>» <gradient:#4facfe:#00f2fe><bold>Warps</bold></gradient>");
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void render() {
        List<String> names = warps.visibleWarps(viewer);
        int maxPage = Math.max(0, (names.size() - 1) / PER_PAGE);
        page = Math.min(page, maxPage);

        for (int slot = 0; slot < PER_PAGE; slot++) {
            int index = page * PER_PAGE + slot;
            if (index >= names.size()) {
                break;
            }
            String name = names.get(index);
            Location location = warps.locationFor(name);

            ItemBuilder icon = ItemBuilder.of(warps.iconFor(name))
                    .name("<aqua>" + Text.prettify(name))
                    .lore("<dark_gray>" + (location == null
                            ? "world not loaded"
                            : Locations.describe(location)))
                    .blank()
                    .lore("<green>Click to teleport");

            button(slot, icon.build(), event -> {
                warps.teleport(viewer, name);
                viewer.closeInventory();
            });
        }

        if (page > 0) {
            button(45, ItemBuilder.of(Material.ARROW).name("<yellow>Previous Page").build(), event -> {
                page--;
                refresh();
            });
        }
        if (page < maxPage) {
            button(53, ItemBuilder.of(Material.ARROW).name("<yellow>Next Page").build(), event -> {
                page++;
                refresh();
            });
        }
        if (names.isEmpty()) {
            set(22, ItemBuilder.of(Material.BARRIER).name("<red>No warps available").build());
        }

        button(49, ItemBuilder.of(Material.BARRIER).name("<red>Close").build(),
                event -> viewer.closeInventory());
        fill(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
    }
}
