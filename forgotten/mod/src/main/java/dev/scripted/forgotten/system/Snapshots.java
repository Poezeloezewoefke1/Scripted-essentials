package dev.scripted.forgotten.system;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;

/** Inventory snapshots: what you carried a long time ago, kept so history can be thrown back at you. */
public final class Snapshots {
    private Snapshots() {
    }

    public static NbtList write(Inventory inventory, RegistryWrapper.WrapperLookup registries) {
        var ops = registries.getOps(NbtOps.INSTANCE);
        NbtList list = new NbtList();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            int slot = i;
            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(el -> {
                NbtCompound c = new NbtCompound();
                c.putInt("slot", slot);
                c.put("item", el);
                list.add(c);
            });
        }
        return list;
    }

    /** Fills an inventory from a snapshot, clearing it first. */
    public static void read(NbtList list, Inventory inventory, RegistryWrapper.WrapperLookup registries) {
        var ops = registries.getOps(NbtOps.INSTANCE);
        for (int i = 0; i < inventory.size(); i++) {
            inventory.setStack(i, ItemStack.EMPTY);
        }
        for (NbtElement el : list) {
            if (el instanceof NbtCompound c) {
                int slot = c.getInt("slot", -1);
                NbtElement item = c.get("item");
                if (slot >= 0 && slot < inventory.size() && item != null) {
                    ItemStack.CODEC.parse(ops, item).result().ifPresent(stack -> inventory.setStack(slot, stack));
                }
            }
        }
    }
}
