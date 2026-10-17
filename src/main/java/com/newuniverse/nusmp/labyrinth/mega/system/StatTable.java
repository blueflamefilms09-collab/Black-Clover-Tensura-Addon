package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: what a stat point DOES is a balance decision the owner will want to change (a point of strength is +0.2 attack damage, a point of intelligence +2 % of the
 * magicule pool ...), and which attribute carries it depends on which mods are installed (Tensura's magicule pool exists only with Tensura). So the effect of a
 * stat is a table row in {@code data/nusmp/mega/system/stats.json}: an attribute id, how the modifier combines, how much one point adds and a ceiling on the total.
 * This pure class is that table and the arithmetic over it ({@link Effect#amount}); the game-side {@link SystemStats} resolves each attribute id at runtime
 * and simply skips one that does not exist. A stat may have several effects (Sense lengthens the presence sense AND adds luck).
 */
public final class StatTable {
    public static final StatTable EMPTY = new StatTable(List.of());

    /** How a modifier combines with the attribute (the three vanilla operations, by their serialised names). */
    public enum Op {
        ADD_VALUE("add_value"), ADD_MULTIPLIED_BASE("add_multiplied_base"), ADD_MULTIPLIED_TOTAL("add_multiplied_total");

        private final String id;

        Op(String id) { this.id = id; }

        public String id() { return id; }

        public static Op fromId(String s) {
            if (s == null) return null;
            String t = s.trim().toLowerCase(Locale.ROOT);
            for (Op o : values()) if (o.id.equals(t) || o.name().toLowerCase(Locale.ROOT).equals(t)) return o;
            return null;
        }
    }

    /** One attribute a stat feeds: {@code perPoint} added for each point, the total never above {@code cap} in magnitude (0 = no ceiling). */
    public record Effect(String attribute, Op op, double perPoint, double cap) {
        /** The modifier amount for this many points: {@code perPoint * points}, cut to the ceiling (the sign is kept). */
        public double amount(int points) {
            double a = perPoint * Math.max(0, points);
            if (cap > 0) a = Math.copySign(Math.min(Math.abs(a), cap), a);
            return a;
        }
    }

    /** One stat's row: its display name and its effects. */
    public record Def(Stat stat, String name, List<Effect> effects) {
        public Def {
            effects = List.copyOf(effects);
        }
    }

    private final Map<Stat, Def> byStat = new EnumMap<>(Stat.class);

    public StatTable(List<Def> defs) {
        for (Def d : defs) byStat.put(d.stat(), d);                 // a later row of the same stat replaces the earlier (a datapack override)
    }

    public Def def(Stat s) { return byStat.get(s); }

    public int size() { return byStat.size(); }

    public boolean isEmpty() { return byStat.isEmpty(); }

    public List<Def> all() { return new ArrayList<>(byStat.values()); }
}
