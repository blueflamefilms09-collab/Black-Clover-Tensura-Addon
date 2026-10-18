package com.newuniverse.nusmp.heroweapon.forms;

import com.newuniverse.nusmp.heroweapon.HeroClass;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroStyle;
import com.newuniverse.nusmp.heroweapon.HeroTrigger;
import com.newuniverse.nusmp.heroweapon.Sin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: fourteen builders write forms files they cannot run, so the rules of {@code docs/heroweapon/design.md} section 6 must be checked by a machine that
 * runs in JUnit (before a merge) and again in the game (at every data load), with the SAME code and the same messages. This class is that machine. It takes
 * the file as a small tree of {@code Map} / {@code List} / {@code String} / {@code Number} / {@code Boolean} / {@code null} (see {@link FormsJson}) so that it
 * imports nothing of Minecraft, checks every rule, builds the {@link HeroForm} records and says exactly what is wrong in words the author can act on.
 *
 * <p>Two grades of problem. An <b>error</b> makes the loader skip the form (and every form below it in the tree); an error in the base form, in the
 * file header or in the shape of the form list rejects the whole file and the weapon keeps its synthetic base form. A <b>warning</b> is a rule about the
 * file as a whole (how many forms, the canon sins having a curse form, a misspelt key): the loader still uses the file, but {@code FormsDataTest} and the
 * {@code hero.forms} self-test fail on any warning, so it cannot reach a release unnoticed.
 *
 * <p>What is NOT checked here because it needs the game: that an item or tag exists (the self-test resolves them in the real registry), and that a skill id
 * exists in the weapon's logic (the loader passes a {@link SkillIndex}; JUnit passes none).
 */
public final class FormsSchema {
    private FormsSchema() {}

    /** Asks the weapon logics about skills without this class importing them. A null index switches the skill-existence and ultimate checks off. */
    public interface SkillIndex {
        /** True when a skill with this global id is registered. */
        boolean exists(String skillId);

        /** True when the skill is marked as the ultimate of a form. */
        boolean ultimate(String skillId);
    }

    /**
     * The outcome of reading one file. {@code forms} is never empty: the accepted forms, base first, then by tier (a rejected file gives just the synthetic base).
     */
    public record Parsed(HeroKind kind, List<HeroForm> forms, List<String> errors, List<String> warnings, boolean fileRejected) {
        /** True when nothing at all is wrong. */
        public boolean clean() { return errors.isEmpty() && warnings.isEmpty(); }

        /** Every message, errors first, one per line, for a log or a test failure. */
        public String report() {
            List<String> all = new ArrayList<>();
            for (String e : errors) all.add("error: " + e);
            for (String w : warnings) all.add("warning: " + w);
            return String.join("\n", all);
        }
    }

    // ------------------------------------------------------------------------------------------------ the numbers of design.md section 6
    public static final String LOOK_BASE = HeroForm.LOOK_BASE, LOOK_CURSE = HeroForm.LOOK_CURSE;
    public static final int MAX_FORMS = 40, MAX_TIER = 5, MIN_MASTERY = 40, MAX_MASTERY = 400;
    public static final int REQ_MIN = 4, REQ_MAX = 64, REQ_TOP_MAX = 256, REQ_PER_FORM = 4, TOP_TIER = 3;
    public static final int NAME_MAX = 40, DESC_MAX = 500, PASSIVES_MAX = 8, PARENTS_MAX = 4, CORRUPTION_MAX = 100, ID_MAX = 100, SKILL_ID_MAX = 64;
    public static final int HOLY_ORDINARY_MIN = 8, HOLY_ORDINARY_MAX = 10, OTHER_ORDINARY_MIN = 3, OTHER_ORDINARY_MAX = 5, CURSE_FORMS_MAX = 8;

    /** The budget per stat key: {minimum, maximum} of a form's {@code stats}; {@code abilityBonus} may use a quarter of each side. Same keys as {@link HeroForm#STAT_KEYS}. */
    private static final Map<String, double[]> LIMITS = new LinkedHashMap<>();

    static {
        LIMITS.put("attack_damage", new double[]{-14, 14});
        LIMITS.put("attack_speed", new double[]{-3, 1});
        LIMITS.put("armor", new double[]{-8, 8});
        LIMITS.put("armor_toughness", new double[]{-4, 4});
        LIMITS.put("max_health", new double[]{-12, 12});
        LIMITS.put("movement_speed", new double[]{-0.2, 0.2});
        LIMITS.put("knockback_resistance", new double[]{-0.6, 0.6});
        LIMITS.put("reach", new double[]{-2, 2});
        LIMITS.put("luck", new double[]{-3, 3});
    }

