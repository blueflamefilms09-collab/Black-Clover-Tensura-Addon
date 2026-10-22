package com.newuniverse.nusmp.engraving.codex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.franchise.FranchiseData;
import com.newuniverse.nusmp.franchise.FranchiseMap;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The shipped data the Codex tests build their book from (no Minecraft types: the game side, {@link CodexGather}, is covered by the server self-test). */
final class CodexTestData {
    private CodexTestData() {}

    static final File RES = new File("src/main/resources");
    static final File SRC = new File("src/main/java/com/newuniverse/nusmp");

    static String read(File f) throws Exception { return Files.readString(f.toPath()); }

    static FranchiseData.Table table() throws Exception { return FranchiseData.parse(read(new File(RES, "data/nusmp/franchise/engravings.json"))); }

    /** en_us.json overlaid with the lang fragments the Codex reads (the franchise engravings' and its own). */
    static Map<String, String> lang() throws Exception {
        Map<String, String> out = new HashMap<>();
        for (File f : new File[]{new File(RES, "assets/nusmp/lang/en_us.json"), new File("tools/lang/franchise_engravings.json"), new File("tools/lang/engraving_codex.json")}) {
            JsonObject o = JsonParser.parseString(read(f)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : o.entrySet()) out.put(e.getKey(), e.getValue().getAsString());
        }
        return out;
    }

    /** The enchantments of the mod that are not franchise engravings: every file of data/nusmp/enchantment whose name does not start with fr_. */
    static List<String> ownIds() {
        List<String> out = new ArrayList<>();
        File[] files = new File(RES, "data/nusmp/enchantment").listFiles((d, n) -> n.endsWith(".json"));
        Arrays.sort(files);
        for (File f : files) {
            String id = f.getName().substring(0, f.getName().length() - 5);
            if (!id.startsWith("fr_")) out.add(id);
        }
        return out;
    }

    /** Every enchantment file of the mod, ids without the namespace. */
    static List<String> allEnchantmentIds() {
        List<String> out = new ArrayList<>();
        File[] files = new File(RES, "data/nusmp/enchantment").listFiles((d, n) -> n.endsWith(".json"));
        Arrays.sort(files);
        for (File f : files) out.add(f.getName().substring(0, f.getName().length() - 5));
        return out;
    }

    /** The engraving ids the item tables name, read from the SOURCE of the tables (item path -> ids), so the test does not need the game. */
    static Map<String, List<String>> tableSpecs(String javaFile) throws Exception {
        String src = read(new File(SRC, javaFile));
        Map<String, List<String>> out = new TreeMap<>();
        Matcher entry = Pattern.compile("Map\\.entry\\(\"([a-z0-9_]+)\", List\\.of\\(((?:e\\(\"[a-z_:]+\", \\d\\)(?:, )?)+)\\)\\)").matcher(src);
        while (entry.find()) {
            List<String> ids = new ArrayList<>();
            Matcher id = Pattern.compile("e\\(\"([a-z_:]+)\", \\d\\)").matcher(entry.group(2));
            while (id.find()) ids.add(id.group(1));
            out.put(entry.group(1), ids);
        }
        return out;
    }

    /** Every Tensura engraving id named anywhere in the source (the tables, the Djinn rules, {@code .engrave("...")} of the relic defs); namespaced ids and "@level" are dropped. */
    static TreeSet<String> referencedTensuraIds() throws Exception {
        TreeSet<String> ids = new TreeSet<>();
        for (String f : new String[]{"item/WeaponEngravings.java", "item/ArmorEngravings.java", "relics/djinn/weapon/DjinnWeaponRules.java"}) {
            Matcher m = Pattern.compile("\\be\\(\"([a-z_:]+)\", \\d\\)").matcher(read(new File(SRC, f)));
            while (m.find()) if (!m.group(1).contains(":")) ids.add(m.group(1));
        }
        java.nio.file.Path root = SRC.toPath();
        try (var walk = Files.walk(root)) {
            for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String s = Files.readString(p);
                Matcher call = Pattern.compile("\\.engrave\\(([^)]*)\\)").matcher(s);
                while (call.find()) {
                    Matcher id = Pattern.compile("\"([a-z_:]+)(?:@\\d)?\"").matcher(call.group(1));
                    while (id.find()) if (!id.group(1).contains(":")) ids.add(id.group(1));
                }
            }
        }
        return ids;
    }

    /** A book with the franchise table, the mod's own enchantments (carriers read from the weapon table source), the given Tensura ids and the English lang. */
    static CodexEntries.Book book(Function<String, String> lang) throws Exception {
        FranchiseData.Table table = table();
        Map<String, List<String>> franchiseItems = new HashMap<>();
        for (Map.Entry<String, String> e : FranchiseMap.map().items().entrySet()) franchiseItems.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add("Item " + e.getKey());
        Map<String, List<String>> weapons = tableSpecs("item/WeaponEngravings.java");
        Map<String, TreeSet<String>> carriers = new HashMap<>();
        for (Map.Entry<String, List<String>> w : weapons.entrySet()) for (String id : w.getValue()) carriers.computeIfAbsent(id, k -> new TreeSet<>()).add("Item " + w.getKey());
        List<CodexEntries.Own> own = new ArrayList<>();
        for (String id : ownIds()) own.add(new CodexEntries.Own(id, "main", 1, carriers.getOrDefault("nusmp:" + id, new TreeSet<>())));
        List<CodexEntries.TensuraRef> tensura = new ArrayList<>();
        for (String id : referencedTensuraIds()) tensura.add(new CodexEntries.TensuraRef(id, null, 0, carriers.getOrDefault(id, new TreeSet<>())));
        return CodexEntries.build(new CodexEntries.Input(table, franchiseItems, own, tensura, TensuraNotes.parse(read(new File(RES, "data/nusmp/engraving_codex/tensura_engravings.json"))), lang));
    }

    static Function<String, String> english() throws Exception {
        Map<String, String> l = lang();
        return l::get;
    }
}
