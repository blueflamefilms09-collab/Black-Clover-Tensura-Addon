package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.DarkProps;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Dark Magic, rebuilt after Yami Sukehiro (the Dark Cloaked style: his slashes, the Black Hole, the Black Moon, the Black Blade, the Dimension Slash). The nine pages that existed keep
 * their ids, order and cost tier and are re-implemented with their numbers kept (CanonSpells is untouched) on the new bodies: crescents and the black blade are models
 * ({@link DarkProps}), the black moon and the hole are models, every page draws the rebuilt Dark VFX ({@code DARKFX_*}), and every successful cast puts the Dark armour on the caster
 * for 5 s ({@link WaterArmour}); the long spells keep it for their whole duration. Three pages are appended.
 * <pre>
 *   id                      what it does (numbers as before)
 *   avidya_slash            a crescent flies 17 blocks, 12 damage, cuts every spell it passes
 *   black_hole              a hole at the aim point for 3 s swallows spells within 5 blocks and slows / weakens whoever cast them
 *   dimension_slash         an 18-block cut, 18 damage, spells in it destroyed
 *   death_thrust            8 blocks, 13 damage; under Black Moon 14 blocks and 26 and the moon is spent
 *   black_blade             +2 reach and Strength for 30 s, the black blade hangs in front of you the whole time
 *   avidya_wild_slash       six crescents sprayed round your look
 *   black_moon              10 s: every enemy spell within 12 blocks is swallowed (allies' pass); the moon hangs overhead
 *   iai_slash               dash through the nearest foe (15, 22 under Black Moon)
 *   dimension_slash_equinox a 48-block cut, 28 damage
 *   dark_cloak              (new) Dark Cloaked Guard: 15 s of resistance and absorption, 12 percent of the damage you deal comes back as healing
 *   shadow_embrace          (new) a void round you for 4 s: foes are slowed, blinded and drained of magicules (Energy Drain, up to five at once)
 *   black_hole_collapse     (new) a great hole at the aim point for 3.5 s pulls foes in (hard control, after the shared control gate; never a boss) and collapses: 30 damage in 8 blocks
 * </pre>
 */
public final class DarkArts {
    private DarkArts() {}

    private static final ResourceLocation BLACK_BLADE = ResourceLocation.fromNamespaceAndPath("nusmp", "black_blade_reach");

    private static boolean ally(ServerPlayer p, LivingEntity t) { return t == p || t.isAlliedTo(p); }

    /** A flying crescent: the bolt that hits, the model that is seen, the cut streaks that are drawn, every enemy spell in its path removed. */
    static void slash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 dir, float dmg, double speed, int life, float size) {
        Vec3 eye = p.getEyePosition(), n = dir.normalize();
        SpellRuntime.bolt(p, eye, n.scale(speed), 1.1, life, true, null, (bolt, t) -> {
            if (ally(p, t)) return;
            b.hurt(i, p, t, mode, dmg);
            Vec3 push = t.position().subtract(p.position()).normalize().scale(0.6);
            t.push(push.x, 0.15, push.z);
            t.hurtMarked = true;
        }, null);
        Vec3 end = eye.add(n.scale(speed * life));
        CanonSpells.cutSpells(p, new AABB(eye, end).inflate(1.3));
        DarkProps.crescent(p.serverLevel(), p, eye.add(n.scale(0.9)).add(0, -0.35, 0), n, speed, life, size);
        b.vfx(p, VfxShape.DARKFX_CAST, eye, end, Math.max(10, life), 0.9f * size);
    }

    /** A long straight cut along the look: spells in it removed, everything standing in it hit. */
    static void cut(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, double length, double width, float dmg, float fx) {
        Vec3 a = p.getEyePosition(), end = a.add(p.getViewVector(1f).scale(length));
        CanonSpells.cutSpells(p, new AABB(a, end).inflate(width));
        for (LivingEntity t : GrimoireBook.along(p, a, end, width)) b.hurt(i, p, t, mode, dmg);
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.DARKFX_CAST, a, end, 20, fx);
        b.vfx(p, VfxShape.DARKFX_BURST, end, end.add(0, 1, 0), 24, Math.min(2.2f, fx * 0.8f));
    }

    // ---------------------------------------------------------------- the nine that existed
    static boolean avidyaSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        slash(b, i, p, mode, p.getViewVector(1f), 12, 1.2, 14, 1.0f);
        b.castCircle(p, 0.6f);
        return true;
    }

    static boolean avidyaWildSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 look = p.getViewVector(1f);
        var rnd = p.getRandom();
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(rnd.nextGaussian() * 18), e = rnd.nextGaussian() * 0.15;
            Vec3 dir = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), look.y + e, look.x * Math.sin(a) + look.z * Math.cos(a));
            SpellRuntime.later(p.serverLevel(), k * 3, () -> slash(b, i, p, mode, dir, 7, 1.2, 12, 0.8f));
        }
        b.castCircle(p, 0.8f);
        return true;
    }

    static boolean blackHole(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 12);
        b.castCircle(p, 1f);
        DarkProps.hole(p.serverLevel(), p, c.add(0, 1.0, 0), 60, 1.5f);
        b.vfx(p, VfxShape.DARKFX_ZONE, c, c.add(0, 1, 0), 60, 5f);
        SpellRuntime.zone(p.serverLevel(), 60, 2, age -> {
            for (Entity caster : CanonSpells.cutSpells(p, new AABB(c, c).inflate(5)))
                if (caster instanceof LivingEntity le && !ally(p, le)) {
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6));
                    le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2));
                }
        });
        return true;
    }

    static boolean dimensionSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        cut(b, i, p, mode, 18, 1.4, 18, 1.5f);
        Vec3 look = p.getViewVector(1f);
        DarkProps.crescent(p.serverLevel(), p, p.getEyePosition().add(look.scale(1.2)).add(0, -0.3, 0), look, 1.8, 10, 2.2f);
        return true;
    }

    static boolean deathThrust(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        boolean moon = CanonSpells.blackMoonActive(p);
        cut(b, i, p, mode, moon ? 14 : 8, 0.8, moon ? 26 : 13, moon ? 1.4f : 0.9f);
        if (moon) p.getPersistentData().putLong("nusmp_black_moon_until", 0);
        else p.displayClientMessage(Component.literal("Without Black Moon the thrust is only a thrust.").withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    static boolean blackBlade(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        var reach = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach != null) {
            reach.removeModifier(BLACK_BLADE);
            reach.addTransientModifier(new AttributeModifier(BLACK_BLADE, 2.0, AttributeModifier.Operation.ADD_VALUE));
        }
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
        SpellRuntime.later(p.serverLevel(), 600, () -> { var r = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE); if (r != null) r.removeModifier(BLACK_BLADE); });
        b.castCircle(p, 0.6f);
        DarkProps.blade(p.serverLevel(), p, 600, 1.0f, false);
        b.vfx(p, VfxShape.DARKFX_BURST, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 22, 0.7f);
        WaterArmour.put(p, Aura.DARK, 600, 0);
        return true;
    }

    static boolean blackMoon(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        p.getPersistentData().putLong("nusmp_black_moon_until", p.level().getGameTime() + 200);
        Vec3 c = p.position();
        b.castCircle(p, 1.4f);
        DarkProps.moon(p.serverLevel(), p, c.add(0, 7.5, 0), 200, 2.6f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.DARKFX_ZONE, p, c, b.color, 200, 9f);
        SpellRuntime.zone(p.serverLevel(), 200, 2, age -> {
            if (!CanonSpells.blackMoonActive(p)) return;
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(12))) {
                if (pr.getOwner() == p) continue;
                if (pr.getOwner() instanceof LivingEntity o && ally(p, o)) continue;
                pr.discard();
            }
        });
        WaterArmour.put(p, Aura.DARK, 200, 0);
        return true;
    }

    static boolean iaiSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 8);
        if (t == null) { GrimoireBook.fail(p, "No one in reach of the draw."); return false; }
        Vec3 from = p.position(), dest = t.position().add(t.position().subtract(from).normalize().scale(1.5));
        if (!p.level().noCollision(p, p.getBoundingBox().move(dest.subtract(from)))) dest = t.position().subtract(t.position().subtract(from).normalize().scale(1.2));
        b.hurt(i, p, t, mode, CanonSpells.blackMoonActive(p) ? 22 : 15);
        p.teleportTo(dest.x, dest.y, dest.z);
        b.vfx(p, VfxShape.DARKFX_CAST, from.add(0, 1, 0), dest.add(0, 1, 0), 14, 1.3f);
        b.vfx(p, VfxShape.DARKFX_BURST, dest.add(0, 1, 0), dest.add(0, 1, 0), 22, 0.8f);
        return true;
    }

    static boolean dimensionSlashEquinox(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        cut(b, i, p, mode, 48, 2.4, 28, 2.4f);
        Vec3 look = p.getViewVector(1f);
        for (int k = -1; k <= 1; k++) {
            double a = Math.toRadians(k * 6.0);
            Vec3 d = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), look.y, look.x * Math.sin(a) + look.z * Math.cos(a));
            DarkProps.crescent(p.serverLevel(), p, p.getEyePosition().add(d.scale(1.4)).add(0, -0.3, 0), d, 3.2, 15, 2.8f);
        }
        WaterArmour.put(p, Aura.DARK, 200, 1);
        return true;
    }

    // ---------------------------------------------------------------- appended
    static final String CLOAK = "DarkCloakUntil";

    /** Dark Cloaked Guard: 15 s of resistance and absorption; while it lasts 12 percent of the damage you deal comes back to you as healing (DarkBook.onDamageEntity). */
    static boolean darkCloak(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.1f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1 + Math.min(2, EnergyBridge.modifier(p))));
        i.getOrCreateTag().putLong(CLOAK, p.level().getGameTime() + 300);
        i.markDirty();
        b.vfx(p, VfxShape.DARKFX_BURST, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 22, 0.9f);
        WaterArmour.put(p, Aura.DARK, 300, 0);
        return true;
    }

    /** Called by DarkBook when the caster hits something: heals 12 percent of the extra damage while Dark Cloaked Guard is up. */
    static void lifesteal(ManasSkillInstance i, ServerPlayer p, float dealt) {
        if (p.level().getGameTime() >= i.getOrCreateTag().getLong(CLOAK) || dealt <= 0f) return;
        BalanceLaw.heal(p, Math.min(6f, dealt * 0.12f));
    }

    /** Shadow Embrace: a void round you for 4 s: foes within 7 blocks (scaled by EP) are slowed and blinded, and up to five at a time are drained of magicules. */
    static boolean shadowEmbrace(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 7 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.3f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.DARKFX_ZONE, p, p.position(), b.color, 80, r);
        SpellRuntime.zone(p.serverLevel(), 80, 10, age -> {
            int drained = 0;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
                if (ally(p, t)) continue;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                if (drained++ < 5) EnergyBridge.drain(t, p, 0.02);
                if (age % 20 == 0) b.hurt(i, p, t, mode, 3f);
            }
        });
        WaterArmour.put(p, Aura.DARK, 100, 0);
        return true;
    }

    /** Black Hole Collapse: a great black hole at the aim point for 3.5 s pulls the foes in (never a boss; the shared control gate first) and then collapses: 30 damage within 8 blocks. */
    static boolean blackHoleCollapse(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 16);
        float r = 8 * GrimoireBook.size(i, p);
        boolean pull = GrimoireBook.control(p);
        b.castCircle(p, 1.6f);
        DarkProps.hole(p.serverLevel(), p, c.add(0, 1.4, 0), 76, 2.8f);
        b.vfx(p, VfxShape.DARKFX_ZONE, c, c.add(0, 1, 0), 76, r);
        SpellRuntime.zone(p.serverLevel(), 70, 2, age -> {
            for (Entity caster : CanonSpells.cutSpells(p, new AABB(c, c).inflate(r))) { /* the hole swallows every spell in its reach */ }
            if (!pull) return;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (ally(p, t) || BalanceLaw.isBoss(t)) continue;
                Vec3 pullv = c.subtract(t.position()).normalize().scale(0.3);
                t.setDeltaMovement(pullv.x, Math.max(t.getDeltaMovement().y, 0.04), pullv.z);
                t.hurtMarked = true;
                if (age % 10 == 0) b.hurt(i, p, t, mode, 2f);
            }
        });
        SpellRuntime.later(p.serverLevel(), 72, () -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (ally(p, t)) continue;
                b.hurt(i, p, t, mode, 30f);
            }
            b.vfx(p, VfxShape.DARKFX_BURST, c.add(0, 1, 0), c.add(0, 1, 0), 30, 2.0f);
        });
        WaterArmour.put(p, Aura.DARK, 100, 0);
        return true;
    }
}
