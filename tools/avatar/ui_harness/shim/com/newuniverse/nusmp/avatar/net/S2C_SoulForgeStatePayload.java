package com.newuniverse.nusmp.avatar.net;

import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;

import java.util.List;

/** Why: harness shim. */
public record S2C_SoulForgeStatePayload(ForgeRules rules, ForgeContext reserves, int rollsLeft, List<ForgeAptitudeRow> aptitudes, List<ForgeSkillRow> skills) {}
