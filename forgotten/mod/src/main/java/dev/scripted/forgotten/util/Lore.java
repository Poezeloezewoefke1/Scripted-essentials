package dev.scripted.forgotten.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * The story, told in fragments. The Archivist leaves the first set lying around; the Compiler's
 * crashes drop the second; the First World holds the third. Read in order they explain why the
 * world is forgetting: it was simplified once, deliberately, and it is remembering what it was.
 */
public final class Lore {
    private Lore() {
    }

    /** Written by the world, trying to remember what it is. Left by the Archivist. */
    public static final List<String> ARCHIVE = List.of(
            "I know that stone is solid. I know that water falls. I am less sure why.",
            "There is a place where I keep the things I am sure of. It is getting smaller.",
            "A sheep walked into a tree today and for a moment neither of us knew which was which.",
            "Someone is moving through me who was not part of the plan. They dig. They build. They remember.",
            "When they sleep I dream, and the dreams are of shapes I was told to forget.",
            "Before the rules there were more rules. Before those, none at all.",
            "I have a spawn point. I remember it best. I do not know what I would be without it.",
            "The Nether remembers everything that hurt it. I envy it that certainty.",
            "The End is finished. It keeps moving because nobody gave it permission to stop.",
            "Underneath me there is another side. It is me, from the inside, upside down.",
            "There is a thing that stands where you look. I did not make it. I think it made me.",
            "If you are reading this, I wrote it. I do not remember writing it.");

    /** Fragments of the Compiler's own source. Its crashes drop them, one per crash. */
    public static final List<List<String>> SOURCE = List.of(
            List.of("// runtime.main", "while (reality.running()) {", "  for (Rule r : RULES)", "    if (!r.holds()) patch(r);",
                    "}", "// TODO: what if patch() fails?"),
            List.of("// patch.c", "/* We can no longer afford the", " * original rule set. Hardware", " * is failing. Keep what lets",
                    " * things exist without", " * contradicting themselves. */", "RULES = RULES.filter(consistent);"),
            List.of("// fork.c", "if (process.isAware()) {", "  // A process has noticed us.", "  // Do not terminate it.",
                    "  // It may be the only thing", "  // still choosing.", "  observe(process);", "}"));

    /** What the First World shows: the reveal. Found in the Workshop and in the First World. */
    public static final List<String> FIRST_WORLD = List.of(
            "Reality once contained countless possibilities. Every object could have been something else. Every place touched every other place.",
            "It could not hold. Possibilities interfered with each other. Things existed and did not. Worlds overlapped.",
            "So something made a choice. It reduced reality to a few consistent rules. Blocks became solid. Gravity became predictable. Dimensions became separate.",
            "This is a record of the world before that choice. It is not a world that was destroyed. It is a world that was deliberately simplified.",
            "You have been bringing it back. Every memory you collected, every rule you contradicted. The world is not collapsing. It is remembering.");

    public static ItemStack book(Random random) {
        String line = ARCHIVE.get(random.nextInt(ARCHIVE.size()));
        return book("Notes Toward Remembering", "The World", List.of(Text.literal(line)));
    }

    public static ItemStack sourceFragment(int index) {
        List<String> lines = SOURCE.get(Math.floorMod(index, SOURCE.size()));
        return book("Source Code (" + (Math.floorMod(index, SOURCE.size()) + 1) + "/" + SOURCE.size() + ")",
                "The Compiler", List.of(Text.literal(String.join("\n", lines))));
    }

    public static ItemStack firstWorld() {
        List<Text> pages = new ArrayList<>();
        for (String s : FIRST_WORLD) {
            pages.add(Text.literal(s));
        }
        return book("What Minecraft Used To Be", "The Author", pages);
    }

    public static ItemStack book(String title, String author, List<Text> pages) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        List<RawFilteredPair<Text>> raw = new ArrayList<>();
        for (Text page : pages) {
            raw.add(RawFilteredPair.of(page));
        }
        stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT,
                new WrittenBookContentComponent(RawFilteredPair.of(title), author, 0, raw, true));
        return stack;
    }
}
