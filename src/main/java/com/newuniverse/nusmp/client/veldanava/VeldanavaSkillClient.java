package com.newuniverse.nusmp.client.veldanava;

import net.minecraft.client.Minecraft;

/**
 * Why: the client end of {@code nusmp:veldanava_skills_open}, kept in a client-only class so a dedicated server never loads screen code. It only opens the card screen; the choice goes back to the
 * server as {@code nusmp:veldanava_skills_claim} and the server decides.
 */
public final class VeldanavaSkillClient {
    private VeldanavaSkillClient() {}

    public static void onOpen(String chosen) {
        Minecraft.getInstance().setScreen(new VeldanavaSkillScreen(chosen == null ? "" : chosen));
    }
}
