package com.newuniverse.nusmp.item;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.item.MagicWeaponItem.Kind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 1.15.0: the weapons work as Curios items.
 * <ul>
 *   <li><b>Slots</b> are data: data/curios/tags/item/{curio,belt,back,hands,charm}.json list the weapons, so Curios (when it is installed)
 *       lets them be socketed: blades on the belt, long arms and the shield on the back, the Hand of Providence in the hands slot, the Brooch
 *       Pin as a charm. Nothing here needs Curios to be present.</li>
 *   <li><b>Carried passives</b>: with Curios loaded, a weapon worn in a Curios slot (and not also in a hand) grants its small sheathed bonus,
 *       listed in {@link #PASSIVES}. The Curios inventory is read by reflection (no compile dependency on the API: the build cannot reach its
 *       maven), and every failure just means "nothing is worn": the weapons stay fully usable in hand.</li>
 * </ul>
 */
public final class CuriosLink {
    private CuriosLink() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static Boolean loaded;
    private static volatile long brokenUntil;               // 1.46.0: a failure pauses Curios reads for 10 s instead of switching them off for the whole session
    private static Method getInventory, getEquipped;

    private static boolean loaded() {
        if (loaded == null) {
            ModList list = ModList.get();
            if (list == null) return false;
            loaded = list.isLoaded("curios");
        }
        return loaded;
    }

    /** Is Curios installed and its inventory readable? (The Nexus relics fall back to the off-hand when it is not.) */
    public static boolean available() { return loaded() && !isBroken(); }

    private static boolean isBroken() { return System.currentTimeMillis() < brokenUntil; }

    // ---------------------------------------------------------------- reading the Curios inventory
    private static volatile String lastFailure = "";
    private static volatile int failures;

    /** The last reason a read of the Curios inventory failed ("" if none did since the game started); for {@code /multiverse relic debug}. */
    public static String lastFailure() { return lastFailure; }

    /** How many reads failed since the game started. */
    public static int failureCount() { return failures; }

    /** Records a failure and logs it once per distinct reason (a failing read repeats every ten seconds; the log must not fill up with it). */
    private static void noteFailure(String what, Throwable t) {
        failures++;
        String msg = what + ": " + t;
        if (!msg.equals(lastFailure)) LOG.warn("[nusmp] Curios is present but its inventory could not be read ({}); relics worn in Curios slots may be invisible until this is fixed", msg);
        lastFailure = msg;
    }

    /** Every stack worn in a Curios slot of this entity (empty without Curios, or if its API is not what was expected). */
    public static List<ItemStack> equipped(LivingEntity e) {
        if (isBroken() || !loaded()) return List.of();
        try {
            // each method is looked up on its own: flags() fills getInventory first, and a shared "if getInventory == null" left getEquipped null (1.46.0 fix)
            Method inv = getInventory;
            if (inv == null) inv = getInventory = Class.forName("top.theillusivec4.curios.api.CuriosApi").getMethod("getCuriosInventory", LivingEntity.class);
            Object opt = inv.invoke(null, e);
            if (!(opt instanceof Optional<?> o) || o.isEmpty()) return List.of();
            Object handler = o.get();
            try {                                                                   // the first road: the combined handler of every worn stack
                Method eq = getEquipped;
                if (eq == null) eq = getEquipped = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler").getMethod("getEquippedCurios");
                if (eq.invoke(handler) instanceof IItemHandler h) return stacksOf(h);
            } catch (ReflectiveOperationException | RuntimeException firstRoad) {
                getEquipped = null;
                noteFailure("getEquippedCurios", firstRoad);                        // 1.99.0: not the end of the read, the second road below still answers
            }
            List<ItemStack> out = new ArrayList<>();                                // the second road: each slot type's own stack handler (what the render toggles read)
            initStackMethods();
            if (getCurios.invoke(handler) instanceof Map<?, ?> map) {
                for (Object sh : map.values()) if (getStacks.invoke(sh) instanceof IItemHandler h) out.addAll(stacksOf(h));
            }
            return out;
        } catch (Throwable t) {
            brokenUntil = System.currentTimeMillis() + 10_000L;
            getInventory = null; getEquipped = null;
            noteFailure("read", t);
            return List.of();
        }
    }

    /** One kept read of a wearer's Curios slots: the entity's own tick count when it was made, and the stacks. */
    private record Seen(int tick, List<ItemStack> stacks) {}

