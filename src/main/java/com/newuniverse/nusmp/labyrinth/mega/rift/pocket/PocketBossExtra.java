package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.google.gson.JsonObject;

/**
 * Why: a pocket's mega-boss is the Labyrinth Guardian wearing a D&amp;D profile (phases, legendary and lair actions, a mythic second life: {@code guard/DndProfile}); what makes it a MEGA-boss
 * and not a Decennial one is a handful of extra numbers that the profile has no place for, kept in the same file under {@code "mega"} so one boss is one file: how much stronger it hits than the
 * Guardian of that floor (damage), how much larger it stands (scale), the words it says when it wakes and when it falls, and the court it calls at half health (so many of the pocket's own
 * monsters, by id, once). Pure, strict, clamped, like every profile reader here.
 *
 * @param damage multiplier on the Guardian's attack damage (1 to 3)
 * @param scale  multiplier on its size (1 to 1.6)
 * @param adds   id of the pocket mob called at half health ("" = none), {@code addCount} of them
 */
public record PocketBossExtra(double damage, double scale, String intro, String death, String adds, int addCount) {
    public static final PocketBossExtra NONE = new PocketBossExtra(1.0, 1.0, "", "", "", 0);

    public static PocketBossExtra parse(String where, JsonObject profile) {
        if (!profile.has("mega")) return NONE;
        if (!profile.get("mega").isJsonObject()) throw new IllegalArgumentException(where + ": 'mega' is not an object");
        JsonObject m = profile.getAsJsonObject("mega");
        String adds = m.has("adds") ? PocketDef.str(m, "adds") : "";
        if (!adds.isEmpty() && !PocketDef.ID.matcher(adds).matches()) throw new IllegalArgumentException(where + ": mega.adds '" + adds + "' is not a mob id");
        return new PocketBossExtra(PocketDef.clampD(PocketDef.num(m, "damage", 1.0), 1.0, 3.0), PocketDef.clampD(PocketDef.num(m, "scale", 1.0), 1.0, 1.6), m.has("intro") ? PocketDef.str(m, "intro") : "",
                m.has("death") ? PocketDef.str(m, "death") : "", adds, PocketDef.clampI((int) PocketDef.num(m, "add_count", 0), 0, 6));
    }
}
