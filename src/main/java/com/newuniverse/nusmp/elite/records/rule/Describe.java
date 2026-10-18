package com.newuniverse.nusmp.elite.records.rule;

import com.newuniverse.nusmp.elite.records.rule.Bonuses.Attr;
import com.newuniverse.nusmp.elite.records.rule.Bonuses.Effect;
import com.newuniverse.nusmp.elite.records.rule.Defs.RecordDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the screen and the commands must say in words what a title needs and what it gives, and the sentences must come from the data (a datapack title with a new requirement must read right without a
 * code change). These pure functions turn a requirement, a condition or a bonus set into short English lines. The server builds the requirement lines into the sync packet, the client builds the bonus
 * lines from the synced bonuses. English only, like the record and title texts in the data files themselves; the fixed words of the screen are translatable ({@code RecordsText}).
 */
public final class Describe {
    private Describe() {}

    private static final Map<String, String> STAT_LINES = Map.of(
            StatNames.XP_BONUS_PERCENT, "+%s%% experience from kills",
            StatNames.KILL_HEAL, "Heal %s HP when you kill something",
            StatNames.BOSS_DAMAGE_PERCENT, "+%s%% damage to bosses",
            StatNames.FORGE_QUALITY_BONUS, "+%s forge quality chance",
            StatNames.COIN_BONUS_PERCENT, "+%s%% Tensura coins",
            StatNames.EP_GAIN_PERCENT, "+%s%% EP gained",
            StatNames.WAR_DAMAGE_PERCENT, "+%s%% damage in wars",
            StatNames.CALAMITY_DAMAGE_PERCENT, "+%s%% damage to calamities",
            StatNames.COUNCIL_VOTE_WEIGHT, "+%s council vote weight");

    /** "1,234" or "1.5" without trailing zeros. */
    public static String num(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.format(Locale.ROOT, "%,d", (long) Math.rint(v));
        String s = String.format(Locale.ROOT, "%.2f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    private static String pretty(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        path = path.substring(path.lastIndexOf('.') + 1);                // "generic.max_health" -> "max_health"
        StringBuilder sb = new StringBuilder();
        for (String w : path.split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private static String roman(int n) {
        String[] r = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return r[Math.max(0, Math.min(r.length - 1, n - 1))];
    }

    /** One attribute bonus: "+2% Movement Speed", "+0.5 Attack Damage". */
    public static String attr(Attr a) {
        boolean mult = a.op() != Bonuses.Op.ADD_VALUE;
        double v = mult ? a.amount() * 100.0 : a.amount();
        return (v >= 0 ? "+" : "") + num(v) + (mult ? "%" : "") + " " + pretty(a.attribute());
    }

    public static String effect(Effect e) { return pretty(e.effect()) + " " + roman(e.amplifier() + 1); }

    public static String stat(String name, double v) {
        String fmt = STAT_LINES.get(name);
        return fmt == null ? pretty(name) + " " + num(v) : String.format(Locale.ROOT, fmt, num(v));
    }

    /** All the lines of a bonus set: attributes, effects, then stats. */
    public static List<String> bonusLines(Bonuses b) {
        List<String> out = new ArrayList<>();
        for (Attr a : b.attributes()) out.add(attr(a));
        for (Effect e : b.effects()) out.add(effect(e));
        for (var s : b.stats().entrySet()) out.add(stat(s.getKey(), s.getValue()));
        return out;
    }

    /** What a single condition asks, for a title requirement line (records have their own description). */
    public static String condition(Condition c) {
        return switch (c.type()) {
            case BIOME_DISCOVERY -> c.str("biome", "").isEmpty() ? "Discover " + num(c.num("count", 1)) + " biomes" : "Discover the " + pretty(c.str("biome", "")) + " biome";
            case STRUCTURE_DISCOVERY -> c.str("structure", "").isEmpty() ? "Find " + num(c.num("count", 1)) + " kinds of structure" : "Find: " + pretty(c.str("structure", "").replace("*", ""));
            case CALAMITY_DAMAGE -> "Deal " + num(c.num("amount", 0)) + " damage to calamities";
            case CALAMITY_KILL -> "Kill " + num(c.num("count", 1)) + " calamities";
            case DEATH_COUNT -> "Die " + num(c.num("count", 1)) + " times";
            case DISTANCE_TRAVELED -> "Travel " + num(c.num("meters", 0) / 1000.0) + " km (" + c.str("mode", "all") + ")";
            case EP_REACHED -> "Reach " + num(c.num("ep", 0)) + " EP";
            case FORGE_QUALITY -> "Forge an item of quality " + c.str("quality", "1");
            case FORGE_COUNT -> "Forge " + num(c.num("count", 1)) + " items" + (c.has("min_quality") ? " of quality " + c.str("min_quality", "") + "+" : "");
            case FTB_CHUNKS_CLAIMED -> "Hold " + num(c.num("count", 1)) + " claimed chunks";
            case DEMON_LORD_STATUS -> "Be a Demon Lord (" + c.str("status", "true") + ")";
            case HIGHER_BEING_STATUS -> "Be a higher being (" + c.str("status", "spiritual") + ")";
            case TRUE_HERO_STATUS -> "Be a True Hero (" + c.str("status", "true") + ")";
            case KILL_ENTITY -> "Kill " + num(c.num("count", 1)) + " " + pretty(c.str("entity", "creatures").replace("*", ""));
            case KILL_COUNT -> "Kill " + num(c.num("count", 1)) + " creatures (" + c.str("group", "all") + ")";
            case SINGLE_HIT_DAMAGE -> "Deal " + num(c.num("damage", 0)) + " damage in one hit";
            case SKILL_LEARNED -> c.str("skill", "").isEmpty() ? "Learn " + num(c.num("count", 1)) + " skills" : "Learn " + pretty(c.str("skill", ""));
            case WALPURGIS_ATTENDED -> "Attend " + num(c.num("count", 1)) + " Walpurgis councils";
            case WAR_KILLS -> "Make " + num(c.num("count", 1)) + " war kills";
            case WAR_WINS -> "Win " + num(c.num("count", 1)) + " wars";
        };
    }

    /** One line for a title requirement; record names come from the catalog. */
    public static String requirement(Requirement r, Catalog cat) {
        switch (r.kind()) {
            case RECORD: {
                RecordDef d = cat.record(r.id());
                return "Record: " + (d == null ? r.id() : d.name());
            }
            case EP:
                return "Reach " + num(r.number()) + " EP";
            case CONDITION:
                return condition(r.condition());
            case TITLE: {
                var t = cat.title(r.id());
                return "Title: " + (t == null ? r.id() : t.name());
            }
            case RECORDS: {
                String cat0 = r.category() == null ? "" : " " + r.category().id();
                String rar = r.rarity() == null ? "" : " of " + r.rarity().id() + " or better";
                return "Unlock " + num(r.number()) + cat0 + " records" + rar;
            }
            case POINTS:
                return "Reach " + num(r.number()) + " record points";
            case ANY: {
                List<String> parts = new ArrayList<>();
                for (Requirement x : r.anyOf()) parts.add(requirement(x, cat));
                return "Any of: " + String.join("; ", parts);
            }
            default:
                return "?";
        }
    }
}
