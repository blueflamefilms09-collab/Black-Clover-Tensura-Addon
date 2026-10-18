package com.newuniverse.nusmp.elite.races.race;

/**
 * Why: the base of the four Saiyan tiers, so "is this a Saiyan?" is one instanceof (the Great Ape's tail-less look, Zenkai and the ki states all key off the template's
 * traits and states, not off this class, but the commands and the HUD use it to tell the line). Nothing else differs from {@link EliteRace}.
 */
public abstract class SaiyanRace extends EliteRace {
    protected SaiyanRace(String templateId) { super(templateId); }
}
