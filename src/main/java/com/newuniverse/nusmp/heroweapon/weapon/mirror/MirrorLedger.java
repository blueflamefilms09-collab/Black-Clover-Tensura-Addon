package com.newuniverse.nusmp.heroweapon.weapon.mirror;

/**
 * Why: one wielder's Mirror has a little memory that several techniques share: the panes that stand, the gleam stored, the image left behind by the last step
 * (and whether it has already repeated a technique), and the rebound that is open (and how much of its budget is left). Keeping it in one pure object per
 * wielder, with the rules (an image lives so long, repeats once, soaks one blow; a rebound closes when its time or its budget is used up), lets a test play
 * whole sequences without a game. Nothing here is saved: it is the working memory of a fight and is dropped when the person logs out or the server stops.
 * Times are the game clock in ticks.
 */
public final class MirrorLedger {
    public final PaneBook panes = new PaneBook(MirrorNumbers.PANE_MAX);
    public final GleamMeter gleam = new GleamMeter(MirrorNumbers.GLEAM_MAX, MirrorNumbers.GLEAM_DECAY_AFTER, MirrorNumbers.GLEAM_DECAY_EVERY);

    private boolean hasImage, imageEchoed;
    private double ix, iy, iz;
    private float iyaw;
    private long imageUntil;

    private long reboundUntil;
    private double reboundLeft;

    // ------------------------------------------------------------------ the image
    /** Leaves an image where the wielder stood (it replaces an older one). */
    public void placeImage(long now, double x, double y, double z, float yaw, int life) {
        hasImage = true;
        imageEchoed = false;
        ix = x;
        iy = y;
        iz = z;
        iyaw = yaw;
        imageUntil = now + life;
    }

    public boolean imageLive(long now) {
        return hasImage && now < imageUntil;
    }

    public double imageX() { return ix; }

    public double imageY() { return iy; }

    public double imageZ() { return iz; }

    public float imageYaw() { return iyaw; }

    /** True when a live image has not yet repeated a technique. */
    public boolean imageCanEcho(long now) {
        return imageLive(now) && !imageEchoed;
    }

    /** Marks the image as having repeated a technique (it may repeat only once). */
    public void markEchoed() {
        imageEchoed = true;
    }

    public void clearImage() {
        hasImage = false;
        imageEchoed = false;
    }

    // ------------------------------------------------------------------ the rebound
    /** Opens a rebound for {@code ticks} with a budget of {@code budget} damage to turn back. */
    public void openRebound(long now, int ticks, double budget) {
        reboundUntil = now + ticks;
        reboundLeft = Math.max(0.0, budget);
    }

    public boolean reboundOpen(long now) {
        return now < reboundUntil && reboundLeft > 1e-9;
    }

    /** How much budget is left (0 when closed). */
    public double reboundLeft(long now) {
        return reboundOpen(now) ? reboundLeft : 0.0;
    }

    /** Spends {@code amount} of the budget (never below zero). */
    public void spendRebound(double amount) {
        reboundLeft = Math.max(0.0, reboundLeft - Math.max(0.0, amount));
    }

    /** Forgets everything but the panes (the weapon was put away: the glass already in the air stays until its time is up). */
    public void putAway() {
        clearImage();
        gleam.reset();
        reboundUntil = 0;
        reboundLeft = 0;
    }
}
