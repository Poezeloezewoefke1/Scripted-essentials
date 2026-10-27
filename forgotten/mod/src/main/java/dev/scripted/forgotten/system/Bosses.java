package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.item.MemoryItem;
import dev.scripted.forgotten.logic.boss.ActionMemory;
import dev.scripted.forgotten.logic.boss.CuratorProfile;
import dev.scripted.forgotten.logic.boss.StackDepth;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.nesting.Mass;
import dev.scripted.forgotten.logic.nesting.NestingInstability;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Displays;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * The bosses. Each is a vanilla body the mod drives - a scaled spider, a crowned wither
 * skeleton, a giant creaking, an evoker, an iron golem made of errors, a tower of the world's
 * most-placed blocks, a ravager that comes for your first Frame Anchor - with a boss bar and
 * a rule of its own about how it can be hurt, taken from the logic module.
 */
public final class Bosses {
    public enum Kind {
        RECURSION_QUEEN("The Recursion Queen", BossBar.Color.PURPLE),
        KING_OF_YESTERDAY("The King of Yesterday", BossBar.Color.YELLOW),
        GARDENER("The Gardener", BossBar.Color.GREEN),
        CARTOGRAPHER("The Cartographer", BossBar.Color.BLUE),
        COMPILER("The Compiler", BossBar.Color.WHITE),
        CURATOR("The Curator", BossBar.Color.PINK),
        UNWRAPPER("The Unwrapper", BossBar.Color.RED);

        final String title;
        final BossBar.Color color;

        Kind(String title, BossBar.Color color) {
            this.title = title;
            this.color = color;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        static Kind byId(String id) {
            for (Kind k : values()) {
                if (k.id().equals(id)) {
                    return k;
                }
            }
            return null;
        }
    }

    static final class Boss {
        final Kind kind;
        final MobEntity body;
        final ServerBossBar bar;
        final ActionMemory actions = new ActionMemory();
        final StackDepth stack = new StackDepth();
        double fed;
        CuratorProfile curator;

        Boss(Kind kind, MobEntity body) {
            this.kind = kind;
            this.body = body;
            this.bar = new ServerBossBar(Text.literal(kind.title), kind.color, BossBar.Style.NOTCHED_10);
        }
    }

    private static final Map<UUID, Boss> BOSSES = new HashMap<>();
    private static final SplittableRandom RANDOM = new SplittableRandom();
    private static final Identifier BOSS_HEALTH = Forgotten.id("boss_health");
    private static final Identifier BOSS_SCALE = Forgotten.id("boss_scale");
    private static final Identifier BOSS_DAMAGE = Forgotten.id("boss_damage");
    private static final Identifier UNWRAPPER_SLOW = Forgotten.id("unwrapper_slow");

    private Bosses() {
    }

    public static boolean alive(Kind kind) {
        return BOSSES.values().stream().anyMatch(b -> b.kind == kind && b.body.isAlive());
    }

    /** The Compiler's stack, for inspection (the Breakpoint Wand, tests). Null for anything else. */
    public static StackDepth stackOf(Entity entity) {
        Boss boss = BOSSES.get(entity.getUuid());
        return boss == null ? null : boss.stack;
    }

    /** How much mass the Unwrapper has eaten. */
    public static double fedOf(Entity entity) {
        Boss boss = BOSSES.get(entity.getUuid());
        return boss == null ? 0 : boss.fed;
    }

    public static Kind kindOf(Entity entity) {
        String id = entity.getAttached(ModAttachments.BOSS);
        return id == null ? null : Kind.byId(id);
    }

    // ---- summoning ----

