package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Spatial Magic rebuild (Black Clover: Finral Roulacase's gate magic and Langris Vaude's folded space, as the wiki describes them): the spells of the
 * book that were re-implemented or added. Every page keeps its id, its place in the book and its mastery bit; the old casts that already did the canon
 * thing (the gate pair, the red room, Myriad Black, the Door of Fate) are called as they were and only gain what the rebuild adds: the gate frame and rift
 * summon models ({@link PropKind#SPATIAL_1} / {@link PropKind#SPATIAL_2}), the own effects (SPATIAL_FX1..3) and the temporary armour (client.aura.SpatialAura,
 * five seconds after a cast, wrapped by {@link SpatialArmourCast}).
 * <ul>
 *   <li><b>Dimensional Armour</b> (new, 20 s): the Gate Garb. Blows aimed at you may fall through a gate and miss (15 to 35 percent by EP), the rest are
 *       cut by a fifth.</li>
 *   <li><b>Gate Strike</b> (new): a rift opens behind a foe and the cut comes out of it; 14 x EP scale, 40 percent of the armour ignored.</li>
 *   <li><b>Gate Swap</b> (new): you and a foe or ally swap places through two gates (bosses are too heavy: they are only shaken).</li>
 *   <li><b>Dimension Collapse</b> (new signature): space folds in on the aimed point; for two seconds everything in 6 blocks is pulled in, then the fold
 *       shuts for 22 x EP scale (bosses are pulled a third as hard, damage capped by the balance law).</li>
 * </ul>
 */
public final class SpatialWorks {
    private SpatialWorks() {}

    static final String K_GARB = "nusmp_spatial_garb_until";
    private static final int VIOLET = 0xFFB088FF;

    // ================================================================ helpers
    /** The yaw (degrees, Minecraft convention) that makes a prop's front look from {@code at} towards {@code lookAt}. */
    static float yawToward(Vec3 at, Vec3 lookAt) {
        double dx = lookAt.x - at.x, dz = lookAt.z - at.z;
        return dx * dx + dz * dz < 1.0E-8 ? 0f : (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** A standing gate frame (the summon model) at the gate's foot, facing {@code lookAt}. */
    static void gateProp(ServerLevel level, Vec3 at, Vec3 lookAt, int life, float scale, ServerPlayer owner) {
        MagicProps.spawn(level, PropKind.SPATIAL_1, at, yawToward(at, lookAt), scale, life, 0, owner);
    }

    /** A standing rift (a cut seen from the side), param 1 = the big collapse form. */
    static void riftProp(ServerLevel level, Vec3 at, float yaw, int life, float scale, int param, ServerPlayer owner) {
        MagicProps.spawn(level, PropKind.SPATIAL_2, at, yaw, scale, life, param, owner);
    }

    static void fx(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) { VfxSpawn.send(level, shape, from, to, VIOLET, ticks, power); }

    // ================================================================ hooks (registered once, from SpatialProps.init)
    public static void initHooks() {
        AttributeEvents.incoming(e -> {
            if (!(e.getEntity() instanceof ServerPlayer p) || p.getPersistentData().getLong(K_GARB) <= p.level().getGameTime()) return;
            float chance = 0.15f + 0.05f * EnergyBridge.modifier(p);
            if (p.getRandom().nextFloat() < chance) {
                e.setCanceled(true);
                fx(p.serverLevel(), VfxShape.SPATIAL_FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 1), 14, 0.7f);
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.6f);
                SpatialArmourCast.say(p, "nusmp.spatial.fell_through", "The blow falls through a gate.");
            } else e.setAmount(e.getAmount() * 0.8f);
        });
        AttributeEvents.logout(p -> p.getPersistentData().remove(K_GARB));
    }

    // ================================================================ the old pages, with the rebuild's models and effects added
    /** Spatial Cut: the old space-cut shot, then the rift it leaves where it lands. */
    static BookPage.Cast spaceCut() {
        BookPage.Cast shot = TensuraShots.shot(TensuraShots.Shot.SPACE_CUT, 12, 2.2f, 0.5f, 0);
        return (b, i, p, mode) -> {
            if (!shot.cast(b, i, p, mode)) return false;
            Vec3 hand = p.getEyePosition().add(0, -0.25, 0), end = GrimoireBook.aim(p, 14);
            float pw = EnergyBridge.scale(p);
            fx(p.serverLevel(), VfxShape.SPATIAL_FX1, hand, end, 14, pw);
            riftProp(p.serverLevel(), end.subtract(0, 0.6, 0), p.getYRot(), 24, 0.8f * pw, 0, p);
            return true;
        };
    }

    /** Unopening Red Room: the old room of folded space (the foe is shut in; walking out walks back in), now with its field and a gate on each wall. */
    static boolean redRoom(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 20);
        if (!WikiSpells.redRoom(b, i, p, mode)) return false;
        if (t != null) {
            ServerLevel sl = p.serverLevel();
            Vec3 c = t.position();
            int ticks = BalanceLaw.controlTicks(t, 100);
            fx(sl, VfxShape.SPATIAL_FX2, c, c.add(0, 1, 0), ticks, 2.2f);
            for (int k = 0; k < 4; k++) {
                double a = k * Math.PI / 2;
                Vec3 at = c.add(Math.cos(a) * 2.3, 0, Math.sin(a) * 2.3);
                gateProp(sl, at, c, Math.min(ticks, 100), 0.7f, p);
            }
        }
        return true;
    }

    /** Myriad Black: the old five cuts, each with a rift standing in the air where it passes. */
    static boolean myriadBlack(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!WikiSpells.myriadBlack(b, i, p, mode)) return false;
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        for (int k = 0; k < 5; k++) {
            int kk = k;
            SpellRuntime.later(sl, k * 3, () -> {
                Vec3 at = eye.add(look.scale(4 + kk * 2.0)).subtract(0, 1.2, 0);
                riftProp(sl, at, p.getYRot() + (kk % 2 == 0 ? 12f : -12f), 14, 0.7f, 0, p);
                fx(sl, VfxShape.SPATIAL_FX1, eye.add(0, -0.25, 0), at.add(0, 1.2, 0), 10, 0.9f);
            });
        }
        return true;
    }

    /** Door of Fate: the old gate to your spawn, with its frame standing in the world while it is open. */
    static boolean doorOfFate(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!WikiSpells.doorOfFate(b, i, p, mode)) return false;
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize();
        Vec3 gate = p.position().add(dir.scale(2));
        gateProp(p.serverLevel(), gate, p.position(), 80, 1.1f, p);
        return true;
    }

    // ================================================================ new pages (appended)
    /** Dimensional Armour: 20 s of the Gate Garb. */
    static boolean dimensionalArmour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        p.getPersistentData().putLong(K_GARB, p.level().getGameTime() + 400);
        SpatialArmourCast.on(p, Aura.SPATIAL, 400, 1);
        b.castCircle(p, 1.2f);
        fx(p.serverLevel(), VfxShape.SPATIAL_FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 1), 24, 1.1f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1f, 1.7f);
        return true;
    }

    /** Gate Strike: a rift opens behind a foe and the cut comes out of it. */
    static boolean gateStrike(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null) { GrimoireBook.fail(p, "No one to strike."); return false; }
        ServerLevel sl = p.serverLevel();
        Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0E-4) away = p.getViewVector(1f).multiply(1, 0, 1);
        Vec3 behind = t.position().add(away.normalize().scale(1.7));
        float pw = EnergyBridge.scale(p);
        riftProp(sl, behind, yawToward(behind, t.position()) + 90f, 16, 0.9f * pw, 0, p);
        Vec3 centre = t.getBoundingBox().getCenter();
        fx(sl, VfxShape.SPATIAL_FX1, behind.add(0, 1.2, 0), centre, 12, 1.1f * pw);
        fx(sl, VfxShape.SPATIAL_FX3, centre, behind.add(0, 1.2, 0), 22, 0.9f * pw);
        float raw = 14f * pw;
        raw *= EnergyBridge.armourBypass(t, p.damageSources().generic(), raw, 0.4f);
        b.hurtAs(i, p, t, mode, raw, TensuraDamageTypes.SPACE_ELEMENTAL);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        sl.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.4f);
        b.castCircle(p, 0.8f);
        return true;
    }

    /** Gate Swap: two gates, two places traded. */
    static boolean gateSwap(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 20);
        if (t == null) { GrimoireBook.fail(p, "No one to swap with."); return false; }
        ServerLevel sl = p.serverLevel();
        Vec3 a = p.position(), c = t.position();
        fx(sl, VfxShape.SPATIAL_FX3, a.add(0, 1, 0), c.add(0, 1, 0), 20, 0.9f);
        fx(sl, VfxShape.SPATIAL_FX3, c.add(0, 1, 0), a.add(0, 1, 0), 20, 0.9f);
        gateProp(sl, a, c, 24, 0.6f, p);
        gateProp(sl, c, a, 24, 0.6f, p);
        if (BalanceLaw.isBoss(t)) {
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
            SpatialArmourCast.say(p, "nusmp.spatial.too_heavy", "It is too heavy for a gate: it is only shaken.");
            return true;
        }
        p.teleportTo(c.x, c.y, c.z);
        t.teleportTo(a.x, a.y, a.z);
        p.fallDistance = 0;
        if (!t.isAlliedTo(p)) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        sl.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.2f);
        return true;
    }

    /** Dimension Collapse: space folds in on the aimed point, then shuts. */
    static boolean dimensionCollapse(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = GrimoireBook.aim(p, 26).add(0, 0.3, 0);
        float pw = EnergyBridge.scale(p), r = 6f * GrimoireBook.size(i, p) * (0.9f + 0.1f * EnergyBridge.power(p));
        b.castCircle(p, 1.6f);
        fx(sl, VfxShape.SPATIAL_FX2, c.subtract(0, 0.3, 0), c, 50, r);
        riftProp(sl, c.subtract(0, 1.0, 0), p.getYRot(), 56, Math.min(2f, r / 4f), 1, p);
        SpellRuntime.zone(sl, 40, 2, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                Vec3 in = c.subtract(t.position()).normalize().scale(BalanceLaw.isBoss(t) ? 0.12 : 0.36);
                t.setDeltaMovement(t.getDeltaMovement().multiply(0.6, 0.6, 0.6).add(in.x, 0.05, in.z));
                t.hurtMarked = true;
            }
        });
        SpellRuntime.later(sl, 44, () -> {
            fx(sl, VfxShape.SPATIAL_FX3, c, c.add(0, 1, 0), 30, r / 2.2f);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.5f);
            for (LivingEntity t : GrimoireBook.around(p, c, r * 0.8)) {
                float raw = 22f * pw;
                raw *= EnergyBridge.armourBypass(t, p.damageSources().generic(), raw, 0.3f);
                b.hurtAs(i, p, t, mode, raw, TensuraDamageTypes.SPACE_ELEMENTAL);
            }
        });
        return true;
    }
}
