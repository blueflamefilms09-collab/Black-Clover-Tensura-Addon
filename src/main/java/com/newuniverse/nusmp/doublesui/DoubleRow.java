package com.newuniverse.nusmp.doublesui;

import java.util.UUID;

/** One Real Double as the owner's HUD sees it: who (id, and the entity's network id so the client can read its live health and distance), health and the time it has left. */
public record DoubleRow(UUID id, int entityId, float health, float maxHealth, int lifeLeft, int lifeMax) {}
