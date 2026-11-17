package com.newuniverse.nusmp.unique.kind;

import com.newuniverse.nusmp.dragonballs.BallSet;
import com.newuniverse.nusmp.dragonballs.DbManager;
import com.newuniverse.nusmp.dragonballs.DragonBallItem;
import com.newuniverse.nusmp.unique.UniqueKind;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/**
 * Why: each Dragon Ball of a set is one of a kind (the registry {@code DbData} knows where each ball is, and the scatter generation says which balls are current). Id: {@code dragonball:<set>:<star>}. The balls are
 * already indestructible on the ground; this covers the ones deleted in an inventory or a container. Back in the pool = the ball goes back into the world the way a ball lost in the void does: the registry gets a new far place
 * for it and the five-second sweep puts it there once a player has loaded the chunk. The guardian stamps the balls it sees, so an old copy dissolves after the return ({@link #stamped}).
 */
public final class DragonBallUnique implements UniqueKind {
    public static final String PREFIX = "dragonball:";

    @Override public String name() { return "dragonball"; }

    @Override public boolean owns(String id) { return set(id) != null && star(id) > 0; }

    @Override public String idOf(ItemStack stack) { return stack.getItem() instanceof DragonBallItem b ? PREFIX + b.set().id() + ":" + b.star() : null; }

    @Override public boolean valid(MinecraftServer server, String id, ItemStack stack) {
        BallSet set = set(id);
        return set != null && DbManager.isCurrent(server, stack, set);
    }

    @Override public boolean stamped() { return true; }

    @Override public boolean exists(MinecraftServer server, String id) {
        BallSet set = set(id);
        int star = star(id);
        return set != null && star > 0 && DbManager.ballExists(server, set, star);
    }

    @Override public boolean release(MinecraftServer server, String id) {
        BallSet set = set(id);
        int star = star(id);
        if (set == null || star <= 0 || !DbManager.ballExists(server, set, star)) return false;
        DbManager.returnToWorld(server, set, star);
        return true;
    }

    @Override public String label(String id) {
        BallSet set = set(id);
        return set == null ? id : set.ballName() + " (" + star(id) + " stars)";
    }

    @Override public String pool(String id) { return "The ball reappears at a new far place of its own in the world (the Dragon Radar finds it), like a ball lost in the void."; }

    private static BallSet set(String id) {
        if (id == null || !id.startsWith(PREFIX)) return null;
        String[] p = id.substring(PREFIX.length()).split(":");
        return p.length == 2 ? BallSet.byId(p[0]) : null;
    }

    private static int star(String id) {
        if (id == null || !id.startsWith(PREFIX)) return -1;
        String[] p = id.substring(PREFIX.length()).split(":");
        if (p.length != 2) return -1;
        try {
            int s = Integer.parseInt(p[1]);
            return s >= 1 && s <= 8 ? s : -1;
        } catch (NumberFormatException bad) {
            return -1;
        }
    }
}
