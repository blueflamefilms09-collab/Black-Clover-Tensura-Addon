package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Map;

/**
 * Why: the curse of a wielder is saved in the bond's open area for this system, {@code HeroBond.extra("curse")}, so it travels with the bond (kept through death,
 * wiped with the bond) and no other class has to change to save it. This class is the only reader and writer of that compound: it turns it into a
 * {@link CurseState} for the pure machine and back. Every key is optional on read and clamped (an old save, a save edited by hand or a damaged one loads as
 * "nothing"), and nothing is created for a bond that never had a curse. The key {@link #SERIES} is the agreement with the forms system: it holds the sin ids of the
 * Curse Series the wielder has embraced, as a list of strings, and the forms system refuses a curse form whose sin is not in it. The few cheap readers
 * ({@link #corruption}, {@link #despair}, {@link #overrunUntil}) read one number without building a state, for the calls that run every tick.
 */
final class CurseStore {
    private CurseStore() {}

    /** The name of the compound inside {@code HeroBond.extra}. */
    static final String KEY = "curse";
    /** The list of embraced sin ids (strings): read by the forms system. */
    static final String SERIES = "series";
    static final String DESPAIR = "despair", CORRUPTION = "corruption", OFFERED = "offered", OFFERED_AT = "offeredAt", NEXT_OFFER_AT = "nextOfferAt";
    static final String OVERRUN_UNTIL = "overrunUntil", WEAK_UNTIL = "weakUntil", TAX = "tax", REFUNDS = "refunds", LAST_GAIN = "lastGain", LAST_REST = "lastRest", CAUSES = "causes";

    private static int clampMeter(int v) { return Math.max(0, Math.min(CurseTuning.MAX, v)); }

    /** The state saved in the bond; a fresh default one when the bond has none. Never null, never throws. */
    static CurseState load(HeroBond b) {
        CurseState s = new CurseState();
        if (b == null || !b.hasExtra(KEY)) return s;
        CompoundTag t = b.extra(KEY);
        s.despair = clampMeter(t.getInt(DESPAIR));
        s.corruption = clampMeter(t.getInt(CORRUPTION));
        if (t.contains(SERIES, Tag.TAG_LIST)) {
            ListTag list = t.getList(SERIES, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                Sin sin = Sin.byId(list.getString(i));
                if (sin != null) s.series.add(sin.id());
            }
        }
        String offered = t.getString(OFFERED);
        s.offered = Sin.byId(offered) == null ? "" : offered;
        s.offeredAt = s.offered.isEmpty() ? 0L : t.getLong(OFFERED_AT);
        s.nextOfferAt = t.getLong(NEXT_OFFER_AT);
        s.overrunUntil = Math.max(0L, t.getLong(OVERRUN_UNTIL));
        s.weakUntil = Math.max(0L, t.getLong(WEAK_UNTIL));
        s.tax = Math.max(0, Math.min(99, t.getInt(TAX)));
        s.refunds = Math.max(0, t.getInt(REFUNDS));
        s.lastGain = t.getLong(LAST_GAIN);
        s.lastRest = t.getLong(LAST_REST);
        if (t.contains(CAUSES, Tag.TAG_COMPOUND)) {
            CompoundTag causes = t.getCompound(CAUSES);
            for (String k : causes.getAllKeys()) s.causeAt.put(k, causes.getLong(k));
        }
        return s;
    }

    /** Writes the state into the bond. Nothing is written for a default state of a bond that has no record yet. */
    static void save(HeroBond b, CurseState s) {
        if (b == null || !b.isBound()) return;
        if (s.isDefault() && !b.hasExtra(KEY)) return;
        CompoundTag t = b.extra(KEY);
        t.putInt(DESPAIR, s.despair);
        t.putInt(CORRUPTION, s.corruption);
        ListTag list = new ListTag();
        for (String id : s.series) list.add(StringTag.valueOf(id));
        t.put(SERIES, list);
        t.putString(OFFERED, s.offered);
        t.putLong(OFFERED_AT, s.offeredAt);
        t.putLong(NEXT_OFFER_AT, s.nextOfferAt);
        t.putLong(OVERRUN_UNTIL, s.overrunUntil);
        t.putLong(WEAK_UNTIL, s.weakUntil);
        t.putInt(TAX, s.tax);
        t.putInt(REFUNDS, s.refunds);
        t.putLong(LAST_GAIN, s.lastGain);
        t.putLong(LAST_REST, s.lastRest);
        CompoundTag causes = new CompoundTag();
        for (Map.Entry<String, Long> e : s.causeAt.entrySet()) causes.putLong(e.getKey(), e.getValue());
        t.put(CAUSES, causes);
    }

    /** The corruption saved in the bond, 0 when there is none. */
    static int corruption(HeroBond b) { return b != null && b.hasExtra(KEY) ? clampMeter(b.extra(KEY).getInt(CORRUPTION)) : 0; }

    /** The despair saved in the bond, 0 when there is none. */
    static int despair(HeroBond b) { return b != null && b.hasExtra(KEY) ? clampMeter(b.extra(KEY).getInt(DESPAIR)) : 0; }

    /** The sin id of the offer saved in the bond, empty when none is open (the offer may have run out and not been closed yet: the look closes it). */
    static String offered(HeroBond b) { return b != null && b.hasExtra(KEY) ? b.extra(KEY).getString(OFFERED) : ""; }

    /** The game time the Overrun ends at, 0 when none runs. */
    static long overrunUntil(HeroBond b) { return b != null && b.hasExtra(KEY) ? Math.max(0L, b.extra(KEY).getLong(OVERRUN_UNTIL)) : 0L; }

    /** True when the bond has embraced the series of this sin. */
    static boolean hasSeries(HeroBond b, Sin sin) {
        if (b == null || sin == null || !b.hasExtra(KEY)) return false;
        CompoundTag t = b.extra(KEY);
        if (!t.contains(SERIES, Tag.TAG_LIST)) return false;
        ListTag list = t.getList(SERIES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) if (sin.id().equals(list.getString(i))) return true;
        return false;
    }

    /** The hearts of tax saved in the bond. */
    static int tax(HeroBond b) { return b != null && b.hasExtra(KEY) ? Math.max(0, Math.min(99, b.extra(KEY).getInt(TAX))) : 0; }
}
