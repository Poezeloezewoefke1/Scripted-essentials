package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.paradox.CausalDebt;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.player.RealityEdit;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.MannequinEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Copies of players, made out of vanilla Mannequins so they wear the player's own skin without
 * any custom rendering. The mod moves them itself.
 *
 * <ul>
 *   <li>SECOND_YOU - a Correction. Reality sends it when a player's causal debt is high. It
 *       walks into their base, opens a chest, then turns and attacks. Defeat it and the debt is
 *       partly repaid; lose and it takes one of your reality edits away.</li>
 *   <li>ECHO - left behind when a deeply indebted player dies. It does not want to kill you.
 *       It wants to be you: if it reaches you it takes your place and something you carry.</li>
 *   <li>HOLLOW - a black silhouette that copies your movements three seconds late. It stops
 *       when you look at it and closes in when you do not. Touch it and your hands forget how
 *       to hold things until you sleep.</li>
 *   <li>PHANTOM - a memory of you walking somewhere you once walked. Harmless.</li>
 * </ul>
 */
public final class Puppets {
    public enum Kind {
        SECOND_YOU, ECHO, HOLLOW, PHANTOM
    }

    private static final class State {
        final MannequinEntity body;
        final Kind kind;
        final UUID owner;
        int age;
        int cooldown;
        BlockPos chest;
        ItemStack stolen = ItemStack.EMPTY;
        List<Vec3d> path = new ArrayList<>();

        State(MannequinEntity body, Kind kind, UUID owner) {
            this.body = body;
            this.kind = kind;
            this.owner = owner;
        }
    }

    private static final Map<UUID, State> PUPPETS = new HashMap<>();

    private Puppets() {
    }

    public static int count(Kind kind) {
        return (int) PUPPETS.values().stream().filter(s -> s.kind == kind).count();
    }