    /** {minimum, maximum} allowed for a stat key in {@code stats}, or null for an unknown key. */
    public static double[] limits(String statKey) {
        double[] l = LIMITS.get(statKey);
        return l == null ? null : l.clone();
    }

    private static final double EPS = 1e-9;
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{1,31}");
    private static final Pattern RESOURCE = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> ITEM_NAMESPACES = Set.of("minecraft", "tensura", "nusmp");
    private static final Set<String> TAG_NAMESPACES = Set.of("minecraft", "c", "nusmp");

    /** Vanilla items a survival player cannot hold; a requirement on one could never be met. */
    private static final Set<String> UNOBTAINABLE = Set.of("minecraft:barrier", "minecraft:bedrock", "minecraft:command_block", "minecraft:chain_command_block",
            "minecraft:repeating_command_block", "minecraft:command_block_minecart", "minecraft:structure_block", "minecraft:structure_void", "minecraft:jigsaw",
            "minecraft:light", "minecraft:debug_stick", "minecraft:knowledge_book", "minecraft:end_portal_frame", "minecraft:reinforced_deepslate", "minecraft:spawner",
            "minecraft:trial_spawner", "minecraft:vault", "minecraft:budding_amethyst", "minecraft:petrified_oak_slab", "minecraft:farmland",
            "minecraft:air", "minecraft:test_block", "minecraft:test_instance_block");

    /** Wrong names authors tend to reach for, lower-cased, and the real key each one means. */
    private static final Map<String, String> ALIASES = Map.ofEntries(Map.entry("requirements", "requires"), Map.entry("requirement", "requires"), Map.entry("needs", "requires"),
            Map.entry("cost", "requires"), Map.entry("costs", "requires"), Map.entry("skill", "skills"), Map.entry("techniques", "skills"), Map.entry("abilities", "skills"),
            Map.entry("passive", "passives"), Map.entry("parent", "parents"), Map.entry("stat", "stats"), Map.entry("bonus", "abilityBonus"), Map.entry("ability_bonus", "abilityBonus"),
            Map.entry("corruption", "corruptionPerSecond"), Map.entry("corruption_per_second", "corruptionPerSecond"), Map.entry("description", "desc"), Map.entry("form", "forms"));

    private static final Set<String> ROOT_KEYS = Set.of("weapon", "forms");
    private static final Set<String> FORM_KEYS = Set.of("id", "name", "desc", "tier", "parents", "requires", "mastery", "stats", "abilityBonus", "skills", "passives",
            "look", "curse", "sin", "corruptionPerSecond");

    // ------------------------------------------------------------------------------------------------ entry points
    /** Reads a file whose weapon is {@code kind}. {@code tree} is the parsed JSON ({@link FormsJson#parse}); {@code skills} may be null. Never throws. */
    public static Parsed parse(HeroKind kind, Object tree, SkillIndex skills) {
        Run run = new Run(kind, skills);
        try {
            run.go(tree);
        } catch (RuntimeException unexpected) {
            run.errors.add("the validator itself failed on this file (" + unexpected + "); the file is rejected");
            run.rejected = true;
        }
        return run.result();
    }

    /** Reads a file from its text; a text that is not JSON gives a rejected file with that single error. */
    public static Parsed parseText(HeroKind kind, String json, SkillIndex skills) {
        Object tree;
        try {
            tree = FormsJson.parse(json);
        } catch (IllegalArgumentException bad) {
            return new Parsed(kind, List.of(HeroForm.base(kind)), List.of(bad.getMessage()), List.of(), true);
        }
        Parsed p = parse(kind, tree, skills);
        List<String> twice = FormsJson.duplicateKeys(json);
        if (twice.isEmpty()) return p;
        List<String> warnings = new ArrayList<>(p.warnings());
        warnings.addAll(twice);
        return new Parsed(p.kind(), p.forms(), p.errors(), List.copyOf(warnings), p.fileRejected());
    }

