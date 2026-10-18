package com.newuniverse.nusmp.heroweapon;

/**
 * Why: a hero weapon is held and used in one of a few ways, and the item class, the animation and the input handling depend on that way, not on the weapon.
 * {@link #BLOCK}: held up with the use key (the Shield; the use key therefore cannot also fire a skill, so a blocking weapon fires its skills with sneak and the
 * keys); {@link #DRAW}: drawn and released like a bow; {@link #SWING}: a melee weapon (use key = the first technique); {@link #THROW}: thrown or fired in a line;
 * {@link #CAST}: a focus the wielder casts through (staff, book, mirror, instrument, sewing set); {@link #RIDE}: calls a vehicle (the Carriage).
 */
public enum HeroStyle {
    SWING, BLOCK, DRAW, THROW, CAST, RIDE
}
