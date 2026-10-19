package com.newuniverse.nusmp.celestial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Why: the server must know, for each player, which keys they have ever held, which spirits they have a contract with, how close they are to each spirit, whether the
 * king has been faced and when they may face him again. This is that record, a plain object with no game class (the attachment that saves it is
 * {@link CelestialAttachments}); every change goes through a method so the rules (a key is collected once, the twelfth gold key is noticed once, a reward is claimed once)
 * live in one place and can be tested. The masks are indexed by the ordinals of {@link CelestialKey}.
 */
public final class CelestialData {
    /** Keys ever held (the collection), and keys with a sealed contract. */
    private int owned, bound;
    private final int[] bond = new int[CelestialKey.values().length];
    /** The game time before which the player may not challenge the king again; the time the open Gate's choice runs out. */
    private long kingCooldownUntil, gateOpenUntil;
    private boolean gateKeyGiven, won, recognised, winClaimed, recognitionClaimed, sceptreGiven;
    /** The Star Dress worn: its key ordinal (-1 none) and the game time it ends. */
    private int dressKey = -1;
    private long dressUntil;
    private int fights;

    public CelestialData() {}

    // ================================================================ keys
    public int owned() { return owned; }

    public int bound() { return bound; }

    /** Records a key as collected. Returns true the first time the key is seen. */
    public boolean collect(CelestialKey key) {
        boolean fresh = !CelestialRules.has(owned, key);
        owned = CelestialRules.collect(owned, key);
        return fresh;
    }

    public boolean has(CelestialKey key) { return CelestialRules.has(owned, key); }

    public boolean isBound(CelestialKey key) { return CelestialRules.has(bound, key); }

    /** Seals the contract with a spirit (also counts the key as collected). */
    public void bind(CelestialKey key) { owned = CelestialRules.collect(owned, key); bound = CelestialRules.collect(bound, key); }

    public void unbind(CelestialKey key) { bound &= ~key.bit(); }

    /** True when the collection holds all twelve gold keys. */
    public boolean complete() { return CelestialRules.completes(owned); }

    public boolean gateKeyGiven() { return gateKeyGiven; }

    /**
     * True exactly once: when the collection is complete and the gate key has not been handed over yet (the caller then gives the item and calls {@link #gateKeyHandedOver}).
     */
    public boolean gateKeyDue() { return complete() && !gateKeyGiven; }

    public void gateKeyHandedOver() { gateKeyGiven = true; }

    // ================================================================ bond
    public int bondXp(CelestialKey key) { return bond[key.ordinal()]; }

    public int bondLevel(CelestialKey key) { return CelestialRules.bondLevel(bond[key.ordinal()]); }

    /** Adds bond points; returns true when it raised the level. */
    public boolean addBond(CelestialKey key, int points) {
        if (points <= 0) return false;
        int before = bondLevel(key);
        bond[key.ordinal()] = Math.min(10_000, bond[key.ordinal()] + points);
        return bondLevel(key) > before;
    }

    public int[] bondCopy() { return Arrays.copyOf(bond, bond.length); }

    // ================================================================ the king
    public long kingCooldownUntil() { return kingCooldownUntil; }

    public void setKingCooldown(long until) { kingCooldownUntil = until; }

    public long gateOpenUntil() { return gateOpenUntil; }

    public void setGateOpenUntil(long t) { gateOpenUntil = t; }

    public boolean won() { return won; }

    public boolean recognised() { return recognised; }

    public int fights() { return fights; }

    public void fought() { fights = Math.min(10_000, fights + 1); }

    /** Marks the outcome; true the first time it happens. */
    public boolean markWon() { boolean f = !won; won = true; return f; }

    public boolean markRecognised() { boolean f = !recognised; recognised = true; return f; }

    /** True once per outcome: the reward of a victory is claimed only the first time. */
    public boolean claimWin() { boolean f = won && !winClaimed; if (f) winClaimed = true; return f; }

    public boolean claimRecognition() { boolean f = recognised && !recognitionClaimed; if (f) recognitionClaimed = true; return f; }

    public boolean winClaimed() { return winClaimed; }

    public boolean recognitionClaimed() { return recognitionClaimed; }

    public boolean sceptreGiven() { return sceptreGiven; }

    public void sceptreHandedOver() { sceptreGiven = true; }

    /** The king's modes open with the rewards actually claimed. */
    public boolean blessingMode() { return winClaimed || recognitionClaimed; }

    public boolean summonMode() { return winClaimed; }

    // ================================================================ star dress
    public int dressKey() { return dressKey; }

    public long dressUntil() { return dressUntil; }

    public void setDress(int key, long until) { dressKey = key; dressUntil = until; }

    public boolean dressActive(long now) { return dressKey >= 0 && now < dressUntil; }

    // ================================================================ copy / persistence form
    /** Everything as a flat list of numbers: the attachment's codec writes this (new fields are only ever appended). */
    public List<Long> toList() {
        List<Long> out = new ArrayList<>();
        out.add((long) owned);
        out.add((long) bound);
        out.add(kingCooldownUntil);
        out.add(gateOpenUntil);
        long flags = (gateKeyGiven ? 1 : 0) | (won ? 2 : 0) | (recognised ? 4 : 0) | (winClaimed ? 8 : 0) | (recognitionClaimed ? 16 : 0) | (sceptreGiven ? 32 : 0);
        out.add(flags);
        out.add((long) dressKey);
        out.add(dressUntil);
        out.add((long) fights);
        for (int b : bond) out.add((long) b);
        return out;
    }

    public static CelestialData fromList(List<Long> in) {
        CelestialData d = new CelestialData();
        if (in == null) return d;
        d.owned = (int) get(in, 0, 0);
        d.bound = (int) get(in, 1, 0);
        d.kingCooldownUntil = get(in, 2, 0);
        d.gateOpenUntil = get(in, 3, 0);
        long f = get(in, 4, 0);
        d.gateKeyGiven = (f & 1) != 0; d.won = (f & 2) != 0; d.recognised = (f & 4) != 0; d.winClaimed = (f & 8) != 0; d.recognitionClaimed = (f & 16) != 0; d.sceptreGiven = (f & 32) != 0;
        d.dressKey = (int) get(in, 5, -1);
        d.dressUntil = get(in, 6, 0);
        d.fights = (int) get(in, 7, 0);
        for (int i = 0; i < d.bond.length; i++) d.bond[i] = (int) get(in, 8 + i, 0);
        return d;
    }

    private static long get(List<Long> in, int i, long dflt) { return i < in.size() ? in.get(i) : dflt; }

    public CelestialData copy() { return fromList(toList()); }

    /** A death keeps everything: the keys and the contracts are the player's, not the body's. */
    public void copyFrom(CelestialData o) {
        CelestialData c = fromList(o.toList());
        owned = c.owned; bound = c.bound; kingCooldownUntil = c.kingCooldownUntil; gateOpenUntil = c.gateOpenUntil;
        gateKeyGiven = c.gateKeyGiven; won = c.won; recognised = c.recognised; winClaimed = c.winClaimed; recognitionClaimed = c.recognitionClaimed; sceptreGiven = c.sceptreGiven;
        dressKey = c.dressKey; dressUntil = c.dressUntil; fights = c.fights;
        System.arraycopy(c.bond, 0, bond, 0, bond.length);
    }

    /** Admin reset: everything is forgotten. */
    public void reset() { copyFrom(new CelestialData()); }
}