    // ------------------------------------------------------------------------------------------------ the run
    /** One form as written, before the cross-form rules. */
    private static final class Raw {
        int index;
        String id = "";
        String name = "", desc = "";
        int tier;
        boolean tierOk, masteryOk;
        List<String> parents = new ArrayList<>();
        List<HeroForm.Requirement> requires = new ArrayList<>();
        int mastery;
        Map<String, Double> stats = new LinkedHashMap<>(), bonus = new LinkedHashMap<>();
        Map<String, String> skills = new LinkedHashMap<>();
        List<String> passives = new ArrayList<>();
        String look = LOOK_BASE;
        boolean curse;
        Sin sin;
        int corruption;
        boolean bad;

        String label() { return "form '" + (id.isEmpty() ? "#" + index : id) + "'"; }
    }

    private static final class Run {
        final HeroKind kind;
        final SkillIndex skills;
        final List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();
        boolean rejected;
        final List<HeroForm> accepted = new ArrayList<>();

        Run(HeroKind kind, SkillIndex skills) {
            this.kind = kind;
            this.skills = skills;
        }

        Parsed result() {
            if (rejected || accepted.isEmpty()) return new Parsed(kind, List.of(HeroForm.base(kind)), List.copyOf(errors), List.copyOf(warnings), true);
            return new Parsed(kind, List.copyOf(accepted), List.copyOf(errors), List.copyOf(warnings), false);
        }

        void reject(String why) {
            errors.add(why);
            rejected = true;
        }

        // -------------------------------------------------------------------------------------------- file level
        void go(Object tree) {
            if (!(tree instanceof Map<?, ?> root)) {
                reject("the file must be a JSON object with a \"forms\" list");
                return;
            }
            for (Object k : root.keySet()) unknownKey(String.valueOf(k), ROOT_KEYS, "the file");
            Object weapon = root.get("weapon");
            if (weapon == null) warnings.add("the file has no \"weapon\" key; add \"weapon\": \"" + kind.itemId() + "\" so a copied file is noticed");
            else if (!(weapon instanceof String w) || !w.replaceFirst("^nusmp:", "").equals(kind.itemId())) {
                reject("\"weapon\" says " + weapon + " but this file belongs to " + kind.itemId() + " (a copied file?)");
                return;
            }
            Object list = root.get("forms");
            if (!(list instanceof List<?> forms) || forms.isEmpty()) {
                reject("\"forms\" must be a non-empty list of form objects");
                return;
            }
            if (forms.size() > MAX_FORMS) {
                reject("\"forms\" has " + forms.size() + " entries; at most " + MAX_FORMS + " are allowed");
                return;
            }
            List<Raw> raws = new ArrayList<>();
            for (int i = 0; i < forms.size(); i++) {
                Raw r = new Raw();
                r.index = i;
                if (forms.get(i) instanceof Map<?, ?> m) readForm(r, m);
                else {
                    errors.add("forms[" + i + "] is not a JSON object");
                    r.bad = true;
                }
                raws.add(r);
            }
            Raw first = raws.get(0);
            if (!HeroForm.BASE_ID.equals(first.id)) {
                reject("the first form must be the base form (\"id\": \"" + HeroForm.BASE_ID + "\"), not '" + first.id + "'");
                return;
            }
            if (first.bad) {
                reject("the base form has errors (listed above); a file without a valid base form is rejected");
                return;
            }
            link(raws);
            if (rejected) return;
            fileRules();
        }

        /**
         * The design says unknown keys are ignored, so only a key that looks like a slip is reported: one a couple of letters away from a real key, or a common wrong name
         * for one ("requirements" for "requires"). Anything else (a note, an extra the weapon's own code reads) passes silently.
         */
        void unknownKey(String key, Set<String> known, String where) {
            if (known.contains(key) || key.startsWith("_") || key.equals("comment") || key.equals("note")) return;
            String hint = closest(key, known);
            if (hint == null) {
                String alias = ALIASES.get(key.toLowerCase(Locale.ROOT));
                if (alias != null && known.contains(alias)) hint = alias;
            }
            if (hint == null) return;
            warnings.add("unknown key \"" + key + "\" in " + where + " (did you mean \"" + hint + "\"?); it is ignored");
        }

