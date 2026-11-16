package com.newuniverse.nusmp.skill.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the reference skill (Tensura More Skills, Mitama Faith) is built on worshippers who make offerings to their god and a god who chooses a gift from a menu. Our Faith had neither: the only
 * source of Faith was Pray and a bestowal was one random skill. The numbers of that model (who may be recruited, how many, what an offering costs and yields, what a gift costs a worshipper, when an
 * invitation or an offering is still allowed) live here with no Minecraft class so they are unit-tested; the skill and the commands only read them.
 */
public final class FaithWorshipRules {
    private FaithWorshipRules() {}

    public static final int MIN_CAP = 1, MAX_CAP = 64;
    /** The offering sizes a worshipper can choose (Faith units); the server refuses any other number. */
    public static final int[] OFFER_STEPS = {1, 5, 10};
    public static final int MAX_WORSHIP = 1_000_000;
    /** Prayer gains at most this much extra from worshippers. */
    public static final int MAX_PRAY_BONUS = 3;
    /** One extra Faith per prayer for every this many worshippers. */
    public static final int WORSHIPPERS_PER_BONUS = 4;
    /** The most gifts one menu lists (keeps the chat message short). */
    public static final int MENU_MAX = 12;
    /** Names kept in the saved list are cut to this length. */
    public static final int NAME_MAX = 32;

    /** One worshipper as saved on the god's skill: who, the name last seen, and the Faith units offered so far. */
    public record Worshipper(UUID id, String name, int worship) {}

    public enum Recruit { OK, SELF, ALREADY, FULL }

    public static int capOf(int configured) { return Math.max(MIN_CAP, Math.min(MAX_CAP, configured)); }

    public static Recruit canRecruit(List<Worshipper> list, UUID god, UUID candidate, int cap) {
        if (god != null && god.equals(candidate)) return Recruit.SELF;
        if (indexOf(list, candidate) >= 0) return Recruit.ALREADY;
        if (list != null && list.size() >= capOf(cap)) return Recruit.FULL;
        return Recruit.OK;
    }

    public static int indexOf(List<Worshipper> list, UUID id) {
        if (list == null || id == null) return -1;
        for (int i = 0; i < list.size(); i++) if (id.equals(list.get(i).id())) return i;
        return -1;
    }

    public static boolean isWorshipper(List<Worshipper> list, UUID id) { return indexOf(list, id) >= 0; }

    /** A new list with the candidate added (the list itself when the candidate may not be recruited). */
    public static List<Worshipper> add(List<Worshipper> list, UUID god, UUID id, String name, int cap) {
        List<Worshipper> in = list == null ? new ArrayList<>() : list;
        if (canRecruit(in, god, id, cap) != Recruit.OK) return in;
        List<Worshipper> out = new ArrayList<>(in);
        out.add(new Worshipper(id, shortName(name), 0));
        return out;
    }

    public static List<Worshipper> remove(List<Worshipper> list, UUID id) {
        List<Worshipper> out = new ArrayList<>();
        if (list != null) for (Worshipper w : list) if (!w.id().equals(id)) out.add(w);
        return out;
    }

    /** A new list where the worshipper's offered units grew by {@code units} (clamped); unchanged when they are not on the list. */
    public static List<Worshipper> addWorship(List<Worshipper> list, UUID id, int units) {
        int i = indexOf(list, id);
        if (i < 0) return list == null ? new ArrayList<>() : list;
        List<Worshipper> out = new ArrayList<>(list);
        Worshipper w = out.get(i);
        long grown = (long) w.worship() + Math.max(0, units);
        out.set(i, new Worshipper(w.id(), w.name(), (int) Math.min(MAX_WORSHIP, grown)));
        return out;
    }

    public static int totalWorship(List<Worshipper> list) {
        long sum = 0;
        if (list != null) for (Worshipper w : list) sum += w.worship();
        return (int) Math.min(MAX_WORSHIP, sum);
    }

    public static String shortName(String name) {
        String n = name == null ? "" : name.trim();
        return n.length() > NAME_MAX ? n.substring(0, NAME_MAX) : n;
    }

    // ------------------------------------------------------------------ offerings
    public static boolean isOfferStep(int units) {
        for (int s : OFFER_STEPS) if (s == units) return true;
        return false;
    }

    /** Magicules an offering of {@code units} costs; 0 for a size that is not offered (the caller refuses it first). */
    public static double offerCost(int units, double perUnit) {
        return isOfferStep(units) && perUnit > 0 ? units * perUnit : 0.0;
    }

    /** True when the worshipper may make another offering now ({@code last} < 0 = never). */
    public static boolean ready(long last, long now, int gapTicks) { return last < 0 || now - last >= Math.max(0, gapTicks) || now < last; }

    // ------------------------------------------------------------------ prayer and gifts
    /** Extra Faith a prayer yields from the number of worshippers (one per four, never above {@link #MAX_PRAY_BONUS}). */
    public static int prayBonus(int worshippers) { return Math.min(MAX_PRAY_BONUS, Math.max(0, worshippers) / WORSHIPPERS_PER_BONUS); }

    /** Faith a chosen gift costs: the base price, less {@code discountPercent} for a worshipper (never below 1 unless the base is free). */
    public static int giftCost(int baseCost, boolean worshipper, int discountPercent) {
        if (baseCost <= 0) return 0;
        if (!worshipper) return baseCost;
        int pct = Math.max(0, Math.min(90, discountPercent));
        return Math.max(1, (int) ((long) baseCost * (100 - pct) / 100));
    }

    // ------------------------------------------------------------------ slow regeneration from worshippers
    /** The most periods one settlement pays (a long absence does not bank a flood). */
    public static final int MAX_TRICKLE_PERIODS = 20;

    /** Whole periods in {@code elapsed} ticks, 0..{@link #MAX_TRICKLE_PERIODS}; 0 when the period is off (<= 0). */
    public static int periods(long elapsed, int periodTicks) {
        if (periodTicks <= 0 || elapsed <= 0) return 0;
        return (int) Math.min(MAX_TRICKLE_PERIODS, elapsed / periodTicks);
    }

    /** Faith for {@code periods} periods: one per period, one more for every {@link #WORSHIPPERS_PER_BONUS} worshippers after the first; none without worshippers. */
    public static int trickleGain(int periods, int worshippers) {
        if (periods <= 0 || worshippers <= 0) return 0;
        return periods * (1 + (worshippers - 1) / WORSHIPPERS_PER_BONUS);
    }

    // ------------------------------------------------------------------ invitations
    /** An invitation made at {@code created} is still open at {@code now}; a clock that went back closes it. */
    public static boolean inviteLive(long created, long now, int ttlTicks) { return now >= created && now - created <= Math.max(0, ttlTicks); }
}
