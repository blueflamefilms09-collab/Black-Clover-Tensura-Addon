package com.newuniverse.nusmp.elite.nations.world;

import com.mojang.authlib.GameProfile;
import com.newuniverse.nusmp.elite.nations.contract.Bounty;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.Permission;
import com.newuniverse.nusmp.elite.nations.core.Res;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Why: ONE entry point for everything a player can ask of the nations, used by the {@code /nation} commands AND by the screen's action packet, so the two can never disagree: {@code run(player, action, a, b, n)}
 * maps an action word and up to two strings and a number to the service call and answers a {@link Res}. The screen sends exactly these four values and nothing else; the server revalidates everything in the
 * services, so a modified client gains nothing. A small per-player rate limit stops a flood of packets. Server thread only.
 */
public final class NationActions {
    private NationActions() {}

    private static final Map<UUID, long[]> RATE = new HashMap<>();   // player to {tick, count}
    private static final int MAX_PER_TICK_WINDOW = 10;               // actions per 20 ticks

    public static void forget(UUID id) { RATE.remove(id); }

    public static void forgetAll() { RATE.clear(); }

    private static boolean allowed(ServerPlayer p) {
        long now = p.getServer().overworld().getGameTime();
        long[] r = RATE.computeIfAbsent(p.getUUID(), k -> new long[]{now, 0});
        if (now - r[0] >= 20) { r[0] = now; r[1] = 0; }
        return ++r[1] <= MAX_PER_TICK_WINDOW;
    }

    private static String s(String v) { return v == null ? "" : v.trim(); }

    public static Res run(ServerPlayer p, String action, String a, String b, long n) {
        if (p == null || action == null) return Res.fail("nusmp.nation.m.unknown_action");
        if (!allowed(p)) return Res.fail("nusmp.nation.m.too_fast");
        a = s(a);
        b = s(b);
        MinecraftServer srv = p.getServer();
        NationsData d = NationService.data(srv);
        Nation mine = d.nationOf(p.getUUID());
        String act = action.toLowerCase(Locale.ROOT);
        return switch (act) {
            case "found" -> NationService.found(p, a, (int) n);
            case "disband" -> NationService.disband(p);
            case "invite" -> {
                ServerPlayer t = srv.getPlayerList().getPlayerByName(a);
                yield t == null ? Res.fail("nusmp.nation.m.player_offline", a) : NationService.invite(p, t);
            }
            case "accept" -> NationService.accept(p, a);
            case "decline" -> NationService.decline(p, a);
            case "leave" -> NationService.leave(p);
            case "kick" -> {
                UUID t = NationService.memberByName(mine, a);
                yield t == null ? Res.fail("nusmp.nation.m.not_a_member") : NationService.kick(p, t);
            }
            case "setrole" -> {
                UUID t = NationService.memberByName(mine, a);
                yield t == null ? Res.fail("nusmp.nation.m.not_a_member") : NationService.setRole(p, t, b);
            }
            case "transfer" -> {
                UUID t = NationService.memberByName(mine, a);
                yield t == null ? Res.fail("nusmp.nation.m.not_a_member") : NationService.transferLeadership(p, t);
            }
            case "role_create" -> NationService.createRole(p, a, b);
            case "role_delete" -> NationService.deleteRole(p, a);
            case "role_perm" -> {
                Permission perm = Permission.parse(b);
                yield perm == null ? Res.fail("nusmp.nation.m.bad_permission", b) : NationService.setRolePermission(p, a, perm, n != 0);
            }
            case "rename" -> NationService.rename(p, a);
            case "color" -> NationService.setColor(p, (int) n);
            case "tax" -> Treasury.setTax(p, (int) n);
            case "deposit" -> Treasury.deposit(p, n, a.equalsIgnoreCase("ubs"));
            case "withdraw" -> Treasury.withdraw(p, n, a.equalsIgnoreCase("ubs"));
            case "take" -> ContractService.take(p, n);
            case "abandon" -> ContractService.abandon(p, n);
            case "deliver" -> ContractService.deliver(p, n);
            case "bounty_player", "bounty_player_nation" -> {
                GameProfile gp = profile(srv, a);
                yield gp == null ? Res.fail("nusmp.nation.m.player_unknown", a) : ContractService.post(p, Bounty.TargetKind.PLAYER, gp.getId().toString(), gp.getName(), n, act.endsWith("_nation"));
            }
            case "bounty_mob", "bounty_mob_nation" -> ContractService.post(p, Bounty.TargetKind.MOB, a, a, n, act.endsWith("_nation"));
            case "bounty_cancel" -> ContractService.cancel(p, n);
            case "war_declare" -> WarService.declare(p, a, b);
            case "war_ceasefire" -> WarService.ceasefire(p, a);
            case "war_decline" -> WarService.declineCeasefire(p, a);
            case "war_surrender" -> WarService.surrender(p, a);
            case "war_cancel" -> WarService.cancel(p, a);
            case "ally_propose" -> WarService.propose(p, a);
            case "ally_accept" -> WarService.acceptAlliance(p, a);
            case "ally_decline" -> WarService.declineAlliance(p, a);
            case "ally_break" -> WarService.breakAlliance(p, a);
            case "banner_item" -> bannerItem(p, mine);
            default -> Res.fail("nusmp.nation.m.unknown_action");
        };
    }

    private static Res bannerItem(ServerPlayer p, Nation mine) {
        if (mine == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!mine.leader.equals(p.getUUID())) return Res.fail("nusmp.nation.m.leader_only");
        if (mine.hasCapital) return Res.fail("nusmp.nation.m.has_capital");
        NationService.giveBannerItem(p);
        return Res.ok("nusmp.nation.m.banner_given");
    }

    /** An online player by name, else the server's profile cache (a player who was here before). */
    static GameProfile profile(MinecraftServer srv, String name) {
        ServerPlayer online = srv.getPlayerList().getPlayerByName(name);
        if (online != null) return online.getGameProfile();
        Optional<GameProfile> cached = srv.getProfileCache() == null ? Optional.empty() : srv.getProfileCache().get(name);
        return cached.orElse(null);
    }
}
