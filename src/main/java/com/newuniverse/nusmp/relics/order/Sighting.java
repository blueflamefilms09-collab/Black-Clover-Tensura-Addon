package com.newuniverse.nusmp.relics.order;

import net.minecraft.world.Container;

import java.lang.ref.WeakReference;

/**
 * A duplicate that was seen: which relic, where, and what is needed to find it again when the Order arrives (the container is held weakly: the
 * Order must never keep a chest alive).
 */
record Sighting(String relicId, Spot spot, WeakReference<Container> container, long tick) {
    Sighting withTick(long t) { return new Sighting(relicId, spot, container, t); }
}
