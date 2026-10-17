package com.newuniverse.nusmp.avatar.aptitude;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: Magic Aptitudes are datapack data ({@code data/<namespace>/avatar_aptitudes/<id>.json}, one file per aptitude, the file name is the id), so the
 * server owner can retune a tax or add a family without a rebuild. The reader is strict about what is WRONG (an unknown rarity, a tax that is not a number,
 * a skill id that cannot be a resource location) and tolerant about what is merely missing (every field has a default); a file that throws here is skipped
 * and logged by {@link AptitudeLoader}, never allowed to break the reload. Pure (Gson only), so the shipped files and bad files are both unit tested.
 *
 * <p>Format (all keys optional except none; defaults in brackets):
 * <pre>
 * { "name": "Fire",                                   [the id]
 *   "rarity": "COMMON",                               [COMMON]  COMMON | UNCOMMON | RARE | EPIC | LEGENDARY
 *   "tax_percent": 0,                                 [0]       permanent percent of the creator's maximum magicule when picked BY HAND (0..90)
 *   "magic": "flame",                                 [""]      a MagicType name in lower case ("flame", "spatial", "anti_magic"), "" for a Tensura-only aptitude
 *   "skills": ["tensura:flame_manipulation"],         [[]]      Tensura skill ids the Avatar may use because of it
 *   "tint": "#FF6A1E",                                [rarity colour]
 *   "icon": "nusmp:textures/gui/soulforge/apt/fire.png",  [that path]
 *   "description": "One line of flavour." }           [""]
 * </pre>
 */
public final class AptitudeParser {
    private AptitudeParser() {}

    /** The aptitude and its look, read from one file. */
    public record Parsed(Aptitude aptitude, AptitudeLook look) {}

    public static final int MAX_SKILLS = 8, MAX_DESCRIPTION = 200;

    public static Parsed parse(String id, JsonObject o) {
        if (!AptitudeLook.validId(id)) throw new IllegalArgumentException("an aptitude id is snake_case: " + id);
        String who = "aptitude " + id;
        String name = ForgeJson.str(who, o, "name", id).strip();
        if (name.isEmpty() || name.length() > 48) throw new IllegalArgumentException(who + ": name must be 1..48 characters");
        Aptitude.Rarity rarity = rarity(who, ForgeJson.str(who, o, "rarity", "COMMON"));
        double tax = ForgeJson.num(who, o, "tax_percent", 0.0);
        if (tax < 0.0 || tax > 90.0) throw new IllegalArgumentException(who + ": tax_percent must be 0..90, got " + tax);
        String magic = ForgeJson.str(who, o, "magic", "").strip().toLowerCase(Locale.ROOT);
        if (!magic.isEmpty() && !magic.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException(who + ": magic must be a lower case MagicType name");
        List<String> skills = new ArrayList<>();
        if (o.has("skills")) {
            if (!o.get("skills").isJsonArray()) throw new IllegalArgumentException(who + ": 'skills' must be a list");
            JsonArray arr = o.getAsJsonArray("skills");
            if (arr.size() > MAX_SKILLS) throw new IllegalArgumentException(who + ": at most " + MAX_SKILLS + " skills");
            for (JsonElement e : arr) {
                String s = e.isJsonPrimitive() ? e.getAsString().trim().toLowerCase(Locale.ROOT) : "";
                if (!ForgeJson.looksLikeResourceId(s)) throw new IllegalArgumentException(who + ": '" + s + "' is not a skill id (namespace:path)");
                if (!skills.contains(s)) skills.add(s);
            }
        }
        int tint = ForgeJson.colour(who, o, "tint", AptitudeLook.rarityColour(rarity));
        String icon = ForgeJson.str(who, o, "icon", AptitudeLook.defaultIcon(id)).strip();
        if (!ForgeJson.looksLikeResourceId(icon)) throw new IllegalArgumentException(who + ": icon must be a resource location");
        String description = ForgeJson.str(who, o, "description", "").strip();
        if (description.length() > MAX_DESCRIPTION) throw new IllegalArgumentException(who + ": description is longer than " + MAX_DESCRIPTION);
        return new Parsed(new Aptitude(id, name, rarity, tax, magic, skills), new AptitudeLook(tint, icon, description));
    }

    private static Aptitude.Rarity rarity(String who, String s) {
        for (Aptitude.Rarity r : Aptitude.Rarity.values()) if (r.name().equalsIgnoreCase(s.strip())) return r;
        throw new IllegalArgumentException(who + ": unknown rarity '" + s + "'");
    }
}
