package com.newuniverse.nusmp.elite.races.state;

import java.util.List;
import java.util.Set;

/**
 * Why: everything one transformation state of one race tier is, as plain data read from the race file: who may enter it and how ({@code from}, {@code entries}), for how long and
 * with what rest afterwards ({@code durationTicks}, {@code cooldownTicks}), what it costs ({@code auraDrainPerSecond}: a share of the maximum aura, the ki pool, every second),
 * whether it ends with the night ({@code endsAtDawn}, the Great Ape), whether its bonuses are scaled
 * down while the Kaioken skill is also on ({@code kiScaled}), which entries may start it by themselves (the {@code auto} flag of an {@link EntryCondition}), its look ({@code color}, {@code darkRukh}) and the attribute modifiers it adds.
 */
public record TransformSpec(TransformState state, Set<TransformState> from, int minTier, List<EntryCondition> entries, int durationTicks, int cooldownTicks,
                            double auraDrainPerSecond, boolean endsAtDawn, boolean kiScaled, boolean darkRukh, int color, List<StateModifier> modifiers) {
    public TransformSpec {
        from = Set.copyOf(from);
        entries = List.copyOf(entries);
        modifiers = List.copyOf(modifiers);
    }
}
