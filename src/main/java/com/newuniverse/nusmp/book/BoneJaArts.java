package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.prop.BoneJaProps;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the casts of the Bone Magic remake after the Black Clover wiki (the table is {@link LightBoneJa}). Bone Magic grows and shapes the user's own bones: they merge, stretch, branch and end in
 * points. Bone Bullets (Lucius) are sharp fragments fired from a hole in the palm and spun for piercing; Eternal Fangs (Zenon) are countless bones pushed out of the body; the Demon Sword
 * Dainsleif is a bone sword coated in offensive Spatial Magic, and Spatial Rupture is the flying slashes it makes. The old pages Bone Armor, Rib Cage and Bone Rain are kept as they were.
 * <p>
 * Cost rules (nothing here may lag a public server): counts and numbers come from {@link LightBoneJa} (capped); a pulse touches at most {@link LightBoneJa#MAX_HITS} foes and every search for
 * foes is made once per pulse (three for the fangs, three for the swings), never per tick and never per prop; a bone hit ignores part of the armour (a bone fragment spins through it; space cut by
 * Dainsleif ignores nearly all of it) and goes through the book's balance law; props are only the visible bodies ({@link BoneJaProps}), capped per caster and for the server
 * ({@link LightBoneJaBudget}) and never moved by the server; delayed parts check that the caster is still there, alive and in the same dimension; timed by the probe section {@code s.book.bone_ja}.
 */
public final class BoneJaArts {
    private BoneJaArts() {}

    private static final PerfProbe.Section P = LightBoneJaKit.BONE_PROBE;

    /** The page casts, timed by the performance probe. */
    public static BookPage.Cast bullets() { return LightBoneJaKit.probed(P, BoneJaArts::bullets); }
    public static BookPage.Cast eternalFangs() { return LightBoneJaKit.probed(P, BoneJaArts::eternalFangs); }
    public static BookPage.Cast dainsleif() { return LightBoneJaKit.probed(P, BoneJaArts::dainsleif); }
    public static BookPage.Cast spatialRupture() { return LightBoneJaKit.probed(P, BoneJaArts::spatialRupture); }

    /** A bone hit: raw damage through the book, with part of the target's armour ignored. */
    static void boneHit(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity t, int mode, float raw, float bypass) {
        float dmg = raw * EnergyBridge.scale(p);
        dmg *= EnergyBridge.armourBypass(t, p.damageSources().generic(), dmg, bypass);
        b.hurt(i, p, t, mode, dmg);
    }

    private static void knock(LivingEntity t, Vec3 from, double strength) {
        if (BalanceLaw.isBoss(t)) strength *= 0.3;
        Vec3 away = t.position().subtract(from).multiply(1, 0, 1);
        if (away.lengthSqr() < 1e-6) return;
        away = away.normalize().scale(strength);
        t.setDeltaMovement(t.getDeltaMovement().add(away.x, 0.12, away.z));
        t.hurtMarked = true;
    }

    // ================================================================ Bone Bullets
    /** Bone Bullets: sharp fragments of bone are fired one after another from a hole in the palm, spinning to bore through armour. */
    static boolean bullets(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        int n = LightBoneJa.bulletCount(EnergyBridge.modifier(p));
        float size = GrimoireBook.size(i, p);
        b.castCircle(p, 0.8f);
        for (int k = 0; k < n; k++) {
            LightBoneJaKit.later(level, k * 2, P, () -> {
                if (!LightBoneJaKit.here(p, level)) return;
                Vec3 look = p.getViewVector(1f), palm = p.getEyePosition().add(look.scale(0.7)).add(LightBoneJaKit.right(look).scale(0.28)).add(0, -0.35, 0);
                double spread = LightBoneJa.BULLET_SPREAD_DEG;
                Vec3 dir = LightBoneJaKit.turn(look, (level.random.nextDouble() - 0.5) * 2 * spread).add(0, (level.random.nextDouble() - 0.5) * 0.08, 0).normalize();
                LightBoneJaKit.projectile(p, PropKind.BONE_JA, BoneJaProps.BULLET, palm, dir, LightBoneJa.BULLET_SPEED, 0.45, 18, false,
                        (bolt, t) -> { boneHit(b, i, p, t, mode, LightBoneJa.BULLET_RAW, LightBoneJa.BULLET_BYPASS); knock(t, bolt.pos, 0.25); },
                        (bolt, at) -> {
                            LightBoneJaKit.bonePuff(level, at, 6, 0.25);
                            VfxSpawn.send(level, VfxShape.BONE_FX3, at, at.add(0, 0.5, 0), 0xFFE8E0C8, 10, 0.35f);
                        }, size);
                level.playSound(null, p.blockPosition(), SoundEvents.SKELETON_SHOOT, SoundSource.PLAYERS, 0.5f, 1.8f);
            });
        }
        return true;
    }

    // ================================================================ Eternal Fangs
    /** Eternal Fangs: countless bones are pushed out of the body in every direction, in three waves, too fast and too many to leave room to dodge. */
    static boolean eternalFangs(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        int n = LightBoneJa.fangCount(EnergyBridge.modifier(p));
        float size = GrimoireBook.size(i, p);
        double reach = LightBoneJa.FANG_REACH * (0.9 + 0.1 * size);
        b.castCircle(p, 1.4f);
        for (int k = 0; k < n; k++) {
            double[] d = LightBoneJa.fangDirection(k, n);
            Vec3 dir = new Vec3(d[0], d[1], d[2]);
            double len = reach * (0.7 + 0.3 * ((k * 7) % 5) / 4.0);
            int half = Math.min(255, (int) Math.round(len * 2));
            LightBoneJaKit.later(level, LightBoneJa.fangDelay(k, n), P, () -> {
                if (!LightBoneJaKit.here(p, level)) return;
                LightBoneJaKit.prop(p, PropKind.BONE_JA, BoneJaProps.FANG | (half << 8), p.position().add(0, 1.0, 0), dir, size, LightBoneJa.FANG_LIFE);
            });
        }
        for (int w = 0; w < LightBoneJa.FANG_WAVES; w++) {
            LightBoneJaKit.later(level, 1 + w * LightBoneJa.FANG_WAVE_GAP, P, () -> {
                if (!LightBoneJaKit.here(p, level)) return;
                Vec3 c = p.position().add(0, 1.0, 0);
                LightBoneJaKit.boneBurst(level, c, 14, reach * 0.35);
                VfxSpawn.send(level, VfxShape.BONE_FX2, c, c.add(0, 0.5, 0), 0xFFE8E0C8, 12, (float) (reach / 6.0));
                for (LivingEntity t : LightBoneJaKit.nearest(GrimoireBook.around(p, c, reach), c, LightBoneJa.MAX_HITS)) {      // one search per wave
                    if (!p.hasLineOfSight(t)) continue;                                           // bones do not go through walls
                    boneHit(b, i, p, t, mode, LightBoneJa.FANG_RAW, LightBoneJa.FANG_BYPASS);
                    knock(t, p.position(), 0.45);
                }
            });
        }
        level.playSound(null, p.blockPosition(), SoundEvents.SKELETON_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.6f);
        return true;
    }

    // ================================================================ Demon Sword Dainsleif
    /** Demon Sword Dainsleif: a bone sword coated in offensive Spatial Magic cuts a close-range arc three times; space itself is cut, so little armour helps. */
    static boolean dainsleif(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float size = GrimoireBook.size(i, p);
        b.castCircle(p, 1.4f);
        for (MagicPropEntity old : SongRebuildKit.mine(p, PropKind.BONE_JA)) if (BoneJaProps.variant(old.param()) == BoneJaProps.DAINSLEIF) old.expire();
        LightBoneJaKit.prop(p, PropKind.BONE_JA, BoneJaProps.DAINSLEIF, p.getEyePosition(), p.getViewVector(1f), size, LightBoneJa.DAINSLEIF_TICKS);
        double cos = Math.cos(Math.toRadians(LightBoneJa.DAINSLEIF_ARC_DEG / 2));
        for (int k = 0; k < LightBoneJa.DAINSLEIF_SWINGS; k++) {
            LightBoneJaKit.later(level, 3 + k * LightBoneJa.DAINSLEIF_GAP, P, () -> {
                if (!LightBoneJaKit.here(p, level)) return;
                Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
                LightBoneJaKit.prop(p, PropKind.BONE_JA, BoneJaProps.SLASH | BoneJaProps.SHORT | LightBoneJa.speedBits(0.7), eye.add(look.scale(1.8)).add(0, -0.3, 0), look, size, 4);
                VfxSpawn.send(level, VfxShape.BONE_FX2, eye.add(look.scale(1.8)), eye.add(look.scale(3.0)), 0xFFB48CFF, 10, 0.8f);
                LightBoneJaKit.bonePuff(level, eye.add(look.scale(2.2)), 8, 0.5);
                double reach = LightBoneJa.DAINSLEIF_REACH * (0.9 + 0.1 * size);
                for (LivingEntity t : LightBoneJaKit.nearest(GrimoireBook.around(p, p.position(), reach), p.position(), 8)) {     // one search per swing
                    Vec3 to = LightBoneJaKit.centre(t).subtract(eye);
                    if (to.lengthSqr() < 1e-6 || to.normalize().dot(look) < cos) continue;
                    boneHit(b, i, p, t, mode, LightBoneJa.DAINSLEIF_RAW, LightBoneJa.SPACE_BYPASS);
                    knock(t, p.position(), 0.35);
                }
                level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.6f);
            });
        }
        return true;
    }

    // ================================================================ Spatial Rupture
    /** Spatial Rupture: the Dainsleif is swung and flying slashes of cut space leave it, piercing everything in their way. */
    static boolean spatialRupture(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        int n = LightBoneJa.slashCount(EnergyBridge.modifier(p));
        float size = GrimoireBook.size(i, p);
        b.castCircle(p, 1.2f);
        for (MagicPropEntity old : SongRebuildKit.mine(p, PropKind.BONE_JA)) if (BoneJaProps.variant(old.param()) == BoneJaProps.DAINSLEIF) old.expire();
        LightBoneJaKit.prop(p, PropKind.BONE_JA, BoneJaProps.DAINSLEIF, p.getEyePosition(), p.getViewVector(1f), size, 16);
        LightBoneJaKit.later(level, 4, P, () -> {
            if (!LightBoneJaKit.here(p, level)) return;
            Vec3 look = p.getViewVector(1f), from = p.getEyePosition().add(look.scale(0.9)).add(0, -0.3, 0);
            double[] fan = n == 1 ? new double[]{0} : n == 2 ? new double[]{-10, 10} : new double[]{-16, 0, 16};
            for (double deg : fan) {
                Vec3 dir = LightBoneJaKit.turn(look, deg).normalize();
                LightBoneJaKit.projectile(p, PropKind.BONE_JA, BoneJaProps.SLASH, from, dir, LightBoneJa.SLASH_SPEED, LightBoneJa.SLASH_WIDTH, LightBoneJa.SLASH_LIFE, true,
                        (bolt, t) -> { boneHit(b, i, p, t, mode, LightBoneJa.SLASH_RAW, LightBoneJa.SPACE_BYPASS); knock(t, bolt.pos, 0.3); },
                        (bolt, at) -> {
                            LightBoneJaKit.bonePuff(level, at, 8, 0.4);
                            VfxSpawn.send(level, VfxShape.BONE_FX3, at, at.add(0, 0.6, 0), 0xFFB48CFF, 12, 0.6f);
                        }, size);
            }
            level.playSound(null, BlockPos.containing(from), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.1f, 0.5f);
        });
        return true;
    }

    /** The pages of the remake, for tests and the self test: ids in the order the book shows them. */
    public static List<String> pageIds() { return LightBoneJa.BONE_JA; }
}
