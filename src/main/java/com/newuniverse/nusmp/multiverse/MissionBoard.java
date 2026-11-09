package com.newuniverse.nusmp.multiverse;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.newuniverse.nusmp.multiverse.MissionBoardRules.Kind;

/**
 * Why: the Magic Knights take missions and are paid in stars by how hard the task was. The Mission Board (/multiverse mission) offers three missions a day, scaled to the knight's rank; one is taken at a time,
 * the player's own kills count by themselves, and finishing pays gold stars through {@link MultiverseProfile#addStars} (so ranks, squad score and the Wizard King follow). The state is one small record in the
 * player's profile (kind, need, have, reward, deadline, the day and how many were finished that day). Cost: one event per creature a player kills (a null check and a tag test), no tick work, no packet of its own.
 */
public final class MissionBoard {
    private MissionBoard() {}

    private static final String SEC = "Board";

    public record Offer(Kind kind, int need, int reward) {}

    private static long now(ServerPlayer p) { return p.serverLevel().getGameTime(); }

    /** The three offers of today for this player. */
    public static List<Offer> offers(ServerPlayer p) {
        int step = MultiverseProfile.step(p), pct = MultiverseConfig.get(MultiverseConfig.BOARD_REWARD_PERCENT);
        long seed = MissionBoardRules.seed(p.getUUID().getMostSignificantBits(), p.getUUID().getLeastSignificantBits(), MissionBoardRules.day(now(p)));
        List<Offer> out = new ArrayList<>();
        for (Kind k : MissionBoardRules.offerKinds(seed)) {
            int need = MissionBoardRules.need(k, step);
            out.add(new Offer(k, need, MissionBoardRules.reward(k, need, pct)));
        }
        return out;
    }

    private static boolean active(CompoundTag t, long now) { return t.contains("Kind") && MissionBoardRules.open(now, t.getLong("Until")); }

    private static int doneToday(CompoundTag t, long now) { return t.getInt("Day") == MissionBoardRules.day(now) ? t.getInt("Done") : 0; }