        // -------------------------------------------------------------------------------------------- one form's own fields
        void readForm(Raw r, Map<?, ?> m) {
            Object idv = m.get("id");
            r.id = idv instanceof String s ? s : "";
            String L = r.label() + ": ";
            for (Object k : m.keySet()) unknownKey(String.valueOf(k), FORM_KEYS, r.label());
            if (!(idv instanceof String) || !ID.matcher(r.id).matches()) {
                errors.add("forms[" + r.index + "]: \"id\" must be lower-case snake_case, 2 to 32 characters, starting with a letter (got " + idv + ")");
                r.bad = true;
            }
            r.name = text(m.get("name"), L + "\"name\"", NAME_MAX, r);
            r.desc = text(m.get("desc"), L + "\"desc\"", DESC_MAX, r);
            boolean isBase = HeroForm.BASE_ID.equals(r.id);
            Integer tier = m.get("tier") == null && isBase ? Integer.valueOf(0) : integer(m.get("tier"));
            if (tier == null || tier < 0 || tier > MAX_TIER) {
                errors.add(L + "\"tier\" must be a whole number 0 to " + MAX_TIER + " (got " + m.get("tier") + ")");
                r.bad = true;
            } else {
                r.tier = tier;
                r.tierOk = true;
            }
            r.parents = ids(m.get("parents"), L + "\"parents\"", PARENTS_MAX, r);
            readRequires(r, m.get("requires"), L);
            Integer mastery = m.get("mastery") == null && isBase ? Integer.valueOf(0) : integer(m.get("mastery"));
            if (mastery == null) {
                errors.add(L + "\"mastery\" is required (a whole number; 0 for the base form, " + MIN_MASTERY + " to " + MAX_MASTERY + " for the others)");
                r.bad = true;
            } else {
                r.mastery = mastery;
                r.masteryOk = true;
            }
            r.stats = stats(m.get("stats"), L + "\"stats\"", 1.0, r);
            r.bonus = stats(m.get("abilityBonus"), L + "\"abilityBonus\"", 0.25, r);
            readSkills(r, m.get("skills"), L);
            r.passives = ids(m.get("passives"), L + "\"passives\"", PASSIVES_MAX, r);
            Object curse = m.get("curse");
            if (curse == null) r.curse = false;
            else if (curse instanceof Boolean b) r.curse = b;
            else {
                errors.add(L + "\"curse\" must be true or false");
                r.bad = true;
            }
            readSin(r, m.get("sin"), L);
            Integer cps = m.get("corruptionPerSecond") == null ? Integer.valueOf(0) : integer(m.get("corruptionPerSecond"));
            if (cps == null || cps < 0 || cps > CORRUPTION_MAX) {
                errors.add(L + "\"corruptionPerSecond\" must be a whole number 0 to " + CORRUPTION_MAX);
                r.bad = true;
            } else r.corruption = cps;
            readLook(r, m.get("look"), L);
            // rules that tie fields of this form together
            if (HeroForm.BASE_ID.equals(r.id)) baseRules(r, L);
            else ordinaryRules(r, L);
            if (!r.curse) {
                if (r.sin != null) bad(r, L + "\"sin\" is only for curse forms (\"curse\": true)");
                if (r.corruption != 0) bad(r, L + "\"corruptionPerSecond\" is only for curse forms (\"curse\": true)");
            }
        }

        void bad(Raw r, String message) {
            errors.add(message);
            r.bad = true;
        }

        String text(Object v, String where, int max, Raw r) {
            if (!(v instanceof String s) || s.isBlank()) {
                bad(r, where + " is required and must be text");
                return "";
            }
            if (s.length() > max) bad(r, where + " is " + s.length() + " characters; the limit is " + max);
            return s.strip();
        }

        /**
         * The base form is the starting shape: always learned, never finished. What would change the shape of the tree or make the book show something false is an error
         * (the whole file is then rejected). A permanent bonus is the one thing that is merely inert there (nothing ever finishes the base form, so it would never be
         * given), so it is dropped with a warning instead of costing the weapon its whole tree; the tests still fail on the warning, so it is fixed before a release.
         */
        void baseRules(Raw r, String L) {
            if (r.tierOk && r.tier != 0) bad(r, L + "the base form must have tier 0");
            if (!r.parents.isEmpty()) bad(r, L + "the base form must have no parents");
            if (!r.requires.isEmpty()) bad(r, L + "the base form must have no requirements (it is always learned)");
            if (r.masteryOk && r.mastery != 0) bad(r, L + "the base form must have mastery 0 (it is the starting shape, not a form to finish)");
            if (!r.bonus.isEmpty()) {
                warnings.add(L + "the base form must not have an abilityBonus: it is never finished, so the bonus would never be given; it is ignored (move it to a form with mastery)");
                r.bonus = new LinkedHashMap<>();
            }
            if (r.curse) bad(r, L + "the base form must not be a curse form");
        }

