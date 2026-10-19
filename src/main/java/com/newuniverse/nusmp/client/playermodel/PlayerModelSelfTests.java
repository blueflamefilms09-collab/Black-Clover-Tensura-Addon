package com.newuniverse.nusmp.client.playermodel;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;

import java.util.List;

/**
 * Why: nothing of the player-model kit can be seen by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs
 * {@code render.playermodel}, which reads the kit's data out of the jar exactly as the client would ({@link PlayerModelAudit}): the hair table parses, every style's sheet exists and every box is
 * inside it with the scalp cap on the hat layer's UV, every look's skin (and glow map) exists at 64x64, every haircut it names exists, every extras model has its {@code x_<anchor>} roots and
 * textures, every look a body asks for by name is in the table, every converted arc boss has a look, and every old avatar hair id maps to a haircut. It touches no client class, so it runs on
 * the dedicated server; the drawing itself is checked by the unit tests of the pure parts and by the preview images (docs/playermodel.md). Registered from the mod constructor.
 */
public final class PlayerModelSelfTests {
    private PlayerModelSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("render.playermodel", server -> {
            List<String> bad = PlayerModelAudit.run(PlayerModelAudit::classpath);
            if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problems, first: " + bad.get(0));
            return SelfTests.Result.ok("hair, skins, glow maps, extras and legacy hair ids all resolve");
        });
    }
}
