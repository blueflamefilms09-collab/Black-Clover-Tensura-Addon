package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the one place that knows which stone a player has in the {@code infinity_stone} Curios slot and whether the gate lets it work. Server side only (the client only draws).
 * The slot always exists (Curios slots are fixed data and the mod does not compile against Curios), so the gate is checked here: when a stone is seen in the slot (equip) and once a second after
 * (a bearer whose EP fell pauses). The answer is kept per player UUID, one small entry per bearer, dropped on logout. Cost: one cached Curios read per player per second to see whether a stone is
 * worn at all; the slot map is only built for a player who wears one.
 */
public final class StoneSlot {
    private StoneSlot() {}

    /** What a player has in the slot right now. {@code working} is false while the gate or the setting pauses it. */
    public record State(InfinityStone stone, boolean working) {
        public static final State NONE = new State(null, false);
    }

    private static final Map<UUID, State> STATE = new ConcurrentHashMap<>();

    /** The stone that works for this player (slot + gate + setting), or null. Cheap: one map read. */
    public static InfinityStone active(Player p) {
        if (p == null) return null;
        State s = STATE.get(p.getUUID());
        return s != null && s.working() ? s.stone() : null;
    }

    /** The kept state (a paused stone is still "worn"), never null. */
    public static State state(Player p) {
        State s = p == null ? null : STATE.get(p.getUUID());
        return s == null ? State.NONE : s;
    }

    public static void forget(UUID id) { if (id != null) STATE.remove(id); }

    public static void clear() { STATE.clear(); }

    /** The base maximum EP of the player (0 when Tensura does not know it). The gate never reads the grade-boosted value. */
    public static double baseEp(Player p) { return TensuraBridge.baseMaxEp(p); }

    public static boolean gateOk(Player p) { return StoneRules.gateOk(baseEp(p), StoneConfig.epGate()); }

    /** {@code 1 + log10(EP/gate)}, at most 2 (1 when the setting is off). */
    public static double scale(Player p) {
        return StoneConfig.epScale() ? StoneRules.stoneScale(baseEp(p), StoneConfig.epGate()) : 1.0;
    }

    /** The stone in the infinity_stone slot (the stack itself), or EMPTY. A fresh read of the slot map: not for per-tick use. */
    public static ItemStack inSlot(ServerPlayer p) {
        IItemHandler h = CuriosLink.slotHandlers(p).get(StoneRules.SLOT);
        if (h == null) return ItemStack.EMPTY;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty() && stoneOf(s) != null) return s;
        }
        return ItemStack.EMPTY;
    }

    public static InfinityStone stoneOf(ItemStack s) {
        RelicDef d = Relics.defOf(s);
        return d == null ? null : InfinityStone.byRelicId(d.id());
    }

    /** Does the player carry a stone in any Curios slot? (the cached read the relic scan shares), so a player with none costs one map lookup. */
    public static boolean wearsAnyStone(ServerPlayer p) {
        for (ItemStack s : CuriosLink.equippedCached(p, 20)) if (stoneOf(s) != null) return true;
        return false;
    }

    /**
     * Re-reads the slot and the gate and stores the result. Returns the previous state so the caller can see a change (a new stone is the equip). A stone that sits in the slot while the gate is
     * not met is NOT removed here; it is paused (see {@link StoneEvents} for the equip refusal).
     */
    public static State refresh(ServerPlayer p) {
        State before = state(p);
        State now = State.NONE;
        if (StoneConfig.enabled() && wearsAnyStone(p)) {
            InfinityStone s = stoneOf(inSlot(p));
            if (s != null) now = new State(s, gateOk(p));
        }
        if (now.stone() == null) STATE.remove(p.getUUID());
        else STATE.put(p.getUUID(), now);
        return before;
    }
}