        void ordinaryRules(Raw r, String L) {
            if (r.tierOk && r.tier < 1) bad(r, L + "only the base form has tier 0");
            if (r.parents.isEmpty()) bad(r, L + "needs at least one parent (the tree hangs off \"base\")");
            if (r.masteryOk && (r.mastery < MIN_MASTERY || r.mastery > MAX_MASTERY)) bad(r, L + "\"mastery\" must be " + MIN_MASTERY + " to " + MAX_MASTERY + " (got " + r.mastery + ")");
            if (!r.curse && r.requires.isEmpty()) bad(r, L + "needs one to " + REQ_PER_FORM + " \"requires\" entries (only a curse form may ask for nothing)");
        }

        // -------------------------------------------------------------------------------------------- field readers
        Integer integer(Object v) {
            if (v instanceof Long l) return l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE ? l.intValue() : null;
            if (v instanceof Integer i) return i;
            if (v instanceof Number n) {
                double d = n.doubleValue();
                if (d == Math.rint(d) && Math.abs(d) < 1e9) return (int) d;
            }
            return null;
        }

        List<String> ids(Object v, String where, int max, Raw r) {
            List<String> out = new ArrayList<>();
            if (v == null) return out;
            if (!(v instanceof List<?> list)) {
                bad(r, where + " must be a list of ids");
                return out;
            }
            if (list.size() > max) bad(r, where + " has " + list.size() + " entries; at most " + max);
            Set<String> seen = new HashSet<>();
            for (Object o : list) {
                if (!(o instanceof String s) || !ID.matcher(s).matches()) bad(r, where + ": '" + o + "' is not a lower-case snake_case id");
                else if (!seen.add(s)) bad(r, where + ": '" + s + "' is listed twice");
                else out.add(s);
            }
            return out;
        }

        void readRequires(Raw r, Object v, String L) {
            if (v == null) return;
            if (!(v instanceof List<?> list)) {
                bad(r, L + "\"requires\" must be a list");
                return;
            }
            if (list.size() > REQ_PER_FORM) bad(r, L + "\"requires\" has " + list.size() + " entries; at most " + REQ_PER_FORM);
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                String at = L + "requires[" + i + "]: ";
                if (!(list.get(i) instanceof Map<?, ?> e)) {
                    bad(r, at + "must be an object like {\"item\": \"minecraft:leather\", \"count\": 16}");
                    continue;
                }
                for (Object k : e.keySet()) if (!Set.of("item", "tag", "count").contains(String.valueOf(k))) unknownKey(String.valueOf(k), Set.of("item", "tag", "count"), at.strip());
                Object item = e.get("item"), tag = e.get("tag");
                Integer count = integer(e.get("count"));
                if ((item == null) == (tag == null)) {
                    bad(r, at + "needs exactly one of \"item\" and \"tag\"");
                    continue;
                }
                if (count == null || count < REQ_MIN || count > REQ_TOP_MAX) {
                    bad(r, at + "\"count\" must be a whole number " + REQ_MIN + " to " + REQ_MAX + " (up to " + REQ_TOP_MAX + " for a top form); got " + e.get("count"));
                    continue;
                }
                String id = resource(tag == null ? item : tag, tag != null, at, r);
                if (id == null) continue;
                if (!seen.add((tag != null ? "#" : "") + id)) {
                    bad(r, at + "'" + id + "' is already required by this form");
                    continue;
                }
                r.requires.add(tag != null ? new HeroForm.Requirement("", id, count) : new HeroForm.Requirement(id, "", count));
            }
        }

        /** The normalised {@code namespace:path} of an item or tag id, or null after reporting why not. */
        String resource(Object v, boolean tag, String at, Raw r) {
            if (!(v instanceof String s0) || s0.isBlank()) {
                bad(r, at + "\"" + (tag ? "tag" : "item") + "\" must be a registry id text");
                return null;
            }
            String s = s0.strip().toLowerCase(Locale.ROOT);
            if (tag && s.startsWith("#")) s = s.substring(1);
            if (s.indexOf(':') < 0) s = "minecraft:" + s;
            if (s.length() > ID_MAX || !RESOURCE.matcher(s).matches()) {
                bad(r, at + "'" + s0 + "' is not a valid id (use namespace:path in lower case)");
                return null;
            }
            String ns = s.substring(0, s.indexOf(':'));
            if (tag ? !TAG_NAMESPACES.contains(ns) : !ITEM_NAMESPACES.contains(ns)) {
                bad(r, at + "'" + s + "' is in the namespace '" + ns + "'; " + (tag ? "tags must be minecraft:, c: or nusmp:" : "items must be minecraft:, tensura: or nusmp:"));
                return null;
            }
            if (!tag && (UNOBTAINABLE.contains(s) || s.endsWith("_spawn_egg"))) {
                bad(r, at + "'" + s + "' cannot be obtained in survival, so the requirement could never be met");
                return null;
            }
            return s;
        }

