package dev.scripted.forgotten.menu;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.genesis.Ending;
import dev.scripted.forgotten.logic.genesis.GenesisRule;
import dev.scripted.forgotten.logic.genesis.Law;
import dev.scripted.forgotten.logic.genesis.LawSubject;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.system.Genesis;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * The Genesis Interface. Row one: the world's rules. Row two: the endings, once the beginning
 * can be rewritten. Rows three to six: the thirty-six laws, writable only by the Author.
 * Everything is a button; every click is decided on the server.
 */
public class GenesisMenu extends ServerMenu {
    public static final int RULES = 0;
    public static final int ENDINGS = 9;
    public static final int LAWS = 18;
    private final ServerPlayerEntity player;
    private Ending armed;

    public GenesisMenu(int syncId, PlayerInventory inventory) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, 6);
        this.player = (ServerPlayerEntity) inventory.player;
        for (int i = 0; i < containerSize; i++) {
            addSlot(new LockedSlot(display, i));
        }
        for (int i = 0; i < GenesisRule.values().length; i++) {
            button(RULES + i);
        }
        for (int i = 0; i < Ending.values().length; i++) {
            button(ENDINGS + i);
        }
        for (int i = 0; i < 36; i++) {
            button(LAWS + i);
        }
        addPlayerSlots(inventory);
        refresh();
    }

    static Law lawAt(int index) {
        int i = index - LAWS;
        return new Law(LawSubject.values()[i / 12], Concept.values()[i % 12]);
    }

    public void refresh() {
        WorldMemory memory = Reality.memory(player.getEntityWorld().getServer());
        boolean asked = memory.flag(Genesis.AUTHOR_ASKED);
        GenesisRule[] rules = GenesisRule.values();
        for (int i = 0; i < 9; i++) {
            if (i < rules.length) {
                GenesisRule rule = rules[i];
                boolean on = memory.rule(rule);
                display.setStack(RULES + i, button(on ? Items.LIME_DYE : (asked ? Items.GRAY_DYE : Items.BARRIER), rule.statement(),
                        on ? Formatting.GREEN : Formatting.GRAY, List.of(rule.effect(), asked ? "Click to " + (on ? "unwrite" : "write") + "."
                                : "The Author has not yet asked its question."), on));
            } else {
                display.setStack(RULES + i, ItemStack.EMPTY);
            }
        }
        Ending[] endings = Ending.values();
        boolean open = Genesis.endingsOpen(memory);
        for (int i = 0; i < 9; i++) {
            if (i < endings.length) {
                Ending e = endings[i];
                boolean chosen = memory.ending() == e;
                boolean available = open && e.available(Reality.lucidity(player.getEntityWorld().getServer()), true);
                String hint = memory.ending() != null ? (chosen ? "This is how it ended." : "It has already ended.")
                        : !open ? "Rewrite the beginning first, and know what the First World was."
                        : !available ? "The world is not awake enough for this."
                        : armed == e ? "Click again to choose. This cannot be undone." : "Click to consider.";
                display.setStack(ENDINGS + i, button(available ? Items.NETHER_STAR : Items.BARRIER, e.title(),
                        chosen ? Formatting.GOLD : Formatting.WHITE, List.of(e.description(), hint), chosen || armed == e));
            } else {
                display.setStack(ENDINGS + i, ItemStack.EMPTY);
            }
        }
        boolean author = Genesis.isAuthor(memory, player);
        for (int i = 0; i < 36; i++) {
            Law law = lawAt(LAWS + i);
            boolean on = memory.law(law);
            Item icon = ModItems.concept(law.concept());
            display.setStack(LAWS + i, button(icon, law.describe(), on ? Formatting.AQUA : Formatting.DARK_GRAY,
                    List.of(author ? "Click to " + (on ? "repeal." : "enact.") : "Only the Author writes laws."), on));
        }
        sendContentUpdates();
    }

    static ItemStack button(Item item, String name, Formatting color, List<String> lore, boolean glint) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(color));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore.stream().map(l -> (Text) Text.literal(l).formatted(Formatting.GRAY)).toList()));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glint);
        return stack;
    }

    @Override
    protected void onButton(int index, PlayerEntity clicker, int mouseButton) {
        if (!(clicker instanceof ServerPlayerEntity sp)) {
            return;
        }
        WorldMemory memory = Reality.memory(sp.getEntityWorld().getServer());
        if (index < ENDINGS) {
            int i = index - RULES;
            if (i < GenesisRule.values().length) {
                Genesis.toggleRule(sp, GenesisRule.values()[i]);
            }
        } else if (index < LAWS) {
            int i = index - ENDINGS;
            if (i < Ending.values().length) {
                Ending e = Ending.values()[i];
                if (armed == e) {
                    armed = null;
                    if (Genesis.choose(sp, e)) {
                        sp.closeHandledScreen();
                        return;
                    }
                } else if (memory.ending() == null && Genesis.endingsOpen(memory)) {
                    armed = e;
                }
            }
        } else {
            Genesis.toggleLaw(sp, lawAt(index));
        }
        refresh();
    }
}
