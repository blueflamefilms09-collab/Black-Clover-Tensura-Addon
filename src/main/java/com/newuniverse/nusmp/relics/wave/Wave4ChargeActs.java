package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Wave 4: the two charged amulets.
 * <ul>
 *   <li><b>Arcane Glass of Seeing</b>: 6 charges, 1d4+2 back at every dawn. Looking into the glass spends one and rolls a d6: a 1 or 2
 *       clouds your next blow (30 percent weaker), a 3 to 6 sharpens it (50 percent stronger), for 10 s or one blow. The last charge
 *       risks a crack (a d20 of 1): a cracked gem skips the next dawn's recovery.</li>
 *   <li><b>Pendant of the Dreamer</b>: 6 charges, 1d6 back at every midnight. Blessing of the Deep (2): Water Breathing. Features of the
 *       Ancient (3): gills and webbed hands, toggled. Force of Mhigh'Raine (3): a mind-blast on one creature you can see (mental damage,
 *       and on a failed resist Tensura fear and a 5 percent bleed of its magicules).</li>
 * </ul>
 */
final class Wave4ChargeActs {
    private Wave4ChargeActs() {}

    static final int MAX = 6;
    static final String GLASS_ID = "arcane_glass_of_seeing";
    static final String DREAMER_ID = "pendant_of_the_dreamer";
    static final int BREATH_TICKS = 12000;
    static final int GILLS_TICKS = 12000;
    static final double MIND_RANGE = 90.0;
    static final int BLUE = 0x3FA9FF;
    static final int SILVER = 0xC8D2E6;

    // ---------------------------------------------------------------- Arcane Glass of Seeing
    static void glass(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        final long now = lv.getGameTime();
        long today = Wave4Charges.dayIndex(now, Wave4Charges.DAWN_OFFSET);
        CompoundTag t = Wave4Store.read(c.stack());
        Wave4Charges.State s = Wave4Store.charges(t, Wave4Store.K_GLASS, MAX, today);
        s = Wave4Charges.refill(s, today, MAX, Wave4Charges.glassRecovery(lv.random.nextInt(4) + 1));
        if (!Wave4Charges.canSpend(s, 1)) {
            save(c.stack(), Wave4Store.K_GLASS, s);
            Fx.say(p, "The glass is dark: no charges left. It wakes at dawn.");
            return;
        }
        boolean empties = Wave4Charges.emptiesOn(s, 1);
        s = Wave4Charges.spend(s, 1);
        boolean crack = empties && Wave4Charges.cracks(lv.random.nextInt(20) + 1);
        if (crack) s = Wave4Charges.crack(s);
        save(c.stack(), Wave4Store.K_GLASS, s);
        int kind = Wave4Glass.kindOf(lv.random.nextInt(6) + 1);
        Wave4Glass.arm(p.getUUID(), kind, now);
        Fx.ring(lv, p.position().add(0, 1.4, 0), 0.8, SILVER);
        boolean keen = kind == Wave4Glass.KEEN;
        Fx.say(p, (keen ? "The glass shows you the opening: your next blow within 10 seconds lands 50% harder."
                : "The glass clouds over: your next blow within 10 seconds lands 30% weaker.") + " (" + s.charges() + "/" + MAX + " charges)");
        if (crack) Fx.say(p, "The gem cracks! It will not wake at the next dawn.");
    }

