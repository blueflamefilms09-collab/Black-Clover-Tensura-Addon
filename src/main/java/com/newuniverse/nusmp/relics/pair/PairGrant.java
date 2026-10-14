package com.newuniverse.nusmp.relics.pair;

import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * How a pair comes into the world: both halves together. The Order's ledger keeps one entry per half (the right half under the card's id, the left under
 * {@code <id>_left}), so each half is a one-of-a-kind stack and a world can never hold two pairs; this class issues the halves as one act (a command, a
 * loot roll, a Mimic) and completes a pair that was made before pairs existed (see {@link PairHooks} for the first-login grant).
 */
public final class PairGrant {
    private PairGrant() {}

    /** The right-half def of a pair, from either half's def (or null if it is no pair). */
    public static RelicDef rightOf(RelicDef any) {
        if (any == null || !any.isPairHalf()) return null;
        return any.isPairLeft() ? Relics.byId(any.pairBaseId()) : any;
    }

    /**
     * Issues every half of the pair that does not live yet: both for a new pair, only the missing one to complete a pair (a half lost in a fire).
     * Each returned stack is stamped in the ledger. Empty when the pair is complete already.
     */
    public static List<ItemStack> issueMissing(ServerLevel level, RelicDef any) {
        List<ItemStack> out = new ArrayList<>(2);
        RelicDef right = rightOf(any);
        if (level == null || level.getServer() == null || right == null) return out;
        RelicDef left = Relics.byId(right.pairPartnerId());
        if (left == null) return out;
        Set<String> live = UniqueLedger.liveIds(level.getServer());
        int missing = PairRules.missingHalves(live.contains(right.id()), live.contains(left.id()));
        if ((missing & 1) != 0) add(level, right, out);
        if ((missing & 2) != 0) add(level, left, out);
        return out;
    }

    /** A new pair for a loot roll or a Mimic: both halves, or nothing at all when either half lives already. */
    public static List<ItemStack> issueNewPair(ServerLevel level, RelicDef any) {
        RelicDef right = rightOf(any);
        if (level == null || level.getServer() == null || right == null) return new ArrayList<>();
        Set<String> live = UniqueLedger.liveIds(level.getServer());
        if (!PairRules.rollable(live.contains(right.id()), live.contains(right.pairPartnerId()))) return new ArrayList<>();
        return issueMissing(level, right);
    }

    /** Issues just this one half (a stamped stack), or EMPTY if it lives already or cannot be issued. */
    public static ItemStack issueOne(ServerLevel level, RelicDef half) {
        List<ItemStack> out = new ArrayList<>(1);
        if (level != null && level.getServer() != null && half != null && !UniqueLedger.liveIds(level.getServer()).contains(half.id())) add(level, half, out);
        return out.isEmpty() ? ItemStack.EMPTY : out.get(0);
    }

    private static void add(ServerLevel level, RelicDef d, List<ItemStack> out) {
        ItemStack s = RelicCatalog.newStack(d);
        if (!s.isEmpty() && UniqueLedger.issue(level, d.id(), s)) out.add(s);
    }

    /** Has a left half of this pair ever been issued (or adopted) in this world? */
    public static boolean leftEverIssued(MinecraftServer server, RelicDef right) {
        return UniqueLedger.get(server).known(right.pairPartnerId()).exists();
    }
}
