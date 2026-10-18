package com.newuniverse.nusmp.skilltree.ft.kit;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the owner's balance law for Fairy Tail techniques (design.md section 6) is only worth anything if it is checked against the numbers that really ship, and those
 * numbers live in JSON so that a datapack can retune them. So the law is written once, as pure functions over a node's {@code values} tables (no Minecraft type), and is used
 * twice: a unit test runs it on the shipped route files at build time, and the in-game self-test runs the very same code on the routes the server actually loaded (a datapack that
 * pushes a number over the line is reported, not silently obeyed). The rules are tied to the NAMES of the tables, which is the contract between a route file and its
 * abilities: a key ending in {@code damage} is raw damage per hit (4 to 14 for an ordinary technique, at most 30 for a Secret Art, and a Secret Art that hits several times must
 * not exceed 30 over the whole sequence), {@code cooldown} is in ticks (at least 3 s, a Secret Art at least 45 s), {@code cost} is a share of the maximum magicules (1 to 4 percent, a
 * Secret Art at most 8), {@code targets} is at most 12, {@code heal} is the share of maximum health one cast may return (at most 25 percent), {@code life} is how long a construct
 * stands (at most 10 s), and any hit above 8 raw must have a {@code windup} of at least 8 ticks (the tell). A construct or a buff cannot outlast its own cooldown, so it can never be kept up
 * for ever, and a turret ({@code shots} every {@code interval} ticks) must stand long enough to fire all its shots. A route is also checked for the shape the owner asked for: 7 to 10 nodes, one root, a Secret Art that needs two branches, prices and
 * levels inside the allowed ranges. Why by price and not by a flag: the owner's rule already says an ordinary node costs 1 to 3 SP a level and a Secret Art 4 to 8, so the price IS the
 * classification and no field has to be added to the shared node record.
 */
public final class FtBudget {
    private FtBudget() {}

    /** What a node is, decided by what it grants and what it costs. */
    public enum Kind { ORDINARY, SECRET_ART, PASSIVE }

    public static final double DAMAGE_MIN = 4, DAMAGE_MAX = 14, SECRET_DAMAGE_MAX = 30, TELL_ABOVE = 8;
    public static final int TELL_MIN_TICKS = 8, COOLDOWN_MIN = 60, SECRET_COOLDOWN_MIN = 900, TARGETS_MAX = 12, LIFE_MAX = 200, SPAWNS_MAX = 4;
    public static final double COST_MIN = 0.01, COST_MAX = 0.04, SECRET_COST_MAX = 0.08, HEAL_MAX = 0.25;
    public static final int MIN_NODES = 7, MAX_NODES = 10, MAX_LEVEL = 5, ORDINARY_PRICE_MAX = 3, SECRET_PRICE_MIN = 4, SECRET_PRICE_MAX = 8, GRID_X = 12, GRID_Y = 9;

    /** A node that grants no skill is a passive; one whose first level costs 4 SP or more is a Secret Art; the rest are ordinary techniques. */
    public static Kind kindOf(SkillNode n) {
        if (!n.grantsSkill()) return Kind.PASSIVE;
        return n.costOf(1) >= SECRET_PRICE_MIN ? Kind.SECRET_ART : Kind.ORDINARY;
    }

    private static List<Double> table(Map<String, List<Double>> values, String key) { return values.get(key); }

