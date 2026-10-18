package com.newuniverse.nusmp.elite.races.race;

/**
 * Why: the Primordial race (the owner's {@code PrimordialRace}): a being of pure mana, the top of its own line. It extends {@link EliteRace} directly, not the Saiyan base, because
 * nothing in the Saiyan line leads to it; it is entered by the Primordial Rite (the template's {@code entry} rules) or by an administrator. All numbers live in its file.
 */
public class PrimordialRace extends EliteRace {
    public static final String ID = "primordial";

    public PrimordialRace() { super(ID); }
}