    private static final Map<java.util.UUID, Seen> SEEN = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 1.112.0 (lag audit): {@link #equipped} for code that asks once a second or so (the franchise engravings, the sheathed passives below): the answer of the last read is reused for up to
     * {@code maxAgeTicks} of the entity's own ticks, so two once-a-second passes of one player that are a few ticks apart share ONE reflective read (which builds a combined handler and a list each
     * time). The stacks are the live ones, so what is inside them is always current; only which slots hold something can be up to {@code maxAgeTicks} old. A server of a hundred players made
     * three of these reads per player per second before.
     */
    public static List<ItemStack> equippedCached(LivingEntity e, int maxAgeTicks) {
        if (e == null) return List.of();
        int now = e.tickCount;
        Seen s = SEEN.get(e.getUUID());
        if (s != null && now >= s.tick() && now - s.tick() < maxAgeTicks) return s.stacks();
        List<ItemStack> fresh = equipped(e);
        if (SEEN.size() > 512) SEEN.clear();                                         // players who left are never asked again; this keeps the table small without a log-out hook
        SEEN.put(e.getUUID(), new Seen(now, fresh));
        return fresh;
    }

    /** Forgets the kept read of this entity (log out, server stop). */
    public static void forget(java.util.UUID id) { if (id != null) SEEN.remove(id); }

    public static void clearCaches() { SEEN.clear(); }

