package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Water Magic. Undine spirit. */
public class WaterBook extends GrimoireBook {
    // 1.93.0 (the Water rebuild, after Noelle Silva): the six pages that existed keep their ids, order, names and cost tiers and are re-implemented in WaterArts (the sea dragon is a model, the
    // javelin and the lance are models, the VFX are the new WATERFX_* shapes, every cast puts the Water armour on for 5 s); four pages are appended. The old static spell methods below stay.
    private final List<BookPage> pages = List.of(
            WaterArmour.wrap(BookPage.starter("waterball", "Sea Dragon's Waterball", TensuraShots.shot(TensuraShots.Shot.WATER_BALL, 9, 1.4f, 1.2f, 0)), Aura.WATER),
            WaterArmour.wrap(BookPage.signature("roar", "Sea Dragon's Roar", WaterArts::roar), Aura.WATER),
            WaterArmour.wrap(BookPage.zone("cradle", "Sea Dragon's Cradle", WaterArts::cradle), Aura.WATER),
            // 0.31: wiki spells, appended
            WaterArmour.wrap(BookPage.mid("aqua_javelin", "Aqua Javelin", WaterArts::aquaJavelin), Aura.WATER),
            WaterArmour.wrap(BookPage.zone("sea_dragons_nest", "Sea Dragon's Nest", WaterArts::nest).withCooldown(600), Aura.WATER),
            BookPage.signature("valkyrie_dress", "Valkyrie Dress", WaterArts::valkyrieDress).withCooldown(1200),
            // 1.93.0: appended
            WaterArmour.wrap(BookPage.signature("sea_dragons_wrath", "Sea Dragon's Wrath", WaterArts::wrath).withCooldown(900), Aura.WATER),
            WaterArmour.wrap(BookPage.mid("tide_lance", "Valkyrie Tide Lance", WaterArts::tideLance).withCooldown(300), Aura.WATER),
            BookPage.mid("scale_mail", "Sea Dragon's Scale Mail", WaterArts::scaleMail).withCooldown(900),
            WaterArmour.wrap(BookPage.zone("torrent", "Sea Dragon's Torrent", WaterArts::torrent).withCooldown(400), Aura.WATER));

    public WaterBook() { super(MagicType.WATER, 0xFF4FA8FF); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.WATER_ELEMENTAL; }
    @Override public Element spiritElement() { return Element.WATER; }
    @Override public String spiritName() { return "Undine"; }

    static boolean waterball(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.7f);
        SpellRuntime.bolt(p, start, dir.scale(1.1), 0.7 * s, 28, false, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 9f);
            t.clearFire();
            t.knockback(1.2, -bolt.vel.x, -bolt.vel.z);
        }, (bolt, at) -> b.vfx(p, VfxShape.WATER_SPLASH, at, at, 18, 0.7f * s));
        return true;
    }

    /** A homing water dragon. Big knockback, splash. Signature (screen shake). */
    static boolean roar(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        LivingEntity target = target(p, 32);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1.4f);
        // the sea dragon: drawn along the line to the target (or aim); the bolt below does the hitting
        Vec3 end = target != null ? target.position().add(0, target.getBbHeight() / 2, 0) : aim(p, 32);
        int flight = Math.max(10, Math.min(70, (int) (start.distanceTo(end) / 0.9) + 6));
        com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.WATER_DRAGON, start, end, 0, flight, 1.2f * s);
        SpellRuntime.bolt(p, start, dir.scale(0.9), 1.4 * s, 70, false, target, (bolt, t) -> {}, (bolt, at) -> {
            for (LivingEntity t : around(p, at, 2.5 * s)) {
                b.hurt(i, p, t, mode, 20f);
                t.clearFire();
                Vec3 away = t.position().subtract(at).normalize();
                t.knockback(2.5, -away.x, -away.z);
            }
            com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.WATER_BURST, at, at, 0, 24, 1.4f * s);
        });
        return true;
    }

    /** A water sphere: you and nearby players regenerate, projectiles inside crawl. */
    static boolean cradle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 4 * size(i, p);
        b.castCircle(p, 1f);
        Vec3 centre = p.position().add(0, 1, 0);       // the whirling sphere, following you for the whole spell
        com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), new com.newuniverse.nusmp.vfx.VfxPayload(VfxShape.WATER_CRADLE.ordinal(),
                centre, centre, 0, 160, r, p.getId(), p.getRandom().nextLong()));
        SpellRuntime.zone(p.serverLevel(), 160, 1, age -> {
            Vec3 c = p.position();
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r))) {
                if (pr.getOwner() != p) { pr.setDeltaMovement(pr.getDeltaMovement().scale(0.6)); pr.hurtMarked = true; }
            }
            if (age % 20 == 0) {
                for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r))) {
                    if (ally == p || ally.isAlliedTo(p)) BalanceLaw.heal(ally, 2f);
                }
            }
        });
        return true;
    }

    /** Aqua Javelin: a high-pressure lance of water that pierces everything in a long line. */
    static boolean aquaJavelin(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.8f);
        com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.WATER_JAVELIN, start, start.add(dir.scale(26)), 0, 14, 1.2f * s);
        SpellRuntime.bolt(p, start, dir.scale(2.6), 0.6 * s, 10, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 12f);
            t.clearFire();
            t.knockback(0.7, -dir.x, -dir.z);
            com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.WATER_BURST, t.position().add(0, t.getBbHeight() / 2, 0), t.position(), 0, 16, 0.5f);
        }, null);
        return true;
    }

    /** Sea Dragon's Nest: a dome of water over you for 10 s: enemy spells inside are swallowed, foes are slowed and battered, allies heal. */
    static boolean nest(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 6 * size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.2f);
        com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.WATER_NEST, c, c.add(0, 1, 0), 0, 200, r);
        SpellRuntime.zone(p.serverLevel(), 200, 5, age -> {
            AABB box = new AABB(c, c).inflate(r);
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, box)) {
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
                if (pr.position().distanceToSqr(c) <= r * r) pr.discard();
            }
            for (LivingEntity t : around(p, c, r)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                if (age % 20 == 0) b.hurt(i, p, t, mode, 3f);
            }
            if (age % 20 == 0) {
                for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, box)) {
                    if ((ally == p || ally.isAlliedTo(p)) && ally.position().distanceToSqr(c) <= r * r) BalanceLaw.heal(ally, 1.5f);
                }
            }
        });
        return true;
    }

    /** Valkyrie Dress: water armour for 30 s: faster, stronger, harder to hurt, light on your feet. */
    static boolean valkyrieDress(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.4f);
        com.newuniverse.nusmp.vfx.VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WATER_DRESS, p, p.position().add(0, 1, 0), 0, 600, 1f);
        com.newuniverse.nusmp.blackclover.ModeArmor.start(p, com.newuniverse.nusmp.blackclover.ModeArmor.Mode.VALKYRIE, 600);           // 0.38: the Valkyrie Dress armour overlay
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 600, 1));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
        p.clearFire();
        return true;
    }
}
