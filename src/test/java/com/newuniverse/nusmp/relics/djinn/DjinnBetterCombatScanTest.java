package com.newuniverse.nusmp.relics.djinn;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 1.101.0: a source and data scan for the Better Combat binding fix (pure, CWD = repo root). It records what the data does (which vessel weapons are
 * two-handed, so which weapons make Better Combat hide the off hand), and fails when the code goes back to reading "the other hand" through the
 * game's off-hand answer, which Better Combat overrides, or when a Djinn weapon has no Better Combat file.
 */
class DjinnBetterCombatScanTest {
    private static final Path ROOT = Path.of(".");
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path ATTRS = ROOT.resolve("src/main/resources/data/nusmp/weapon_attributes");
    private static final Path BUILTIN = ROOT.resolve("tools/bettercombat/builtin_animations.json");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    // ---------------------------------------------------------------- data: which vessel weapons are two-handed
    /** Better Combat's presets with {@code two_handed: true} (resolved), from tools/bettercombat/builtin_animations.json. */
    private static Set<String> twoHandedPresets() throws IOException {
        String t = read(BUILTIN);
        String presets = t.substring(t.indexOf("\"presets\""));
        Set<String> out = new TreeSet<>();
        Matcher m = Pattern.compile("\"([a-z_]+)\"\\s*:\\s*\\{([^{}]*)\\}").matcher(presets);
        while (m.find()) if (Pattern.compile("\"two_handed\"\\s*:\\s*true").matcher(m.group(2)).find()) out.add(m.group(1));
        return out;
    }

    /** Is the weapon of this file two-handed under Better Combat: its own {@code two_handed}, else the parent preset's. */
    static boolean twoHanded(String json, Set<String> presets) {
        Matcher own = Pattern.compile("\"two_handed\"\\s*:\\s*(true|false)").matcher(json);
        if (own.find()) return own.group(1).equals("true");
        Matcher parent = Pattern.compile("\"parent\"\\s*:\\s*\"bettercombat:([a-z_]+)\"").matcher(json);
        return parent.find() && presets.contains(parent.group(1));
    }

