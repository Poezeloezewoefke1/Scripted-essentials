package dev.scripted.forgotten.registry;

import com.mojang.serialization.Codec;
import dev.scripted.forgotten.Forgotten;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Item data components. Plain strings and ints on purpose: they are stable on disk, readable
 * in commands, and need no custom codecs.
 */
public final class ModComponents {
    /** What a Memory was extracted from, as a registry id ("minecraft:creeper", "minecraft:water"). */
    public static final ComponentType<String> MEMORY_SOURCE = string("memory_source");
    /** A compiled Bio-Node program (see {@code Program.serialize}). */
    public static final ComponentType<String> PROGRAM = string("program");
    /** Which rule a Rule Scroll applies. */
    public static final ComponentType<String> RULE = string("rule");
    /** A folded memory of a stack: "item_id|count". Weighs nothing. */
    public static final ComponentType<String> FOLDED = string("folded");
    /** A remembered location: "dimension|x|y|z". */
    public static final ComponentType<String> LINK = string("link");
    /** Which fragment of the Compiler's source this is. */
    public static final ComponentType<Integer> FRAGMENT = integer("fragment");
    /** Uses left on items with limited charges. */
    public static final ComponentType<Integer> CHARGES = integer("charges");
    /** Items Remember Their Owners: blocks mined with this item. */
    public static final ComponentType<Integer> HISTORY_MINED = integer("history_mined");
    /** Items Remember Their Owners: creatures killed with this item. */
    public static final ComponentType<Integer> HISTORY_KILLS = integer("history_kills");
    /** Who an item last belonged to. */
    public static final ComponentType<String> HISTORY_OWNER = string("history_owner");

    private ModComponents() {
    }

    private static ComponentType<String> string(String name) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Forgotten.id(name),
                ComponentType.<String>builder().codec(Codec.STRING).packetCodec(PacketCodecs.STRING).build());
    }

    private static ComponentType<Integer> integer(String name) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Forgotten.id(name),
                ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).build());
    }

    public static void init() {
        // Static initialisation registers everything above.
    }
}
