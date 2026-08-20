package org.bukkit.potion;

public class PotionEffect {
    public PotionEffect(PotionEffectType type, int duration, int amplifier) { }
    public PotionEffect(PotionEffectType type, int duration, int amplifier, boolean ambient) { }
    public PotionEffect(PotionEffectType type, int duration, int amplifier, boolean ambient, boolean particles) { }
    public PotionEffect(PotionEffectType type, int duration, int amplifier, boolean ambient, boolean particles, boolean icon) { }
    public PotionEffectType getType() { throw new UnsupportedOperationException(); }
    public int getDuration() { throw new UnsupportedOperationException(); }
    public int getAmplifier() { throw new UnsupportedOperationException(); }
    public boolean isAmbient() { throw new UnsupportedOperationException(); }
    public boolean hasParticles() { throw new UnsupportedOperationException(); }
    public boolean hasIcon() { throw new UnsupportedOperationException(); }
}
