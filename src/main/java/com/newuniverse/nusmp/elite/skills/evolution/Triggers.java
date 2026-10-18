package com.newuniverse.nusmp.elite.skills.evolution;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the data file names triggers by id; this is the one table that turns an id into its class. The twelve built-in triggers are listed once here, in a stable order, so
 * the engine, the self-test and the unit tests all see the same set, and an unknown id in a data file is found at load time (and the path skipped) instead of silently passing.
 */
public final class Triggers {
    private Triggers() {}

    private static final Map<String, EvolutionTrigger> ALL = new LinkedHashMap<>();

    static {
        for (EvolutionTrigger t : List.of(new AbilityMastered(), new ActiveTitleEvolution(), new AwakeningEvolution(), new EPGate(), new ForgeCraftedCount(),
                new ForgeQuality(), new HasTitle(), new HeldTitleCount(), new ItemConsume(), new KillEvolution(), new RecordUnlocked(), new ResetCount())) {
            ALL.put(t.id(), t);
        }
    }

    /** The trigger with this id, or null. */
    public static EvolutionTrigger get(String id) { return id == null ? null : ALL.get(id); }

    public static List<String> ids() { return Collections.unmodifiableList(List.copyOf(ALL.keySet())); }

    public static int count() { return ALL.size(); }
}
