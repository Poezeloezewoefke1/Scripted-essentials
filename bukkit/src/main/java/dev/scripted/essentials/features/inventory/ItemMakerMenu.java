package dev.scripted.essentials.features.inventory;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.gui.ItemBuilder;
import dev.scripted.essentials.gui.Menu;
import dev.scripted.essentials.util.Enchants;
import dev.scripted.essentials.util.Numbers;
import dev.scripted.essentials.util.Sounds;
import dev.scripted.essentials.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The item editor.
 *
 * <p>Every button acts on whatever is in the player's main hand at the moment it is pressed, so
 * the menu never works from a stale copy of the item.
 */
public final class ItemMakerMenu extends Menu {

    public ItemMakerMenu(ScriptedEssentials plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected Component title() {
        return Text.parse("<dark_gray>» <gradient:#4facfe:#00f2fe><bold>Item Maker</bold></gradient>");
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void render() {
        ItemStack held = viewer.getInventory().getItemInMainHand();
        boolean hasItem = held.getType() != Material.AIR;
        ItemMeta meta = hasItem ? held.getItemMeta() : null;

        set(13, hasItem
                ? held.clone()
                : ItemBuilder.of(Material.BARRIER).name("<red>Hold an item to edit it").build());

        button(29, tool(Material.NAME_TAG, "Rename",
                "Set the display name.", "MiniMessage and &-codes both work."), event ->
                prompt("prompt-item-name", input -> edit(m -> m.displayName(Text.item(input)))));

        button(30, tool(Material.WRITABLE_BOOK, "Add Lore Line",
                "Append one line of lore."), event ->
                prompt("prompt-item-lore", input -> edit(m -> {
                    List<Component> lore = m.hasLore() ? new ArrayList<>(m.lore()) : new ArrayList<>();
                    lore.add(Text.item(input));
                    m.lore(lore);
                })));

        button(31, tool(Material.SHEARS, "Remove Last Lore Line",
                "Delete the bottom line of lore."), event -> edit(m -> {
            if (!m.hasLore()) {
                return;
            }
            List<Component> lore = new ArrayList<>(m.lore());
            if (!lore.isEmpty()) {
                lore.remove(lore.size() - 1);
            }
            m.lore(lore.isEmpty() ? null : lore);
        }));

        button(32, tool(Material.LAVA_BUCKET, "Clear Lore",
                "Remove every line of lore."), event -> edit(m -> m.lore(null)));

        button(33, tool(Material.ENCHANTED_BOOK, "Add Enchantment",
                "Type it as: sharpness 5"), event ->
                prompt("prompt-item-enchant", input -> {
                    String[] parts = input.trim().split("\\s+");
                    Enchantment enchantment = Enchants.byKey(parts[0]);
                    if (enchantment == null) {
                        plugin.messages().send(viewer, "itemmaker-unknown-enchant",
                                Text.placeholder("input", parts[0]));
                        return;
                    }
                    int level = parts.length > 1 ? Numbers.parseInt(parts[1]).orElse(1) : 1;
                    edit(m -> m.addEnchant(enchantment, Numbers.clamp(level, 1, 255), true));
                }));

        button(38, tool(Material.GRINDSTONE, "Clear Enchantments",
                "Strip every enchantment."), event -> edit(m ->
                new ArrayList<>(m.getEnchants().keySet()).forEach(m::removeEnchant)));

        boolean unbreakable = meta != null && meta.isUnbreakable();
        button(39, tool(Material.OBSIDIAN, "Unbreakable: " + onOff(unbreakable),
                "Stop the item losing durability."), event ->
                edit(m -> m.setUnbreakable(!m.isUnbreakable())));

        boolean glowing = meta != null && Boolean.TRUE.equals(meta.getEnchantmentGlintOverride());
        button(40, tool(Material.GLOWSTONE_DUST, "Glow: " + onOff(glowing),
                "Show the enchantment shimmer", "without adding an enchantment."), event ->
                edit(m -> m.setEnchantmentGlintOverride(
                        Boolean.TRUE.equals(m.getEnchantmentGlintOverride()) ? null : Boolean.TRUE)));

        button(41, tool(Material.BLACK_BANNER, "Hide Attributes",
                "Hide enchantments, attributes and", "flags from the tooltip."), event ->
                edit(m -> m.addItemFlags(ItemFlag.values())));

        button(42, tool(Material.PAPER, "Set Amount",
                "Type a stack size."), event ->
                prompt("prompt-item-amount", input -> {
                    ItemStack current = viewer.getInventory().getItemInMainHand();
                    if (current.getType() == Material.AIR) {
                        plugin.messages().send(viewer, "itemmaker-empty-hand");
                        return;
                    }
                    int amount = Numbers.parseInt(input).orElse(1);
                    current.setAmount(Numbers.clamp(amount, 1, current.getMaxStackSize()));
                    viewer.getInventory().setItemInMainHand(current);
                    viewer.updateInventory();
                }));

        button(49, ItemBuilder.of(Material.BARRIER).name("<red>Close").build(),
                event -> viewer.closeInventory());
        fill(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
    }

    /** Applies a change to the held item, then redraws the menu. */
    private void edit(Consumer<ItemMeta> mutator) {
        ItemStack held = viewer.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            plugin.messages().send(viewer, "itemmaker-empty-hand");
            return;
        }
        ItemMeta meta = held.getItemMeta();
        if (meta == null) {
            return;
        }
        mutator.accept(meta);
        held.setItemMeta(meta);
        viewer.getInventory().setItemInMainHand(held);
        viewer.updateInventory();

        Sounds.click(plugin, viewer);
        refresh();
    }

    /** Asks for a value in chat, applies it, and brings the editor back up. */
    private void prompt(String messageKey, Consumer<String> onInput) {
        plugin.prompts().await(viewer, messageKey, input -> {
            onInput.accept(input);
            if (viewer.isValid()) {
                new ItemMakerMenu(plugin, viewer).open();
            }
        });
    }

    private ItemStack tool(Material icon, String name, String... description) {
        ItemBuilder builder = ItemBuilder.of(icon).name("<aqua>" + name).blank();
        for (String line : description) {
            builder.lore("<dark_gray>" + line);
        }
        return builder.clean().build();
    }

    private static String onOff(boolean value) {
        return value ? "<green>on" : "<red>off";
    }
}
