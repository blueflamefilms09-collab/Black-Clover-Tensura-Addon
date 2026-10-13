package com.newuniverse.nusmp.relics.wave;

import java.util.Set;

/**
 * Wave 4: the Charm Person of the Amulet of the Forest's peacock. Charm Person works on humanoids: here, the entity types below
 * (the vanilla humanoids and Tensura's). A charmed mob forgets the charmer and will not pick any player as a target for twenty
 * seconds; a mob too mighty to be charmed (the "saving throw") shrugs it off and cannot be charmed again for a game day.
 * Pure Java.
 */
final class Wave4Charm {
    private Wave4Charm() {}

    static final double RADIUS = 5.0;                  // 15 feet
    static final int TICKS = 400;
    static final int IMMUNE_TICKS = 24000;
    /** A creature this tough resists the charm outright. */
    static final float RESIST_FROM_HEALTH = 40f;
    static final int MAX_TARGETS = 12;

    private static final Set<String> HUMANOIDS = Set.of(
            "pillager", "vindicator", "evoker", "illusioner", "witch", "piglin", "piglin_brute", "zombie", "husk", "drowned",
            "zombie_villager", "skeleton", "stray", "bogged", "villager", "wandering_trader",
            "goblin", "hobgoblin", "orc", "lizardman", "war_gnome", "lesser_daemon", "dwarf", "elf");

    /** {@code path} is the entity type's registry path; only the vanilla and Tensura namespaces count. */
    static boolean humanoid(String namespace, String path) {
        return ("minecraft".equals(namespace) || "tensura".equals(namespace)) && path != null && HUMANOIDS.contains(path);
    }

    static boolean resists(float maxHealth) { return maxHealth >= RESIST_FROM_HEALTH; }
}