    /** Every relic def of the sources that is flagged {@code .vessel()} and has a Better Combat file: id -> two-handed. */
    private static Map<String, Boolean> vesselWeapons() throws IOException {
        Set<String> presets = twoHandedPresets();
        Map<String, Boolean> out = new TreeMap<>();
        Pattern of = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\"");
        try (Stream<Path> files = Files.walk(JAVA.resolve("relics"))) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String t = read(f);
                if (!t.contains("RelicDef.of(")) continue;
                List<Integer> starts = new ArrayList<>();
                List<String> names = new ArrayList<>();
                Matcher m = of.matcher(t);
                while (m.find()) { starts.add(m.start()); names.add(m.group(1)); }
                for (int i = 0; i < starts.size(); i++) {
                    int end = i + 1 < starts.size() ? starts.get(i + 1) : t.length();
                    String block = t.substring(starts.get(i), end);
                    Path attr = ATTRS.resolve(names.get(i) + ".json");
                    if (block.contains(".vessel()") && Files.exists(attr)) out.put(names.get(i), twoHanded(read(attr), presets));
                }
            }
        }
        return out;
    }

    @Test
    void twoHandedVesselWeaponsExistSoTheOffHandReadMustNotGoThroughTheGame() throws IOException {
        Map<String, Boolean> v = vesselWeapons();
        long two = v.values().stream().filter(b -> b).count();
        assertTrue(v.size() >= 30, "the vessel weapons with a Better Combat file: " + v.size());
        assertTrue(two >= 8, "the two-handed ones (Better Combat answers EMPTY for their off hand): " + two + " of " + v.size() + " " + v);
        assertTrue(v.getOrDefault("noon_cleaver", false), "Noon Cleaver (double_axe) is two-handed");
        assertTrue(v.getOrDefault("gungnir_the_unerring", false), "Gungnir inherits two_handed from the spear preset");
        assertFalse(v.getOrDefault("vorpal_sword", true), "Vorpal Sword is one-handed");
    }

    @Test
    void theTwoHandedProfilesWereKeptNotFlippedAsTheFixIsInTheCode() throws IOException {
        // rule 2 (keep the old): the fix is that the ritual no longer depends on the game's off-hand answer, not that the profiles lost their hands
        Set<String> presets = twoHandedPresets();
        assertTrue(presets.containsAll(Set.of("claymore", "double_axe", "glaive", "staff", "scythe", "hammer", "katana", "spear")), presets.toString());
        assertTrue(twoHanded(read(ATTRS.resolve("noon_cleaver.json")), presets));
        assertTrue(twoHanded(read(ATTRS.resolve("blackrazor.json")), presets));
    }

    // ---------------------------------------------------------------- code: nothing reads the other hand through the game
    private static final Pattern GAME_OFFHAND = Pattern.compile("getOffhandItem\\(\\)|getItemInHand\\(\\s*InteractionHand\\.OFF_HAND\\s*\\)|InteractionHand\\.MAIN_HAND\\s*\\?\\s*InteractionHand\\.OFF_HAND");

    /** Strips comments so a sentence that names the old call does not count as a use. */
    private static String code(String t) { return t.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    @Test
    void theBindingCodeReadsTheRawInventoryForTheOtherHand() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String f : new String[]{"relics/djinn/DjinnSealItem.java", "relics/djinn/DjinnBinding.java", "relics/djinn/dark/DarkRukhShardItem.java",
                "relics/djinn/DjinnCommand.java", "relics/UnbindingSealItem.java"}) {
            String c = code(read(JAVA.resolve(f)));
            Matcher m = GAME_OFFHAND.matcher(c);
            if (m.find()) bad.add(f + ": " + m.group());
        }
        // DarkDjinn and VesselAccess keep a getOffhandItem() only next to the raw read (VesselAccess.equipped adds both)
        String dark = code(read(JAVA.resolve("relics/djinn/dark/DarkDjinn.java")));
        assertFalse(GAME_OFFHAND.matcher(dark).find(), "DarkDjinn.onRightClickItem reads the other hand through the game");
        String access = code(read(JAVA.resolve("relics/djinn/VesselAccess.java")));
        assertTrue(access.contains("rawOff(p)") && access.contains("p.getInventory().offhand.get(0)"), "VesselAccess reads the raw off hand");
        assertTrue(bad.isEmpty(), "reads the other hand through the game (Better Combat answers EMPTY for it): " + bad);
    }

    @Test
    void theAlwaysWorksGesturesAreWiredAndDefaultOn() throws IOException {
        String cmd = read(JAVA.resolve("relics/djinn/DjinnCommand.java"));
        assertTrue(cmd.contains("Commands.literal(\"bind\")") && cmd.contains("registerAlias"), "/multiverse djinn bind and /djinn bind");
        assertTrue(read(JAVA.resolve("multiverse/MultiverseCommands.java")).contains("DjinnCommand.registerAlias(d)"));
        String seal = read(JAVA.resolve("relics/djinn/DjinnSealItem.java"));
        assertTrue(seal.contains("overrideStackedOnOther") && seal.contains("overrideOtherStackedOnMe"), "the inventory click gesture");
        assertTrue(read(JAVA.resolve("relics/djinn/DjinnEngine.java")).contains("BindGesture.Gesture.KEY"), "the Djinn key starts the ritual when no Djinn is bound");
        String cfg = read(JAVA.resolve("relics/djinn/DjinnBindConfig.java"));
        assertTrue(Pattern.compile("\"commandBind\",\\s*true").matcher(cfg).find() && Pattern.compile("\"guiBind\",\\s*true").matcher(cfg).find(), "both switches default ON");
        assertTrue(read(JAVA.resolve("NUSMP.java")).contains("DjinnBindConfig.SPEC"), "the config is registered");
    }

    // ---------------------------------------------------------------- the other direction: every Djinn weapon has a Better Combat file
    @Test
    void everyMeleeDjinnWeaponHasABetterCombatFile() throws IOException {
        String rules = read(JAVA.resolve("relics/djinn/weapon/DjinnWeaponRules.java"));
        Matcher m = Pattern.compile("row\\(Djinn\\.([A-Z_]+),\\s*\"([a-z]+)\"").matcher(rules);
        int rows = 0;
        List<String> missing = new ArrayList<>();
        while (m.find()) {
            rows++;
            if (m.group(2).equals("bow")) continue;                                        // Sitri's and Barbatos' bows are ranged
            String id = "djinn_weapon_" + m.group(1).toLowerCase();
            for (String suffix : new String[]{"", "_dark"})
                if (!Files.exists(ATTRS.resolve(id + suffix + ".json"))) missing.add(id + suffix);
        }
        assertTrue(rows >= 36, "weapon rows found: " + rows);
        assertTrue(missing.isEmpty(), "Djinn weapons without a Better Combat file (they swing with the vanilla attack): " + missing
                + " -> python tools/bettercombat/inventory.py && python tools/bettercombat/gen_attributes.py");
    }

    @Test
    void theLangFragmentHasEveryReasonWithTheFallbackText() throws IOException {
        String frag = read(ROOT.resolve("tools/lang/djinn_bc_fix.json"));
        for (BindGesture.Why w : BindGesture.Why.values()) {
            Matcher m = Pattern.compile("\"" + Pattern.quote(w.key()) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(frag);
            assertTrue(m.find(), "no lang entry for " + w.key());
            assertEquals(w.fallback(), m.group(1), "the lang text and the fallback of " + w + " differ");
        }
        assertTrue(frag.contains("tooltip.nusmp.djinn_seal.bind_hint"));
    }
}
