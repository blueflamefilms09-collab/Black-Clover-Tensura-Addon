package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.elite.hub.QuestService;
import com.newuniverse.nusmp.mana.zone.ManaZoneGate.Progress;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import com.newuniverse.nusmp.multiverse.WizardKing;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The player-side data of the Mana Zone: what the gate reads (EP, rank, mana skin mastery, boss kills, depth) and what the technique keeps on the player
 * (attunement, cooldown, exhaustion). Stored beside the old mana data in the persisted player tag under its own key; nothing of the 1.5.0 data
 * ({@code nusmp_mana.ZoneUntil}) is touched, so old saves keep it and are simply refused until the gate is met.
 */
public final class ManaZonePlayer {
    private ManaZonePlayer() {}

    private static final String KEY = "nusmp_mana_zone";

    public static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    // ---------------------------------------------------------------- attunement (the ritual)
    public static double attuneSeconds(Player p) { return data(p).getDouble("Attune"); }

    public static void addAttune(Player p, double seconds) {
        if (seconds <= 0) return;
        CompoundTag d = data(p);
        d.putDouble("Attune", Math.min(1_000_000.0, d.getDouble("Attune") + seconds));
    }

    /** Admin: set the attunement (seconds); used by /multiverse mana zone attune. */
    public static void setAttune(Player p, double seconds) { data(p).putDouble("Attune", Math.max(0, seconds)); }

    // ---------------------------------------------------------------- cooldown and exhaustion
    public static long cooldownUntil(Player p) { return data(p).getLong("CooldownUntil"); }

    public static void setCooldownUntil(Player p, long tick) { data(p).putLong("CooldownUntil", tick); }

    public static long exhaustUntil(Player p) { return data(p).getLong("ExhaustUntil"); }

    public static void setExhaustUntil(Player p, long tick) { data(p).putLong("ExhaustUntil", tick); }

    public static boolean exhausted(Player p) { return p.level().getGameTime() < exhaustUntil(p); }

    /** True when this player's own flight flag is set (the Mana Zone gave them mayfly and must take it back). */
    public static boolean flightFlag(Player p) { return data(p).getBoolean("Flight"); }

    public static void setFlightFlag(Player p, boolean on) {
        if (on) data(p).putBoolean("Flight", true); else data(p).remove("Flight");
    }

    public static int casts(Player p) { return data(p).getInt("Casts"); }

    public static void countCast(Player p) { data(p).putInt("Casts", data(p).getInt("Casts") + 1); }

    // ---------------------------------------------------------------- what the gate reads
    /** The player's progress against the gate. Server thread. */
    public static Progress progress(ServerPlayer p) {
        double ep = 0;
        try { ep = EnergyHelper.getMaxEP(p); } catch (Throwable ignored) { /* no Tensura data (a stand-in player): no EP */ }
        double grimoire = 0, reinforcement = 0;
        try {
            var g = GrimoirePages.grimoireOf(p);
            if (g.isPresent() && g.get().getSkill() instanceof GrimoireBook b) grimoire = b.masteryFrac(g.get());
            var r = SkillAPI.getSkillsFrom(p).getSkill(NUSkills.BOOK_REINFORCEMENT.getId());
            if (r.isPresent() && r.get().getSkill() instanceof GrimoireBook rb) reinforcement = rb.masteryFrac(r.get());
        } catch (Throwable ignored) { /* no skills: no mastery */ }
        int kills = 0, floor = 0;
        try { kills = NusmpEvents.count(p, "boss.defeated", null); } catch (Throwable ignored) { /* the quest counters are optional */ }
        try { floor = QuestService.data(p).maxFloor(); } catch (Throwable ignored) { /* no dungeon data */ }
        boolean king = false;
        try { king = WizardKing.isKing(p); } catch (Throwable ignored) { /* no king data */ }
        return new Progress(ep, MultiverseProfile.step(p), grimoire, reinforcement, kills, floor, (int) attuneSeconds(p), king);
    }
}
