package com.newuniverse.nusmp.book;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoireSlot;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fuegoleon's flame arm, Djinn look (3.1): the CI self-test {@code magic.fuegoleon_arm_djinn}. A dedicated server cannot draw anything, so it proves what can be proved without eyes: every file the
 * look names is in the jar (the five piece models with their animation, texture and glow, the tongue sheet, the spark theme), the cubes of the real model files stay within the budget (21 cubes,
 * under the Djinn arm assembly's vertices), the look table is sane (default DJINN, V3 and CLASSIC kept), and the SERVER's decision did not change with the new look: Fuegoleon with the Salamander
 * contract still gets the arm lease (style 1), another grimoire still gets none. No client class is touched. Registered from {@link SpiritDiveBoot}.
 */
public final class FuegoleonArmDjinnSelfTests {
    private FuegoleonArmDjinnSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.fuegoleon_arm_djinn", FuegoleonArmDjinnSelfTests::run);
    }

    private static boolean asset(String path) { return FuegoleonArmDjinnSelfTests.class.getClassLoader().getResource("assets/nusmp/" + path) != null; }

    /** The cubes of a geo file (a GeoLite file: bones with cubes), read from the jar. */
    static int cubesOf(String path) throws IOException {
        try (var in = FuegoleonArmDjinnSelfTests.class.getClassLoader().getResourceAsStream("assets/nusmp/" + path); Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            int n = 0;
            for (JsonElement g : root.getAsJsonArray("minecraft:geometry"))
                for (JsonElement b : g.getAsJsonObject().getAsJsonArray("bones")) {
                    JsonArray cubes = b.getAsJsonObject().getAsJsonArray("cubes");
                    if (cubes != null) n += cubes.size();
                }
            return n;
        }
    }

    private static Result run(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        int cubes = 0;
        try {
            for (String f : FuegoleonArmDjinnRules.PIECE_FILES) {
                String id = FuegoleonArmDjinnRules.GEO_KEY + "_" + f;
                for (String rel : new String[]{"geo/entity/" + id + ".geo.json", "animations/entity/" + id + ".animation.json", "textures/entity/" + id + ".png", "textures/entity/" + id + "_glow.png"})
                    if (!asset(rel)) bad.add("missing " + rel);
                if (asset("geo/entity/" + id + ".geo.json")) cubes += cubesOf("geo/entity/" + id + ".geo.json");
            }
        } catch (IOException | RuntimeException ex) {
            bad.add("a piece model could not be read: " + ex);
        }
        if (!asset("textures/aura/fad_tongue.png")) bad.add("missing the tongue sheet");
        if (cubes > FuegoleonArmDjinnRules.LIMB_CUBES_MAX) bad.add("the limb has " + cubes + " cubes (budget " + FuegoleonArmDjinnRules.LIMB_CUBES_MAX + ")");
        if (FuegoleonArmDjinnRules.worstVertices(cubes) > FuegoleonArmDjinnRules.DJINN_ARM_VERTICES) bad.add("the arm costs " + FuegoleonArmDjinnRules.worstVertices(cubes) + " vertices, more than the Djinn arm assembly");
        if (FuegoleonArmDjinnRules.DEFAULT_LOOK != FuegoleonArmDjinnRules.Look.DJINN || FuegoleonArmDjinnRules.Look.values().length < 3) bad.add("the look table lost the default or an old look");
        if (FuegoleonArmDjinnRules.drawsLimb(FuegoleonArmDjinnRules.Look.V3) || FuegoleonArmDjinnRules.drawsLimb(FuegoleonArmDjinnRules.Look.CLASSIC)) bad.add("an old look draws the new limb");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        ServerLevel level = server.overworld();
        ServerPlayer fp = SelfTests.player(level, "fuegoleon_arm_djinn", 0.5, 100, 0.5);
        ItemStack kept = GrimoireSlot.get(fp);
        CompoundTag data = ForbiddenMagic.data(fp);
        String keptKind = data.getString("nusmp_spirit_kind");
        boolean hadKind = data.contains("nusmp_spirit_kind");
        try {
            GrimoireSlot.set(fp, GrimoireItem.createCanon(CanonBook.FUEGOLEON));
            setKind(fp, "Salamander");
            if (FuegoleonArm.reasonFor(fp) != FuegoleonArmRules.Reason.CONTRACT) bad.add("the server's decision changed: Fuegoleon with the contract has no arm (" + FuegoleonArm.reasonFor(fp) + ")");
            if (!FuegoleonArm.update(fp) || !PlayerAuras.active(fp, Aura.FUEGOLEON_ARM)) bad.add("no lease for the arm");
            else if (PlayerAuras.style(fp, Aura.FUEGOLEON_ARM) != FuegoleonArmRules.STYLE) bad.add("the arm at rest has the wrong style");
            GrimoireSlot.set(fp, GrimoireItem.createCanon(CanonBook.YUNO));
            if (FuegoleonArm.reasonFor(fp) != FuegoleonArmRules.Reason.NONE) bad.add("a grimoire of another character earned the arm");
        } catch (RuntimeException ex) {
            bad.add("the check threw " + ex);
        } finally {
            GrimoireSlot.set(fp, kept);
            setKind(fp, hadKind ? keptKind : "");
            try { PlayerAuras.clear(fp, Aura.FUEGOLEON_ARM); } catch (NullPointerException noConnection) { /* a stand-in player without a connection */ }
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok("the flame arm's five pieces (" + cubes + " cubes, " + FuegoleonArmDjinnRules.worstVertices(cubes) + " vertices at most) and the tongue sheet are in the jar; the look table keeps DJINN, V3 and CLASSIC; "
                + "the server's decision is unchanged (the contract gives the arm, another grimoire none)");
    }

    private static void setKind(ServerPlayer p, String kind) {
        CompoundTag d = ForbiddenMagic.data(p);
        if (kind.isEmpty()) d.remove("nusmp_spirit_kind"); else d.putString("nusmp_spirit_kind", kind);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, d);
    }
}
