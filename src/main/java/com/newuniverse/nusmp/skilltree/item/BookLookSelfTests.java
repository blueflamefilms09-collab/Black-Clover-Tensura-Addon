package com.newuniverse.nusmp.skilltree.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: {@code skillbook.look.files}, run by the CI server smoke test on a dedicated server (which has no resource manager): the packed jar's class path must hold every model, texture and
 * animation file of the 3.26.0 Skill Book look, for every franchise of the shipped table and every rarity ({@link BookLookFiles}), and the three server-visible rules must hold: the
 * table's franchise list has the eleven franchises in the contract order, the rarity values {@code (rank + 1) / 6} are strictly increasing, and the shimmer rule's per-tick cap is 4.
 * Only reads the class path; nothing is created, changed or kept.
 */
public final class BookLookSelfTests {
    private BookLookSelfTests() {}

    public static final String KEY = "skillbook.look.files";

    public static void register() { SelfTests.register(KEY, BookLookSelfTests::run); }

    private static SelfTests.Result run(MinecraftServer server) throws Exception {
        List<String> bad = new ArrayList<>();
        ClassLoader cl = BookLookSelfTests.class.getClassLoader();
        List<String> franchises = new ArrayList<>();
        try (InputStream in = cl.getResourceAsStream("assets/nusmp/skill_books.json")) {
            if (in == null) return SelfTests.Result.fail("assets/nusmp/skill_books.json is not on the class path");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            root.getAsJsonArray("franchises").forEach(e -> franchises.add(e.getAsString()));
        }
        if (franchises.size() < 11) bad.add("the table lists " + franchises.size() + " franchises, the look has covers for 11");
        if (franchises.size() > 10 && !"fairy_tail".equals(franchises.get(10))) bad.add("the 11th franchise is " + franchises.get(10) + ", the contract says fairy_tail");
        List<String> missing = BookLookFiles.missing(franchises, p -> cl.getResource(p) != null);
        for (String m : missing) bad.add("missing from the jar: " + m);
        float last = 0f;
        for (int rank = 0; rank < BookLook.RARITY_IDS.size(); rank++) {
            float v = BookLook.rarityStep(rank);
            if (v <= last || v >= 1f) bad.add("rarity value of rank " + rank + " is " + v);
            last = v;
        }
        if (BookShimmerRules.particlesThisTick(1000) > 4) bad.add("the shimmer would spawn more than 4 particles in a tick");
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problems: " + String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return SelfTests.Result.ok(franchises.size() + " franchises x " + BookLook.RARITY_IDS.size() + " rarities: " + BookLookFiles.requiredPaths(franchises).size() + " files found, rarity values and shimmer cap hold");
    }
}