    public static List<Component> list(ServerPlayer p) {
        List<Component> out = new ArrayList<>();
        if (!MultiverseConfig.get(MultiverseConfig.BOARD_ENABLED)) { out.add(Component.literal("The Mission Board is closed on this server.").withStyle(ChatFormatting.RED)); return out; }
        long now = now(p);
        CompoundTag t = MultiverseProfile.section(p, SEC);
        out.add(Component.literal("Magic Knight Mission Board - " + MultiverseProfile.describeRank(p)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        int slot = 1;
        for (Offer o : offers(p)) out.add(Component.literal(" " + slot++ + ". " + o.kind.title + ": " + o.need + " kills, +" + o.reward + " gold star" + (o.reward == 1 ? "" : "s")).withStyle(ChatFormatting.WHITE));
        out.add(Component.literal(" Finished today: " + doneToday(t, now) + " of " + MultiverseConfig.get(MultiverseConfig.BOARD_PER_DAY) + ". Take one with /multiverse mission take <1-3>.").withStyle(ChatFormatting.GRAY));
        if (active(t, now)) out.addAll(status(p));
        return out;
    }

    public static List<Component> status(ServerPlayer p) {
        long now = now(p);
        CompoundTag t = MultiverseProfile.section(p, SEC);
        List<Component> out = new ArrayList<>();
        if (!active(t, now)) { out.add(Component.literal("You have no mission. See /multiverse mission.").withStyle(ChatFormatting.GRAY)); return out; }
        Kind k = kindOf(t.getString("Kind"));
        long left = Math.max(0L, (t.getLong("Until") - now) / 20L);
        out.add(Component.literal("Mission: " + (k == null ? "?" : k.title) + " - " + t.getInt("Have") + "/" + t.getInt("Need") + " (+" + t.getInt("Reward") + " gold stars, " + left / 60 + "m " + left % 60 + "s left)").withStyle(ChatFormatting.YELLOW));
        return out;
    }

    private static Kind kindOf(String name) { try { return Kind.valueOf(name); } catch (IllegalArgumentException e) { return null; } }

    /** Takes offer {@code slot} (1..3). Returns the message. */
    public static Component take(ServerPlayer p, int slot) {
        if (!MultiverseConfig.get(MultiverseConfig.BOARD_ENABLED)) return Component.literal("The Mission Board is closed on this server.").withStyle(ChatFormatting.RED);
        long now = now(p);
        CompoundTag t = MultiverseProfile.section(p, SEC);
        if (active(t, now)) return Component.literal("Finish or abandon your mission first (/multiverse mission status).").withStyle(ChatFormatting.RED);
        if (doneToday(t, now) >= MultiverseConfig.get(MultiverseConfig.BOARD_PER_DAY)) return Component.literal("You have finished enough missions for today. The board has new ones tomorrow.").withStyle(ChatFormatting.RED);
        List<Offer> o = offers(p);
        if (slot < 1 || slot > o.size()) return Component.literal("Choose 1 to " + o.size() + ".").withStyle(ChatFormatting.RED);
        Offer pick = o.get(slot - 1);
        CompoundTag n = new CompoundTag();
        n.putString("Kind", pick.kind.name());
        n.putInt("Need", pick.need);
        n.putInt("Have", 0);
        n.putInt("Reward", pick.reward);
        n.putLong("Until", now + 1200L * MultiverseConfig.get(MultiverseConfig.BOARD_MINUTES));
        n.putInt("Day", MissionBoardRules.day(now));
        n.putInt("Done", doneToday(t, now));
        MultiverseProfile.putSection(p, SEC, n);
        return Component.literal("Mission taken: " + pick.kind.title + " (" + pick.need + " kills). Your kills count by themselves.").withStyle(ChatFormatting.GOLD);
    }

    public static Component abandon(ServerPlayer p) {
        CompoundTag t = MultiverseProfile.section(p, SEC);
        if (!active(t, now(p))) return Component.literal("You have no mission.").withStyle(ChatFormatting.GRAY);
        CompoundTag n = new CompoundTag();
        n.putInt("Day", t.getInt("Day"));
        n.putInt("Done", t.getInt("Done"));
        MultiverseProfile.putSection(p, SEC, n);
        return Component.literal("Mission abandoned. No stars are lost.").withStyle(ChatFormatting.GRAY);
    }

    /** The kinds a creature counts for. */
    static EnumSet<Kind> kindsOf(LivingEntity e) {
        EnumSet<Kind> s = EnumSet.noneOf(Kind.class);
        if (e instanceof Player || (e instanceof TamableAnimal ta && ta.isTame())) return s;
        if (e instanceof Enemy) s.add(Kind.HOSTILE);
        if (e.getType().is(EntityTypeTags.UNDEAD)) s.add(Kind.UNDEAD);
        if (e.getType().is(EntityTypeTags.ARTHROPOD)) s.add(Kind.ARTHROPOD);
        if (e.getType().is(EntityTypeTags.RAIDERS)) s.add(Kind.RAIDER);
        return s;
    }

    /** One kill that counts for these kinds. Returns the stars paid when this finished the mission, else 0. */
    public static int credit(ServerPlayer p, EnumSet<Kind> kinds) {
        if (kinds.isEmpty()) return 0;
        long now = now(p);
        CompoundTag t = MultiverseProfile.section(p, SEC);
        if (!active(t, now)) return 0;
        Kind k = kindOf(t.getString("Kind"));
        if (k == null || !kinds.contains(k)) return 0;
        int have = t.getInt("Have") + 1, need = t.getInt("Need");
        if (have < need) {
            t.putInt("Have", have);
            MultiverseProfile.putSection(p, SEC, t);
            if (have % 5 == 0) p.displayClientMessage(Component.literal("Mission " + have + "/" + need).withStyle(ChatFormatting.YELLOW), true);
            return 0;
        }
        int reward = t.getInt("Reward");
        CompoundTag n = new CompoundTag();
        n.putInt("Day", MissionBoardRules.day(now));
        n.putInt("Done", doneToday(t, now) + 1);
        MultiverseProfile.putSection(p, SEC, n);
        int before = MultiverseProfile.goldStars(p);
        MultiverseProfile.addStars(p, reward);
        int got = MultiverseProfile.goldStars(p) - before;
        p.sendSystemMessage(Component.literal("Mission complete: " + k.title + "  +" + got + " gold star" + (got == 1 ? "" : "s")).withStyle(ChatFormatting.GOLD));
        com.newuniverse.nusmp.quests.NusmpEvents.fire(p, "board.mission", k.name().toLowerCase(java.util.Locale.ROOT));
        return got;
    }

    public static void onDeath(LivingDeathEvent e) {
        try {
            if (!(e.getSource().getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer || !MultiverseConfig.get(MultiverseConfig.BOARD_ENABLED)) return;
            credit(p, kindsOf(e.getEntity()));
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] mission board failed", t);
        }
    }
}
