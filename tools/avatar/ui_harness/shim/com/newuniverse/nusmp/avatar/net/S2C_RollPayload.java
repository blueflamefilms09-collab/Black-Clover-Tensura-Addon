package com.newuniverse.nusmp.avatar.net;

/** Why: harness shim. */
public record S2C_RollPayload(int[] stats, boolean fiveDice, double luck, long token, int rerollCost) {}
