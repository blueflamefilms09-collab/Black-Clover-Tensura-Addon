package com.newuniverse.nusmp.arc.kakashi;

import com.newuniverse.nusmp.boss.bars.BarStyleIds;
import com.newuniverse.nusmp.boss.bars.BossBarSource;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;

import java.util.List;

/**
 * Why: what the custom boss bar shows for Kakashi: the Naruto frame, FOUR phases with their own names and the notches at 75, 50 and 25 percent, the Susanoo shield as the shield strip, the
 * Kamui window as "invulnerable", the move being wound up, and the party size. A small adapter (the entity cannot implement the interface itself: its {@code fighters()} is the framework's set).
 * Pure reads, no allocation but the answer; the hub asks at most five times a second.
 */
public final class KakashiBarSource implements BossBarSource {
    private static final List<Integer> THRESHOLDS = List.of(75, 50, 25);
    private final KakashiBoss b;

    public KakashiBarSource(KakashiBoss b) { this.b = b; }

    @Override public String styleId() { return BarStyleIds.NARUTO; }

    @Override public String name() { return b.bossName(); }

    @Override public String title() { return KakashiBoss.CHARACTER.title(); }

    @Override public int phase() { return b.phaseIdx() + 1; }

    @Override public int phaseCount() { return KakashiMath.PHASES; }

    @Override public List<Integer> phaseThresholds() { return THRESHOLDS; }

    @Override public String phaseName() { return KakashiMath.PHASE_NAMES[b.phaseIdx()]; }

    @Override public float shield() { return b.shieldShare(); }

    @Override public boolean invulnerable() { return b.flag(KakashiBoss.F_INTANGIBLE); }

    @Override public boolean enraged() { return b.phaseIdx() >= KakashiMath.SUSANOO; }

    @Override public String castName() { return b.state() == ZogratisBoss.STATE_CASTING ? b.castLabel() : ""; }

    @Override public float castProgress() { return b.state() == ZogratisBoss.STATE_CASTING ? b.phaseProgress() : -1f; }

    @Override public int fighters() { return b.fightActive() ? Math.max(1, b.fighters().size()) : 0; }
}
