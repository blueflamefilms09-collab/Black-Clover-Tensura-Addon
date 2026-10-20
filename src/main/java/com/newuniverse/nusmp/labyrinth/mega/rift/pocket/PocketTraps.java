package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.List;

/**
 * Why: the elemental traps of the ancient tombs, "fire, plants and the like that trigger on contact". A trap room has four tiles of three by three blocks with the element's rune at the middle
 * ({@link PocketBuild} sets it); a delver who steps within {@link PocketRules#TRAP_REACH} of the middle of a tile fires it: a burst of the element's particle, its sound, a hit that is a share of the victim's
 * maximum health ({@link PocketElement#share}), capped and NEVER lethal ({@link PocketRules#trapDamage} leaves at least one half heart), and the element's status or fire. A tile fires for the same delver
 * at most once a second and a half ({@link PocketRules#TRAP_COOLDOWN}). A careful delver can see them coming: while anyone is within five blocks the tile breathes a faint ring of its particle. Every
 * id is resolved from the vanilla registries at the moment of use, and a missing damage type falls back to magic: a trap never throws. No element uses hunger or exhaustion.
 */
public final class PocketTraps {
    private PocketTraps() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** Fires the traps under {@code p} (and breathes the warning rings of the near ones). Returns how many fired. */
    public static int tick(ServerLevel level, PocketRuntime rt, ServerPlayer p, long now, boolean ring) {
        if (p.isSpectator() || p.isCreative()) return 0;
        List<PocketPlan.Feature> traps = rt.plan.all(PocketPlan.Type.TRAP);
        int fired = 0;
        for (int i = 0; i < traps.size(); i++) {
            PocketPlan.Feature f = traps.get(i);
            double tx = PocketCell.centreX(rt.slot) + f.x(), tz = PocketCell.centreZ() + f.z();
            double dx = p.getX() - (tx + 0.5), dz = p.getZ() - (tz + 0.5);
            double d2 = dx * dx + dz * dz;
            if (d2 > 25.0 || Math.abs(p.getY() - f.y()) > 3.0) continue;
            PocketElement e = PocketElement.byId(f.tag());
            if (e == null) continue;
            if (ring && d2 > PocketRules.TRAP_REACH * PocketRules.TRAP_REACH) warn(level, e, tx + 0.5, f.y() + 0.15, tz + 0.5, now);
            if (!PocketRules.onTile(p.getX(), p.getZ(), tx, tz)) continue;
            String key = PocketRuntime.trapKey(p.getUUID(), i);
            Long last = rt.trapCooldown.get(key);
            if (last != null && now - last < PocketRules.TRAP_COOLDOWN) continue;
            rt.trapCooldown.put(key, now);
            fire(level, e, p, tx + 0.5, f.y(), tz + 0.5);
            fired++;
        }
        if (rt.trapCooldown.size() > 400) rt.trapCooldown.values().removeIf(t -> now - t > PocketRules.TRAP_COOLDOWN * 4L);
        return fired;
    }

    private static void warn(ServerLevel level, PocketElement e, double x, double y, double z, long now) {
        if (now % 20 != 0) return;
        ParticleOptions opt = particle(e);
        if (opt == null) return;
        for (int k = 0; k < 8; k++) {
            double a = Math.PI * 2 * k / 8;
            level.sendParticles(opt, x + Math.cos(a) * 1.4, y, z + Math.sin(a) * 1.4, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    /** One blow of an element on a delver. */
    public static void fire(ServerLevel level, PocketElement e, ServerPlayer p, double x, double y, double z) {
        ParticleOptions opt = particle(e);
        if (opt != null) level.sendParticles(opt, x, y + 0.6, z, 36, 0.7, 0.6, 0.7, 0.04);
        SoftSound.playVanilla(level, p.blockPosition(), e.sound(), 1.0f, 1.0f);
        float dmg = PocketRules.trapDamage(p.getMaxHealth(), p.getHealth(), e.share());
        if (dmg > 0f) p.hurt(source(level, e), dmg);
        if (!e.effect().isEmpty() && !PocketElement.forbiddenEffect(e.effect())) {
            ResourceLocation rl = ResourceLocation.tryParse(e.effect());
            Holder<MobEffect> eff = rl == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
            if (eff != null) p.addEffect(new MobEffectInstance(eff, e.effectSeconds() * 20, e.effectAmplifier(), true, true));
        }
        if (e.igniteSeconds() > 0) p.igniteForSeconds(e.igniteSeconds());
    }

    private static DamageSource source(ServerLevel level, PocketElement e) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(e.damageType());
            if (rl != null) {
                var reg = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
                Holder<DamageType> h = reg.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, rl)).orElse(null);
                if (h != null) return new DamageSource(h);
            }
        } catch (RuntimeException ex) {
            LOG.debug("[nusmp] damage type {} not usable: {}", e.damageType(), ex.toString());
        }
        return level.damageSources().magic();
    }

    /** The element's particle as an option (a dust of its colour for dust ids, the simple particle of that id otherwise), or null. */
    public static ParticleOptions particle(PocketElement e) {
        if (e.particle().equals("minecraft:dust")) return new DustParticleOptions(new Vector3f(((e.rgb() >> 16) & 255) / 255f, ((e.rgb() >> 8) & 255) / 255f, (e.rgb() & 255) / 255f), 1.0f);
        ResourceLocation rl = ResourceLocation.tryParse(e.particle());
        if (rl == null) return null;
        return BuiltInRegistries.PARTICLE_TYPE.getOptional(rl).filter(t -> t instanceof SimpleParticleType).map(t -> (ParticleOptions) t).orElse(null);
    }
}
