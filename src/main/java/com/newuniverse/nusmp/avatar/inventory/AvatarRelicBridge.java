package com.newuniverse.nusmp.avatar.inventory;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.relics.Attune;
import com.newuniverse.nusmp.relics.AttuneRules;
import com.newuniverse.nusmp.relics.MobRelicsConfig;
import com.newuniverse.nusmp.relics.RelicComponents2;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Why: the Avatar's "Curios slots" are only worth having if the relics in them work. The relic system already lets a named mob wear relics (the 1.50.0 mob relics:
 * a pack kept in the mob's persistent data, attunement to the mob's own UUID, a once-a-second pass that applies the numbers) and the Avatar is such a mob. So the
 * trinket slots of the inventory menu are a VIEW of that pack, and this class does what {@code MobRelics.give} does when it hands a relic over, for a relic put into
 * the Avatar's hands, armour or trinket slots through the menu: it attunes a relic that needs it to the Avatar (three at most, like anyone), lets go of that bond when
 * the relic leaves, keeps the relic system's "this mob carries relics" marker true exactly while the Avatar holds a relic, drops the relic cache, and refuses a relic
 * beyond the configured number a mob may carry. The marker's key is private to {@code MobRelics}, so it is repeated in {@link #GIVER} and a test
 * ({@code MobRelicsKeysTest}) reads the source of that class and fails if it ever changes.
 */
public final class AvatarRelicBridge {
    private AvatarRelicBridge() {}

    /** The persistent-data keys of {@code MobRelics} (its pack of relics, and the marker that this mob carries relics). Repeated on purpose; pinned by a test. */
    public static final String PACK = "nusmp_mob_relics", GIVER = "nusmp_mob_relic_giver";

    /** Is the stack a relic of this mod's relic system? */
    public static boolean isRelic(ItemStack s) { return s != null && !s.isEmpty() && Relics.defOf(s) != null; }

    /** A relic is going into one of the Avatar's slots: bind it to the Avatar when it needs attunement and the Avatar still has room for one more bond. */
    public static void onEnter(AvatarEntity a, ItemStack s) {
        RelicDef d = s == null || s.isEmpty() ? null : Relics.defOf(s);
        if (d == null || !d.needsAttunement() || d.isPairLeft()) return;
        if (Attune.owner(s) != null && !a.getUUID().equals(Attune.owner(s))) return;      // bound to somebody else already: it simply does not work for the Avatar
        if (attunedCount(a, s) < AttuneRules.CAP) s.set(RelicComponents2.ATTUNED.get(), a.getUUID());
    }

    /** A relic is leaving the Avatar: the bond to it is cut, so the relic is free to attune to a player again (what taking it back from the mob does). */
    public static void onLeave(AvatarEntity a, ItemStack s) {
        if (s == null || s.isEmpty()) return;
        UUID owner = Attune.owner(s);
        if (owner != null && owner.equals(a.getUUID())) s.remove(RelicComponents2.ATTUNED.get());
    }

    /** How many relics that need attunement are already bound to the Avatar, not counting {@code except}. */
    private static int attunedCount(AvatarEntity a, ItemStack except) {
        int n = 0;
        Relics.invalidate(a);
        for (Relics.Worn w : Relics.wornList(a)) {
            if (w.stack() == except) continue;
            RelicDef d = w.def();
            if (d.needsAttunement() && !d.isPairLeft() && a.getUUID().equals(Attune.owner(w.stack()))) n++;
        }
        return n;
    }

    /** May one more relic be put on a slot that is empty (or holds no relic)? Not beyond the number a named mob may carry in all. */
    public static boolean mayCarryMore(AvatarEntity a) {
        Relics.invalidate(a);
        return Relics.wornList(a).size() < MobRelicsConfig.maxCarried();
    }

    /**
     * After any change: the relic system's marker is set while the Avatar carries a relic in its hands, on its body or in its pack (so its once-a-second pass applies
     * the relics' numbers) and removed when none is left; the relic cache is dropped. Server side only.
     */
    public static void refresh(AvatarEntity a) {
        if (a.level().isClientSide) return;
        Relics.invalidate(a);
        boolean any = !Relics.wornList(a).isEmpty();                 // hands, armour, a Curios slot and the pack: the same list MobRelics.takeBack looks at
        CompoundTag data = a.getPersistentData();
        if (any) {
            if (!data.hasUUID(GIVER)) data.putUUID(GIVER, a.creatorUuid() != null ? a.creatorUuid() : a.getUUID());
        } else {
            data.remove(GIVER);
        }
    }
}
