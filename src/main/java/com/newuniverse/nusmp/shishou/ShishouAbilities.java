package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;
import com.newuniverse.nusmp.perf.PerfParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the four ACTIVE skills added by the rework: the canon Telekinetic Grasp and his three original abilities, Akashic Index, Blades of the Record and Akashic Sanctuary (docs/items/shishou_rework.md). Same
 * contract as {@link ShishouSpells}: each is server-decided and BOUNDED, returns a {@link ShishouSpells.Result} (the service pays and starts the cooldown), loads no chunk, spawns no entity, places no block.
 * Costs: Grasp one box query and at most {@link ShishouCanon#GRASP_TARGETS} hurts; Index one box query, at most {@link ShishouCanon#INDEX_TARGETS} marks (one small int table per wielder, cleaned on
 * logout, death and dimension change); Blades five waves of one box query and at most {@link ShishouCanon#BLADE_TARGETS} hurts each ({@link ShishouCanon#maxBladeHits()} in all); Sanctuary ten pulses of one box
 * query and at most {@link ShishouCanon#SANCTUARY_TARGETS} touches each. Particles: every step asks for at most {@link ShishouVfx#STEP_MAX} through {@link PerfParticles} (the perf budget drops the rest).
 * No packet of its own. There is ONE Shishou per world, so these timers exist for one wielder at a time, and the skill cooldowns (15 s to 45 s) cap how often they can start.
 */
public final class ShishouAbilities {
    private ShishouAbilities() {}

    private static ParticleOptions light() { return Fx.options(FxThemes.SKILL_SHISHOU, FxTone.LIGHT); }

    private static ParticleOptions dark() { return Fx.options(FxThemes.SKILL_SHISHOU, FxTone.DARK); }

    private static ParticleOptions rune() { return Fx.options(FxThemes.SKILL_SHISHOU, FxOld.END_ROD); }

    private static void dot(ServerLevel l, ParticleOptions p, double x, double y, double z) { PerfParticles.send(l, p, x, y, z, 1, 0.0, 0.0, 0.0, 0.0); }

    /** A turning rune ring of {@code radius} around {@code centre} at the height of the centre, at most {@link ShishouVfx#RING_MAX} points. Returns how many particles it asked for. */
    static int ring(ServerLevel l, ParticleOptions p, Vec3 centre, double radius, long t, boolean inner) {
        int n = ShishouVfx.ringPoints(radius);
        double ph = ShishouVfx.phase(t, inner);
        for (int i = 0; i < n; i++) dot(l, p, centre.x + ShishouVfx.ringX(i, n, radius, ph), centre.y, centre.z + ShishouVfx.ringZ(i, n, radius, ph));
        return n;
    }

    private static void sound(ServerLevel l, Vec3 at, net.minecraft.sounds.SoundEvent e, float vol, float pitch) { l.playSound(null, at.x, at.y, at.z, e, SoundSource.PLAYERS, vol, pitch); }

    // ================================================================ Telekinetic Grasp (Nennou Shuaku)
    /** Lifts up to four hostile creatures around the aimed point into the air for 1.5 s (Levitation) and holds them slowed for 3 s, with a small hurt. */
    static ShishouSpells.Result grasp(ShishouSpells.Caster c) {
        Vec3 at = ShishouSpells.aimPoint(c, ShishouCanon.GRASP_RANGE);
        List<Mob> list = ShishouTargets.hostilesNear(c.level(), c.wielder(), at, ShishouCanon.GRASP_SPLASH, ShishouCanon.GRASP_TARGETS);
        if (list.isEmpty()) return ShishouSpells.Result.FAIL;
        double dmg = ShishouRules.spellDamage(c.swordLevel(), 3.0, 0.12, 14.0);
        for (Mob m : list) {
            ShishouSpells.hit(c, m, dmg);
            m.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ShishouCanon.GRASP_LIFT_TICKS, ShishouCanon.GRASP_LIFT_AMP, false, false));
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ShishouCanon.GRASP_SLOW_TICKS, 3, false, false));
            int n = 10;                                                                       // a spiral lifting the creature: two turns, ten points
            for (int i = 0; i < n; i++) {
                double a = ShishouVfx.spiralAngle(i, n);
                dot(c.level(), light(), m.getX() + Math.cos(a) * 0.7, m.getY() + ShishouVfx.spiralY(i, n, 2.0), m.getZ() + Math.sin(a) * 0.7);
            }
        }
        sound(c.level(), at, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.4f);
        return ShishouSpells.Result.of(ShishouSkill.TELEKINETIC_GRASP.manaCost(), ShishouSkill.TELEKINETIC_GRASP.cooldown());
    }

    // ================================================================ Akashic Index
    /** Per wielder: the entity ids the record named (a fixed small array; the open window is {@link ShishouWindows.Kind#INDEX}). */
    private static final Map<UUID, int[]> INDEXED = new HashMap<>();

    /** Names up to six hostile creatures around the wielder for 10 s: they glow for 5 s and every blow on them hits 15% harder ({@link ShishouCanon#indexed}). */
    static ShishouSpells.Result index(ShishouSpells.Caster c) {
        if (c.wielder() == null) return ShishouSpells.Result.FAIL;
        List<Mob> list = ShishouTargets.hostilesNear(c.level(), c.wielder(), c.wielder().position(), ShishouCanon.INDEX_RADIUS, ShishouCanon.INDEX_TARGETS);
        if (list.isEmpty()) return ShishouSpells.Result.FAIL;
        int[] ids = new int[ShishouCanon.INDEX_TARGETS];
        java.util.Arrays.fill(ids, -1);
        int i = 0;
        for (Mob m : list) {
            ids[i++] = m.getId();
            m.addEffect(new MobEffectInstance(MobEffects.GLOWING, ShishouCanon.INDEX_GLOW_TICKS, 0, false, false));
            ring(c.level(), rune(), m.position().add(0.0, m.getBbHeight() + 0.4, 0.0), 0.7, c.now(), false);
        }
        if (INDEXED.size() < ShishouCanon.INDEX_TABLE_CAP || INDEXED.containsKey(c.key())) INDEXED.put(c.key(), ids);
        ShishouLive.WINDOWS.start(c.key(), ShishouWindows.Kind.INDEX, c.now(), ShishouCanon.INDEX_TICKS);
        sound(c.level(), c.wielder().position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 1.6f);
        return ShishouSpells.Result.of(ShishouSkill.AKASHIC_INDEX.manaCost(), ShishouSkill.AKASHIC_INDEX.cooldown());
    }

    /** Does the record name this creature for this wielder right now? One map read and a window read; nothing while no window is open. */
    public static boolean isIndexed(UUID wielder, int entityId, long now) {
        if (!ShishouLive.WINDOWS.anyActive(ShishouWindows.Kind.INDEX) || !ShishouLive.WINDOWS.active(wielder, ShishouWindows.Kind.INDEX, now)) return false;
        int[] ids = INDEXED.get(wielder);
        if (ids == null) return false;
        for (int id : ids) if (id == entityId) return true;
        return false;
    }

    /** The extra hit of a named creature: 15% of the blow, as its own hurt. Returns true when it was applied. */
    static boolean indexBonus(LivingEntity attacker, UUID wielder, LivingEntity target, double hit, ServerLevel sl, long now) {
        if (!ShishouConfig.canonPassives() || !isIndexed(wielder, target.getId(), now)) return false;
        double extra = ShishouCanon.indexed(hit, true) - hit;
        if (extra <= 0.0) return false;
        target.invulnerableTime = 0;
        target.hurt(sl.damageSources().indirectMagic(attacker, attacker), (float) extra);
        return true;
    }

    /** A wielder left or the world closed: their names are forgotten. */
    public static void forget(UUID who) { INDEXED.remove(who); }

    public static void clearAll() { INDEXED.clear(); }

    // ================================================================ Blades of the Record
    /** Five waves of spectral blades, one every 4 ticks, falling on the aimed point: each wave hurts up to six hostile creatures within 6 blocks for 30% of a sword strike. */
    static ShishouSpells.Result blades(ShishouSpells.Caster c) {
        if (c.wielder() == null) return ShishouSpells.Result.FAIL;
        Vec3 at = ShishouSpells.aimPoint(c, ShishouCanon.BLADE_RANGE);
        double strike = ShishouRules.strikeDamage(c.record().level(), c.record().attackSteps(), ShishouRules.elementBonus(0), ShishouConfig.strikeCap(), ShishouConfig.damagePercent());
        double dmg = ShishouCanon.bladeWave(strike, ShishouConfig.strikeCap());
        ServerLevel sl = c.level();
        ServerPlayer w = c.wielder();
        wave(c, at, dmg, 0);
        for (int i = 1; i < ShishouCanon.BLADE_WAVES; i++) {
            final int wave = i;
            SpellRuntime.later(sl, i * ShishouCanon.BLADE_GAP, () -> {
                if (!SpellRuntime.gone(w, sl)) wave(c, at, dmg, wave);
            });
        }
        sound(sl, at, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 0.7f);
        return ShishouSpells.Result.of(ShishouSkill.RECORD_BLADES.manaCost(), ShishouSkill.RECORD_BLADES.cooldown());
    }

    private static void wave(ShishouSpells.Caster c, Vec3 at, double dmg, int wave) {
        ServerLevel sl = c.level();
        for (Mob m : ShishouTargets.hostilesNear(sl, c.wielder(), at, ShishouCanon.BLADE_RADIUS, ShishouCanon.BLADE_TARGETS)) ShishouSpells.hit(c, m, dmg);
        double radius = 4.0;                                                                  // the falling blades: a ring and a column, at most ShishouVfx.STEP_MAX particles
        int budget = ShishouVfx.bladeWaveParticles(radius), used = ring(sl, rune(), at.add(0.0, 0.2, 0.0), radius, wave * 5L, wave % 2 == 1);
        for (int i = 0; i < 8 && used < budget; i++, used++) dot(sl, wave % 2 == 0 ? light() : dark(), at.x + (i % 3 - 1) * 0.6, at.y + ShishouVfx.columnY(i, 8, 5.0), at.z + (i / 3 - 1) * 0.6);
    }

    // ================================================================ Akashic Sanctuary
    /**
     * A field of the record around the wielder for 5 s: every half second the wielder is mended a little (Regeneration) and up to twelve hostile creatures within 5 blocks are slowed and hurt a little.
     * One zone of ten pulses, only one at a time per wielder ({@link ShishouWindows.Kind#SANCTUARY}); it ends at once when the wielder leaves, dies or changes dimension.
     */
    static ShishouSpells.Result sanctuary(ShishouSpells.Caster c) {
        if (c.wielder() == null) return ShishouSpells.Result.FAIL;
        if (ShishouLive.WINDOWS.active(c.key(), ShishouWindows.Kind.SANCTUARY, c.now())) return ShishouSpells.Result.FAIL;
        ShishouLive.WINDOWS.start(c.key(), ShishouWindows.Kind.SANCTUARY, c.now(), ShishouCanon.SANCTUARY_TICKS);
        ServerLevel sl = c.level();
        ServerPlayer w = c.wielder();
        double dmg = ShishouRules.spellDamage(c.swordLevel(), 2.0, 0.08, 9.0);
        SpellRuntime.zone(sl, ShishouCanon.SANCTUARY_TICKS, ShishouCanon.SANCTUARY_PULSE, age -> {
            if (SpellRuntime.gone(w, sl)) {
                ShishouLive.WINDOWS.stop(c.key(), ShishouWindows.Kind.SANCTUARY);
                return;
            }
            Vec3 centre = w.position().add(0.0, 0.1, 0.0);
            w.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 1, false, false));
            for (Mob m : ShishouTargets.hostilesNear(sl, w, centre, ShishouCanon.SANCTUARY_RADIUS, ShishouCanon.SANCTUARY_TARGETS)) {
                ShishouSpells.hit(c, m, dmg);
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false));
            }
            int budget = ShishouVfx.sanctuaryPulseParticles(ShishouCanon.SANCTUARY_RADIUS), used = 0;
            long t = sl.getGameTime();
            used += ring(sl, rune(), centre, ShishouCanon.SANCTUARY_RADIUS, t, false);
            for (int i = 0; i < 6 && used < budget; i++, used++) dot(sl, light(), centre.x + Math.cos(i) * 1.5, centre.y + ShishouVfx.columnY(i, 6, 2.5), centre.z + Math.sin(i) * 1.5);
        });
        sound(sl, w.position(), SoundEvents.BEACON_ACTIVATE, 0.8f, 1.5f);
        return ShishouSpells.Result.of(ShishouSkill.AKASHIC_SANCTUARY.manaCost(), ShishouSkill.AKASHIC_SANCTUARY.cooldown());
    }
}
