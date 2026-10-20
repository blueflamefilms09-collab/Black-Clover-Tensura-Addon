package com.newuniverse.nusmp.core.magic.profile;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.blackclover.MagicType.HitEffect;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffect;

import java.util.List;
import java.util.function.Consumer;

/**
 * An explicit profile: wraps a base profile (normally the derived one) and replaces only the parts that were set. Register with
 * {@link MagicProfiles#override}:
 * <pre>
 *   MagicProfiles.override(MagicType.AIR, o -&gt; o.color(0xFF9FE8FF).shapes(VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_FIELD, VfxShape.WIKI_MAGIC_BURST));
 * </pre>
 */
public final class ProfileOverride implements MagicProfile {
    private final MagicProfile base;
    private String displayName, word, soul;
    private HitEffect hit, secondaryHit;
    private Boolean restricted;
    private Integer color, cooldown;
    private ParticleOptions particle;
    private Holder<MobEffect> guard;
    private VfxStyle style;
    private VfxShape cast, field, burst;
    private Aura aura;
    private String iconPath;
    private Double costPercent, costFloor;
    private List<String> parents;

    public ProfileOverride(MagicProfile base) { this.base = base == null ? new FallbackMagicProfile("UNKNOWN") : base; }

    /** Builds an override of {@code base} with the setter lambda applied. */
    public static ProfileOverride of(MagicProfile base, Consumer<ProfileOverride> edit) {
        ProfileOverride o = new ProfileOverride(base);
        if (edit != null) edit.accept(o);
        return o;
    }

    public ProfileOverride displayName(String v) { displayName = v; return this; }
    public ProfileOverride word(String v) { word = v; return this; }
    public ProfileOverride soulType(String v) { soul = v; return this; }
    public ProfileOverride hit(HitEffect v) { hit = v; return this; }
    public ProfileOverride secondaryHit(HitEffect v) { secondaryHit = v; return this; }
    public ProfileOverride restricted(boolean v) { restricted = v; return this; }
    public ProfileOverride color(int argb) { color = argb | 0xFF000000; return this; }
    public ProfileOverride particle(ParticleOptions v) { particle = v; return this; }
    public ProfileOverride guardEffect(Holder<MobEffect> v) { guard = v; return this; }
    public ProfileOverride style(VfxStyle v) { style = v; return this; }
    public ProfileOverride shapes(VfxShape cast, VfxShape field, VfxShape burst) { this.cast = cast; this.field = field; this.burst = burst; return this; }
    public ProfileOverride aura(Aura v) { aura = v; return this; }
    public ProfileOverride iconPath(String v) { iconPath = v; return this; }
    public ProfileOverride safeCost(double percent, double floor, int cooldownTicks) { costPercent = percent; costFloor = floor; cooldown = cooldownTicks; return this; }
    public ProfileOverride parents(List<String> v) { parents = v == null ? null : List.copyOf(v); return this; }

    @Override public String id() { return base.id(); }
    @Override public String displayName() { return displayName != null && !displayName.isBlank() ? displayName : base.displayName(); }
    @Override public String word() { return word != null && !word.isBlank() ? word : base.word(); }
    @Override public String soulType() { return soul != null && !soul.isBlank() ? soul : base.soulType(); }
    @Override public HitEffect hit() { return hit != null ? hit : base.hit(); }
    @Override public HitEffect secondaryHit() { return secondaryHit != null ? secondaryHit : base.secondaryHit(); }
    @Override public boolean restricted() { return restricted != null ? restricted : base.restricted(); }
    @Override public boolean isMerged() { return base.isMerged(); }
    @Override public List<String> parentIds() { return parents != null ? parents : base.parentIds(); }
    @Override public int color() { return color != null ? color : base.color(); }
    @Override public ParticleOptions particle() { return particle != null ? particle : base.particle(); }
    @Override public Holder<MobEffect> guardEffect() { return guard != null ? guard : base.guardEffect(); }
    @Override public VfxStyle vfxStyle() { return style != null ? style : base.vfxStyle(); }
    @Override public VfxShape castShape() { return cast != null ? cast : base.castShape(); }
    @Override public VfxShape fieldShape() { return field != null ? field : base.fieldShape(); }
    @Override public VfxShape burstShape() { return burst != null ? burst : base.burstShape(); }
    @Override public Aura auraStyle() { return aura != null ? aura : base.auraStyle(); }
    @Override public String iconPath() { return iconPath != null ? iconPath : base.iconPath(); }
    @Override public double safeCostPercent() { return costPercent != null ? costPercent : base.safeCostPercent(); }
    @Override public double safeCostFloor() { return costFloor != null ? costFloor : base.safeCostFloor(); }
    @Override public int safeCooldownTicks() { return cooldown != null ? cooldown : base.safeCooldownTicks(); }
}
