package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: everything the mod needs to know about one named character of the arcs, in one immutable value that has no game types: the boss bar and glow
 * colours, the stats of a boss, the floors and road a floor boss may stand on, the story prerequisites that unlock it, the skills its Charter Grimoire
 * lends, and the lines it speaks. Boss classes, the NPC entity, the floor-boss picker, the story unlock and the advancement generator all read this
 * one record, so a character is defined once (in its own file) and cannot drift between systems. It is pure so it can be unit tested and listed by a
 * command without a running game. Build one with {@link #boss} or {@link #npc}.
 *
 * @param id             snake_case id, also the entity path of a boss ({@code heath_grice}) and the character key of an NPC
 * @param name           display name ("Heath Grice")
 * @param title          epithet ("the Death Knight"), may be empty
 * @param source         the work it comes from ("Black Clover", "The Seven Deadly Sins", "DanMachi")
 * @param saga           the arc it belongs to
 * @param role           boss or NPC
 * @param tier           threat tier 1..5 (the briefs' tiers: 1 minor, 2 Eye of the Midnight Sun, 3 the Seven Deadly Sins, 4 Spade Kingdom, 5 the god tier)
 * @param color          main colour 0xRRGGBB of its glow, telegraphs and boss bar
 * @param accent         lighter accent colour 0xRRGGBB
 * @param devil          the devil whose style framework lines it borrows; {@link Devil#LILITH} (neutral) for everyone not bound to a devil
 * @param magic          the id of the magic family it fights with (an existing {@code MagicType} id such as "gravity"), or empty
 * @param health         boss maximum health (0 for an NPC)
 * @param attack         boss melee damage
 * @param speed          boss movement speed
 * @param armor          boss armor
 * @param road           the road a floor boss belongs to ({@link Polarity#HOLY}, {@link Polarity#ABYSSAL}) or null for both
 * @param floorMin       lowest absolute floor number a floor boss may stand on (0 = never a floor boss)
 * @param floorMax       highest absolute floor number a floor boss may stand on
 * @param themes         ids of the dungeon themes it favours (empty = any)
 * @param prerequisites  ids of characters whose defeat unlocks this one in the story chain
 * @param charterSkills  ids ("tensura:gravity_field") its Charter Grimoire lends; empty = no grimoire
 * @param charterChance  chance of the Charter Grimoire drop on the first kill (0..1)
 * @param opening        line when the fight begins
 * @param gates          line when it draws its full power
 * @param death          last line
 * @param arena          id of the story arena it is fought in (empty = no story arena)
 */
public record ArcCharacter(String id, String name, String title, String source, ArcSaga saga, ArcRole role, int tier, int color, int accent, Devil devil,
                           String magic, double health, double attack, double speed, double armor, Polarity road, int floorMin, int floorMax,
                           List<String> themes, List<String> prerequisites, List<String> charterSkills, double charterChance,
                           String opening, String gates, String death, String arena) {

    public ArcCharacter {
        if (id == null || !id.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("a character id is snake_case: " + id);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("a character needs a name: " + id);
        title = title == null ? "" : title;
        source = source == null ? "" : source;
        saga = saga == null ? ArcSaga.ALLIES : saga;
        role = role == null ? ArcRole.NPC : role;
        tier = Math.max(1, Math.min(5, tier));
        devil = devil == null ? Devil.LILITH : devil;
        magic = magic == null ? "" : magic;
        themes = themes == null ? List.of() : List.copyOf(themes);
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
        charterSkills = charterSkills == null ? List.of() : List.copyOf(charterSkills);
        charterChance = Math.max(0.0, Math.min(1.0, charterChance));
        opening = opening == null ? "" : opening;
        gates = gates == null ? "" : gates;
        death = death == null ? "" : death;
        arena = arena == null ? "" : arena;
    }

    /** "Heath Grice, the Death Knight" or just the name. */
    public String fullName() { return title.isEmpty() ? name : name + ", " + title; }

    public boolean isBoss() { return role == ArcRole.BOSS; }

    public boolean isNpc() { return role == ArcRole.NPC; }

    /** May this character stand as the floor boss of this absolute floor on this road? */
    public boolean standsOn(int floor, Polarity onRoad) {
        if (!isBoss() || floorMax <= 0) return false;
        int f = Math.abs(floor);
        return f >= floorMin && f <= floorMax && (road == null || road == onRoad);
    }

    /** The key of its Charter Grimoire flag and item suffix ({@code charter_grimoire_<id>}). */
    public String charterId() { return id; }

    /** Does a defeat of it give a Charter Grimoire at all? */
    public boolean hasCharter() { return isBoss() && !charterSkills.isEmpty(); }

    /** Starts a boss definition. */
    public static Builder boss(String id, String name) { return new Builder(id, name, ArcRole.BOSS); }

    /** Starts an NPC definition. */
    public static Builder npc(String id, String name) { return new Builder(id, name, ArcRole.NPC); }

    /** Fluent builder with the defaults a typical character wants (tier 1, neutral devil, grey palette, 500 health). */
    public static final class Builder {
        private final String id, name;
        private final ArcRole role;
        private String title = "", source = "", magic = "", opening = "", gates = "", death = "", arena = "";
        private ArcSaga saga = ArcSaga.ALLIES;
        private int tier = 1, color = 0x8A8A9A, accent = 0xC8C8D8, floorMin, floorMax;
        private Devil devil = Devil.LILITH;
        private double health = 500, attack = 10, speed = 0.3, armor = 4, charterChance = 0.25;
        private Polarity road;
        private final List<String> themes = new ArrayList<>(), prerequisites = new ArrayList<>(), charterSkills = new ArrayList<>();

        private Builder(String id, String name, ArcRole role) {
            this.id = id;
            this.name = name;
            this.role = role;
        }

        public Builder title(String v) { title = v; return this; }
        public Builder source(String v) { source = v; return this; }
        public Builder saga(ArcSaga v) { saga = v; return this; }
        public Builder tier(int v) { tier = v; return this; }
        public Builder colors(int main, int rim) { color = main; accent = rim; return this; }
        public Builder devil(Devil v) { devil = v; return this; }
        public Builder magic(String v) { magic = v; return this; }
        public Builder stats(double health, double attack, double speed, double armor) { this.health = health; this.attack = attack; this.speed = speed; this.armor = armor; return this; }
        /** Makes it a floor boss of this road (null = either) for the absolute floors {@code min..max}. */
        public Builder floors(Polarity onRoad, int min, int max) { road = onRoad; floorMin = min; floorMax = max; return this; }
        public Builder themes(String... v) { themes.addAll(List.of(v)); return this; }
        public Builder after(String... v) { prerequisites.addAll(List.of(v)); return this; }
        /** The Charter Grimoire: the tensura skill ids it lends and the first-kill chance. */
        public Builder charter(double chance, String... skills) { charterChance = chance; for (String s : skills) charterSkills.add(s.contains(":") ? s : "tensura:" + s); return this; }
        public Builder lines(String opening, String gates, String death) { this.opening = opening; this.gates = gates; this.death = death; return this; }
        public Builder arena(String v) { arena = v; return this; }

        public ArcCharacter build() {
            return new ArcCharacter(id.toLowerCase(Locale.ROOT), name, title, source, saga, role, tier, color, accent, devil, magic, health, attack, speed, armor, road,
                    floorMin, floorMax, themes, prerequisites, charterSkills, charterChance, opening, gates, death, arena);
        }
    }
}