    public static MobEntity summon(Kind kind, ServerWorld world, BlockPos at) {
        if (alive(kind)) {
            return null;
        }
        EntityType<? extends MobEntity> type = switch (kind) {
            case RECURSION_QUEEN -> EntityType.SPIDER;
            case KING_OF_YESTERDAY -> EntityType.WITHER_SKELETON;
            case GARDENER -> EntityType.CREAKING;
            case CARTOGRAPHER -> EntityType.EVOKER;
            case COMPILER -> EntityType.IRON_GOLEM;
            case CURATOR -> EntityType.ZOMBIE;
            case UNWRAPPER -> EntityType.RAVAGER;
        };
        MobEntity body = type.create(world, SpawnReason.EVENT);
        if (body == null) {
            return null;
        }
        body.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        body.setAttached(ModAttachments.BOSS, kind.id());
        body.setPersistent();
        body.setCustomName(Text.literal(kind.title).formatted(Formatting.BOLD));
        double health = switch (kind) {
            case RECURSION_QUEEN -> 300;
            case KING_OF_YESTERDAY -> 260;
            case GARDENER -> 280;
            case CARTOGRAPHER -> 220;
            case COMPILER -> 100;
            case CURATOR -> CuratorProfile.from(Reality.memory(world.getServer()).chronicle).health();
            case UNWRAPPER -> 400;
        };
        double scale = switch (kind) {
            case RECURSION_QUEEN -> 3.0;
            case KING_OF_YESTERDAY -> 1.5;
            case GARDENER -> 2.2;
            case CARTOGRAPHER -> 1.4;
            case COMPILER -> 1.6;
            case CURATOR -> 2.4;
            case UNWRAPPER -> 1.6;
        };
        Creatures.modifier(body, EntityAttributes.MAX_HEALTH, BOSS_HEALTH, health - body.getMaxHealth());
        Creatures.modifier(body, EntityAttributes.SCALE, BOSS_SCALE, scale - 1.0);
        if (body.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE) != null) {
            Creatures.modifier(body, EntityAttributes.ATTACK_DAMAGE, BOSS_DAMAGE, 6.0);
        }
        body.setHealth(body.getMaxHealth());
        dress(world, body, kind);
        world.spawnEntity(body);
        Boss boss = adopt(body);
        if (boss != null && kind == Kind.CURATOR) {
            boss.curator = CuratorProfile.from(Reality.memory(world.getServer()).chronicle);
            assemble(world, boss);
        }
        world.playSound(null, at, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.5f, 0.6f);
        for (ServerPlayerEntity player : world.getPlayers(p -> p.squaredDistanceTo(body) < 64 * 64)) {
            Texts.tell(player, Texts.glitch(kind.title.toUpperCase(java.util.Locale.ROOT)));
        }
        return body;
    }

