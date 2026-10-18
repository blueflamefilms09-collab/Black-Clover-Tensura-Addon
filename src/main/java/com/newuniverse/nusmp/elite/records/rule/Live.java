package com.newuniverse.nusmp.elite.records.rule;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Why: the live readings the game gives the records system on a beat, as plain values: Tensura's EP and statuses, the FTB Chunks claim count, vanilla's death and distance statistics, the forge
 * numbers other subsystems publish through hooks, the skills the player knows. A reading that is {@code null} means "that source did not answer / does not exist here" (so a condition built on it is
 * UNAVAILABLE, not zero). The game-side class fills one of these; {@link FactsBuilder} merges it into the saved {@link RecordData} (bests only rise, found-sets only grow) so a number that later drops
 * (an EP loss, a claim released, a skill forgotten) never takes a record away. Pure.
 */
public final class Live {
    /** False when Tensura's storage could not be read at all (then every EP and status condition is unavailable). */
    public boolean tensuraOk;
    public double ep;
    /** 0 none, 1 seed, 2 true. */
    public int demon, hero, higher;
    /** Null when FTB Chunks is absent or could not answer. */
    public Double ftbChunks;
    /** Vanilla statistic "deaths". */
    public int deaths;
    /** Metres per mode: walk, sprint, swim, fly, elytra, boat, mount and all. */
    public final Map<String, Double> distance = new HashMap<>();
    /** The forge's published numbers (0 when the forge is not built or off). */
    public int hookForgeCount, hookForgeBest;
    /** Null when the skill system could not be read. */
    public Set<String> skills;
    /** Tensura skill type (unique, ultimate ...) to the learned skill ids of that type. */
    public final Map<String, Set<String>> skillsByType = new HashMap<>();
}
