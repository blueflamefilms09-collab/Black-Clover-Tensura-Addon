package com.newuniverse.nusmp.saiyan;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the extensible registry of the Saiyan Ultimate Skills (3.65.0, Stage 5), pure data with no Minecraft type. Each entry carries the RACE it is restricted to (so another race can never take a
 * Saiyan Ultimate) and the awakening requirements, so Vegeta, Broly, Gohan and Frieza are later new rows, not a rewrite. Frieza stays a racial evolution of its own line and is NOT a Saiyan Ultimate. A
 * Saiyan holds at most ONE primary Ultimate at a time. The first and only entry is Son Goku, King of Martial Arts: the owner's decision for the old {@code KiSkill} / {@code db_goku} content was to
 * keep it and add this path next to it, so nothing there is touched or replaced.
 */
public final class SaiyanUltimates {
    private SaiyanUltimates() {}

    public static final String GOKU = "goku";
    public static final String RACE_SAIYAN = "saiyan";

    /** What the awakening asks for (the server can change every number). {@code flightTier}/{@code senseTier} are tier indexes (0 = the first). */
    public record Need(double bp, int techniques, int flightTier, int senseTier, int hardMartialKills, int zenkai, int trialKills) {
        public static final Need GOKU_DEFAULT = new Need(100_000.0, 5, 2, 1, 1, 1, 10);
    }

    /** What a Saiyan has done, as the awakening reads it. */
    public record Progress(double bp, int techniquesMastered, int flightTier, int senseTier, int hardMartialKills, int zenkai, int trialKills) {}

    /** One Ultimate: its id (a save key, never renamed), the race it is restricted to, the default requirements. */
    public record Ultimate(String id, String race, Need need) {
        public String nameKey() { return "nusmp.saiyan.ultimate." + id; }
        public String epithetKey() { return "nusmp.saiyan.ultimate." + id + ".epithet"; }
    }

    public static final List<Ultimate> ALL = List.of(new Ultimate(GOKU, RACE_SAIYAN, Need.GOKU_DEFAULT));

    public static Ultimate byId(String id) {
        if (id != null) for (Ultimate u : ALL) if (u.id().equals(id)) return u;
        return null;
    }

    /** Is this Ultimate open to a player of this race? */
    public static boolean openTo(Ultimate u, String race) { return u != null && u.race().equals(race); }

    /** A Saiyan may take a new primary Ultimate only while holding none. */
    public static boolean mayTake(String current) { return current == null || current.isEmpty(); }

    /** What is still missing for the awakening, in words (empty = ready). */
    public static List<String> missing(Need n, Progress p) {
        List<String> out = new ArrayList<>();
        if (p.bp() < n.bp()) out.add("Battle Power " + Math.round(n.bp()) + " (you have " + Math.round(p.bp()) + ")");
        if (p.techniquesMastered() < n.techniques()) out.add("master " + n.techniques() + " universal techniques (you have " + p.techniquesMastered() + ")");
        if (p.flightTier() < n.flightTier()) out.add("Ki Flight tier " + SaiyanFlightRules.Tier.of(n.flightTier()).name().toLowerCase());
        if (p.senseTier() < n.senseTier()) out.add("Saiyan Senses tier " + SaiyanSenseRules.Tier.of(n.senseTier()).name().toLowerCase());
        if (p.hardMartialKills() < n.hardMartialKills()) out.add("defeat a challenging opponent with martial arts (" + p.hardMartialKills() + "/" + n.hardMartialKills() + ")");
        if (p.zenkai() < n.zenkai()) out.add("survive a major battle against a stronger enemy (Zenkai " + p.zenkai() + "/" + n.zenkai() + ")");
        if (p.trialKills() < n.trialKills()) out.add("complete the Trial of the Martial King (" + p.trialKills() + "/" + n.trialKills() + " hard martial victories)");
        return out;
    }

    public static boolean ready(Need n, Progress p) { return missing(n, p).isEmpty(); }
}
