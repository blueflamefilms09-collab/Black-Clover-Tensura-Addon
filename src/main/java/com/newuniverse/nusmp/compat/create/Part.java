package com.newuniverse.nusmp.compat.create;

import java.util.List;

/**
 * Why: the vocabulary of a trap blueprint. A part is a LOGICAL block ("a mechanical saw", "a clutch", "a lever"), not a registry id, so the blueprint and its rules stay pure
 * and testable without Create. Each Create part lists the registry paths under the {@code create} namespace it may be found under, best guess first (every id is written from
 * memory of Create 6.x for NeoForge 1.21.1, and each one is OPTIONAL: when none resolves the part falls back to the vanilla block named here, and the miss is logged once),
 * and each part names the vanilla block that stands in for it. The vanilla parts (lever, plate, hooks, the redstone block that powers a clutch) have no Create ids.
 * {@code schematicannon} is deliberately in no list (see {@link CreateIds#FORBIDDEN}). The gearshift, the rotation speed controller, the piston head and the casings are not parts: no drawing uses them (see docs/compat/create.md, section 8).
 */
public enum Part {
    // ---- the power train (kinetic)
    MOTOR(true, "minecraft:copper_block", false, "creative_motor"),
    SHAFT(true, "minecraft:chain", false, "shaft"),
    COGWHEEL(true, "minecraft:polished_andesite", false, "cogwheel"),
    LARGE_COGWHEEL(true, "minecraft:polished_andesite", false, "large_cogwheel"),
    GEARBOX(true, "minecraft:polished_andesite", false, "gearbox"),
    CLUTCH(true, "minecraft:polished_andesite", false, "clutch"),
    CHAIN_DRIVE(true, "minecraft:polished_andesite", false, "encased_chain_drive", "chain_drive"),
    // ---- the working parts
    SAW(true, "minecraft:grindstone", false, "mechanical_saw"),
    DRILL(true, "minecraft:dispenser", false, "mechanical_drill"),
    PISTON(true, "minecraft:piston", false, "mechanical_piston"),
    PISTON_POLE(false, "minecraft:end_rod", false, "piston_extension_pole"),
    BELT(true, "minecraft:black_concrete", false, "belt"),
    // ---- decoration only
    DEPLOYER(true, "minecraft:dispenser", false, "deployer"),
    STORAGE_INTERFACE(false, "minecraft:polished_andesite", false, "portable_storage_interface"),
    // ---- the fluid line
    SPOUT(false, "minecraft:hopper", false, "spout"),
    PIPE(false, "minecraft:lightning_rod", false, "fluid_pipe"),
    TANK(false, "minecraft:copper_block", false, "fluid_tank"),
    // ---- signals
    NIXIE(false, "minecraft:redstone_lamp", false, "nixie_tube"),
    LINK(false, "minecraft:observer", false, "redstone_link"),
    // ---- steam, for the look
    WHISTLE(false, "minecraft:copper_block", false, "steam_whistle"),
    STEAM_ENGINE(false, "minecraft:copper_block", false, "steam_engine"),
    /** The ram of the press: a solid block the runtime puts into the lane's edge columns for a moment. */
    RAM(false, "minecraft:iron_block", false, "industrial_iron_block"),
    // ---- vanilla parts (no Create id)
    LEVER(false, "minecraft:lever", true),
    PLATE(false, "minecraft:stone_pressure_plate", true),
    HOOK(false, "minecraft:tripwire_hook", true),
    STRING(false, "minecraft:tripwire", true),
    /** A redstone block: powers the clutch (or the nixie tube) next to it. */
    POWER(false, "minecraft:redstone_block", true),
    /** The block that takes the place of {@link #POWER} when the power is switched off. */
    HOLD(false, "minecraft:deepslate_bricks", true);

    private final boolean kinetic;
    private final String fallback;
    private final boolean vanilla;
    private final List<String> createPaths;

    Part(boolean kinetic, String fallback, boolean vanilla, String... createPaths) {
        this.kinetic = kinetic;
        this.fallback = fallback;
        this.vanilla = vanilla;
        this.createPaths = List.of(createPaths);
    }

    /** Is this part a block of the power train (it takes or passes rotation)? */
    public boolean kinetic() { return kinetic; }

    /** The vanilla block id that stands in for the part (and IS the part for the vanilla parts). */
    public String fallback() { return fallback; }

    /** A part with no Create id. */
    public boolean vanilla() { return vanilla; }

    /** The registry paths (namespace {@code create}) to try, in order. */
    public List<String> createPaths() { return createPaths; }

    /** A part the player can walk through (a lever, a plate, a hook, a string): it may stand in the lane. */
    public boolean passable() { return this == LEVER || this == PLATE || this == HOOK || this == STRING; }
}
