package com.newuniverse.nusmp.labyrinth.mega.system;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Why: this is where a completed quest PAYS, and the brief's two prizes ("a permanent stat boost capability OR a Shadow Core item") need the rest of the
 * server around them to behave. The roll itself is the pure {@link RewardRoller} (seeded, weighted {@code statWeight} against {@code shadowCoreWeight}); this
 * class applies the result: a stat point is added to the saved {@link SystemState} up to the per-stat cap and made real at once as an attribute modifier
 * ({@link SystemStats}), a Shadow Core first recharges one the player already holds (config {@code rechargeFirst}, default on: a player should not collect
 * cores they cannot use) and only otherwise becomes a new item in the inventory, dropped at the player's feet if the inventory is full. Each outcome pops up on
 * the player's System window. Nothing here can pay twice: it is called once, from the single completion path of {@link SystemQuests}.
 */
public final class SystemRewards {
    private SystemRewards() {}

    /** Rolls and pays the reward for a quest that was just completed. Returns what was rolled (for the self-test and the command). */
    public static RewardRoller.Reward grant(ServerPlayer p, SystemState s, QuestProgress q) {
        QuestTemplate t = SystemData.quests().get(q.id());
        List<Stat> stats = t == null ? q.kind().defaultStats() : t.stats();
        int points = t == null ? 1 : t.points();
        boolean coreAllowed = t == null || t.shadowCore();
        int cap = SystemConfig.get(SystemConfig.MAX_STAT_POINTS);
        long seed = RewardRoller.seed(p.getUUID(), q.floor(), q.id(), s.done());
        RewardRoller.Reward r;
        if (SystemConfig.pointMode() == PointMode.CHOOSE) {
            // the player decides: a stat reward is paid into the free pool, as long as the stats still have room under their caps
            r = RewardRoller.rollChoose(seed, SystemConfig.get(SystemConfig.STAT_WEIGHT), SystemConfig.get(SystemConfig.SHADOW_CORE_WEIGHT), points, coreAllowed,
                    PointRules.room(s.pointsCopy(), s.free(), cap));
        } else {
            r = RewardRoller.roll(seed, SystemConfig.get(SystemConfig.STAT_WEIGHT), SystemConfig.get(SystemConfig.SHADOW_CORE_WEIGHT), stats, points, coreAllowed,
                    s.pointsCopy(), cap);
        }
        switch (r.kind()) {
            case STAT -> giveStat(p, s, r.stat(), r.points(), cap);
            case FREE -> giveFree(p, s, r.points());
            case CORE -> giveCore(p, s);
            case NONE -> { /* every stat the quest pays is at its limit and the quest pays no core */ }
        }
        return r;
    }

    /** Adds points of a stat (up to the cap), makes them real as attribute modifiers and tells the player. Returns the points actually added. */
    public static int giveStat(ServerPlayer p, SystemState s, Stat stat, int points, int cap) {
        int added = s.addPoints(stat, points, cap);
        SystemStats.apply(p, s);
        if (added > 0) SystemSync.toast(p, ToastKind.REWARD, SystemText.TOAST_REWARD_STAT, Integer.toString(added), stat.name());
        SystemSync.request(p);
        return added;
    }

    /** Pays points into the free pool (config {@code system.pointMode} = choose), tells the player and syncs. Returns the points actually added. */
    public static int giveFree(ServerPlayer p, SystemState s, int points) {
        int added = s.addFree(points);
        if (added > 0) SystemSync.toast(p, ToastKind.REWARD, PointsText.TOAST_FREE, Integer.toString(added));
        SystemSync.request(p);
        return added;
    }

    /** Pays a Shadow Core: one more charge in a core the player holds (when {@code rechargeFirst}), else a new core. Returns true when it was a new item. */
    public static boolean giveCore(ServerPlayer p, SystemState s) {
        if (SystemConfig.get(SystemConfig.RECHARGE_FIRST)) {
            ItemStack held = heldCoreWithRoom(p);
            if (held != null) {
                ShadowCoreItem.setCharges(held, CoreCharges.recharge(ShadowCoreItem.charges(held)));
                SystemSync.toast(p, ToastKind.REWARD, SystemText.TOAST_REWARD_RECHARGE);
                return false;
            }
        }
        ItemStack core = ShadowCoreItem.create();
        if (!p.getInventory().add(core)) p.drop(core, false);
        s.addCore();
        SystemSync.toast(p, ToastKind.REWARD, SystemText.TOAST_REWARD_CORE);
        return true;
    }

    /** The first Shadow Core in the player's inventory (or off hand) that has a charge missing, or null. */
    static ItemStack heldCoreWithRoom(ServerPlayer p) {
        for (ItemStack st : p.getInventory().items) if (ShadowCoreItem.is(st) && CoreCharges.canRecharge(ShadowCoreItem.charges(st))) return st;
        ItemStack off = p.getOffhandItem();
        return ShadowCoreItem.is(off) && CoreCharges.canRecharge(ShadowCoreItem.charges(off)) ? off : null;
    }
}