    private static List<ItemStack> stacksOf(IItemHandler h) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    /**
     * relics_wear: the slot types this entity really has in its Curios inventory, each with the handler of its places (slot id to the stacks, in Curios' own order). This is the runtime
     * fact behind "can a relic be worn here": a slot id that is not a key of this map was never given to the entity (its slot file or entity assignment is missing or was overridden), whatever
     * the mod's own data says. Used by the right-click equip ({@code RelicCurioEquip}), {@code /multiverse relic debug} and the self-test {@code relic.wear}. Empty without Curios or on any
     * failure (the failure is recorded like every other read). Not for per-tick use: it builds a map.
     */
    public static Map<String, IItemHandler> slotHandlers(LivingEntity e) {
        if (e == null || isBroken() || !loaded()) return Map.of();
        try {
            Method inv = getInventory;
            if (inv == null) inv = getInventory = Class.forName("top.theillusivec4.curios.api.CuriosApi").getMethod("getCuriosInventory", LivingEntity.class);
            Object opt = inv.invoke(null, e);
            if (!(opt instanceof Optional<?> o) || o.isEmpty()) return Map.of();
            initStackMethods();
            if (!(getCurios.invoke(o.get()) instanceof Map<?, ?> map)) return Map.of();
            Map<String, IItemHandler> out = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> en : map.entrySet()) {
                if (en.getKey() instanceof String id && getStacks.invoke(en.getValue()) instanceof IItemHandler h) out.put(id, h);
            }
            return out;
        } catch (Throwable t) {
            brokenUntil = System.currentTimeMillis() + 10_000L;
            getInventory = null; getCurios = null; getStacks = null; getRenders = null; rendersChecked = false;
            noteFailure("slot types", t);
            return Map.of();
        }
    }

    private static boolean rendersChecked;

    /** getCurios and getStacks are needed to read the worn stacks; getRenders (the eye icon) is optional: without it every worn stack counts as shown. */
    private static void initStackMethods() throws ReflectiveOperationException {
        if (getCurios != null && getStacks != null && rendersChecked) return;
        Class<?> handler = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
        Class<?> stacksHandler = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");
        Method c = handler.getMethod("getCurios"), st = stacksHandler.getMethod("getStacks");
        Method r = null;
        try {
            r = stacksHandler.getMethod("getRenders");
        } catch (NoSuchMethodException noToggle) {
            noteFailure("getRenders", noToggle);                                    // an API without the eye icon: everything is shown
        }
        getCurios = c;
        getStacks = st;
        getRenders = r;
        rendersChecked = true;
    }

    // ---------------------------------------------------------------- the render toggles (the eye icon of the Curios screen)
    /** One Curios stack and whether its slot's render toggle is on. */
    public record RenderFlag(ItemStack stack, boolean render) {}

    private static Method getCurios, getStacks, getRenders;
    private static volatile long flagsBrokenUntil;

    /**
     * The worn stacks of this entity with the render flag of the slot each sits in (the eye icon of the Curios screen: ICurioStacksHandler#getRenders, one boolean per
     * slot index). Empty without Curios or on any failure; an index the list does not cover renders (true).
     */
    public static List<RenderFlag> flags(LivingEntity e) {
        if (e == null || System.currentTimeMillis() < flagsBrokenUntil || !loaded()) return List.of();
        try {
            if (getInventory == null) {
                getInventory = Class.forName("top.theillusivec4.curios.api.CuriosApi").getMethod("getCuriosInventory", LivingEntity.class);
            }
            initStackMethods();
            Object opt = getInventory.invoke(null, e);
            if (!(opt instanceof Optional<?> o) || o.isEmpty()) return List.of();
            if (!(getCurios.invoke(o.get()) instanceof Map<?, ?> map)) return List.of();
            List<RenderFlag> out = new ArrayList<>();
            for (Object sh : map.values()) {
                if (!(getStacks.invoke(sh) instanceof IItemHandler h)) continue;
                List<?> renders = getRenders != null && getRenders.invoke(sh) instanceof List<?> l ? l : List.of();
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack s = h.getStackInSlot(i);
                    if (s.isEmpty()) continue;
                    boolean on = i >= renders.size() || !(renders.get(i) instanceof Boolean b) || b;
                    out.add(new RenderFlag(s, on));
                }
            }
            return out;
        } catch (Throwable t) {
            flagsBrokenUntil = System.currentTimeMillis() + 10_000L;
            getInventory = null; getCurios = null; getStacks = null; getRenders = null; rendersChecked = false;
            noteFailure("render toggles", t);                                      // every worn relic is drawn while the toggles cannot be read
            return List.of();
        }
    }

    /** Per worn Curios stack, in the order of {@link #flags}: true if its slot's render toggle is on. True for everything when Curios is absent (the list is empty then). */
    public static List<Boolean> renderFlags(LivingEntity e) {
        List<RenderFlag> f = flags(e);
        List<Boolean> out = new ArrayList<>(f.size());
        for (RenderFlag r : f) out.add(r.render());
        return out;
    }

    /** Is this worn stack to be drawn? True unless Curios says its slot's render toggle is off (identity first, then an equal stack). True on any doubt. */
    public static boolean isRendered(LivingEntity e, ItemStack stack) {
        try {
            List<RenderFlag> f = flags(e);
            for (RenderFlag r : f) if (r.stack() == stack) return r.render();
            for (RenderFlag r : f) if (ItemStack.matches(r.stack(), stack)) return r.render();
        } catch (Throwable ignored) {
            // draw it
        }
        return true;
    }

    /** Is a weapon of this kind worn in a Curios slot? */
    public static boolean carries(LivingEntity e, Kind k) {
        for (ItemStack s : equipped(e)) if (s.getItem() instanceof MagicWeaponItem w && w.kind == k) return true;
        return false;
    }

    // ---------------------------------------------------------------- the sheathed passives
    /** One carried bonus: an attribute change (the amount is a share for the multiplied operations) and its tooltip text. */
    private record Pass(Holder<Attribute> attr, double amount, Operation op, String text) {}

    private static final Map<Kind, Pass> PASSIVES = new EnumMap<>(Kind.class);

    private static void put(Kind k, Holder<Attribute> a, double amount, Operation op, String text) { PASSIVES.put(k, new Pass(a, amount, op, text)); }

    static {
        Operation add = Operation.ADD_VALUE, pct = Operation.ADD_MULTIPLIED_BASE;
        put(Kind.DEMON_SLASHER_KATANA, Attributes.MOVEMENT_SPEED, 0.04, pct, "Ready stance: +4% speed");
        put(Kind.MIASMA_KATANA, Attributes.MOVEMENT_SPEED, 0.04, pct, "Shrouded steps: +4% speed");
        put(Kind.SPELL_FORGED_RAPIER, Attributes.ENTITY_INTERACTION_RANGE, 0.3, add, "Long reach: +0.3 reach");
        put(Kind.SEVERING_GREATSWORD, Attributes.ARMOR_TOUGHNESS, 1, add, "Heavy guard: +1 armour toughness");
        put(Kind.DEMON_SLAYER, Attributes.ARMOR, 2, add, "Anti-magic aegis: +2 armour");
        put(Kind.DEMON_DWELLER, Attributes.ARMOR_TOUGHNESS, 1, add, "Anti-magic guard: +1 armour toughness");
        put(Kind.DEMON_DESTROYER, Attributes.ARMOR_TOUGHNESS, 1, add, "Anti-magic guard: +1 armour toughness");
        put(Kind.LICHT_DWELLER, Attributes.MAX_HEALTH, 2, add, "Licht's light: +2 health");
        put(Kind.LICHT_DESTROYER, Attributes.MAX_HEALTH, 2, add, "Licht's light: +2 health");
        put(Kind.RIMEHEART, Attributes.KNOCKBACK_RESISTANCE, 0.1, add, "Rooted in frost: +10% knockback resistance");
        put(Kind.LAST_WORD, Attributes.LUCK, 1, add, "The last word: +1 luck");
        put(Kind.ELSDOCIA, Attributes.MAX_HEALTH, 4, add, "Legacy of the Wizard Kings: +4 health");
        put(Kind.QUICKLINGS_IVY, Attributes.MOVEMENT_SPEED, 0.04, pct, "Quickling's grace: +4% speed");
        put(Kind.MAGEBANE_LONGSWORD, Attributes.ARMOR_TOUGHNESS, 1, add, "Magebane guard: +1 armour toughness");
        put(Kind.PRISM_BLADE, Attributes.LUCK, 1, add, "Spectrum light: +1 luck");
        put(Kind.THUNDERCLEAVE, Attributes.ATTACK_DAMAGE, 1, add, "Thunder in the bones: +1 attack damage");
        put(Kind.BLOODBOUND_KATANA, Attributes.ATTACK_DAMAGE, 1, add, "Blood-bound: +1 attack damage");
        put(Kind.PERMAFROST_SABRE, Attributes.KNOCKBACK_RESISTANCE, 0.1, add, "Permafrost: +10% knockback resistance");
        put(Kind.CYCLONE_RAPIER, Attributes.MOVEMENT_SPEED, 0.05, pct, "Cyclone step: +5% speed");
        put(Kind.BOLT_THROWER, Attributes.LUCK, 0.5, add, "Sure shot: +0.5 luck");
        put(Kind.CROSSBOW_OF_SWIFT_DEATH, Attributes.LUCK, 0.5, add, "Swift death: +0.5 luck");
        put(Kind.CLUB_OF_ROT, Attributes.MAX_HEALTH, 2, add, "Hardy rot: +2 health");
        put(Kind.DOOM_HAMMER, Attributes.KNOCKBACK_RESISTANCE, 0.15, add, "Immovable: +15% knockback resistance");
        put(Kind.MAGEBANE_SHIELD, Attributes.ARMOR, 1, add, "Magic damage taken is cut by a third (a half with the Magebane Longsword in a hand); +1 armour");
        put(Kind.WEAPON_OF_RESURGENCE, Attributes.MAX_HEALTH, 2, add, "Resurgent: +2 health");
        put(Kind.BROOCH_PIN, Attributes.LUCK, 1, add, "Lucky pin: +1 luck");
        put(Kind.BUTCHER_CLEAVER, Attributes.LUCK, 0.5, add, "A cook's luck: +0.5 luck");
        put(Kind.HAND_OF_PROVIDENCE, Attributes.MAX_HEALTH, 4, add, "Providence: +4 health");
    }

    /** 1.99.0 (performance pass): the modifier id of each kind, built once. The once-a-second pass asked for one per kind per player (28 strings and locations per player per second). */
    private static final ResourceLocation[] IDS = new ResourceLocation[Kind.values().length];

    private static ResourceLocation id(Kind k) {
        ResourceLocation r = IDS[k.ordinal()];
        if (r == null) IDS[k.ordinal()] = r = ResourceLocation.fromNamespaceAndPath("nusmp", "curio_" + k.name().toLowerCase(java.util.Locale.ROOT));
        return r;
    }

    /** The tooltip line of a weapon that can be worn. */
    static void tooltip(Kind k, List<Component> tip) {
        Pass p = PASSIVES.get(k);
        if (p == null) return;
        tip.add(Component.literal("Worn in a Curios slot: " + p.text()).withStyle(ChatFormatting.DARK_GREEN));
    }

    /** Every second: bring each weapon's carried bonus in line with what is worn (and not already in a hand, where its own modifiers apply). */
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_CURIOS_PASSIVES = com.newuniverse.nusmp.perf.PerfProbe.section("s.curios.passives");   // 1.99.0: performance probe (off by default: one flag read)

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onPlayerTick0(event);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_CURIOS_PASSIVES, t0);
        }
    }

    private static void onPlayerTick0(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 13) return;
        Set<Kind> worn = EnumSet.noneOf(Kind.class);
        for (ItemStack s : equippedCached(p, 5)) if (s.getItem() instanceof MagicWeaponItem w) worn.add(w.kind);        // 1.112.0 (lag audit): shares the read of the franchise pass
        if (p.getMainHandItem().getItem() instanceof MagicWeaponItem w1) worn.remove(w1.kind);
        if (p.getOffhandItem().getItem() instanceof MagicWeaponItem w2) worn.remove(w2.kind);
        for (Map.Entry<Kind, Pass> e : PASSIVES.entrySet()) {
            AttributeInstance inst = p.getAttribute(e.getValue().attr());
            if (inst == null) continue;
            ResourceLocation id = id(e.getKey());
            boolean has = inst.hasModifier(id);
            if (worn.contains(e.getKey())) {
                if (!has) inst.addTransientModifier(new AttributeModifier(id, e.getValue().amount(), e.getValue().op()));
            } else if (has) inst.removeModifier(id);
        }
    }
}
