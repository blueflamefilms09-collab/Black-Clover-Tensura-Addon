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

    /** How many ticks a look at a wearer stays good. */
    public static final int TTL = 10;

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

    /** One resistance a worn relic grants. */
    public record Resist(String id, double fraction) {}

    /** What works on an entity right now (equipped, and attuned where needed, at most three attuned): its relics, flags, resistances, EP lift. */
    public record Loadout(List<Worn> active, Set<String> flags, List<Resist> resists, double epBonus) {
        public static final Loadout EMPTY = new Loadout(List.of(), Set.of(), List.of(), 0.0);
    }

    private record Cached(long tick, List<Worn> worn, Loadout loadout) {}

    private static final Map<UUID, Cached> SERVER = new ConcurrentHashMap<>(), CLIENT = new ConcurrentHashMap<>();

    private static Map<UUID, Cached> cacheFor(LivingEntity e) { return e.level().isClientSide() ? CLIENT : SERVER; }

    /** Every relic the entity has equipped (a hand for hand relics, the head/chest/feet slots, a Curios slot), working or not. Never null. */
    public static List<Worn> wornList(LivingEntity e) { return cached(e, false).worn(); }

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
        if (c != null && now >= c.tick() && now - c.tick() < TTL) return c;
        List<Worn> worn = scan(e);
        Cached made = new Cached(now, worn, worn.isEmpty() ? Loadout.EMPTY : work(e, worn));
        if (cache.size() > 400) cache.clear();                                   // a mob-heavy world cannot make this grow without end
        cache.put(e.getUUID(), made);
        return made;
    }

    private static List<Worn> scan(LivingEntity e) {
        List<Worn> out = new ArrayList<>(4);
        for (ItemStack s : new ItemStack[]{e.getMainHandItem(), e.getOffhandItem()}) {
            RelicDef d = defOf(s);
            if (d != null && d.slot().isHand()) out.add(new Worn(s, d));
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET}) {
            ItemStack s = e.getItemBySlot(slot);
            RelicDef d = defOf(s);
            if (d != null && d.slot().isArmor()) out.add(new Worn(s, d));
        }
        for (ItemStack s : CuriosLink.equipped(e)) {
            RelicDef d = defOf(s);
            if (d != null && d.slot().isCurio()) out.add(new Worn(s, d));
        }
        return out;
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
