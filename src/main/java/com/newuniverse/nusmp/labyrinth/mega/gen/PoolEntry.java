package com.newuniverse.nusmp.labyrinth.mega.gen;

/**
 * Why: one weighted element of a template pool, as the assembler reads it: which template (shape) it names, how often it should be drawn compared
 * with its neighbours, and which processor list the game must run over it when it is placed. The processor list is carried as an opaque string
 * (the assembler never looks at it) so the plan that comes out of the pure code already says everything the placer needs.
 *
 * @param shape      the template id, e.g. {@code nusmp:mega/hall/crypt_a}
 * @param weight     the pool weight (at least 1)
 * @param processors the processor list id of the pool element, e.g. {@code nusmp:mega_interior}, or {@code minecraft:empty}
 */
public record PoolEntry(String shape, int weight, String processors) {
    public PoolEntry {
        if (weight < 1) weight = 1;
        if (processors == null || processors.isEmpty()) processors = "minecraft:empty";
    }
}
