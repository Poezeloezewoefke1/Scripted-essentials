package dev.scripted.forgotten;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Forgotten implements ModInitializer {
    public static final String MOD_ID = "forgotten";
    public static final Logger LOG = LoggerFactory.getLogger("The World That Forgot Itself");

    public static Item LENS;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id("lens"));
        LENS = Registry.register(Registries.ITEM, key, new Item(new Item.Settings().registryKey(key).maxCount(1)));
        LOG.info("The world has begun to forget.");
    }
}
