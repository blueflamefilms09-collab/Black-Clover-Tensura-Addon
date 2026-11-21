package com.newuniverse.nusmp.bossdrops2;

import java.util.UUID;

/** One player who fought a boss: how much damage they dealt (0 for a fighter only known by presence) and whether theirs was the last blow. Pure. */
public record Fighter(UUID id, double damage, boolean killer) {}
