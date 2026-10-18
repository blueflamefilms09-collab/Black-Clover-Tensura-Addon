package com.newuniverse.nusmp.heroweapon.weapon.chain;

import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Kind;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Row;

import java.util.List;
import java.util.Set;

/**
 * Why: every number the Vassal Chain casts with lives in this one pure table, so the balance budget of design.md section 5 can be pinned by a test and a later edit
 * that breaks it fails the build. The logic builds its skills from the {@link Row}s (so the table and the game cannot differ) and reads every other constant from here.
 * Raw damage is before the level factor (1.0 to 2.2) and the balance law. Design, not canon: the figures are the author's; the technique names are the wiki's (Chain
 * Binding, Chain Defence, Hydra, Binding: Multi-Headed Orochi). The Sloth series (a design choice: canon ties Sloth to the Hunting Tool, not to the Chain) has three
 * techniques of its own.
 */
public final class ChainNumbers {
    private ChainNumbers() {}

    // ------------------------------------------------------------------ the budget rows (one per skill)
    public static final Row LASH = Row.damage("chain.chain_lash", Kind.ORDINARY, 6, 6, 4, 60, 0.01);
    public static final Row PULL = Row.damage("chain.chain_pull", Kind.UTILITY, 4, 4, 1, 160, 0.02);
    public static final Row BINDING = Row.damage("chain.chain_binding", Kind.UTILITY, 3, 3, 1, 240, 0.02);
    public static final Row HYDRA = Row.damage("chain.hydra", Kind.ORDINARY, 7, 7, 8, 140, 0.02);
    public static final Row DEFENCE = Row.utility("chain.chain_defence", 240, 0.02).reaching(4);
    public static final Row GRAPPLE = Row.damage("chain.grapple", Kind.ORDINARY, 6, 6, 1, 180, 0.02);
    public static final Row OROCHI = Row.damage("chain.orochi", Kind.ULTIMATE, 8, 16, 4, 700, 0.06);
    public static final Row SLOTH_WEIGHT = Row.utility("chain.sloth_weight", 260, 0.03).reaching(8);
    public static final Row SLOTH_SNAP = Row.damage("chain.sloth_snap", Kind.HEAVY, 12, 12, 1, 200, 0.025);
    public static final Row DEBT = Row.damage("chain.debt_comes_due", Kind.ULTIMATE, 16, 16, 8, 700, 0.07);

    public static final List<Row> ALL = List.of(LASH, PULL, BINDING, HYDRA, DEFENCE, GRAPPLE, OROCHI, SLOTH_WEIGHT, SLOTH_SNAP, DEBT);

    /** The ids of the ultimates (one per form at most). */
    public static final Set<String> ULTIMATES = Set.of(OROCHI.id(), DEBT.id());

    // ------------------------------------------------------------------ Chain Lash
    public static final double LASH_RANGE = 7.0, LASH_WIDTH = 1.0, LASH_TUG = 0.5, LASH_TUG_FROM = 3.0;

    // ------------------------------------------------------------------ Chain Pull, Grapple
    public static final double PULL_RANGE = 16.0, PULL_SPEED = 1.3, PULL_STOP = 2.2, BOSS_WEIGHT = 0.25;
    public static final double GRAPPLE_RANGE = 20.0, GRAPPLE_SPEED = 1.4, GRAPPLE_STOP = 2.5;
    public static final int PULL_TICKS = 14;

    // ------------------------------------------------------------------ Chain Binding
    public static final double BINDING_RANGE = 14.0;
    public static final int BINDING_TICKS = 60;

    // ------------------------------------------------------------------ Hydra
    public static final int HYDRA_LINES = 5, HYDRA_TELL = 10;
    public static final double HYDRA_SPREAD = 70.0, HYDRA_LENGTH = 10.0, HYDRA_WIDTH = 1.0, HYDRA_POP = 0.35;

    // ------------------------------------------------------------------ Chain Defence
    public static final double DEFENCE_RADIUS = 4.0;
    public static final int DEFENCE_TICKS = 120;

    // ------------------------------------------------------------------ Orochi
    public static final double OROCHI_RANGE = 18.0, OROCHI_RADIUS = 6.0;
    /** The second constriction comes this many ticks after the first: more than the hurt-resistance window, or it would do nothing. */
    public static final int OROCHI_TELL = 24, OROCHI_BIND_TICKS = 80, OROCHI_SQUEEZE = 30;

    // ------------------------------------------------------------------ the heavy links (passive)
    public static final int WEIGHT_SLOW_TICKS = 40;

    // ------------------------------------------------------------------ the Sloth series
    public static final double SLOTH_RADIUS = 8.0;
    public static final int SLOTH_TICKS = 120, SLOTH_WEIGHT_CORRUPTION = 30;
    public static final double SNAP_RANGE = 14.0, SNAP_KEEP = 18.0;
    public static final int SNAP_DELAY = 60, SNAP_CORRUPTION = 35;
    public static final double DEBT_RADIUS = 9.0, DEBT_BILL_RADIUS = 11.0;
    public static final int DEBT_TICKS = 120, DEBT_STAGGER = 20, DEBT_CORRUPTION = 120;

    // ------------------------------------------------------------------ small pure rules
    /** How strongly a creature is pulled: a boss barely moves, everything else fully. */
    public static double pullWeight(boolean boss) {
        return boss ? BOSS_WEIGHT : 1.0;
    }

    /** The Absorption amplifier of a Chain Defence: level I (4 health) for most, level II (8) from 40 maximum health, never above a quarter of the health protected. */
    public static int absorptionAmp(double maxHealth) {
        return maxHealth >= 40.0 ? 1 : 0;
    }
}
