package dev.scripted.forgotten.system;

import dev.scripted.forgotten.logic.bio.Agent;
import dev.scripted.forgotten.logic.bio.Condition;
import dev.scripted.forgotten.logic.bio.Target;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** The body of a Bio-Node: the program's actions carried out by a real mob in a real world. */
final class MobAgent implements Agent {
    static final int SENSOR = 12;
    static final double REACH = 2.5;

    private final ServerWorld world;
    private final BioNodes.Node node;
    private final MobEntity mob;

    MobAgent(ServerWorld world, BioNodes.Node node) {
        this.world = world;
        this.node = node;
        this.mob = node.mob;
    }

    @Override
    public Status move(Target target) {
        Vec3d goal = switch (target) {
            case MATURE_CROP -> center(nearestCrop(SENSOR));
            case CONTAINER -> center(nearestContainer(SENSOR));
            case HOSTILE -> {
                HostileEntity h = nearestHostile(16);
                yield h == null ? null : h.getEntityPos();
            }
            case OWNER -> {
                ServerPlayerEntity owner = owner();
                yield owner == null ? null : owner.getEntityPos();
            }
            case HOME -> center(node.home());
            case ITEM -> {
                List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, mob.getBoundingBox().expand(SENSOR), e -> true);
                yield items.stream().min(Comparator.comparingDouble(mob::squaredDistanceTo)).map(ItemEntity::getEntityPos).orElse(null);
            }
        };
        if (goal == null) {
            return Status.FAILED;
        }
        if (mob.getEntityPos().squaredDistanceTo(goal) <= REACH * REACH) {
            mob.getNavigation().stop();
            return Status.DONE;
        }
        mob.getNavigation().startMovingTo(goal.x, goal.y, goal.z, 1.1);
        return Status.RUNNING;
    }

    @Override
    public Status harvest() {
        BlockPos crop = nearestCrop(3);
        if (crop == null) {
            return Status.FAILED;
        }
        BlockState state = world.getBlockState(crop);
        CropBlock block = (CropBlock) state.getBlock();
        for (ItemStack drop : Block.getDroppedStacks(state, world, crop, null)) {
            if (!take(drop)) {
                Block.dropStack(world, crop, drop);
            }
        }
        world.setBlockState(crop, block.withAge(0), Block.NOTIFY_ALL);
        world.playSound(null, crop, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 0.8f, 1f);
        return Status.DONE;
    }

    @Override
    public Status deposit(String filter) {
        BlockPos at = nearestContainer(3);
        if (at == null || !(world.getBlockEntity(at) instanceof Inventory inventory)) {
            return Status.FAILED;
        }
        boolean moved = false;
        for (ItemStack stack : node.buffer) {
            if (stack.isEmpty() || (!filter.isEmpty() && !Registries.ITEM.getId(stack.getItem()).toString().equals(filter))) {
                continue;
            }
            int before = stack.getCount();
            insert(inventory, stack);
            moved |= stack.getCount() != before;
        }
        node.buffer.removeIf(ItemStack::isEmpty);
        if (moved) {
            inventory.markDirty();
            world.playSound(null, at, SoundEvents.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5f, 1.4f);
        }
        return moved ? Status.DONE : Status.FAILED;
    }

    @Override
    public Status pickup() {
        boolean any = false;
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, mob.getBoundingBox().expand(3), e -> e.isAlive())) {
            ItemStack stack = item.getStack().copy();
            if (take(stack)) {
                item.discard();
                any = true;
            } else {
                item.setStack(stack);
            }
        }
        return any ? Status.DONE : Status.FAILED;
    }

    @Override
    public Status attack() {
        HostileEntity target = nearestHostile(3);
        if (target == null) {
            return Status.FAILED;
        }
        mob.getLookControl().lookAt(target, 30f, 30f);
        target.damage(world, world.getDamageSources().mobAttack(mob), 4f);
        mob.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        return Status.DONE;
    }

    @Override
    public Status shoot() {
        HostileEntity target = nearestHostile(16);
        if (target == null) {
            return Status.FAILED;
        }
        ArrowEntity arrow = new ArrowEntity(world, mob, new ItemStack(Items.ARROW), null);
        arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        double dx = target.getX() - mob.getX();
        double dy = target.getBodyY(0.33) - arrow.getY();
        double dz = target.getZ() - mob.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.setVelocity(dx, dy + flat * 0.2, dz, 1.6f, 4f);
        world.spawnEntity(arrow);
        world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.NEUTRAL, 1f, 1.2f);
        return Status.DONE;
    }

    @Override
    public Status follow() {
        ServerPlayerEntity owner = owner();
        if (owner == null) {
            return Status.FAILED;
        }
        if (mob.squaredDistanceTo(owner) > 9) {
            mob.getNavigation().startMovingTo(owner, 1.2);
        }
        return Status.RUNNING;
    }

    @Override
    public boolean test(Condition condition) {
        return switch (condition) {
            case BUFFER_FULL -> node.buffer.size() >= BioNodes.BUFFER_STACKS;
            case BUFFER_EMPTY -> node.buffer.isEmpty();
            case HOSTILE_NEAR -> nearestHostile(16) != null;
            case CROP_READY -> nearestCrop(SENSOR) != null;
            case DAY -> world.isDay();
            case PLAYER_NEAR -> world.getClosestPlayer(mob, 8) != null;
            case FAILED -> false; // handled by the interpreter
        };
    }

    @Override
    public void emit(int pitch) {
        float p = (float) Math.pow(2.0, (Math.floorMod(pitch, 25) - 12) / 12.0);
        world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.RECORDS, 1f, p);
    }

    // ---- senses ----

    private ServerPlayerEntity owner() {
        UUID id = node.owner();
        if (id == null) {
            return null;
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(id);
        return player != null && player.getEntityWorld() == world ? player : null;
    }

    private HostileEntity nearestHostile(double range) {
        return world.getEntitiesByClass(HostileEntity.class, mob.getBoundingBox().expand(range), HostileEntity::isAlive)
                .stream().min(Comparator.comparingDouble(mob::squaredDistanceTo)).orElse(null);
    }

    private BlockPos nearestCrop(int range) {
        for (BlockPos p : BlockPos.iterateOutwards(mob.getBlockPos(), range, 2, range)) {
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof CropBlock crop && crop.isMature(s)) {
                return p.toImmutable();
            }
        }
        return null;
    }

    private BlockPos nearestContainer(int range) {
        for (BlockPos p : BlockPos.iterateOutwards(mob.getBlockPos(), range, 2, range)) {
            BlockEntity be = world.getBlockEntity(p);
            if (be instanceof Inventory && !(be instanceof dev.scripted.forgotten.block.entity.CompilerChestBlockEntity)) {
                return p.toImmutable();
            }
        }
        return null;
    }

    private static Vec3d center(BlockPos pos) {
        return pos == null ? null : Vec3d.ofBottomCenter(pos);
    }

    // ---- the buffer ----

    /** Adds a stack to the buffer. Returns false, leaving the remainder in {@code stack}, when full. */
    private boolean take(ItemStack stack) {
        for (ItemStack held : node.buffer) {
            if (ItemStack.areItemsAndComponentsEqual(held, stack) && held.getCount() < held.getMaxCount()) {
                int move = Math.min(stack.getCount(), held.getMaxCount() - held.getCount());
                held.increment(move);
                stack.decrement(move);
                if (stack.isEmpty()) {
                    return true;
                }
            }
        }
        if (node.buffer.size() < BioNodes.BUFFER_STACKS) {
            node.buffer.add(stack.copy());
            stack.setCount(0);
            return true;
        }
        return false;
    }

    private static void insert(Inventory inventory, ItemStack stack) {
        for (int i = 0; i < inventory.size() && !stack.isEmpty(); i++) {
            ItemStack slot = inventory.getStack(i);
            if (slot.isEmpty()) {
                if (inventory.isValid(i, stack)) {
                    inventory.setStack(i, stack.copy());
                    stack.setCount(0);
                }
            } else if (ItemStack.areItemsAndComponentsEqual(slot, stack)) {
                int room = Math.min(slot.getMaxCount(), inventory.getMaxCount(slot)) - slot.getCount();
                int move = Math.min(room, stack.getCount());
                if (move > 0) {
                    slot.increment(move);
                    stack.decrement(move);
                }
            }
        }
    }
}
