package com.newuniverse.nusmp.client.relics.djinn.gui;

/**
 * What the panel shows this frame: one mutable object the game refills every frame (no allocation). Times are game ticks, fractions 0..1.
 * The clock {@link #time} is the game time plus the partial tick.
 */
public final class GuiState {
    public int phase, prev;
    /** Ticks since the phase last changed. */
    public float phaseAge = 1000f;
    /** Ticks left of a timed phase (Lesser Summon, Full Equip, the charge of an Extreme Magic) and the most it could be. */
    public float timeLeft, timeMax = 1f;
    /** The Magoi pool 0..100 and the assimilation 0..1000. */
    public int magoi, assim, trust;
    /** The Maso lockout and the Extreme Magic cooldown, ticks left and the longest they run. */
    public float lockLeft, lockMax = 6000f, coolLeft, coolMax = 12000f;
    public boolean dark;
    /** 1 right after an Extreme Magic strikes, fading to 0. */
    public float pulse;
    /** Ticks since the panel was first shown for this Djinn (the summon intro); big when over. */
    public float introAge = 1000f;
    public float time;

    public boolean full() { return phase >= 3; }

    /** How much of the Djinn the emblem shows, 0..1 ({@link GuiAnim#reveal}). */
    public float reveal() { return GuiAnim.reveal(phase, prev, phaseAge); }

    /** How hard everything moves ({@link GuiAnim#energy}), eased across a phase change. */
    public float energy() {
        return GuiAnim.lerp(GuiAnim.energy(prev), GuiAnim.energy(phase), GuiAnim.ramp(phaseAge, 0f, 14f));
    }

    /** The charge of an Extreme Magic 0..1 (0 in any other phase). */
    public float charge() { return phase >= 4 ? 1f - GuiAnim.clamp01(timeLeft / Math.max(1f, timeMax)) : 0f; }

    /** The fraction the timer bar and the timer ring show: time left of a timed phase, the charge of an Extreme Magic, full for the untimed Half Equip. */
    public float timerFrac() {
        if (phase >= 4) return charge();
        if (phase == 1 || phase == 3) return GuiAnim.clamp01(timeLeft / Math.max(1f, timeMax));
        return phase == 2 ? 1f : 0f;
    }

    /** True while the panel has something to say about the lockout or the cooldown even with no phase running. */
    public boolean recovering() { return lockLeft > 0f || coolLeft > 0f; }
}
