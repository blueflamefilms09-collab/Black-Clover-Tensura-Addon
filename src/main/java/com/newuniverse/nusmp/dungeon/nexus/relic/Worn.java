package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.newuniverse.nusmp.item.CuriosLink;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Which relics a player is wearing right now and has attuned (docs/living_dungeon.md section 7): a relic works only while it sits in its
 * Curios slot (the Gambler's Page: in the off-hand) and is attuned to its wearer. Without Curios the off-hand is the only place anything
 * can be worn. Reading the Curios inventory goes through reflection, so the answer is kept for {@link #TTL} ticks per player.
 * Server thread (and the client, where only the stack's own data is known).
 */
public final class Worn {
    private Worn() {}

    /** How many ticks a look at a player's relics stays good. */
    public static final int TTL = 10;
    private static final Map<UUID, long[]> CACHE = new HashMap<>();

    /** The relics this player wears and has attuned, as a bit mask of {@link Relic#bit()}. */
    public static int mask(Player p) { return mask(p, false); }

    /** The same; {@code fresh} ignores the kept answer. */
    public static int mask(Player p, boolean fresh) {
        long now = p.level().getGameTime();
        long[] kept = CACHE.get(p.getUUID());
        if (!fresh && kept != null && now >= kept[0] && now - kept[0] < TTL) return (int) kept[1];
        int mask = 0;
        for (Place place : places(p)) {
            Relic r = Relic.of(place.stack);
            if (r != null && fits(r, place.offHand) && Attunement.isAttuned(place.stack, p)) mask |= r.bit();
        }
        CACHE.put(p.getUUID(), new long[]{now, mask});
        return mask;
    }

    public static boolean has(Player p, Relic r) { return (mask(p) & r.bit()) != 0; }

    /** The worn, attuned stack of this relic (the real one in the slot, so it can be changed), or EMPTY. Not kept. */
    public static ItemStack stack(Player p, Relic r) {
        for (Place place : places(p)) {
            if (Relic.of(place.stack) == r && fits(r, place.offHand) && Attunement.isAttuned(place.stack, p)) return place.stack;
        }
        return ItemStack.EMPTY;
    }

    public static void forget(UUID id) { CACHE.remove(id); }

    public static void clear() { CACHE.clear(); }

    private record Place(ItemStack stack, boolean offHand) {}

    private static List<Place> places(Player p) {
        List<Place> out = new ArrayList<>();
        for (ItemStack s : CuriosLink.equipped(p)) if (s.getItem() instanceof RelicItem) out.add(new Place(s, false));
        ItemStack off = p.getOffhandItem();
        if (off.getItem() instanceof RelicItem) out.add(new Place(off, true));
        return out;
    }

    /** May this relic work from here? The Page only from the off-hand; the others from a Curios slot, or from the off-hand when there is no Curios. */
    private static boolean fits(Relic r, boolean offHand) {
        if (r == Relic.GAMBLERS_GRIMOIRE_PAGE) return offHand;
        return offHand ? !CuriosLink.available() : true;
    }
}
