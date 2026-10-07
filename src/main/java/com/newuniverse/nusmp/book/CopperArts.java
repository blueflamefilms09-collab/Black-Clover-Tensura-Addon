package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Copper Magic: red-hot maces, current that jumps from body to body, rust, and plating that turns blows aside. */
final class CopperArts {
    private CopperArts() {}

    private static final VfxShape CAST = VfxShape.COPPER_FX1, ZONE = VfxShape.COPPER_FX2, BURST = VfxShape.COPPER_FX3;

    private static Vec3 hand(ServerPlayer p) { return p.getEyePosition().add(0, -0.35, 0); }
    private static Vec3 mid(LivingEntity t) { return t.getBoundingBox().getCenter(); }

    static void slow(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
    }

    /** Bosses keep a status for a third of the time. */
    static int dur(LivingEntity t, int base) { return BalanceLaw.isBoss(t) ? Math.max(20, base / 3) : base; }

    /** At most n of the list, so a crowd cannot make a cast cost too much. */
    static List<LivingEntity> cap(List<LivingEntity> l, int n) { return new ArrayList<>(l.subList(0, Math.min(n, l.size()))); }

    /** The current jams the target's own magic for a moment. */
    static void jam(LivingEntity t, int ticks) { EnergyBridge.effect(t, "silence", dur(t, ticks), 0); }

    // ------------------------------------------------------------------ Conduction Arc
    /** The charge leaps from the first target to the nearest foe beside it, up to five bodies, losing 15% each jump. */
    static boolean arc(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity first = GrimoireBook.target(p, 18);
        if (first == null) { GrimoireBook.fail(p, "Nothing to conduct into."); return false; }
        b.castCircle(p, 0.7f);
        List<LivingEntity> chain = new ArrayList<>();
        chain.add(first);
        double hop = 6 * GrimoireBook.size(i, p);
        while (chain.size() < 5) {
            LivingEntity last = chain.get(chain.size() - 1), best = null;
            for (LivingEntity o : GrimoireBook.around(p, mid(last), hop))
                if (!chain.contains(o) && (best == null || o.distanceToSqr(last) < best.distanceToSqr(last))) best = o;
            if (best == null) break;
            chain.add(best);
        }
        for (int k = 0; k < chain.size(); k++) {
            final LivingEntity t = chain.get(k);
            final Vec3 from = k == 0 ? hand(p) : mid(chain.get(k - 1));
            final float dmg = 7f * (float) Math.pow(0.85, k);
            SpellRuntime.later(p.serverLevel(), k * 3, () -> {
                if (!t.isAlive()) return;
                b.vfx(p, CAST, from, mid(t), 8, 0.7f);
                b.hurt(i, p, t, mode, dmg);
                slow(t, 40, 1);
                jam(t, 30);
                b.vfx(p, BURST, mid(t), mid(t), 14, 0.4f);
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ Red Shine Mace
    /** The wiki spell: a huge glowing polearm with a heavy head is slammed down on the target and the ground around it. */
    static boolean mace(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 14);
        final Vec3 c = t != null ? t.position() : GrimoireBook.aim(p, 14);
        final double r = 3.2 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.1f);
        b.vfx(p, CAST, hand(p), c.add(0, 1.2, 0), 10, 1.3f);
        SpellRuntime.later(p.serverLevel(), 9, () -> {
            b.vfx(p, BURST, c, c.add(0, 1, 0), 26, 1.3f);
            b.vfx(p, ZONE, c, c.add(0, 1, 0), 24, (float) r);
            for (LivingEntity x : cap(GrimoireBook.around(p, c, r), 10)) {
                float raw = 14f;
                raw *= EnergyBridge.armourBypass(x, p.damageSources().mobAttack(p), raw, 0.5f);   // the heavy head crushes through armour
                b.hurt(i, p, x, mode, raw);
                ElementBook.knock(0.7).apply(x, p);
                slow(x, 60, 2);
                x.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur(x, 80), 0));
                x.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Copper Wire Bind
    /** Copper wire lashes one foe and up to two beside it: they cannot move and the wire bleeds their magicules. */
    static boolean wireBind(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "Nothing to bind."); return false; }
        if (!GrimoireBook.control(p)) return false;
        List<LivingEntity> bound = new ArrayList<>();
        bound.add(t);
        for (LivingEntity o : GrimoireBook.around(p, mid(t), 4.5 * GrimoireBook.size(i, p))) {
            if (o != t && bound.size() < 3) bound.add(o);
        }
        b.castCircle(p, 0.9f);
        for (int k = 0; k < bound.size(); k++) {
            LivingEntity x = bound.get(k);
            Vec3 from = k == 0 ? hand(p) : mid(bound.get(k - 1));
            WikiSpells.root(x, 50);
            b.hurt(i, p, x, mode, 4f);
            Nullification.bleed(x, 0.02);
            b.vfx(p, CAST, from, mid(x), 40, 0.8f);
        }
        return true;
    }

    // ------------------------------------------------------------------ Verdigris Blight
    /** Green rust blooms where you aim: a burst, then a pulse every second that poisons, corrodes and strips armour. */
    static boolean verdigris(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        final Vec3 c = GrimoireBook.aim(p, 16);
        final double r = 4.5 * GrimoireBook.size(i, p);
        b.castCircle(p, 1f);
        b.vfx(p, ZONE, c, c.add(0, 1, 0), 80, (float) r);
        b.vfx(p, BURST, c, c.add(0, 1, 0), 24, 1f);
        for (LivingEntity t : cap(GrimoireBook.around(p, c, r), 10)) { b.hurt(i, p, t, mode, 5f); rust(t); }
        SpellRuntime.zone(p.serverLevel(), 80, 20, age -> {
            for (LivingEntity t : cap(GrimoireBook.around(p, c, r), 10)) { b.hurt(i, p, t, mode, 2.5f); rust(t); }
        });
        return true;
    }

    private static void rust(LivingEntity t) {
        t.addEffect(new MobEffectInstance(MobEffects.POISON, dur(t, 60), 0));
        EnergyBridge.effect(t, "corrosion", dur(t, 100), 0);
        EnergyBridge.effect(t, "fragility", dur(t, 100), 0);
    }

    // ------------------------------------------------------------------ Rail Dash
    /** Slide along a rail of charge: everything on the line is shocked, and the legs stay quick for 3 s. */
    static BookPage.Cast railDash() {
        BookPage.Cast dash = ElementBook.dash(6f, 11, false, CAST,
                ElementBook.all(ElementBook.knock(0.4), (t, p) -> { slow(t, 30, 1); jam(t, 30); }));
        return (b, i, p, mode) -> {
            if (!dash.cast(b, i, p, mode)) return false;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
            b.vfx(p, BURST, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 18, 0.7f);
            return true;
        };
    }

    // ------------------------------------------------------------------ Galvanic Edge
    /** For 15 s your melee hits carry extra current and a part of every blow arcs to the nearest other foe (see CopperBook). */
    static boolean galvanicEdge(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        var tag = i.getOrCreateTag();
        tag.putLong("EdgeUntil", p.level().getGameTime() + 300);
        tag.putInt("EdgeMode", mode);
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 0));
        b.castCircle(p, 1f);
        b.vfx(p, BURST, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 26, 0.9f);
        return true;
    }

