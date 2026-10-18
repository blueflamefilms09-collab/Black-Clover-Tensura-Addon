package com.newuniverse.nusmp.elite.nations.econ;

/**
 * Why: the treasury bridge ("treasury bridges"): the one small interface other systems use to pay into or draw from a nation's treasury (the Walpurgis council paying a prize, a season finale paying a
 * reward, a shop sending its tax) without knowing anything of the nations' internals. Amounts are copper coins, the same unit as the wallet hook. Obtained from {@code NationApi.treasuryBridge()}. Every method is
 * null-safe: an unknown nation, a non-positive amount or a disabled system answers 0 / false.
 */
public interface TreasuryBridge {
    long balance(String nationId);

    /** Pays coins in; returns what was accepted (the treasury cap may refuse part of it). */
    long deposit(String nationId, long coins, String reason);

    /** Takes coins out; false when the balance is too small. */
    boolean withdraw(String nationId, long coins, String reason);

    /** The cap of the nation's treasury at its current level, or 0 for an unknown nation. */
    long cap(String nationId);
}
