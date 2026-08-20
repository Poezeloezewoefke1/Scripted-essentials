package dev.scripted.essentials.features.inventory;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.gui.ItemBuilder;
import dev.scripted.essentials.gui.Menu;
import dev.scripted.essentials.util.Numbers;
import dev.scripted.essentials.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/** The kit picker, showing each kit's cooldown state. */
public final class KitMenu extends Menu {

    private static final int PER_PAGE = 45;

    private final KitFeature kits;
    private int page;

    public KitMenu(ScriptedEssentials plugin, Player viewer, KitFeature kits) {
        super(plugin, viewer);
        this.kits = kits;
    }

    @Override
    protected Component title() {
        return Text.parse("<dark_gray>» <gradient:#4facfe:#00f2fe><bold>Kits</bold></gradient>");
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void render() {
        List<String> names = kits.available(viewer);
        int maxPage = Math.max(0, (names.size() - 1) / PER_PAGE);
        page = Math.min(page, maxPage);

        for (int slot = 0; slot < PER_PAGE; slot++) {
            int index = page * PER_PAGE + slot;
            if (index >= names.size()) {
                break;
            }
            String name = names.get(index);
            long remaining = kits.remainingCooldown(viewer, name);
            boolean ready = remaining <= 0;

            ItemBuilder icon = ItemBuilder.of(kits.iconFor(name))
                    .name("<aqua>" + Text.prettify(name))
                    .lore(ready
                            ? "<green>Ready to claim"
                            : "<red>Available in " + Numbers.formatDuration(remaining))
                    .lore("<dark_gray>Cooldown: " + Numbers.formatDuration(kits.cooldownOf(name)))
                    .blank()
                    .lore(ready ? "<green>Click to claim" : "<dark_gray>Come back later")
                    .glow(ready);

            button(slot, icon.build(), event -> {
                kits.claim(viewer, name);
                refresh();
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
            set(22, ItemBuilder.of(Material.BARRIER).name("<red>No kits available").build());
        }

        button(49, ItemBuilder.of(Material.BARRIER).name("<red>Close").build(),
                event -> viewer.closeInventory());
        fill(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name("<reset>").build());
    }
}
