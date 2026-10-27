package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.dimension.Dimensions;
import dev.scripted.forgotten.logic.boss.RecursionLineage;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.logic.waking.LucidityPhase;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The world's wrong creatures that are not bosses. All are vanilla bodies with forgotten
 * identities: Recursion Spiders that split when killed, faceless Null Crawlers that climb
 * walls near unmade ground, Syntax Wraiths that come with Reality Storms, Bloom Creepers that
 * flower instead of exploding, and Potential Creatures that have not decided what they are.
 */
public final class Creatures {
    public static final String NULL_CRAWLER = "forgotten_null_crawler";
    public static final String WRAITH = "forgotten_syntax_wraith";
    public static final String BLOOM = "forgotten_bloom";
    public static final String POTENTIAL = "forgotten_potential";
    private static final Identifier RECURSION_HEALTH = Forgotten.id("recursion_health");
    private static final Identifier RECURSION_SPEED = Forgotten.id("recursion_speed");
    private static final Identifier RECURSION_SCALE = Forgotten.id("recursion_scale");
    private static final List<EntityType<? extends MobEntity>> POTENTIALS = List.of(EntityType.COW, EntityType.PIG,
            EntityType.SHEEP, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CHICKEN, EntityType.FOX);
    /** Recursion kills per chunk, for the Queen. Not saved: a lineage has to be finished in one sitting. */
    private static final Map<Long, Integer> RECURSION_KILLS = new HashMap<>();
    private static final Map<UUID, Integer> STARES = new HashMap<>();

    private Creatures() {
    }

    // ---- spawning into the world ----

    /** Chances, rolled every two seconds per player. */
    public static void roll(ServerWorld world, ServerPlayerEntity player, long now) {
        Random random = world.getRandom();
        BlockPos pos = player.getBlockPos();
        IntegrityStage stage = Reality.stage(world, pos);
        if (world.isNight() && stage.atLeast(IntegrityStage.AMNESIA) && random.nextInt(90) == 0) {
            BlockPos at = Anomalies.surfaceNear(world, pos, random, 20);
            if (at != null && !at.isWithinDistance(pos, 8)) {
                nullCrawler(world, at);
            }
        }
        if (Reality.storming(world.getServer()) && random.nextInt(25) == 0) {
            wraith(world, player.getEntityPos().add(random.nextInt(13) - 6, 3, random.nextInt(13) - 6), player);
        }
        boolean unwritten = world.getRegistryKey() == Dimensions.UNWRITTEN || stage == IntegrityStage.UNWRITTEN;
        if (unwritten && random.nextInt(60) == 0) {
            BlockPos at = Anomalies.surfaceNear(world, pos, random, 16);
            if (at != null) {
                potential(world, at, random);
            }
        }
    }

