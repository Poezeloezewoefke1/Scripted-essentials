package dev.scripted.essentials.gui;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.util.Sounds;
import dev.scripted.essentials.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The control panel: every feature as an icon showing its state and the commands it owns,
 * clickable to switch it on or off.
 */
public final class FeatureMenu extends Menu {

    private static final int CONTENT_SLOTS = 45;

    private final List<FeatureCategory> tabs = new ArrayList<>();
    private int categoryIndex;
    private int page;

    public FeatureMenu(ScriptedEssentials plugin, Player viewer) {
        super(plugin, viewer);
        tabs.add(null); // "All"
        for (FeatureCategory category : FeatureCategory.values()) {
            if (!plugin.features().byCategory(category).isEmpty()) {
                tabs.add(category);
            }
        }
    }

    @Override
    protected Component title() {
        FeatureCategory category = tabs.get(categoryIndex);
        String label = category == null ? "All Features" : category.displayName();
        return Text.parse("<dark_gray>» <gradient:#4facfe:#00f2fe><bold>Feature Controls</bold></gradient> "
                + "<dark_gray>| <gray>" + label);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void render() {
        List<Feature> visible = visibleFeatures();
        int maxPage = Math.max(0, (visible.size() - 1) / CONTENT_SLOTS);
        page = Math.min(page, maxPage);

        int start = page * CONTENT_SLOTS;
        for (int slot = 0; slot < CONTENT_SLOTS; slot++) {
            int index = start + slot;
            if (index >= visible.size()) {
                break;
            }
            Feature feature = visible.get(index);
            button(slot, icon(feature), event -> {
                boolean nowEnabled = plugin.features().toggle(feature.id());
                Sounds.play(plugin, viewer, nowEnabled ? Sounds.TOGGLE_ON : Sounds.TOGGLE_OFF,
                        0.7f, nowEnabled ? 1.6f : 0.7f);
                refresh();
            });
        }

        renderFooter(maxPage);
        fill(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
    }

    private void renderFooter(int maxPage) {
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

        FeatureCategory current = tabs.get(categoryIndex);
        button(48, ItemBuilder.of(current == null ? Material.BOOKSHELF : current.icon())
                .name("<aqua>Category: <white>"
                        + (current == null ? "All Features" : current.displayName()))
                .blank()
                .lore("<gray>Left click for the next category")
                .lore("<gray>Right click for the previous one")
                .build(), event -> {
            categoryIndex = event.isRightClick()
                    ? (categoryIndex - 1 + tabs.size()) % tabs.size()
                    : (categoryIndex + 1) % tabs.size();
            page = 0;
            // The title carries the category name, so the window has to be rebuilt rather than
            // refreshed. Opening an inventory from inside a click event desyncs the client's
            // in-progress transaction, so it waits a tick.
            plugin.getServer().getScheduler().runTask(plugin, this::open);
        });

        int enabled = plugin.features().enabledCount();
        int total = plugin.features().all().size();
        set(49, ItemBuilder.of(Material.NETHER_STAR)
                .name("<gradient:#4facfe:#00f2fe><bold>Scripted Essentials</bold></gradient>")
                .blank()
                .lore("<gray>Active features: <green>" + enabled + "<dark_gray>/<gray>" + total)
                .lore("<gray>Page: <white>" + (page + 1) + "<dark_gray>/<white>" + (maxPage + 1))
                .blank()
                .lore("<dark_gray>Every feature can be switched")
                .lore("<dark_gray>on or off without a restart.")
                .build());

        button(50, ItemBuilder.of(Material.BARRIER).name("<red>Close").build(),
                event -> viewer.closeInventory());
    }

    private ItemStack icon(Feature feature) {
        boolean on = feature.isEnabled();
        ItemBuilder builder = ItemBuilder.of(feature.definition().icon())
                .name("<white>" + feature.definition().displayName())
                .lore("<gray>Status: " + (on ? "<green>Enabled" : "<red>Disabled"));

        if (!feature.definition().controls().isEmpty()) {
            builder.lore("<dark_gray>Controls: <gray>"
                    + String.join("<dark_gray>, <gray>", feature.definition().controls()));
        }
        if (!feature.definition().description().isBlank()) {
            builder.blank();
            for (String line : wrap(feature.definition().description())) {
                builder.lore("<dark_gray>" + line);
            }
        }
        return builder.blank()
                .lore(on ? "<red>Click to disable" : "<green>Click to enable")
                .glow(on)
                .clean()
                .build();
    }

    /** Splits a description into lore-sized lines so long text does not run off the screen. */
    private List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() + word.length() > 34) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private List<Feature> visibleFeatures() {
        FeatureCategory category = tabs.get(categoryIndex);
        return category == null
                ? new ArrayList<>(plugin.features().all())
                : plugin.features().byCategory(category);
    }
}
