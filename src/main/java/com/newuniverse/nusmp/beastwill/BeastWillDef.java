package com.newuniverse.nusmp.beastwill;

import java.util.List;
import java.util.Locale;

/**
 * Why: the definition of ONE Beast Will, a plain record with no Minecraft types (3.45.0). A will is a data file ({@code data/nusmp/beast_will/<id>.json}, read by {@link BeastWillSchema}); this record is what the
 * rest of the system reads, so a new will is a new file and no new rule. Everything a will may do comes from this record and the bounds in {@link BeastWillRules}; what a kind of ability DOES is Java
 * ({@code BeastWillAbilities}), what it costs, how often and how strongly is data.
 *
 * @param id               the file name and the save key (lower snake_case), never renamed
 * @param name             what the player reads (the lang key {@code nusmp.beastwill.<id>.name} wins when it exists)
 * @param beast            the creature the will comes from, for the preview and the core
 * @param affinity         the element or theme the will leans on (a label for the preview)
 * @param legendary        true = a legendary will (rarer, slower to assimilate); false = a rare one
 * @param minRank          the lowest rank of beast that gives it
 * @param sources          entity type ids that can give it ({@code namespace:path})
 * @param keywords         entity id path pieces that also count (checked on the Tensura namespace only)
 * @param dropChance       the chance one qualifying kill drops the core (0..0.05), before the server's scale
 * @param requireEp        the host's maximum EP needed to absorb the core
 * @param hostRaces        the race names of the Multiverse profile that fit the will (the others fit worse, see {@code otherHostCompat})
 * @param otherHostCompat  how well another race fits (0.2..1.0)
 * @param assimilationSeconds seconds of held form (or equal training) that fully assimilate the will
 * @param masterySeconds   the same for mastery
 * @param assimilationDayCap the most assimilation (a share of the whole) one day can give
 * @param masteryDayCap    the most mastery one day can give
 * @param coreCooldownDays days a player waits between two cores of this will
 * @param tint             the colour of the will's glow (0xRRGGBB)
 * @param passive          what the will gives while merely bound (small, no cost)
 * @param acquire          the first phase
 * @param integrate        the second phase
 */
