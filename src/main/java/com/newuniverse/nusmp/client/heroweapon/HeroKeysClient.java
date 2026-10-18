package com.newuniverse.nusmp.client.heroweapon;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.bond.HeroNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the hero weapon's five keys, under Controls > Multiverse of Anime. Z, X and C cast the three techniques of the active form (a press; while a key stays
 * down the client sends a heartbeat every ten ticks so the server knows it is held, and one packet when it is let go); B summons the weapon or puts it away; N
 * changes to the next learned form. The keys already used by the mod are V, R, G, J, K, H, Y, U, the arrow keys, Home and End, and none of them is reused here
 * (the Weapon Book's key, M, is the forms system's). The client only says what was pressed: the server decides what it means, re-checks everything, and
 * answers a repeated press at most every few ticks. With no hero weapon bound the keys do nothing and send nothing.
 */
public final class HeroKeysClient {
    private HeroKeysClient() {}

    private static final String CATEGORY = "key.categories.nusmp";

    public static final KeyMapping SKILL_1 = new KeyMapping("key.nusmp.hero_skill_1", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping SKILL_2 = new KeyMapping("key.nusmp.hero_skill_2", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping SKILL_3 = new KeyMapping("key.nusmp.hero_skill_3", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_C, CATEGORY);
    /** Summon the weapon, or put it away. */
    public static final KeyMapping SUMMON = new KeyMapping("key.nusmp.hero_summon", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_B, CATEGORY);
    /** Change to the next learned form. */
    public static final KeyMapping CYCLE = new KeyMapping("key.nusmp.hero_cycle", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_N, CATEGORY);

    private static final KeyMapping[] SKILLS = {SKILL_1, SKILL_2, SKILL_3};
    private static final KeyMapping[] ALL = {SKILL_1, SKILL_2, SKILL_3, SUMMON, CYCLE};
    /** Ticks between two heartbeats of a held technique key. */
    private static final int BEAT = 10;

    private static final boolean[] DOWN = new boolean[3];
    private static final int[] SINCE = new int[3];

    /** Mod bus: puts the keys in the Controls screen. */
    public static void register(RegisterKeyMappingsEvent e) {
        for (KeyMapping k : ALL) e.register(k);
    }

    /** The name the player sees for hero key {@code i}: 0 to 2 the technique keys, 3 summon, 4 change form. */
    public static String name(int i) { return i >= 0 && i < ALL.length ? ALL[i].getTranslatedKeyMessage().getString() : "?"; }

    /** Game bus, client: turns presses into packets. */
    public static void onClientTick(ClientTickEvent.Post e) {
        SafeCast.client("hero.keys", HeroKeysClient::tick);       // a fault is logged once and never reaches the client tick
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isPaused()) ClientHeroBond.tick();      // the server's clock stands still in a paused single-player game, so the HUD's bars must too
        if (mc.player == null) {
            reset();
            return;
        }
        boolean act = mc.screen == null && !mc.player.isSpectator() && ClientHeroBond.bound();
        for (int i = 0; i < SKILLS.length; i++) {
            KeyMapping k = SKILLS[i];
            while (k.consumeClick()) {
                if (!act) continue;
                PacketDistributor.sendToServer(new HeroNet.Key(i, HeroNet.PRESS));
                DOWN[i] = true;
                SINCE[i] = 0;
            }
            if (!DOWN[i]) continue;
            if (!act || !k.isDown()) {
                PacketDistributor.sendToServer(new HeroNet.Key(i, HeroNet.RELEASE));
                DOWN[i] = false;
            } else if (++SINCE[i] >= BEAT) {
                PacketDistributor.sendToServer(new HeroNet.Key(i, HeroNet.HOLD));
                SINCE[i] = 0;
            }
        }
        while (SUMMON.consumeClick()) if (act) PacketDistributor.sendToServer(new HeroNet.Act(HeroNet.ACT_TOGGLE));
        while (CYCLE.consumeClick()) if (act) PacketDistributor.sendToServer(new HeroNet.Act(HeroNet.ACT_CYCLE));
    }

    private static void reset() {
        for (int i = 0; i < DOWN.length; i++) {
            DOWN[i] = false;
            SINCE[i] = 0;
        }
    }
}
