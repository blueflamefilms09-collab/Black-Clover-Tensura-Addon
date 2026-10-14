package com.newuniverse.nusmp.relics.socket;

/**
 * The numbers of a socket's active ability: the key (its cooldown is kept per relic and key by the relic engine), the display name, the cooldown in
 * ticks, the cost in percent of the user's maximum magicules (paid in magicules, then aura, like every relic act) and a power figure the body scales
 * its damage and healing by (before the balance law caps them). Pure.
 */
public record ActSpec(String key, String name, int cooldownTicks, int manaPercent, double power) {}
