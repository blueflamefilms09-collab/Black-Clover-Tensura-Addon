package com.newuniverse.nusmp.skilltree.data;

/**
 * Why (3.26.0, the Evolution skills): a line may ask the player for something to DO before its last art, the Ultimate Evolution art, can be cast, and before its passives reach their evolved numbers
 * ("Kill 1,000 foes", "Take 2,000 fire damage", "Survive 15 near-lethal blows"). The route file says it once, in the route's {@code evolution} block: which counter to watch ({@code requirement}, one of the
 * kinds of {@code catalog.EVOLUTION}), how far it has to go ({@code count}), the sentence the skill card shows ({@code text}), the name the skill is known by once it has evolved ({@code ultimateName}) and, for
 * the {@code damage_taken} counter only, which kind of damage counts ({@code damageKind}, empty meaning any). It is plain immutable data with no Minecraft type, so the schema, the wire format, the
 * card and the pure rules ({@code EvolutionRules}) all share the one record; what is counted for which player is the engine's business (the {@code Evolution} class of the line package).
 */
public record RouteEvolution(String requirement, int count, String text, String ultimateName, String damageKind) {
    /** The counters never go above this (the catalogue's {@code EVOLUTION_MAX_COUNT}). */
    public static final int MAX_COUNT = 1_000_000;
    /** Longest sentence of the skill card (the wire limit). */
    public static final int LEN_TEXT = 200;

    public RouteEvolution {
        requirement = requirement == null ? "" : requirement;
        text = text == null ? "" : text;
        ultimateName = ultimateName == null ? "" : ultimateName;
        damageKind = damageKind == null ? "" : damageKind;
    }

    /** The damage kind the {@code damage_taken} counter watches: {@code any} when the file named none. */
    public String damageKindOrAny() { return damageKind.isEmpty() ? "any" : damageKind; }
}
