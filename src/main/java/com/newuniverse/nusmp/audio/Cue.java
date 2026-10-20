package com.newuniverse.nusmp.audio;

/**
 * Why: the eight moments of a mob's life that have a voice of their own. Every mob family of the dungeon has one sound event per cue ({@code mob.<family>.<cue>}, see
 * {@link MobSoundSet}), so no mob has to fall back on a vanilla zombie, skeleton or pillager sound. The order is the order of the generator's table
 * ({@code tools/gen_mob_sounds.py}), and the ids are the last word of the event and of the files ({@code ambient_1.ogg}).
 */
public enum Cue {
    /** Idle: a mutter, a rattle, a whisper, now and then. */
    AMBIENT("ambient"),
    /** It spotted a target: a shout, a shriek, a roar. */
    AGGRO("aggro"),
    /** A blow lands: a grunt, a bite, a clang. */
    ATTACK("attack"),
    /** It is hurt. */
    HURT("hurt"),
    /** It dies. */
    DEATH("death"),
    /** One footstep. */
    STEP("step"),
    /** A spell, a breath or an ability winds up. */
    CAST("cast"),
    /** The family's signature taunt: the goblin's cackle, the lich's whispered laugh, the dragon's chuckle. */
    TAUNT("taunt");

    private final String id;

    Cue(String id) { this.id = id; }

    public String id() { return id; }

    /** The cue with this id, or null. */
    public static Cue byId(String id) {
        for (Cue c : values()) if (c.id.equals(id)) return c;
        return null;
    }
}
