package com.newuniverse.nusmp.unique.kind;

import com.newuniverse.nusmp.charter.CharterGrimoireItem;
import com.newuniverse.nusmp.charter.CharterNet;
import com.newuniverse.nusmp.charter.CharterTable;
import com.newuniverse.nusmp.charter.ModWorldDropData;
import com.newuniverse.nusmp.unique.UniqueKind;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/**
 * Why: a Canon Grimoire is the world-exclusive drop of one boss, locked by the world's {@code DropLedger} (the first kill that leaves it claims it for ever). Id: {@code charter:<boss>}. The book has no token, so the guardian
 * stamps the copies it sees with the unique's epoch ({@link #stamped}). Back in the pool = the world's drop lock of the boss is cleared (the same step as {@code /multiverse charter reset}), so the boss's next kill rolls
 * for the grimoire again; every copy stamped before that dissolves. A grimoire that an operator gave out without claiming the lock is not guarded ({@link #exists} is false).
 */
public final class CharterUnique implements UniqueKind {
    public static final String PREFIX = "charter:";

    @Override public String name() { return "charter"; }

    @Override public boolean owns(String id) { return id != null && id.startsWith(PREFIX) && id.length() > PREFIX.length(); }

    @Override public String idOf(ItemStack stack) { return stack.getItem() instanceof CharterGrimoireItem g ? PREFIX + g.boss().id() : null; }

    @Override public boolean valid(MinecraftServer server, String id, ItemStack stack) { return true; }

    @Override public boolean stamped() { return true; }

    @Override public boolean exists(MinecraftServer server, String id) { return ModWorldDropData.get(server).claim(boss(id)) != null; }

    @Override public boolean release(MinecraftServer server, String id) {
        ModWorldDropData d = ModWorldDropData.get(server);
        if (d.releaseToPool(boss(id), "Unique Guardian") == null) return false;
        CharterNet.sendAll(d);
        return true;
    }

    @Override public String label(String id) {
        CharterTable.Boss b = CharterTable.byId(boss(id));
        return "Canon Grimoire of " + (b == null ? boss(id) : b.name());
    }

    @Override public String pool(String id) { return "The boss drops it again: its first-kill lock is cleared, so the next kill of the boss rolls for the grimoire (DropRoll)."; }

    static String boss(String id) { return id.startsWith(PREFIX) ? id.substring(PREFIX.length()) : id; }
}
