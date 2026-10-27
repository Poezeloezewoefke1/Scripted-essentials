package dev.scripted.forgotten.registry;

import dev.scripted.forgotten.Forgotten;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroups {
    public static final ItemGroup FORGOTTEN = Registry.register(Registries.ITEM_GROUP, Forgotten.id("forgotten"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.LENS))
                    .displayName(Text.translatable("itemGroup.forgotten"))
                    .entries((context, entries) -> {
                        for (Item item : ModItems.ALL) {
                            entries.add(item);
                        }
                        for (Block block : ModBlocks.WITH_ITEMS) {
                            entries.add(block);
                        }
                    })
                    .build());

    private ModItemGroups() {
    }

    public static void init() {
        // Static initialisation registers everything above.
    }
}
