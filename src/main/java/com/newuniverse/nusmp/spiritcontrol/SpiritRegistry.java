package com.newuniverse.nusmp.spiritcontrol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the one small registry every spirit package registers with (an append-only line in its Boot), so the grimoire pages, the command and the combat mode all read the same list. Pure and generic over
 * the handle type so it is tested without Minecraft. Registration keeps the order, rejects an empty or repeated id and a spirit with too many abilities; a repeat of the very same object is ignored (a Boot
 * that runs twice).
 */
public final class SpiritRegistry<H extends SpiritInfo> {
    private final Map<String, H> byId = new LinkedHashMap<>();

    public synchronized boolean register(H h) {
        if (h == null || h.id() == null || h.id().isBlank()) throw new IllegalArgumentException("spirit id");
        String id = h.id().toLowerCase(Locale.ROOT);
        H old = byId.get(id);
        if (old == h) return false;
        if (old != null) throw new IllegalStateException("spirit " + id + " is registered twice");
        if (h.abilities().size() > PageGate.MAX_ABILITIES) throw new IllegalArgumentException("spirit " + id + " has more than " + PageGate.MAX_ABILITIES + " abilities");
        byId.put(id, h);
        return true;
    }

    public synchronized H get(String id) { return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT)); }

    /** All spirits in registration order. */
    public synchronized List<H> all() { return Collections.unmodifiableList(new ArrayList<>(byId.values())); }

    /** The spirits whose pages the book of this magic carries, in registration order. */
    public synchronized List<H> forMagic(String magic) {
        List<H> out = new ArrayList<>();
        if (magic == null) return out;
        for (H h : byId.values()) if (h.magics().contains(magic)) out.add(h);
        return out;
    }

    public synchronized int size() { return byId.size(); }
}
