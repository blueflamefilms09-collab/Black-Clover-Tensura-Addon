package com.newuniverse.nusmp.audio;

/**
 * Why: a mob class that knows which family of {@link MobSoundSet} it sounds like says so here, so {@link MobVoice#familyOf} and the vanilla-voice remap ({@link MobSoundRemap})
 * ask one question of every entity. The dungeon creatures (living armour, direwolves, basilisks, slimes, the Warden and the Guardian) and the Foundry mob implement it; an entity that
 * does not is looked up by its type id ({@link MobSoundSet#forEntityType}).
 */
public interface SoundFamilied {
    /** The id of the family in {@link MobSoundSet} this mob sounds like (never null: unknown ids fall back to the default family). */
    String soundFamily();
}
