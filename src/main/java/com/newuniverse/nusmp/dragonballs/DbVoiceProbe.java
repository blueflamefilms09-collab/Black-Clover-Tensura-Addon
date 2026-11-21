package com.newuniverse.nusmp.dragonballs;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the voice gate ({@code dragonballs.voice.DbVoice}) must know "is this player at a complete set?" once a second for every player, so the answer cannot scan the world. This reads only what is already
 * known: the player's own inventory (at most about 41 stacks) and the 16-row ball registry ({@link DbData}) for balls lying on the ground, with a small margin; no entity query, no chunk. It is a hint to decide
 * who is listened to, never a rule: the real check of a summoning stays {@link DbManager#trySummon}. Server thread only. Lives in this package because the registry accessors are package-private.
 */
public final class DbVoiceProbe {
    private DbVoiceProbe() {}

    /** Extra blocks of slack on the gather radius (a ball item can settle a little after its place was recorded). */
    private static final double MARGIN = 2.0;

    /** Is the player carrying or standing at a complete, unlocked set that no ritual is using? Allocation free. */
    public static boolean nearCompleteSet(ServerPlayer p) {
        MinecraftServer s = p.getServer();
        if (s == null || !DbConfig.enabled()) return false;
        int n = DbConfig.count(), full = (1 << n) - 1;
        DbData d = DbManager.data(s);
        String dim = p.level().dimension().location().toString();
        double r = DbConfig.gatherRadius() + MARGIN;
        Inventory inv = p.getInventory();
        for (BallSet set : BallSet.values()) {
            if (DbManager.locked(s, set) || DbSession.of(set) != null) continue;
            int mask = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack st = inv.getItem(i);
                if (st.getItem() instanceof DragonBallItem b && b.set() == set && b.star() <= n && DbManager.current(s, st, set)) mask |= 1 << (b.star() - 1);
            }
            for (int star = 1; star <= n && mask != full; star++) {
                DbData.Ball b = d.balls[set.ordinal()][star - 1];
                if (b == null || b.pending || b.entity == null || !dim.equals(b.dim)) continue;
                if (Math.abs(b.x - p.getX()) <= r && Math.abs(b.z - p.getZ()) <= r && Math.abs(b.y - p.getY()) <= r) mask |= 1 << (star - 1);
            }
            if (mask == full) return true;
        }
        return false;
    }

    /** The summoners of the (at most two) running rituals. */
    public static List<UUID> summoners() {
        List<UUID> out = new ArrayList<>(2);
        for (BallSet set : BallSet.values()) {
            DbSession x = DbSession.of(set);
            if (x != null) out.add(x.summoner);
        }
        return out;
    }
}
