package com.newuniverse.nusmp.avatar.net;

import net.minecraft.nbt.CompoundTag;

import java.util.List;

/** Why: harness shim. */
public record C2S_FinalizeSoulForgePayload(String raceId, String primary, String secondary, CompoundTag appearance, String name, long statToken,
                                           String unique, String ultimate, List<String> aptitudes, long rollToken, boolean paidAwakening) {}
