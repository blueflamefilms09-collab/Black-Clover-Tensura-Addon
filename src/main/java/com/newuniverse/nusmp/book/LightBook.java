package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Light Magic (0.45 remake; Lemiel, Patolli). After the wiki: the attribute's defining trait is speed, so its projectiles are
 * the fastest thing in the mod ({@link #SPEED} blocks a tick, x EP), shaped into blinding geometric swords.
 * <ul>
 *   <li><b>Light hits</b> ({@link #lightHit}): through Tensura's light element, they ignore physical armour, and cut the spirit
 *       (spiritual damage). Only an <b>Aura barrier</b> holds light back: a target whose aura is at least 30% full halves the
 *       hit and burns aura to do it.</li>
 *   <li><b>Light Speed</b> moves you at light speed and draws on Aura (physical high-speed movement).</li>
 * </ul>
 * Pages: Light Sword of Judgment and Light Shaft of Divine Punishment (ids kept, remade), then Healing Ray of Light, Lamp of
 * Avior Gloria and Light Speed (appended).
 */
public class LightBook extends GrimoireBook {
    /** Blocks per tick: the fastest projectiles in the mod (Brushstroke flies at 1.6, Reflect Ray is instant but short). */
    static final double SPEED = 6.0;
    static final int GOLD = 0xFFFFE8A0, WHITE = 0xFFFFFFF0;

    private final List<BookPage> pages = List.of(
            BookPage.starter("judgment", "Light Sword of Judgment", LightBook::sword),
            BookPage.zone("divine_punishment", "Light Shaft of Divine Punishment", LightBook::rays),
            // 0.45: appended
            BookPage.mid("healing_ray", "Healing Ray of Light", LightBook::healingRay).withCooldown(300),
            BookPage.signature("lamp_of_avior", "Lamp of Avior Gloria", LightBook::lamp).withCooldown(900),
            BookPage.mid("light_speed", "Light Speed", LightBook::lightSpeed).withCooldown(60));

    public LightBook() { super(MagicType.LIGHT, 0xFFFFF2A8); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.LIGHT_ELEMENTAL; }

    /** A light hit: armour ignored, the spirit cut, held back only by an Aura barrier. */
    static void lightHit(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity t, int mode, float raw) {
        float dmg = raw * EnergyBridge.scale(p);
        if (EnergyBridge.auraBarrier(t)) {                                          // an Aura barrier takes the brunt
            dmg *= 0.5f;
            EnergyBridge.burnAura(t, 0.05);
        } else {
            dmg *= EnergyBridge.armourBypass(t, p.damageSources().generic(), dmg, 1f);
            EnergyBridge.spirit(t, 1);
        }
        b.hurtAs(i, p, t, mode, dmg, TensuraDamageTypes.LIGHT_ELEMENTAL);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0));
    }

    /** Fires one light sword along 'dir' at light speed (a bolt that crosses 30+ blocks in a handful of ticks). */
    static void fireSword(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 from, Vec3 dir, float raw, LivingEntity homing) {
        double speed = SPEED * (0.85 + 0.25 * EnergyBridge.power(p));
        Vec3 end = from.add(dir.normalize().scale(48));
        var hit = p.level().clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 stop = hit.getLocation();
        b.vfx(p, VfxShape.LIGHT_BLADE, from, homing != null ? homing.getBoundingBox().getCenter() : stop, 10, 0.8f + 0.2f * EnergyBridge.scale(p));
        SpellRuntime.bolt(p, from, dir.normalize().scale(speed), 0.7, 9, true, homing,
                (bolt, t) -> lightHit(b, i, p, t, mode, raw),
                (bolt, at) -> b.vfx(p, VfxShape.LIGHT_FLARE, at, at, 12, 0.9f));
    }

    /** Light Sword of Judgment: three geometric light swords cross the field almost before they are seen. */
    static boolean sword(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 40);
        Vec3 look = p.getViewVector(1f), side = new Vec3(-look.z, 0, look.x).normalize();
        b.castCircle(p, 0.8f);
        for (int k = -1; k <= 1; k++) {
            Vec3 from = p.getEyePosition().add(side.scale(k * 0.9)).add(0, 0.4 + 0.2 * Math.abs(k), 0);
            Vec3 dir = t != null ? t.getBoundingBox().getCenter().subtract(from) : look;
            int kk = k;
            SpellRuntime.later(p.serverLevel(), 1 + (k + 1) * 2, () -> fireSword(b, i, p, mode, from, dir, 8f, kk == 0 ? t : null));
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.8f);
        return true;
    }

    /** Light Shaft of Divine Punishment: mark a target; shafts of light fall on it from the sky in quick succession. */
    static boolean rays(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 40);
        if (t == null) { fail(p, "Mark a target first."); return false; }
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.ELF_CIRCLE, t.position().add(0, 0.05, 0), t.position().add(0, 1, 0), 30, 0.8f);
        int shafts = 4 + MirrorWorks.tier(EnergyBridge.power(p)) * 2;
        for (int k = 0; k < shafts; k++) {
            SpellRuntime.later(p.serverLevel(), 10 + k * 2, () -> {
                if (!t.isAlive()) return;
                Vec3 at = t.position();
                b.vfx(p, VfxShape.LIGHT_BLADE, at.add(0, 18, 0), at, 6, 1.4f);
                b.vfx(p, VfxShape.LIGHT_FLARE, at.add(0, 0.2, 0), at, 10, 1.1f);
                for (LivingEntity e : around(p, at, 1.8)) lightHit(b, i, p, e, mode, 6f);
            });
        }
        return true;
    }

    /** Healing Ray of Light: a ray to the ally you look at (or yourself) that mends wounds and burns away one affliction. */
    static boolean healingRay(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        Player ally = p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(24), x -> x != p && x.isAlliedTo(p))
                .stream().filter(x -> x.getEyePosition().subtract(eye).normalize().dot(look) > 0.97)
                .min(Comparator.comparingDouble(x -> x.distanceToSqr(p))).orElse(null);
        LivingEntity who = ally != null ? ally : p;
        b.vfx(p, VfxShape.LIGHT_BLADE, eye, who.getBoundingBox().getCenter(), 10, 0.6f);
        b.vfx(p, VfxShape.LIGHT_FLARE, who.getBoundingBox().getCenter(), who.position(), 16, 1f);
        who.heal(6f * EnergyBridge.scale(p));
        for (MobEffectInstance e : new java.util.ArrayList<>(who.getActiveEffects()))
            if (!e.getEffect().value().isBeneficial()) { who.removeEffect(e.getEffect()); break; }
        p.serverLevel().playSound(null, who.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.6f);
        return true;
    }

    /** Lamp of Avior Gloria: a lamp of light hangs over you and rains light swords on every foe in reach for three seconds. */
    static boolean lamp(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        b.castCircle(p, 1.6f);
        float r = 18 * GrimoireBook.size(i, p);
        SpellRuntime.zone(level, 60, 6, age -> {
            Vec3 lampAt = p.position().add(0, 4.5, 0);
            if (age % 18 == 0) b.vfx(p, VfxShape.LIGHT_FLARE, lampAt, lampAt, 20, 2.2f);
            List<LivingEntity> foes = around(p, p.position(), r);
            for (int k = 0; k < Math.min(3, foes.size()); k++) {
                LivingEntity t = foes.get(level.random.nextInt(foes.size()));
                fireSword(b, i, p, mode, lampAt, t.getBoundingBox().getCenter().subtract(lampAt), 5f, t);
            }
        });
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.5f);
        return true;
    }

    /** Light Speed: you become light and arrive up to 24 blocks ahead in an instant, cutting through what stands between. Aura. */
    static boolean lightSpeed(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!EnergyBridge.aura(p, 0.03, 10)) { fail(p, "Not enough aura to move at light speed."); return false; }
        Vec3 from = p.position(), look = p.getViewVector(1f);
        double reach = 14 + 6 * EnergyBridge.power(p) / 2.4;
        var hit = p.level().clip(new ClipContext(p.getEyePosition(), p.getEyePosition().add(look.scale(reach)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 to = hit.getLocation().subtract(look.scale(0.8)).subtract(0, p.getEyeHeight(), 0);
        for (LivingEntity t : along(p, from.add(0, 1, 0), to.add(0, 1, 0), 1.2)) lightHit(b, i, p, t, mode, 6f);
        b.vfx(p, VfxShape.LIGHT_BLADE, from.add(0, 1, 0), to.add(0, 1, 0), 8, 1.6f);
        b.vfx(p, VfxShape.LIGHT_FLARE, to.add(0, 1, 0), to, 12, 1.2f);
        p.teleportTo(to.x, to.y, to.z);
        p.fallDistance = 0;
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 2f);
        p.displayClientMessage(Component.literal("Light Speed").withStyle(ChatFormatting.YELLOW), true);
        return true;
    }
}
