package com.newuniverse.nusmp.compat.create;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Why: the one pure place that names Create (the mod id, and the registry ids of the entity types and damage types the runtime looks up) so nothing else in the package
 * has to spell {@code "create"}. The block ids live with their {@link Part}; this class adds the table the documentation prints, the list of what the mod must NEVER
 * use, and the matching rule of the contraption guard.
 */
public final class CreateIds {
    private CreateIds() {}

    public static final String MOD_ID = "create";

    /**
     * Blocks the traps must never place (the owner: "schematicannon is NOT to be used"). A test walks every {@link Part} and fails if any of these appears among the
     * candidates.
     */
    public static final Set<String> FORBIDDEN = Set.of("create:schematicannon", "create:schematic_table", "create:creative_crate", "create:creative_fluid_tank");

    /**
     * Damage types a hit tries first, by hazard kind ({@code create:} paths, from memory), before the vanilla stand-in. A missing key just means the vanilla source is
     * used.
     */
    public static String damageTypePath(HazardKind kind) {
        return switch (kind) {
            case SLASH -> "mechanical_saw";
            case CRUSH -> "crush";
            case BORE -> "mechanical_drill";
            case SCALD -> "fan_lava";
            case SHOCK -> "cuckoo_surprise";
        };
    }

    /**
     * The contraption guard's rule on entity types: a Create entity whose id contains {@code contraption} (the contraption, the stationary, gantry and carriage
     * contraption entities) may not join a world that the Dungeon Guard holds. {@code typeId} is a registry id string such as {@code create:contraption}.
     */
    public static boolean isContraptionEntity(String typeId) {
        if (typeId == null) return false;
        int colon = typeId.indexOf(':');
        if (colon < 0) return false;
        return typeId.substring(0, colon).equals(MOD_ID) && typeId.substring(colon + 1).contains("contraption");
    }

    /** The full candidate ids of a part, in the order they are tried (empty for a vanilla part). */
    public static List<String> candidates(Part part) {
        List<String> out = new ArrayList<>();
        for (String p : part.createPaths()) out.add(MOD_ID + ":" + p);
        return out;
    }
}
