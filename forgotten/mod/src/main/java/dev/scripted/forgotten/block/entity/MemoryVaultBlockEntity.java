package dev.scripted.forgotten.block.entity;

import dev.scripted.forgotten.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

/** The Memory Vault's store of extracted memories. */
public class MemoryVaultBlockEntity extends BlockEntity {
    public final SimpleInventory memories = new SimpleInventory(27);

    public MemoryVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEMORY_VAULT, pos, state);
        memories.addListener(inv -> markDirty());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, memories.getHeldStacks());
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, memories.getHeldStacks());
    }
}
