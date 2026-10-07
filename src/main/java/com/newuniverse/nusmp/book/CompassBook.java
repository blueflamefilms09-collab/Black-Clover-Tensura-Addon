package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Compass Magic (0.45, new; Letoile Becquerel, Kivn). After the wiki: compasses that change the trajectory of other spells and
 * make the user's attacks lock on and follow a target, "much like how a compass locks on to magnetic north".
 * <ul>
 *   <li><b>The array:</b> brass-and-gold compasses hover in an arc behind the caster with a golden aura, their needles swinging
 *       to the target ({@link VfxShape#COMPASS_ARRAY}).</li>
 *   <li><b>Magicule signature:</b> needles lock onto the strongest presence in sight (highest Tensura EP), or the foe marked by
 *       Another Atlas, and home on it.</li>
 *   <li><b>Redirection:</b> Willful Compass bends incoming shots right round and sends them back at whoever fired them.</li>
 * </ul>
 * Pages: Useless North (offensive), Willful Compass (defensive), Another Atlas (supplementary), all three from the wiki, and
 * Compass Rose (appended signature).
 */
public class CompassBook extends GrimoireBook {
    static final int GOLD = 0xFFFFC94A, GREEN = 0xFF4AD08A;
    static final String K_MARK = "nusmp_compass_mark", K_MARK_UNTIL = "nusmp_compass_mark_until", K_TURNED = "nusmp_compass_turned";

    private final List<BookPage> pages = List.of(
            BookPage.starter("useless_north", "Useless North", CompassBook::uselessNorth),
            BookPage.mid("willful_compass", "Willful Compass", CompassBook::willfulCompass).withCooldown(500),
            BookPage.mid("another_atlas", "Another Atlas", CompassBook::anotherAtlas).withCooldown(600),
            BookPage.signature("compass_rose", "Compass Rose", CompassBook::compassRose).withCooldown(900));

    public CompassBook() { super(MagicType.COMPASS, GOLD); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.SPACE_ELEMENTAL; }

    /** The foe the needles lock onto: the Another Atlas mark, else the strongest magicule signature in front of you. */
    static LivingEntity lock(ServerPlayer p, double range) {
        var d = p.getPersistentData();
        if (d.getLong(K_MARK_UNTIL) > p.level().getGameTime() && d.hasUUID(K_MARK)) {
            Entity e = p.serverLevel().getEntity(d.getUUID(K_MARK));
            if (e instanceof LivingEntity le && le.isAlive() && le.distanceToSqr(p) < range * range * 4) return le;
        }
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        return around(p, p.position(), range).stream()
                .filter(t -> t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > 0.7)
                .max(Comparator.comparingDouble(t -> { try { return EnergyHelper.getMaxEP(t); } catch (Throwable x) { return t.getMaxHealth(); } }))
                .orElse(target(p, range));
    }

    /** One homing needle from 'from' to 't'. */
    static void needle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 from, LivingEntity t, float raw) {
        Vec3 dir = t.getBoundingBox().getCenter().subtract(from).normalize();
        b.vfx(p, VfxShape.COMPASS_NEEDLE, from, t.getBoundingBox().getCenter(), 14, 0.8f);
        SpellRuntime.bolt(p, from, dir.scale(1.3 + 0.3 * EnergyBridge.scale(p)), 0.6, 30, false, t,
                (bolt, v) -> b.hurt(i, p, v, mode, raw * EnergyBridge.scale(p)), null);
    }

    static Vec3 behind(ServerPlayer p, int k, int n) {
        Vec3 fwd = p.getViewVector(1f).multiply(1, 0, 1).normalize(), side = new Vec3(-fwd.z, 0, fwd.x);
        double a = (n == 1 ? 0 : (k / (double) (n - 1) - 0.5)) * 2.4;
        return p.position().add(0, 1.7 + 0.5 * Math.cos(a), 0).subtract(fwd.scale(0.8)).add(side.scale(Math.sin(a) * 1.8));
    }

    /** Useless North (wiki): the compasses behind you turn to your target and loose needles that follow it wherever it goes. */
    static boolean uselessNorth(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = lock(p, 28);
        if (t == null) { fail(p, "The needles find no signature."); return false; }
        int n = 3 + MirrorWorks.tier(EnergyBridge.power(p));
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.COMPASS_ARRAY, p, t.getBoundingBox().getCenter(), GOLD, 40, n);
        b.vfx(p, VfxShape.COMPASS_LOCK, t.position().add(0, 0.05, 0), t.position(), 30, 1f);
        for (int k = 0; k < n; k++) {
            int kk = k;
            SpellRuntime.later(p.serverLevel(), 6 + k * 3, () -> { if (t.isAlive()) needle(b, i, p, mode, behind(p, kk, n), t, 5f); });
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.2f, 1f);
        return true;
    }

    /** Willful Compass (wiki): for 10 s the compasses wrench every incoming shot round and send it back at its shooter. */
    static boolean willfulCompass(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        int n = 3 + MirrorWorks.tier(EnergyBridge.power(p));
        VfxSpawn.sendFollowing(level, VfxShape.COMPASS_ARRAY, p, p.position().add(p.getViewVector(1f).scale(5)), GOLD, 200, n);
        SpellRuntime.zone(level, 200, 1, age -> {
            for (Projectile pr : level.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(7),
                    x -> x.getOwner() != p && !x.getPersistentData().getBoolean(K_TURNED))) {
                Entity shooter = pr.getOwner();
                double speed = Math.max(0.8, pr.getDeltaMovement().length());
                Vec3 to = shooter != null && shooter.isAlive() ? shooter.getBoundingBox().getCenter().subtract(pr.position()).normalize()
                        : pr.getDeltaMovement().normalize().scale(-1).yRot(0.6f);
                pr.setDeltaMovement(to.scale(speed * 1.2));
                pr.setOwner(p);
                pr.hasImpulse = true;
                pr.getPersistentData().putBoolean(K_TURNED, true);
                b.vfx(p, VfxShape.COMPASS_LOCK, pr.position(), pr.position(), 12, 0.4f);
            }
        });
        level.playSound(null, p.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.2f, 0.7f);
        return true;
    }

    /** Another Atlas (wiki): mark a foe's magicule signature for 15 s; the array fires on it by itself every second and a half. */
    static boolean anotherAtlas(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = lock(p, 32);
        if (t == null) { fail(p, "No signature to chart."); return false; }
        ServerLevel level = p.serverLevel();
        UUID id = t.getUUID();
        p.getPersistentData().putUUID(K_MARK, id);
        p.getPersistentData().putLong(K_MARK_UNTIL, level.getGameTime() + 300);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
        VfxSpawn.sendFollowing(level, VfxShape.COMPASS_LOCK, t, t.position(), GOLD, 300, 1.2f);
        VfxSpawn.sendFollowing(level, VfxShape.COMPASS_ARRAY, p, t.getBoundingBox().getCenter(), GOLD, 300, 3);
        SpellRuntime.zone(level, 300, 30, age -> {
            if (age == 0 || !t.isAlive() || !p.isAlive() || t.distanceToSqr(p) > 48 * 48) return;
            needle(b, i, p, mode, behind(p, (int) (age / 30) % 3, 3), t, 3.5f);
        });
        p.displayClientMessage(Component.literal("Charted: " + t.getName().getString()).withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** Compass Rose: every needle swings at once; each foe within 20 blocks is locked and struck by two homing needles. */
    static boolean compassRose(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        List<LivingEntity> foes = around(p, p.position(), 20 * GrimoireBook.size(i, p));
        if (foes.isEmpty()) { fail(p, "No signatures nearby."); return false; }
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.COMPASS_LOCK, p.position().add(0, 0.05, 0), p.position(), 30, 3f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.COMPASS_ARRAY, p, p.position().add(p.getViewVector(1f).scale(5)), GOLD, 50, 5);
        int k = 0;
        for (LivingEntity t : foes.subList(0, Math.min(10, foes.size()))) {
            b.vfx(p, VfxShape.COMPASS_LOCK, t.position().add(0, 0.05, 0), t.position(), 30, 0.8f);
            for (int j = 0; j < 2; j++) {
                int kk = k++;
                SpellRuntime.later(p.serverLevel(), 8 + kk * 2, () -> { if (t.isAlive()) needle(b, i, p, mode, behind(p, kk % 5, 5), t, 6f); });
            }
        }
        return true;
    }
}
