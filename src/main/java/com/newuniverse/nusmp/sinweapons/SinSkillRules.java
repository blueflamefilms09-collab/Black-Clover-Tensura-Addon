package com.newuniverse.nusmp.sinweapons;

/**
 * Why: the rules of the five Berserk weapon skills (Lord of Slothful Weight, Lord of Green-Eyed Envy, Lord of Empty Vanity, Lord of the Twin Rings, Seifort's Holy Judgement) in one pure class (no game types) so a
 * test can pin every decision: who a stack belongs to, may a stack's technique be used, when an unbound stack is bound, and whether the Manifest mode stores, forges or does nothing. The weapon is the same item as
 * before ({@link SinWeaponItem}); the skill is the way it is built (forged) and kept (stored). The owner is a UUID text in the stack's custom data under {@link #OWNER_TAG}, so no new data component exists.
 * The work is {@link SinSkillService}, on the server; the skill is {@code skill.SinWeaponSkill}.
 */
public final class SinSkillRules {
    private SinSkillRules() {}

    /** The custom-data tag of a stack that holds its owner's UUID as text. */
    public static final String OWNER_TAG = "sin_owner";
    /** The least ticks between two forges (and after a store) of one player and one weapon. */
    public static final int FORGE_COOLDOWN_TICKS = 100;
    /** One look at the inventory per player per this many ticks (staggered by entity id); at most {@link #MAX_SLOTS} slots are read. */
    public static final int SCAN_PERIOD = 20, MAX_SLOTS = 41;

    /** The skill id of each weapon (the registry id in NUSkills and the name of its icon file textures/skill/&lt;id&gt;.png). */
    public static String skillId(SinRules.Kind k) {
        return switch (k) {
            case SLOTH -> "lord_of_slothful_weight";
            case ENVY -> "lord_of_green_eyed_envy";
            case VANITY -> "lord_of_empty_vanity";
            case CHAKRAMS -> "lord_of_the_twin_rings";
            case HOLY -> "seifort_holy_judgement";
        };
    }

    /** Whose a stack is, seen from one player: nobody's yet, theirs, or another player's. */
    public enum Owner { UNBOUND, MINE, OTHER }

    /** {@code stored} is the text in the stack's tag (null or empty = none), {@code me} the player's UUID text. */
    public static Owner ownerOf(String stored, String me) {
        if (stored == null || stored.isEmpty()) return Owner.UNBOUND;
        return stored.equals(me) ? Owner.MINE : Owner.OTHER;
    }

    /** May this stack's right-click technique (or the skill's Technique mode) be used? With the setting soulBound off anyone may; otherwise a stack bound to another player is a plain melee weapon. */
    public static boolean mayUseTechnique(boolean soulBound, Owner o) { return !soulBound || o != Owner.OTHER; }

    /** Is an unbound stack bound to its carrier now? Only if the carrier has no bound stack of that weapon yet (one bound weapon per player per kind). */
    public static boolean shouldBind(Owner o, boolean carrierAlreadyHasOne) { return o == Owner.UNBOUND && !carrierAlreadyHasOne; }

    /** What the Manifest mode does. */
    public enum Manifest { STORE, ALREADY_CARRIED, FORGE, WAIT }

    /** The Manifest / Store mode: the bound weapon in the main hand goes back into the skill; else one carried somewhere stays as it is; else a fresh one is forged when the cooldown allows. */
    public static Manifest manifest(boolean holdsMineInMainHand, boolean carriesMine, long now, long readyAt) {
        if (holdsMineInMainHand) return Manifest.STORE;
        if (carriesMine) return Manifest.ALREADY_CARRIED;
        return forgeReady(now, readyAt) ? Manifest.FORGE : Manifest.WAIT;
    }

    /** True when the cooldown is over (a clock set back far does not lock the player out for ever). */
    public static boolean forgeReady(long now, long readyAt) { return now >= readyAt || readyAt - now > FORGE_COOLDOWN_TICKS; }

    public static long nextForgeAt(long now) { return now + FORGE_COOLDOWN_TICKS; }

    /** Is this tick this player's turn for the inventory look? (one tick in {@link #SCAN_PERIOD}, spread by entity id) */
    public static boolean scanTurn(int tickCount, int entityId) { return Math.floorMod(tickCount + entityId, SCAN_PERIOD) == 0; }

    /** The slots one look reads: the whole inventory, never more than {@link #MAX_SLOTS}. */
    public static int slotsScanned(int containerSize) { return Math.max(0, Math.min(containerSize, MAX_SLOTS)); }
}
