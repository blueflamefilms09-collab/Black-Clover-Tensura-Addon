package com.newuniverse.nusmp.avatar;

/** Contract stub (AV2 replaces the body): {@code say} broadcasts "[Name]: message" to players within 30 blocks. Safe to call from the server thread only. */
public final class AvatarChatterEngine {
    private AvatarChatterEngine() {}

    public static void say(AvatarEntity avatar, ChatEvent event) {}

    /** {@code arg} is the event's subject: a skill name for SKILL_CAST, a target name for COMBAT_START. May be null. */
    public static void say(AvatarEntity avatar, ChatEvent event, String arg) {}
}