        Map<String, Double> stats(Object v, String where, double share, Raw r) {
            Map<String, Double> out = new LinkedHashMap<>();
            if (v == null) return out;
            if (!(v instanceof Map<?, ?> m)) {
                bad(r, where + " must be an object like {\"armor\": 2.0}");
                return out;
            }
            for (Map.Entry<?, ?> e : m.entrySet()) {
                String key = String.valueOf(e.getKey());
                double[] lim = LIMITS.get(key);
                if (lim == null) {
                    bad(r, where + ": unknown stat '" + key + "' (allowed: " + String.join(", ", HeroForm.STAT_KEYS) + ")");
                    continue;
                }
                if (!(e.getValue() instanceof Number n) || Double.isNaN(n.doubleValue()) || Double.isInfinite(n.doubleValue())) {
                    bad(r, where + ": '" + key + "' must be a number");
                    continue;
                }
                double val = n.doubleValue(), lo = lim[0] * share, hi = lim[1] * share;
                if (val < lo - EPS || val > hi + EPS) {
                    bad(r, where + ": " + key + " " + trim(val) + " is outside the budget " + trim(lo) + " to " + trim(hi));
                    continue;
                }
                out.put(key, val);
            }
            return out;
        }

        void readSkills(Raw r, Object v, String L) {
            if (v == null) return;
            if (!(v instanceof Map<?, ?> m)) {
                bad(r, L + "\"skills\" must be an object like {\"sneak\": \"" + kind.name().toLowerCase(Locale.ROOT) + ".some_skill\"}");
                return;
            }
            String prefix = kind.name().toLowerCase(Locale.ROOT) + ".";
            int ultimates = 0;
            Map<HeroTrigger, String> ordered = new java.util.EnumMap<>(HeroTrigger.class);
            for (Map.Entry<?, ?> e : m.entrySet()) {
                String slot = String.valueOf(e.getKey());
                HeroTrigger t = HeroTrigger.bySlot(slot);
                String at = L + "skills." + slot + ": ";
                if (t == null) {
                    bad(r, at + "unknown slot (allowed: use, sneak, key1, key2, key3)");
                    continue;
                }
                if (!slot.equals(t.slot())) {
                    bad(r, at + "slot words are lower case: \"" + t.slot() + "\"");
                    continue;
                }
                if (t == HeroTrigger.USE && (kind.style() == HeroStyle.BLOCK || kind.style() == HeroStyle.DRAW)) {
                    bad(r, at + "the " + kind.display() + " is " + kind.style() + "-style: its use key " + (kind.style() == HeroStyle.BLOCK ? "raises the guard" : "draws the bow")
                            + ", so techniques sit on sneak, key1, key2 and key3");
                    continue;
                }
                if (!(e.getValue() instanceof String id) || id.isBlank()) {
                    bad(r, at + "must be a skill id text");
                    continue;
                }
                if (id.length() > SKILL_ID_MAX) {
                    bad(r, at + "a skill id is at most " + SKILL_ID_MAX + " characters");
                    continue;
                }
                if (!id.startsWith(prefix)) {
                    bad(r, at + "'" + id + "' is not a skill of the " + kind.display() + " (its ids start with '" + prefix + "')");
                    continue;
                }
                if (skills != null && !skills.exists(id)) {
                    bad(r, at + "no skill '" + id + "' is registered by the weapon's logic");
                    continue;
                }
                if (skills != null && skills.ultimate(id)) ultimates++;
                ordered.put(t, id);
            }
            if (ultimates > 1) bad(r, L + "has " + ultimates + " ultimate techniques; a form has at most one");
            for (Map.Entry<HeroTrigger, String> e : ordered.entrySet()) r.skills.put(e.getKey().slot(), e.getValue());
        }

