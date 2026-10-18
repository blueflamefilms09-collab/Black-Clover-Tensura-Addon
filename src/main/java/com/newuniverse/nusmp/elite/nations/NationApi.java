package com.newuniverse.nusmp.elite.nations;

import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.Permission;
import com.newuniverse.nusmp.elite.nations.econ.InterestPolicy;
import com.newuniverse.nusmp.elite.nations.econ.TreasuryBridge;
import com.newuniverse.nusmp.elite.nations.rep.ReputationTier;
import com.newuniverse.nusmp.elite.nations.war.War;
import com.newuniverse.nusmp.elite.nations.world.NationPerks;
import com.newuniverse.nusmp.elite.nations.world.NationService;
import com.newuniverse.nusmp.elite.nations.world.NationsConfig;
import com.newuniverse.nusmp.elite.nations.world.NationsData;
import com.newuniverse.nusmp.elite.nations.world.ReputationService;
import com.newuniverse.nusmp.elite.nations.world.Treasury;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * THE PUBLIC FACADE of the Nations & War system, for the builders and systems that come after it (the Walpurgis council, the seasons and chronicles, shops, quests). Static methods, server side only,
 * with PLAIN types in the signatures (UUIDs, strings, numbers) so a caller needs no Minecraft class and no knowledge of the internals. Every method is NULL-SAFE and cheap: before the server has started,
 * with the master switch off, or for an unknown id it answers the neutral value ("" / 0 / false / empty set) and never throws. A nation is identified by its stable id (a slug of its name at founding,
 * never changed by a rename); a player by UUID. Money is in copper coins, the unit of the {@code EliteHooks.wallet} hook.
 *
 * <p>Events (NeoForge GAME bus, package {@code com.newuniverse.nusmp.elite.nations.event}, all plain values): {@code NationCreatedEvent}, {@code NationDisbandedEvent}, {@code NationLevelUpEvent},
 * {@code NationMemberJoinedEvent}, {@code NationMemberLeftEvent}, {@code TreasuryChangedEvent}, {@code WarDeclaredEvent}, {@code WarPhaseChangedEvent}, {@code WarEndedEvent}, {@code AllianceFormedEvent},
 * {@code AllianceBrokenEvent}, {@code ContractCompletedEvent}, {@code BountyClaimedEvent}, {@code ReputationChangedEvent}.
 *
 * <p>Semantics worth knowing: {@link #isAtWar} is true from the declaration until the fighting ends (phases declared, preparation, active); {@link #isFighting} only in the ACTIVE phase;
 * after a war a short ceasefire window follows in which neither is true. Reputation keys may be any faction id, not only nations.
 */
public final class NationApi {
    private NationApi() {}

    private static NationsData data() {
        if (!NationsConfig.enabled()) return null;
        return NationService.dataOrNull();
    }

    private static Nation nation(String id) {
        NationsData d = data();
        return d == null || id == null ? null : d.nations.get(id);
    }

    // ---------------------------------------------------------------- identity
    /** The id of the nation the player belongs to, or "" for none. */
    public static String nationOf(UUID player) {
        NationsData d = data();
        return d == null || player == null ? "" : d.nationIdOf(player);
    }

    public static boolean exists(String nationId) { return nation(nationId) != null; }

    /** The display name, or "" if unknown. */
    public static String nameOf(String nationId) {
        Nation n = nation(nationId);
        return n == null ? "" : n.name;
    }

    /** The banner colour as 0xRRGGBB, or 0. */
    public static int colorOf(String nationId) {
        Nation n = nation(nationId);
        return n == null ? 0 : n.color;
    }

    /** Every nation id, in founding order. */
    public static List<String> allNations() {
        NationsData d = data();
        return d == null ? List.of() : new ArrayList<>(d.nations.keySet());
    }

    /** The id of the nation with this display name (case-insensitive), or "". */
    public static String findByName(String name) {
        NationsData d = data();
        if (d == null || name == null) return "";
        for (Nation n : d.nations.values()) if (n.name.equalsIgnoreCase(name.trim()) || n.id.equalsIgnoreCase(name.trim())) return n.id;
        return "";
    }

    public static UUID leader(String nationId) {
        Nation n = nation(nationId);
        return n == null ? null : n.leader;
    }

    /** Every member, online or not (a copy). */
    public static Set<UUID> members(String nationId) {
        Nation n = nation(nationId);
        return n == null ? Set.of() : new LinkedHashSet<>(n.members.keySet());
    }

    public static boolean isMember(UUID player, String nationId) {
        String own = nationOf(player);
        return !own.isEmpty() && own.equals(nationId);
    }

    /** Both belong to the same nation. */
    public static boolean sameNation(UUID a, UUID b) {
        String na = nationOf(a);
        return !na.isEmpty() && na.equals(nationOf(b));
    }

    /** Does the player hold the permission (invite, kick, treasury, war, contracts, build, diplomacy, roles) in their nation? The leader holds all. */
    public static boolean hasPermission(UUID player, String permission) {
        NationsData d = data();
        Permission p = Permission.parse(permission);
        if (d == null || p == null || player == null) return false;
        Nation n = d.nationOf(player);
        return n != null && n.can(player, p);
    }

    // ---------------------------------------------------------------- level and perks
    public static int level(String nationId) {
        Nation n = nation(nationId);
        return n == null ? 0 : n.level;
    }

    public static long xp(String nationId) {
        Nation n = nation(nationId);
        return n == null ? 0L : n.xp;
    }

    /** The score the level is computed from (members, treasury, contracts, area, wars, XP). */
    public static double score(String nationId) {
        Nation n = nation(nationId);
        return n == null ? 0.0 : NationService.score(n);
    }

    /** Adds (or removes, if negative) nation XP and re-evaluates the level; fires NationLevelUpEvent when it passes its highest level. */
    public static void addNationXp(String nationId, long xp, String reason) {
        NationsData d = data();
        MinecraftServer s = NationService.server();
        Nation n = d == null || nationId == null ? null : d.nations.get(nationId);
        if (n != null && s != null) NationService.addXp(s, n, xp, reason == null ? "" : reason);
    }

    /** The sum of a perk type ("shop_discount", "treasury_cap", "member_cap", "guard_count", "contract_bonus", "board_slots", "attribute") for the nation's level. */
    public static double perk(String nationId, String perkType) {
        Nation n = nation(nationId);
        return n == null || perkType == null ? 0.0 : NationPerks.table().sum(perkType.toLowerCase(Locale.ROOT), n.level);
    }

    /** Percent of the normal price a shop should charge a member of this nation's own perk discount: 100 = no discount. A player without a nation pays 100. */
    public static int priceModifierPercent(UUID player) {
        Nation n = nation(nationOf(player));
        return n == null ? 100 : 100 - NationPerks.shopDiscountPercent(n);
    }

    /**
     * Percent of the normal price a shop owned by {@code shopNationId} charges this player: the reputation tier of the player with that nation (hated 160 ... revered 80) combined with the perk discount if
     * the player is a member. 100 = unchanged.
     */
    public static int priceModifierPercent(UUID player, String shopNationId) {
        NationsData d = data();
        Nation shop = nation(shopNationId);
        if (d == null || shop == null || player == null) return 100;
        double pct = ReputationTier.of(d.reputation.get(player, shopNationId)).shopPercent;
        if (shop.isMember(player)) pct = pct * (100 - NationPerks.shopDiscountPercent(shop)) / 100.0;
        return (int) Math.round(pct);
    }

    // ---------------------------------------------------------------- capital
    public static boolean hasCapital(String nationId) {
        Nation n = nation(nationId);
        return n != null && n.hasCapital;
    }

    /** The dimension id of the capital ("minecraft:overworld"), or "". */
    public static String capitalDimension(String nationId) {
        Nation n = nation(nationId);
        return n == null || !n.hasCapital ? "" : n.capitalDim;
    }

    /** {x, y, z} of the capital banner, or an empty array. */
    public static int[] capitalPos(String nationId) {
        Nation n = nation(nationId);
        return n == null || !n.hasCapital ? new int[0] : new int[]{n.cx, n.cy, n.cz};
    }

    // ---------------------------------------------------------------- treasury
    /** Copper coins in the treasury. */
    public static long treasury(String nationId) { return Treasury.BRIDGE.balance(nationId); }

    public static long treasuryCap(String nationId) { return Treasury.BRIDGE.cap(nationId); }

    /** Pays coins in. Returns what was accepted (the treasury cap may refuse part). */
    public static long deposit(String nationId, long coins, String reason) { return Treasury.BRIDGE.deposit(nationId, coins, reason); }

    /** Takes coins out; false when the balance is too small. */
    public static boolean withdraw(String nationId, long coins, String reason) { return Treasury.BRIDGE.withdraw(nationId, coins, reason); }

    /** The bridge object other systems may hold on to (same four operations). */
    public static TreasuryBridge treasuryBridge() { return Treasury.BRIDGE; }

    /** Replaces the interest calculation of every treasury (null restores the configured one). Used by banking integrations. */
    public static void setInterestPolicy(InterestPolicy policy) { Treasury.setCustomInterest(policy); }

    // ---------------------------------------------------------------- war and diplomacy
    /** Hostile relations: a war between the two is declared, preparing or fighting. */
    public static boolean isAtWar(String a, String b) {
        NationsData d = data();
        return d != null && a != null && b != null && !a.equals(b) && d.wars.hostileBetween(a, b) != null;
    }

    /** Fighting is allowed right now: the war between the two is in its ACTIVE phase. */
    public static boolean isFighting(String a, String b) {
        NationsData d = data();
        return d != null && a != null && b != null && !a.equals(b) && d.wars.fightingBetween(a, b) != null;
    }

    public static boolean isAllied(String a, String b) {
        NationsData d = data();
        return d != null && d.alliances.areAllied(a, b);
    }

    public static Set<String> alliesOf(String nationId) {
        NationsData d = data();
        return d == null || nationId == null ? Set.of() : Collections.unmodifiableSet(d.alliances.alliesOf(nationId));
    }

    /** Nations this one is at war with (hostile phases). */
    public static Set<String> enemiesOf(String nationId) {
        NationsData d = data();
        if (d == null || nationId == null) return Set.of();
        Set<String> out = new LinkedHashSet<>();
        for (War w : d.wars.warsOf(nationId)) if (w.hostile()) out.add(w.other(nationId));
        return out;
    }

    /** The war score of {@code nationId} in its current war against {@code other}, or 0. */
    public static int warScore(String nationId, String other) {
        NationsData d = data();
        War w = d == null || nationId == null || other == null ? null : d.wars.find(nationId, other);
        return w == null ? 0 : w.score(nationId);
    }

    /** Wars won / lost by a nation, all time. {wins, losses}. */
    public static int[] warRecord(String nationId) {
        Nation n = nation(nationId);
        return n == null ? new int[]{0, 0} : new int[]{n.warWins, n.warLosses};
    }

    // ---------------------------------------------------------------- reputation
    /** Reputation of a player with a nation (or any faction id): -1000..1000. */
    public static int reputation(UUID player, String nationId) {
        NationsData d = data();
        return d == null || player == null || nationId == null ? 0 : d.reputation.get(player, nationId);
    }

    /** The tier id: hated, hostile, unfriendly, neutral, friendly, honored, revered. */
    public static String reputationTier(UUID player, String nationId) { return ReputationTier.of(reputation(player, nationId)).id(); }

    /** Adds (negative: removes) reputation, clamped; fires ReputationChangedEvent. */
    public static void addReputation(UUID player, String nationId, int delta, String reason) {
        MinecraftServer s = NationService.server();
        if (s != null && data() != null && player != null && nationId != null) ReputationService.add(s, player, nationId, delta, reason);
    }
}
