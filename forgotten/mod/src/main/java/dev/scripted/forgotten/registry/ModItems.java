package dev.scripted.forgotten.registry;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.item.BreakpointWandItem;
import dev.scripted.forgotten.item.CartridgeItem;
import dev.scripted.forgotten.item.CodexItem;
import dev.scripted.forgotten.item.CrashCoreItem;
import dev.scripted.forgotten.item.DnaStrandItem;
import dev.scripted.forgotten.item.EditorItem;
import dev.scripted.forgotten.item.FirstWorldMemoryItem;
import dev.scripted.forgotten.item.GitResetItem;
import dev.scripted.forgotten.item.GlitchGrenadeItem;
import dev.scripted.forgotten.item.UndefinedSwordItem;
import dev.scripted.forgotten.item.ConceptItem;
import dev.scripted.forgotten.item.DestabilizerItem;
import dev.scripted.forgotten.item.FoldedMemoryItem;
import dev.scripted.forgotten.item.LensItem;
import dev.scripted.forgotten.item.LoreItem;
import dev.scripted.forgotten.item.MemoryItem;
import dev.scripted.forgotten.item.MemorySiphonItem;
import dev.scripted.forgotten.item.PatchNotesItem;
import dev.scripted.forgotten.item.RuleScrollItem;
import dev.scripted.forgotten.item.SimpleUseItem;
import dev.scripted.forgotten.item.StitcherItem;
import dev.scripted.forgotten.logic.concept.Concept;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ModItems {
    public static final List<Item> ALL = new ArrayList<>();
    public static final List<Item> CONCEPTS = new ArrayList<>();
    private static final Map<Concept, Item> BY_CONCEPT = new EnumMap<>(Concept.class);

    // ---- perception and knowledge ----
    public static final Item LENS = item("lens", LensItem::new, new Item.Settings().maxCount(1));
    public static final Item ARCHITECT_CODEX = item("architect_codex", CodexItem::new, new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    public static final Item MEMORY_SIPHON = item("memory_siphon", MemorySiphonItem::new, new Item.Settings().maxDamage(64));
    public static final Item MEMORY = item("memory", MemoryItem::new, new Item.Settings().maxCount(16));
    public static final Item FOLDED_MEMORY = item("folded_memory", FoldedMemoryItem::new, new Item.Settings().maxCount(1).rarity(Rarity.RARE));

    // ---- currencies of paradox ----
    public static final Item COHERENCE_SHARD = item("coherence_shard", LoreItem::new, new Item.Settings());
    public static final Item ENTROPY_SHARD = item("entropy_shard", LoreItem::new, new Item.Settings());
    public static final Item CORRUPTED_PARADOX = item("corrupted_paradox", s -> new LoreItem(s, true), new Item.Settings().rarity(Rarity.RARE));
    public static final Item PARADOX_FRAGMENT = item("paradox_fragment", s -> new LoreItem(s, true), new Item.Settings().rarity(Rarity.UNCOMMON));
    public static final Item UNREMEMBERED_THING = item("unremembered_thing", LoreItem::new, new Item.Settings());
    public static final Item UNWRITTEN_FRAGMENT = item("unwritten_fragment", s -> new LoreItem(s, true), new Item.Settings().rarity(Rarity.EPIC));
    public static final Item MEMORY_FRAGMENT_FIRST_WORLD = item("memory_of_the_first_world", FirstWorldMemoryItem::new,
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    // ---- the Mender and the Unmaker ----
    public static final Item PATCH_NOTES = item("patch_notes", PatchNotesItem::new, new Item.Settings().maxCount(16));
    public static final Item STITCHER = item("stitcher", StitcherItem::new, new Item.Settings().maxDamage(128));
    public static final Item DESTABILIZER = item("destabilizer", DestabilizerItem::new, new Item.Settings().maxDamage(64));
    public static final Item RULE_SCROLL = item("rule_scroll", RuleScrollItem::new, new Item.Settings().maxCount(1).rarity(Rarity.RARE));

    // ---- the waking world and the Draft ----
    public static final Item LULLABY_SHARD = item("lullaby_shard", LoreItem::new, new Item.Settings());
    public static final Item LULLABY = item("lullaby", s -> new SimpleUseItem(s, SimpleUseItem.Kind.LULLABY), new Item.Settings().maxCount(16));
    public static final Item BLOCK_OF_AIR = item("block_of_air", s -> new SimpleUseItem(s, SimpleUseItem.Kind.BLOCK_OF_AIR), new Item.Settings());
    public static final Item TUESDAY = item("concept_of_tuesday", s -> new SimpleUseItem(s, SimpleUseItem.Kind.TUESDAY), new Item.Settings());
    public static final Item UNWRITTEN_DEATH = item("unwritten_death", s -> new SimpleUseItem(s, SimpleUseItem.Kind.UNWRITTEN_DEATH),
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    public static final Item ONE_GRAVITY = item("one_gravity", s -> new SimpleUseItem(s, SimpleUseItem.Kind.ONE_GRAVITY), new Item.Settings());

    // ---- silicon and spores ----
    public static final Item SILICON = item("silicon", LoreItem::new, new Item.Settings());
    public static final Item SILICON_WAFER = item("silicon_wafer", LoreItem::new, new Item.Settings());
    public static final Item DNA_STRAND = item("dna_strand", DnaStrandItem::new, new Item.Settings());
    public static final Item CARTRIDGE = item("cartridge", s -> new CartridgeItem(s, false), new Item.Settings().maxCount(1));
    public static final Item INFINITE_LOOP = item("infinite_loop", s -> new CartridgeItem(s, true), new Item.Settings().maxCount(1).rarity(Rarity.RARE));
    public static final Item BREAKPOINT_WAND = item("breakpoint_wand", BreakpointWandItem::new, new Item.Settings().maxCount(1));
    public static final Item GIT_RESET = item("git_reset", GitResetItem::new, new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));

    // ---- the Unmaker's arsenal and Null Materials ----
    public static final Item GLITCH_GRENADE = item("glitch_grenade", GlitchGrenadeItem::new, new Item.Settings().maxCount(16));
    public static final Item EDITOR = item("editor", EditorItem::new, new Item.Settings().maxDamage(96).rarity(Rarity.UNCOMMON));
    public static final Item CRASH_CORE = item("crash_core", CrashCoreItem::new, new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    /** What is left of the Compiler: the world's source, which the Genesis Interface reads. */
    public static final Item SOURCE_CODE = item("source_code", s -> new LoreItem(s, true), new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item NULL_SHARD = item("null_shard", s -> new LoreItem(s, true), new Item.Settings().rarity(Rarity.RARE));
    /** NaN protection: the chestplate's defense is not a number. Its effect is in Hooks. */
    public static final ArmorMaterial NAN_MATERIAL = new ArmorMaterial(37,
            Map.of(EquipmentType.HELMET, 0, EquipmentType.CHESTPLATE, 0, EquipmentType.LEGGINGS, 0,
                    EquipmentType.BOOTS, 0, EquipmentType.BODY, 0),
            15, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, 0f, 0f, ItemTags.REPAIRS_NETHERITE_ARMOR, EquipmentAssetKeys.NETHERITE);
    public static final Item UNDEFINED_SWORD = item("undefined_sword", UndefinedSwordItem::new,
            new Item.Settings().sword(ToolMaterial.NETHERITE, 3f, -2.4f).fireproof().rarity(Rarity.EPIC));
    public static final Item NAN_CHESTPLATE = item("nan_chestplate", s -> new LoreItem(s, true),
            new Item.Settings().armor(NAN_MATERIAL, EquipmentType.CHESTPLATE).fireproof().rarity(Rarity.EPIC));

    static {
        for (Concept concept : Concept.values()) {
            Item item = item(concept.itemId(), s -> new ConceptItem(s, concept), new Item.Settings().rarity(Rarity.UNCOMMON));
            CONCEPTS.add(item);
            BY_CONCEPT.put(concept, item);
        }
    }

    private ModItems() {
    }

    public static Item concept(Concept concept) {
        return BY_CONCEPT.get(concept);
    }

    static <T extends Item> T item(String name, Function<Item.Settings, T> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Forgotten.id(name));
        T item = Registry.register(Registries.ITEM, key, factory.apply(settings.registryKey(key)));
        ALL.add(item);
        return item;
    }

    public static void init() {
        // Static initialisation registers everything above.
    }
}
