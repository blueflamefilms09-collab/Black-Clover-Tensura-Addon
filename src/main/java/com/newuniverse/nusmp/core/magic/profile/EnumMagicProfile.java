package com.newuniverse.nusmp.core.magic.profile;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.MagicType.HitEffect;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/**
 * The profile DERIVED from a {@link MagicType} constant: its own name, word, soul, hit effect, particle and guard effect; the effects
 * from {@link MagicShapes}; the colour of its book; the aura slot of the same name. Whatever the constant leaves null falls back to the
 * interface default, so an enum constant with a missing supplier still resolves a complete profile.
 */
public final class EnumMagicProfile implements MagicProfile {
    private final MagicType type;
    private final String id;
    private final MagicShapes.ShapeSet shapes;
    private final Aura aura;
    private volatile int color;          // 0 = not yet known (a real colour always has alpha 0xFF)

    public EnumMagicProfile(MagicType type) {
        this.type = type;
        this.id = type.name();
        this.shapes = MagicShapes.of(id, isWikiAttribute(type));
        this.aura = auraOf(id);
    }

    private static boolean isWikiAttribute(MagicType t) {
        try { return com.newuniverse.nusmp.book.WikiBooks.isNewAttribute(t); } catch (Throwable e) { return false; }
    }

    private static Aura auraOf(String id) {
        try { return Aura.valueOf(MagicKeys.auraName(id)); } catch (IllegalArgumentException | NullPointerException e) { return null; }
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return type.displayName == null || type.displayName.isBlank() ? MagicProfile.super.displayName() : type.displayName; }
    @Override public String word() { return type.word == null || type.word.isBlank() ? MagicProfile.super.word() : type.word; }
    @Override public String soulType() { return type.soulType == null || type.soulType.isBlank() ? "EMPTY" : type.soulType; }
    @Override public HitEffect hit() { return type.hit == null ? HitEffect.PIERCE : type.hit; }

    @Override public SimpleParticleType particle() {
        SimpleParticleType p = type.rawParticle();
        return p != null ? p : com.newuniverse.nusmp.particle.rukh.RukhParticles.light();
    }

    @Override public Holder<MobEffect> guardEffect() {
        Holder<MobEffect> h = type.rawGuardEffect();
        return h != null ? h : MobEffects.DAMAGE_RESISTANCE;
    }

    @Override public VfxStyle vfxStyle() { return shapes.style(); }
    @Override public VfxShape castShape() { return shapes.cast(); }
    @Override public VfxShape fieldShape() { return shapes.field(); }
    @Override public VfxShape burstShape() { return shapes.burst(); }
    @Override public Aura auraStyle() { return aura; }

    /** The colour of the magic's Unique book if it is registered (the colour its own spells use), else the grimoire palette. */
    @Override public int color() {
        int c = color;
        if (c != 0) return c;
        try {
            var book = com.newuniverse.nusmp.skill.NUSkills.bookFor(type);
            if (book != null) { c = book.color | 0xFF000000; color = c; return c; }
        } catch (Throwable ignored) { /* registries not ready yet: use the palette this time, ask again later */ }
        return MagicProfile.super.color();
    }

    /** The enum constant this profile is derived from. */
    public MagicType type() { return type; }
}
