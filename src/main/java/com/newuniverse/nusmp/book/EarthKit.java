package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.EarthConstruct;
import com.newuniverse.nusmp.prop.EarthConstructProps;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.EarthFxStyle;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.IntFunction;

/**
 * 1.93.0 The shared spell building blocks of the earth family (Sandstone, Rock, Red Ochre, Stone, Earth, Mineral): the pieces every magic's pages are made of, each one
 * parametrised by the magic (its colour, effect identity, riders), so six magics read as six magics without six copies of the same code. All damage goes through
 * {@link GrimoireBook#hurt} (balance law, cover, mastery); hard control goes through {@link GrimoireBook#control} and {@link BalanceLaw#controlTicks}; bosses are
 * handled by those two. Server side only: the client sees the effects (EarthFamilyLayer), the armour (Aura.WIKI, EarthAura) and the props (EarthPropPainter).
 */
public final class EarthKit {
    private EarthKit() {}

    public static int style(GrimoireBook b) { return EarthFxStyle.style(b.magic.name()); }

    public static void fx(GrimoireBook b, ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxSpawn.send(level, shape, from, to, EarthFxStyle.color(style(b)), ticks, power);
    }

    // ------------------------------------------------------------------ the temporary armour
    /** Forges the magic's temporary armour on the caster for 'ticks' (Aura.WIKI, the style picks the set), with Resistance and the thorns tag of the book. */
    public static void armour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int ticks, int resistAmp, float bonus) {
        PlayerAuras.set(p, Aura.WIKI, ticks, EarthFxStyle.armourStyle(style(b)));
        if (resistAmp >= 0) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, resistAmp));
        var tag = i.getOrCreateTag();
        long until = p.level().getGameTime() + ticks;
        tag.putLong("ThornsUntil", until);
        if (bonus > 0) { tag.putLong("EmpowerUntil", until); tag.putFloat("EmpowerBonus", bonus); }
        i.markDirty();
    }

    /** The short armour after a big cast (the brief's five seconds): only when no longer armour is already on. */
    public static void flash(GrimoireBook b, ServerPlayer p) {
        if (PlayerAuras.active(p, Aura.WIKI)) return;
        PlayerAuras.set(p, Aura.WIKI, 100, EarthFxStyle.armourStyle(style(b)));
    }

    /** A guard page: forged armour for 'ticks', costs a share of aura (physical hardening), then the burst effect. */
    public static BookPage.Cast guard(int ticks, int resistAmp, float bonus, double auraFrac) {
        return (b, i, p, mode) -> {
            if (!EnergyBridge.aura(p, auraFrac, 40)) { GrimoireBook.fail(p, "Not enough aura to harden your body."); return false; }
            int t = (int) (ticks * Math.min(1.5, 1 + 0.04 * (BalanceLaw.epScale(p) - 1)));
            armour(b, i, p, t, resistAmp, bonus);
            b.castCircle(p, 1.2f);
            fx(b, p.serverLevel(), VfxShape.EARTHFAM_BURST, p.position(), p.position().add(0, 1, 0), 26, 1.3f);
            return true;
        };
    }

    // ------------------------------------------------------------------ constructs
    /** Ends the caster's earlier constructs of a type (a new cast replaces them). */
    public static void dismiss(ServerPlayer p, int type) {
        for (MagicPropEntity e : p.serverLevel().getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(96),
                x -> x.kind() == PropKind.EARTH_CONSTRUCT && EarthConstruct.type(x.param()) == type && p.getUUID().equals(x.ownerId()))) e.expire();
    }

    /** A guardian page: raises a construct of the type at the caster's side (it follows and defends for 25 s, longer for a mighty caster). */
    public static BookPage.Cast guardian(int type, int baseTicks, int count) {
        return (b, i, p, mode) -> {
            ServerLevel level = p.serverLevel();
            Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
            look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
            Vec3 side = new Vec3(-look.z, 0, look.x);
            dismiss(p, type);
            int life = EarthConstructProps.lifeFor(p, baseTicks);
            float s = EarthConstructProps.sizeFor(p) * GrimoireBook.size(i, p);
            int made = 0;
            for (int k = 0; k < count; k++) {
                Vec3 at = p.position().add(look.scale(2.0)).add(side.scale(1.2 * (k % 2 == 0 ? 1 : -1) * (1 + k / 2)));
                if (EarthConstructProps.spawn(level, type, at, p.getYRot() + 180f, Math.min(1.6f, s), life, p) != null) made++;
            }
            if (made == 0) { GrimoireBook.fail(p, "The earth would not take form."); return false; }
            b.castCircle(p, 1.2f);
            flash(b, p);
            return true;
        };
    }

    /** A construct raised where you aim (a totem, a spire): at most 'cap' of the type stand at once. */
    public static BookPage.Cast planted(int type, int baseTicks, float scaleMul, int cap) {
        return (b, i, p, mode) -> {
            ServerLevel level = p.serverLevel();
            Vec3 at = GrimoireBook.aim(p, 18);
            List<MagicPropEntity> mine = level.getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(96),
                    x -> x.kind() == PropKind.EARTH_CONSTRUCT && EarthConstruct.type(x.param()) == type && p.getUUID().equals(x.ownerId()));
            if (mine.size() >= cap) mine.get(0).expire();
            float s = Math.min(1.8f, EarthConstructProps.sizeFor(p) * scaleMul * GrimoireBook.size(i, p));
            if (EarthConstructProps.spawn(level, type, at, p.getYRot() + 180f, s, EarthConstructProps.lifeFor(p, baseTicks), p) == null) {
                GrimoireBook.fail(p, "The earth would not take form.");
                return false;
            }
            b.castCircle(p, 1f);
            return true;
        };
    }

    // ------------------------------------------------------------------ thrown bodies
    /** One thrown construct body (a boulder, a crystal lance) leaves the caster's hand along 'dir' (a unit vector). */
    public static MagicPropEntity throwBody(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int type, Vec3 dir, double speed, float scale, int life) {
        Vec3 start = p.getEyePosition().add(dir.scale(1.0)).add(0, -0.35, 0);
        MagicPropEntity e = EarthConstructProps.spawn(p.serverLevel(), type, start, p.getYRot(), scale, life, p);
        if (e != null) { e.setDeltaMovement(dir.scale(speed)); e.setYRot((float) (Math.atan2(dir.z, dir.x) * 57.29577951308232) - 90f); }
        return e;
    }

    /** A fan of n thrown bodies (spread in degrees, across the whole fan). */
    public static BookPage.Cast thrown(int type, int n, double spreadDeg, double speed, float scale, int life) {
        return (b, i, p, mode) -> {
            b.castCircle(p, n > 1 ? 1f : 0.7f);
            Vec3 dir = p.getViewVector(1f);
            float s = scale * Math.min(1.5f, 0.9f + 0.04f * (float) Math.min(8.0, BalanceLaw.epScale(p) - 1.0)) * GrimoireBook.size(i, p);
            int made = 0;
            for (int k = 0; k < n; k++) {
                double off = n == 1 ? 0 : (k - (n - 1) / 2.0) * Math.toRadians(spreadDeg) / (n - 1);
                Vec3 side = new Vec3(-dir.z, 0, dir.x).normalize();
                Vec3 d = dir.add(side.scale(Math.tan(off))).normalize();
                if (throwBody(b, i, p, type, d, speed, Math.min(1.8f, s), life) != null) made++;
            }
            if (made == 0) { GrimoireBook.fail(p, "The earth would not take form."); return false; }
            fx(b, p.serverLevel(), VfxShape.EARTHFAM_CAST, p.getEyePosition(), p.getEyePosition().add(dir.scale(3)), 8, 0.5f);
            return true;
        };
    }

    // ------------------------------------------------------------------ area spells
    /** A burst round you (or at the aim point): damage, a rider on every hit, the magic's burst effect. */
    public static BookPage.Cast nova(float dmg, double radius, boolean atAim, int maxHits, ElementBook.Rider rider, boolean armourFlash) {
        return (b, i, p, mode) -> {
            Vec3 c = atAim ? GrimoireBook.aim(p, 24) : p.position();
            double r = radius * GrimoireBook.size(i, p);
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (n++ >= maxHits) break;
                b.hurt(i, p, t, mode, dmg);
                rider.apply(t, p);
            }
            b.castCircle(p, 1f);
            fx(b, p.serverLevel(), VfxShape.EARTHFAM_BURST, c, c.add(0, 1, 0), 28, (float) (r / 3));
            fx(b, p.serverLevel(), VfxShape.EARTHFAM_ZONE, c, c.add(0, 1, 0), 20, (float) Math.min(r, 8) * 0.8f);
            if (armourFlash) flash(b, p);
            return true;
        };
    }

    /** A lingering field: every 'interval' ticks hits everything inside, with the magic's zone effect for the whole time. */
    public static BookPage.Cast field(float dmg, double radius, int ticks, int interval, boolean atAim, ElementBook.Rider rider) {
        return (b, i, p, mode) -> {
            Vec3 c = atAim ? GrimoireBook.aim(p, 24) : p.position();
            double r = radius * GrimoireBook.size(i, p);
            ServerLevel level = p.serverLevel();
            b.castCircle(p, 1f);
            fx(b, level, VfxShape.EARTHFAM_ZONE, c.add(0, 0.05, 0), c.add(0, 1, 0), ticks, (float) r);
            SpellRuntime.zone(level, ticks, interval, age -> {
                Vec3 at = atAim ? c : p.position();
                int n = 0;
                for (LivingEntity t : GrimoireBook.around(p, at, r)) {
                    if (n++ >= 10) break;
                    b.hurt(i, p, t, mode, dmg);
                    rider.apply(t, p);
                }
                fx(b, level, VfxShape.EARTHFAM_BURST, at, at.add(0, 1, 0), 14, (float) r * 0.2f);
            });
            return true;
        };
    }

    /** The ground erupts one burst after another along your line of sight (flat), each hit with a rider; the magic's burst effect at every other point. */
    public static BookPage.Cast eruption(int steps, float dmg, double lift, double width, ElementBook.Rider rider) {
        return (b, i, p, mode) -> {
            Vec3 a = p.position(), dir = p.getViewVector(1f).multiply(1, 0, 1);
            dir = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
            final Vec3 d = dir;
            int n = (int) Math.max(4, steps * GrimoireBook.size(i, p));
            ServerLevel level = p.serverLevel();
            b.castCircle(p, 0.9f);
            for (int k = 1; k <= n; k++) {
                final int kk = k;
                Vec3 pt = a.add(d.scale(k * 1.2));
                SpellRuntime.later(level, k * 2, () -> {
                    for (LivingEntity t : GrimoireBook.around(p, pt, width)) {
                        b.hurt(i, p, t, mode, dmg);
                        if (lift > 0) { t.setDeltaMovement(t.getDeltaMovement().add(0, BalanceLaw.isBoss(t) ? lift * 0.3 : lift, 0)); t.hurtMarked = true; }
                        rider.apply(t, p);
                    }
                    if (kk % 2 == 1) fx(b, level, VfxShape.EARTHFAM_BURST, pt, pt.add(0, 1, 0), 16, 0.55f);
                });
            }
            return true;
        };
    }

    /** An instant strike along the line of sight (a brush stroke, a slash of grit). */
    public static BookPage.Cast stroke(float dmg, double length, double width, ElementBook.Rider rider) {
        return (b, i, p, mode) -> {
            Vec3 a = p.getEyePosition(), end = a.add(p.getViewVector(1f).scale(length * GrimoireBook.size(i, p)));
            int n = 0;
            for (LivingEntity t : GrimoireBook.along(p, a, end, width)) {
                if (n++ >= 10) break;
                b.hurt(i, p, t, mode, dmg);
                rider.apply(t, p);
            }
            b.castCircle(p, 0.8f);
            fx(b, p.serverLevel(), VfxShape.EARTHFAM_CAST, a.add(0, -0.2, 0), end, 14, 1.1f);
            return true;
        };
    }

    /** A temporary wall in front of the caster (self-restoring blocks), with the magic's burst effect along its length. */
    public static BookPage.Cast wall(BlockState state, int wide, int high, int ticks) {
        return (b, i, p, mode) -> {
            Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1);
            dir = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
            Vec3 side = new Vec3(-dir.z, 0, dir.x), base = p.position().add(dir.scale(2.5));
            int placed = 0;
            for (int w = -wide / 2; w < wide - wide / 2; w++) for (int h = 0; h < high; h++)
                if (SpellRuntime.tempBlock(p.serverLevel(), BlockPos.containing(base.add(side.scale(w + 0.5)).add(0, h, 0)), state, ticks)) placed++;
            if (placed == 0) { GrimoireBook.fail(p, "There is no room to raise a wall."); return false; }
            b.castCircle(p, 0.8f);
            for (int k = -1; k <= 1; k++) fx(b, p.serverLevel(), VfxShape.EARTHFAM_BURST, base.add(side.scale(k * wide * 0.3)), base.add(side.scale(k * wide * 0.3)).add(0, 1.5, 0), 18, 0.7f);
            return true;
        };
    }

    // ------------------------------------------------------------------ riders
    public static ElementBook.Rider slow(int ticks, int amp) { return (t, p) -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(t) ? ticks / 2 : ticks, amp)); }

    public static ElementBook.Rider blind(int ticks) { return (t, p) -> t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.isBoss(t) ? ticks / 3 : ticks, 0)); }

    public static ElementBook.Rider weaken(int ticks, int amp) { return (t, p) -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BalanceLaw.isBoss(t) ? ticks / 2 : ticks, amp)); }

    public static ElementBook.Rider mark(int ticks) { return (t, p) -> t.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0)); }

    public static ElementBook.Rider knock(double force) {
        return (t, p) -> {
            Vec3 d = t.position().subtract(p.position()).multiply(1, 0, 1);
            if (d.lengthSqr() < 1e-4) return;
            d = d.normalize();
            t.knockback(BalanceLaw.isBoss(t) ? force * 0.25 : force, -d.x, -d.z);
        };
    }

    /** Magic Jamming (Tensura's silence): the dressed stone seals the target's casting; shortened on bosses and by the control diminishing returns. Gate it with beginControl. */
    public static ElementBook.Rider jam(int ticks) { return (t, p) -> EnergyBridge.effect(t, "silence", BalanceLaw.controlTicks(t, ticks), 0); }

    /** Energy Drain: the crystal takes a share of the target's magicules and hands it to the caster. Gate it with beginControl. */
    public static ElementBook.Rider siphon(double frac) { return (t, p) -> EnergyBridge.drain(t, p, frac); }

    public static ElementBook.Rider all(ElementBook.Rider... rs) { return ElementBook.all(rs); }

    /** Spreads a count over the EP: 'base' at low EP, up to base + extra at the cap. */
    public static int scaled(LivingEntity e, int base, int extra) { return base + (int) Math.round(extra * Math.min(1.0, (BalanceLaw.epScale(e) - 1.0) / 8.0)); }

    /** Raises 'count' pages from a factory (used by the book builders). */
    public static List<BookPage> pages(int n, IntFunction<BookPage> f) {
        BookPage[] a = new BookPage[n];
        for (int k = 0; k < n; k++) a[k] = f.apply(k);
        return List.of(a);
    }
}
