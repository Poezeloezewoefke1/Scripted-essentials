package dev.scripted.forgotten.block.entity;

import dev.scripted.forgotten.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

/** Holds what is in the Engine's three slots between uses. */
public class ParadoxEngineBlockEntity extends BlockEntity {
    public static final int SUBJECT = 0;
    public static final int CONTRADICTION = 1;
    public static final int CATALYST = 2;

    public final SimpleInventory inputs = new SimpleInventory(3);

    public ParadoxEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARADOX_ENGINE, pos, state);
        inputs.addListener(inv -> markDirty());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, inputs.getHeldStacks());
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, inputs.getHeldStacks());
    }
}
