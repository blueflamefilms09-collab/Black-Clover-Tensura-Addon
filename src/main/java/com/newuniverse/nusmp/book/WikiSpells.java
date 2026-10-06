package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.34: spells rebuilt after the Black Clover wiki for Thread, Seal, Lightning, Transmutation, Poison, Spatial, Ash, Mirror,
 * Cotton / Food, Shadow and Recombination magic, plus the captains' character spells (see CharacterSpells). Every method is a
 * {@link BookPage.Cast}. Fire, Water, Wind, Earth and Time keep their own spells and effects.
 */
public final class WikiSpells {
    private WikiSpells() {}

    // ---------------------------------------------------------------- shared
    static boolean ally(ServerPlayer p, LivingEntity t) { return t == p || t.isAlliedTo(p); }

    static void send(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int color, int ticks, float power) {
        VfxSpawn.send(p.serverLevel(), shape, from, to, color, ticks, power);
    }

    /** Takes a fraction of the target's max magicule; returns how much was taken. */
    static double drainMana(LivingEntity t, double frac) {
        var ex = TensuraStorages.getExistenceFrom(t);
        if (ex == null) return 0;
        double take = Math.min(ex.getMagicule(), EnergyHelper.getMaxMagicule(t) * frac);
        ex.setMagicule(ex.getMagicule() - take);
        ex.markDirty();
        return take;
    }

    static void giveMana(LivingEntity t, double amount) {
        var ex = TensuraStorages.getExistenceFrom(t);
        if (ex == null) return;
        ex.setMagicule(Math.min(EnergyHelper.getMaxMagicule(t), ex.getMagicule() + amount));
        ex.markDirty();
    }

    static void giveManaFrac(LivingEntity t, double frac) { giveMana(t, EnergyHelper.getMaxMagicule(t) * frac); }

