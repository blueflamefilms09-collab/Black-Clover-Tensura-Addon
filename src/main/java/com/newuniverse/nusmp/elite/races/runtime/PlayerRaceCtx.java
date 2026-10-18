package com.newuniverse.nusmp.elite.races.runtime;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.races.EliteRacesConfig;
import com.newuniverse.nusmp.elite.races.rule.RaceCtx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Why: the real {@link RaceCtx} a rule reads: EP from Tensura (through the existing defensive {@code TensuraBridge}), the kill counters of {@link RaceLedger}, skill mastery from
 * ManasCore, the carried items, and the records system through {@code EliteHooks.recordUnlocked} (false until that system is built). For anything that is not a server player it
 * is an empty context (every rule at 0), so a mob that somehow holds an elite race can never satisfy an evolution.
 */
public final class PlayerRaceCtx implements RaceCtx {
    private final ServerPlayer p;

    private PlayerRaceCtx(ServerPlayer p) { this.p = p; }

    public static RaceCtx of(LivingEntity e) { return e instanceof ServerPlayer sp ? new PlayerRaceCtx(sp) : EMPTY; }

    private static final RaceCtx EMPTY = new RaceCtx() {
        @Override public double ep() { return 0; }

        @Override public int kills(String key) { return 0; }

        @Override public double mastery(String skillId) { return 0; }

        @Override public int itemCount(String itemId) { return 0; }

        @Override public boolean record(String recordId) { return false; }

        @Override public double epScale() { return EliteRacesConfig.epGateScale(); }
    };

    @Override public double ep() { return TensuraBridge.maxEp(p); }

    @Override public int kills(String key) { return RaceLedger.kills(p, key); }

    @Override public double mastery(String skillId) { return RaceSkills.mastery(p, skillId); }

    @Override public int itemCount(String itemId) { return count(p, itemId); }

    @Override public boolean record(String recordId) {
        try { return EliteHooks.recordUnlocked.test(p.getUUID(), recordId); } catch (RuntimeException e) { return false; }
    }

    @Override public boolean consume(String itemId, int count) { return take(p, itemId, count); }

    @Override public double epScale() { return EliteRacesConfig.epGateScale(); }

    // ---------------------------------------------------------------- inventory helpers (shared with the transformation context)
    static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return null;
        return BuiltInRegistries.ITEM.get(rl);
    }

    public static boolean itemExists(String id) { return item(id) != null; }

    private static List<List<ItemStack>> lists(Inventory inv) { return List.of(inv.items, inv.offhand); }

    public static int count(ServerPlayer p, String itemId) {
        Item it = item(itemId);
        if (it == null) return 0;
        int n = 0;
        for (List<ItemStack> l : lists(p.getInventory())) for (ItemStack s : l) if (!s.isEmpty() && s.is(it)) n += s.getCount();
        return n;
    }

    /** Takes {@code count} of an item (all or nothing); true when taken. */
    public static boolean take(ServerPlayer p, String itemId, int count) {
        Item it = item(itemId);
        if (it == null || count <= 0 || count(p, itemId) < count) return false;
        int left = count;
        for (List<ItemStack> l : lists(p.getInventory())) {
            for (ItemStack s : l) {
                if (left <= 0) break;
                if (s.isEmpty() || !s.is(it)) continue;
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
        return left <= 0;
    }
}