        void readSin(Raw r, Object v, String L) {
            if (v == null) {
                if (r.curse) bad(r, L + "a curse form needs a \"sin\" (one of " + sinIds() + ")");
                return;
            }
            Sin s = v instanceof String text ? Sin.byId(text) : null;
            if (s == null || !s.id().equals(v)) {
                bad(r, L + "\"sin\" must be one of " + sinIds() + " (lower case); got " + v);
                return;
            }
            r.sin = s;
            if (r.curse && kind.heroClass() == HeroClass.HOLY && !kind.sins().contains(s))
                bad(r, L + "the " + kind.display() + " carries " + kind.sins().stream().map(Sin::id).toList() + " in canon, not " + s.id() + " (Holy Weapons keep their sins)");
        }

        void readLook(Raw r, Object v, String L) {
            boolean holyCurse = r.curse && kind.heroClass() == HeroClass.HOLY;
            String want = holyCurse ? LOOK_CURSE : LOOK_BASE;
            if (v == null) {
                r.look = want;
                return;
            }
            if (!(v instanceof String s) || !(s.equals(LOOK_BASE) || s.equals(LOOK_CURSE))) {
                bad(r, L + "\"look\" must be \"base\" or \"curse\"");
                return;
            }
            r.look = s;
            if (!s.equals(want))
                bad(r, L + (holyCurse ? "a curse form of a Holy Weapon uses look \"curse\" (the weapon has a cursed mesh)"
                        : r.curse ? "a curse form of a Seven Star or Vassal weapon keeps look \"base\" (those weapons have no cursed mesh)" : "an ordinary form uses look \"base\""));
        }

        // -------------------------------------------------------------------------------------------- the rules between forms
        void link(List<Raw> raws) {
            Map<String, Raw> byId = new LinkedHashMap<>();
            for (Raw r : raws) {
                if (r.id.isEmpty()) continue;
                if (byId.containsKey(r.id)) {
                    errors.add(r.label() + " (#" + r.index + "): the id is used twice; the first one is kept");
                    r.bad = true;
                } else byId.put(r.id, r);
            }
            Set<String> children = new HashSet<>();
            for (Raw r : byId.values()) children.addAll(r.parents);
            cycles(byId);
            List<Raw> order = new ArrayList<>(byId.values());
            order.sort(Comparator.comparingInt((Raw r) -> r.tier).thenComparingInt(r -> r.index));
            Map<String, HeroForm> built = new LinkedHashMap<>();
            Map<String, Integer> tiers = new HashMap<>();
            for (Raw r : order) {
                if (r.bad) continue;
                if (!parentsOk(r, byId, built, tiers)) continue;
                topRule(r, children);
                if (r.bad) continue;
                HeroForm f = new HeroForm(kind, r.id, r.name, r.desc, r.tier, List.copyOf(r.parents), List.copyOf(r.requires), r.mastery,
                        Collections.unmodifiableMap(new LinkedHashMap<>(r.stats)), Collections.unmodifiableMap(new LinkedHashMap<>(r.bonus)),
                        Collections.unmodifiableMap(new LinkedHashMap<>(r.skills)), List.copyOf(r.passives), r.look, r.curse, r.sin, r.corruption);
                built.put(r.id, f);
                tiers.put(r.id, r.tier);
            }
            if (!built.containsKey(HeroForm.BASE_ID)) {
                reject("the base form was not accepted (see the errors above)");
                return;
            }
            accepted.addAll(built.values());                                  // built in (tier, file order)
        }

        boolean parentsOk(Raw r, Map<String, Raw> byId, Map<String, HeroForm> built, Map<String, Integer> tiers) {
            if (HeroForm.BASE_ID.equals(r.id)) return true;
            int maxParent = 0;
            for (String p : r.parents) {
                if (p.equals(r.id)) {
                    bad(r, r.label() + ": a form cannot be its own parent");
                    return false;
                }
                if (!byId.containsKey(p)) {
                    bad(r, r.label() + ": the parent '" + p + "' does not exist in this file");
                    return false;
                }
                if (!built.containsKey(p)) {
                    bad(r, r.label() + ": the parent '" + p + "' was not accepted (" + (byId.get(p).bad ? "it has errors" : "its tier " + byId.get(p).tier + " is not below " + r.tier) + "), so this form is skipped too");
                    return false;
                }
                maxParent = Math.max(maxParent, tiers.get(p));
            }
            if (r.tier != maxParent + 1) {
                bad(r, r.label() + ": \"tier\" is " + r.tier + " but its highest parent is tier " + maxParent + ", so it must be " + (maxParent + 1) + " (tier = depth in the tree)");
                return false;
            }
            return true;
        }

