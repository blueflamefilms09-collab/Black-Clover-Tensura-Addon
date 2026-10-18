package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import java.util.Set;
import java.util.TreeSet;

/**
 * Why: "never stacks conflicting mechanics (conflict table)". Two mechanics that both flip gravity, both snuff the lights, both own the weather or both shield the monsters
 * would cancel each other out or make a room unreadable. Instead of a thousand-by-thousand table, every primitive declares the exclusive CHANNELS it occupies in the catalogue
 * ({@code ops.py}: gravity, light, water, weather, time, doors, shield, reflect, revive, burden, vision, floor ...), a mob effect may occupy one too (darkness and blindness are
 * "vision"), and two mechanics conflict exactly when they share a channel; two variants of the same base design never appear together either. The result is the conflict table,
 * derived from the data so a new mechanic needs no edit here. Pure.
 */
public final class Channels {
    private Channels() {}

    /** The exclusive channels a mechanic occupies. */
    public static Set<String> of(MechanicDef d, OpCatalog cat) {
        Set<String> out = new TreeSet<>();
        for (MechanicDef.Effect e : d.effects()) {
            OpCatalog.OpSpec s = cat.op(e.op());
            if (s != null) out.addAll(s.channels());
            if ("effect".equals(e.op()) || "buff_mobs".equals(e.op())) {
                String ch = cat.effectChannel(e.args().str("id", e.op().equals("effect") ? "minecraft:slowness" : "minecraft:strength"));
                if (ch != null) out.add(ch);
            }
        }
        out.retainAll(cat.exclusiveChannels());
        return out;
    }

    /** Do two mechanics conflict (share an exclusive channel, or are variants of one base)? A mechanic never conflicts with a different one that shares nothing. */
    public static boolean conflict(MechanicDef a, MechanicDef b, OpCatalog cat) {
        if (a.base().equals(b.base()) && a.category() == b.category()) return true;
        Set<String> x = of(a, cat);
        for (String c : of(b, cat)) if (x.contains(c)) return true;
        return false;
    }
}
