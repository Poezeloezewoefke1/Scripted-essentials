package dev.scripted.essentials.gui;

import dev.scripted.essentials.ScriptedEssentials;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A chest-backed menu.
 *
 * <p>The menu is its own {@link InventoryHolder}, which is how {@link MenuListener} tells plugin
 * menus apart from ordinary chests without tracking open inventories in a map.
 */
public abstract class Menu implements InventoryHolder {

    protected final ScriptedEssentials plugin;
    protected final Player viewer;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();
    private Inventory inventory;

    protected Menu(ScriptedEssentials plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    protected abstract Component title();

    /** Menu height in rows, 1 to 6. */
    protected abstract int rows();

    /** Fills the inventory. Called on open and on every {@link #refresh()}. */
    protected abstract void render();

    public void open() {
        this.inventory = Bukkit.createInventory(this, Math.max(1, Math.min(6, rows())) * 9, title());
        render();
        viewer.openInventory(inventory);
    }

    /** Rebuilds the contents in place, keeping the window open. */
    public void refresh() {
        if (inventory == null) {
            open();
            return;
        }
        handlers.clear();
        inventory.clear();
        render();
        // A refresh usually happens while handling a cancelled click, and cancelling makes the
        // server resend the pre-click view. Without this the player sees the stale icons.
        viewer.updateInventory();
    }

    protected void set(int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    /** Places an item and the action to run when it is clicked. */
    protected void button(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick) {
        set(slot, item);
        handlers.put(slot, onClick);
    }

    protected void fill(ItemStack filler) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    /** Draws the filler around the outer edge, leaving the interior free. */
    protected void border(ItemStack filler) {
        int size = inventory.getSize();
        int lastRow = size - 9;
        for (int slot = 0; slot < size; slot++) {
            boolean edge = slot < 9 || slot >= lastRow || slot % 9 == 0 || slot % 9 == 8;
            if (edge && inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> handler = handlers.get(event.getRawSlot());
        if (handler != null) {
            handler.accept(event);
        }
    }

    /** Whether the player may move items in their own inventory while this menu is open. */
    protected boolean allowBottomInventory() {
        return false;
    }

    protected void onClose(InventoryCloseEvent event) {
    }

    void fireClose(InventoryCloseEvent event) {
        onClose(event);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
