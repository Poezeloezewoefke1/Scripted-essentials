package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.genesis.Ending;
import dev.scripted.forgotten.logic.genesis.GenesisRule;
import dev.scripted.forgotten.logic.genesis.Law;
import dev.scripted.forgotten.logic.genesis.LawSubject;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModComponents;
import dev.scripted.forgotten.util.Scheduler;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Genesis Interface's consequences: the world rules written once the Author has asked its
 * question, the thirty-six laws only the Author can write, and the five endings. Every rule and
 * law is an ongoing change implemented here, run from the tick loop and the game's hooks.
 */
public final class Genesis {
    public static final String AUTHOR_ASKED = "author_asked";
    public static final String BEGINNING_REVEALED = "beginning_revealed";
    private static final Identifier FINAL_KNOCKBACK = Forgotten.id("law_finality");
    /** Entities that have used their one return under the Recursion law, and when. */
    private static final Map<UUID, Long> RETURNED = new HashMap<>();
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> false);

    private Genesis() {
    }

    // ---- state ----

    public static boolean endingsOpen(WorldMemory memory) {
        return memory.rule(GenesisRule.REWRITE_THE_BEGINNING) && memory.flag(BEGINNING_REVEALED);
    }

    public static boolean isAuthor(WorldMemory memory, ServerPlayerEntity player) {
        return memory.ending() == Ending.AUTHOR && player.getUuid().equals(memory.author());
    }

    static WorldMemory memory(Entity entity) {
        return Reality.memory(entity.getEntityWorld().getServer());
    }

    static boolean law(MinecraftServer server, LawSubject subject, Concept concept) {
        WorldMemory memory = Reality.memory(server);
        return memory.ending() == Ending.AUTHOR && memory.law(new Law(subject, concept));
    }

    static boolean law(Entity entity, Concept concept) {
        LawSubject subject = entity instanceof PlayerEntity ? LawSubject.PLAYERS : LawSubject.CREATURES;
        return law(entity.getEntityWorld().getServer(), subject, concept);
    }

    // ---- writing ----

    public static void toggleRule(ServerPlayerEntity player, GenesisRule rule) {
        WorldMemory memory = memory(player);
        if (!memory.flag(AUTHOR_ASKED)) {
            Texts.bar(player, Texts.world("Nothing has asked you yet."));
            return;
        }
        if (rule == GenesisRule.REWRITE_THE_BEGINNING && !memory.flag(BEGINNING_REVEALED)) {
            Texts.bar(player, Texts.world("You cannot rewrite a beginning you have never seen. Find the First World."));
            return;
        }
        boolean on = !memory.rule(rule);
        memory.setRule(rule, on);
        ServerWorld world = Reality.world(player);
        Reality.chronicle(world, ChronicleEvent.REALITY_EDIT, player.getBlockPos(), player.getName().getString(),
                (on ? "+" : "-") + rule.name());
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
            Texts.tell(p, Texts.world((on ? "" : "No longer: ") + rule.statement()));
        }
    }

    public static void toggleLaw(ServerPlayerEntity player, Law law) {
        WorldMemory memory = memory(player);
        if (!isAuthor(memory, player)) {
            Texts.bar(player, Texts.world("Only the Author writes laws."));
            return;
        }
        boolean on = memory.toggleLaw(law);
        if (!on && law.concept() == Concept.FINALITY) {
            for (ServerWorld w : player.getEntityWorld().getServer().getWorlds()) {
                for (Entity e : w.iterateEntities()) {
                    if (e instanceof LivingEntity living) {
                        var inst = living.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
                        if (inst != null) {
                            inst.removeModifier(FINAL_KNOCKBACK);
                        }
                    }
                }
            }
        }
        if (!on && law.concept() == Concept.SILENCE) {
            for (ServerWorld w : player.getEntityWorld().getServer().getWorlds()) {
                for (Entity e : w.iterateEntities()) {
                    if (e instanceof LivingEntity) {
                        e.setSilent(false);
                    }
                }
            }
        }
        Texts.tell(player, Texts.discovery((on ? "Enacted: " : "Repealed: ") + law.describe()));
    }

    // ---- endings ----

    /** Chooses how the world ends. Returns false if it cannot be chosen now. */
    public static boolean choose(ServerPlayerEntity player, Ending ending) {
        WorldMemory memory = memory(player);
        MinecraftServer server = player.getEntityWorld().getServer();
        if (memory.ending() != null || !endingsOpen(memory) || !ending.available(Reality.lucidity(server), true)) {
            return false;
        }
        ServerWorld world = Reality.world(player);
        memory.end(ending, player.getUuid());
        Reality.chronicle(world, ChronicleEvent.ENDING, player.getBlockPos(), player.getName().getString(), ending.name());
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            Texts.tell(p, Texts.discovery(ending.title().toUpperCase(java.util.Locale.ROOT)));
            Texts.tell(p, Texts.world(ending.description()));
            Reality.advance(p, "ending_" + ending.name().toLowerCase(java.util.Locale.ROOT));
        }
        Bosses.dismissAuthor(server);
        switch (ending) {
            case PRESERVE -> {
                RegionMemory region = Reality.region(world);
                region.chunk(ChunkPos.toLong(player.getBlockPos())).memorial = true;
                region.markDirty();
            }
            case RESTORE -> Texts.tell(player, Texts.world("Hold the Memory of the First World to phase between them."));
            case AUTHOR -> Texts.tell(player, Texts.world("The Genesis Interface will take your laws now."));
            case MERGE -> merge(world, player);
            case TERMINATE -> {
                Palimpsests.record(server, ending);
                Scheduler.after(100, () -> {
                    for (ServerPlayerEntity p : List.copyOf(server.getPlayerManager().getPlayerList())) {
                        p.networkHandler.disconnect(Text.literal("The world has ended. Make another. It will remember this one."));
                    }
                    server.stop(false);
                });
                return true;
            }
        }
        Palimpsests.record(server, ending);
        return true;
    }

    /** Merge: the player becomes part of the terrain. Their likeness stays, bearing their name. */
    static void merge(ServerWorld world, ServerPlayerEntity player) {
        BlockPos at = player.getBlockPos();
        world.setBlockState(at.down(), Blocks.LODESTONE.getDefaultState(), Block.NOTIFY_ALL);
        ArmorStandEntity heart = EntityType.ARMOR_STAND.create(world, SpawnReason.EVENT);
        if (heart != null) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponentTypes.PROFILE, ProfileComponent.ofStatic(player.getGameProfile()));
            heart.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYaw(), 0f);
            heart.equipStack(EquipmentSlot.HEAD, head);
            heart.setInvisible(true);
            heart.setInvulnerable(true);
            heart.setCustomName(Text.literal(player.getName().getString()).formatted(Formatting.GOLD));
            heart.setCustomNameVisible(true);
            heart.addCommandTag("forgotten_heartblock");
            world.spawnEntity(heart);
        }
        player.changeGameMode(GameMode.SPECTATOR);
        Texts.tell(player, Texts.world("You are the ground now, and the ground remembers your name."));
    }

    // ---- per tick ----

    public static void tick(MinecraftServer server, long now) {
        WorldMemory memory = Reality.memory(server);
        explosions(server, memory);
        if (now % 20 != 0) {
            return;
        }
        boolean laws = memory.ending() == Ending.AUTHOR;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = Reality.world(player);
            if (laws) {
                livingLaws(player, LawSubject.PLAYERS);
                for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(32), Entity::isAlive)) {
                    livingLaws(mob, LawSubject.CREATURES);
                }
                worldLaws(world, player, memory);
            }
            if (now % 100 == 0) {
                rules(world, player, memory);
            }
            if (memory.ending() == Ending.RESTORE && now % 100 == 0) {
                bleed(world, player);
            }
            if (memory.rule(GenesisRule.GRAVITY_OPTIONAL) && player.isSneaking() && !player.isOnGround() && !player.isTouchingWater()) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 30, 0, false, false));
            }
        }
    }

    static void effect(LivingEntity e, RegistryEntry<StatusEffect> effect, int amplifier) {
        e.addStatusEffect(new StatusEffectInstance(effect, 50, amplifier, true, false));
    }

    static void livingLaws(LivingEntity e, LawSubject subject) {
        MinecraftServer server = e.getEntityWorld().getServer();
        if (law(server, subject, Concept.GRAVITY)) {
            effect(e, StatusEffects.SLOW_FALLING, 0);
        }
        if (law(server, subject, Concept.MEMORY)) {
            effect(e, StatusEffects.REGENERATION, 0);
        }
        if (law(server, subject, Concept.DISTANCE)) {
            effect(e, StatusEffects.SPEED, 0);
        }
        if (law(server, subject, Concept.HUNGER) && e instanceof PlayerEntity) {
            effect(e, StatusEffects.SATURATION, 0);
        }
        if (law(server, subject, Concept.IDENTITY) && e instanceof MobEntity mob) {
            Glitches.heal(mob);
        }
        if (law(server, subject, Concept.IDENTITY) && e instanceof ServerPlayerEntity player) {
            Reality.player(player).hollowCursed = false;
        }
        if (law(server, subject, Concept.SILENCE)) {
            e.setSilent(true);
        }
        if (law(server, subject, Concept.FINALITY)) {
            Creatures.modifier(e, EntityAttributes.KNOCKBACK_RESISTANCE, FINAL_KNOCKBACK, 1.0);
        }
        if (law(server, subject, Concept.ABSENCE)
                && (e.isSneaking() || e.getEntityWorld().getLightLevel(e.getBlockPos()) < 4)) {
            effect(e, StatusEffects.INVISIBILITY, 0);
        }
        if (law(server, subject, Concept.WITNESS)) {
            effect(e, StatusEffects.GLOWING, 0);
        }
    }

    static void worldLaws(ServerWorld world, ServerPlayerEntity player, WorldMemory memory) {
        MinecraftServer server = world.getServer();
        Random random = world.getRandom();
        if (law(server, LawSubject.WORLD, Concept.WITNESS)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, true, false));
        }
        if (law(server, LawSubject.WORLD, Concept.HUNGER)) {
            // Crops grow three times as fast: two extra random ticks for crops near each player.
            BlockPos center = player.getBlockPos();
            for (int i = 0; i < 96; i++) {
                BlockPos p = center.add(random.nextInt(49) - 24, random.nextInt(9) - 4, random.nextInt(49) - 24);
                BlockState s = world.getBlockState(p);
                if (s.getBlock() instanceof CropBlock && s.hasRandomTicks()) {
                    s.randomTick(world, p, random);
                    s = world.getBlockState(p);
                    if (s.hasRandomTicks()) {
                        s.randomTick(world, p, random);
                    }
                }
            }
        }
        if (law(server, LawSubject.WORLD, Concept.MEMORY)) {
            RegionMemory region = Reality.region(world);
            RegionMemory.ChunkRecord r = region.peek(ChunkPos.toLong(player.getBlockPos()));
            if (r != null && !r.mined.isEmpty() && random.nextInt(4) == 0) {
                RegionMemory.Remembered m = r.mined.remove(r.mined.size() - 1);
                if (world.getBlockState(m.blockPos()).isAir()) {
                    world.setBlockState(m.blockPos(), m.state(), Block.NOTIFY_ALL);
                }
                region.markDirty();
            }
        }
    }

    static void rules(ServerWorld world, ServerPlayerEntity player, WorldMemory memory) {
        Random random = world.getRandom();
        if (memory.rule(GenesisRule.RUINS_GROW)) {
            RegionMemory region = Reality.region(world);
            RegionMemory.ChunkRecord r = region.peek(ChunkPos.toLong(player.getBlockPos()));
            if (r != null && !r.ruins.isEmpty()) {
                RegionMemory.Remembered ruin = r.ruins.remove(0);
                if (world.getBlockState(ruin.blockPos()).isAir() && !ruin.state().hasBlockEntity()) {
                    world.setBlockState(ruin.blockPos(), ruin.state(), Block.NOTIFY_ALL);
                }
                region.markDirty();
            }
        }
        if (memory.rule(GenesisRule.WORLDS_CONNECTED) && random.nextInt(6) == 0) {
            BlockPos at = Anomalies.surfaceNear(world, player.getBlockPos(), random, 32);
            if (at != null && !at.isWithinDistance(player.getBlockPos(), 8)) {
                boolean nether = world.getRegistryKey() == World.NETHER;
                for (BlockPos p : BlockPos.iterate(at.add(-1, -1, -1), at.add(1, -1, 1))) {
                    BlockState s = world.getBlockState(p);
                    if (!s.isAir() && !s.hasBlockEntity() && s.getHardness(world, p) >= 0) {
                        world.setBlockState(p, nether ? Blocks.MOSS_BLOCK.getDefaultState() : Blocks.NETHERRACK.getDefaultState(), Block.NOTIFY_ALL);
                    }
                }
                if (nether && world.getBlockState(at).isAir()) {
                    world.setBlockState(at, Blocks.AZALEA.getDefaultState(), Block.NOTIFY_ALL);
                }
            }
        }
    }

    /** The Restore ending: dimensions bleed into each other where people are. */
    static void bleed(ServerWorld world, ServerPlayerEntity player) {
        Random random = world.getRandom();
        if (random.nextInt(4) != 0) {
            return;
        }
        BlockPos at = Anomalies.surfaceNear(world, player.getBlockPos(), random, 24);
        if (at == null) {
            return;
        }
        BlockState[] from = {Blocks.END_STONE.getDefaultState(), Blocks.NETHERRACK.getDefaultState(),
                ModBlocks.MEMORY_BLOCK.getDefaultState(), ModBlocks.MARROWGLASS.getDefaultState(), Blocks.CALCITE.getDefaultState()};
        BlockState into = from[random.nextInt(from.length)];
        for (BlockPos p : BlockPos.iterate(at.add(-1, -1, -1), at.add(1, -1, 1))) {
            if (!world.getBlockState(p).isAir() && !world.getBlockState(p).hasBlockEntity()) {
                world.setBlockState(p, into, Block.NOTIFY_ALL);
            }
        }
    }

    // ---- explosions: Finality (nothing breaks) and Causality (it is undone) ----

    private record Snapshot(ServerWorld world, Map<BlockPos, BlockState> blocks) {
    }

    static void explosions(MinecraftServer server, WorldMemory memory) {
        if (memory.ending() != Ending.AUTHOR) {
            return;
        }
        boolean finality = memory.law(new Law(LawSubject.WORLD, Concept.FINALITY));
        boolean causality = memory.law(new Law(LawSubject.WORLD, Concept.CAUSALITY));
        if (!finality && !causality) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = Reality.world(player);
            for (Entity e : world.getOtherEntities(null, player.getBoundingBox().expand(64), Genesis::aboutToExplode)) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                for (BlockPos p : BlockPos.iterateOutwards(e.getBlockPos(), 6, 6, 6)) {
                    BlockState s = world.getBlockState(p);
                    if (!s.isAir() && !s.hasBlockEntity() && s.getFluidState().isEmpty()) {
                        blocks.put(p.toImmutable(), s);
                    }
                }
                Snapshot snapshot = new Snapshot(world, blocks);
                Scheduler.after(finality ? 2 : 100, () -> restore(snapshot));
            }
        }
    }

    static boolean aboutToExplode(Entity e) {
        if (e instanceof CreeperEntity c) {
            return c.currentFuseTime == c.fuseTime - 1;
        }
        return e instanceof TntEntity tnt && tnt.getFuse() == 1;
    }

    static void restore(Snapshot snapshot) {
        int restored = 0;
        for (Map.Entry<BlockPos, BlockState> e : snapshot.blocks().entrySet()) {
            BlockState now = snapshot.world().getBlockState(e.getKey());
            if (now.isAir() || now.isOf(Blocks.FIRE)) {
                snapshot.world().setBlockState(e.getKey(), e.getValue(), Block.NOTIFY_LISTENERS);
                restored++;
            }
        }
        if (restored > 0) {
            BlockPos any = snapshot.blocks().keySet().iterator().next();
            snapshot.world().playSound(null, any, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1f, 0.6f);
        }
    }

    // ---- hooks ----

    /** New entities under the world's laws and rules. */
    public static void onLoad(Entity entity, ServerWorld world) {
        MinecraftServer server = world.getServer();
        WorldMemory memory = Reality.memory(server);
        boolean authored = memory.ending() == Ending.AUTHOR;
        if (entity instanceof FallingBlockEntity falling && (memory.rule(GenesisRule.GRAVITY_OPTIONAL)
                || (authored && memory.law(new Law(LawSubject.WORLD, Concept.GRAVITY))))) {
            BlockPos at = falling.getBlockPos();
            if (world.getBlockState(at).isAir()) {
                world.setBlockState(at, falling.getBlockState(), Block.NOTIFY_ALL);
            }
            falling.discard();
            return;
        }
        if (entity instanceof AbstractPiglinEntity piglin && memory.rule(GenesisRule.WORLDS_CONNECTED)) {
            piglin.setImmuneToZombification(true);
        }
        if (!authored || entity.age > 0) {
            return;
        }
        if (entity instanceof ProjectileEntity projectile && memory.law(new Law(LawSubject.WORLD, Concept.DISTANCE))) {
            projectile.setVelocity(projectile.getVelocity().multiply(1.5));
        }
        if (entity instanceof HostileEntity hostile && memory.law(new Law(LawSubject.WORLD, Concept.SILENCE))) {
            hostile.setSilent(true);
        }
        if (entity instanceof HostileEntity hostile && memory.law(new Law(LawSubject.WORLD, Concept.ABSENCE))
                && entity.getCommandTags().isEmpty() && !hostile.hasAttached(dev.scripted.forgotten.registry.ModAttachments.BOSS)
                && world.getRandom().nextBoolean()) {
            hostile.discard();
            return;
        }
        if (entity instanceof AnimalEntity animal && animal.isBaby() && !animal.getCommandTags().contains("forgotten_twin")
                && memory.law(new Law(LawSubject.WORLD, Concept.RECURSION)) && world.getRandom().nextBoolean()) {
            Entity twin = animal.getType().create(world, SpawnReason.BREEDING);
            if (twin instanceof AnimalEntity t) {
                t.setBaby(true);
                t.addCommandTag("forgotten_twin");
                t.refreshPositionAndAngles(animal.getX(), animal.getY(), animal.getZ(), animal.getYaw(), 0f);
                Scheduler.after(1, () -> world.spawnEntity(t));
            }
        }
    }

    /** Damage under the laws: Possibility sometimes cancels it, Causality returns a third of it. */
    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world) || Reality.ending(world.getServer()) != Ending.AUTHOR) {
            return true;
        }
        if (law(entity, Concept.POSSIBILITY) && world.getRandom().nextInt(10) == 0) {
            return false;
        }
        if (law(entity, Concept.CAUSALITY) && source.getAttacker() instanceof LivingEntity attacker && !REFLECTING.get()) {
            REFLECTING.set(true);
            try {
                attacker.damage(world, world.getDamageSources().thorns(entity), amount / 3f);
            } finally {
                REFLECTING.set(false);
            }
        }
        return true;
    }

    /** The Recursion law: once in a while, the dead come back. */
    public static boolean allowDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world) || !law(entity, Concept.RECURSION)) {
            return true;
        }
        long now = Reality.now(world.getServer());
        Long last = RETURNED.get(entity.getUuid());
        if (last != null && now - last < 12000) {
            return true;
        }
        RETURNED.put(entity.getUuid(), now);
        entity.setHealth(entity.getMaxHealth() / 2);
        world.spawnParticles(net.minecraft.particle.ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getBodyY(0.5), entity.getZ(), 40, 0.4, 0.6, 0.4, 0.3);
        return false;
    }

    /** Breaking blocks: Possibility sometimes doubles the drops; tools remember their use. */
    public static void broken(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        WorldMemory memory = Reality.memory(world.getServer());
        ItemStack tool = player.getMainHandStack();
        if (memory.ending() == Ending.AUTHOR && memory.law(new Law(LawSubject.WORLD, Concept.POSSIBILITY))
                && world.getRandom().nextInt(10) == 0 && !player.isCreative()) {
            for (ItemStack drop : Block.getDroppedStacks(state, world, pos, blockEntity, player, tool)) {
                Block.dropStack(world, pos, drop);
            }
        }
        if (memory.rule(GenesisRule.ITEMS_REMEMBER) && tool.isDamageable()) {
            int mined = tool.getOrDefault(ModComponents.HISTORY_MINED, 0) + 1;
            tool.set(ModComponents.HISTORY_MINED, mined);
            tool.set(ModComponents.HISTORY_OWNER, player.getName().getString());
            if (mined % 256 == 0) {
                var efficiency = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY);
                int level = net.minecraft.enchantment.EnchantmentHelper.getLevel(efficiency, tool);
                if (level < 5) {
                    tool.addEnchantment(efficiency, level + 1);
                    Texts.bar(player, Texts.world("Your " + tool.getName().getString() + " remembers everything it has dug."));
                }
            }
        }
    }

    /** Death is a door: the dead walk as ghosts for a while, then return where they fell. */
    public static void respawned(ServerPlayerEntity oldPlayer, ServerPlayerEntity player, boolean alive) {
        if (alive || !Reality.memory(player.getEntityWorld().getServer()).rule(GenesisRule.DEATH_IS_A_DOOR)) {
            return;
        }
        ServerWorld deathWorld = Reality.world(oldPlayer);
        Vec3d fell = oldPlayer.getEntityPos();
        GameMode mode = player.interactionManager.getGameMode();
        if (mode == GameMode.SPECTATOR || fell.y < deathWorld.getBottomY()) {
            return;
        }
        player.changeGameMode(GameMode.SPECTATOR);
        Travel.teleport(player, deathWorld, fell.add(0, 1, 0), player.getYaw(), player.getPitch());
        Texts.tell(player, Texts.world("You walk for a while as a ghost. Then the door opens back the way you came."));
        Scheduler.after(600, () -> {
            if (!player.isDisconnected()) {
                Travel.teleport(player, deathWorld, fell, player.getYaw(), player.getPitch());
                player.changeGameMode(mode);
            }
        });
    }

    /** The Witness ending: the First World's secret has been seen. */
    public static void revealBeginning(ServerPlayerEntity player) {
        WorldMemory memory = memory(player);
        if (!memory.flag(BEGINNING_REVEALED)) {
            memory.setFlag(BEGINNING_REVEALED);
            Texts.tell(player, Texts.discovery("You know what the world was before it was simplified."));
        }
    }

    public static void clear() {
        RETURNED.clear();
    }
}
