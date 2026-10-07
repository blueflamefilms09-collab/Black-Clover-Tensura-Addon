package com.newuniverse.nusmp.seal;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Basic Sealing Magic with its random combo (0.56). One sigil = a base shape, an elemental tint and an orb count, rolled on every cast:
 * shape 0 hexagram (binds), 1 rings (silences), 2 star (drains mana); tint 0 red (burns), 1 blue (slows), 2 purple (gravity crush);
 * orbs 1..5 (longer seal). encode() packs it into one int (shape | tint << 2 | orbs << 4): the param of the SEALING_1 prop and the saved value.
 * The VFX gets inst.color = the tint colour and inst.seed = shape + 3 * (orbs - 1), as the SealSecreLayer javadoc asks.
 */
public record BasicSeal(int shape, int tint, int orbs) {
    public static final int BASE_TICKS = 100;
    public static final int[] TINT_COLORS = {0xFFFF4A3A, 0xFF4AA8FF, 0xFFB04AFF};
    private static final String[] SHAPES = {"Hexagram", "Concentric Rings", "Star"};
    private static final String[] TINTS = {"Red", "Blue", "Purple"};

    public BasicSeal {
        shape = Math.floorMod(shape, 3);
        tint = Math.floorMod(tint, 3);
        orbs = Math.max(1, Math.min(5, orbs));
    }

    public int encode() { return shape | tint << 2 | orbs << 4; }

    public static BasicSeal decode(int v) { return new BasicSeal(v & 3, (v >> 2) & 3, (v >> 4) & 7); }

    public static BasicSeal roll(RandomSource r) { return new BasicSeal(r.nextInt(3), r.nextInt(3), 1 + r.nextInt(5)); }

    public int color() { return TINT_COLORS[tint]; }

    /** The seed the VFX layer reads: shape + 3 * (orbs - 1) (0..14). */
    public long vfxSeed() { return shape + 3L * (orbs - 1); }

    public String label() { return SHAPES[shape] + ", " + TINTS[tint] + ", " + orbs + (orbs == 1 ? " orb" : " orbs"); }

    /** base * (0.6 + 0.4 * orbs) ticks; bosses get a fifth of it. */
    public int duration(boolean boss) {
        int d = (int) (BASE_TICKS * (0.6 + 0.4 * orbs));
        return boss ? Math.max(10, d / 5) : d;
    }

    // ------------------------------------------------------------------ the page
    /** "Seal Magic: Sigil Combo": rolls a combo and seals the foe in sight with it. */
    public static boolean cast(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null || t instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())) { GrimoireBook.fail(p, "Nothing to seal."); return false; }
        BasicSeal c = roll(p.getRandom());
        b.castCircle(p, 0.8f);
        p.displayClientMessage(Component.literal("Sigil: " + c.label()).withStyle(ChatFormatting.AQUA), true);
        c.apply(b, i, p, t, mode);
        return true;
    }

    /** Applies this combo to the target and shows it (lingering prop + circle VFX). */
    public void apply(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity t, int mode) {
        ServerLevel level = p.serverLevel();
        boolean boss = BalanceLaw.isBoss(t);
        int dur = duration(boss);
        long until = level.getGameTime() + dur;
        b.hurt(i, p, t, mode, 3f + orbs);
        // base shape
        switch (shape) {
            case 0 -> {                                                   // hexagram: binds
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 3));
                t.addEffect(new MobEffectInstance(MobEffects.JUMP, dur, 128, false, false));
            }
            case 1 -> {                                                   // rings: silences
                if (t instanceof Player) t.getPersistentData().putLong("nusmp_sealed_until", until);
                else t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 1));
            }
            default -> EnergyBridge.drain(t, null, (0.08 + 0.04 * orbs) * (boss ? 0.3 : 1.0));   // star: drains mana
        }
        // tint
        if (tint == 0) t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), Math.min(dur, 60)));
        if (tint == 1) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, shape == 0 ? 4 : 2));
        if (tint != 1 || shape == 2) {
            final BasicSeal self = this;
            SpellRuntime.zone(level, dur, 10, age -> {
                if (!t.isAlive() || p.isRemoved()) return;
                if (self.tint == 0 && age % 20 == 0) t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 40));
                if (self.tint == 2) {                                     // gravity crush: pulled down, hurt now and then
                    Vec3 v = t.getDeltaMovement();
                    t.setDeltaMovement(v.x * 0.5, Math.min(v.y, 0) - 0.6, v.z * 0.5);
                    t.hurtMarked = true;
                    if (age % 20 == 0) b.hurt(i, p, t, mode, 1.5f);
                }
                if (self.shape == 2 && age % 20 == 0) EnergyBridge.drain(t, null, 0.02);
            });
        }
        // visuals: the lingering prop and the circle
        Vec3 at = t.position();
        float radius = Math.max(1.2f, t.getBbWidth() * 1.3f);
        MagicProps.spawn(level, PropKind.SEALING_1, at, 0f, Math.max(1f, t.getBbWidth() * 1.5f), dur, encode(), p);
        VfxSpawn.send(level, new VfxPayload(VfxShape.SEAL_BASIC.ordinal(), at.add(0, 0.05, 0), at.add(0, 1, 0), color(), dur, radius, -1, vfxSeed()));
    }
}