    // ---------------------------------------------------------------- Pendant of the Dreamer
    /** Blessing of the Deep: Water Breathing for a long dive (2 charges). */
    static void deep(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spend(c, 2)) return;
        Wave4Look.ambient(p, "minecraft:water_breathing", BREATH_TICKS, 0);
        Fx.ring(p.serverLevel(), p.position().add(0, 0.3, 0), 1.6, BLUE);
        Fx.say(p, "Blessing of the Deep: you breathe water for ten minutes.");
    }

    /** Features of the Ancient: gills and webbed hands, on until switched off (3 charges to open, free to close). */
    static void gills(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        CompoundTag t = Wave4Store.read(c.stack());
        if (t.getLong(Wave4Store.K_GILLS) > now) {
            Wave4Store.edit(c.stack(), x -> x.remove(Wave4Store.K_GILLS));
            Wave4Upkeep.dreamer(p);
            Fx.say(p, "Your gills close and the webbing fades.");
            return;
        }
        if (!spend(c, 3)) return;
        final long until = now + GILLS_TICKS;
        Wave4Store.edit(c.stack(), x -> x.putLong(Wave4Store.K_GILLS, until));
        Wave4Upkeep.dreamer(p);
        Fx.ring(p.serverLevel(), p.position().add(0, 0.3, 0), 1.6, BLUE);
        Fx.say(p, "Features of the Ancient: gills and webbing; you breathe water and swim as fast as you walk, for ten minutes (use again to close them).");
    }

    /** Force of Mhigh'Raine: a mind-blast on one creature you can see (3 charges). */
    static void mind(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        LivingEntity target = Wave4Look.target(p, MIND_RANGE, true, e -> true);
        if (target == null) {
            Fx.say(p, "Look at a creature you can see (within 90 blocks) to reach into its mind.");
            return;
        }
        if (!spend(c, 3)) return;
        boolean saved = lv.random.nextBoolean();                                // the Wisdom save
        float dmg = Wave4Charges.mindBlast(lv.random.nextInt(6) + 1, lv.random.nextInt(6) + 1, lv.random.nextInt(6) + 1, saved, target instanceof ServerPlayer);
        Fx.damage(p, target, dmg, Wave4Look.damageType(p, "tensura:mind_crush", "minecraft:magic"));    // a mental blow, Tensura's own kind when the game has it
        if (!saved) {
            Fx.addEffect(target, "minecraft:nausea", 100, 0);
            Fx.addEffect(target, "tensura:fear", 100, 0);                      // Tensura fear (skipped if absent)
            Nullification.bleed(target, 0.05);                                  // and a mind torn open leaks magicules: 5% of its pool, which regenerates
        }
        Fx.ring(lv, p.position().add(0, 1.5, 0), 0.8, BLUE);
        if (target.level() instanceof ServerLevel tl) Fx.burst(tl, target.position().add(0, target.getBbHeight() * 0.7, 0), BLUE);
        Fx.say(p, "Force of Mhigh'Raine: " + target.getName().getString() + (saved ? " resists; the blast is halved." : " reels from the blast, afraid and leaking magicules."));
    }

    // ---------------------------------------------------------------- the pool of charges
    /** Recovers what is owed, then spends {@code cost}; false (with a message) when the pendant holds fewer. */
    private static boolean spend(ActCtx c, int cost) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        long now = lv.getGameTime();
        long today = Wave4Charges.dayIndex(now, Wave4Charges.MIDNIGHT_OFFSET);
        CompoundTag t = Wave4Store.read(c.stack());
        Wave4Charges.State s = Wave4Store.charges(t, Wave4Store.K_DREAM, MAX, today);
        s = Wave4Charges.refill(s, today, MAX, Wave4Charges.dreamerRecovery(lv.random.nextInt(6) + 1));
        if (!Wave4Charges.canSpend(s, cost)) {
            save(c.stack(), Wave4Store.K_DREAM, s);
            Fx.say(p, "The pendant holds only " + s.charges() + " of the " + cost + " charges this needs. It regains 1d6 at midnight.");
            return false;
        }
        s = Wave4Charges.spend(s, cost);
        save(c.stack(), Wave4Store.K_DREAM, s);
        Fx.say(p, s.charges() + "/" + MAX + " charges left.");
        return true;
    }

    private static void save(ItemStack st, String prefix, Wave4Charges.State s) { Wave4Store.edit(st, x -> Wave4Store.putCharges(x, prefix, s)); }
}