public record BeastWillDef(String id, String name, String beast, String affinity, boolean legendary, BeastRank minRank, List<String> sources, List<String> keywords, double dropChance, double requireEp,
                           List<String> hostRaces, double otherHostCompat, double assimilationSeconds, double masterySeconds, double assimilationDayCap, double masteryDayCap, int coreCooldownDays,
                           int tint, List<Stat> passive, Phase acquire, Phase integrate) {

    public enum Op { ADD, MULT }

    /** One attribute bonus: the attribute id, how it applies, and the amount at full strength. */
    public record Stat(String attribute, Op op, double amount) {}

    /** One ability: {@code kind} picks the behaviour in Java, the numbers are the data. Slot 1 or 2 maps to the ability keys. */
    public record Ability(String id, String kind, int slot, double costPct, int cooldownTicks, double strain, double range, double radius, int durationTicks, double power, int maxTargets, double bossFactor) {}

    /** A phase: when it unlocks, what it drains and strains per second, the health it mends per second, its bonuses and abilities. */
    public record Phase(double unlockAssimilation, double unlockMastery, double drainPct, double strainPerSecond, double regen, List<Stat> stats, List<Ability> abilities) {}

    public Phase phase(int which) { return which == BeastWillRules.INTEGRATE ? integrate : which == BeastWillRules.ACQUIRE ? acquire : null; }

    public Ability ability(int phase, int slot) {
        Phase p = phase(phase);
        if (p != null) for (Ability a : p.abilities()) if (a.slot() == slot) return a;
        return null;
    }

    public boolean hostRace(String raceName) {
        if (raceName != null) for (String r : hostRaces) if (r.equalsIgnoreCase(raceName)) return true;
        return false;
    }

    /** Does a creature of this entity type id give the will (before the rank is looked at)? */
    public boolean fromEntity(String typeId) {
        if (typeId == null) return false;
        String t = typeId.toLowerCase(Locale.ROOT);
        if (sources.contains(t)) return true;
        if (t.startsWith("tensura:")) {
            String path = t.substring(8);
            for (String k : keywords) if (path.contains(k)) return true;
        }
        return false;
    }

    public String langKey(String part) { return "nusmp.beastwill." + id + "." + part; }

    /** The first problem with this row, or null when it is fine (used by the tests, the validator and the self-test). */
    public String problem() {
        if (id == null || !id.matches("[a-z][a-z0-9_]{2,31}")) return "bad id " + id;
        if (name == null || name.length() < 4 || name.length() > 48 || !name.equals(name.strip())) return id + ": bad name";
        if (beast == null || beast.isBlank() || beast.length() > 48) return id + ": bad beast";
        if (affinity == null || affinity.isBlank() || affinity.length() > 24) return id + ": bad affinity";
        if (minRank == null || !minRank.atLeast(BeastRank.C)) return id + ": a will needs a minimum beast rank";
        if (sources == null || keywords == null || (sources.isEmpty() && keywords.isEmpty())) return id + ": no creature gives it";
        for (String s : sources) if (!s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) return id + ": bad source " + s;
        for (String k : keywords) if (!k.matches("[a-z0-9_]{3,24}")) return id + ": bad keyword " + k;
        if (!(dropChance > 0) || dropChance > 0.05) return id + ": dropChance must be above 0 and at most 0.05";
        if (requireEp < 1000 || requireEp > 1e9) return id + ": requireEp out of bounds";
        if (hostRaces == null || hostRaces.isEmpty() || hostRaces.size() > 8) return id + ": hostRaces needs 1 to 8 names";
        if (otherHostCompat < 0.2 || otherHostCompat > 1.0) return id + ": otherHostCompat must be 0.2 to 1.0";
        if (assimilationSeconds < 600 || assimilationSeconds > 200_000) return id + ": assimilationSeconds out of bounds";
        if (masterySeconds < 300 || masterySeconds > 200_000) return id + ": masterySeconds out of bounds";
        if (assimilationDayCap < 0.02 || assimilationDayCap > 1.0 || masteryDayCap < 0.02 || masteryDayCap > 1.0) return id + ": a day cap must be 0.02 to 1.0";
        if (legendary && (assimilationDayCap > 0.25 || dropChance > 0.01)) return id + ": a legendary will needs a day cap of 0.25 or less and a drop chance of 0.01 or less";
        if (coreCooldownDays < 1 || coreCooldownDays > 60) return id + ": coreCooldownDays must be 1 to 60";
        if (tint < 0 || tint > 0xFFFFFF) return id + ": bad tint";
        if (passive == null || acquire == null || integrate == null) return id + ": passive, acquire and integrate are all needed";
        String p;
        if ((p = statsProblem(passive, "passive")) != null) return p;
        if ((p = phaseProblem(acquire, "acquire")) != null) return p;
        if ((p = phaseProblem(integrate, "integrate")) != null) return p;
        if (acquire.unlockAssimilation() > 0.5) return id + ": acquire must unlock at half the assimilation or less";
        if (integrate.unlockAssimilation() < 0.9 || integrate.unlockMastery() < 0.3) return id + ": integrate needs a (nearly) complete assimilation and real mastery";
        return null;
    }

    private String statsProblem(List<Stat> stats, String where) {
        for (Stat s : stats) {
            BeastWillRules.AttrCap cap = BeastWillRules.ATTR_CAPS.get(s.attribute());
            if (cap == null) return id + ": " + where + " touches an attribute that is not allowed: " + s.attribute();
            double max = s.op() == Op.ADD ? cap.maxAdd() : cap.maxMult();
            if (s.amount() <= 0 || s.amount() > max) return id + ": " + where + " " + s.attribute() + " " + s.op() + " must be above 0 and at most " + max;
        }
        return null;
    }

    private String phaseProblem(Phase ph, String where) {
        if (ph.unlockAssimilation() < 0 || ph.unlockAssimilation() > 1 || ph.unlockMastery() < 0 || ph.unlockMastery() > 1) return id + ": " + where + " unlock out of 0..1";
        if (ph.drainPct() < 0 || ph.drainPct() > BeastWillRules.MAX_DRAIN_PCT) return id + ": " + where + " drainPct out of bounds";
        if (ph.strainPerSecond() < 0 || ph.strainPerSecond() > BeastWillRules.MAX_STRAIN_PER_SECOND) return id + ": " + where + " strainPerSecond out of bounds";
        if (ph.regen() < 0 || ph.regen() > BeastWillRules.MAX_REGEN) return id + ": " + where + " regen out of bounds";
        String p = statsProblem(ph.stats(), where);
        if (p != null) return p;
        if (ph.abilities().isEmpty() || ph.abilities().size() > BeastWillRules.MAX_ABILITIES_PER_PHASE) return id + ": " + where + " needs 1 or 2 abilities";
        boolean[] slots = new boolean[3];
        for (Ability a : ph.abilities()) {
            String q = abilityProblem(a, where);
            if (q != null) return q;
            if (slots[a.slot()]) return id + ": " + where + " uses slot " + a.slot() + " twice";
            slots[a.slot()] = true;
        }
        return null;
    }

    private String abilityProblem(Ability a, String where) {
        String at = id + ": " + where + "/" + a.id();
        if (a.id() == null || !a.id().matches("[a-z][a-z0-9_]{2,31}")) return at + ": bad ability id";
        BeastWillRules.KindLimit k = BeastWillRules.KINDS.get(a.kind());
        if (k == null) return at + ": unknown kind " + a.kind();
        if (a.slot() < 1 || a.slot() > 2) return at + ": slot must be 1 or 2";
        if (a.costPct() < 0 || a.costPct() > BeastWillRules.MAX_COST_PCT) return at + ": costPct out of bounds";
        if (a.cooldownTicks() < k.minCooldown() || a.cooldownTicks() > BeastWillRules.MAX_COOLDOWN) return at + ": cooldownTicks must be " + k.minCooldown() + " to " + BeastWillRules.MAX_COOLDOWN;
        if (a.strain() < 0 || a.strain() > BeastWillRules.MAX_STRAIN) return at + ": strain out of bounds";
        if (a.range() < 0 || a.range() > BeastWillRules.MAX_RANGE) return at + ": range out of bounds";
        if (a.radius() < 0 || a.radius() > k.maxRadius()) return at + ": radius must be at most " + k.maxRadius();
        if (a.durationTicks() < 0 || a.durationTicks() > k.maxDuration()) return at + ": durationTicks must be at most " + k.maxDuration();
        if (a.power() < 0 || a.power() > BeastWillRules.MAX_POWER) return at + ": power out of bounds";
        if (a.maxTargets() < 1 || a.maxTargets() > k.maxTargets()) return at + ": maxTargets must be 1 to " + k.maxTargets();
        if (a.bossFactor() < 0 || a.bossFactor() > 1) return at + ": bossFactor must be 0..1";
        return null;
    }
}
