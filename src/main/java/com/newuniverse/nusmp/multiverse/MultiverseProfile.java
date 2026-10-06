package com.newuniverse.nusmp.multiverse;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * A player's Magic Knight record: rank and class, gold / black stars, social class, race, ceremony state, anti-magic mode.
 * Stored in the player's persisted NBT (survives death, like the rest of this mod's player data). Every write copies the
 * compound, changes the copy and puts it back (copy-on-write), then marks the player for a status sync.
 *
 * <p>Defaults: everyone starts as a 5th Class Junior Magic Knight, a Commoner, Human, eligible for the ceremony.
 */
public final class MultiverseProfile {
    private static final String KEY = "nusmp_multiverse";

    private MultiverseProfile() {}

    private static CompoundTag read(Player p) {
        CompoundTag root = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return root.getCompound(KEY).copy();
    }

    private static void write(Player p, CompoundTag profile) {
        CompoundTag root = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy();
        root.put(KEY, profile);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, root);
        if (p instanceof ServerPlayer sp) MultiverseSync.markDirty(sp);
    }

    // ---------------------------------------------------------------- rank
    public static KnightRank rank(Player p) {
        KnightRank r = KnightRank.byName(read(p).getString("Rank"));
        return r == null ? KnightRank.JUNIOR : r;
    }

    public static int rankClass(Player p) {
        CompoundTag t = read(p);
        return t.contains("Class") ? Math.max(1, Math.min(5, t.getInt("Class"))) : 5;
    }

    public static int step(Player p) { return KnightRank.step(rank(p), rankClass(p)); }

    public static String describeRank(Player p) { return KnightRank.describe(rank(p), rankClass(p)); }

    public static void setRank(ServerPlayer p, KnightRank rank, int cls) {
        CompoundTag t = read(p);
        int before = KnightRank.step(rank(p), rankClass(p));
        t.putString("Rank", rank.name());
        t.putInt("Class", rank.hasClasses ? Math.max(1, Math.min(5, cls)) : 0);
        write(p, t);
        int after = KnightRank.step(rank, cls);
        if (after != before) {
            p.sendSystemMessage(Component.literal((after > before ? "Promoted: " : "Rank changed: ") + KnightRank.describe(rank, cls))
                    .withStyle(after > before ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        }
    }

    // ---------------------------------------------------------------- stars
    public static int goldStars(Player p) { return read(p).getInt("Gold"); }
    public static int blackStars(Player p) { return read(p).getInt("Black"); }
    public static int netStars(Player p) { return goldStars(p) - blackStars(p); }

    /** Positive = gold stars (merit), negative = black stars (demerit). Auto-promotes when enabled. */
    public static void addStars(ServerPlayer p, int amount) {
        if (amount == 0) return;
        CompoundTag t = read(p);
        if (amount > 0) t.putInt("Gold", t.getInt("Gold") + amount);
        else t.putInt("Black", t.getInt("Black") - amount);
        write(p, t);
        p.displayClientMessage(Component.literal(amount > 0 ? "+" + amount + " gold star" + (amount == 1 ? "" : "s") : -amount + " black star" + (amount == -1 ? "" : "s"))
                .withStyle(amount > 0 ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY), true);
        Squads.onStarsChanged(p);
        if (amount > 0) MasteryPages.onMissionStars(p, amount);
        autoPromote(p);
    }

    /** Junior 5th -> ... -> Senior 1st, one class per N net stars. Never demotes and never reaches Grand / Wizard King on its own. */
    public static void autoPromote(ServerPlayer p) {
        if (!MultiverseConfig.get(MultiverseConfig.AUTO_PROMOTE)) return;
        int per = Math.max(1, MultiverseConfig.get(MultiverseConfig.STARS_PER_CLASS));
        int earned = Math.min(14, Math.max(0, netStars(p)) / per);
        int now = step(p);
        if (now < 15 && earned > now) setRank(p, KnightRank.rankOfStep(earned), KnightRank.classOfStep(earned));
    }

    // ---------------------------------------------------------------- class & race
    public static SocialClass socialClass(Player p) {
        SocialClass c = SocialClass.byName(read(p).getString("Social"));
        return c == null ? SocialClass.COMMONER : c;
    }

    public static void setSocialClass(ServerPlayer p, SocialClass c) {
        CompoundTag t = read(p);
        t.putString("Social", c.name());
        write(p, t);
    }

    public static Race race(Player p) {
        Race r = Race.byName(read(p).getString("Race"));
        return r == null ? Race.HUMAN : r;
    }

    public static void setRace(ServerPlayer p, Race r) {
        CompoundTag t = read(p);
        t.putString("Race", r.name());
        write(p, t);
    }

    // ---------------------------------------------------------------- ceremony
    /** Admin flag; with everyoneEligible (default) every player without a grimoire counts as eligible. */
    public static boolean eligibleFlag(Player p) {
        CompoundTag t = read(p);
        return !t.contains("Eligible") || t.getBoolean("Eligible");
    }

    public static void setEligible(ServerPlayer p, boolean eligible) {
        CompoundTag t = read(p);
        t.putBoolean("Eligible", eligible);
        write(p, t);
    }

    public static boolean accepted(Player p) { return read(p).getBoolean("Accepted"); }

    public static void setAccepted(ServerPlayer p, boolean accepted, String where) {
        CompoundTag t = read(p);
        t.putBoolean("Accepted", accepted);
        if (accepted) t.putString("AcceptedAt", where);
        else t.remove("AcceptedAt");
        write(p, t);
    }

    public static String acceptedAt(Player p) { return read(p).getString("AcceptedAt"); }

    // ---------------------------------------------------------------- anti-magic
    public static int antiMode(Player p) { return read(p).getInt("AntiMode"); }

    public static void setAntiMode(ServerPlayer p, int mode) {
        CompoundTag t = read(p);
        t.putInt("AntiMode", Math.max(0, Math.min(3, mode)));
        write(p, t);
    }

    public static final String[] ANTI_MODES = {"Dormant", "Black Arm", "Black Form", "Devil Union"};
}
