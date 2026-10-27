package dev.scripted.forgotten.menu;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashSet;
import java.util.Set;

/**
 * A menu that the client sees as an ordinary chest, laid out and enforced entirely on the
 * server. This is how the Paradox Engine, the Genesis Interface and the Compiler Chest get
 * interfaces without any client-side screen code: vanilla clients draw them correctly, and
 * every click is decided here.
 *
 * <p>Subclasses add their container slots (via {@link #addSlot}) first, then call
 * {@link #addPlayerSlots}. Slots registered with {@link #button} are clickable but hold
 * nothing; clicking calls {@link #onButton}.
 */
public abstract class ServerMenu extends ScreenHandler {
    protected final SimpleInventory display;
    protected final int containerSize;
    private final Set<Integer> buttons = new HashSet<>();

    protected ServerMenu(ScreenHandlerType<?> type, int syncId, int rows) {
        super(type, syncId);
        this.containerSize = rows * 9;
        this.display = new SimpleInventory(containerSize);
    }

    protected void addPlayerSlots(PlayerInventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 0, 0));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 0, 0));
        }
    }

    protected void button(int index) {
        buttons.add(index);
    }

    protected abstract void onButton(int index, PlayerEntity player, int mouseButton);

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (actionType == SlotActionType.PICKUP_ALL) {
            return;
        }
        if (slotIndex >= 0 && slotIndex < containerSize) {
            if (buttons.contains(slotIndex)) {
                if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                    onButton(slotIndex, player, button);
                    sendContentUpdates();
                }
                return;
            }
            if (slots.get(slotIndex) instanceof LockedSlot) {
                return;
            }
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasStack() || slot instanceof LockedSlot) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        if (index < containerSize) {
            if (!insertItem(stack, containerSize, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveIntoContainer(stack)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return stack.getCount() == copy.getCount() ? ItemStack.EMPTY : copy;
    }

    /** Where shift-clicked items from the player go. Default: any open container slot. */
    protected boolean moveIntoContainer(ItemStack stack) {
        boolean moved = false;
        for (int i = 0; i < containerSize && !stack.isEmpty(); i++) {
            Slot s = slots.get(i);
            if (!(s instanceof LockedSlot) && !buttons.contains(i)) {
                moved |= insertItem(stack, i, i + 1, false);
            }
        }
        return moved;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
