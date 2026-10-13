package com.newuniverse.nusmp.dungeon.nexus.vault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** What a vault is made of: the ordered ops that build it and the named spots the glue fills with live things (a pedestal, a guardian, plates). Pure. */
public final class VaultLayout {
    /** What a spot is for. */
    public enum Role { PLATE, PEDESTAL, BARS, BLOOM, GUARDIAN, GATE, STAND, DOOR }

    /** A named position in the world; {@code index} orders spots of one role (the nine plates are 0 to 8). */
    public record Spot(Role role, int index, int x, int y, int z) {}

    public final VaultSite site;
    public final List<VaultOp> ops;
    private final Map<Role, List<Spot>> spots;

    VaultLayout(VaultSite site, List<VaultOp> ops, Map<Role, List<Spot>> spots) {
        this.site = site;
        this.ops = Collections.unmodifiableList(new ArrayList<>(ops));
        Map<Role, List<Spot>> copy = new EnumMap<>(Role.class);
        for (Map.Entry<Role, List<Spot>> e : spots.entrySet()) copy.put(e.getKey(), List.copyOf(e.getValue()));
        this.spots = copy;
    }

    /** The spots of a role in index order (empty if the layout has none). */
    public List<Spot> spots(Role role) { return spots.getOrDefault(role, List.of()); }

    /** The first spot of a role, or null. */
    public Spot first(Role role) {
        List<Spot> l = spots(role);
        return l.isEmpty() ? null : l.get(0);
    }

    /** Total cells all ops cover (a measure of the work). */
    public long cells() {
        long n = 0;
        for (VaultOp o : ops) n += o.volume();
        return n;
    }
}
