package com.newuniverse.nusmp.audio.music;

/**
 * Why: the server's decision "which layer is this player in" kept as a pure function of a few facts, so the order of the rules is written down and tested once instead of
 * hidden in a tick handler. The order is: the Hub is always the Hub (it is a peaceful dimension); a boss close by beats everything else in the dungeon (the fight IS the boss);
 * a rift pocket keeps its own sound unless a boss is there; a safe room is safe even if something is still hunting the player outside its door (the guard keeps hostiles out);
 * otherwise enough hunters means BATTLE and nothing means EXPLORE. How long a fight lingers after the last hunter is the client's business ({@link MusicDirector}).
 */
public final class MusicRules {
    private MusicRules() {}

    /** How many monsters must be hunting the player for the fight layer. */
    public static final int BATTLE_HUNTERS = 1;
    /** How far (blocks) a boss counts as "close" (the scan box is a little wider, the distance is checked). */
    public static final double BOSS_RANGE = 40.0;
    /** How far (blocks) a monster that targets the player still counts as a hunter. */
    public static final double HUNTER_RANGE = 28.0;

    public static MusicLayer decide(boolean hub, boolean bossNear, boolean rift, boolean safe, int hunters) {
        if (hub) return MusicLayer.HUB;
        if (bossNear) return MusicLayer.BOSS;
        if (rift) return MusicLayer.RIFT;
        if (safe) return MusicLayer.SAFE;
        if (hunters >= BATTLE_HUNTERS) return MusicLayer.BATTLE;
        return MusicLayer.EXPLORE;
    }
}