    // ------------------------------------------------------------------ Copper Plating
    /** A skin of copper plates for 15 s: physical blows are cut by a third and the attacker is shocked (see CopperBook). */
    static boolean plating(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        i.getOrCreateTag().putLong("PlateUntil", p.level().getGameTime() + 300);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1));
        b.castCircle(p, 1.1f);
        b.vfx(p, BURST, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 28, 1f);
        return true;
    }

    // ------------------------------------------------------------------ Copper Bulwark
    /** A wall of cut copper five wide and three high that stays charged: foes who press on it are shocked and slowed. */
    static boolean bulwark(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize(), side = new Vec3(-dir.z, 0, dir.x);
        final Vec3 base = p.position().add(dir.scale(2.5));
        int placed = 0;
        for (int w = -2; w <= 2; w++) for (int h = 0; h < 3; h++)
            if (SpellRuntime.tempBlock(p.serverLevel(), BlockPos.containing(base.add(side.scale(w)).add(0, h, 0)), Blocks.CUT_COPPER.defaultBlockState(), 200)) placed++;
        if (placed == 0) { GrimoireBook.fail(p, "No room for the wall."); return false; }
        b.castCircle(p, 0.9f);
        b.vfx(p, ZONE, base, base.add(0, 1, 0), 60, 2.6f);
        b.vfx(p, BURST, base.add(0, 1, 0), base.add(0, 1, 0), 22, 0.9f);
        SpellRuntime.zone(p.serverLevel(), 200, 20, age -> {
            for (LivingEntity t : cap(GrimoireBook.around(p, base.add(0, 1, 0), 3.4), 6)) {
                b.hurt(i, p, t, mode, 2.5f);
                slow(t, 40, 1);
                b.vfx(p, CAST, base.add(0, 1.5, 0), mid(t), 6, 0.5f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Conduction Field
    /** A field of live copper at the aim point for 6 s: every second the current runs through all foes inside, linking them. */
    static boolean field(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        final Vec3 c = GrimoireBook.aim(p, 20);
        final double r = 5.5 * GrimoireBook.size(i, p);
        final int[] pulse = {0};
        b.castCircle(p, 1.2f);
        b.vfx(p, ZONE, c, c.add(0, 1, 0), 120, (float) r);
        SpellRuntime.zone(p.serverLevel(), 120, 20, age -> {
            int n = ++pulse[0];
            List<LivingEntity> ts = cap(GrimoireBook.around(p, c, r), 8);
            for (int k = 0; k < ts.size(); k++) {
                LivingEntity t = ts.get(k);
                b.hurt(i, p, t, mode, 3.5f);
                slow(t, 30, 1);
                if (n % 2 == 0) jam(t, 40);
                if (k > 0) b.vfx(p, CAST, mid(ts.get(k - 1)), mid(t), 8, 0.6f);
            }
            if (!ts.isEmpty()) b.vfx(p, BURST, c, c.add(0, 1, 0), 14, 0.7f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Copper Tempest (signature)
    /** A copper rod is driven in at the aim point and calls three bolts from the sky: foes are drawn to it, struck, linked and paralysed. */
    static boolean tempest(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        final Vec3 c = GrimoireBook.aim(p, 22);
        final double r = 7 * GrimoireBook.size(i, p);
        final boolean hold = BalanceLaw.beginControl(p);
        final int[] step = {0};
        BlockPos bp = BlockPos.containing(c);
        if (p.serverLevel().isLoaded(bp)) {
            var rod = Blocks.LIGHTNING_ROD.defaultBlockState();
            if (!SpellRuntime.tempBlock(p.serverLevel(), bp, rod, 100)) SpellRuntime.tempBlock(p.serverLevel(), bp.above(), rod, 100);
        }
        b.castCircle(p, 1.6f);
        b.vfx(p, ZONE, c, c.add(0, 1, 0), 100, (float) r);
        SpellRuntime.zone(p.serverLevel(), 100, 5, age -> {
            int n = ++step[0];
            List<LivingEntity> ts = cap(GrimoireBook.around(p, c, r), 10);
            if (n < 16) for (LivingEntity t : ts) ChainArts.pull(t, c, 0.25);
            if (n != 8 && n != 12 && n != 16) return;
            b.vfx(p, CAST, c.add(0, 16, 0), c.add(0, 1, 0), 8, 1.3f);
            b.vfx(p, BURST, c, c.add(0, 1, 0), 26, 1.4f);
            for (LivingEntity t : ts) {
                b.hurt(i, p, t, mode, 13f);
                slow(t, 40, 2);
                jam(t, 80);
                b.vfx(p, CAST, c.add(0, 1, 0), mid(t), 8, 0.6f);
                if (n == 16 && hold) EnergyBridge.effect(t, "paralysis", BalanceLaw.controlTicks(t, 40), 0);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Plating Restoration (daily)
    /** The plates are beaten flat again: you and allies within 8 blocks heal 30% of max health, gain Absorption and shed poison. */
    static boolean restoration(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.4f);
        b.vfx(p, ZONE, p.position(), p.position().add(0, 1, 0), 60, 8f);
        for (Player a : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(8),
                q -> (q == p || q.isAlliedTo(p)) && q.distanceToSqr(p) <= 64)) {
            BalanceLaw.heal(a, a.getMaxHealth() * 0.3f);
            a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 2));
            a.removeEffect(MobEffects.POISON);
            a.removeEffect(MobEffects.WITHER);
            if (a != p) b.vfx(p, CAST, p.getEyePosition(), a.position().add(0, 1, 0), 20, 0.6f);
        }
        i.getOrCreateTag().putLong("PlateUntil", p.level().getGameTime() + 300);
        b.vfx(p, BURST, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 28, 1.2f);
        return true;
    }
}
