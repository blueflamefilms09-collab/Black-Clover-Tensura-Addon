package com.newuniverse.nusmp.labyrinth.mega.party;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.loot.LootDrops;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the owner's rule (5.0.0): in a party the loot is shared EVENLY, not owned by whoever landed the last blow. When a player of an FTB team kills a monster in the dungeon, everything it
 * would drop (the mod's table and anything else added to the drops) is taken away from the ground and divided between the team members who are alive, in the same dimension and within
 * {@code party.shareRadius} of the kill: every stack is cut into equal parts ({@link #split}), the rest goes to the members after a cursor that rotates (per team), so over many kills nobody
 * is ahead by more than one item; one-item stacks (gear) go round the members in turn. Each part goes straight into the member's inventory (what does not fit drops at their feet), and every
 * member reads ONE chat line with the whole split. The popup to pick items by hand is not built: the even split is the rule. Cost per kill: at most 8 members and 64 stacks are looked at, one
 * chat line per member, no tick work, no world scan (the members come from the dimension's player list). A fault leaves the drops where they were. Server side only.
 */
public final class PartyLoot {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    /** Most members who share one kill, and most stacks one kill shares (the rest of a huge drop stays on the ground). */
    static final int MAX_MEMBERS = 8, MAX_STACKS = 64;

    private static final Map<UUID, Integer> CURSOR = new HashMap<>();
    private static boolean warned;

    private PartyLoot() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.LOWEST, PartyLoot::onDrops);                        // after every other handler has added its drops
        gameBus.addListener((ServerStoppedEvent e) -> CURSOR.clear());
    }

    /**
     * How a stack of {@code count} is divided among {@code n} members: everybody gets {@code count / n}, and the {@code count % n} left over go to the members from {@code start} on
     * (wrapping). The parts always add up to {@code count} and differ by at most one. Pure.
     */
    public static int[] split(int count, int n, int start) {
        int[] out = new int[Math.max(1, n)];
        int base = count / out.length, rest = count % out.length, first = Math.floorMod(start, out.length);
        for (int i = 0; i < out.length; i++) out[i] = base;
        for (int k = 0; k < rest; k++) out[(first + k) % out.length]++;
        return out;
    }

    /** The cursor after a stack of {@code count} was split among {@code n}: moved on by the remainder, so the next remainder starts after the last one that got an extra. Pure. */
    public static int advance(int cursor, int count, int n) { return Math.floorMod(cursor + count % Math.max(1, n), Math.max(1, n)); }

    private static void onDrops(LivingDropsEvent e) {
        try {
            if (!PartyConfig.get(PartyConfig.SHARE_LOOT) || e.isCanceled() || e.getDrops().isEmpty()) return;
            LivingEntity mob = e.getEntity();
            if (mob instanceof Player || !(mob.level() instanceof ServerLevel level) || !LootDrops.dungeon(level)) return;
            if (!(e.getSource().getEntity() instanceof ServerPlayer killer)) return;
            List<ServerPlayer> crew = crew(killer, mob);
            if (crew.size() < 2) return;
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemEntity ie : e.getDrops()) {
                if (stacks.size() >= MAX_STACKS) return;                                       // too much to share: leave it all on the ground, unchanged
                ItemStack s = ie.getItem();
                if (!s.isEmpty()) stacks.add(s.copy());
            }
            if (stacks.isEmpty()) return;
            e.setCanceled(true);                                                               // the ground gets nothing: everything is given below
            share(killer, crew, stacks);
        } catch (RuntimeException ex) {
            if (!warned) {
                warned = true;
                LOG.warn("[anime_multiverse] The party loot split failed; the drops were left alone (further failures are not logged): {}", ex.toString());
            }
        }
    }

    /** The killer first, then the other members of their team who are alive, not spectating, in this dimension and within the radius of the kill; at most {@link #MAX_MEMBERS}. */
    private static List<ServerPlayer> crew(ServerPlayer killer, LivingEntity mob) {
        double r = PartyConfig.get(PartyConfig.SHARE_RADIUS), r2 = r * r;
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer q : PartyService.questMembers(killer)) {
            if (out.size() >= MAX_MEMBERS) break;
            if (!q.isAlive() || q.isSpectator() || q.level() != mob.level() || q.distanceToSqr(mob) > r2) continue;
            if (!out.contains(q)) out.add(q);
        }
        if (!out.contains(killer)) out.add(0, killer);
        return out;
    }

    private static void share(ServerPlayer killer, List<ServerPlayer> crew, List<ItemStack> stacks) {
        int n = crew.size();
        UUID team = PartyResolver.keyOf(killer).id();
        int cursor = Math.floorMod(CURSOR.getOrDefault(team, 0), n);
        if (CURSOR.size() > 256) CURSOR.clear();
        List<List<String>> got = new ArrayList<>();
        for (int i = 0; i < n; i++) got.add(new ArrayList<>());
        for (ItemStack s : stacks) {
            int[] parts = split(s.getCount(), n, cursor);
            for (int i = 0; i < n; i++) {
                if (parts[i] <= 0) continue;
                ItemStack part = s.copyWithCount(parts[i]);
                got.get(i).add(parts[i] + "x " + part.getHoverName().getString());
                give(crew.get(i), part);
            }
            cursor = advance(cursor, s.getCount(), n);
        }
        CURSOR.put(team, cursor);
        StringBuilder line = new StringBuilder("Loot shared evenly: ");
        for (int i = 0; i < n; i++) {
            if (i > 0) line.append("; ");
            line.append(crew.get(i).getGameProfile().getName()).append(" ").append(got.get(i).isEmpty() ? "nothing this time" : String.join(", ", got.get(i)));
        }
        Component msg = Component.literal(line.toString()).withStyle(net.minecraft.ChatFormatting.GOLD);
        for (ServerPlayer q : crew) q.sendSystemMessage(msg);
    }

    private static void give(ServerPlayer to, ItemStack part) {
        try {
            ItemHandlerHelper.giveItemToPlayer(to, part);                                      // into the inventory; what does not fit drops at their feet
        } catch (RuntimeException ex) {
            to.drop(part, false);
        }
    }
}