    /** The problems of one node's tables; empty when the numbers obey the law. {@code id} only labels the messages. */
    public static List<String> check(String id, Kind kind, int maxLevel, Map<String, List<Double>> values) {
        List<String> bad = new ArrayList<>();
        for (Map.Entry<String, List<Double>> e : values.entrySet()) {
            String key = e.getKey();
            List<Double> t = e.getValue();
            if (t.isEmpty()) { bad.add(id + "." + key + " is an empty table"); continue; }
            if (t.size() != maxLevel) bad.add(id + "." + key + " has " + t.size() + " entries for " + maxLevel + " levels");
            for (double v : t) {
                if (!Double.isFinite(v) || v < 0) { bad.add(id + "." + key + " holds " + v); break; }
                if (key.endsWith("damage") && kind != Kind.PASSIVE) {
                    double max = kind == Kind.SECRET_ART ? SECRET_DAMAGE_MAX : DAMAGE_MAX;
                    if (v > max) bad.add(id + "." + key + " " + v + " is above " + max);
                    else if (v != 0 && kind == Kind.ORDINARY && v < DAMAGE_MIN) bad.add(id + "." + key + " " + v + " is below " + DAMAGE_MIN);
                } else if (key.equals("targets") && (v < 1 || v > TARGETS_MAX)) bad.add(id + ".targets " + v + " is outside 1.." + TARGETS_MAX);
                else if (key.equals("heal") && v > HEAL_MAX) bad.add(id + ".heal " + v + " is above " + HEAL_MAX);
                else if (key.equals("life") && v > LIFE_MAX) bad.add(id + ".life " + v + " ticks is above " + LIFE_MAX);
            }
        }
        if (kind == Kind.PASSIVE) return bad;

        List<Double> cooldown = table(values, "cooldown"), cost = table(values, "cost");
        if (cooldown == null) bad.add(id + " has no cooldown table");
        else for (double v : cooldown) {
            double min = kind == Kind.SECRET_ART ? SECRET_COOLDOWN_MIN : COOLDOWN_MIN;
            if (v < min) { bad.add(id + ".cooldown " + v + " ticks is below " + (int) min); break; }
        }
        if (cost == null) bad.add(id + " has no cost table");
        else for (double v : cost) {
            double max = kind == Kind.SECRET_ART ? SECRET_COST_MAX : COST_MAX;
            if (v < COST_MIN || v > max) { bad.add(id + ".cost " + v + " is outside " + COST_MIN + ".." + max); break; }
        }

        // a hit above 8 raw is announced: the data must carry a windup of at least 8 ticks
        boolean heavy = false;
        for (Map.Entry<String, List<Double>> e : values.entrySet()) {
            if (!e.getKey().endsWith("damage")) continue;
            for (double v : e.getValue()) if (v > TELL_ABOVE) heavy = true;
        }
        List<Double> windup = table(values, "windup");
        if (heavy && windup == null) bad.add(id + " hits for more than " + (int) TELL_ABOVE + " raw but has no windup (the tell)");
        if (windup != null) for (double v : windup) if (v < TELL_MIN_TICKS) { bad.add(id + ".windup " + v + " ticks is shorter than a tell (" + TELL_MIN_TICKS + ")"); break; }

        // a Secret Art that strikes in pulses plus a burst is held to the cap over the whole sequence, per target
        List<Double> pulses = table(values, "pulses"), pulseDamage = table(values, "pulse_damage"), burst = table(values, "burst_damage");
        if (kind == Kind.SECRET_ART && pulses != null && pulseDamage != null && burst != null) {
            int n = Math.min(pulses.size(), Math.min(pulseDamage.size(), burst.size()));
            for (int i = 0; i < n; i++) {
                double sum = pulses.get(i) * pulseDamage.get(i) + burst.get(i);
                if (sum > SECRET_DAMAGE_MAX) { bad.add(id + " level " + (i + 1) + " deals " + sum + " raw to one target over its sequence (cap " + (int) SECRET_DAMAGE_MAX + ")"); break; }
            }
        }

        // a construct or a buff cannot outlast its own cooldown, so it can never be kept up for ever
        keepUp(bad, id, cooldown, table(values, "life"), 1.0, "life");
        keepUp(bad, id, cooldown, table(values, "seconds"), 20.0, "seconds");

        // a turret that fires `shots` times, `interval` ticks apart, must stand long enough to fire them all
        List<Double> shots = table(values, "shots"), interval = table(values, "interval"), life = table(values, "life");
        if (shots != null && interval != null && life != null) {
            for (int i = 0; i < Math.min(shots.size(), Math.min(interval.size(), life.size())); i++) {
                if (shots.get(i) * interval.get(i) > life.get(i)) { bad.add(id + " level " + (i + 1) + ": " + shots.get(i) + " shots " + interval.get(i) + " ticks apart do not fit in a life of " + life.get(i)); break; }
            }
        }
        return bad;
    }

