package com.newuniverse.nusmp.relics.djinn;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * A player's Djinn record, as a view over the attachment's compound: which Djinn is bound (or lent to them), the serial of the vessel it sits in,
 * the phase, how long it has run, trust and assimilation (these two live on Tensura attribute modifiers: {@link DjinnBody}), and the clocks (Maso lockout, Lesser Summon and Extreme Magic cooldowns). Game times
 * are absolute ticks of the level's clock, so a relog or a death does not reset a wait. Cheap to make: it holds no data of its own.
 */
public final class DjinnState {
    private final CompoundTag t;
    private final Player player;

    private DjinnState(CompoundTag t, Player player) { this.t = t; this.player = player; }

    public static DjinnState of(Player p) { return new DjinnState(p.getData(DjinnAttachments.STATE), p); }

    // ---------------------------------------------------------------- the bond
    /** The Djinn this player owns, or null. */
    public Djinn djinn() { return Djinn.byName(t.getString("djinn")); }

    public UUID serial() { return t.hasUUID("serial") ? t.getUUID("serial") : null; }

    public void bind(Djinn d, UUID serial, int trust) {
        t.remove("dark");                                                   // a new bond starts in its own light
        t.putString("djinn", d.name());
        t.putUUID("serial", serial);
        t.putInt("phase", 0);
        t.putInt("used", 0);
        setTrust(trust);
        setAssimilation(0f);
        t.remove("lentFrom");
        t.remove("lentDjinn");
    }

    /** The Djinn lent to this player by a household owner, or null. */
    public Djinn lentDjinn() { return t.hasUUID("lentFrom") ? Djinn.byName(t.getString("lentDjinn")) : null; }

    public UUID lentFrom() { return t.hasUUID("lentFrom") ? t.getUUID("lentFrom") : null; }

    public void lend(UUID owner, Djinn d) {
        t.remove("dark");                                                   // a household member never gets a dark Djinn
        t.putUUID("lentFrom", owner);
        t.putString("lentDjinn", d.name());
        t.putInt("phase", 0);
        setAssimilation(0f);
    }

    /** Clears the bond (owner or household): the Djinn, its serial, the phase and what use built up. Clocks stay: a wait is not washed away. */
    public void clearBond() {
        for (String k : new String[]{"djinn", "serial", "lentFrom", "lentDjinn", "phase", "start", "end", "used", "trust", "assim", "active", "fly", "dark"}) t.remove(k);
        DjinnBody.clear(player);
    }

    /** The Djinn this player can call: their own, or the one lent to them. */
    public Djinn active() {
        Djinn d = djinn();
        return d != null ? d : lentDjinn();
    }

    public boolean owns() { return djinn() != null; }

    public boolean lent() { return djinn() == null && lentDjinn() != null; }

    // ---------------------------------------------------------------- the phase
    public int phase() { return t.getInt("phase"); }
    public void setPhase(int p) { t.putInt("phase", p); }

    public long start() { return t.getLong("start"); }
    /** The game time a Lesser Summon ends / a Maso times out (0 when the phase has no end). */
    public long end() { return t.getLong("end"); }
    public void setSpan(long start, long end) { t.putLong("start", start); t.putLong("end", end); }

    /** Ticks spent in the current Maso. */
    public int used() { return t.getInt("used"); }
    public void setUsed(int u) { t.putInt("used", u); }

    /** Ticks of use since the last point of trust. */
    public int activeTicks() { return t.getInt("active"); }
    public void setActiveTicks(int a) { t.putInt("active", a); }

    // ---------------------------------------------------------------- trust, assimilation
    /** Trust: a persistent Tensura ability-mastery-gain modifier (see {@link DjinnBody}). */
    public int trust() { return DjinnBody.trust(player, t); }
    public void setTrust(int v) { DjinnBody.setTrust(player, t, com.newuniverse.nusmp.relics.djinn.dark.DarkRules.trustAfter(trust(), v, dark())); }

    /** Assimilation: a persistent Tensura ability-learning-gain modifier, with element resistance while it lasts (see {@link DjinnBody}). */
    public float assimilation() { return DjinnBody.assimilation(player, t); }
    public void setAssimilation(float v) { DjinnBody.setAssimilation(player, t, v); }

    // ---------------------------------------------------------------- the dark state
    /**
     * True while the owner's Djinn has drunk the black rukh (the Dark Djinn: {@code relics.djinn.dark}). Saved under {@code "dark"}, kept through
     * death with the rest of the record, removed by {@link #clearBond()} and never set on a household member. Read it through
     * {@code DarkDjinn.active(state)} when the question is "is the Djinn this player calls dark": that also checks that they own it.
     */
    public boolean dark() { return t.getBoolean("dark"); }

    public void setDark(boolean b) { if (b) t.putBoolean("dark", true); else t.remove("dark"); }

    // ---------------------------------------------------------------- the clocks
    public long lockoutUntil() { return t.getLong("lock"); }
    public void setLockoutUntil(long v) { t.putLong("lock", v); }
    public long lesserReadyAt() { return t.getLong("lesserReady"); }
    public void setLesserReadyAt(long v) { t.putLong("lesserReady", v); }
    public long extremeReadyAt() { return t.getLong("extremeReady"); }
    public void setExtremeReadyAt(long v) { t.putLong("extremeReady", v); }

    // ---------------------------------------------------------------- strain (the backlash that heals back)
    /** The game time the maximum-health strain ends, how much health it takes and how long it was meant to last (0 = none). */
    public long strainUntil() { return t.getLong("strainUntil"); }
    public int strainLoss() { return t.getInt("strainLoss"); }
    public int strainTotal() { return t.getInt("strainTotal"); }
    public void setStrain(long until, int loss, int total) { t.putLong("strainUntil", until); t.putInt("strainLoss", loss); t.putInt("strainTotal", total); }
    public void clearStrain() { t.remove("strainUntil"); t.remove("strainLoss"); t.remove("strainTotal"); }

    // ---------------------------------------------------------------- the weapon loan
    /**
     * The compound the Djinn weapon's loan saves its state in (the hotbar slot the weapon took, the item it displaced, the stash of items that found no
     * free slot): see {@code relics.djinn.weapon.DjinnWeaponService}. {@link #clearBond()} never touches it: a stash is the player's own item and only
     * the loan's end gives it back.
     */
    public CompoundTag loan() {
        if (!t.contains("wloan", 10)) t.put("wloan", new CompoundTag());
        return t.getCompound("wloan");
    }

    public boolean hasLoan() { return t.contains("wloan", 10) && !t.getCompound("wloan").isEmpty(); }

    public void clearLoan() { t.remove("wloan"); }

    // ---------------------------------------------------------------- flight
    /** True while the Maso gave this player the ability to fly (so the end of it must take it back). */
    public boolean flightGranted() { return t.getBoolean("fly"); }
    public void setFlightGranted(boolean b) { if (b) t.putBoolean("fly", true); else t.remove("fly"); }
}
