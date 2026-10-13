package com.newuniverse.nusmp.relics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Attunement state on a stack: the {@code relic_attuned} component holds the player's UUID. A relic that needs attunement works only for that
 * player; binding it takes five seconds of sneaking with the use button held (see {@link RelicEngine#attuneTick}). Binding another player's relic
 * takes it over.
 */
public final class Attune {
    private Attune() {}

    public static UUID owner(ItemStack stack) { return stack.get(RelicComponents2.ATTUNED.get()); }

    /** Is this stack attuned to this entity? (Mobs that wear a relic count as attuned: they cannot go through the ritual.) */
    public static boolean isMine(ItemStack stack, LivingEntity e) {
        if (!(e instanceof Player)) return true;
        UUID u = owner(stack);
        return u != null && u.equals(e.getUUID());
    }

    /** Does this stack work for this entity: it needs no attunement, or it is attuned to them? */
    public static boolean works(ItemStack stack, RelicDef def, LivingEntity e) { return !def.needsAttunement() || isMine(stack, e); }

    /** How many relics that need attunement are attuned to this player and carried (inventory, off-hand, armour, Curios), counting {@code except} out. */
    public static int carried(Player p, ItemStack except) {
        int n = 0;
        for (ItemStack s : RelicEngine.carriedStacks(p)) {
            if (s == except) continue;
            RelicDef d = Relics.defOf(s);
            if (d != null && d.needsAttunement() && isMine(s, p)) n++;
        }
        return n;
    }

    /** Binds the stack to the player if they have room; false when three are already attuned. */
    public static boolean bind(ServerPlayer p, ItemStack stack) {
        if (!AttuneRules.canAttune(carried(p, stack))) return false;
        stack.set(RelicComponents2.ATTUNED.get(), p.getUUID());
        Relics.invalidate(p);
        return true;
    }

    /** Lets go of the relic. */
    public static void release(ServerPlayer p, ItemStack stack) {
        stack.remove(RelicComponents2.ATTUNED.get());
        Relics.invalidate(p);
    }
}
