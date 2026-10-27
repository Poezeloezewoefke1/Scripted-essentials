package dev.scripted.forgotten;

import dev.scripted.forgotten.paradox.ParadoxRecipes;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.registry.ModBlockEntities;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModComponents;
import dev.scripted.forgotten.registry.ModItemGroups;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.system.Hooks;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The World That Forgot Itself.
 *
 * <p>Registration order matters only where one registry refers to another: components before
 * items, blocks before items (block items) and block entities.
 */
public final class Forgotten implements ModInitializer {
    public static final String MOD_ID = "forgotten";
    public static final Logger LOG = LoggerFactory.getLogger("The World That Forgot Itself");

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModComponents.init();
        ModAttachments.init();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModItemGroups.init();
        ParadoxRecipes.init();
        Hooks.init();
        LOG.info("The world has begun to forget.");
    }
}
