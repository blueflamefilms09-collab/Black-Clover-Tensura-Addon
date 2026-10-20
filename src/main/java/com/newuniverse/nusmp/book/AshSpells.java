package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.AirPropKit;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Ash Magic, rebuilt (Zora Ideale's magic in the story; the repo's wiki extracts name Ash Absorbing Formation and Magic Warding Ash, the wiki itself could
 * not be reached, see docs/attributes/ash_rebuild.md). The four pages every Ash grimoire has keep their ids and order (ash_bullets, ash_cloud,
 * ash_absorbing_formation, revelation_of_the_cowardly: the old implementations in WikiSpells stay, unused); three pages are appended (ash_shroud, ash_soldiers, cursed_ash).
 * <pre>
 *   0 ash_bullets                   starter    six bullets of ash in a spray: 3.5 damage each, blind 1.5 s
 *   1 ash_cloud                     mid        you vanish in a cloud of ash for 4 s: foes within 5 blocks are blinded and lose you; the cloud stays 3 s; an ash trap (ASH_1 variant 1, armed 30 s) is left under it; 5 s armour
 *   2 ash_absorbing_formation       zone       a ring of cursed ash for 8 s: drinks 3 percent of the mana of every foe inside per second into you, withers them, removes enemy shots
 *   3 revelation_of_the_cowardly    signature  the next three foes who strike you in 30 s set off an ash trap (8 damage, blind, wither II, glowing); hidden things glow; 5 s armour
 *   4 ash_shroud                    mid        9 s of the ash shroud: resistance, attackers withered (thorns), the empower bonus, the armour set
 *   5 ash_soldiers                  zone       two ash soldiers (ASH_1 variant 0) fight for you for 15 s
 *   6 cursed_ash                    mid        cursed ash on one foe: wither II, 6 damage, drains 6 percent of its mana into you
 * </pre>
 */
public final class AshSpells {
    private AshSpells() {}

    private static final int COLOR = 0xFFB0AAA2;

