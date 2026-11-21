package com.newuniverse.nusmp.devil.dual;

import com.newuniverse.nusmp.core.CatchLog;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.devil.DevilTier;
import com.newuniverse.nusmp.devil.WorldDevil;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Rankings;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Why: devils have ranks like the anime (lesser, middle, greater ...), and the owner's rule is that the mod has ONE ranking language, the relics': {@link Grade} (names, colours, stars) and the place inside
 * a grade ({@link Rankings.Rank}). So no ranking text is invented here. The four {@link DevilTier}s map to four Grades ({@link #gradeOf}); a devil's own place inside its Grade is data: the {@code devils}
 * object of {@code assets/anime_multiverse/rankings.json} (devil id to {@code "grade:place"}), read by the same {@link Rankings} parser as the items. A devil missing from the table takes its tier's Grade
 * with no place, so a devil appended later is never unranked. The tier enum is untouched.
 */
public final class DevilRanks {
    private DevilRanks() {}

    private static volatile Rankings.Table table;

    /** Low is Rare, Mid is Unique, High is Legend, Supreme is God. Genesis and above stay with the relics. */
    public static Grade gradeOf(DevilTier t) {
        return switch (t == null ? DevilTier.NONE : t) { case LOW -> Grade.RARE; case MID -> Grade.UNIQUE; case HIGH -> Grade.LEGEND; case SUPREME -> Grade.GOD; default -> Grade.NORMAL; };
    }

    /** Parses the {@code devils} object of a rankings file into the same table type the items use. */
    public static Rankings.Table parse(JsonObject root) {
        JsonObject items = root != null && root.has("devils") && root.get("devils").isJsonObject() ? root.getAsJsonObject("devils") : new JsonObject();
        JsonObject wrap = new JsonObject();
        wrap.add("items", items);
        return Rankings.parse(wrap);
    }

    public static Rankings.Table table() {
        Rankings.Table t = table;
        if (t != null) return t;
        try (InputStream in = DevilRanks.class.getResourceAsStream("/assets/anime_multiverse/rankings.json")) {
            t = in == null ? new Rankings.Table() : parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Exception unreadable) {
            CatchLog.once("rankings.json (devil ranks)", unreadable);
            t = new Rankings.Table();
        }
        table = t;
        return t;
    }

    /** The rank of a devil by its id ("lucifero", "pact_ignivar"): the table's, or the tier's Grade with no place. */
    public static Rankings.Rank of(String devilId, DevilTier tier) {
        Rankings.Rank r = devilId == null ? null : table().of(devilId);
        return r != null ? r : new Rankings.Rank(gradeOf(tier), 0, 0, "tier");
    }

    public static Rankings.Rank of(WorldDevil d) { return of(d.id(), d.tier()); }

    /** The old grimoire devils (Ignivar, Verbael, Gravemaw, Kairovore) are ranked by name; their tier depends on the price, so the table gives them a Grade of their own. */
    public static Rankings.Rank ofPact(String pactName, DevilTier tier) {
        return of(pactName == null ? null : "pact_" + pactName.toLowerCase(java.util.Locale.ROOT), tier);
    }

    /** The stars and the label of the relics' tooltip: "★★★★★☆☆  Legend Grade #1". */
    public static String line(Rankings.Rank r) { return r.grade().stars() + "  " + r.label(); }
}
