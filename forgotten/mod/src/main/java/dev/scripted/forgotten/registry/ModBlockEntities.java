package dev.scripted.forgotten.registry;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.block.entity.ParadoxEngineBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlockEntities {
    public static final BlockEntityType<ParadoxEngineBlockEntity> PARADOX_ENGINE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Forgotten.id("paradox_engine"),
            FabricBlockEntityTypeBuilder.create(ParadoxEngineBlockEntity::new, ModBlocks.PARADOX_ENGINE).build());

    private ModBlockEntities() {
    }

    public static void init() {
        // Static initialisation registers everything above.
    }
}
