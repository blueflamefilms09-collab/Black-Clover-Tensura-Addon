package com.newuniverse.nusmp.shishou;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: there is exactly ONE Shishou per world: one flying entity, or one dormant entity, or one item, or one thrown minion, never two and never none-by-accident. This is the pure state machine of that
 * rule, so the cases a mistake would break are unit tests: what can become what, who may claim a dormant sword, which copy of the item is the real one, and how a lost sword is brought back.
 *
 * <p>Every incarnation carries a {@code token}: a number that changes at every transition. The item stack carries the token it was made with; an entity carries its own uuid. A stack or entity whose token or
 * uuid is not the current one is a GHOST (a copy, a leftover in an unloaded chunk, an old stack in a chest) and the game removes it when it next meets it. Two stacks with the current token ticking in
 * the same tick are a DUPLICATE (a creative copy): the second one is removed. Nothing here can destroy the sword for good: {@link #recoverToDormant} and {@link #recoverToHeld} always bring it back.</p>
 *
 * <p>Server thread only. The saved form is a flat string map ({@link #toMap}); the wrapper {@link ShishouData} writes it to NBT.</p>
 */
public final class ShishouCap {
    public enum Form { NONE, WILD, DORMANT, HELD, MINION }

    public enum Claim { OK, NOT_DORMANT, RESERVED }

    public enum Beat { OK, GHOST, DUPLICATE }

    private Form form = Form.NONE;
    private UUID entity, owner;
    private long token, counter, reservedUntil;
    private String dim = "";
    private int x, y, z;
    private String homeDim = "";
    private int hx, hy, hz;
    private boolean hasHome;

    private long beatTick = Long.MIN_VALUE;
    private int beatIdentity;

    // ---------------------------------------------------------------- reading
    public Form form() { return form; }
    public UUID entity() { return entity; }
    public UUID owner() { return owner; }
    public long token() { return token; }
    public long reservedUntil() { return reservedUntil; }
    public boolean exists() { return form != Form.NONE; }
    public String dim() { return dim; }
    public int[] pos() { return new int[]{x, y, z}; }
    public boolean hasHome() { return hasHome; }
    public String homeDim() { return homeDim; }
    public int[] home() { return new int[]{hx, hy, hz}; }

    public boolean isOwner(UUID who) { return who != null && who.equals(owner); }

    /** Is the dormant sword kept for its old wielder at {@code now}, so that {@code who} may not claim it? */
    public boolean reservedAgainst(UUID who, long now) { return owner != null && !owner.equals(who) && now < reservedUntil; }

    private long next() { return ++counter; }

    private void where(String d, int px, int py, int pz) {
        dim = d == null ? "" : d;
        x = px;
        y = py;
        z = pz;
    }

    // ---------------------------------------------------------------- the transitions
    /** A flying sword enters the world. Only when there is no Shishou at all. */
    public boolean spawnWild(UUID entityId, String d, int px, int py, int pz) {
        if (form != Form.NONE || entityId == null) return false;
        form = Form.WILD;
        entity = entityId;
        owner = null;
        token = next();
        reservedUntil = 0L;
        where(d, px, py, pz);
        return true;
    }

    /** The wild sword settles into the ground. */
    public boolean dormant(UUID entityId, String d, int px, int py, int pz) {
        if (form != Form.WILD || entityId == null || !entityId.equals(entity)) return false;
        form = Form.DORMANT;
        where(d, px, py, pz);
        return true;
    }

    /** The ground under the dormant sword is gone: it flies again and will settle somewhere else. */
    public boolean wake(UUID entityId) {
        if (form != Form.DORMANT || entityId == null || !entityId.equals(entity)) return false;
        form = Form.WILD;
        return true;
    }

    /** A player right-clicks the dormant sword. */
    public Claim claim(UUID player, long now) {
        if (form != Form.DORMANT || player == null) return Claim.NOT_DORMANT;
        if (reservedAgainst(player, now)) return Claim.RESERVED;
        form = Form.HELD;
        owner = player;
        entity = null;
        reservedUntil = 0L;
        token = next();
        return Claim.OK;
    }

    /** A stack without a token reached a player (the creative tab, a command): it becomes the sword only when there is none. */
    public boolean adopt(UUID player) {
        if (form != Form.NONE || player == null) return false;
        form = Form.HELD;
        owner = player;
        entity = null;
        token = next();
        reservedUntil = 0L;
        return true;
    }

    /** The wielder throws the sword: the item stops being valid, a minion entity is the sword. */
    public boolean throwOut(long stackToken, UUID entityId, String d, int px, int py, int pz) {
        if (form != Form.HELD || stackToken != token || entityId == null) return false;
        form = Form.MINION;
        entity = entityId;
        token = next();
        where(d, px, py, pz);
        return true;
    }

    /** The wielder recalls it: the minion stops being valid, a new stack is the sword. Returns the token of that stack, or 0. */
    public long recall() {
        if (form != Form.MINION) return 0L;
        form = Form.HELD;
        entity = null;
        token = next();
        return token;
    }

    /**
     * The sword was lost (the void, lava, a despawn, a thief) or an operator asked: it becomes a dormant entity {@code entityId} at the given place, kept for its wielder for {@code reserveTicks}. Works from
     * every form, including NONE (a repair), and makes every old stack and entity a ghost.
     */
    public void recoverToDormant(UUID entityId, String d, int px, int py, int pz, long now, long reserveTicks) {
        form = Form.DORMANT;
        entity = entityId;
        token = next();
        reservedUntil = owner == null ? 0L : now + Math.max(0L, reserveTicks);
        where(d, px, py, pz);
    }

    /** The sword goes straight back to its wielder (online): a new stack is the sword. Returns its token, or 0 when the sword has no wielder. */
    public long recoverToHeld() {
        if (owner == null) return 0L;
        form = Form.HELD;
        entity = null;
        reservedUntil = 0L;
        token = next();
        return token;
    }

    /** An operator's reset: nothing exists; every stack and entity is a ghost; the wielder is forgotten. */
    public void reset() {
        form = Form.NONE;
        entity = null;
        owner = null;
        reservedUntil = 0L;
        token = next();
    }

    /** Where the wielder's home is (their respawn point, or where they last held the sword), kept for a recovery while they are offline. */
    public void setHome(String d, int px, int py, int pz) {
        homeDim = d == null ? "" : d;
        hx = px;
        hy = py;
        hz = pz;
        hasHome = true;
    }

    public void moved(String d, int px, int py, int pz) { if (form == Form.WILD || form == Form.MINION) where(d, px, py, pz); }

    // ---------------------------------------------------------------- validity
    /** Is this stack the one valid item? */
    public boolean validStack(long stackToken) { return form == Form.HELD && stackToken != 0L && stackToken == token; }

    /** Is this entity the one valid body (wild, dormant or minion)? */
    public boolean validEntity(UUID id) { return id != null && id.equals(entity) && (form == Form.WILD || form == Form.DORMANT || form == Form.MINION); }

    /**
     * A stack with {@code stackToken}, identified by {@code identity} in this very object graph, ticked at {@code now}. A stack of another token is a ghost; a second stack of the current token ticking in the
     * same tick as another one is a duplicate.
     */
    public Beat beat(long stackToken, int identity, long now) {
        if (!validStack(stackToken)) return Beat.GHOST;
        if (beatTick == now && beatIdentity != identity) return Beat.DUPLICATE;
        beatTick = now;
        beatIdentity = identity;
        return Beat.OK;
    }

    // ---------------------------------------------------------------- saving
    public Map<String, String> toMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("form", form.name());
        if (entity != null) m.put("entity", entity.toString());
        if (owner != null) m.put("owner", owner.toString());
        m.put("token", Long.toString(token));
        m.put("counter", Long.toString(counter));
        m.put("reserved", Long.toString(reservedUntil));
        m.put("dim", dim);
        m.put("pos", x + "," + y + "," + z);
        if (hasHome) {
            m.put("homeDim", homeDim);
            m.put("home", hx + "," + hy + "," + hz);
        }
        return m;
    }

    private static UUID uuid(String s) {
        try {
            return s == null ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static long num(String s, long d) {
        try {
            return s == null ? d : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return d;
        }
    }

    private static int[] xyz(String s) {
        int[] r = new int[3];
        if (s == null) return r;
        String[] p = s.split(",");
        for (int i = 0; i < 3 && i < p.length; i++) r[i] = (int) num(p[i].trim(), 0L);
        return r;
    }

    public static ShishouCap fromMap(Map<String, String> m) {
        ShishouCap c = new ShishouCap();
        if (m == null) return c;
        try {
            c.form = Form.valueOf(m.getOrDefault("form", "NONE"));
        } catch (IllegalArgumentException e) {
            c.form = Form.NONE;
        }
        c.entity = uuid(m.get("entity"));
        c.owner = uuid(m.get("owner"));
        c.token = num(m.get("token"), 0L);
        c.counter = Math.max(num(m.get("counter"), 0L), c.token);
        c.reservedUntil = num(m.get("reserved"), 0L);
        c.dim = m.getOrDefault("dim", "");
        int[] p = xyz(m.get("pos"));
        c.x = p[0];
        c.y = p[1];
        c.z = p[2];
        if (m.containsKey("home")) {
            c.hasHome = true;
            c.homeDim = m.getOrDefault("homeDim", "");
            int[] h = xyz(m.get("home"));
            c.hx = h[0];
            c.hy = h[1];
            c.hz = h[2];
        }
        return c;
    }

    /** A body form with no entity id cannot be matched to any entity: the service repairs it by recovering the sword to its home (never by forgetting it, which could make a second one). */
    public boolean needsRepair() { return (form == Form.WILD || form == Form.DORMANT || form == Form.MINION) && entity == null; }
}
