package com.newuniverse.nusmp.vupgrade;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.particle.fx.FxCatalog;
import com.newuniverse.nusmp.particle.fx.FxConfig;
import com.newuniverse.nusmp.particle.fx.FxTypes;
import com.newuniverse.nusmp.particle.fx.ParticleNearCap;
import com.newuniverse.nusmp.particle.fx.TintedParticleOptions;
import com.newuniverse.nusmp.particle.fx.UpgradeParticleAlias;
import com.newuniverse.nusmp.particle.fx.UpgradeParticleOptions;
import com.newuniverse.nusmp.perf.UpgradeBudget;
import com.newuniverse.nusmp.perf.VfxQuality;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Why: 1.110.0 moved every particle of the upgraded weapons and relics onto the mod's own particles and put a hard budget in front of them. The self-test {@code visual.upgrade_particles} (run
 * by the CI server smoke test; it touches no client class, so it runs on the dedicated server) checks inside the booted mod that every alias target is a REGISTERED custom particle type, that
 * the options built for a table token are the mod's tinted options of such a type (never a vanilla one), that the shipped tables of a few upgraded items name only custom particles, and that
 * the budget maths and the first-person size cap answer what the owner asked for (a default density of 0.5, nothing beyond 16 blocks, nothing at the eye).
 */
public final class UpgradeParticlesSelfTests {
    private UpgradeParticlesSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("visual.upgrade_particles", UpgradeParticlesSelfTests::run);
    }

    private static final String[] TABLES = {"blackrazor", "demon_slayer_sword", "elsdocia", "aurliels_seraph_halberd", "djinn_weapon_bael_dark", "white_whistle"};
    private static final String[] TOKENS = {"fx", "dust", "minecraft:end_rod", "minecraft:flame", "flame", "minecraft:cloud", "minecraft:witch", "minecraft:soul_fire_flame", "minecraft:glow", "electric_spark",
            "nusmp:ember", "ember", "no_such:particle", "", "minecraft:explosion", "minecraft:note"};

    private static SelfTests.Result run(MinecraftServer server) throws IOException {
        for (String t : UpgradeParticleAlias.targets()) {
            if (FxTypes.type(t) == null) return SelfTests.Result.fail("the alias target " + t + " is not a registered custom particle type");
        }
        for (String token : TOKENS) {
            ParticleOptions o = UpgradeParticleOptions.of(token, 0x9A4AFF, 2.0f);
            if (!(o instanceof TintedParticleOptions t)) return SelfTests.Result.fail("the token '" + token + "' gave " + o + ", not the mod's own particle options");
            if (t.getType() == null || !FxCatalog.has(idOf(t))) return SelfTests.Result.fail("the token '" + token + "' gave a type that is not in the catalog");
            if (t.scale() > UpgradeParticleOptions.MAX_SCALE + 0.02f) return SelfTests.Result.fail("the token '" + token + "' gave a scale of " + t.scale() + " (the cap is " + UpgradeParticleOptions.MAX_SCALE + ")");
        }
        int tokens = 0;
        for (String id : TABLES) {
            String text = text("/assets/nusmp/visual_upgrade/" + id + ".json");
            if (text == null) return SelfTests.Result.fail(id + ": the table is not in the jar");
            tokens += walk(JsonParser.parseString(text), id);
        }
        if (FxConfig.upgradeDensity() < 0f || FxConfig.upgradeDensity() > 1f) return SelfTests.Result.fail("particles.upgradeDensity is out of 0..1: " + FxConfig.upgradeDensity());
        float def = UpgradeBudget.scale(UpgradeBudget.DEFAULT_DENSITY, VfxQuality.HIGH, 60, 4.0, 1.0, true);
        if (Math.abs(def - 0.5f) > 1e-4f) return SelfTests.Result.fail("the default density does not halve the table rates: " + def);
        if (UpgradeBudget.scale(1f, VfxQuality.HIGH, 60, UpgradeBudget.OTHERS_MAX_DISTANCE + 1, 1.0, false) != 0f) return SelfTests.Result.fail("somebody else's item still spawns beyond 16 blocks");
        if (UpgradeBudget.scale(1f, VfxQuality.HIGH, UpgradeBudget.MIN_FPS - 1, 4.0, 1.0, true) != 0f) return SelfTests.Result.fail("the effects still spawn under the minimum frame rate");
        UpgradeBudget b = new UpgradeBudget();
        int cap = UpgradeBudget.liveCap(VfxQuality.HIGH, 400), granted = 0;
        for (long tick = 0; tick < 200; tick++) for (int holder = 0; holder < 30; holder++) granted += b.take(holder, 1, 1000 + tick, 30, VfxQuality.HIGH, 400);
        if (granted > 30 * 200 * UpgradeBudget.perItemPerSecond(VfxQuality.HIGH) / 20 + 30 * 8) return SelfTests.Result.fail("thirty greedy holders were granted " + granted + " in ten seconds");
        if (b.live(1199) > cap) return SelfTests.Result.fail("the live cap " + cap + " was passed: " + b.live(1199));
        if (ParticleNearCap.cap(0.5f, 0.2 * 0.2) != 0f || ParticleNearCap.cap(0.14f, 25.0) < 0.139f) return SelfTests.Result.fail("the first-person size cap does not hide a particle at the eye and keep one far away");
        return SelfTests.Result.ok(UpgradeParticleAlias.targets().size() + " alias targets registered, " + TOKENS.length + " tokens give the mod's own options, " + tokens + " table tokens custom, default density 0.5, live cap " + cap + ", " + granted + " granted to a crowd of 30");
    }

    private static String idOf(TintedParticleOptions t) {
        for (Map.Entry<String, ?> e : FxTypes.holders().entrySet()) if (FxTypes.type(e.getKey()) == t.getType()) return e.getKey();
        return "";
    }

    private static int walk(JsonElement e, String where) {
        int n = 0;
        if (e.isJsonObject()) {
            for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
                if (en.getKey().equals("particle") && en.getValue().isJsonPrimitive() && en.getValue().getAsJsonPrimitive().isString()) {
                    String token = en.getValue().getAsString();
                    String id = UpgradeParticleAlias.resolve(token);
                    if (!token.equals("fx") && !token.startsWith("nusmp:")) throw new IllegalStateException(where + ": the token '" + token + "' is not a custom one (run tools/visual_upgrade/custom_particles.py)");
                    if (id != null && !FxCatalog.has(id)) throw new IllegalStateException(where + ": '" + token + "' resolves to " + id + ", which is not a catalog particle");
                    n++;
                } else {
                    n += walk(en.getValue(), where);
                }
            }
        } else if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) n += walk(x, where);
        }
        return n;
    }

    private static String text(String path) throws IOException {
        try (InputStream in = UpgradeParticlesSelfTests.class.getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
