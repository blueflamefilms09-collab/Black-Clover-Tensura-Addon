package com.newuniverse.nusmp.labyrinth.mega.system;

/**
 * Why: "which dead creature may become a shadow" is a list of exclusions the owner stated in the brief (bosses, summons of other owners, named and persistent
 * mobs are never extractable) plus the sense ones (it must be hostile, able to fight, and have been slain by a player on a floor of the dungeon, on either road since 1.58.0). Deciding that from
 * plain facts, in one pure function that answers WHY NOT in a word, keeps the rule testable case by case, lets the command and the chat line say what is wrong
 * with a target, and keeps the game side down to reading each fact off the entity once. The first reason that applies wins, in the order of the list below.
 */
public final class ExtractRules {
    private ExtractRules() {}

    /**
     * The facts about a slain creature.
     *
     * @param killedByPlayer a player (not a minion, not the environment) struck the last blow
     * @param holyRoad       it died inside a sector of a road the System covers (every road since 1.58.0; the name is the 1.55.0 one, kept so callers compile)
     * @param enemy          it is a hostile monster (a vanilla {@code Enemy}, or a monster of the Dungeon Master, whose Tensura creatures are not {@code Enemy})
     * @param mob            it is a {@code Mob} that can carry AI
     * @param armed          it can fight (has an attack damage attribute or a ranged attack)
     * @param boss           a boss by any of the mod's definitions
     * @param servant        somebody's servant already (a minion, a Paladin, a Soul Corpse, a shadow soldier, a relic summon, a tamed pet)
     * @param named          it carries a custom name (a named creature is somebody)
     * @param persistent     it is flagged persistent (summons and event creatures are; ordinary dungeon monsters are not)
     * @param forbiddenType  its entity type is on the datapack's refusal list
     */
    public record Facts(boolean killedByPlayer, boolean holyRoad, boolean enemy, boolean mob, boolean armed, boolean boss, boolean servant,
                        boolean named, boolean persistent, boolean forbiddenType) {}

    /** Null when the creature may be extracted, else one word naming the first rule it breaks. */
    public static String refusal(Facts f) {
        if (!f.killedByPlayer()) return "not slain by a player";
        if (!f.holyRoad()) return "not on a floor of the dungeon";
        if (!f.mob()) return "not a creature that can follow";
        if (!f.enemy()) return "not hostile";
        if (f.boss()) return "a boss";
        if (f.servant()) return "already someone's servant";
        if (f.named()) return "named";
        if (f.persistent()) return "persistent";
        if (f.forbiddenType()) return "forbidden by the datapack";
        if (!f.armed()) return "cannot fight";
        return null;
    }

    public static boolean allowed(Facts f) { return refusal(f) == null; }
}
