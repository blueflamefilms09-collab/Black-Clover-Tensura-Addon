package com.newuniverse.nusmp.devil.mastery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.devil.WorldDevil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: the abilities and passives of a devil come from the anime it is from and from its magic aspect (owner request, docs/devil_mastery.md). They are DATA: {@code assets/anime_multiverse/devil_kits.json}
 * (written by tools/devil/gen_devil_kits.py), read by this pure class, validated by {@link #problems}, so a kit can be changed or added without code. A kit has two to four arts (an art opens at its tier of the
 * skill's mastery), two or three passives (each opens at a tier and works while the devil is let in, or only inside the Devil Union), the canon names of the stage transitions (horns, wings, union), and for the
 * Black Clover devils the union's own passives; {@code fusions} are the double unions (two held devils). Every shape the server can run is a name in a short list ({@link #ART_KINDS}, {@link #PASSIVE_KINDS}),
 * so a kit can never ask for anything unbounded: it picks a size, never a loop. No Minecraft type here.
 */
public final class DevilKit {
    private DevilKit() {}

    public static final Set<String> ART_KINDS = Set.of("strike", "burst", "rush", "ward");
    public static final Set<String> SPECIALS = Set.of("", "push", "pull", "burn", "strip", "drain", "launch");
    public static final Set<String> PASSIVE_KINDS = Set.of("stat", "resist", "regen", "onhit");
    public static final Set<String> STAT_ATTRS = Set.of("attack", "speed", "armor", "health", "knockback", "reach");
    public static final Set<String> RESIST_TYPES = Set.of("fire", "magic", "projectile", "fall", "explosion", "physical");
    public static final Set<String> WHEN = Set.of("awake", "union", "fusion");

    public record Fx(String id, int ticks, int amp) {}

    /** {@code dmg} a multiple of the tier's base hit ({@code DualRules.baseDamage}), {@code cost} a multiple of the tier's art price, {@code range} blocks (a cone, a radius or a dash). */
    public record Art(String id, String name, String kind, double dmg, int cd, double cost, double range, List<Fx> fx, String special, List<Fx> self, int tier, String canon) {}

    public record Passive(String id, String name, int tier, String when, String kind, String type, double value, Fx fx, String canon) {}

    public record Kit(String devil, String anime, String aspect, String source, List<Art> arts, List<Passive> passives, List<String> stages, String unionName) {
        public List<Passive> passivesWhen(String when) {
            List<Passive> out = new ArrayList<>(3);
            for (Passive p : passives) if (p.when().equals(when)) out.add(p);
            return out;
        }
    }

    public record Fusion(String a, String b, String name, boolean canon, String note, List<Passive> passives) {}

    public record Table(Map<String, Kit> kits, List<Fusion> fusions) {
        public Kit of(String devil) { return devil == null ? null : kits.get(devil); }
    }

    private static volatile Table table;

    public static Table table() {
        Table t = table;
        if (t != null) return t;
        try (InputStream in = DevilKit.class.getResourceAsStream("/assets/anime_multiverse/devil_kits.json")) {
            t = in == null ? new Table(Map.of(), List.of()) : parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Exception unreadable) {
            t = new Table(Map.of(), List.of());
        }
        table = t;
        return t;
    }

    /** The kit of a devil id; the old pact devils and any devil missing from the file get none (their skill then has only the awakening). */
    public static Kit of(String devil) { return table().of(devil); }

    // ---------------------------------------------------------------- parsing (never throws: a bad row is dropped, {@link #problems} says why)
    private static String str(JsonObject o, String k, String def) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : def; }

    private static double num(JsonObject o, String k, double def) { return o.has(k) && o.get(k).isJsonPrimitive() && o.get(k).getAsJsonPrimitive().isNumber() ? o.get(k).getAsDouble() : def; }

    private static Fx fx(JsonElement e) {
        if (e == null || !e.isJsonArray() || e.getAsJsonArray().size() != 3) return null;
        JsonArray a = e.getAsJsonArray();
        try { return new Fx(a.get(0).getAsString(), a.get(1).getAsInt(), a.get(2).getAsInt()); } catch (RuntimeException bad) { return null; }
    }

    private static List<Fx> fxList(JsonObject o, String k) {
        List<Fx> out = new ArrayList<>(2);
        if (o.has(k) && o.get(k).isJsonArray()) for (JsonElement e : o.getAsJsonArray(k)) { Fx f = fx(e); if (f != null) out.add(f); }
        return out;
    }

    private static Passive passive(JsonObject o, String when) {
        return new Passive(str(o, "id", ""), str(o, "name", ""), (int) num(o, "tier", -1), when, str(o, "kind", ""), str(o, "type", ""), num(o, "value", 0), o.has("fx") ? fx(o.get("fx")) : null, str(o, "canon", ""));
    }

    private static List<Passive> passives(JsonObject o, String k, String when) {
        List<Passive> out = new ArrayList<>(4);
        if (o.has(k) && o.get(k).isJsonArray()) for (JsonElement e : o.getAsJsonArray(k)) if (e.isJsonObject()) out.add(passive(e.getAsJsonObject(), when));
        return out;
    }

    public static Table parse(JsonObject root) {
        Map<String, Kit> kits = new LinkedHashMap<>();
        JsonObject devils = root.has("devils") && root.get("devils").isJsonObject() ? root.getAsJsonObject("devils") : new JsonObject();
        for (Map.Entry<String, JsonElement> en : devils.entrySet()) {
            if (!en.getValue().isJsonObject()) continue;
            JsonObject d = en.getValue().getAsJsonObject();
            List<Art> arts = new ArrayList<>(4);
            if (d.has("arts") && d.get("arts").isJsonArray()) for (JsonElement e : d.getAsJsonArray("arts")) {
                if (!e.isJsonObject()) continue;
                JsonObject a = e.getAsJsonObject();
                arts.add(new Art(str(a, "id", ""), str(a, "name", ""), str(a, "kind", ""), num(a, "dmg", 0), (int) num(a, "cd", 0), num(a, "cost", 0), num(a, "range", 0), fxList(a, "fx"), str(a, "special", ""), fxList(a, "self"), (int) num(a, "tier", arts.size()), str(a, "canon", "")));
            }
            List<String> stages = new ArrayList<>(3);
            if (d.has("stages") && d.get("stages").isJsonArray()) for (JsonElement e : d.getAsJsonArray("stages")) stages.add(e.getAsString());
            List<Passive> ps = passives(d, "passives", "awake");
            ps.addAll(passives(d, "union", "union"));
            kits.put(en.getKey(), new Kit(en.getKey(), str(d, "anime", ""), str(d, "aspect", ""), str(d, "source", ""), arts, ps, stages, str(d, "unionName", "")));
        }
        List<Fusion> fusions = new ArrayList<>(4);
        if (root.has("fusions") && root.get("fusions").isJsonArray()) for (JsonElement e : root.getAsJsonArray("fusions")) {
            if (!e.isJsonObject()) continue;
            JsonObject f = e.getAsJsonObject();
            fusions.add(new Fusion(str(f, "a", ""), str(f, "b", ""), str(f, "name", ""), f.has("canon") && f.get("canon").getAsBoolean(), str(f, "note", ""), passives(f, "passives", "fusion")));
        }
        return new Table(kits, fusions);
    }

    // ---------------------------------------------------------------- validation
    private static boolean fxOk(Fx f) { return f != null && f.id().matches("[a-z0-9_.]+:[a-z0-9_./]+") && f.ticks() > 0 && f.ticks() <= 600 && f.amp() >= 0 && f.amp() <= 4; }

    /** Every reason this table is not safe to run (empty = fine): ids and names, shapes in the short lists, numbers inside their bounds, tiers, the anime of the devil, canon text present. */
    public static List<String> problems(Table t) {
        List<String> out = new ArrayList<>();
        for (Kit k : t.kits().values()) {
            WorldDevil w = WorldDevil.byId(k.devil());
            String at = k.devil() + ": ";
            if (w == null) { out.add(at + "not a devil of the roster"); continue; }
            if (!k.anime().equals(w.world().label)) out.add(at + "anime '" + k.anime() + "' is not the devil's world '" + w.world().label + "'");
            if (k.aspect().isEmpty() || !k.aspect().equals(w.attribute().key())) out.add(at + "aspect '" + k.aspect() + "' is not the devil's attribute '" + w.attribute().key() + "'");
            if (k.source().isBlank()) out.add(at + "no source");
            if (k.arts().size() < 2 || k.arts().size() > 4) out.add(at + "needs 2 to 4 arts, has " + k.arts().size());
            Set<String> seen = new java.util.HashSet<>();
            for (Art a : k.arts()) {
                String an = at + "art " + a.id() + ": ";
                if (!a.id().matches("[a-z0-9_]{2,32}") || !seen.add(a.id())) out.add(an + "bad or repeated id");
                if (a.name().isBlank() || a.canon().isBlank()) out.add(an + "needs a name and its canon line");
                if (!ART_KINDS.contains(a.kind())) out.add(an + "kind '" + a.kind() + "' is not allowed");
                if (!SPECIALS.contains(a.special())) out.add(an + "special '" + a.special() + "' is not allowed");
                if (a.dmg() < 0 || a.dmg() > 3.0 || (!a.kind().equals("ward") && a.dmg() <= 0)) out.add(an + "dmg out of 0..3");
                if (a.cd() < 6 || a.cd() > 120) out.add(an + "cooldown must be 6 to 120 seconds");
                if (a.cost() < 0.1 || a.cost() > 2.0) out.add(an + "cost out of 0.1..2");
                if (a.range() < 2 || a.range() > 14) out.add(an + "range out of 2..14");
                if (a.tier() < 0 || a.tier() >= MasteryRules.TIERS - 1) out.add(an + "tier must be 0 to 3");
                if (a.fx().size() > 2 || a.self().size() > 3) out.add(an + "too many effects");
                for (Fx f : a.fx()) if (!fxOk(f)) out.add(an + "bad effect " + f);
                for (Fx f : a.self()) if (!fxOk(f)) out.add(an + "bad self effect " + f);
                if (a.kind().equals("ward") && a.self().isEmpty()) out.add(an + "a ward needs a self effect");
            }
            for (int i = 1; i < k.arts().size(); i++) if (k.arts().get(i).tier() < k.arts().get(i - 1).tier()) out.add(at + "arts must open in tier order");
            int awake = 0, union = 0;
            for (Passive p : k.passives()) {
                String pn = at + "passive " + p.id() + ": ";
                if (!p.id().matches("[a-z0-9_]{2,32}") || !seen.add(p.id())) out.add(pn + "bad or repeated id");
                problem(out, pn, p);
                if (p.when().equals("awake")) awake++; else if (p.when().equals("union")) union++; else out.add(pn + "when must be awake or union in a kit");
            }
            if (awake < 2 || awake > 3) out.add(at + "needs 2 or 3 passives, has " + awake);
            if (union > 2) out.add(at + "at most 2 union passives, has " + union);
            if (k.stages().size() != 3 || k.stages().stream().anyMatch(String::isBlank)) out.add(at + "needs three stage names (horns, wings, union)");
            if (union > 0 && k.unionName().isBlank()) out.add(at + "union passives need the union's name");
        }
        for (WorldDevil w : allDevils()) if (!t.kits().containsKey(w.id())) out.add(w.id() + ": the devil has no kit");
        for (Fusion f : t.fusions()) {
            String fn = "fusion " + f.a() + "+" + f.b() + ": ";
            if (WorldDevil.byId(f.a()) == null || WorldDevil.byId(f.b()) == null || f.a().equals(f.b())) out.add(fn + "devils invalid");
            if (f.name().isBlank() || f.note().isBlank()) out.add(fn + "needs a name and a note on its source");
            if (f.passives().isEmpty() || f.passives().size() > 3) out.add(fn + "needs 1 to 3 passives");
            for (Passive p : f.passives()) { problem(out, fn + p.id() + ": ", p); if (!p.when().equals("fusion")) out.add(fn + p.id() + ": when must be fusion"); }
        }
        return out;
    }

    private static void problem(List<String> out, String pn, Passive p) {
        if (p.name().isBlank() || p.canon().isBlank()) out.add(pn + "needs a name and its canon line");
        if (!PASSIVE_KINDS.contains(p.kind())) out.add(pn + "kind '" + p.kind() + "' is not allowed");
        if (p.tier() < 0 || p.tier() >= MasteryRules.TIERS) out.add(pn + "tier out of 0..4");
        if (p.when().equals("union") && p.tier() != MasteryRules.TIERS - 1) out.add(pn + "a union passive opens at tier 4");
        if (!WHEN.contains(p.when())) out.add(pn + "bad when");
        switch (p.kind()) {
            case "stat" -> { if (!STAT_ATTRS.contains(p.type()) || p.value() <= 0 || p.value() > 10) out.add(pn + "bad stat"); }
            case "resist" -> { if (!RESIST_TYPES.contains(p.type()) || p.value() <= 0 || p.value() > 0.5) out.add(pn + "bad resist (type, 0 to 0.5)"); }
            case "regen" -> { if (p.value() <= 0 || p.value() > 1.0) out.add(pn + "regen is 0 to 1 hp a second"); }
            case "onhit" -> { if (!fxOk(p.fx())) out.add(pn + "bad on-hit effect"); }
            default -> { }
        }
    }

    private static List<WorldDevil> allDevils() {
        List<WorldDevil> l = new ArrayList<>(WorldDevil.count());
        for (int i = 0; i < WorldDevil.count(); i++) l.add(WorldDevil.at(i));
        return l;
    }

    public static String describe(Art a) { return a.name() + " (" + a.kind().toUpperCase(Locale.ROOT).charAt(0) + a.kind().substring(1) + ", " + a.cd() + " s)"; }
}