    private static void keepUp(List<String> bad, String id, List<Double> cooldown, List<Double> span, double ticksPerUnit, String name) {
        if (cooldown == null || span == null) return;
        for (int i = 0; i < Math.min(cooldown.size(), span.size()); i++) {
            if (span.get(i) * ticksPerUnit > cooldown.get(i)) { bad.add(id + " level " + (i + 1) + ": " + name + " outlasts the cooldown"); return; }
        }
    }

    /** The problems of a whole route: the shape the owner asked for, the price and level ranges, the canvas, and every node's numbers. Empty when all is well. */
    public static List<String> problems(SkillRoute route) {
        List<String> bad = new ArrayList<>();
        List<SkillNode> nodes = route.nodes();
        String rid = route.id();
        if (nodes.size() < MIN_NODES || nodes.size() > MAX_NODES) bad.add(rid + " has " + nodes.size() + " nodes, not " + MIN_NODES + " to " + MAX_NODES);
        if (route.roots().size() != 1) bad.add(rid + " has " + route.roots().size() + " root nodes, not 1");
        Set<String> seen = new HashSet<>();
        Map<String, Integer> cells = new HashMap<>();
        int secret = 0;
        for (SkillNode n : nodes) {
            String id = n.id();
            if (!seen.add(id)) bad.add(rid + ": node id " + id + " is used twice");
            for (SkillNode.Parent p : n.parents()) {
                SkillNode parent = route.node(p.node());
                if (parent == null) bad.add(id + " names the unknown parent " + p.node());
                else if (!seen.contains(p.node())) bad.add(id + " comes before its parent " + p.node() + " in the file");
                else if (p.level() < 1 || p.level() > parent.maxLevel()) bad.add(id + " asks for level " + p.level() + " of " + p.node() + ", which goes to " + parent.maxLevel());
            }
            if (n.maxLevel() < 1 || n.maxLevel() > MAX_LEVEL) bad.add(id + " has max level " + n.maxLevel() + ", not 1.." + MAX_LEVEL);
            if (n.x() < 0 || n.x() > GRID_X || n.y() < 0 || n.y() > GRID_Y) bad.add(id + " sits at " + n.x() + "," + n.y() + ", off the canvas");
            Integer other = cells.put(n.x() + "," + n.y(), 1);
            if (other != null) bad.add(id + " shares the cell " + n.x() + "," + n.y() + " with another node");
            Kind kind = kindOf(n);
            for (int l = 1; l <= n.maxLevel(); l++) {
                int price = n.costOf(l);
                if (kind == Kind.SECRET_ART ? price < SECRET_PRICE_MIN || price > SECRET_PRICE_MAX : price < 1 || price > ORDINARY_PRICE_MAX) {
                    bad.add(id + " level " + l + " costs " + price + " SP, outside the " + (kind == Kind.SECRET_ART ? SECRET_PRICE_MIN + ".." + SECRET_PRICE_MAX : "1.." + ORDINARY_PRICE_MAX) + " range");
                    break;
                }
            }
            if (kind == Kind.SECRET_ART) {
                secret++;
                if (n.parents().size() < 2) bad.add(id + " is a Secret Art with " + n.parents().size() + " parent(s); it must need two branches");
            }
            if (kind == Kind.PASSIVE && n.passives().isEmpty()) bad.add(id + " grants no skill and no passive");
            if (n.grantsSkill() && !n.skill().startsWith("nusmp:ft_")) bad.add(id + ": the skill " + n.skill() + " does not start with nusmp:ft_");
            bad.addAll(check(id, kind, n.maxLevel(), n.values()));
        }
        if (secret != 1) bad.add(rid + " has " + secret + " Secret Arts, not 1");
        return bad;
    }
}
