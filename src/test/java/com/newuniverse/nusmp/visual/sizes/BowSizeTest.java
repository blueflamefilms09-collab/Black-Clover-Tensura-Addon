package com.newuniverse.nusmp.visual.sizes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (docs/heroweapon/bow_size.md): the owner found every bow tiny in the hand, in third and first person. {@link BowSize} brings each bow of the mod to a size of its tier. Data tests over the REAL mesh files:
 * every bow mesh of the mod (a class-bow id, its draw stages, the forms) is in the size table, so a bow added later without a size fails the build; every table entry has its mesh; in a third-person hand every
 * bow reads inside its tier's band (hero 1.3 to 1.6 blocks, the rest 1.2 to 1.5; vanilla's bow reads 1.08) and in first person between 0.9 and the camera cap of 1.2 (vanilla's 0.82), rest mesh and every draw stage
 * alike; the classic path (bigBows off) gives back exactly the sizes of before; the user multiplier moves the size but never past the first-person cap; and the model json of every bow still carries vanilla's
 * Bow display scale the arithmetic assumes.
 */
public class BowSizeTest {
    static final Path RES = Path.of("src", "main", "resources", "assets", "anime_multiverse");
    static final Pattern PLACE = Pattern.compile("(?m)^place\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)");
    static final Pattern STAGE = Pattern.compile("_pull[123]$");

    /** Bows that are not held with the vanilla-sized bow mesh and are NOT enlarged: the Djinn Barbatos and Sitri bows are 2.7 to 3.2 blocks in the hand already (a bow-class grip entry, not a bow-class weapon). */
    static final Set<String> BIG_ALREADY = Set.of("djinn_weapon_barbatos", "djinn_weapon_barbatos_dark", "djinn_weapon_sitri", "djinn_weapon_sitri_dark");

    static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    /** Tip-to-tip length of a mesh in blocks as WeaponRenderer.bowFactor measures it: the extent along y of every quad of layers 0 to 2 times the place scale (a few meshes reach past the place line's ymin). */
    public static double length(Path p) throws IOException {
        String text = Files.readString(p);
        Matcher pl = PLACE.matcher(text);
        if (!pl.find()) return 0;
        double scale = Double.parseDouble(pl.group(4)), lo = 1e9, hi = -1e9;
        for (String line : text.split("\n")) {
            String[] v = line.trim().split("\\s+");
            if (v.length < 22 || !(v[0].equals("q") || v[0].equals("qn")) || Integer.parseInt(v[1]) > 2) continue;
            int stride = v[0].equals("q") ? 5 : 8;
            for (int k = 0; k < 4; k++) {
                double y = Double.parseDouble(v[2 + k * stride + 1]);
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
            }
        }
        return (hi - lo) * scale;
    }

    /** Every top-level mesh file of a bow of the mod: id of the file to its rest id. */
    static Map<String, String> bowMeshes() throws IOException {
        Map<String, String> classes = new TreeMap<>();
        for (var e : json(RES.resolve("weapon_classes.json")).getAsJsonObject("weapons").entrySet()) classes.put(e.getKey(), e.getValue().getAsString());
        Map<String, String> out = new TreeMap<>();
        try (Stream<Path> s = Files.list(RES.resolve("weapon_meshes"))) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String n = p.getFileName().toString();
                if (!n.endsWith(".txt")) continue;
                String id = n.substring(0, n.length() - 4), rest = STAGE.matcher(id).replaceFirst("");
                boolean bowClass = "bow".equals(classes.get(rest));
                boolean bowName = rest.contains("bow") && !rest.contains("crossbow") && !rest.contains("rainbow") && !rest.contains("bowl");
                if (bowClass || bowName) out.put(id, rest);
            }
        }
        return out;
    }

    static String f(double v) { return String.format(Locale.ROOT, "%.2f", v); }

    @Test
    void everyBowMeshOfTheModIsInTheSizeTable() throws IOException {
        List<String> bad = new ArrayList<>();
        for (var e : bowMeshes().entrySet())
            if (BowSize.tier(e.getKey()) == null) bad.add(e.getKey() + " is a bow mesh (rest id " + e.getValue() + ") that is not in BowSize's table: add it with a tier, or it stays tiny in the hand");
        assertTrue(bad.isEmpty(), String.join("\n", bad));
        Set<String> rests = new TreeSet<>(bowMeshes().values());
        assertTrue(rests.size() >= 21, "the bows of the mod are found: " + rests);
    }

    @Test
    void everyTableEntryIsARealBowWithItsMesh() throws IOException {
        JsonObject weapons = json(RES.resolve("weapon_classes.json")).getAsJsonObject("weapons");
        List<String> bad = new ArrayList<>();
        for (String id : BowSize.ids()) {
            if (!Files.exists(RES.resolve("weapon_meshes/" + id + ".txt"))) bad.add(id + " is in the size table but has no weapon_meshes/" + id + ".txt");
            if (!weapons.has(id) || !"bow".equals(weapons.get(id).getAsString())) bad.add(id + " is in the size table but is not class bow in weapon_classes.json");
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    /** A bow-class grip entry (the Djinn bows) that the table leaves out must already be bigger than a hero bow: it needs no enlarging. */
    @Test
    void aBowLeftOutOfTheTableIsAlreadyBig() throws IOException {
        JsonObject classOf = json(RES.resolve("weapon_grips.json")).getAsJsonObject("classOf");
        List<String> bad = new ArrayList<>();
        for (var e : classOf.entrySet()) {
            if (!"bow".equals(e.getValue().getAsString()) || BowSize.tier(e.getKey()) != null) continue;
            if (!BIG_ALREADY.contains(e.getKey())) { bad.add(e.getKey() + " is a bow-class grip entry that is neither in the size table nor in BIG_ALREADY"); continue; }
            double apparent = length(RES.resolve("weapon_meshes/" + e.getKey() + ".txt")) * 0.85;
            if (apparent < 1.6) bad.add(e.getKey() + " is left out of the table but reads only " + f(apparent) + " blocks in the hand");
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void everyBowReadsBigInThirdAndFirstPerson() throws IOException {
        List<String> bad = new ArrayList<>();
        for (var e : bowMeshes().entrySet()) {
            BowSize.Tier tier = BowSize.tier(e.getKey());
            if (tier == null) continue;
            double rest = length(RES.resolve("weapon_meshes/" + e.getValue() + ".txt")), own = length(RES.resolve("weapon_meshes/" + e.getKey() + ".txt"));
            float tp = BowSize.apparent(e.getKey(), (float) rest, (float) own, false, true, 1f), fp = BowSize.apparent(e.getKey(), (float) rest, (float) own, true, true, 1f);
            if (tp < tier.min || tp > tier.max) bad.add(e.getKey() + " reads " + f(tp) + " blocks in a third-person hand (" + tier + " band " + tier.min + " to " + tier.max + ")");
            if (fp < 0.9 || fp > BowSize.FP_MAX_APPARENT + 1e-4) bad.add(e.getKey() + " reads " + f(fp) + " blocks in a first-person hand (0.9 to " + BowSize.FP_MAX_APPARENT + ")");
            if (!e.getKey().equals(e.getValue()) && Math.abs(own - rest) > 0.06 * rest) bad.add(e.getKey() + " is " + f(own) + " blocks long, its rest mesh " + f(rest) + ": the draw would change the size");
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    /** The genesis (Vu) bow draws its own meshes with its own renderer and is not scaled again: it must already read inside the band. */
    @Test
    void theGenesisBowAlreadyReadsInItsBand() throws IOException {
        for (String stage : new String[]{"", "_pull1", "_pull2", "_pull3"}) {
            double len = length(RES.resolve("weapon_meshes_vu/starforged_aeon_bow" + stage + ".txt"));
            BowSize.Tier t = BowSize.tier("starforged_aeon_bow");
            assertTrue(len * BowSize.TP_SCALE >= t.min && len * BowSize.TP_SCALE <= t.max, "genesis starforged_aeon_bow" + stage + " reads " + f(len * BowSize.TP_SCALE));
            assertTrue(len * BowSize.FP_SCALE >= 0.9 && len * BowSize.FP_SCALE <= BowSize.FP_MAX_APPARENT, "genesis starforged_aeon_bow" + stage + " first person " + f(len * BowSize.FP_SCALE));
        }
    }

    @Test
    void theClassicPathGivesBackTheSizesOfBefore() throws IOException {
        for (var e : bowMeshes().entrySet()) {
            double rest = length(RES.resolve("weapon_meshes/" + e.getValue() + ".txt")), own = length(RES.resolve("weapon_meshes/" + e.getKey() + ".txt"));
            assertEquals(1f, BowSize.factor(false, 1.7f, BowSize.baseFactor(e.getKey(), (float) rest), (float) own, false), 0f, e.getKey());
            assertEquals(own * BowSize.TP_SCALE, BowSize.apparent(e.getKey(), (float) rest, (float) own, false, false, 2f), 1e-4, e.getKey() + " third person");
            assertEquals(own * BowSize.FP_SCALE, BowSize.apparent(e.getKey(), (float) rest, (float) own, true, false, 2f), 1e-4, e.getKey() + " first person");
        }
        assertEquals(1f, BowSize.factor(false, 1f, 1.5f, 1.2f, true));
        assertEquals(0f, BowSize.baseFactor("magebane_longsword", 2.0f), "not a bow: no factor");
        assertEquals(1f, BowSize.factor(true, 1f, 0f, 2.0f, false), "not a bow: factor 1");
    }

    @Test
    void theUserScaleMovesTheSizeButNeverPastTheCamera() throws IOException {
        for (var e : bowMeshes().entrySet()) {
            if (BowSize.tier(e.getKey()) == null) continue;
            double rest = length(RES.resolve("weapon_meshes/" + e.getValue() + ".txt")), own = length(RES.resolve("weapon_meshes/" + e.getKey() + ".txt"));
            float one = BowSize.apparent(e.getKey(), (float) rest, (float) own, false, true, 1f), big = BowSize.apparent(e.getKey(), (float) rest, (float) own, false, true, BowSize.SCALE_MAX),
                    small = BowSize.apparent(e.getKey(), (float) rest, (float) own, false, true, BowSize.SCALE_MIN);
            assertEquals(one * BowSize.SCALE_MAX, big, 1e-3, e.getKey() + " scale 2.0 third person");
            assertEquals(one * BowSize.SCALE_MIN, small, 1e-3, e.getKey() + " scale 0.5 third person");
            float fp = BowSize.apparent(e.getKey(), (float) rest, (float) own, true, true, BowSize.SCALE_MAX);
            assertTrue(fp <= BowSize.FP_MAX_APPARENT + 1e-4, e.getKey() + " first person at scale 2.0 reads " + f(fp));
        }
    }

    @Test
    void theSettingsAreClampedAndTheStagesShareTheRestId() {
        BowSize.setScale(9.0);
        assertEquals(BowSize.SCALE_MAX, BowSize.userScale());
        BowSize.setScale(0.0);
        assertEquals(BowSize.SCALE_MIN, BowSize.userScale());
        BowSize.setScale(Double.NaN);
        assertEquals(1f, BowSize.userScale());
        BowSize.setScale(1.0);
        assertEquals("legendary_bow", BowSize.restId("legendary_bow_pull2"));
        assertEquals("legendary_bow_curse", BowSize.restId("legendary_bow_curse_pull3"));
        assertEquals("legendary_bow__scouts_bow", BowSize.restId("legendary_bow__scouts_bow_pull1"));
        assertEquals("legendary_bow_curse", BowSize.restId("legendary_bow_curse"));
        assertEquals(BowSize.Tier.HERO, BowSize.tier("legendary_bow_pull1"));
        assertEquals(BowSize.Tier.FORM, BowSize.tier("legendary_bow__sagittarius_pull3"));
        assertEquals(BowSize.Tier.RELIC, BowSize.tier("the_reaver"));
    }

    /** The arithmetic assumes vanilla's Bow display scale (0.9 third person, 0.68 first person) on every bow's model json. */
    @Test
    void everyBowModelCarriesVanillasBowDisplayScale() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String rest : new TreeSet<>(BowSize.ids())) {
            String item = rest.contains("__") ? rest.substring(0, rest.indexOf("__")) : rest.equals("legendary_bow_curse") ? "legendary_bow" : rest;      // a form and the curse look are drawn from the Legendary Bow item's model json
            Path p = RES.resolve("models/item/" + item + ".json");
            if (!Files.exists(p)) { bad.add(rest + " has no models/item/" + item + ".json"); continue; }
            JsonObject d = json(p).getAsJsonObject("display");
            for (String[] k : new String[][]{{"thirdperson_righthand", "0.9"}, {"firstperson_righthand", "0.68"}}) {
                double s = d.getAsJsonObject(k[0]).getAsJsonArray("scale").get(0).getAsDouble();
                if (Math.abs(s - Double.parseDouble(k[1])) > 1e-6) bad.add(rest + " " + k[0] + " scale is " + s + ", BowSize assumes " + k[1]);
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }
}
