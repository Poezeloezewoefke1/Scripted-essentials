package dev.scripted.forgotten.item;

import dev.scripted.forgotten.system.GlitchKind;
import dev.scripted.forgotten.system.Glitches;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A Null Material. It deals "undefined" damage: each hit is somewhere between nothing and far
 * too much, and now and then the thing it hits forgets what it is.
 */
public class UndefinedSwordItem extends LoreItem {
    public UndefinedSwordItem(Settings settings) {
        super(settings, true);
    }

    @Override
    public float getBonusAttackDamage(Entity target, float baseAttackDamage, DamageSource damageSource) {
        return (float) ThreadLocalRandom.current().nextDouble(-baseAttackDamage, 20.0);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHit(stack, target, attacker);
        if (target instanceof MobEntity mob && target.isAlive() && ThreadLocalRandom.current().nextInt(12) == 0) {
            GlitchKind[] kinds = GlitchKind.values();
            Glitches.apply(mob, kinds[ThreadLocalRandom.current().nextInt(kinds.length)]);
        }
    }
}