    public static MannequinEntity spawn(ServerWorld world, ServerPlayerEntity owner, Kind kind, Vec3d at) {
        MannequinEntity body = EntityType.MANNEQUIN.create(world, SpawnReason.EVENT);
        if (body == null) {
            return null;
        }
        body.refreshPositionAndAngles(at.x, at.y, at.z, owner.getYaw(), 0);
        if (kind == Kind.HOLLOW) {
            body.setInvisible(true);
            body.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.BLACK_CONCRETE));
            for (var slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack piece = new ItemStack(slot == EquipmentSlot.CHEST ? Items.LEATHER_CHESTPLATE
                        : slot == EquipmentSlot.LEGS ? Items.LEATHER_LEGGINGS : Items.LEATHER_BOOTS);
                piece.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(0x050505));
                body.equipStack(slot, piece);
            }
        } else {
            body.setMannequinProfile(ProfileComponent.ofStatic(owner.getGameProfile()));
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                    EquipmentSlot.FEET, EquipmentSlot.MAINHAND}) {
                body.equipStack(slot, owner.getEquippedStack(slot).copy());
            }
        }
        if (kind == Kind.SECOND_YOU) {
            EntityAttributeInstance health = body.getAttributeInstance(EntityAttributes.MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(40);
            }
            body.setHealth(40);
            body.setCustomName(Text.literal(owner.getName().getString()).formatted(Formatting.DARK_RED));
        }
        if (kind == Kind.PHANTOM) {
            body.setNoGravity(true);
            body.setInvulnerable(true);
            body.setGlowing(true);
        }
        body.setAttached(ModAttachments.PUPPET, kind.name() + ";" + owner.getUuidAsString());
        world.spawnEntity(body);
        PUPPETS.put(body.getUuid(), new State(body, kind, owner.getUuid()));
        return body;
    }

    /** A phantom that walks a remembered path. */
    public static void phantom(ServerWorld world, ServerPlayerEntity owner, List<Vec3d> path) {
        if (path.size() < 2) {
            return;
        }
        MannequinEntity body = spawn(world, owner, Kind.PHANTOM, path.get(0));
        if (body != null) {
            PUPPETS.get(body.getUuid()).path = new ArrayList<>(path);
        }
    }

    /** Re-adopts puppets after a restart: they lose their plans but keep their purpose. */
    public static void adopt(Entity entity) {
        if (entity instanceof MannequinEntity m && !PUPPETS.containsKey(m.getUuid())) {
            String tag = m.getAttached(ModAttachments.PUPPET);
            if (tag != null) {
                String[] p = tag.split(";");
                Kind kind = Kind.valueOf(p[0]);
                if (kind == Kind.PHANTOM) {
                    m.discard();
                    return;
                }
                PUPPETS.put(m.getUuid(), new State(m, kind, UUID.fromString(p[1])));
            }
        }
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (PUPPETS.isEmpty()) {
            return;
        }
        for (State s : new ArrayList<>(PUPPETS.values())) {
            if (s.body.isRemoved() || !s.body.isAlive()) {
                PUPPETS.remove(s.body.getUuid());
                continue;
            }
            s.age++;
            if (s.cooldown > 0) {
                s.cooldown--;
            }
            ServerWorld world = (ServerWorld) s.body.getEntityWorld();
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(s.owner);
            switch (s.kind) {
                case SECOND_YOU -> secondYou(world, s, owner);
                case ECHO -> echo(world, s, owner);
                case HOLLOW -> hollow(world, s, owner);
                case PHANTOM -> phantomStep(s);
            }
        }
    }

    private static void secondYou(ServerWorld world, State s, ServerPlayerEntity owner) {
        if (owner == null || owner.getEntityWorld() != world || owner.isSpectator()) {
            if (s.age > 6000) {
                s.body.discard();
            }
            return;
        }
        if (s.age < 160) {
            // It performs what you did: walks to your storage and opens it.
            if (s.chest == null) {
                s.chest = Anomalies.find(world, s.body.getBlockPos(), 12, 300, world.getRandom(), st -> st.getBlock() instanceof ChestBlock);
                if (s.chest == null) {
                    s.chest = owner.getBlockPos();
                }
            }
            walk(s.body, Vec3d.ofBottomCenter(s.chest), 0.22);
            if (s.age == 120 && s.chest != null) {
                world.addSyncedBlockEvent(s.chest, world.getBlockState(s.chest).getBlock(), 1, 1);
                world.playSound(null, s.chest.getX(), s.chest.getY(), s.chest.getZ(), SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.8f, 0.8f);
            }
            if (s.age == 150) {
                lookAt(s.body, owner.getEyePos());
                Texts.bar(owner, Texts.world("It looks at you."));
            }
            return;
        }
        walk(s.body, owner.getEntityPos(), 0.3);
        if (s.body.squaredDistanceTo(owner) < 2.4 * 2.4 && s.cooldown == 0) {
            s.body.swingHand(Hand.MAIN_HAND);
            owner.damage(world, world.getDamageSources().mobAttack(s.body), 6.0f);
            s.cooldown = 18;
        }
    }

    private static void echo(ServerWorld world, State s, ServerPlayerEntity owner) {
        if (owner == null || owner.getEntityWorld() != world) {
            return;
        }
        walk(s.body, owner.getEntityPos(), 0.18);
        if (s.body.squaredDistanceTo(owner) < 1.5 * 1.5 && s.stolen.isEmpty() && s.cooldown == 0) {
            Vec3d mine = s.body.getEntityPos();
            Vec3d theirs = owner.getEntityPos();
            owner.requestTeleport(mine.x, mine.y, mine.z);
            s.body.refreshPositionAndAngles(theirs.x, theirs.y, theirs.z, owner.getYaw(), owner.getPitch());
            var inv = owner.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack st = inv.getStack(i);
                if (!st.isEmpty() && world.getRandom().nextInt(4) == 0) {
                    s.stolen = st.copy();
                    inv.setStack(i, ItemStack.EMPTY);
                    s.body.equipStack(EquipmentSlot.OFFHAND, s.stolen.copy());
                    break;
                }
            }
            Texts.bar(owner, Texts.world("For a moment you were standing where it stood. It is wearing your face better than you are."));
            s.cooldown = 200;
        }
    }

    private static void hollow(ServerWorld world, State s, ServerPlayerEntity owner) {
        if (owner == null || owner.getEntityWorld() != world || s.age > 2400) {
            s.body.discard();
            return;
        }
        Vec3d eye = owner.getEyePos();
        Vec3d to = s.body.getEntityPos().add(0, 1, 0).subtract(eye);
        boolean watched = to.normalize().dotProduct(owner.getRotationVector()) > 0.9;
        if (watched) {
            s.body.setVelocity(0, s.body.getVelocity().y, 0);
        } else {
            walk(s.body, owner.getEntityPos(), 0.12);
        }
        if (s.body.squaredDistanceTo(owner) < 1.3) {
            PlayerMemory memory = Reality.player(owner);
            memory.hollowCursed = true;
            Texts.tell(owner, Texts.world("It touches you. Your hands forget how to hold things."));
            s.body.discard();
        }
    }

    private static void phantomStep(State s) {
        int i = s.age / 10;
        if (i + 1 >= s.path.size()) {
            s.body.discard();
            return;
        }
        Vec3d a = s.path.get(i);
        Vec3d b = s.path.get(i + 1);
        double t = (s.age % 10) / 10.0;
        Vec3d p = a.add(b.subtract(a).multiply(t));
        lookAt(s.body, b.add(0, 1.6, 0));
        s.body.refreshPositionAndAngles(p.x, p.y, p.z, s.body.getYaw(), 0);
    }

    /** Hands that have forgotten how to hold things: nothing stays in the hotbar. */
    public static void applyHollowCurse(ServerPlayerEntity player) {
        if (!Reality.player(player).hollowCursed || player.isCreative()) {
            return;
        }
        var inv = player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inv.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            inv.setStack(slot, ItemStack.EMPTY);
            boolean placed = false;
            for (int i = 9; i < inv.size() && !placed; i++) {
                if (inv.getStack(i).isEmpty()) {
                    inv.setStack(i, stack);
                    placed = true;
                }
            }
            if (!placed) {
                player.dropItem(stack, false);
            }
        }
    }

    static void walk(LivingEntity body, Vec3d target, double speed) {
        Vec3d d = target.subtract(body.getEntityPos());
        Vec3d flat = new Vec3d(d.x, 0, d.z);
        if (flat.lengthSquared() < 0.05) {
            return;
        }
        Vec3d v = flat.normalize().multiply(speed);
        double vy = body.getVelocity().y;
        if (body.horizontalCollision && body.isOnGround()) {
            vy = 0.42;
        }
        body.setVelocity(v.x, vy, v.z);
        body.velocityDirty = true;
        lookAt(body, target.add(0, 1.6, 0));
    }

    static void lookAt(LivingEntity body, Vec3d target) {
        Vec3d d = target.subtract(body.getEyePos());
        float yaw = (float) (Math.atan2(d.z, d.x) * 180.0 / Math.PI) - 90f;
        float pitch = (float) (-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180.0 / Math.PI);
        body.setYaw(yaw);
        body.setHeadYaw(yaw);
        body.setBodyYaw(yaw);
        body.setPitch(pitch);
    }

    /** Called when any living thing dies; handles puppets dying and players dying to puppets. */
    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        State s = PUPPETS.remove(entity.getUuid());
        if (s != null) {
            if (!s.stolen.isEmpty()) {
                Block.dropStack(world, entity.getBlockPos(), s.stolen);
            }
            if (s.kind == Kind.SECOND_YOU) {
                ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(s.owner);
                Block.dropStack(world, entity.getBlockPos(), new ItemStack(ModItems.concept(Concept.CAUSALITY)));
                Block.dropStack(world, entity.getBlockPos(), new ItemStack(ModItems.COHERENCE_SHARD, 3));
                if (owner != null) {
                    PlayerMemory m = Reality.player(owner);
                    m.debt = CausalDebt.afterDeath(m.debt);
                    Texts.tell(owner, Texts.world("You recover the memories it carried. Reality accepts the correction, for now."));
                }
            }
            return;
        }
        if (entity instanceof ServerPlayerEntity player && source.getAttacker() instanceof MannequinEntity m) {
            String tag = m.getAttached(ModAttachments.PUPPET);
            if (tag != null && tag.startsWith(Kind.SECOND_YOU.name())) {
                stealEdit(world, player);
            }
        }
    }

    /** The Correction won: one of the player's edits is taken back. */
    static void stealEdit(ServerWorld world, ServerPlayerEntity player) {
        PlayerMemory memory = Reality.player(player);
        List<RealityEdit> active = memory.activeEdits();
        if (active.isEmpty()) {
            return;
        }
        RealityEdit edit = active.get(world.getRandom().nextInt(active.size()));
        ServerWorld target = RealityEdits.worldOf(world, edit.dimension());
        if (target != null) {
            RealityEdits.revoke(target, edit);
        }
        if (edit.kind() == RealityEdit.Kind.DEATHLESS) {
            memory.deathlessUntil = 0;
        }
        memory.edits.set(memory.edits.indexOf(edit), edit.deactivated());
        Texts.tell(player, Texts.discovery("THE CORRECTION TAKES SOMETHING BACK: " + edit.kind().name().replace('_', ' ')));
        Reality.chronicle(world, ChronicleEvent.REALITY_EDIT, player.getBlockPos(), player.getName().getString(), "correction reclaimed " + edit.kind());
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 80, 1, 1, 1, 0.3);
    }

    public static void clear() {
        PUPPETS.clear();
    }
}
