package com.newuniverse.nusmp.avatar.cast;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: an Avatar casts grimoire pages without a real player behind them, so only pages that are safe to run that way may be cast: attacks and a few
 * self buffs. Anything that grants items, opens a GUI, changes blocks for good, summons or reanimates, bends time or space for the world, seals or
 * dominates, heals the owner or targets the owner is excluded. The grimoire has ~500 pages and their code is not audited one by one, so the rule is
 * a pure, testable ALLOW-by-shape list (the id says it is a strike / bolt / lance / shard ...) minus an explicit DENY list and word filter, plus
 * a hard cap on cost (signature and domain pages are never cast by an Avatar). Tensura skills go through a separate short id allowlist.
 */
public final class AvatarSkillList {
    private AvatarSkillList() {}

    /** Page ids (or id fragments) that read as a plain attack. */
    private static final List<String> ATTACK_WORDS = List.of("strike", "shot", "bolt", "lance", "ball", "blast", "slash", "shard", "volley", "barrage",
            "bullet", "spit", "javelin", "thrust", "claw", "fang", "lash", "whip", "stream", "arc", "stinger", "spike", "cannon", "beam", "fist", "linea", "rain",
            "rush", "kamaitachi", "waterball", "orbit", "needle", "cleave", "tempest", "shrapnel", "wedge_driver", "thorn_lash", "sweep");

    /** Ids that must never be cast even if they match an attack word (they alter the world, the owner or other people's items). */
    private static final Set<String> DENY = Set.of("summon_grimoire", "store_weapon", "draw_weapon", "mana_zone", "mana_strike_zone", "devil_union",
            "devil_dive", "spirit_dive", "spirit_channeling", "reversal", "reversal_living", "black_hole", "stolen_time", "moment_reprise",
            "chrono_stasis", "chrono_anastasis", "gravity_well", "gravity_sphere", "random_seal", "grand_seal", "trinity_seal", "seal", "domination",
            "endless_domination", "life_exchange", "reincarnation", "red_thread", "world_end_church", "world_end_martyr", "doom_s_gate",
            "fallen_angel_gate", "underworld_gate", "gate_of_keys", "unlock", "master_key", "skeleton_keys", "key_armory");

    /** Fragments of ids that mean the page does something an Avatar must not do. */
    private static final List<String> DENY_WORDS = List.of("summon", "corpse", "reanimate", "domain", "armour", "armor", "heal", "mend", "regrowth",
            "rebirth", "restoration", "refresher", "cleanse", "seal", "gate", "door", "key", "world", "time", "stasis", "dominat", "swap", "exchange",
            "transfer", "banquet", "feast", "meal", "table", "platter", "garden", "grove", "kingdom", "voodoo", "clone", "double", "dice", "game",
            "gamble", "fate", "scan", "eye", "sight", "detect", "ride", "step", "dash", "blink", "teleport", "form", "titan", "colossus", "statue",
            "wall", "cage", "prison", "trap", "mines", "bind", "chain_cage", "shackle", "snare", "web", "mire", "pit", "zone", "field", "dome",
            "barrier", "ward", "veil", "shield", "aegis", "bulwark", "guard", "plating", "plate", "cradle", "cocoon", "ritual", "candle", "curse",
            "hex_eater", "transform", "mantle");

    /** The few self-buff pages that are fine for an Avatar (they only touch the caster). */
    private static final Set<String> UTILITY = Set.of("acceleration", "hardened_frame", "reinforcement", "flame_body", "gel_skin", "bubble_skin");

    /** Pages above this cost (percent of max magicule) are never cast: signature / zone pages are devastating or grief. */
    public static final double MAX_COST_PERCENT = 20.0;

    /**
     * Tensura magic an Avatar may learn and cast through {@code entity.TensuraCaster} (the mod's existing way for mobs to cast Tensura magic):
     * plain projectile / bolt attacks of the six elements. These are id keywords (TensuraCaster matches them inside the skill id); barrier, summon,
     * recovery, illusion, mental, space and gravity magic are not in the list on purpose. Taken from the class list of the Tensura API dump
     * (FireBallMagic, FireLanceMagic, StoneShotMagic, MudSpearsMagic, IcicleLanceMagic, IcicleSpearMagic, ThunderLanceMagic, ThunderOrbMagic,
     * WaterCutterMagic, WindCutterMagic); whether the registry ids really are these snake_case names is unverified without the game.
     */
    private static final List<String> TENSURA_KEYWORDS = List.of("fire_ball", "fire_lance", "stone_shot", "mud_spears", "icicle_lance", "icicle_spear",
            "thunder_lance", "thunder_orb", "water_cutter", "wind_cutter");

    public enum Kind { ATTACK, UTILITY, NONE }

    /** What an Avatar may do with a grimoire page of this id and cost (percent of max magicule). Matching is by whole id token ("pawn_march" has no "arc"). */
    public static Kind kind(String pageId, double costPercent) {
        if (pageId == null || pageId.isBlank()) return Kind.NONE;
        String id = pageId.toLowerCase(Locale.ROOT);
        if (DENY.contains(id) || costPercent > MAX_COST_PERCENT) return Kind.NONE;
        if (UTILITY.contains(id)) return Kind.UTILITY;
        String[] tokens = id.split("_");
        boolean strike = id.endsWith("_strike") || id.equals("strike");
        for (String t : tokens)
            for (String w : DENY_WORDS)
                if (t.startsWith(w) && !(strike && !HARD_DENY.contains(w))) return Kind.NONE;
        for (String t : tokens) {
            if (t.endsWith("ball")) return Kind.ATTACK;
            for (String w : ATTACK_WORDS) if (t.equals(w) || t.equals(w + "s")) return Kind.ATTACK;
        }
        return Kind.NONE;
    }

    /** Deny words that stay fatal even for a "..._strike" page (it would still summon, swap or transfer something). */
    private static final Set<String> HARD_DENY = Set.of("summon", "corpse", "reanimate", "heal", "transfer", "exchange", "swap", "dominat", "seal", "time", "stasis");

    public static boolean allowedPage(String pageId, double costPercent) { return kind(pageId, costPercent) != Kind.NONE; }

    /** Is this Tensura skill id (path part) on the Avatar allowlist? */
    public static boolean allowedTensuraSkill(String skillPath) {
        if (skillPath == null) return false;
        String p = skillPath.toLowerCase(Locale.ROOT);
        for (String k : TENSURA_KEYWORDS) if (p.contains(k)) return true;
        return false;
    }

    /** The keywords to hand to {@code TensuraCaster.learn}. */
    public static String[] tensuraKeywords() { return TENSURA_KEYWORDS.toArray(new String[0]); }
}
