package com.newuniverse.nusmp.heroweapon.weapon.claw;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Strike;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: a wound of the Claw goes on hurting after the claw is gone. The count of wounds and the tick they close ({@link BleedMath}) are kept in the foe's own saved data, so a
 * foe that unloads or dies takes nothing with it; the damage itself is a short chain of delayed pulses, one a second, that starts when the first wound opens, runs while any
 * wound is open, and never longer than {@link BleedMath#CHAIN_MAX} ticks (the owner's rule: no repeating task beyond 20 seconds). Each pulse is a hit through the balance law
 * in the name of the wielder who opened the wound, without the shove of a blow; it stops when the foe dies, the wounds close or the wielder is gone. Armour does reduce it
 * (the balance law has one damage kind for the weapons), which is why the Claw's Shredding Rush takes armour off.
 */
final class ClawBleed {
    private ClawBleed() {}

    private static final String STACKS = "nusmp_cl_bn", UNTIL = "nusmp_cl_bu", CHAIN = "nusmp_cl_bc";
    /** The dark red of the dust a pulse leaves. */
    static final int BLOOD = 0xB01020;

    static BleedMath.State read(LivingEntity t) {
        CompoundTag d = t.getPersistentData();
        return d.contains(UNTIL) ? new BleedMath.State(d.getInt(STACKS), d.getLong(UNTIL)) : BleedMath.State.EMPTY;
    }

    private static void write(LivingEntity t, BleedMath.State s) {
        CompoundTag d = t.getPersistentData();
        d.putInt(STACKS, s.stacks());
        d.putLong(UNTIL, s.until());
    }

    /** The wounds open on a foe right now. */
    static int stacks(LivingEntity t) { return BleedMath.stacks(read(t), t.level().getGameTime()); }

    /** Closes every wound of a foe (a Hemorrhage spends them). */
    static void clear(LivingEntity t) { write(t, BleedMath.State.EMPTY); }

    /** Opens {@code add} wounds on a foe (more of them and for longer with the Dragon Claws) and starts the pulses if none are running. */
    static void add(HeroCtx c, LivingEntity t, int add) {
        if (t == null || !t.isAlive() || add <= 0) return;
        long now = c.level.getGameTime();
        boolean fangs = c.form.has(ClawNumbers.DRAGON_FANGS);
        write(t, BleedMath.add(read(t), add, fangs ? BleedMath.MAX_STACKS_FANGS : BleedMath.MAX_STACKS, now, fangs ? BleedMath.TICKS_FANGS : BleedMath.TICKS));
        CompoundTag d = t.getPersistentData();
        if (d.getLong(CHAIN) <= now) {                                       // no chain of pulses on this foe: start one
            d.putLong(CHAIN, now + BleedMath.CHAIN_MAX);
            schedule(c.player, t, now);
        }
    }

    private static void schedule(ServerPlayer owner, LivingEntity t, long start) {
        SpellRuntime.later(owner.serverLevel(), BleedMath.INTERVAL, () -> pulse(owner, t, start));
    }

    private static void pulse(ServerPlayer owner, LivingEntity t, long start) {
        CompoundTag d = t.getPersistentData();
        long now = t.level().getGameTime();
        int stacks = BleedMath.stacks(read(t), now);
        HeroCtx cx = !t.isAlive() || BleedMath.chainOver(start, now) || stacks <= 0 || owner.isRemoved() || !owner.isAlive() || owner.level() != t.level() ? null : HeroCtx.of(owner);
        if (cx == null) {                                                    // the foe died, the wounds closed, the chain ran out, or the wielder is gone
            d.putLong(CHAIN, 0L);
            return;
        }
        Strike.hitStill(cx, t, BleedMath.pulse(stacks));
        StrikeFx.burst(cx.level, Strike.center(t), BLOOD, 3);
        if (t.isAlive()) schedule(owner, t, start);
        else d.putLong(CHAIN, 0L);
    }
}
