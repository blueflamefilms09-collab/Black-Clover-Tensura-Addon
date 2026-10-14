package com.newuniverse.nusmp.boss;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The per-fight scratch state of Lucius's overhaul (marks locked by a wind-up, the rainbow stacks, the Usurper cooldown). Main thread only. */
final class LuciusKit {
    final DeadEndRainbow<UUID> rainbow = new DeadEndRainbow<>();
    long nextUsurper;
    List<Vec3> blastMarks = new ArrayList<>();
    Vec3 cutterFrom, cutterDir;
    long lastRainbowNote;

    void reset() {
        rainbow.clear();
        blastMarks = new ArrayList<>();
        cutterFrom = null;
        cutterDir = null;
        nextUsurper = 0;
    }
}