    /** Enemy projectiles in a box (spells, arrows); not the caster's or allies'. */
    static List<Projectile> enemyShots(ServerPlayer p, AABB box) {
        List<Projectile> out = new ArrayList<>();
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, box)) {
            if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && ally(p, o))) continue;
            out.add(pr);
        }
        return out;
    }

    static void root(LivingEntity t, int ticks) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, ticks), 9));
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        t.hurtMarked = true;
    }

    static boolean buff(ServerPlayer p, int ticks, MobEffectInstance... effects) {
        for (MobEffectInstance e : effects) p.addEffect(e);
        return true;
    }

    // ================================================================ Thread Magic (Vanessa)
    /** Arachne's Web: a web of thread spreads where you aim; everything caught in it is held fast and slowly cut. */
    public static boolean arachnesWeb(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 18);
        float r = 4 * GrimoireBook.size(i, p);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.THREAD_WEB, c, c.add(0, 1, 0), 120, r);
        boolean hold = BalanceLaw.beginControl(p);
        SpellRuntime.zone(p.serverLevel(), 120, 5, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (hold) root(t, 20);
                if (age % 20 == 0) b.hurt(i, p, t, mode, 2.5f);
            }
        });
        return true;
    }

    /** Dancing Doll: puppet threads take a foe - for 3 s it walks where you look, and a mob turns on its own side. */
    public static boolean dancingDoll(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No one to string up."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 60);
        b.castCircle(p, 0.8f);
        if (t instanceof Mob m) {
            LivingEntity victim = null;
            for (LivingEntity o : p.serverLevel().getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(10), e -> e != t && e != p && e.isAlive() && !ally(p, e)))
                if (victim == null || o.distanceToSqr(t) < victim.distanceToSqr(t)) victim = o;
            if (victim != null) m.setTarget(victim);
        }
        SpellRuntime.zone(p.serverLevel(), ticks, 1, age -> {
            if (!t.isAlive()) return;
            Vec3 want = GrimoireBook.aim(p, 12), d = want.subtract(t.position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 0.5) { Vec3 v = d.normalize().scale(0.18); t.setDeltaMovement(v.x, t.getDeltaMovement().y, v.z); t.hurtMarked = true; }
            if (age % 20 == 0) b.vfx(p, VfxShape.THREAD_STRINGS, p.getEyePosition().add(p.getViewVector(1f).scale(0.6)).add(0, -0.3, 0), t.position().add(0, t.getBbHeight() / 2, 0), 22, 1f);
        });
        return true;
    }

    /** Thread Magic: Mending: you sew wounds shut - you and allies near you heal a quarter of your health over 5 s. */
    public static boolean threadMending(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 0.8f);
        for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(8), a -> a == p || a.isAlliedTo(p))) {
            BalanceLaw.heal(ally, ally.getMaxHealth() * 0.15f);
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            ally.removeEffect(MobEffects.POISON);
            ally.removeEffect(MobEffects.WITHER);
            if (ally != p) b.vfx(p, VfxShape.THREAD_STRINGS, p.getEyePosition().add(0, -0.3, 0), ally.position().add(0, 1, 0), 30, 0.8f);
        }
        return true;
    }

    // ================================================================ Seal Magic
    /** Sealing chains: rune chains bind one foe - it cannot move or jump, and a mage cannot open their grimoire. */
    public static boolean sealingChains(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "Nothing to chain."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 70);
        root(t, 70);
        if (t instanceof Player) t.getPersistentData().putLong("nusmp_sealed_until", t.level().getGameTime() + ticks);
        b.hurt(i, p, t, mode, 5f);
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.SEAL_CHAINS, t.position().add(0, t.getBbHeight() / 2, 0), t.position(), ticks, Math.max(0.8f, t.getBbHeight() / 1.8f));
        return true;
    }

    /** Trinity Seal: three seals close on the foe and crystallise its mana - a third of it is gone, and it is sealed and weakened. */
    public static boolean trinitySeal(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null) { GrimoireBook.fail(p, "Nothing to seal."); return false; }
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.SEAL_TRINITY, t.position().add(0, t.getBbHeight() / 2, 0), t.position(), 40, Math.max(1f, t.getBbHeight() / 1.6f));
        SpellRuntime.later(p.serverLevel(), 24, () -> {
            if (!t.isAlive()) return;
            drainMana(t, BalanceLaw.isBoss(t) ? 0.1 : 0.3);
            if (t instanceof Player) t.getPersistentData().putLong("nusmp_sealed_until", t.level().getGameTime() + (BalanceLaw.isBoss(t) ? 20 : 120));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            b.hurt(i, p, t, mode, 14f);
        });
        return true;
    }

    /** Seal barrier: seal chains ring you for 8 s - enemy spells that reach them are sealed away, allies inside are harder to hurt. */
    public static boolean sealBarrier(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SEAL_CHAINS, p, p.position().add(0, 1, 0), 0, 160, 2.4f);
        SpellRuntime.zone(p.serverLevel(), 160, 2, age -> {
            for (Projectile pr : enemyShots(p, p.getBoundingBox().inflate(5))) pr.discard();
            if (age % 20 == 0) for (Player a : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(5), x -> x == p || x.isAlliedTo(p)))
                a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0));
        });
        return true;
    }

    // ================================================================ Lightning Magic
    /** Thunder Fiend (Luck Voltia): lightning wreathes your body for 30 s - very fast, and your blows arc to a second foe. */
    public static boolean thunderFiend(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.LIGHTNING_FIEND, p, p.position().add(0, 1, 0), 0, 600, 1f);
        i.getOrCreateTag().putLong("FiendUntil", p.level().getGameTime() + 600);
        com.newuniverse.nusmp.blackclover.ModeArmor.start(p, com.newuniverse.nusmp.blackclover.ModeArmor.Mode.LIGHTNING_GOD, 600);      // 0.38: Luck's Lightning God armour
        i.getOrCreateTag().putBoolean("FiendBlack", false);
        i.markDirty();
        return buff(p, 600, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2), new MobEffectInstance(MobEffects.DIG_SPEED, 600, 1));
    }

    /** Black Lightning Battle Fiend (Luck Voltia): black lightning - faster, stronger, and every blow arcs to two more foes. */
    public static boolean blackLightningFiend(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.4f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.LIGHTNING_FIEND, p, p.position().add(0, 1, 0), 0, 600, 2f);
        i.getOrCreateTag().putLong("FiendUntil", p.level().getGameTime() + 600);
        com.newuniverse.nusmp.blackclover.ModeArmor.start(p, com.newuniverse.nusmp.blackclover.ModeArmor.Mode.LIGHTNING_GOD, 600);      // 0.38: Luck's Lightning God armour
        i.getOrCreateTag().putBoolean("FiendBlack", true);
        i.markDirty();
        return buff(p, 600, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 3), new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1),
                new MobEffectInstance(MobEffects.DIG_SPEED, 600, 2));
    }

    /** Pulsaranta: three balls of lightning that hunt the target. */
    public static boolean pulsaranta(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        b.castCircle(p, 0.8f);
        for (int k = 0; k < 3; k++) {
            int delay = k * 4;
            SpellRuntime.later(p.serverLevel(), delay, () -> {
                Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
                SpellRuntime.bolt(p, start, dir.scale(1.3), 0.7, 30, false, t, (bolt, hit) -> {}, (bolt, at) -> {
                    for (LivingEntity e : GrimoireBook.around(p, at, 2)) b.hurt(i, p, e, mode, 7f);
                    b.vfx(p, VfxShape.FX_LIGHTNING_ARC, at.add(0, 3, 0), at, 0, 0.8f);
                });
                b.vfx(p, VfxShape.LIGHTNING_SPEAR, start, t != null ? t.getBoundingBox().getCenter() : start.add(dir.scale(16)), 10, 0.6f);
            });
        }
        return true;
    }

    /** God of Lightning Rising Salim: a pillar of lightning crashes down where you aim and throws everything up. */
    public static boolean risingSalim(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 26);
        float s = GrimoireBook.size(i, p);
        b.castCircle(p, 1.5f);
        b.vfx(p, VfxShape.LIGHTNING_GOD, c, c.add(0, 20, 0), 26, 1.3f * s);
        SpellRuntime.later(p.serverLevel(), 4, () -> {
            for (LivingEntity t : GrimoireBook.around(p, c, 4.5 * s)) {
                b.hurt(i, p, t, mode, 22f);
                t.setDeltaMovement(t.getDeltaMovement().add(0, 0.8, 0));
                t.hurtMarked = true;
            }
        });
        return true;
    }

    // ================================================================ Transmutation Magic (Grey)
    /** Magic Convert: a transmutation circle in front of you for 2 s - every enemy spell that touches it becomes your mana. */
    public static boolean magicConvert(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 look = p.getViewVector(1f), c = p.getEyePosition().add(look.scale(1.8));
        b.vfx(p, VfxShape.ALCHEMY_CIRCLE, c, c.add(look), 40, 1.2f);
        SpellRuntime.zone(p.serverLevel(), 40, 1, age -> {
            for (Projectile pr : enemyShots(p, new AABB(c, c).inflate(2.2))) {
                pr.discard();
                giveManaFrac(p, 0.04);
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
            }
        });
        return true;
    }

    /** Transmutation: Iron Spikes - the ground ahead is turned to iron and thrust up in a line. */
    public static boolean ironSpikes(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 a = p.position(), dir = p.getViewVector(1f).multiply(1, 0, 1).normalize();
        int n = (int) (9 * GrimoireBook.size(i, p));
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.ALCHEMY_CIRCLE, a.add(0, 0.1, 0), a.add(0, 1, 0), 20, 0.8f);
        b.vfx(p, VfxShape.STONE_SPIKES, a, a.add(dir.scale(n)), n * 2 + 24, 0.9f);
        for (int k = 1; k <= n; k++) {
            Vec3 pt = a.add(dir.scale(k));
            SpellRuntime.later(p.serverLevel(), k * 2, () -> {
                for (LivingEntity t : GrimoireBook.around(p, pt, 1.2)) { b.hurt(i, p, t, mode, 9f); t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0)); }
            });
        }
        return true;
    }

    /** Transmutation: Quagmire - the ground where you aim turns to sand for 5 s: everything in it sinks and drags. */
    public static boolean quagmire(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 20);
        float r = 4 * GrimoireBook.size(i, p);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.ALCHEMY_CIRCLE, c.add(0, 0.1, 0), c.add(0, 2, 0), 100, r / 1.6f);
        SpellRuntime.zone(p.serverLevel(), 100, 5, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
                t.setDeltaMovement(t.getDeltaMovement().multiply(0.6, 0, 0.6).add(0, -0.1, 0));
                t.hurtMarked = true;
                if (age % 20 == 0) b.hurt(i, p, t, mode, 2f);
            }
        });
        return true;
    }

    /** Grand Transmutation: every blessing on the foes around you is turned into its curse - and you take the blessings. */
    public static boolean grandTransmutation(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.5f);
        b.vfx(p, VfxShape.ALCHEMY_CIRCLE, p.position().add(0, 0.1, 0), p.position().add(0, 2, 0), 40, 3.5f);
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 8)) {
            for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) {
                if (!e.getEffect().value().isBeneficial()) continue;
                Holder<MobEffect> curse = inverse(e.getEffect());
                t.removeEffect(e.getEffect());
                t.addEffect(new MobEffectInstance(curse, Math.min(e.getDuration(), 200), Math.min(e.getAmplifier(), 2)));
                p.addEffect(new MobEffectInstance(e.getEffect(), Math.min(e.getDuration(), 200), e.getAmplifier()));
            }
            b.hurt(i, p, t, mode, 10f);
        }
        return true;
    }

    static Holder<MobEffect> inverse(Holder<MobEffect> e) {
        if (e.equals(MobEffects.MOVEMENT_SPEED)) return MobEffects.MOVEMENT_SLOWDOWN;
        if (e.equals(MobEffects.DAMAGE_BOOST)) return MobEffects.WEAKNESS;
        if (e.equals(MobEffects.REGENERATION)) return MobEffects.POISON;
        if (e.equals(MobEffects.DIG_SPEED)) return MobEffects.DIG_SLOWDOWN;
        if (e.equals(MobEffects.NIGHT_VISION) || e.equals(MobEffects.INVISIBILITY)) return MobEffects.BLINDNESS;
        if (e.equals(MobEffects.JUMP)) return MobEffects.MOVEMENT_SLOWDOWN;
        return MobEffects.WITHER;
    }

    // ================================================================ Poison Magic (Gordon)
    /** Aufwachen Dachs: a badger of poison lunges at the foe. */
    public static boolean aufwachenDachs(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.POISON_BREATH, start, start.add(dir.scale(10)), 18, 0.5f);
        SpellRuntime.bolt(p, start, dir.scale(1.3), 0.8, 18, false, GrimoireBook.target(p, 20), (bolt, t) -> {
            b.hurt(i, p, t, mode, 8f);
            t.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        }, null);
        return true;
    }

    /** Violett Schirm: a curtain of poison hangs in front of you for 8 s - spells that cross it melt, foes that touch it sicken. */
    public static boolean violettSchirm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize(), side = new Vec3(-dir.z, 0, dir.x);
        Vec3 mid = p.position().add(dir.scale(2.5)), a = mid.add(side.scale(3.5)), c = mid.subtract(side.scale(3.5));
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.POISON_CURTAIN, a, c, 160, 3.2f);
        AABB wall = new AABB(a, c.add(0, 3.5, 0)).inflate(0.8);
        SpellRuntime.zone(p.serverLevel(), 160, 2, age -> {
            for (Projectile pr : enemyShots(p, wall)) pr.discard();
            for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, wall, e -> e.isAlive() && !ally(p, e))) {
                t.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                if (age % 20 == 0) b.hurt(i, p, t, mode, 2f);
            }
        });
        return true;
    }

    /** Basilisk's Breath: a cone of venom mist - burning poison and withering. */
    public static boolean basiliskBreath(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        double len = 10 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.POISON_BREATH, eye, eye.add(look.scale(len)), 30, 1.3f);
        for (LivingEntity t : GrimoireBook.around(p, p.position(), len)) {
            if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.7) continue;
            b.hurt(i, p, t, mode, 12f);
            t.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 2));
            t.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
        }
        return true;
    }

    /** Curse-Worker's Neighbor (Gordon Agrippa): you rework curses - your side is cleansed, and the curses fall on your foes. */
    public static boolean curseWorkersNeighbor(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        List<MobEffectInstance> taken = new ArrayList<>();
        for (Player a : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(8), x -> x == p || x.isAlliedTo(p))) {
            for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects())) if (!e.getEffect().value().isBeneficial()) { taken.add(e); a.removeEffect(e.getEffect()); }
            a.getPersistentData().putLong("nusmp_misfortune_until", 0);
            a.getPersistentData().putLong("nusmp_sealed_until", 0);
        }
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 6)) for (MobEffectInstance e : taken) t.addEffect(new MobEffectInstance(e.getEffect(), Math.max(60, e.getDuration()), e.getAmplifier()));
        b.castCircle(p, 1f);
        send(p, VfxShape.ALCHEMY_CIRCLE, p.position().add(0, 0.1, 0), p.position().add(0, 2, 0), 0xFF9A40D0, 30, 2.4f);
        return true;
    }

    // ================================================================ Spatial Magic
    /** Unopening Red Room: the foe is shut in a room of folded space for 5 s; walking out of it walks back in. */
    public static boolean redRoom(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 20);
        if (t == null) { GrimoireBook.fail(p, "No one to shut in."); return false; }
        if (!GrimoireBook.control(p)) return false;
        Vec3 c = t.position();
        int ticks = BalanceLaw.controlTicks(t, 100);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.SPACE_CUBE, c, c.add(0, 1, 0), ticks, 1.5f);
        SpellRuntime.zone(p.serverLevel(), ticks, 1, age -> {
            if (t.isAlive() && t.position().distanceToSqr(c) > 1.6 * 1.6) t.teleportTo(c.x, c.y, c.z);
        });
        return true;
    }

    /** Myriad Black: space is cut over and over in front of you. */
    public static boolean myriadBlack(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        BookPage.Cast cut = TensuraShots.shot(TensuraShots.Shot.SPACE_CUT, 7, 2.4f, 0.3f, 0);
        b.castCircle(p, 1f);
        for (int k = 0; k < 5; k++) SpellRuntime.later(p.serverLevel(), k * 3, () -> cut.cast(b, i, p, mode));
        return true;
    }

    /** Door of Fate: a dimensional gate opens beside you; after 3 s you and the allies by you step through to your spawn point. */
    public static boolean doorOfFate(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize();
        Vec3 gate = p.position().add(dir.scale(2));
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.SPACE_PORTAL, gate, p.position(), 80, 1.4f);
        p.displayClientMessage(Component.literal("The Door of Fate opens. Stay close.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        SpellRuntime.later(p.serverLevel(), 60, () -> {
            if (!p.isAlive()) return;
            var spawn = p.getRespawnPosition();
            ServerLevel to = p.getServer().getLevel(p.getRespawnDimension());
            if (to == null || spawn == null) { to = p.getServer().overworld(); spawn = to.getSharedSpawnPos(); }
            List<Player> group = new ArrayList<>(p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(6), x -> x == p || x.isAlliedTo(p)));
            for (Player g : group) if (g instanceof ServerPlayer sp)
                sp.teleportTo(to, spawn.getX() + 0.5, spawn.getY() + 0.1, spawn.getZ() + 0.5, sp.getYRot(), sp.getXRot());
        });
        return true;
    }

    // ================================================================ Ash Magic (Zora)
    /** Ash Bullets: a spray of ash bullets that blind what they hit. */
    public static boolean ashBullets(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        b.castCircle(p, 0.6f);
        send(p, VfxShape.POISON_BREATH, eye, eye.add(look.scale(12)), 0xFF8A847C, 20, 0.6f);
        var rnd = p.getRandom();
        for (int k = 0; k < 6; k++) {
            Vec3 d = look.add((rnd.nextDouble() - 0.5) * 0.15, (rnd.nextDouble() - 0.5) * 0.1, (rnd.nextDouble() - 0.5) * 0.15).normalize();
            SpellRuntime.bolt(p, eye, d.scale(1.8), 0.4, 10, false, null, (bolt, t) -> {
                b.hurt(i, p, t, mode, 3.5f);
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
            }, null);
        }
        return true;
    }

    /** Ash Cloud: you vanish in a cloud of ash; everyone near you is blinded. */
    public static boolean ashCloud(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.ASH_FORMATION, p.position(), p.position().add(0, 1, 0), 60, 2.5f);
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 5)) {
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            if (t instanceof Mob m && m.getTarget() == p) m.setTarget(null);
        }
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0));
        return true;
    }

    /** Ash Absorbing Formation: a ring of ash where you aim for 8 s - it drinks the mana of every foe inside and pours it into you. */
    public static boolean ashFormation(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 20);
        float r = 4 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.ASH_FORMATION, c, c.add(0, 1, 0), 160, r);
        SpellRuntime.zone(p.serverLevel(), 160, 5, age -> {
            for (Projectile pr : enemyShots(p, new AABB(c, c).inflate(r, 3, r))) pr.discard();
            if (age % 20 == 0) for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                giveMana(p, drainMana(t, 0.03));
                t.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                b.hurt(i, p, t, mode, 1.5f);
            }
        });
        return true;
    }

    /** Revelation of the Cowardly (Zora Ideale): for 30 s, the next three foes who strike you set off an ash trap. */
    public static boolean revelationOfTheCowardly(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        i.getOrCreateTag().putLong("AshTrapUntil", p.level().getGameTime() + 600);
        i.getOrCreateTag().putInt("AshTraps", 3);
        i.markDirty();
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.ASH_FORMATION, p.position(), p.position().add(0, 1, 0), 40, 1.5f);
        // the cowardly are revealed: everything hidden nearby glows
        for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16), e -> e != p && e.isAlive()))
            if (t.isInvisible() || !ally(p, t)) t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        return true;
    }

    /** Springs one ash trap on an attacker (AshBook calls this from onTakenDamage). */
    static void ashTrap(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity attacker) {
        var tag = i.getOrCreateTag();
        if (p.level().getGameTime() > tag.getLong("AshTrapUntil") || tag.getInt("AshTraps") <= 0 || attacker == p) return;
        tag.putInt("AshTraps", tag.getInt("AshTraps") - 1);
        i.markDirty();
        b.hurt(i, p, attacker, 0, 8f);
        attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
        attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
        attacker.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        b.vfx(p, VfxShape.ASH_FORMATION, attacker.position(), attacker.position().add(0, 1, 0), 30, 1.6f);
    }

    // ================================================================ Mirror Magic (Gauche)
    /** Reflect Ray: a mirror catches the light and fires it as a piercing ray. */
    public static boolean reflectRay(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return ray(b, i, p, mode, 22, 1.1, 11f, 1f);
    }

    /** Large Reflect Ray: a great mirror, a great ray. */
    public static boolean largeReflectRay(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return ray(b, i, p, mode, 30, 2.4, 19f, 2.2f);
    }

    static boolean ray(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, double len, double width, float dmg, float size) {
        Vec3 look = p.getViewVector(1f), a = p.getEyePosition().add(look.scale(1.2)), end = a.add(look.scale(len));
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.MIRROR_RAY, a, end, 16, size);
        SpellRuntime.later(p.serverLevel(), 4, () -> { for (LivingEntity t : GrimoireBook.along(p, a, end, width)) b.hurt(i, p, t, mode, dmg); });
        return true;
    }

    /** Full Reflection: for 8 s every spell that comes at you is turned back, and blows land softer. */
    public static boolean fullReflection(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.2f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MIRROR_DOUBLE, p, p.position().add(0, 1, 0), 0xFFE8FAFF, 160, 4f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
        SpellRuntime.zone(p.serverLevel(), 160, 1, age -> {
            for (Projectile pr : enemyShots(p, p.getBoundingBox().inflate(3))) {
                pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.1));
                pr.setOwner(p);
                pr.hurtMarked = true;
            }
        });
        return true;
    }

    // ================================================================ Shadow Magic (Nacht)
    /** Dark Garden Invitation: you sink into your shadow and rise from the shadow where you aim (allies at your side come along). */
    public static boolean darkGardenInvitation(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position(), to = GrimoireBook.aim(p, 24);
        if (!p.level().noCollision(p, p.getBoundingBox().move(to.subtract(from)))) to = to.add(0, 1, 0);
        if (!p.level().noCollision(p, p.getBoundingBox().move(to.subtract(from)))) { GrimoireBook.fail(p, "No shadow there to rise from."); return false; }
        b.vfx(p, VfxShape.SHADOW_POOL, from, from.add(0, 1, 0), 30, 1.4f);
        b.vfx(p, VfxShape.SHADOW_POOL, to, to.add(0, 1, 0), 30, 1.4f);
        Vec3 dest = to;
        for (Player a : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(2.5), x -> x != p && x.isAlliedTo(p))) a.teleportTo(dest.x, dest.y, dest.z);
        p.teleportTo(dest.x, dest.y, dest.z);
        return true;
    }

    /** Kids' Playground: shadow hands rise round the place you aim and hold everything there. */
    public static boolean kidsPlayground(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 20);
        float r = 3.5f * GrimoireBook.size(i, p);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.SHADOW_POOL, c, c.add(0, 1, 0), 70, r);
        boolean hold = BalanceLaw.beginControl(p);
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            b.hurt(i, p, t, mode, 8f);
            if (hold) root(t, 70);
        }
        return true;
    }

    /** Shadow Realm: the ground becomes shadow for 5 s; foes in it are blind and slowed. */
    public static boolean shadowRealm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = p.position();
        float r = 5 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.SHADOW_POOL, c, c.add(0, 1, 0), 100, r);
        SpellRuntime.zone(p.serverLevel(), 100, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
                if (age % 20 == 0) b.hurt(i, p, t, mode, 3f);
            }
        });
        return true;
    }

    /** Heaven's Shadow Second Sight: every shadow is your eye - all living things within 32 blocks show through walls. */
    public static boolean secondSight(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 0.8f);
        for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(32), e -> e != p && e.isAlive()))
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0));
        b.vfx(p, VfxShape.SHADOW_POOL, p.position(), p.position().add(0, 1, 0), 30, 2f);
        return true;
    }

    static boolean unite(GrimoireBook b, ServerPlayer p, float size, MobEffectInstance... effects) {
        b.castCircle(p, 1.3f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SHADOW_UNITE, p, p.position().add(0, 1, 0), 0, 600, size);
        return buff(p, 600, effects);
    }

    /** Unite Mode: Canis (Nacht Faust): united with the dog devil - fast and relentless for 30 s. */
    public static boolean uniteCanis(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return unite(b, p, 1f, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2), new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0),
                new MobEffectInstance(MobEffects.JUMP, 600, 1));
    }

    /** Unite Mode: Gallus (Nacht Faust): united with the bird devil - light as a feather, leaping and gliding for 30 s. */
    public static boolean uniteGallus(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return unite(b, p, 1f, new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0), new MobEffectInstance(MobEffects.JUMP, 600, 4),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
    }

    /** Unite Mode: Canis x Felis (Nacht Faust): two devils at once - fast, strong and hard to hurt for 30 s. */
    public static boolean uniteCanisFelis(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return unite(b, p, 1.3f, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2), new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1),
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0), new MobEffectInstance(MobEffects.JUMP, 600, 1));
    }

    // ================================================================ Cotton / Food Magic (Charmy)
    /** Sleeping Sheep Strike: a cotton sheep bounds at the foe and puts it to sleep. */
    public static boolean sleepingSheepStrike(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        Vec3 from = p.position().add(p.getViewVector(1f).multiply(1, 0, 1).normalize()).add(0, 0.6, 0);
        Vec3 to = t != null ? t.position().add(0, 0.6, 0) : GrimoireBook.aim(p, 14).add(0, 0.6, 0);
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.COTTON_SHEEP, from, to, 24, 1f);
        SpellRuntime.later(p.serverLevel(), 14, () -> {
            for (LivingEntity e : GrimoireBook.around(p, to, 2.2)) {
                b.hurt(i, p, e, mode, 6f);
                int ticks = BalanceLaw.controlTicks(e, 60);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 4));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));
                e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 2));
            }
        });
        return true;
    }

    /** Sheep Cook: cotton sheep chefs cook for everyone near you - healing, filling, a lasting regeneration. */
    public static boolean sheepCook(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.COTTON_SHEEP, p.position(), p.position(), 100, 1f);
        for (Player a : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(7), x -> x == p || x.isAlliedTo(p))) {
            BalanceLaw.heal(a, a.getMaxHealth() * 0.2f);
            a.getFoodData().eat(8, 0.8f);
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        }
        return true;
    }

    /** Sheep Bondage: a cotton sheep wraps the foe in cotton and holds it. */
    public static boolean sheepBondage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No one to wrap up."); return false; }
        if (!GrimoireBook.control(p)) return false;
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.COTTON_SHEEP, p.position().add(0, 0.6, 0), t.position().add(0, 0.6, 0), 24, 1.2f);
        SpellRuntime.later(p.serverLevel(), 14, () -> { if (t.isAlive()) { root(t, 80); b.hurt(i, p, t, mode, 4f); } });
        return true;
    }

    /** Cotton Cloud: cotton lifts you and your allies - a soft rise, then a slow float down. */
    public static boolean cottonCloud(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.COTTON_SHEEP, p.position(), p.position(), 60, 0.8f);
        for (Player a : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(5), x -> x == p || x.isAlliedTo(p))) {
            a.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1));
            a.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300, 0));
        }
        return true;
    }

    /** Glutton's Banquet: a giant maw eats every spell in front of you and bites the foes there, turning it all into your mana. */
    public static boolean gluttonsBanquet(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 look = p.getViewVector(1f), c = p.getEyePosition().add(look.scale(3));
        b.castCircle(p, 1.3f);
        b.vfx(p, VfxShape.FOOD_MAW, c, c.add(look), 40, 1.2f);
        SpellRuntime.zone(p.serverLevel(), 30, 1, age -> {
            for (Projectile pr : enemyShots(p, new AABB(c, c).inflate(4))) {
                pr.discard();
                giveManaFrac(p, 0.06);
                BalanceLaw.heal(p, 2f);
            }
            if (age == 26) for (LivingEntity t : GrimoireBook.around(p, c, 3.5)) {
                b.hurt(i, p, t, mode, 12f);
                giveMana(p, drainMana(t, 0.1));
            }
        });
        return true;
    }

    /** Gourmet's Bite: a mouthful of the foe's mana. */
    public static boolean gourmetsBite(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 10);
        if (t == null) { GrimoireBook.fail(p, "Nothing to taste."); return false; }
        Vec3 c = t.getBoundingBox().getCenter();
        b.vfx(p, VfxShape.FOOD_MAW, c.subtract(p.getViewVector(1f).scale(0.6)), c, 20, 0.6f);
        b.hurt(i, p, t, mode, 8f);
        giveMana(p, drainMana(t, 0.15));
        BalanceLaw.heal(p, 3f);
        return true;
    }

    // ================================================================ Recombination Magic (Henry)
    /** Mana Corkscrew: a drill of compressed mana spirals out and bores through everything in a line. */
    public static boolean manaCorkscrew(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.8f);
        send(p, VfxShape.FIRE_SPIRAL, start, start.add(dir.scale(20)), 0xFFFF9A3C, 22, 0.9f);
        SpellRuntime.bolt(p, start, dir.scale(1.7), 0.9, 12, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 10f);
            t.knockback(0.7, -dir.x, -dir.z);
        }, null);
        return true;
    }

    /** Recombination: Bulwark - the magic house folds a ring of walls round you for 10 s. */
    public static boolean bulwark(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        BlockPos c = p.blockPosition();
        int placed = 0;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) continue;
            for (int y = 0; y < 2; y++) if (SpellRuntime.tempBlock(p.serverLevel(), c.offset(dx, y, dz), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 200)) placed++;
        }
        if (placed == 0) { GrimoireBook.fail(p, "No room for walls."); return false; }
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.RECOMBINE_CONSTRUCT, p.position(), p.position().add(0, 1, 0), 30, 1.6f);
        return true;
    }

    /** Recombination: Room Swap - two rooms of the house change places: you and the target swap positions. */
    public static boolean roomSwap(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null) { GrimoireBook.fail(p, "No one to swap with."); return false; }
        Vec3 a = p.position(), c = t.position();
        b.vfx(p, VfxShape.SPACE_CUBE, a, a.add(0, 1, 0), 20, 1f);
        b.vfx(p, VfxShape.SPACE_CUBE, c, c.add(0, 1, 0), 20, 1f);
        t.teleportTo(a.x, a.y, a.z);
        p.teleportTo(c.x, c.y, c.z);
        return true;
    }

    /** The Raging Black Bull (Henry Legolant): the magic house rebuilds itself into a fighting bull that guards you for 60 s. */
    public static boolean ragingBlackBull(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        var golem = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (golem == null) return false;
        Vec3 at = p.position().add(p.getViewVector(1f).multiply(1, 0, 1).normalize().scale(2.5));
        golem.moveTo(at.x, at.y, at.z, p.getYRot(), 0);
        golem.setPlayerCreated(true);
        golem.setCustomName(Component.literal("The Raging Black Bull").withStyle(ChatFormatting.GOLD));
        golem.getPersistentData().putUUID("nusmp_bull_owner", p.getUUID());
        level.addFreshEntity(golem);
        b.castCircle(p, 1.5f);
        VfxSpawn.sendFollowing(level, VfxShape.RECOMBINE_CONSTRUCT, golem, golem.position().add(0, 1, 0), 0, 40, 1.4f);
        SpellRuntime.zone(level, 1200, 20, age -> {
            if (!golem.isAlive() || !p.isAlive()) return;
            if (golem.distanceToSqr(p) > 24 * 24) golem.teleportTo(p.getX(), p.getY(), p.getZ());
            if (golem.getTarget() == null || !golem.getTarget().isAlive()) {
                LivingEntity foe = p.getLastHurtByMob() != null ? p.getLastHurtByMob() : p.getLastHurtMob();
                if (foe != null && foe.isAlive() && !ally(p, foe)) golem.setTarget(foe);
            }
        });
        SpellRuntime.later(level, 1200, () -> {
            if (golem.isAlive()) {
                VfxSpawn.send(level, VfxShape.RECOMBINE_CONSTRUCT, golem.position(), golem.position().add(0, 1, 0), 0, 20, 1.4f);
                golem.discard();
            }
        });
        return true;
    }

    // ================================================================ captains' character spells
    /** Calidos Brachium (Mereoleona Vermillion): arms of flame smash everything in front of you. */
    public static boolean calidosBrachium(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        b.castCircle(p, 1.4f);
        for (int s = -1; s <= 1; s += 2) {
            Vec3 side = new Vec3(-look.z, 0, look.x).normalize().scale(1.2 * s);
            b.vfx(p, VfxShape.FIRE_SPEAR, eye.add(side), eye.add(side).add(look.scale(8)), 16, 1.6f);
        }
        SpellRuntime.later(p.serverLevel(), 6, () -> {
            for (LivingEntity t : GrimoireBook.around(p, p.position(), 8)) {
                if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.5) continue;
                b.hurt(i, p, t, mode, 20f);
                t.igniteForSeconds(8);
                t.knockback(1.6, -look.x, -look.z);
                b.vfx(p, VfxShape.FIRE_BURST, t.position().add(0, 1, 0), t.position(), 18, 0.9f);
            }
        });
        return true;
    }

    /** World Tree Magic: Yggdrasil (William Vangeance): a great tree of mana - allies heal and regenerate, foes are held by its roots. */
    public static boolean yggdrasil(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.ELF_CIRCLE, p.position(), p.position().add(0, 1, 0), 60, 4f);
        b.vfx(p, VfxShape.EARTH_RISE, p.position().add(-3, 0, 0), p.position().add(3, 0, 0), 30, 1.2f);
        for (Player a : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(12), x -> x == p || x.isAlliedTo(p))) {
            BalanceLaw.heal(a, a.getMaxHealth() * 0.3f);
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 1));
        }
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 10)) root(t, 60);
        return true;
    }

    /** Briar Magic: Briar Prison (Charlotte Roselei): a prison of thorny briars grows round the place you aim. */
    public static boolean briarPrison(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 20);
        b.castCircle(p, 1.1f);
        send(p, VfxShape.THREAD_WEB, c, c.add(0, 1, 0), 0xFF3A8A3A, 100, 3.5f);         // briars, green
        send(p, VfxShape.SEAL_CHAINS, c.add(0, 1, 0), c, 0xFF4A9A40, 100, 1.4f);
        boolean hold = BalanceLaw.beginControl(p);
        SpellRuntime.zone(p.serverLevel(), 100, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, 3.5)) {
                if (hold) root(t, 20);
                if (age % 20 == 0) b.hurt(i, p, t, mode, 4f);
            }
        });
        return true;
    }

    /** Slash Magic: Ripper Cut (Jack the Ripper): blades on every limb - a storm of cuts in front of you. */
    public static boolean ripperCut(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        b.castCircle(p, 1.3f);
        for (int k = 0; k < 5; k++) {
            int n = k;
            SpellRuntime.later(p.serverLevel(), k * 3, () -> {
                double a = Math.toRadians((n - 2) * 18);
                Vec3 d = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), look.y, look.x * Math.sin(a) + look.z * Math.cos(a));
                b.vfx(p, VfxShape.WIND_SLASH, eye, eye.add(d.scale(7)), 10, 1.2f);
                for (LivingEntity t : GrimoireBook.along(p, eye, eye.add(d.scale(7)), 1.4)) b.hurt(i, p, t, mode, 7f);
            });
        }
        return true;
    }

    /** Painting Magic: Painted Menagerie (Rill Boismortier): painted beasts leap off the canvas and fight for you for 30 s. */
    public static boolean paintedMenagerie(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        b.castCircle(p, 1.3f);
        b.vfx(p, VfxShape.ALCHEMY_CIRCLE, p.position().add(0, 0.1, 0), p.position().add(0, 2, 0), 30, 2f);
        for (int k = 0; k < 3; k++) {
            var wolf = net.minecraft.world.entity.EntityType.WOLF.create(level);
            if (wolf == null) continue;
            double a = Math.PI * 2 * k / 3;
            wolf.moveTo(p.getX() + Math.cos(a) * 1.5, p.getY(), p.getZ() + Math.sin(a) * 1.5, p.getYRot(), 0);
            wolf.tame(p);
            wolf.setCustomName(Component.literal("Painted Beast").withStyle(ChatFormatting.AQUA));
            level.addFreshEntity(wolf);
            SpellRuntime.later(level, 600, () -> { if (wolf.isAlive()) wolf.discard(); });
        }
        return true;
    }

    /** Glamour World (Dorothy Unsworth): the dream world opens round you - foes dream (slow, blind, weak), allies are restored. */
    public static boolean glamourWorld(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.ELF_CIRCLE, p.position(), p.position().add(0, 1, 0), 200, 4f);
        SpellRuntime.zone(p.serverLevel(), 200, 20, age -> {
            for (LivingEntity t : GrimoireBook.around(p, p.position(), 10)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1));
            }
            for (Player a : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(10), x -> x == p || x.isAlliedTo(p)))
                a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0));
        });
        return true;
    }

    /** Vortex Magic: Vortex Shield (Kaiser Granvorka, the Kingdom's Ultimate Shield): a vortex wall round you for 10 s. */
    public static boolean vortexShield(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.4f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WIND_TORNADO, p, p.position().add(0, 1, 0), 0xFFB090E0, 200, 2.5f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
        SpellRuntime.zone(p.serverLevel(), 200, 2, age -> {
            for (Projectile pr : enemyShots(p, p.getBoundingBox().inflate(3.5))) {
                Vec3 away = pr.position().subtract(p.position()).normalize().scale(1.2);
                pr.setDeltaMovement(away);
                pr.hurtMarked = true;
            }
            for (LivingEntity t : GrimoireBook.around(p, p.position(), 3)) {
                Vec3 away = t.position().subtract(p.position()).normalize();
                t.knockback(0.6, -away.x, -away.z);
            }
        });
        return true;
    }

    /** Mercury Magic: Mercury Rain (Nozel Silva): silver rain falls where you aim for 5 s. */
    public static boolean mercuryRain(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 24);
        float r = 5 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.4f);
        var rnd = p.getRandom();
        SpellRuntime.zone(p.serverLevel(), 100, 4, age -> {
            Vec3 at = c.add((rnd.nextDouble() - 0.5) * 2 * r, 0, (rnd.nextDouble() - 0.5) * 2 * r);
            send(p, VfxShape.LIGHTNING_SPEAR, at.add(0, 12, 0), at, 0xFFD8DCE8, 6, 0.6f);
            for (LivingEntity t : GrimoireBook.around(p, at, 1.8)) {
                b.hurt(i, p, t, mode, 4f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
        });
        return true;
    }

    /** True if this entity is a Raging Black Bull and that owner made it (so it never turns on its maker). */
    public static boolean isBullOf(Entity e, Player owner) {
        return e.getPersistentData().hasUUID("nusmp_bull_owner") && e.getPersistentData().getUUID("nusmp_bull_owner").equals(owner.getUUID());
    }
}
