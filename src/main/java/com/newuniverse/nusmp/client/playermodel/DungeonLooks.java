package com.newuniverse.nusmp.client.playermodel;

import com.newuniverse.nusmp.labyrinth.mega.guard.AbyssalLooks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: which look the player-model kit gives each humanoid of the dungeon, as pure functions of plain values (an entity id, its identity string, a stage, a phase), so the whole mapping from "what the
 * server says this entity is" to "which skin is drawn" can be unit tested for every entity and every id with no game class, and so the renderer's per-frame lookups are a map get, not a string
 * built (the results are interned in concurrent maps: {@code "foundry_" + id} is made once per id).
 *
 * <p>The mapping (docs/playermodel_phase2.md):
 * <ul>
 *   <li>{@code living_armor} to {@link #LIVING_ARMOR}, {@code floor_warden} to {@link #FLOOR_WARDEN};</li>
 *   <li>{@code labyrinth_guardian} to the abyssal look its profile names, in the phase it has reached, when that look is a humanoid (the nine of {@code looks_abyssal.json}: id
 *       {@code abyssal_<look>_p<phase>}, {@link AbyssalLooks#kitId}); to {@link #FLOOR_WARDEN} when the profile names none (the Guardian is the Warden grown, as it always was); and to ""
 *       (its own geo model, {@code AbyssalRenderer}) when the look is a creature (a beholder, a dragon). Decennial bosses of other builders reuse the same {@code look} field and so the same
 *       loader: a profile that names a look needs nothing else;</li>
 *   <li>{@code goliath} to {@link #GOLIATH}, or {@link #GOLIATH_FORM2} with the Gates open;</li>
 *   <li>{@code foundry_mob} to {@code foundry_<id>} (a mob that has no such look is a creature and keeps the art-kit renderer, so the renderer asks the table);</li>
 *   <li>the soul-corpse puppets {@code corpse_jimmy}, {@code corpse_alfredo}, {@code corpse_carl} to {@code corpse_<name>_s<stage>}.</li>
 * </ul>
 * {@link #usesKit} is the rule of the fallback flag {@code client.playermodel.dungeon}: off gives every one of these back to its old renderer.
 */
public final class DungeonLooks {
    private DungeonLooks() {}

    public static final String LIVING_ARMOR = "living_armor", FLOOR_WARDEN = "floor_warden", GOLIATH = "goliath", GOLIATH_FORM2 = "goliath_form2";
    public static final String FOUNDRY_PREFIX = "foundry_", ARCHETYPE_PREFIX = "dg_";
    /** The highest decay stage of a soul corpse ({@code CorpseDecay.MAX_STAGE}). */
    public static final int MAX_STAGE = 3;

    /** The ids (registry paths) of the entity types this phase draws with the kit when the flag allows it. */
    public static final List<String> ENTITIES = List.of("living_armor", "floor_warden", "labyrinth_guardian", "goliath", "foundry_mob", "corpse_jimmy", "corpse_alfredo", "corpse_carl");

    private static final Map<String, String> FOUNDRY = new ConcurrentHashMap<>();
    private static final Map<String, String[]> CORPSE = new ConcurrentHashMap<>();
    private static final Map<String, String[]> GUARDIAN = new ConcurrentHashMap<>();
    private static final String[] NONE = new String[0];

    /** The look id of a Foundry mob (interned: one string per id). */
    public static String foundry(String id) { return FOUNDRY.computeIfAbsent(id, k -> FOUNDRY_PREFIX + k); }

    /** The look of a soul-corpse puppet ({@code corpse_jimmy}) at a decay stage (clamped to 0..3). */
    public static String corpse(String entityId, int stage) {
        String[] byStage = CORPSE.computeIfAbsent(entityId, k -> {
            String[] out = new String[MAX_STAGE + 1];
            for (int s = 0; s <= MAX_STAGE; s++) out[s] = k + "_s" + s;
            return out;
        });
        return byStage[Math.max(0, Math.min(MAX_STAGE, stage))];
    }

    /**
     * What the Labyrinth Guardian wears: the kit look of its abyssal look in its phase, {@link #FLOOR_WARDEN} when it has none, "" when the look is a creature drawn from a full geo model.
     *
     * @param lookId the synced look id of the Guardian ("" for none)
     * @param phase  1 to 3
     */
    public static String guardian(String lookId, int phase) {
        if (lookId == null || lookId.isEmpty()) return FLOOR_WARDEN;
        String[] ids = GUARDIAN.computeIfAbsent(lookId, k -> {
            AbyssalLooks.Look l = AbyssalLooks.get(k);
            if (l == null) return new String[]{FLOOR_WARDEN, FLOOR_WARDEN, FLOOR_WARDEN};
            if (!l.humanoid()) return NONE;
            return new String[]{AbyssalLooks.kitId(k, 1), AbyssalLooks.kitId(k, 2), AbyssalLooks.kitId(k, 3)};
        });
        return ids.length == 0 ? "" : ids[Math.max(1, Math.min(3, phase)) - 1];
    }

    /**
     * The look an entity of this type id wears, from what the server synced: {@code identity} is the Guardian's look id or the Foundry mob's id, {@code variant} the Guardian's phase (1 to 3), the
     * Goliath's form (0 base, anything else the Gates open) or a puppet's decay stage. "" = this entity is not drawn by the kit (a creature look, an unknown type, a blank Foundry mob).
     */
    public static String lookFor(String entityId, String identity, int variant) {
        if (entityId == null) return "";
        return switch (entityId) {
            case "living_armor" -> LIVING_ARMOR;
            case "floor_warden" -> FLOOR_WARDEN;
            case "labyrinth_guardian" -> guardian(identity, variant);
            case "goliath" -> variant == 0 ? GOLIATH : GOLIATH_FORM2;
            case "foundry_mob" -> identity == null || identity.isEmpty() ? "" : foundry(identity);
            case "corpse_jimmy", "corpse_alfredo", "corpse_carl" -> corpse(entityId, variant);
            default -> "";
        };
    }

    /** The rule of the fallback flag: the kit draws an entity when the flag is on and the table has its look. */
    public static boolean usesKit(boolean flag, boolean hasLook) { return flag && hasLook; }

    /** Is this abyssal look a humanoid (the player-model kit draws it), as opposed to a creature that keeps its own geo model? False for an id the table does not have. */
    public static boolean isHumanoidAbyssal(String lookId) {
        AbyssalLooks.Look l = AbyssalLooks.get(lookId);
        return l != null && l.humanoid();
    }
}