        /** A requirement above 64 is allowed only for a top form: nothing builds on it and it sits deep in the tree. */
        void topRule(Raw r, Set<String> hasChildren) {
            boolean top = r.tier >= TOP_TIER && !hasChildren.contains(r.id);
            for (HeroForm.Requirement q : r.requires)
                if (q.count() > REQ_MAX && !top)
                    bad(r, r.label() + ": a count of " + q.count() + " (above " + REQ_MAX + ") is only for a top form (tier " + TOP_TIER + " or deeper and nothing builds on it)");
        }

        /** Reports parent loops once, in words ("a -> b -> a"); the tier rule would also reject them, but this says why. */
        void cycles(Map<String, Raw> byId) {
            Set<String> done = new HashSet<>();
            for (String start : byId.keySet()) walk(start, byId, new ArrayList<>(), done, new HashSet<>());
        }

        boolean walk(String id, Map<String, Raw> byId, List<String> path, Set<String> done, Set<String> onPath) {
            if (done.contains(id) || !byId.containsKey(id)) return false;
            if (onPath.contains(id)) {
                int at = path.indexOf(id);
                List<String> loop = new ArrayList<>(path.subList(at, path.size()));
                loop.add(id);
                errors.add("the parents form a loop: " + String.join(" -> ", loop) + "; the forms in it are skipped");
                for (String member : loop) {
                    byId.get(member).bad = true;
                    done.add(member);
                }
                return true;
            }
            onPath.add(id);
            path.add(id);
            for (String p : byId.get(id).parents) if (walk(p, byId, path, done, onPath)) return true;
            path.remove(path.size() - 1);
            onPath.remove(id);
            done.add(id);
            return false;
        }

        // -------------------------------------------------------------------------------------------- the rules about the file as a whole (warnings)
        void fileRules() {
            int ordinary = 0, curse = 0;
            Set<Sin> sinsWithForms = new HashSet<>();
            for (HeroForm f : accepted) {
                if (f.curse()) {
                    curse++;
                    if (f.sin() != null) sinsWithForms.add(f.sin());
                } else ordinary++;
            }
            boolean holy = kind.heroClass() == HeroClass.HOLY;
            int lo = holy ? HOLY_ORDINARY_MIN : OTHER_ORDINARY_MIN, hi = holy ? HOLY_ORDINARY_MAX : OTHER_ORDINARY_MAX;
            if (ordinary < lo || ordinary > hi)
                warnings.add("the " + kind.display() + " has " + ordinary + " ordinary (non-curse) forms including the base form; the design asks for " + lo + " to " + hi);
            if (curse > CURSE_FORMS_MAX) warnings.add("the file has " + curse + " curse forms; at most " + CURSE_FORMS_MAX);
            if (holy) for (Sin s : kind.sins()) if (!sinsWithForms.contains(s)) warnings.add("the " + kind.display() + " carries " + s.id() + " in canon but the file has no curse form with \"sin\": \"" + s.id() + "\"");
        }
    }

    // ------------------------------------------------------------------------------------------------ small helpers
    static String trim(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e9) return String.valueOf((long) v);
        String s = String.format(Locale.ROOT, "%.4f", v);
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') end--;
        if (end > 0 && s.charAt(end - 1) == '.') end--;
        return s.substring(0, end);
    }

    private static String sinIds() {
        List<String> ids = new ArrayList<>();
        for (Sin s : Sin.values()) ids.add(s.id());
        return String.join(", ", ids);
    }

    /** The known key closest to a misspelt one: one letter away for a short key, two for a longer one (so "lore" is not taken for "look"); null when none is. */
    static String closest(String key, Set<String> known) {
        String best = null;
        int bestD = (key.length() <= 5 ? 1 : 2) + 1;
        for (String k : known) {
            int d = distance(key.toLowerCase(Locale.ROOT), k.toLowerCase(Locale.ROOT));
            if (d < bestD) {
                bestD = d;
                best = k;
            }
        }
        return best;
    }

    private static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++)
                cur[j] = Math.min(Math.min(cur[j - 1], prev[j]) + 1, prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }
}