    static void dress(ServerWorld world, MobEntity body, Kind kind) {
        switch (kind) {
            case KING_OF_YESTERDAY -> {
                body.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                body.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            }
            case COMPILER -> {
                body.setGlowing(true);
                BioNodes.strip(body);
            }
            case CURATOR -> {
                body.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 0, false, false));
                body.setSilent(true);
            }
            case UNWRAPPER -> BioNodes.strip(body);
            default -> {
            }
        }
    }

    /** The Curator's body: the blocks this world was built from most, stacked into a figure. */
    static void assemble(ServerWorld world, Boss boss) {
        List<String> blocks = boss.curator.bodyBlocks();
        Entity below = boss.body;
        for (int i = 0; i < 3; i++) {
            Identifier id = Identifier.tryParse(blocks.get(i % blocks.size()));
            BlockState state = id != null && Registries.BLOCK.containsId(id)
                    ? Registries.BLOCK.get(id).getDefaultState() : ModBlocks.MEMORY_BLOCK.getDefaultState();
            float size = 1.6f - i * 0.3f;
            var part = Displays.block(world, boss.body.getEntityPos(), state, size, size, size, false);
            if (part != null) {
                part.startRiding(below, true, false);
                below = part;
            }
        }
    }

    /** Re-attaches the mod's control to a boss body when it loads. */
    public static Boss adopt(Entity entity) {
        if (!(entity instanceof MobEntity body) || body.getEntityWorld().isClient()) {
            return null;
        }
        Kind kind = kindOf(body);
        if (kind == null) {
            return null;
        }
        Boss boss = BOSSES.computeIfAbsent(body.getUuid(), u -> new Boss(kind, body));
        if (kind == Kind.COMPILER || kind == Kind.UNWRAPPER) {
            BioNodes.strip(body);
        }
        return boss;
    }

    public static void untrack(Entity entity) {
        Boss boss = BOSSES.remove(entity.getUuid());
        if (boss != null) {
            boss.bar.clearPlayers();
        }
    }

    public static void clear() {
        for (Boss boss : BOSSES.values()) {
            boss.bar.clearPlayers();
        }
        BOSSES.clear();
    }

    // ---- per tick ----

    public static void tick(MinecraftServer server, long now) {
        if (BOSSES.isEmpty()) {
            return;
        }
        for (Boss boss : new ArrayList<>(BOSSES.values())) {
            MobEntity body = boss.body;
            if (body.isRemoved() || !(body.getEntityWorld() instanceof ServerWorld world)) {
                boss.bar.clearPlayers();
                BOSSES.remove(body.getUuid());
                continue;
            }
            if (now % 10 == 0) {
                bar(world, boss);
            }
            if (boss.kind == Kind.COMPILER && boss.stack.tickCountdown()) {
                delete(world, boss);
            }
            if ((now + body.getId()) % 20 == 0) {
                act(world, boss, now);
            }
        }
    }

    static void bar(ServerWorld world, Boss boss) {
        MobEntity body = boss.body;
        float percent = switch (boss.kind) {
            case COMPILER -> 1f - boss.stack.crashes() / (float) StackDepth.CRASHES_TO_DEFEAT
                    - boss.stack.depth() / (float) (boss.stack.phaseSize() * 3 * StackDepth.CRASHES_TO_DEFEAT);
            case UNWRAPPER -> Math.min(body.getHealth() / body.getMaxHealth(),
                    1f - (float) (boss.fed / NestingInstability.unwrapperMass(dev.scripted.forgotten.logic.nesting.Depths.MAX)));
            default -> body.getHealth() / body.getMaxHealth();
        };
        boss.bar.setPercent(Math.max(0f, Math.min(1f, percent)));
        if (boss.kind == Kind.COMPILER) {
            boss.bar.setName(Text.literal(boss.kind.title + "  stack depth " + boss.stack.depth() + "  " + boss.stack.phase()
                    + (boss.stack.phase() == StackDepth.Phase.COMPILE ? "  delete in " + boss.stack.countdown() / 20 + "s" : "")));
        }
        for (ServerPlayerEntity player : world.getPlayers()) {
            boolean near = player.squaredDistanceTo(body) < 64 * 64;
            if (near && !boss.bar.getPlayers().contains(player)) {
                boss.bar.addPlayer(player);
            } else if (!near && boss.bar.getPlayers().contains(player)) {
                boss.bar.removePlayer(player);
            }
        }
    }

    static void act(ServerWorld world, Boss boss, long now) {
        MobEntity body = boss.body;
        Random random = world.getRandom();
        ServerPlayerEntity target = (ServerPlayerEntity) world.getClosestPlayer(body.getX(), body.getY(), body.getZ(), 48,
                e -> e instanceof ServerPlayerEntity p && !p.isSpectator() && !p.isCreative());
        switch (boss.kind) {
            case RECURSION_QUEEN -> {
                if (random.nextInt(5) == 0) {
                    for (int i = 0; i < 2; i++) {
                        var child = EntityType.SPIDER.create(world, SpawnReason.EVENT);
                        if (child != null) {
                            child.refreshPositionAndAngles(body.getX(), body.getY(), body.getZ(), 0f, 0f);
                            child.setAttached(ModAttachments.RECURSION, 3);
                            Creatures.shape(child, 3);
                            world.spawnEntity(child);
                        }
                    }
                }
            }
            case KING_OF_YESTERDAY -> {
                if (target != null && random.nextInt(4) == 0) {
                    replay(world, boss, target);
                }
            }
            case GARDENER -> {
                if (target != null && target.squaredDistanceTo(body) < 10 * 10) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));
                    world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, target.getX(), target.getY() + 0.2, target.getZ(), 12, 0.4, 0.1, 0.4, 0.0);
                }
                BlockPos under = body.getBlockPos().down();
                if (world.getBlockState(under).isIn(net.minecraft.registry.tag.BlockTags.LOGS)
                        || world.getBlockState(under).isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) {
                    body.heal(4f);
                }
                if (random.nextInt(3) == 0) {
                    BlockPos p = Anomalies.surfaceNear(world, body.getBlockPos(), random, 8);
                    if (p != null && net.minecraft.block.Blocks.OAK_SAPLING.getDefaultState().canPlaceAt(world, p)) {
                        world.setBlockState(p, net.minecraft.block.Blocks.OAK_SAPLING.getDefaultState(), Block.NOTIFY_ALL);
                    }
                }
            }
            case CARTOGRAPHER -> {
                if (target != null && random.nextInt(6) == 0) {
                    BlockPos to = Anomalies.surfaceNear(world, body.getBlockPos(), random, 24);
                    if (to != null) {
                        target.requestTeleport(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
                        Texts.bar(target, Texts.world("The map has been redrawn. You are somewhere else on it."));
                    }
                }
            }
            case COMPILER -> {
                if (target == null) {
                    return;
                }
                body.getNavigation().startMovingTo(target, 1.0);
                if (body.squaredDistanceTo(target) < 9) {
                    target.damage(world, world.getDamageSources().mobAttack(body), 6f);
                }
                switch (boss.stack.phase()) {
                    case PARSE -> {
                        if (random.nextInt(4) == 0) {
                            ZombieEntity z = EntityType.ZOMBIE.create(world, SpawnReason.EVENT);
                            if (z != null) {
                                z.refreshPositionAndAngles(body.getX() + random.nextInt(7) - 3, body.getY(), body.getZ() + random.nextInt(7) - 3, 0f, 0f);
                                world.spawnEntity(z);
                                GlitchKind[] kinds = GlitchKind.values();
                                Glitches.apply(z, kinds[random.nextInt(kinds.length)]);
                            }
                        }
                    }
                    case EXECUTE -> {
                        if (random.nextInt(3) == 0) {
                            Unraveling.invert(target, Unraveling.INVERT_TICKS / 2);
                        } else if (random.nextInt(3) == 0) {
                            Anomalies.gravityFlip(world, target, random);
                        }
                    }
                    case COMPILE -> world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, body.getX(), body.getBodyY(1.0), body.getZ(),
                            20, 0.6, 0.6, 0.6, 0.3);
                }
            }
            case CURATOR -> {
                if (target != null && boss.curator != null && random.nextInt(5) == 0) {
                    Identifier id = Identifier.tryParse(boss.curator.servant());
                    if (id != null && Registries.ENTITY_TYPE.containsId(id)) {
                        Entity servant = Registries.ENTITY_TYPE.get(id).create(world, SpawnReason.EVENT);
                        if (servant instanceof MobEntity mob) {
                            mob.refreshPositionAndAngles(body.getX() + random.nextInt(9) - 4, body.getY(), body.getZ() + random.nextInt(9) - 4, 0f, 0f);
                            world.spawnEntity(mob);
                            mob.setTarget(target);
                        }
                    }
                }
                if (target != null && random.nextInt(8) == 0) {
                    throwBack(world, target);
                }
            }
            case UNWRAPPER -> unwrap(world, boss);
        }
    }

    // ---- the King of Yesterday ----

    /** Something a player did near the King is remembered; returns the damage multiplier it earns. */
    public static float remember(ServerWorld world, Vec3d at, ActionMemory.Action action) {
        float multiplier = 1f;
        for (Boss boss : BOSSES.values()) {
            if (boss.kind == Kind.KING_OF_YESTERDAY && boss.body.getEntityWorld() == world && boss.body.squaredDistanceTo(at) < 32 * 32) {
                multiplier = boss.actions.remember(action);
            }
        }
        return multiplier;
    }

    static void replay(ServerWorld world, Boss boss, ServerPlayerEntity target) {
        ActionMemory.Action action = boss.actions.replay(RANDOM);
        if (action == null) {
            return;
        }
        MobEntity king = boss.body;
        switch (action.kind()) {
            case MELEE -> {
                Identifier id = Identifier.tryParse(action.signature());
                if (id != null && Registries.ITEM.containsId(id)) {
                    king.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Registries.ITEM.get(id)));
                }
                Vec3d to = target.getEntityPos().subtract(king.getEntityPos()).normalize().multiply(1.2);
                king.setVelocity(to.x, 0.4, to.z);
                king.velocityDirty = true;
                target.damage(world, world.getDamageSources().mobAttack(king), Math.max(4f, action.magnitude()));
            }
            case PROJECTILE -> {
                ArrowEntity arrow = new ArrowEntity(world, king, new ItemStack(Items.ARROW), null);
                arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
                Vec3d d = target.getEyePos().subtract(arrow.getEntityPos());
                arrow.setVelocity(d.x, d.y + Math.sqrt(d.x * d.x + d.z * d.z) * 0.15, d.z, 2.0f, 2f);
                world.spawnEntity(arrow);
            }
            case BLOCK_PLACED -> {
                Identifier id = Identifier.tryParse(action.signature());
                BlockPos at = target.getBlockPos();
                if (id != null && Registries.BLOCK.containsId(id) && world.getBlockState(at).isAir()) {
                    BlockState state = Registries.BLOCK.get(id).getDefaultState();
                    if (!state.hasBlockEntity() && state.canPlaceAt(world, at)) {
                        world.setBlockState(at, state, Block.NOTIFY_ALL);
                    }
                }
            }
            case POTION, PARADOX -> target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
        }
        Texts.bar(target, Texts.world("It remembers you doing that."));
    }

    // ---- the Compiler ----

    /** An error near the Compiler pushes its stack deeper. */
    public static void error(ServerWorld world, Vec3d at, StackDepth.ErrorKind kind) {
        for (Boss boss : new ArrayList<>(BOSSES.values())) {
            if (boss.kind == Kind.COMPILER && boss.body.getEntityWorld() == world && boss.body.squaredDistanceTo(at) < 24 * 24) {
                stackEvent(world, boss, boss.stack.error(kind));
            }
        }
    }

    static void stackEvent(ServerWorld world, Boss boss, StackDepth.Event event) {
        MobEntity body = boss.body;
        switch (event) {
            case EXPECTED -> world.spawnParticles(ParticleTypes.SMOKE, body.getX(), body.getBodyY(0.8), body.getZ(), 6, 0.3, 0.3, 0.3, 0.01);
            case DEEPER, PHASE_CHANGED -> {
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, body.getX(), body.getBodyY(0.8), body.getZ(), 20, 0.5, 0.5, 0.5, 0.2);
                world.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 0.6f, 1.8f);
            }
            case CRASHED -> {
                world.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.5f, 0.5f);
                world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, body.getX(), body.getBodyY(0.5), body.getZ(), 1, 0, 0, 0, 0);
                Block.dropStack(world, body.getBlockPos(), new ItemStack(ModItems.NULL_SHARD, 2));
                for (ServerPlayerEntity p : world.getPlayers(p -> p.squaredDistanceTo(body) < 48 * 48)) {
                    Texts.tell(p, Texts.glitch("The Compiler has crashed. Rebooting."));
                }
            }
            case TERMINATED -> {
                body.setHealth(0.01f);
                body.damage(world, world.getDamageSources().outOfWorld(), 1000f);
            }
            default -> {
            }
        }
    }

    /** The compile countdown ran out: the Compiler deletes something of yours. */
    static void delete(ServerWorld world, Boss boss) {
        for (ServerPlayerEntity player : world.getPlayers(p -> p.squaredDistanceTo(boss.body) < 32 * 32 && !p.isCreative())) {
            int slot = world.getRandom().nextInt(9);
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                player.getInventory().setStack(slot, ItemStack.EMPTY);
                Texts.tell(player, Texts.glitch("deleted: " + stack.getName().getString()));
            }
            player.damage(world, world.getDamageSources().magic(), 12f);
        }
    }

    // ---- the Curator ----

    /** It throws your old inventories back at you: ghosts of what you carried, untouchable, gone in seconds. */
    static void throwBack(ServerWorld world, ServerPlayerEntity target) {
        var memory = Reality.player(target);
        if (memory.snapshots.isEmpty()) {
            return;
        }
        var snapshot = memory.snapshots.get(world.getRandom().nextInt(memory.snapshots.size()));
        var list = snapshot.getListOrEmpty("items");
        var inv = new net.minecraft.inventory.SimpleInventory(41);
        Snapshots.read(list, inv, world.getRegistryManager());
        int thrown = 0;
        for (int i = 0; i < inv.size() && thrown < 6; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity ghost = new ItemEntity(world, target.getX(), target.getY() + 1.5, target.getZ(), stack);
            ghost.setPickupDelayInfinite();
            ghost.setVelocity(world.getRandom().nextGaussian() * 0.2, 0.3, world.getRandom().nextGaussian() * 0.2);
            world.spawnEntity(ghost);
            dev.scripted.forgotten.util.Scheduler.after(80, ghost::discard);
            thrown++;
        }
        if (thrown > 0) {
            Texts.bar(target, Texts.world("This is what you carried, once."));
        }
    }

    // ---- the Unwrapper ----

    /** It walks toward the first Frame Anchor ever placed and eats mass thrown to it. */
    static void unwrap(ServerWorld world, Boss boss) {
        MobEntity body = boss.body;
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, body.getBoundingBox().expand(2), Entity::isAlive)) {
            ItemStack stack = item.getStack();
            boss.fed += Mass.of(Registries.ITEM.getId(stack.getItem()).toString()) * stack.getCount();
            item.discard();
            world.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1f, 0.5f);
        }
        double needed = NestingInstability.unwrapperMass(dev.scripted.forgotten.logic.nesting.Depths.MAX);
        Creatures.modifier(body, EntityAttributes.MOVEMENT_SPEED, UNWRAPPER_SLOW, -Math.min(0.95, boss.fed / needed),
                net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (boss.fed >= needed) {
            body.setHealth(0.01f);
            body.damage(world, world.getDamageSources().outOfWorld(), 1000f);
            return;
        }
        WorldMemory memory = Reality.memory(world.getServer());
        memory.frames.values().stream().filter(f -> f.dimension().equals(world.getRegistryKey().getValue().toString()))
                .min(java.util.Comparator.comparingInt(WorldMemory.FrameRecord::id)).ifPresent(first -> {
                    BlockPos anchor = BlockPos.fromLong(first.pos());
                    body.getNavigation().startMovingTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5, 1.0);
                    if (body.getBlockPos().isWithinDistance(anchor, 3) && world.getBlockState(anchor).isOf(ModBlocks.FRAME_ANCHOR)) {
                        world.breakBlock(anchor, false);
                        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
                            Texts.tell(p, Texts.glitch("The first frame has been opened. Everything inside it is loose."));
                        }
                    }
                });
    }

    // ---- damage ----

    /**
     * Fabric damage hook. Each boss decides how it may be hurt. Returns false to cancel the
     * vanilla damage (the boss may have applied its own instead).
     */
    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof MobEntity body) || !(body.getEntityWorld() instanceof ServerWorld world)) {
            return true;
        }
        Boss boss = BOSSES.get(body.getUuid());
        if (boss == null) {
            return true;
        }
        Entity attacker = source.getAttacker();
        switch (boss.kind) {
            case KING_OF_YESTERDAY -> {
                if (!(attacker instanceof PlayerEntity player)) {
                    return true;
                }
                boolean ranged = source.getSource() instanceof ProjectileEntity;
                String signature = ranged ? Registries.ENTITY_TYPE.getId(source.getSource().getType()).toString()
                        : Registries.ITEM.getId(player.getMainHandStack().getItem()).toString();
                float multiplier = boss.actions.remember(new ActionMemory.Action(ranged ? ActionMemory.Kind.PROJECTILE : ActionMemory.Kind.MELEE,
                        signature, body.getBlockX(), body.getBlockY(), body.getBlockZ(), amount));
                float dealt = amount * multiplier;
                if (dealt <= 0f) {
                    if (player instanceof ServerPlayerEntity sp) {
                        Texts.bar(sp, Texts.world("It has seen that before. It does not work twice."));
                    }
                    return false;
                }
                if (body.getHealth() - dealt <= 0f) {
                    body.setHealth(0.01f);
                    return true;
                }
                body.setHealth(body.getHealth() - dealt);
                body.timeUntilRegen = 10;
                return false;
            }
            case COMPILER -> {
                StackDepth.ErrorKind kind = null;
                if (attacker instanceof PlayerEntity player) {
                    ItemStack held = player.getMainHandStack();
                    if (held.isOf(ModItems.UNDEFINED_SWORD) || held.isOf(ModItems.NULL_SHARD)) {
                        kind = StackDepth.ErrorKind.NULL_WEAPON;
                    } else if (!held.isIn(ItemTags.SWORDS) && !(held.getItem() instanceof AxeItem) && !held.isOf(Items.TRIDENT)
                            && !held.isOf(Items.MACE)) {
                        kind = StackDepth.ErrorKind.WRONG_TOOL;
                    }
                } else if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_EXPLOSION)) {
                    kind = StackDepth.ErrorKind.CONTRADICTORY_STATE;
                }
                if (kind != null) {
                    stackEvent(world, boss, boss.stack.error(kind));
                } else {
                    stackEvent(world, boss, StackDepth.Event.EXPECTED);
                }
                return false;
            }
            case UNWRAPPER -> {
                return !(source.getAttacker() instanceof net.minecraft.entity.mob.HostileEntity);
            }
            default -> {
                return true;
            }
        }
    }

    /** A boss died: rewards, the chronicle, insight for everyone who fought. */
    public static void died(LivingEntity entity, DamageSource source) {
        Kind kind = kindOf(entity);
        if (kind == null || !(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        BlockPos at = entity.getBlockPos();
        List<ItemStack> drops = new ArrayList<>();
        switch (kind) {
            case RECURSION_QUEEN -> {
                drops.add(new ItemStack(ModItems.concept(Concept.RECURSION), 2));
                drops.add(new ItemStack(ModItems.PARADOX_FRAGMENT, 6));
            }
            case KING_OF_YESTERDAY -> {
                drops.add(MemoryItem.of(ModItems.MEMORY, "forgotten:king_of_yesterday"));
                drops.add(new ItemStack(ModItems.concept(Concept.CAUSALITY), 2));
            }
            case GARDENER -> {
                drops.add(new ItemStack(ModItems.concept(Concept.POSSIBILITY), 2));
                drops.add(new ItemStack(Items.OAK_SAPLING, 16));
            }
            case CARTOGRAPHER -> {
                drops.add(new ItemStack(ModItems.concept(Concept.DISTANCE), 2));
                drops.add(new ItemStack(Items.FILLED_MAP));
            }
            case COMPILER -> {
                drops.add(new ItemStack(ModItems.SOURCE_CODE));
                drops.add(new ItemStack(ModItems.NULL_SHARD, 4));
            }
            case CURATOR -> {
                drops.add(new ItemStack(ModItems.concept(Concept.MEMORY), 3));
                drops.add(dev.scripted.forgotten.util.Lore.book(world.getRandom()));
            }
            case UNWRAPPER -> {
                drops.add(new ItemStack(ModItems.concept(Concept.FINALITY), 2));
                drops.add(new ItemStack(ModBlocks.FRAME_ANCHOR, 2));
                for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
                    Reality.player(p).instability = 0;
                }
            }
        }
        for (ItemStack stack : drops) {
            Block.dropStack(world, at, stack);
        }
        WorldMemory memory = Reality.memory(world.getServer());
        memory.setFlag("defeated_" + kind.id());
        Reality.chronicle(world, ChronicleEvent.BOSS_DEFEATED, at, "", kind.id());
        for (ServerPlayerEntity p : world.getPlayers(p -> p.squaredDistanceTo(entity) < 64 * 64)) {
            Reality.insight(p, Insight.BOSS_DEFEATED);
            Reality.advance(p, "boss_" + kind.id());
        }
        untrack(entity);
    }
}
