package dev.scripted.forgotten.block.entity;

import dev.scripted.forgotten.registry.ModBlockEntities;
import dev.scripted.forgotten.system.BioNodes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

/**
 * A Compiler Chest: six rows of source code and, in the last slot, the silicon wafer the
 * program is written onto. Closing the chest compiles it.
 */
public class CompilerChestBlockEntity extends BlockEntity {
    public static final int SIZE = 54;
    public static final int MEDIUM = 53;

    public final SimpleInventory code = new SimpleInventory(SIZE) {
        @Override
        public void onClose(ContainerUser user) {
            super.onClose(user);
            if (user.asLivingEntity() instanceof ServerPlayerEntity player && getWorld() instanceof ServerWorld world) {
                BioNodes.compile(world, getPos(), CompilerChestBlockEntity.this, player);
            }
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return !isRemoved();
        }
    };

    public CompilerChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPILER_CHEST, pos, state);
        code.addListener(inv -> markDirty());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, code.getHeldStacks());
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, code.getHeldStacks());
    }
}
