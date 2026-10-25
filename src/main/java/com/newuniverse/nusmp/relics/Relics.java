package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.item.CuriosLink;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The relics' registry and the question every wave asks: what does this entity carry, and what works? The defs are bound once by
 * {@link RelicCatalog#init}. Looking at a wearer is cached for {@link #TTL} ticks per entity and per side (client and server are separate),
 * because reading the Curios inventory goes through reflection.
 */
public final class Relics {
    private Relics() {}

    /** How many ticks a look at a wearer stays good (by default; 1.99.0: the live value is {@link #ttl()}, set by {@code relicScanInterval} in nusmp-performance-common.toml). */
    public static final int TTL = 10;

    /** How many ticks a look at a wearer stays good right now: the config's {@code relicScanInterval} (10 by default, as {@link #TTL}). */
    public static int ttl() { return com.newuniverse.nusmp.perf.PerfSettings.relicScanInterval(); }

    private static volatile List<RelicDef> all = List.of();
    private static volatile Map<String, RelicDef> byId = Map.of();
    private static volatile Map<Item, RelicDef> adopted;

    /** Called by the catalog once, with the validated defs in registration order. */
    static void bind(List<RelicDef> defs) {
        Map<String, RelicDef> m = new LinkedHashMap<>();
        for (RelicDef d : defs) m.put(d.id(), d);
        byId = Collections.unmodifiableMap(m);
        all = List.copyOf(defs);
        adopted = null;
    }

    public static List<RelicDef> all() { return all; }

    /** The def with this registry path, or null. */
    public static RelicDef byId(String id) { return id == null ? null : byId.get(id); }

    /** The def of this stack's item (a registered relic, or an item a def adopted), if it is one. */
    public static Optional<RelicDef> of(ItemStack s) { return Optional.ofNullable(defOf(s)); }

    /** The same without the Optional. */
    public static RelicDef defOf(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        Item it = s.getItem();
        if (it instanceof RelicBearer b) return b.relic();
        Map<Item, RelicDef> m = adoptedMap();
        return m == null ? null : m.get(it);
    }

    /** The item -> def map of the adopted items, built on first use (their suppliers need the registries to be bound). */
    private static Map<Item, RelicDef> adoptedMap() {
        Map<Item, RelicDef> m = adopted;
        if (m != null) return m;
        Map<Item, RelicDef> built = new IdentityHashMap<>();
        for (RelicDef d : all) {
            if (!d.isAdopted()) continue;
            try {
                built.put(d.adopted().get(), d);
            } catch (RuntimeException notBoundYet) {
                return null;
            }
        }
        adopted = built;
        return built;
    }

    /** The registry id of an adopted or registered relic's item (for messages). */
    public static String itemId(RelicDef d) {
        Map<Item, RelicDef> m = adoptedMap();
        if (m != null) for (Map.Entry<Item, RelicDef> e : m.entrySet()) if (e.getValue() == d) return BuiltInRegistries.ITEM.getKey(e.getKey()).toString();
        return "nusmp:" + d.id();
    }

    // ---------------------------------------------------------------- what an entity carries
    /** A relic that is equipped, with its def. */
    public record Worn(ItemStack stack, RelicDef def) {}

    /** Where a worn relic was found: a hand, the armour slots, a Curios slot, the pack of a named mob, or only in the list the server sent ({@link WornSync}; client only). */
    public enum Where { HAND, ARMOUR, CURIOS, PACK, SYNCED }

    /** A worn relic with the place it was found in (for the worn layers and {@code /multiverse relic debug}); {@link #worn()} is the plain pair. */
    public record Located(ItemStack stack, RelicDef def, Where where) {
        public Worn worn() { return new Worn(stack, def); }
    }

    /** One resistance a worn relic grants. */
    public record Resist(String id, double fraction) {}

    /** What works on an entity right now (equipped, and attuned where needed, at most three attuned): its relics, flags, resistances, EP lift. */
    public record Loadout(List<Worn> active, Set<String> flags, List<Resist> resists, double epBonus) {
        public static final Loadout EMPTY = new Loadout(List.of(), Set.of(), List.of(), 0.0);
    }

    private record Cached(long tick, List<Worn> worn, List<Located> located, Loadout loadout) {}

    private static final Map<UUID, Cached> SERVER = new ConcurrentHashMap<>(), CLIENT = new ConcurrentHashMap<>();
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section SCAN_SERVER = com.newuniverse.nusmp.perf.PerfProbe.section("s.relic.scan"), SCAN_CLIENT = com.newuniverse.nusmp.perf.PerfProbe.section("c.relic.scan");

    private static Map<UUID, Cached> cacheFor(LivingEntity e) { return e.level().isClientSide() ? CLIENT : SERVER; }

    /** Every relic the entity has equipped (a hand for hand relics, the head/chest/feet slots, a Curios slot), working or not. Never null. */
    public static List<Worn> wornList(LivingEntity e) { return cached(e, false).worn(); }

    /** Every equipped relic with where it was found (the same cached look as {@link #wornList}). Never null. */
    public static List<Located> locatedList(LivingEntity e) { return cached(e, false).located(); }

    /**
     * Only what is carried OUTSIDE the vanilla equipment: the relics in the Curios slots and in a named mob's pack, read from this side's own data (the server's truth; on a client the pack
     * is always empty because persistent data never leaves the server). What {@link WornSync} sends. Not cached.
     */
    public static List<Located> scanExtra(LivingEntity e) {
        List<Located> out = new ArrayList<>(4);
        for (ItemStack s : CuriosLink.equipped(e)) {
            RelicDef d = defOf(s);
            if (d != null && (d.slot().isCurio() || isHeadOrFeet(d))) out.add(new Located(s, d, Where.CURIOS));      // 3.10.0: a head or feet relic in its relic_head / relic_feet Curios slot
        }
        for (ItemStack s : MobRelics.pack(e)) {
            RelicDef d = defOf(s);
            if (d != null) out.add(new Located(s, d, Where.PACK));
        }
        return out;
    }

    /** 3.10.0: head and feet relics also fit the Curios slots relic_head / relic_feet (the armour slots stay as they were). */
    static boolean isHeadOrFeet(RelicDef d) { return d.slot() == RelicSlot.HEAD || d.slot() == RelicSlot.FEET; }

    /** relics_wear: the one client-side entity (the local player) whose look is kept for only {@link WornSyncRules#FAST_TTL} ticks right now, or null. Set by the client while a screen is open. */
    private static volatile UUID fastLook;

    /** Client only: the local player's look is refreshed every {@link WornSyncRules#FAST_TTL} ticks (the inventory preview and the Curios screen show a relic put on at once); null switches it off. */
    public static void setFastLook(UUID id) { fastLook = id; }

    /** How many ticks the look at this entity stays good: the fast time for the local player in fast mode (client side only), else {@link #ttl()}. */
    private static int ttlFor(LivingEntity e, int normal) {
        UUID f = fastLook;
        return f != null && e.level().isClientSide() && f.equals(e.getUUID()) ? WornSyncRules.lookTtl(true, normal) : normal;
    }

    /** Forget the kept look at a client-side entity (the server's list for it just changed). */
    public static void invalidateClient(UUID id) {
        if (id != null) CLIENT.remove(id);
    }

    /** What works on the entity (cached for {@link #TTL} ticks). Never null. 1.45.0: nothing works while the entity is relic-nullified ({@link RelicNullification}). */
    public static Loadout loadout(LivingEntity e) { return RelicNullification.isNullified(e) ? Loadout.EMPTY : cached(e, false).loadout(); }

    /** The same, ignoring the cache. */
    public static Loadout loadoutFresh(LivingEntity e) { return RelicNullification.isNullified(e) ? Loadout.EMPTY : cached(e, true).loadout(); }

    /** The equipped relic stacks (the contract's list): held, armour, Curios; attuned or not. */
    public static List<ItemStack> equipped(LivingEntity e) {
        List<ItemStack> out = new ArrayList<>();
        for (Worn w : cached(e, true).worn()) out.add(w.stack());
        return out;
    }

    /** Is this relic equipped and working for the entity (attuned if it needs that)? */
    public static boolean worn(LivingEntity e, String relicId) {
        if (e == null || relicId == null) return false;
        for (Worn w : loadout(e).active()) if (w.def().id().equals(relicId)) return true;
        return false;
    }

    /** Forget the kept look at this entity (it changed what it wears or is attuned to). */
    public static void invalidate(LivingEntity e) {
        if (e == null) return;
        cacheFor(e).remove(e.getUUID());
    }

    public static void clearCaches() {
        SERVER.clear();
        CLIENT.clear();
    }

    private static Cached cached(LivingEntity e, boolean fresh) {
        Map<UUID, Cached> cache = cacheFor(e);
        long now = e.level().getGameTime();
        Cached c = fresh ? null : cache.get(e.getUUID());
        if (c != null && now >= c.tick() && now - c.tick() < ttlFor(e, ttl())) return c;
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        List<Located> located = scan(e);
        List<Worn> worn = new ArrayList<>(located.size());
        for (Located l : located) worn.add(l.worn());
        Cached made = new Cached(now, worn, located, worn.isEmpty() ? Loadout.EMPTY : work(e, worn));
        if (cache.size() > MAX_CACHED) evict(cache, now);                        // a mob-heavy world cannot make this grow without end
        cache.put(e.getUUID(), made);
        com.newuniverse.nusmp.perf.PerfProbe.end(e.level().isClientSide() ? SCAN_CLIENT : SCAN_SERVER, t0);
        return made;
    }

    /** How many entities' looks are kept at most. */
    static final int MAX_CACHED = 1024;

    /**
     * 1.112.0 (lag audit): makes room in a full table. It used to clear the whole table at 400 entries, which on a server with 100+ players (plus whatever mobs asked) made every player
     * rescan on the same tick, again and again. Now only the looks that are no longer good are dropped (older than the lifetime, or from a game time in the future after a world switch);
     * the table is cleared only if that was not enough.
     */
    private static void evict(Map<UUID, Cached> cache, long now) {
        cache.values().removeIf(c -> now < c.tick() || now - c.tick() >= ttl());
        if (cache.size() > MAX_CACHED) cache.clear();
    }

    private static final EquipmentSlot[] ARMOUR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET};

    private static List<Located> scan(LivingEntity e) {
        List<Located> out = new ArrayList<>(4);
        ItemStack main = e.getMainHandItem(), off = e.getOffhandItem();            // 1.112.0: no array per look
        RelicDef dm = defOf(main);
        if (dm != null && dm.slot().isHand()) out.add(new Located(main, dm, Where.HAND));
        RelicDef dof = defOf(off);
        if (dof != null && dof.slot().isHand()) out.add(new Located(off, dof, Where.HAND));
        for (EquipmentSlot slot : ARMOUR_SLOTS) {
            ItemStack s = e.getItemBySlot(slot);
            RelicDef d = defOf(s);
            if (d != null && d.slot().isArmor()) out.add(new Located(s, d, Where.ARMOUR));
        }
        int armourEnd = out.size();
        for (Located x : scanExtra(e)) {
            boolean twin = false;                                                // 3.10.0: the same head/feet relic worn as armour AND in its Curios slot counts once
            if (x.where() == Where.CURIOS && x.def().slot().isArmor()) for (int i = 0; i < armourEnd; i++) if (out.get(i).where() == Where.ARMOUR && out.get(i).def().id().equals(x.def().id())) twin = true;
            if (!twin) out.add(x);
        }                                                                        // the Curios slots, then (1.50.0) the pack of a named mob (empty for players)
        if (e.level().isClientSide()) addSynced(e, out);                         // 1.99.0: what the server says the wearer carries and this client's own look did not find
        return out;
    }

    /** Client side: adds the relics of the server's list ({@link WornSync}) that the client's own look missed (a mob's pack, or a Curios read that failed). */
    private static void addSynced(LivingEntity e, List<Located> out) {
        List<ItemStack> told = WornSync.client(e.getUUID());
        if (told.isEmpty()) return;
        List<String> found = new ArrayList<>(out.size());
        for (Located l : out) if (l.where() != Where.HAND) found.add(WornSync.keyOf(l.stack()));
        for (int i : WornMerge.missing(found, WornSync.keysOf(told))) {
            ItemStack s = told.get(i);
            RelicDef d = defOf(s);
            if (d != null && !d.slot().isHand()) out.add(new Located(s, d, Where.SYNCED));
        }
    }

    private static Loadout work(LivingEntity e, List<Worn> worn) {
        int n = worn.size();
        boolean[] needs = new boolean[n], mine = new boolean[n];
        for (int i = 0; i < n; i++) {
            needs[i] = worn.get(i).def().needsAttunement() && !worn.get(i).def().isPairLeft();      // 1.29.0: the left half of a pair does not use up a place of its own
            mine[i] = Attune.isMine(worn.get(i).stack(), e);
        }
        boolean[] ok = AttuneRules.working(needs, mine, AttuneRules.CAP);
        for (int i = 0; i < n; i++) if (worn.get(i).def().isPairLeft()) ok[i] = !worn.get(i).def().needsAttunement() || mine[i];      // it answers to its own attunement (set together with the right half)
        List<Worn> active = new ArrayList<>(n);
        Set<String> flags = new HashSet<>();
        Map<String, Double> resist = new LinkedHashMap<>();
        double ep = 0;
        for (int i = 0; i < n; i++) {
            if (!ok[i]) continue;
            Worn w = worn.get(i);
            active.add(w);
            ep += com.newuniverse.nusmp.relics.tier.Tiers.effectiveGrade(w.stack(), w.def()).epBonus();      // 1.29.0: the effective (raised) grade
            for (Passive p : w.def().passives()) {
                if (p.kind() == Passive.Kind.FLAG) flags.add(p.key());
                else if (p.kind() == Passive.Kind.RESIST) resist.merge(p.key(), p.amount(), Double::sum);
            }
        }
        List<Resist> rs = new ArrayList<>(resist.size());
        for (Map.Entry<String, Double> r : resist.entrySet()) rs.add(new Resist(r.getKey(), Math.min(0.9, r.getValue())));
        return new Loadout(List.copyOf(active), Set.copyOf(flags), List.copyOf(rs), Math.min(0.25, ep));
    }
}