    static void blind(LivingEntity t, int ticks) { t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(t, ticks), 0)); }

    // ------------------------------------------------------------------ 0 Ash Bullets
    public static boolean bullets(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.ASH_FX1, eye, eye.add(look.scale(1.8 * 10)), 12, 0.34f * GrimoireBook.size(i, p));
        var rnd = p.getRandom();
        for (int k = 0; k < 6; k++) {
            Vec3 d = look.add((rnd.nextDouble() - 0.5) * 0.15, (rnd.nextDouble() - 0.5) * 0.1, (rnd.nextDouble() - 0.5) * 0.15).normalize();
            SpellRuntime.bolt(p, eye, d.scale(1.8), 0.4, 10, false, null, (bolt, t) -> {
                b.hurt(i, p, t, mode, 3.5f);
                blind(t, 30);
            }, (bolt, at) -> b.vfx(p, VfxShape.ASH_FX3, at, at.add(0, 0.5, 0), 12, 0.12f));
        }
        return true;
    }

    // ------------------------------------------------------------------ 1 Ash Cloud
    public static boolean cloud(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 0.8f);
        double r = 5 * GrimoireBook.size(i, p);
        Vec3 c = p.position();
        b.vfx(p, VfxShape.ASH_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), 90, (float) r / 1.5f);
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            blind(t, 80);
            if (t instanceof Mob m && m.getTarget() == p) m.setTarget(null);
        }
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false));
        SpellRuntime.zone(p.serverLevel(), 60, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) blind(t, 30);
            for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, new AABB(c, c).inflate(r), m -> m.getTarget() == p)) m.setTarget(null);
        });
        MagicProps.spawn(p.serverLevel(), PropKind.ASH_1, c, p.getYRot(), 1.0f, 600, AirPropKit.pack(1, 0, 0), p);          // an ash trap stays behind under the cloud
        return true;
    }

    // ------------------------------------------------------------------ 2 Ash Absorbing Formation
    public static boolean formation(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 20);
        float r = 4 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.ASH_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), 160, r / 1.5f);
        SpellRuntime.zone(p.serverLevel(), 160, 5, age -> {
            for (Projectile pr : WikiSpells.enemyShots(p, new AABB(c, c).inflate(r, 3, r))) pr.discard();
            if (age % 20 == 0) for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                WikiSpells.giveMana(p, WikiSpells.drainMana(t, 0.03));
                t.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                b.hurt(i, p, t, mode, 1.5f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ 3 Revelation of the Cowardly
    public static boolean revelation(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        i.getOrCreateTag().putLong("AshTrapUntil", p.level().getGameTime() + 600);
        i.getOrCreateTag().putInt("AshTraps", 3);
        i.markDirty();
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.ASH_FX3, p.position(), p.position().add(0, 1, 0), 30, 0.5f);
        for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16), e -> e != p && e.isAlive()))
            if (t.isInvisible() || !WikiSpells.ally(p, t)) t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        return true;
    }

    /** Springs one ash trap on an attacker (AshBook calls this from onTakenDamage): the old numbers, now with a trap of ash rising under the attacker. */
    static void trap(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity attacker) {
        var tag = i.getOrCreateTag();
        if (p.level().getGameTime() > tag.getLong("AshTrapUntil") || tag.getInt("AshTraps") <= 0 || attacker == p) return;
        tag.putInt("AshTraps", tag.getInt("AshTraps") - 1);
        i.markDirty();
        b.hurt(i, p, attacker, 0, 8f);
        attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(attacker, 80), 0));
        attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
        attacker.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        MagicProps.spawn(p.serverLevel(), PropKind.ASH_1, attacker.position(), attacker.getYRot(), 1.0f, 30, AirPropKit.pack(2, AirPropKit.ACT, 0), p);
        b.vfx(p, VfxShape.ASH_FX3, attacker.position(), attacker.position().add(0, 1, 0), 24, 0.5f);
    }

    // ------------------------------------------------------------------ 4 Ash Shroud
    public static boolean shroud(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 180;
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + ticks);
        tag.putFloat("EmpowerBonus", 4f);
        tag.putLong("ThornsUntil", p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(b.magic.guardEffect(), ticks, 0, false, false));
        p.getPersistentData().putLong("nusmp_ash_until", p.level().getGameTime() + ticks);
        b.castCircle(p, 1f);
        com.newuniverse.nusmp.vfx.VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.ASH_FX3, p, p.position().add(0, 1, 0), COLOR, 26, 0.5f);
        return true;
    }

    /** While the shroud is on, whatever strikes the wearer in melee is withered (hook registered by AshProps.init). */
    public static void witherThorns(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.getPersistentData().getLong("nusmp_ash_until") <= p.level().getGameTime()) return;
        if (e.getSource().getEntity() instanceof LivingEntity a && a != p && !a.isAlliedTo(p) && e.getSource().getDirectEntity() == a)
            a.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
    }

    // ------------------------------------------------------------------ 5 Ash Soldiers
    public static boolean soldiers(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.1f);
        int life = AirPropKit.lifeFor(p, 300);
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1).normalize(), side = new Vec3(-look.z, 0, look.x);
        for (int k = -1; k <= 1; k += 2)
            MagicProps.spawn(p.serverLevel(), PropKind.ASH_1, p.position().add(look.scale(2.2)).add(side.scale(k * 1.4)), p.getYRot() + 180f, 1.0f, life, AirPropKit.pack(0, 0, 0), p);
        b.vfx(p, VfxShape.ASH_FX3, p.position().add(look.scale(2.2)), p.position().add(look.scale(2.2)).add(0, 1, 0), 26, 0.6f);
        return true;
    }

    // ------------------------------------------------------------------ 6 Cursed Ash
    public static boolean cursed(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null || t.isAlliedTo(p)) { GrimoireBook.fail(p, "No one to curse."); return false; }
        b.castCircle(p, 0.8f);
        b.hurt(i, p, t, mode, 6f);
        t.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        WikiSpells.giveMana(p, WikiSpells.drainMana(t, BalanceLaw.isBoss(t) ? 0.02 : 0.06));
        b.vfx(p, VfxShape.ASH_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 14, 0.3f);
        b.vfx(p, VfxShape.ASH_FX3, t.position(), t.position().add(0, 1, 0), 22, 0.45f);
        return true;
    }
}
