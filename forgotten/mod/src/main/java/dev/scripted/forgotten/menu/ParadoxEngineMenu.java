package dev.scripted.forgotten.menu;

import dev.scripted.forgotten.block.entity.ParadoxEngineBlockEntity;
import dev.scripted.forgotten.paradox.ParadoxEngineLogic;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.component.DataComponentTypes;

/**
 * SUBJECT, CONTRADICTION and CATALYST along the middle row, the result at the end. Click the
 * result to make it real.
 *
 * <pre>
 *  . . . . . . . . .
 *  . S &gt; C + K = R .
 *  . . . . ? . . . .
 * </pre>
 */
public class ParadoxEngineMenu extends ServerMenu {
    public static final int SUBJECT = 10;
    public static final int CONTRADICTION = 12;
    public static final int CATALYST = 14;
    public static final int RESULT = 16;
    public static final int HELP = 22;

    private final ParadoxEngineBlockEntity engine;
    private final PlayerEntity player;

    public ParadoxEngineMenu(int syncId, PlayerInventory inventory, ParadoxEngineBlockEntity engine) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, 3);
        this.engine = engine;
        this.player = inventory.player;
        for (int i = 0; i < containerSize; i++) {
            switch (i) {
                case SUBJECT -> addSlot(new InputSlot(0));
                case CONTRADICTION -> addSlot(new InputSlot(1));
                case CATALYST -> addSlot(new InputSlot(2));
                default -> addSlot(new LockedSlot(display, i));
            }
        }
        button(RESULT);
        addPlayerSlots(inventory);
        decorate();
        refresh();
    }

    private void decorate() {
        for (int i = 0; i < containerSize; i++) {
            if (i == SUBJECT || i == CONTRADICTION || i == CATALYST || i == RESULT) {
                continue;
            }
            display.setStack(i, named(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), " ", Formatting.DARK_GRAY));
        }
        display.setStack(11, named(new ItemStack(Items.PURPLE_STAINED_GLASS_PANE), "Subject  >  Contradiction", Formatting.LIGHT_PURPLE));
        display.setStack(13, named(new ItemStack(Items.PURPLE_STAINED_GLASS_PANE), "+ Catalyst (only for the hardest edits)", Formatting.LIGHT_PURPLE));
        display.setStack(15, named(new ItemStack(Items.PURPLE_STAINED_GLASS_PANE), "=  what reality will agree to", Formatting.LIGHT_PURPLE));
        display.setStack(HELP, named(new ItemStack(Items.CLOCK),
                "You cannot tell reality what to do. You must give it a reason to believe you.", Formatting.GRAY));
    }

    static ItemStack named(ItemStack stack, String name, Formatting color) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(color));
        return stack;
    }

    public void refresh() {
        ItemStack preview = player instanceof ServerPlayerEntity sp
                ? ParadoxEngineLogic.preview(sp, engine)
                : ItemStack.EMPTY;
        display.setStack(RESULT, preview);
        sendContentUpdates();
    }

    @Override
    protected void onButton(int index, PlayerEntity clicker, int mouseButton) {
        if (index == RESULT && clicker instanceof ServerPlayerEntity sp) {
            ParadoxEngineLogic.contradict(sp, engine);
            refresh();
        }
    }

    @Override
    protected boolean moveIntoContainer(ItemStack stack) {
        boolean moved = false;
        for (int slot : new int[]{SUBJECT, CONTRADICTION, CATALYST}) {
            if (!stack.isEmpty()) {
                moved |= insertItem(stack, slot, slot + 1, false);
            }
        }
        return moved;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return !engine.isRemoved() && player.squaredDistanceTo(engine.getPos().toCenterPos()) < 64;
    }

    private final class InputSlot extends Slot {
        InputSlot(int index) {
            super(engine.inputs, index, 0, 0);
        }

        @Override
        public void markDirty() {
            super.markDirty();
            refresh();
        }
    }
}