    /** New spiders in forgotten places may be recursive; new creepers in a lucid world may bloom. */
    public static void adopt(Entity entity) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        if (entity instanceof SpiderEntity spider && !spider.hasAttached(ModAttachments.RECURSION)) {
            boolean recursive = Reality.stage(world, spider.getBlockPos()).atLeast(IntegrityStage.DEJA_VU)
                    && world.getRandom().nextInt(6) == 0;
            spider.setAttached(ModAttachments.RECURSION, recursive ? 0 : -1);
            if (recursive) {
                shape(spider, 0);
            }
        }
        if (entity instanceof CreeperEntity creeper && !creeper.getCommandTags().contains(BLOOM) && entity.age == 0
                && Reality.phase(world.getServer()).ordinal() >= LucidityPhase.values().length / 2
                && world.getRandom().nextInt(4) == 0) {
            creeper.addCommandTag(BLOOM);
        }
        if (entity instanceof MobEntity mob && entity.getCommandTags().contains(NULL_CRAWLER)) {
            TRACKED.put(mob.getUuid(), mob);
        }
        if (entity instanceof MobEntity mob && entity.getCommandTags().contains(POTENTIAL)) {
            TRACKED.put(mob.getUuid(), mob);
        }
        if (entity instanceof CreeperEntity creeper && creeper.getCommandTags().contains(BLOOM)) {
            TRACKED.put(creeper.getUuid(), creeper);
        }
    }

    private static final Map<UUID, MobEntity> TRACKED = new HashMap<>();

    public static void untrack(Entity entity) {
        TRACKED.remove(entity.getUuid());
        STARES.remove(entity.getUuid());
    }

    /** Makes a creeper a Bloom Creeper. */
    public static void bloomify(CreeperEntity creeper) {
        creeper.addCommandTag(BLOOM);
        TRACKED.put(creeper.getUuid(), creeper);
    }

    // ---- Recursion Spiders ----

    public static void shape(SpiderEntity spider, int generation) {
        modifier(spider, EntityAttributes.MAX_HEALTH, RECURSION_HEALTH, RecursionLineage.health(generation) - 16.0);
        modifier(spider, EntityAttributes.MOVEMENT_SPEED, RECURSION_SPEED, RecursionLineage.speedMultiplier(generation) - 1.0,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(spider, EntityAttributes.SCALE, RECURSION_SCALE, RecursionLineage.scale(generation) - 1.0,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        spider.setHealth(spider.getMaxHealth());
        spider.setCustomName(Text.literal("Recursion Spider" + (generation > 0 ? " [" + generation + "]" : ""))
                .formatted(Formatting.DARK_PURPLE));
    }

    static void modifier(LivingEntity entity, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
                         Identifier id, double value) {
        modifier(entity, attribute, id, value, EntityAttributeModifier.Operation.ADD_VALUE);
    }

    static void modifier(LivingEntity entity, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
                         Identifier id, double value, EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance != null) {
            instance.removeModifier(id);
            instance.addPersistentModifier(new EntityAttributeModifier(id, value, operation));
        }
    }

    /** A Recursion Spider dies: it becomes two of itself, weaker and faster. Enough of them become the Queen. */
    public static void died(LivingEntity entity, DamageSource source) {
        if (entity instanceof CreeperEntity || !(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        if (entity instanceof MobEntity mob && mob.getCommandTags().contains(POTENTIAL)) {
            Block.dropStack(world, mob.getBlockPos(), new ItemStack(ModItems.concept(Concept.POSSIBILITY)));
            return;
        }
        if (!(entity instanceof SpiderEntity spider)) {
            return;
        }
        Integer generation = spider.getAttached(ModAttachments.RECURSION);
        if (generation == null || generation < 0) {
            return;
        }
        long chunk = ChunkPos.toLong(spider.getBlockPos());
        int kills = RECURSION_KILLS.merge(chunk, 1, Integer::sum);
        if (RecursionLineage.queenEmerges(kills)) {
            RECURSION_KILLS.remove(chunk);
            Bosses.summon(Bosses.Kind.RECURSION_QUEEN, world, spider.getBlockPos());
            return;
        }
        for (int i = 0; i < RecursionLineage.childrenOnDeath(generation); i++) {
            SpiderEntity child = EntityType.SPIDER.create(world, SpawnReason.EVENT);
            if (child == null) {
                continue;
            }
            child.refreshPositionAndAngles(spider.getX() + (i - 0.5) * 0.6, spider.getY(), spider.getZ(), spider.getYaw(), 0f);
            child.setAttached(ModAttachments.RECURSION, generation + 1);
            shape(child, generation + 1);
            world.spawnEntity(child);
        }
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, spider.getX(), spider.getBodyY(0.5), spider.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
        if (generation == 0) {
            Block.dropStack(world, spider.getBlockPos(), new ItemStack(ModItems.concept(Concept.RECURSION)));
        }
    }

    // ---- Null Crawlers ----

    public static ZombieEntity nullCrawler(ServerWorld world, BlockPos at) {
        ZombieEntity crawler = EntityType.ZOMBIE.create(world, SpawnReason.EVENT);
        if (crawler == null) {
            return null;
        }
        crawler.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        crawler.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.BLACK_CONCRETE));
        crawler.equipStack(EquipmentSlot.CHEST, black(Items.LEATHER_CHESTPLATE));
        crawler.equipStack(EquipmentSlot.LEGS, black(Items.LEATHER_LEGGINGS));
        crawler.equipStack(EquipmentSlot.FEET, black(Items.LEATHER_BOOTS));
        crawler.setSilent(true);
        crawler.addCommandTag(NULL_CRAWLER);
        crawler.setCustomName(Text.literal("Null Crawler").formatted(Formatting.BLACK));
        world.spawnEntity(crawler);
        TRACKED.put(crawler.getUuid(), crawler);
        return crawler;
    }

    static ItemStack black(net.minecraft.item.Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(0x050505));
        return stack;
    }

    // ---- Syntax Wraiths ----

    public static VexEntity wraith(ServerWorld world, Vec3d at, ServerPlayerEntity target) {
        VexEntity wraith = EntityType.VEX.create(world, SpawnReason.EVENT);
        if (wraith == null) {
            return null;
        }
        wraith.refreshPositionAndAngles(at.x, at.y, at.z, 0f, 0f);
        String[] errors = {"SyntaxError", "unexpected token", "missing ';'", "unreachable code", "TypeError", "segfault"};
        wraith.setCustomName(Text.literal(errors[world.getRandom().nextInt(errors.length)]).formatted(Formatting.RED, Formatting.ITALIC));
        wraith.setCustomNameVisible(true);
        wraith.setLifeTicks(20 * 60);
        wraith.addCommandTag(WRAITH);
        wraith.setTarget(target);
        world.spawnEntity(wraith);
        return wraith;
    }

    // ---- Potential Creatures ----

    public static MobEntity potential(ServerWorld world, BlockPos at, Random random) {
        MobEntity mob = POTENTIALS.get(random.nextInt(POTENTIALS.size())).create(world, SpawnReason.EVENT);
        if (mob == null) {
            return null;
        }
        mob.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        mob.addCommandTag(POTENTIAL);
        mob.setCustomName(Text.literal("Potential Creature").formatted(Formatting.LIGHT_PURPLE));
        mob.setGlowing(true);
        world.spawnEntity(mob);
        TRACKED.put(mob.getUuid(), mob);
        return mob;
    }

    /** A Potential Creature has not decided what it is. Every few seconds it decides again. */
    static void redecide(ServerWorld world, MobEntity mob) {
        Random random = world.getRandom();
        EntityType<? extends MobEntity> next = POTENTIALS.get(random.nextInt(POTENTIALS.size()));
        if (next == mob.getType()) {
            return;
        }
        MobEntity into = next.create(world, SpawnReason.EVENT);
        if (into == null) {
            return;
        }
        float fraction = mob.getHealth() / mob.getMaxHealth();
        into.refreshPositionAndAngles(mob.getX(), mob.getY(), mob.getZ(), mob.getYaw(), mob.getPitch());
        into.addCommandTag(POTENTIAL);
        into.setCustomName(mob.getCustomName());
        into.setGlowing(true);
        world.spawnEntity(into);
        into.setHealth(Math.max(1f, into.getMaxHealth() * fraction));
        world.spawnParticles(ParticleTypes.WITCH, mob.getX(), mob.getBodyY(0.5), mob.getZ(), 12, 0.3, 0.4, 0.3, 0.05);
        TRACKED.remove(mob.getUuid());
        mob.discard();
        TRACKED.put(into.getUuid(), into);
    }

    // ---- per tick ----

    public static void tick(long now) {
        if (TRACKED.isEmpty() || now % 5 != 0) {
            return;
        }
        for (MobEntity mob : new ArrayList<>(TRACKED.values())) {
            if (mob.isRemoved() || !mob.isAlive() || !(mob.getEntityWorld() instanceof ServerWorld world)) {
                TRACKED.remove(mob.getUuid());
                continue;
            }
            var tags = mob.getCommandTags();
            if (tags.contains(NULL_CRAWLER)) {
                crawl(world, mob);
            } else if (tags.contains(POTENTIAL) && (now + mob.getId()) % 60 == 0) {
                redecide(world, mob);
            } else if (mob instanceof CreeperEntity creeper && tags.contains(BLOOM)) {
                bloom(world, creeper);
            }
        }
    }

    /** Null Crawlers climb walls, and looking at one for too long takes the light away. */
    static void crawl(ServerWorld world, MobEntity crawler) {
        if (crawler.horizontalCollision) {
            Vec3d v = crawler.getVelocity();
            crawler.setVelocity(v.x, 0.3, v.z);
            crawler.velocityDirty = true;
        }
        for (ServerPlayerEntity player : world.getPlayers(p -> p.squaredDistanceTo(crawler) < 32 * 32)) {
            Vec3d look = player.getRotationVec(1f);
            Vec3d to = crawler.getEyePos().subtract(player.getEyePos()).normalize();
            boolean staring = look.dotProduct(to) > 0.97 && player.canSee(crawler);
            int ticks = staring ? STARES.merge(crawler.getUuid(), 5, Integer::sum) : 0;
            if (!staring) {
                STARES.remove(crawler.getUuid());
            }
            if (ticks >= 60) {
                STARES.remove(crawler.getUuid());
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 100, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0));
                Texts.bar(player, Texts.glitch("the world is only edges"));
            }
        }
    }

    /** A Bloom Creeper about to explode flowers instead. */
    static void bloom(ServerWorld world, CreeperEntity creeper) {
        // The fuse only climbs while the creeper is about to go off. (Its fuse speed is no use
        // here: the ignite goal resets it to -1 after the creeper's own tick has used it.)
        if (creeper.currentFuseTime < creeper.fuseTime - 6) {
            return;
        }
        BlockPos center = creeper.getBlockPos();
        Random random = world.getRandom();
        BlockState[] flowers = {Blocks.POPPY.getDefaultState(), Blocks.DANDELION.getDefaultState(), Blocks.CORNFLOWER.getDefaultState(),
                Blocks.AZURE_BLUET.getDefaultState(), Blocks.OXEYE_DAISY.getDefaultState(), Blocks.ALLIUM.getDefaultState()};
        for (BlockPos p : BlockPos.iterateOutwards(center, 3, 1, 3)) {
            BlockState flower = flowers[random.nextInt(flowers.length)];
            if (world.getBlockState(p).isAir() && flower.canPlaceAt(world, p) && random.nextInt(3) > 0) {
                world.setBlockState(p.toImmutable(), flower, Block.NOTIFY_ALL);
            }
        }
        world.spawnParticles(ParticleTypes.CHERRY_LEAVES, creeper.getX(), creeper.getBodyY(0.5), creeper.getZ(), 60, 1.5, 1, 1.5, 0.05);
        world.playSound(null, center, SoundEvents.BLOCK_AZALEA_LEAVES_PLACE, SoundCategory.HOSTILE, 1.5f, 0.8f);
        TRACKED.remove(creeper.getUuid());
        creeper.discard();
    }

    public static void clear() {
        TRACKED.clear();
        RECURSION_KILLS.clear();
        STARES.clear();
    }
}
