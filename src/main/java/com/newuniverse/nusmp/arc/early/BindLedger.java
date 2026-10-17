package com.newuniverse.nusmp.arc.early;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Revchi Salik's whole character is the Magic Binding, chains that hold a foe and seal their magic. A bind that cannot be broken is not a mechanic
 * a party can learn from, and one that always runs its full length makes a caster's fight a wait. This pure class keeps who is bound, until when, and how
 * many links of chain are still on them. A blow the bound foe lands on Revchi tears links off ({@link #struggle}: so a fighter in melee frees themselves
 * in a few blows, a caster whose grimoire is sealed needs help or a weapon), and anti-magic cuts the whole chain at once ({@link #free}, the way Asta's
 * sword did in the anime). A second chain never extends a bind that is still on (otherwise two binds in a row would be a chain-lock). The game glue
 * ({@code RevchiSalikBoss}) turns "bound" into the seal flag and the slow, and "freed" into their removal.
 */
public final class BindLedger {
    /** What a blow of the bound foe did to the chain. */
    public enum Struggle { NONE, LINK, FREE }

    private static final class Bind {
        long until;
        int links;
        double progress;
    }

    private final int links;
    private final double damagePerLink;
    private final Map<UUID, Bind> binds = new HashMap<>();

    /** A ledger whose binds have {@code links} links, each torn off by {@code damagePerLink} points of damage dealt to the binder. */
    public BindLedger(int links, double damagePerLink) {
        this.links = Math.max(1, links);
        this.damagePerLink = Math.max(1, damagePerLink);
    }

    /** The damage that tears one link off a boss of this maximum health: {@code share} of it, at least {@code floor}. */
    public static double linkDamage(double maxHealth, double share, double floor) { return Math.max(floor, maxHealth * share); }

    /** Binds {@code id} until {@code now + ticks}. False (and nothing changes) when they are already bound or the time is no time. */
    public boolean bind(UUID id, long now, int ticks) {
        if (id == null || ticks <= 0 || bound(id, now)) return false;
        Bind b = new Bind();
        b.until = now + ticks;
        b.links = links;
        binds.put(id, b);
        return true;
    }

    /** Is this foe still in chains at {@code now}? An expired bind is forgotten. */
    public boolean bound(UUID id, long now) {
        Bind b = binds.get(id);
        if (b == null) return false;
        if (now >= b.until) {
            binds.remove(id);
            return false;
        }
        return true;
    }

    /** The tick their chain ends at (0 when they are not bound). */
    public long until(UUID id) {
        Bind b = binds.get(id);
        return b == null ? 0 : b.until;
    }

    /** Links still on them (0 when not bound). */
    public int linksLeft(UUID id) {
        Bind b = binds.get(id);
        return b == null ? 0 : b.links;
    }

    /** A blow of {@code damage} the bound foe dealt to the binder: tears links; the chain falls away with the last one. Blows of a foe who is not bound do nothing. */
    public Struggle struggle(UUID id, long now, double damage) {
        if (damage <= 0 || !bound(id, now)) return Struggle.NONE;
        Bind b = binds.get(id);
        b.progress += damage;
        boolean cut = false;
        while (b.progress >= damagePerLink && b.links > 0) {
            b.progress -= damagePerLink;
            b.links--;
            cut = true;
        }
        if (b.links <= 0) {
            binds.remove(id);
            return Struggle.FREE;
        }
        return cut ? Struggle.LINK : Struggle.NONE;
    }

    /** Cuts the whole chain at once (anti-magic). True if they were bound. */
    public boolean free(UUID id) { return binds.remove(id) != null; }

    /** Everyone with a bind on record (some may have run out; ask {@link #bound}). */
    public List<UUID> ids() { return new ArrayList<>(binds.keySet()); }

    public int size() { return binds.size(); }

    public void clear() { binds.clear(); }
}
