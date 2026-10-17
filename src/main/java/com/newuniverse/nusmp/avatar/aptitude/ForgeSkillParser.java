package com.newuniverse.nusmp.avatar.aptitude;

import com.google.gson.JsonObject;

import java.util.Locale;

/**
 * Why: the skill list of the Soul Forge is datapack data ({@code data/<namespace>/avatar_skills/<file>.json}, one skill per file) so the owner can add a
 * skill from another mod, retune a tax or take a skill out of the forge with a data pack alone. The reader is strict about what is wrong (an id that cannot
 * be a resource location, an unknown kind, a tax outside 0..90) and tolerant about what is missing; a bad file is skipped and logged by
 * {@link ForgeSkillLoader}. The file name is only a label (several packs may name a skill differently); the identity is the {@code id} key. Pure (Gson only).
 *
 * <p>Format:
 * <pre>
 * { "id": "tensura:predator",                    [required] a skill that exists in the registry; "tensura:" ids are checked against the owner's id reference
 *   "kind": "unique",                            [required] unique | ultimate   (the registry's own category wins at runtime)
 *   "name": "Predator",                          [the id path]
 *   "ep_tax": 6.0,                               [0]  permanent percent of the creator's maximum magicule (0..90)
 *   "min_ep": 800,                               [0]  the creator's maximum EP must be at least this
 *   "tint": "#8A3CFF",                           [kind colour]
 *   "icon": "",                                  [""] a texture override; empty = the skill's own icon from the registry
 *   "description": "One short line." }
 * </pre>
 */
public final class ForgeSkillParser {
    private ForgeSkillParser() {}

    public static final int UNIQUE_TINT = 0x5CD8FF, ULTIMATE_TINT = 0xE8C468, MAX_DESCRIPTION = 200;

    public static ForgeSkill parse(String file, JsonObject o) {
        String who = "skill file " + file;
        String id = ForgeJson.str(who, o, "id", "").strip().toLowerCase(Locale.ROOT);
        if (!ForgeJson.looksLikeResourceId(id)) throw new IllegalArgumentException(who + ": 'id' must be a skill id (namespace:path), got '" + id + "'");
        ForgeSkill.Kind kind = ForgeSkill.Kind.byName(ForgeJson.str(who, o, "kind", ""));
        if (kind == null) throw new IllegalArgumentException(who + ": 'kind' must be unique or ultimate");
        String name = ForgeJson.str(who, o, "name", "").strip();
        if (name.length() > 48) throw new IllegalArgumentException(who + ": name is longer than 48");
        double tax = ForgeJson.num(who, o, "ep_tax", 0.0);
        if (tax < 0.0 || tax > 90.0) throw new IllegalArgumentException(who + ": ep_tax must be 0..90, got " + tax);
        double minEp = ForgeJson.num(who, o, "min_ep", 0.0);
        if (minEp < 0.0) throw new IllegalArgumentException(who + ": min_ep must not be negative");
        int tint = ForgeJson.colour(who, o, "tint", kind == ForgeSkill.Kind.UNIQUE ? UNIQUE_TINT : ULTIMATE_TINT);
        String icon = ForgeJson.str(who, o, "icon", "").strip();
        if (!icon.isEmpty() && !ForgeJson.looksLikeResourceId(icon)) throw new IllegalArgumentException(who + ": icon must be a resource location");
        String description = ForgeJson.str(who, o, "description", "").strip();
        if (description.length() > MAX_DESCRIPTION) throw new IllegalArgumentException(who + ": description is longer than " + MAX_DESCRIPTION);
        return new ForgeSkill(id, kind, name, tax, minEp, tint, icon, description);
    }
}
