package com.newuniverse.nusmp.heroweapon.weapon.sewingset;

import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Kind;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Row;

import java.util.List;
import java.util.Set;

/**
 * Why: every number the Vassal Sewing Set casts with lives in this one pure table, so the balance budget of design.md section 5 can be pinned by a test and a later
 * edit that breaks it fails the build. The logic builds its skills from the {@link Row}s (so the table and the game cannot differ) and reads every other constant from
 * here. Raw damage is before the level factor (1.0 to 2.2) and the balance law. Design, not canon: the figures are the author's; the technique names are the wiki's
 * (Needle Shot, Bind Wire, Jumping Needle, Scissor Cut, Spider Net, Ariadne's Thread).
 */
public final class SewingNumbers {
    private SewingNumbers() {}

    // ------------------------------------------------------------------ the budget rows (one per skill)
    public static final Row NEEDLE_SHOT = Row.damage("sewing_set.needle_shot", Kind.ORDINARY, 9, 9, 3, 60, 0.01);
    public static final Row BIND_WIRE = Row.damage("sewing_set.bind_wire", Kind.UTILITY, 3, 3, 1, 240, 0.02);
    public static final Row JUMPING_NEEDLE = Row.utility("sewing_set.jumping_needle", 140, 0.02);
    public static final Row STITCH_MEND = Row.utility("sewing_set.stitch_mend", 200, 0.02).withHeal(0.08).reaching(4);
    public static final Row SCISSOR_CUT = Row.damage("sewing_set.scissor_cut", Kind.HEAVY, 13, 13, 5, 140, 0.02);
    public static final Row STITCHED_BARRIER = Row.damage("sewing_set.stitched_barrier", Kind.UTILITY, 3, 3, 8, 180, 0.02);
    public static final Row SPIDER_NET = Row.damage("sewing_set.spider_net", Kind.UTILITY, 4, 4, 3, 260, 0.025);
    public static final Row ARIADNE = Row.damage("sewing_set.ariadnes_thread", Kind.ULTIMATE, 0, 0, 4, 700, 0.06);

    public static final List<Row> ALL = List.of(NEEDLE_SHOT, BIND_WIRE, JUMPING_NEEDLE, STITCH_MEND, SCISSOR_CUT, STITCHED_BARRIER, SPIDER_NET, ARIADNE);

    /** The ids of the ultimates (one per form at most). */
    public static final Set<String> ULTIMATES = Set.of(ARIADNE.id());

    // ------------------------------------------------------------------ Needle Shot and the marks
    public static final int NEEDLES = 3;
    public static final double NEEDLE_RAW = 3.0, NEEDLE_RANGE = 20.0, NEEDLE_SPREAD = 4.0, NEEDLE_WIDTH = 0.25;
    /** How long a stitch mark in a foe, a beacon in an ally and a needle in the ground last (ticks); Fine Stitch adds {@code FINE_EXTRA} to the first. */
    public static final int FOE_MARK_TICKS = 160, ALLY_MARK_TICKS = 400, PLACE_TICKS = 400, FINE_EXTRA = 80;

    // ------------------------------------------------------------------ Bind Wire
    public static final double BIND_RANGE = 16.0;
    public static final int BIND_TICKS = 60;

    // ------------------------------------------------------------------ Jumping Needle
    public static final double JUMP_RANGE = 48.0, JUMP_LAND = 1.6;

    // ------------------------------------------------------------------ Stitch Mend
    public static final double MEND_RADIUS = 6.0, MEND_REACH = 30.0, MEND_SHARE = 0.08;
    public static final int MEND_CURES = 1;

    // ------------------------------------------------------------------ Scissor Cut
    public static final double CUT_BASE = 10.0, CUT_MARKED = 1.3, CUT_RADIUS = 3.0, CUT_RANGE = 10.0;
    public static final int CUT_TELL = 8;

    // ------------------------------------------------------------------ Stitched Barrier
    public static final double BARRIER_HALF = 4.5, BARRIER_DISTANCE = 3.5, BARRIER_TOUCH = 1.0, BARRIER_PRICK = 3.0;
    public static final int BARRIER_TICKS = 120, BARRIER_CATCHES = 6, BARRIER_SLOW_TICKS = 40;

    // ------------------------------------------------------------------ Spider Net
    public static final double NET_RANGE = 14.0, NET_RADIUS = 2.8;
    public static final int NET_TICKS = 80;

    // ------------------------------------------------------------------ Ariadne's Thread
    public static final double ARIADNE_REACH = 40.0, ARIADNE_REVEAL = 24.0, ARIADNE_LAND = 2.4;
    public static final int ARIADNE_TELL = 30, ARIADNE_PICKS = 4, ARIADNE_GLOW = 160, ARIADNE_FOES = 12;

    // ------------------------------------------------------------------ small pure rules
    /** What a Scissor Cut is worth: ten, 30 percent more (thirteen) on a foe with a stitch mark. */
    public static double cutDamage(boolean marked) {
        return marked ? CUT_BASE * CUT_MARKED : CUT_BASE;
    }

    /** The damage one foe takes from a volley when {@code needles} of them hit it: three each, at most the row's nine. */
    public static double volleyDamage(int needles) {
        return NEEDLE_RAW * Math.max(0, Math.min(NEEDLES, needles));
    }

    /** How long a stitch mark in a foe lasts. */
    public static int foeMarkTicks(boolean fineStitch) {
        return fineStitch ? FOE_MARK_TICKS + FINE_EXTRA : FOE_MARK_TICKS;
    }
}
