package com.newuniverse.nusmp.heroweapon.weapon.shield;

/**
 * Why: the Soul Eater Shield eats the souls of what dies near it and spends them as a ward (Soul Aegis). The souls are a small counter with a memory: each soul
 * gained keeps the whole pool alive for {@link ShieldNumbers#SOUL_HOLD} ticks, the pool holds at most {@link ShieldNumbers#SOULS_MAX}, and a pool nobody touched
 * for that long has gone cold and counts as empty. No Minecraft types, so it is tested alone.
 */
public final class SoulPool {
    private int souls;
    private long until;

    /** One soul more (up to the maximum); the pool stays warm for the hold time from now. Returns the souls now. */
    public int add(long now) {
        if (now >= until) souls = 0;
        souls = Math.min(ShieldNumbers.SOULS_MAX, souls + 1);
        until = now + ShieldNumbers.SOUL_HOLD;
        return souls;
    }

    /** The souls held at {@code now} (0 once the pool went cold; a clock that jumped far back also counts as cold). */
    public int count(long now) {
        if (now >= until || until - now > ShieldNumbers.SOUL_HOLD * 2L) return 0;
        return souls;
    }

    /** Spends every soul and returns how many there were. */
    public int spendAll(long now) {
        int n = count(now);
        souls = 0;
        until = 0;
        return n;
    }

    public void clear() {
        souls = 0;
        until = 0;
    }
}
