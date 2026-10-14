package com.newuniverse.nusmp.core.magic.merge;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.blackclover.MagicType.HitEffect;
import com.newuniverse.nusmp.core.magic.profile.MagicProfile;
import com.newuniverse.nusmp.core.magic.profile.MagicProfiles;
import com.newuniverse.nusmp.core.magic.profile.MagicKeys;
import com.newuniverse.nusmp.core.magic.profile.VfxStyle;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.effect.MobEffect;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The profile of a merged magic, built through the same {@link MagicProfile} framework as every other magic, from its parents' profiles:
 * soul type = the dominant parent's; hit effect = the dominant parent's, with another parent's riding along as the secondary; particle =
 * the parents' particles in turn; guard effect = the dominant parent's; colour = the blended palette; effects = the composite
 * layer (MERGED_*); aura = the merged armour layer; safe numbers = the dearest parent's x 1.15.
 */
public final class MergedProfile implements MagicProfile {
    private final MergedDef def;
    private final AtomicInteger turn = new AtomicInteger();

    public MergedProfile(MergedDef def) { this.def = def; }

    public MergedDef def() { return def; }

    private MagicProfile parent(int i) { return MagicProfiles.of(def.parents().get(Math.floorMod(i, def.size()))); }

    private MergedBlend blend() { return MergedRegistry.blend(def); }

    private static HitEffect hitOf(String name) {
        if (name == null) return null;
        try { return HitEffect.valueOf(name); } catch (IllegalArgumentException e) { return null; }
    }

    @Override public String id() { return def.id(); }
    @Override public String displayName() { return def.name(); }
    @Override public String word() { return def.name().split("\\s+")[0]; }
    @Override public String soulType() { return blend().soulType; }
    @Override public HitEffect hit() { HitEffect h = hitOf(blend().hitPrimary); return h != null ? h : HitEffect.PIERCE; }
    @Override public HitEffect secondaryHit() { return hitOf(blend().hitSecondary); }
    @Override public boolean restricted() { return false; }
    @Override public boolean isMerged() { return true; }
    @Override public List<String> parentIds() { return def.parents(); }
    @Override public int color() { return blend().primary; }

    /** The parents' particles in turn, so a merged cast sparkles with all of them. */
    @Override public SimpleParticleType particle() { return parent(turn.getAndIncrement() & 0x7FFFFFFF).particle(); }

    @Override public Holder<MobEffect> guardEffect() { return parent(0).guardEffect(); }
    @Override public VfxStyle vfxStyle() { return VfxStyle.NATIVE; }
    @Override public VfxShape castShape() { return VfxShape.MERGED_CAST; }
    @Override public VfxShape fieldShape() { return VfxShape.MERGED_FIELD; }
    @Override public VfxShape burstShape() { return VfxShape.MERGED_BURST; }
    @Override public Aura auraStyle() { return Aura.MERGED; }
    @Override public String iconPath() { return MagicKeys.FALLBACK_ICON; }

    @Override public double safeCostPercent() { double m = 0; for (int i = 0; i < def.size(); i++) m = Math.max(m, parent(i).safeCostPercent()); return Math.min(MergeLimits.COST_CAP_PERCENT, m * MergeLimits.COST_FACTOR); }
    @Override public double safeCostFloor() { double m = 0; for (int i = 0; i < def.size(); i++) m = Math.max(m, parent(i).safeCostFloor()); return m * MergeLimits.COST_FACTOR; }
    @Override public int safeCooldownTicks() { int m = 0; for (int i = 0; i < def.size(); i++) m = Math.max(m, parent(i).safeCooldownTicks()); return (int) Math.round(m * MergeLimits.COST_FACTOR); }
}
