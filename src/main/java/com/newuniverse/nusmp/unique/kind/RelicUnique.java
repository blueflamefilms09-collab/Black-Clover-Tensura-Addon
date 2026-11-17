package com.newuniverse.nusmp.unique.kind;

import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import com.newuniverse.nusmp.unique.UniqueKind;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/**
 * Why: every relic except the consumables is one of a kind, which is how the six Infinity Stones are one of each per world. The God of Order's ledger ({@link UniqueLedger}) already decides which copy is valid (its serial) and
 * issues the relic from loot, bosses and commands only while no live copy is written. Id: {@code relic:<relic id>}. Back in the pool = the same step as {@code /multiverse relic order release <id>}: the ledger no longer knows a
 * live copy, so loot, bosses and operator commands may issue the relic again, and the old copy is a duplicate that the Order dissolves. The two halves of a pair relic are left to the Order (they are one unique across two items).
 */
public final class RelicUnique implements UniqueKind {
    public static final String PREFIX = "relic:";

    @Override public String name() { return "relic"; }

    @Override public boolean owns(String id) { return def(id) != null; }

    @Override public String idOf(ItemStack stack) {
        RelicDef d = Relics.defOf(stack);
        return d != null && d.isUnique() && !d.isPairHalf() ? PREFIX + d.id() : null;
    }

    @Override public boolean valid(MinecraftServer server, String id, ItemStack stack) { return UniqueLedger.isCanonical(stack); }

    @Override public boolean exists(MinecraftServer server, String id) {
        UniqueLedger.Entry e = UniqueLedger.get(server).entry(relicId(id));
        return e != null && e.live();
    }

    @Override public boolean release(MinecraftServer server, String id) { return UniqueLedger.releaseToPool(server, relicId(id)); }

    @Override public String label(String id) {
        RelicDef d = def(id);
        return d == null ? id : d.cardName();
    }

    @Override public String pool(String id) { return "It can be issued again by its normal sources: relic loot rolls, wave and boss drops, shops and operator give (the ledger holds no live copy)."; }

    private static String relicId(String id) { return id != null && id.startsWith(PREFIX) ? id.substring(PREFIX.length()) : id; }

    private static RelicDef def(String id) {
        if (id == null || !id.startsWith(PREFIX)) return null;
        RelicDef d = Relics.byId(relicId(id));
        return d != null && d.isUnique() && !d.isPairHalf() ? d : null;
    }
}
