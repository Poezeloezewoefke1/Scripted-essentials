package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.block.entity.CompilerChestBlockEntity;
import dev.scripted.forgotten.logic.bio.CompileError;
import dev.scripted.forgotten.logic.bio.Interpreter;
import dev.scripted.forgotten.logic.bio.Program;
import dev.scripted.forgotten.logic.bio.ProgramCompiler;
import dev.scripted.forgotten.logic.bio.ProgramState;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.registry.ModComponents;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Silicon and Spores. Compiles Compiler Chests into Cartridges, splices mobs into Bio-Nodes and
 * runs their programs: a cow that walks to ripe wheat, harvests it, carries it to a chest and
 * shoots at zombies on the way is a vanilla cow whose goals were replaced by your code.
 */
public final class BioNodes {
    /** Stacks a node can carry before its buffer counts as full. */
    public static final int BUFFER_STACKS = 9;
    private static final Map<UUID, Node> NODES = new HashMap<>();

    private BioNodes() {
    }

    // ---- compiling ----

    public static void compile(ServerWorld world, BlockPos pos, CompilerChestBlockEntity chest, ServerPlayerEntity player) {
        SimpleInventory code = chest.code;
        ItemStack medium = code.getStack(CompilerChestBlockEntity.MEDIUM);
        boolean blank = true;
        for (int i = 0; i < CompilerChestBlockEntity.MEDIUM; i++) {
            if (!code.getStack(i).isEmpty()) {
                blank = false;
                break;
            }
        }
        if (blank) {
            return;
        }
        if (!medium.isOf(ModItems.SILICON_WAFER)) {
            if (medium.isOf(ModItems.CARTRIDGE) || medium.isOf(ModItems.INFINITE_LOOP)) {
                return; // already compiled; nothing changed since
            }
            Texts.bar(player, Texts.info("Nothing to compile onto. The last slot takes a silicon wafer."));
            return;
        }
        int rows = CompilerChestBlockEntity.SIZE / 9;
        String[][] items = new String[rows][9];
        int[][] counts = new int[rows][9];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) {
                int slot = r * 9 + c;
                ItemStack stack = slot == CompilerChestBlockEntity.MEDIUM ? ItemStack.EMPTY : code.getStack(slot);
                items[r][c] = stack.isEmpty() ? "" : Registries.ITEM.getId(stack.getItem()).toString();
                counts[r][c] = stack.getCount();
            }
        }
        ProgramCompiler.Result result = ProgramCompiler.compile(items, counts);
        medium.decrement(1);
        if (!result.ok()) {
            fail(world, pos, player, result.error());
            return;
        }
        Program program = result.program();
        ItemStack out = new ItemStack(program.tightLoop() ? ModItems.INFINITE_LOOP : ModItems.CARTRIDGE);
        out.set(ModComponents.PROGRAM, program.serialize());
        if (code.getStack(CompilerChestBlockEntity.MEDIUM).isEmpty()) {
            code.setStack(CompilerChestBlockEntity.MEDIUM, out);
        } else {
            Block.dropStack(world, pos.up(), out);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.6f, 1.8f);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 20, 0.3, 0.2, 0.3, 0.1);
        Texts.bar(player, program.tightLoop()
                ? Texts.glitch("Compiled. It never stops. It is not supposed to stop.")
                : Texts.discovery("Compiled " + program.size() + " lines."));
        Reality.insight(player, Insight.PROGRAM_COMPILED);
        Reality.count(world, ChronicleEvent.PROGRAM_COMPILED, program.tightLoop() ? "infinite_loop" : "cartridge");
        if (program.tightLoop()) {
            Reality.advance(player, "infinite_loop");
        }
    }

    private static void fail(ServerWorld world, BlockPos pos, ServerPlayerEntity player, CompileError error) {
        player.sendMessage(Text.literal("error: " + error.describe()).formatted(Formatting.RED), false);
        if (error.severe()) {
            VexEntity npe = EntityType.VEX.create(world, SpawnReason.EVENT);
            if (npe != null) {
                npe.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0f, 0f);
                npe.setCustomName(Text.literal("NullPointerException").formatted(Formatting.DARK_RED));
                npe.setCustomNameVisible(true);
                npe.setLifeTicks(20 * 30);
                npe.setTarget(player);
                world.spawnEntity(npe);
            }
            world.playSound(null, pos, SoundEvents.ENTITY_VEX_CHARGE, SoundCategory.HOSTILE, 1f, 0.5f);
        } else {
            world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1.5f, World.ExplosionSourceType.NONE);
        }
        Reality.paradox(world, pos, 1);
    }

    // ---- splicing and installing ----

    public static boolean isNode(Entity entity) {
        return entity.hasAttached(ModAttachments.BIO_PROGRAM);
    }

    /** Splices a DNA strand into a mob: it stops being itself and waits for code. */
    public static void splice(MobEntity mob, ServerPlayerEntity owner) {
        mob.setAttached(ModAttachments.BIO_PROGRAM, "");
        mob.setAttached(ModAttachments.OWNER, owner.getUuidAsString());
        mob.setAttached(ModAttachments.BIO_HOME, mob.getBlockPos().asLong());
        mob.setPersistent();
        strip(mob);
        if (mob.getEntityWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, mob.getX(), mob.getBodyY(0.5), mob.getZ(), 30, 0.4, 0.5, 0.4, 0.02);
        }
        track(mob);
    }

    /** Loads a program onto a spliced mob and starts it from its first line. */
    public static void install(MobEntity mob, String serialized) {
        Program program = Program.parse(serialized);
        ProgramState state = new ProgramState();
        state.reset(program);
        mob.setAttached(ModAttachments.BIO_PROGRAM, serialized);
        mob.setAttached(ModAttachments.BIO_STATE, write(state));
        mob.setAttached(ModAttachments.BIO_HOME, mob.getBlockPos().asLong());
        Node node = NODES.get(mob.getUuid());
        if (node != null) {
            node.program = program;
            node.state = state;
        } else {
            track(mob);
        }
    }

    /** Removes the mob's own goals so the program is the only thing driving it. */
    static void strip(MobEntity mob) {
        mob.goalSelector.clear(goal -> true);
        mob.targetSelector.clear(goal -> true);
        mob.setTarget(null);
        mob.getNavigation().stop();
    }

    // ---- running ----

    public static void track(Entity entity) {
        if (!(entity instanceof MobEntity mob) || !isNode(mob) || mob.getEntityWorld().isClient()) {
            return;
        }
        String text = mob.getAttached(ModAttachments.BIO_PROGRAM);
        Program program = Program.parse(text);
        ProgramState state = read(mob.getAttached(ModAttachments.BIO_STATE), program);
        Node node = new Node(mob, program, state, readBuffer(mob.getAttached(ModAttachments.BIO_BUFFER)));
        NODES.put(mob.getUuid(), node);
        strip(mob);
    }

    public static void untrack(Entity entity) {
        Node node = NODES.remove(entity.getUuid());
        if (node != null) {
            save(node);
            if (entity.getRemovalReason() == Entity.RemovalReason.KILLED && entity.getEntityWorld() instanceof ServerWorld world) {
                for (ItemStack stack : node.buffer) {
                    Block.dropStack(world, entity.getBlockPos(), stack);
                }
                node.buffer.clear();
            }
        }
    }

    public static void clear() {
        NODES.clear();
    }

    public static void tick(long now) {
        if (now % 10 != 0) {
            return;
        }
        Iterator<Node> it = NODES.values().iterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (node.mob.isRemoved() || !node.mob.isAlive()) {
                it.remove();
                continue;
            }
            if (!(node.mob.getEntityWorld() instanceof ServerWorld world) || node.program.size() == 0) {
                continue;
            }
            Long frozen = node.mob.getAttached(ModAttachments.FROZEN_UNTIL);
            if (frozen != null && now < frozen) {
                continue; // a Breakpoint holds it
            }
            if (node.state.halted) {
                continue;
            }
            Interpreter.tick(node.program, node.state, new MobAgent(world, node));
            save(node);
            if (node.state.halted) {
                world.spawnParticles(ParticleTypes.SMOKE, node.mob.getX(), node.mob.getBodyY(1.0), node.mob.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    /** Restarts a halted node from line one. Used by the Breakpoint Wand's step. */
    public static boolean restart(MobEntity mob) {
        Node node = NODES.get(mob.getUuid());
        if (node == null || node.program.size() == 0) {
            return false;
        }
        node.state.reset(node.program);
        save(node);
        return true;
    }

    /** What the Breakpoint Wand shows when it inspects a node. */
    public static List<String> inspect(MobEntity mob) {
        Node node = NODES.get(mob.getUuid());
        List<String> out = new ArrayList<>();
        if (node == null) {
            return out;
        }
        out.add("pc=" + node.state.pc + " failed=" + node.state.failed + " halted=" + node.state.halted
                + " buffer=" + node.buffer.size() + "/" + BUFFER_STACKS);
        for (String line : node.program.listing()) {
            out.add((line.startsWith(String.format("%02d", node.state.pc)) ? "> " : "  ") + line);
        }
        return out;
    }

    private static void save(Node node) {
        node.mob.setAttached(ModAttachments.BIO_STATE, write(node.state));
        node.mob.setAttached(ModAttachments.BIO_BUFFER, writeBuffer(node.buffer));
    }

    static String write(ProgramState s) {
        return s.pc + "," + s.timer + "," + s.elapsed + "," + s.failed + "," + s.halted;
    }

    static ProgramState read(String text, Program program) {
        ProgramState state = new ProgramState();
        state.reset(program);
        if (text == null || text.isEmpty()) {
            return state;
        }
        String[] f = text.split(",");
        try {
            return new ProgramState(Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]),
                    Boolean.parseBoolean(f[3]), Boolean.parseBoolean(f[4]));
        } catch (RuntimeException e) {
            return state;
        }
    }

    static String writeBuffer(List<ItemStack> buffer) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack stack : buffer) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(';');
            }
            sb.append(Registries.ITEM.getId(stack.getItem())).append('*').append(stack.getCount());
        }
        return sb.toString();
    }

    static List<ItemStack> readBuffer(String text) {
        List<ItemStack> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        for (String part : text.split(";")) {
            int star = part.lastIndexOf('*');
            if (star <= 0) {
                continue;
            }
            Identifier id = Identifier.tryParse(part.substring(0, star));
            if (id == null || !Registries.ITEM.containsId(id)) {
                continue;
            }
            try {
                out.add(new ItemStack(Registries.ITEM.get(id), Integer.parseInt(part.substring(star + 1))));
            } catch (NumberFormatException ignored) {
                // a corrupted line of memory; drop it
            }
        }
        return out;
    }

    static final class Node {
        final MobEntity mob;
        Program program;
        ProgramState state;
        final List<ItemStack> buffer;

        Node(MobEntity mob, Program program, ProgramState state, List<ItemStack> buffer) {
            this.mob = mob;
            this.program = program;
            this.state = state;
            this.buffer = buffer;
        }

        UUID owner() {
            String id = mob.getAttached(ModAttachments.OWNER);
            try {
                return id == null ? null : UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        BlockPos home() {
            Long home = mob.getAttached(ModAttachments.BIO_HOME);
            return home == null ? mob.getBlockPos() : BlockPos.fromLong(home);
        }
    }
}
