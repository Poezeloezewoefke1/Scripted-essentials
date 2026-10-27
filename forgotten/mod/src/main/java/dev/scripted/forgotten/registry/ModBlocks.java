package dev.scripted.forgotten.registry;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.block.IndexedBlock;
import dev.scripted.forgotten.block.MarrowglassBlock;
import dev.scripted.forgotten.block.NullBlock;
import dev.scripted.forgotten.block.ParadoxEngineBlock;
import dev.scripted.forgotten.block.ParadoxStoneBlock;
import dev.scripted.forgotten.block.ScarBlock;
import dev.scripted.forgotten.block.SingularityBlock;
import dev.scripted.forgotten.block.UnrememberedBlock;
import dev.scripted.forgotten.block.VolatileLogBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ModBlocks {
    /** Every block registered with an item, in creative-tab order. */
    public static final List<Block> WITH_ITEMS = new ArrayList<>();

    public static final Block PARADOX_ENGINE = block("paradox_engine", ParadoxEngineBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(4f, 1200f).requiresTool()
                    .luminance(s -> 7).sounds(BlockSoundGroup.AMETHYST_BLOCK));
    public static final Block REALITY_ANCHOR = block("reality_anchor", IndexedBlock::anchor,
            AbstractBlock.Settings.create().mapColor(MapColor.CYAN).strength(5f, 1200f).requiresTool()
                    .luminance(s -> 10).sounds(BlockSoundGroup.LODESTONE).pistonBehavior(PistonBehavior.BLOCK));
    public static final Block QUIET = block("quiet", IndexedBlock::quiet,
            AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(1f).sounds(BlockSoundGroup.WOOL));
    public static final Block POSSIBILITY_EATER = block("possibility_eater", IndexedBlock::eater,
            AbstractBlock.Settings.create().mapColor(MapColor.MAGENTA).strength(3f, 600f).requiresTool()
                    .luminance(s -> 5).sounds(BlockSoundGroup.SCULK_CATALYST));
    public static final Block FINAL_BLOCK = block("final_block", Block::new,
            AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(50f, 3600000f).requiresTool()
                    .pistonBehavior(PistonBehavior.BLOCK).sounds(BlockSoundGroup.NETHERITE));
    public static final Block UNREMEMBERED_BLOCK = block("unremembered_block", UnrememberedBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(1.5f).ticksRandomly()
                    .luminance(UnrememberedBlock::luminance));
    public static final Block NULL_BLOCK = block("null_block", NullBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(-1f, 3600000f).noCollision()
                    .dropsNothing().pistonBehavior(PistonBehavior.BLOCK));
    public static final Block SKY_FRAGMENT = block("sky_fragment", TransparentBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.LIGHT_BLUE).strength(0.3f).nonOpaque()
                    .luminance(s -> 15).sounds(BlockSoundGroup.GLASS).solidBlock((s, w, p) -> false)
                    .suffocates((s, w, p) -> false).blockVision((s, w, p) -> false));
    public static final Block VOLATILE_LOG = block("volatile_log", VolatileLogBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.LIME).strength(2f).sounds(BlockSoundGroup.WOOD));
    public static final Block LULLABY_ORE = block("lullaby_ore", Block::new,
            AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).strength(3f, 3f).requiresTool().luminance(s -> 4));
    public static final Block DEEPSLATE_LULLABY_ORE = block("deepslate_lullaby_ore", Block::new,
            AbstractBlock.Settings.create().mapColor(MapColor.DEEPSLATE_GRAY).strength(4.5f, 3f).requiresTool()
                    .luminance(s -> 4).sounds(BlockSoundGroup.DEEPSLATE));
    public static final Block MARROWGLASS = block("marrowglass", MarrowglassBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.WHITE).strength(0.6f).nonOpaque().ticksRandomly()
                    .sounds(BlockSoundGroup.GLASS).solidBlock((s, w, p) -> false)
                    .suffocates((s, w, p) -> false).blockVision((s, w, p) -> false));
    public static final Block SCARRED_NETHERRACK = block("scarred_netherrack", ScarBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.DARK_RED).strength(30f, 1200f).requiresTool()
                    .ticksRandomly().sounds(BlockSoundGroup.NETHERRACK));
    public static final Block UNWRITTEN_STONE = block("unwritten_stone", ParadoxStoneBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.WHITE_GRAY).strength(8f, 3600000f).requiresTool()
                    .luminance(s -> 2));
    public static final Block SINGULARITY = block("singularity", SingularityBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(10f, 1200f).requiresTool()
                    .luminance(s -> 3).pistonBehavior(PistonBehavior.BLOCK));
    public static final Block DATA_BLOCK = block("data_block", Block::new,
            AbstractBlock.Settings.create().mapColor(MapColor.LIME).strength(2f).luminance(s -> 9));
    public static final Block MEMORY_BLOCK = block("memory_block", Block::new,
            AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(2f));

    private ModBlocks() {
    }

    static <T extends Block> T block(String name, Function<AbstractBlock.Settings, T> factory, AbstractBlock.Settings settings) {
        return block(name, factory, settings, true);
    }

    static <T extends Block> T block(String name, Function<AbstractBlock.Settings, T> factory, AbstractBlock.Settings settings, boolean item) {
        RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, Forgotten.id(name));
        T block = Registry.register(Registries.BLOCK, key, factory.apply(settings.registryKey(key)));
        if (item) {
            RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Forgotten.id(name));
            Registry.register(Registries.ITEM, itemKey,
                    new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey()));
            WITH_ITEMS.add(block);
        }
        return block;
    }

    public static void init() {
        // Static initialisation registers everything above.
    }
}
