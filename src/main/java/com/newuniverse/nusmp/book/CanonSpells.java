package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Spells rebuilt after the Black Clover wiki (0.28): Anti Magic (Asta), Sword Magic (Licht) and Yami Sukehiro's Dark Magic.
 * Each method is a {@link BookPage.Cast}; the books list them as pages (existing page slots keep their place so unlocks stay).
 */
public final class CanonSpells {
    private CanonSpells() {}

    // ---------------------------------------------------------------- shared helpers
    /** Removes other people's projectiles (spells) in a box; returns their owners. */
    static Set<Entity> cutSpells(ServerPlayer p, AABB box) {
        Set<Entity> owners = new HashSet<>();
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, box)) {
            if (pr.getOwner() == p) continue;
            if (pr.getOwner() != null) owners.add(pr.getOwner());
            pr.discard();
        }
        for (AreaEffectCloud c : p.serverLevel().getEntitiesOfClass(AreaEffectCloud.class, box)) if (c.getOwner() != p) c.discard();
        return owners;
    }

    /** Bats other people's projectiles back where they came from (the flat of the Demon-Slayer Sword). */
    static void reflect(ServerPlayer p, AABB box) {
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, box)) {
            if (pr.getOwner() == p) continue;
            pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.2));
            pr.setOwner(p);
            pr.hurtMarked = true;
        }
    }

    static void strip(LivingEntity t) {
        for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) if (e.getEffect().value().isBeneficial()) { t.removeEffect(e.getEffect()); return; }
    }

    static boolean ally(ServerPlayer p, LivingEntity t) { return t == p || t.isAlliedTo(p); }

    /** A flying slash: a bolt that cuts every spell it passes through and hits what it meets. */
    static void flyingSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 dir, float dmg, double speed, int life,
                            VfxShape shape, boolean antiMagic) {
        Vec3 eye = p.getEyePosition();
        SpellRuntime.bolt(p, eye, dir.normalize().scale(speed), 1.1, life, true, null, (bolt, t) -> {
            if (ally(p, t)) return;
            b.hurt(i, p, t, mode, dmg);
            if (antiMagic) strip(t);
            Vec3 push = t.position().subtract(p.position()).normalize().scale(0.6);
            t.push(push.x, 0.15, push.z);
            t.hurtMarked = true;
        }, null);
        Vec3 end = eye.add(dir.normalize().scale(speed * life));
        cutSpells(p, new AABB(eye, end).inflate(1.3));
        b.vfx(p, shape, eye, end, Math.max(10, life), 1.3f);
    }

    /** A long straight cut along the look direction. */
    static void longCut(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, double length, double width, float dmg,
                        VfxShape shape, boolean antiMagic, boolean spareAllies) {
        Vec3 a = p.getEyePosition(), end = a.add(p.getViewVector(1f).scale(length));
        cutSpells(p, new AABB(a, end).inflate(width));
        for (LivingEntity t : GrimoireBook.along(p, a, end, width)) {
            if (spareAllies && ally(p, t)) continue;
            b.hurt(i, p, t, mode, dmg);
            if (antiMagic) strip(t);
        }
        b.castCircle(p, 1.2f);
        b.vfx(p, shape, a, end, 20, (float) Math.min(3, width));
    }

    // ================================================================ Anti Magic (Asta)
    /** Demon-Dweller Sword: Black Slash - a flying anti-magic slash that cuts through magic and knocks the target down. */
    public static boolean blackSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        flyingSlash(b, i, p, mode, p.getViewVector(1f), 10, 1.6, 12, VfxShape.ANTI_MAGIC_SLASH, true);
        return true;
    }

    /** Demon-Slayer Sword: Black Divider - anti-magic condensed on the edges: a huge sweep that also bats spells back. */
    public static boolean blackDivider(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        reflect(p, p.getBoundingBox().inflate(7).move(look.scale(3)));
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 9)) {
            if (ally(p, t) || t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.3) continue;
            b.hurt(i, p, t, mode, 14);
            strip(t);
        }
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.ANTI_MAGIC_SLASH, eye, eye.add(look.scale(9)), 18, 2.4f);
        return true;
    }

    /** Black Meteorite - coated in anti-magic, charge the target and slash; spells in the way are cut, no harm to you. */
    public static boolean blackMeteorite(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity target = GrimoireBook.target(p, 20);
        Vec3 from = p.position();
        Vec3 dest = target != null ? target.position().subtract(target.position().subtract(from).normalize().scale(1.5))
                : from.add(p.getViewVector(1f).multiply(1, 0, 1).normalize().scale(12));
        if (!p.level().noCollision(p, p.getBoundingBox().move(dest.subtract(from)))) { GrimoireBook.fail(p, "No room to strike."); return false; }
        cutSpells(p, new AABB(from, dest).inflate(2));
        p.teleportTo(dest.x, dest.y, dest.z);
        for (LivingEntity t : GrimoireBook.around(p, dest, 3.5)) {
            if (ally(p, t)) continue;
            b.hurt(i, p, t, mode, 16);
            strip(t);
            t.knockback(1.5, p.getX() - t.getX(), p.getZ() - t.getZ());
        }
        b.vfx(p, VfxShape.ANTI_MAGIC_SLASH, from.add(0, 1, 0), dest.add(0, 1, 0), 16, 1.8f);
        return true;
    }

    /** Black Hurricane - spin, pulled toward the magic around you: every spell and trap nearby is nullified in a moment. */
    public static boolean blackHurricane(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1f);
        SpellRuntime.zone(p.serverLevel(), 40, 5, age -> {
            cutSpells(p, p.getBoundingBox().inflate(8));
            for (LivingEntity t : GrimoireBook.around(p, p.position(), 4)) {
                if (ally(p, t)) continue;
                b.hurt(i, p, t, mode, 3);
                strip(t);
            }
            LivingEntity near = GrimoireBook.target(p, 10);                   // drawn toward magic: drift at the nearest foe
            if (near != null) {
                Vec3 d = near.position().subtract(p.position()).normalize().scale(0.25);
                p.push(d.x, 0, d.z);
                p.hurtMarked = true;
            }
            b.vfx(p, VfxShape.ANTI_MAGIC_SLASH, p.position().add(0, 1, 0), p.position().add(Math.cos(age), 1, Math.sin(age)), 10, 1.5f);
        });
        return true;
    }

    /** Black Asta - anti-magic courses through the body: much stronger and faster, near-flight, magic nearby erodes. Twice a day. */
    public static boolean blackAsta(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        var tag = i.getOrCreateTag();
        long day = p.level().getDayTime() / 24000L;
        if (tag.getLong("BlackAstaDay") != day) { tag.putLong("BlackAstaDay", day); tag.putInt("BlackAstaUses", 0); }
        int uses = tag.getInt("BlackAstaUses");
        if (uses >= 3) { GrimoireBook.fail(p, "Your body cannot take Black Asta again today."); return false; }
        if (uses == 2) p.displayClientMessage(Component.literal("A third time... your body screams.").withStyle(ChatFormatting.DARK_RED), true);
        tag.putInt("BlackAstaUses", uses + 1);
        i.markDirty();
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 2));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 600, 2));
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
        if (uses == 2) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0));
        SpellRuntime.zone(p.serverLevel(), 600, 10, age -> cutSpells(p, p.getBoundingBox().inflate(2.5)));   // magic near you erodes
        b.vfx(p, VfxShape.ANTI_MAGIC_SLASH, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 30, 2f);
        return true;
    }

    /** Bull Thrust - both swords pointed ahead, hurled forward: every spell in front is dispelled on contact. */
    public static boolean bullThrust(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position(), dir = p.getViewVector(1f).multiply(1, 0, 1).normalize(), dest = null;
        for (double d = 14; d >= 1; d -= 0.5) {
            Vec3 c = from.add(dir.scale(d));
            if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
        }
        if (dest == null) { GrimoireBook.fail(p, "No room to charge."); return false; }
        cutSpells(p, new AABB(from, dest).inflate(2));
        for (LivingEntity t : GrimoireBook.along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 1.5)) {
            if (ally(p, t)) continue;
            b.hurt(i, p, t, mode, 12);
            strip(t);
        }
        p.teleportTo(dest.x, dest.y, dest.z);
        b.vfx(p, VfxShape.ANTI_MAGIC_SLASH, from.add(0, 1, 0), dest.add(0, 1, 0), 14, 1.4f);
        return true;
    }

    /** Demon-Slasher: Infinite Slash - a large flying anti-magic slash from overhead; it cuts even through distorted space. */
    public static boolean infiniteSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        longCut(b, i, p, mode, 24, 1.6, 15, VfxShape.ANTI_MAGIC_SLASH, true, false);
        return true;
    }

    /** Demon-Slasher: Infinite Slash Equinox - a massive slash that travels very far and never harms your allies. */
    public static boolean infiniteSlashEquinox(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        longCut(b, i, p, mode, 48, 2.5, 22, VfxShape.ANTI_MAGIC_SLASH, true, true);
        return true;
    }

    // ================================================================ Sword Magic (Licht)
    /** Origin Flash - a sword appears in your hand; one swing releases a giant slash that cuts everything in its path. */
    public static boolean originFlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        longCut(b, i, p, mode, 16, 2.0, 13, VfxShape.WIND_SLASH, false, true);
        return true;
    }

    /** Origin Flash Barrage - several giant slashes at once, fanned out. */
    public static boolean originFlashBarrage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 look = p.getViewVector(1f);
        for (int k = -2; k <= 2; k++) {
            double a = Math.toRadians(k * 12);
            Vec3 dir = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), look.y, look.x * Math.sin(a) + look.z * Math.cos(a));
            flyingSlash(b, i, p, mode, dir, 8, 1.8, 10, VfxShape.WIND_SLASH, false);
        }
        b.castCircle(p, 1f);
        return true;
    }

    /** Demon-Dweller Sword: Conquering Eon - the sword drinks your allies' magic, then one devastating slash; it heals you. */
    public static boolean conqueringEon(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int allies = 0;
        for (var other : p.serverLevel().players()) if (other != p && other.distanceToSqr(p) < 16 * 16 && other.isAlliedTo(p)) allies++;
        float dmg = 20 + 4 * Math.min(5, allies);
        longCut(b, i, p, mode, 28, 2.6, dmg, VfxShape.WIND_SLASH, false, true);
        BalanceLaw.heal(p, 6 + 2 * Math.min(5, allies));
        return true;
    }

    /** Sword Magic: draw Licht's Demon-Dweller or Demon-Destroyer Sword from the grimoire for 60 s. */
    static BookPage.Cast summonSword(boolean dweller) {
        return (b, i, p, mode) -> {
            ItemStack s = MagicWeaponItem.summoned((dweller ? NUItems.LICHT_DWELLER : NUItems.LICHT_DESTROYER).get(), p, 1200);
            p.getInventory().placeItemBackInInventory(s);
            b.castCircle(p, 0.8f);
            p.displayClientMessage(Component.literal((dweller ? "Demon-Dweller" : "Demon-Destroyer") + " Sword drawn from your grimoire (60 s).")
                    .withStyle(ChatFormatting.AQUA), true);
            return true;
        };
    }
    public static boolean summonDweller(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) { return summonSword(true).cast(b, i, p, mode); }
    public static boolean summonDestroyer(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) { return summonSword(false).cast(b, i, p, mode); }

    // ================================================================ Dark Magic (Yami Sukehiro)
    static boolean blackMoonActive(ServerPlayer p) { return p.getPersistentData().getLong("nusmp_black_moon_until") > p.level().getGameTime(); }

    /** Dark Cloaked Avidya Slash - a flying wave of darkness, sharp as a blade, that swallows spells in mid-flight. */
    public static boolean avidyaSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        flyingSlash(b, i, p, mode, p.getViewVector(1f), 12, 1.2, 14, VfxShape.WIND_SLASH, false);   // dark magic is slow and heavy
        b.castCircle(p, 0.6f);
        return true;
    }

    /** Dark Cloaked Avidya Wild Slash - swinging wildly: a spray of Avidya Slashes. */
    public static boolean avidyaWildSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 look = p.getViewVector(1f);
        var rnd = p.getRandom();
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(rnd.nextGaussian() * 18), e = rnd.nextGaussian() * 0.15;
            Vec3 dir = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), look.y + e, look.x * Math.sin(a) + look.z * Math.cos(a));
            int delay = k * 3;
            SpellRuntime.later(p.serverLevel(), delay, () -> flyingSlash(b, i, p, mode, dir, 7, 1.2, 12, VfxShape.WIND_SLASH, false));
        }
        b.castCircle(p, 0.8f);
        return true;
    }

    /** Black Hole - a small black hole (5 m at most) that swallows spells and briefly paralyses whoever cast them. */
    public static boolean blackHole(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 12);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.SPATIAL_RIFT, c, c.add(0, 1, 0), 60, 1f);
        SpellRuntime.zone(p.serverLevel(), 60, 2, age -> {
            for (Entity caster : cutSpells(p, new AABB(c, c).inflate(5)))
                if (caster instanceof LivingEntity le && !ally(p, le)) {
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6));
                    le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2));
                }
        });
        return true;
    }

    /** Dark Cloaked Dimension Slash - a downward slash of darkness that flies out and cuts clouds of mana, space and spells. */
    public static boolean dimensionSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        longCut(b, i, p, mode, 18, 1.4, 18, VfxShape.WIND_SLASH, false, false);
        Vec3 end = p.getEyePosition().add(p.getViewVector(1f).scale(18));
        b.vfx(p, VfxShape.SPATIAL_RIFT, end, end.add(0, 1, 0), 20, 0.8f);
        return true;
    }

    /** Death Thrust - with Black Moon up, the whole zone is crushed into the arm: one thrust, a focused blast. */
    public static boolean deathThrust(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        boolean moon = blackMoonActive(p);
        longCut(b, i, p, mode, moon ? 14 : 8, 0.8, moon ? 26 : 13, VfxShape.WIND_SLASH, false, false);
        if (moon) p.getPersistentData().putLong("nusmp_black_moon_until", 0);     // the zone was spent on the thrust
        else p.displayClientMessage(Component.literal("Without Black Moon the thrust is only a thrust.").withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    private static final net.minecraft.resources.ResourceLocation BLACK_BLADE = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("nusmp", "black_blade_reach");

    /** Dark Cloaked Black Blade - darkness coats the sword and forms a longer blade: more reach and bite for 30 s. */
    public static boolean blackBlade(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        var reach = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach != null) {
            reach.removeModifier(BLACK_BLADE);
            reach.addTransientModifier(new AttributeModifier(BLACK_BLADE, 2.0, AttributeModifier.Operation.ADD_VALUE));
        }
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
        SpellRuntime.later(p.serverLevel(), 600, () -> { var r = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE); if (r != null) r.removeModifier(BLACK_BLADE); });
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.WIND_SLASH, p.getEyePosition(), p.getEyePosition().add(p.getViewVector(1f).scale(3)), 14, 0.8f);
        return true;
    }

    /** Black Moon - Mana Zone: a black hole overhead swallows every enemy spell in the area (allies' spells pass) for 10 s. */
    public static boolean blackMoon(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        p.getPersistentData().putLong("nusmp_black_moon_until", p.level().getGameTime() + 200);
        Vec3 c = p.position();
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.SPATIAL_RIFT, c.add(0, 6, 0), c.add(0, 7, 0), 200, 2.2f);
        SpellRuntime.zone(p.serverLevel(), 200, 2, age -> {
            if (!blackMoonActive(p)) return;
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(12))) {
                if (pr.getOwner() == p) continue;
                if (pr.getOwner() instanceof LivingEntity o && ally(p, o)) continue;   // discriminates: allies' spells are left alone
                pr.discard();
            }
        });
        return true;
    }

    /** Dark Cloaked Iai Slash - sheathed, Black Moon condensed; the blade is drawn and you dash through the nearest foe. */
    public static boolean iaiSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 8);
        if (t == null) { GrimoireBook.fail(p, "No one in reach of the draw."); return false; }
        Vec3 from = p.position(), dest = t.position().add(t.position().subtract(from).normalize().scale(1.5));
        if (!p.level().noCollision(p, p.getBoundingBox().move(dest.subtract(from)))) dest = t.position().subtract(t.position().subtract(from).normalize().scale(1.2));
        b.hurt(i, p, t, mode, blackMoonActive(p) ? 22 : 15);
        p.teleportTo(dest.x, dest.y, dest.z);
        b.vfx(p, VfxShape.WIND_SLASH, from.add(0, 1, 0), dest.add(0, 1, 0), 10, 1.2f);
        return true;
    }

    /** Dark Cloaked Dimension Slash: Equinox - with Mana Zone, the Dimension Slash's range and reach grow enormously. */
    public static boolean dimensionSlashEquinox(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        longCut(b, i, p, mode, 48, 2.4, 28, VfxShape.WIND_SLASH, false, false);
        Vec3 end = p.getEyePosition().add(p.getViewVector(1f).scale(48));
        b.vfx(p, VfxShape.SPATIAL_RIFT, end, end.add(0, 1, 0), 30, 1.6f);
        return true;
    }
}
