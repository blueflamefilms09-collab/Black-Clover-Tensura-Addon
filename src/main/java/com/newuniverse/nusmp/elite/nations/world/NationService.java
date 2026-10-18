package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.nations.core.GuardRules;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.NationLevels;
import com.newuniverse.nusmp.elite.nations.core.Permission;
import com.newuniverse.nusmp.elite.nations.core.Res;
import com.newuniverse.nusmp.elite.nations.core.Role;
import com.newuniverse.nusmp.elite.nations.core.Roles;
import com.newuniverse.nusmp.elite.nations.econ.Ledger;
import com.newuniverse.nusmp.elite.nations.event.NationCreatedEvent;
import com.newuniverse.nusmp.elite.nations.event.NationDisbandedEvent;
import com.newuniverse.nusmp.elite.nations.event.NationLevelUpEvent;
import com.newuniverse.nusmp.elite.nations.event.NationMemberJoinedEvent;
import com.newuniverse.nusmp.elite.nations.event.NationMemberLeftEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Why: the core server logic of the nations ("Nation Management: creation (founding), leveling, metrics, role permissions (custom roles), sync mechanisms"): found, disband, invite/join/leave/kick, roles,
 * settings, the capital, nation XP and the level. Every method checks the permission it needs and answers a {@link Res} (success or the reason it was refused), so the command tree, the screen and the API
 * all share one code path and one set of rules. It never trusts a client: the caller passes the acting player. Server thread only.
 */
public final class NationService {
    private NationService() {}

    // ---------------------------------------------------------------- access
    public static MinecraftServer server() { return ServerLifecycleHooks.getCurrentServer(); }

    public static NationsData data(MinecraftServer s) { return NationsData.get(s); }

    public static long now(MinecraftServer s) { return s.overworld().getGameTime(); }

    /** The data of the running server, or null before it started / after it stopped. */
    public static NationsData dataOrNull() {
        MinecraftServer s = server();
        return s == null ? null : data(s);
    }

    private static Nation own(NationsData d, ServerPlayer p) { return d.nationOf(p.getUUID()); }

    // ---------------------------------------------------------------- level
    public static NationLevels.Weights weights() {
        return new NationLevels.Weights(NationsConfig.get(NationsConfig.W_MEMBER), NationsConfig.get(NationsConfig.COINS_PER_TREASURY_POINT), NationsConfig.get(NationsConfig.W_TREASURY_CAP),
                NationsConfig.get(NationsConfig.W_CONTRACT), NationsConfig.get(NationsConfig.W_AREA), NationsConfig.get(NationsConfig.W_WARWIN));
    }

    public static long[] thresholds() {
        return NationLevels.thresholds(NationsConfig.get(NationsConfig.MAX_LEVEL), NationsConfig.get(NationsConfig.THRESHOLD_BASE), NationsConfig.get(NationsConfig.THRESHOLD_GROWTH));
    }

    public static double score(Nation n) { return NationLevels.score(weights(), n.metrics()); }

    /** Recomputes the level of a nation from its metrics; fires NationLevelUpEvent when it passes the highest level it ever announced. */
    public static void refresh(MinecraftServer s, Nation n) {
        int lvl = NationLevels.levelOf(score(n), thresholds());
        n.level = lvl;
        if (lvl > n.peakLevel) {
            int old = n.peakLevel;
            n.peakLevel = lvl;
            Msg.broadcast(s, n, "nusmp.nation.m.level_up", n.name, lvl);
            Treasury.post(new NationLevelUpEvent(n.id, old, lvl));
        }
        data(s).touch(n.id);
    }

    public static void addXp(MinecraftServer s, Nation n, long xp, String reason) {
        if (n == null || xp == 0) return;
        n.xp = Math.max(0L, n.xp + xp);
        refresh(s, n);
    }

    // ---------------------------------------------------------------- founding
    private static boolean unclaimedName(NationsData d, String name) {
        String slug = Nation.slug(name);
        for (Nation n : d.nations.values()) if (n.id.equals(slug) || n.name.equalsIgnoreCase(name.trim())) return false;
        return true;
    }

    public static Res found(ServerPlayer p, String rawName, int color) {
        if (!NationsConfig.enabled()) return Res.fail("nusmp.nation.m.disabled");
        MinecraftServer s = p.getServer();
        NationsData d = data(s);
        if (d.nationOf(p.getUUID()) != null) return Res.fail("nusmp.nation.m.already_in_nation");
        String name = rawName == null ? "" : rawName.trim();
        if (!Nation.validName(name, NationsConfig.get(NationsConfig.NAME_MIN), NationsConfig.get(NationsConfig.NAME_MAX))) return Res.fail("nusmp.nation.m.bad_name", NationsConfig.get(NationsConfig.NAME_MIN), NationsConfig.get(NationsConfig.NAME_MAX));
        if (!unclaimedName(d, name)) return Res.fail("nusmp.nation.m.name_taken", name);
        if (d.nations.size() >= NationsConfig.get(NationsConfig.MAX_NATIONS)) return Res.fail("nusmp.nation.m.too_many_nations");
        Res cost = chargeFoundingCost(p);
        if (!cost.ok()) return cost;
        createNation(s, p.getUUID(), p.getGameProfile().getName(), name, color);
        giveBannerItem(p);
        Msg.everyone(s, "nusmp.nation.m.founded_broadcast", name, p.getGameProfile().getName());
        return Res.ok("nusmp.nation.m.founded", name);
    }

    /** Creates the nation record (no checks, no price): the shared tail of founding, also used by the self-tests. The name must already be validated and unclaimed. */
    public static Nation createNation(MinecraftServer s, UUID leader, String leaderName, String name, int color) {
        NationsData d = data(s);
        String base = Nation.slug(name), id = base;
        for (int i = 2; d.nations.containsKey(id); i++) id = base + "_" + i;
        Nation n = new Nation(id, name.trim(), color & 0xFFFFFF, leader, leaderName, now(s), NationsConfig.get(NationsConfig.LEDGER_MAX_ENTRIES));
        n.lastInterest = now(s);
        d.nations.put(id, n);
        d.indexMember(leader, id);
        d.touch(id);
        refresh(s, n);
        Treasury.post(new NationCreatedEvent(id, n.name, leader));
        return n;
    }

    /** Charges the founding price: coins from the wallet, items from the inventory, both, or nothing. Checks everything before taking anything. */
    private static Res chargeFoundingCost(ServerPlayer p) {
        String mode = NationsConfig.lower(NationsConfig.FOUND_COST_MODE);
        boolean coins = mode.equals("coins") || mode.equals("both");
        boolean items = mode.equals("item") || mode.equals("both");
        long price = NationsConfig.get(NationsConfig.FOUND_COST_COINS);
        int count = NationsConfig.get(NationsConfig.FOUND_COST_ITEM_COUNT);
        Item item = itemOf(NationsConfig.get(NationsConfig.FOUND_COST_ITEM));
        if (coins && price > 0 && EliteHooks.wallet.balance(p) < price) return Res.fail("nusmp.nation.m.found_need_coins", price);
        if (items && item != null && count > 0 && countItem(p, item) < count) return Res.fail("nusmp.nation.m.found_need_item", count, BuiltInRegistries.ITEM.getKey(item).toString());
        if (coins && price > 0 && !EliteHooks.wallet.charge(p, price)) return Res.fail("nusmp.nation.m.found_need_coins", price);
        if (items && item != null && count > 0) takeItem(p, item, count);
        return Res.ok("nusmp.nation.m.paid");
    }

    static Item itemOf(String id) {
        if (id == null || id.isBlank()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id.trim());
        if (rl == null) return null;
        return BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
    }

    static int countItem(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack st : p.getInventory().items) if (st.is(item)) n += st.getCount();
        return n;
    }

    static void takeItem(ServerPlayer p, Item item, int count) {
        int left = count;
        for (ItemStack st : p.getInventory().items) {
            if (left <= 0) break;
            if (st.is(item)) { int take = Math.min(left, st.getCount()); st.shrink(take); left -= take; }
        }
    }

    public static void giveBannerItem(ServerPlayer p) {
        ItemStack st = new ItemStack(NationBlocks.BANNER_ITEM.get());
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    // ---------------------------------------------------------------- disband
    public static Res disband(ServerPlayer p) {
        NationsData d = data(p.getServer());
        Nation n = own(d, p);
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!n.leader.equals(p.getUUID())) return Res.fail("nusmp.nation.m.leader_only");
        long payout = n.ledger.balance();
        Res r = disbandNation(p.getServer(), n.id, "disbanded");
        if (r.ok() && payout > 0) EliteHooks.wallet.give(p, payout);
        return r.ok() ? Res.ok("nusmp.nation.m.disbanded", n.name, payout) : r;
    }

    /** Removes a nation and everything that hangs on it. The treasury is gone unless the caller paid it out first. */
    public static Res disbandNation(MinecraftServer s, String nationId, String reason) {
        NationsData d = data(s);
        Nation n = d.nations.get(nationId);
        if (n == null) return Res.fail("nusmp.nation.m.no_such_nation", nationId);
        List<UUID> members = new ArrayList<>(n.members.keySet());
        GuardService.discardAll(s, n.id);
        removeBannerBlock(s, n);
        for (var w : d.wars.removeNation(n.id)) { /* open wars vanish without a history line */ }
        d.alliances.removeNation(n.id);
        d.reputation.removeNation(n.id);
        d.boards.remove(n.id);
        for (var b : d.bounties) if (n.id.equals(b.posterNation)) b.posterNation = "";
        d.nations.remove(n.id);
        for (UUID m : members) {
            d.unindexMember(m);
            ServerPlayer sp = s.getPlayerList().getPlayer(m);
            if (sp != null) {
                NationPerks.clear(sp);
                Msg.tell(sp, "nusmp.nation.m.your_nation_disbanded", n.name);
            }
            Treasury.post(new NationMemberLeftEvent(n.id, m, "disbanded"));
        }
        d.setDirty();
        Treasury.post(new NationDisbandedEvent(n.id, n.name));
        return Res.ok("nusmp.nation.m.disbanded", n.name, 0L);
    }

    // ---------------------------------------------------------------- membership
    public static Res invite(ServerPlayer actor, ServerPlayer target) {
        NationsData d = data(actor.getServer());
        Nation n = own(d, actor);
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!n.can(actor.getUUID(), Permission.INVITE)) return Res.fail("nusmp.nation.m.no_permission", Permission.INVITE.id());
        if (d.nationOf(target.getUUID()) != null) return Res.fail("nusmp.nation.m.target_in_nation", target.getGameProfile().getName());
        if (n.memberCount() >= NationPerks.memberCap(n)) return Res.fail("nusmp.nation.m.nation_full", NationPerks.memberCap(n));
        n.invites.put(target.getUUID(), now(actor.getServer()) + NationsConfig.seconds(NationsConfig.INVITE_TTL_SECONDS));
        Msg.tell(target, "nusmp.nation.m.invited", n.name, actor.getGameProfile().getName(), n.id);
        return Res.ok("nusmp.nation.m.invite_sent", target.getGameProfile().getName());
    }

    /** Joins the nation that invited the player. {@code nationId} may be empty when there is exactly one invitation. */
    public static Res accept(ServerPlayer p, String nationId) {
        MinecraftServer s = p.getServer();
        NationsData d = data(s);
        if (!NationsConfig.enabled()) return Res.fail("nusmp.nation.m.disabled");
        if (d.nationOf(p.getUUID()) != null) return Res.fail("nusmp.nation.m.already_in_nation");
        long now = now(s);
        Nation chosen = null;
        for (Nation n : d.nations.values()) {
            Long exp = n.invites.get(p.getUUID());
            if (exp == null) continue;
            if (now >= exp) { n.invites.remove(p.getUUID()); continue; }
            if (nationId == null || nationId.isEmpty() || n.id.equals(nationId)) { chosen = n; break; }
        }
        if (chosen == null) return Res.fail("nusmp.nation.m.no_invite");
        if (chosen.memberCount() >= NationPerks.memberCap(chosen)) return Res.fail("nusmp.nation.m.nation_full", NationPerks.memberCap(chosen));
        chosen.addMember(p.getUUID(), p.getGameProfile().getName(), Roles.RECRUIT);
        d.indexMember(p.getUUID(), chosen.id);
        refresh(s, chosen);
        Msg.broadcast(s, chosen, "nusmp.nation.m.joined", p.getGameProfile().getName());
        Treasury.post(new NationMemberJoinedEvent(chosen.id, p.getUUID()));
        return Res.ok("nusmp.nation.m.you_joined", chosen.name);
    }

    public static Res decline(ServerPlayer p, String nationId) {
        NationsData d = data(p.getServer());
        boolean any = false;
        for (Nation n : d.nations.values()) if ((nationId == null || nationId.isEmpty() || n.id.equals(nationId)) && n.invites.remove(p.getUUID()) != null) any = true;
        return any ? Res.ok("nusmp.nation.m.invite_declined") : Res.fail("nusmp.nation.m.no_invite");
    }

    public static Res leave(ServerPlayer p) {
        MinecraftServer s = p.getServer();
        NationsData d = data(s);
        Nation n = own(d, p);
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (n.leader.equals(p.getUUID())) return Res.fail("nusmp.nation.m.leader_cannot_leave");
        n.removeMember(p.getUUID());
        d.unindexMember(p.getUUID());
        NationPerks.clear(p);
        refresh(s, n);
        Msg.broadcast(s, n, "nusmp.nation.m.left", p.getGameProfile().getName());
        Treasury.post(new NationMemberLeftEvent(n.id, p.getUUID(), "left"));
        return Res.ok("nusmp.nation.m.you_left", n.name);
    }

    public static Res kick(ServerPlayer actor, UUID target) {
        MinecraftServer s = actor.getServer();
        NationsData d = data(s);
        Nation n = own(d, actor);
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!n.can(actor.getUUID(), Permission.KICK)) return Res.fail("nusmp.nation.m.no_permission", Permission.KICK.id());
        if (!n.isMember(target)) return Res.fail("nusmp.nation.m.not_a_member");
        if (target.equals(actor.getUUID())) return Res.fail("nusmp.nation.m.cannot_target_self");
        if (!actor.getUUID().equals(n.leader) && !Roles.canActOn(n.roleOf(actor.getUUID()), n.roleOf(target))) return Res.fail("nusmp.nation.m.rank_too_low");
        String name = n.nameOf(target);
        if (!n.removeMember(target)) return Res.fail("nusmp.nation.m.rank_too_low");
        d.unindexMember(target);
        ServerPlayer sp = s.getPlayerList().getPlayer(target);
        if (sp != null) { NationPerks.clear(sp); Msg.tell(sp, "nusmp.nation.m.you_were_kicked", n.name); }
        refresh(s, n);
        Msg.broadcast(s, n, "nusmp.nation.m.kicked", name);
        Treasury.post(new NationMemberLeftEvent(n.id, target, "kicked"));
        return Res.ok("nusmp.nation.m.kick_ok", name);
    }

    public static Res setRole(ServerPlayer actor, UUID target, String roleId) {
        NationsData d = data(actor.getServer());
        Nation n = own(d, actor);
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!n.isMember(target)) return Res.fail("nusmp.nation.m.not_a_member");
        Role newRole = n.role(roleId);
        if (newRole == null) return Res.fail("nusmp.nation.m.no_such_role", roleId);
        boolean leader = actor.getUUID().equals(n.leader);
        Role actorRole = n.roleOf(actor.getUUID());
        if (!leader) {
            if (!Roles.canAssign(actorRole, newRole)) return Res.fail("nusmp.nation.m.rank_too_low");
            if (!Roles.canActOn(actorRole, n.roleOf(target))) return Res.fail("nusmp.nation.m.rank_too_low");
        }
        if (Roles.LEADER.equals(roleId)) return Res.fail("nusmp.nation.m.use_transfer");
        if (!n.setRole(target, roleId)) return Res.fail("nusmp.nation.m.cannot_change_leader");
        d.touch(n.id);
        return Res.ok("nusmp.nation.m.role_set", n.nameOf(target), newRole.name());
    }

    public static Res transferLeadership(ServerPlayer actor, UUID target) {
        NationsData d = data(actor.getServer());
        Nation n = own(d, actor);
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!n.leader.equals(actor.getUUID())) return Res.fail("nusmp.nation.m.leader_only");
        if (!n.transferLeadership(target)) return Res.fail("nusmp.nation.m.not_a_member");
        d.touch(n.id);
        Msg.broadcast(actor.getServer(), n, "nusmp.nation.m.new_leader", n.nameOf(target));
        return Res.ok("nusmp.nation.m.new_leader", n.nameOf(target));
    }

    // ---------------------------------------------------------------- roles
    public static Res createRole(ServerPlayer actor, String id, String name) {
        NationsData d = data(actor.getServer());
        Nation n = own(d, actor);
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        Role actorRole = n.roleOf(actor.getUUID());
        if (!n.can(actor.getUUID(), Permission.ROLES)) return Res.fail("nusmp.nation.m.no_permission", Permission.ROLES.id());
        if (!Roles.validId(id) || !Roles.validName(name)) return Res.fail("nusmp.nation.m.bad_role");
        if (n.roles.containsKey(id)) return Res.fail("nusmp.nation.m.role_exists", id);
        int custom = 0;
        for (String rid : n.roles.keySet()) if (!List.of(Roles.LEADER, Roles.OFFICER, Roles.MEMBER, Roles.RECRUIT).contains(rid)) custom++;
        if (custom >= NationsConfig.get(NationsConfig.MAX_CUSTOM_ROLES)) return Res.fail("nusmp.nation.m.too_many_roles", NationsConfig.get(NationsConfig.MAX_CUSTOM_ROLES));
        Role actorEff = actor.getUUID().equals(n.leader) ? n.role(Roles.LEADER) : actorRole;
        n.roles.put(id, new Role(id, name.trim(), Roles.rankForNewRole(actorEff), 0));
        d.touch(n.id);
        return Res.ok("nusmp.nation.m.role_created", name.trim());
    }

    public static Res deleteRole(ServerPlayer actor, String id) {
        NationsData d = data(actor.getServer());
        Nation n = own(d, actor);
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        Role target = n.role(id);
        if (target == null) return Res.fail("nusmp.nation.m.no_such_role", id);
        Role actorRole = actor.getUUID().equals(n.leader) ? n.role(Roles.LEADER) : n.roleOf(actor.getUUID());
        if (List.of(Roles.LEADER, Roles.OFFICER, Roles.MEMBER, Roles.RECRUIT).contains(id)) return Res.fail("nusmp.nation.m.role_protected");
        if (!Roles.canEdit(actorRole, target)) return Res.fail("nusmp.nation.m.rank_too_low");
        for (var e : n.members.entrySet()) if (e.getValue().equals(id)) e.setValue(Roles.RECRUIT);
        n.roles.remove(id);
        d.touch(n.id);
        return Res.ok("nusmp.nation.m.role_deleted", target.name());
    }

    public static Res setRolePermission(ServerPlayer actor, String id, Permission perm, boolean on) {
        NationsData d = data(actor.getServer());
        Nation n = own(d, actor);
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        Role target = n.role(id);
        if (target == null) return Res.fail("nusmp.nation.m.no_such_role", id);
        Role actorRole = actor.getUUID().equals(n.leader) ? n.role(Roles.LEADER) : n.roleOf(actor.getUUID());
        if (!Roles.canEdit(actorRole, target)) return Res.fail("nusmp.nation.m.rank_too_low");
        if (on && !Roles.canGrant(actorRole, perm)) return Res.fail("nusmp.nation.m.cannot_grant", perm.id());
        n.roles.put(id, target.withPerm(perm, on));
        d.touch(n.id);
        return Res.ok("nusmp.nation.m.role_perm", target.name(), perm.id(), on ? "on" : "off");
    }

    // ---------------------------------------------------------------- settings
    public static Res rename(ServerPlayer actor, String rawName) {
        NationsData d = data(actor.getServer());
        Nation n = own(d, actor);
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!n.leader.equals(actor.getUUID())) return Res.fail("nusmp.nation.m.leader_only");
        String name = rawName == null ? "" : rawName.trim();
        if (!Nation.validName(name, NationsConfig.get(NationsConfig.NAME_MIN), NationsConfig.get(NationsConfig.NAME_MAX))) return Res.fail("nusmp.nation.m.bad_name", NationsConfig.get(NationsConfig.NAME_MIN), NationsConfig.get(NationsConfig.NAME_MAX));
        for (Nation o : d.nations.values()) if (o != n && o.name.equalsIgnoreCase(name)) return Res.fail("nusmp.nation.m.name_taken", name);
        n.name = name;
        d.touch(n.id);
        return Res.ok("nusmp.nation.m.renamed", name);
    }

    public static Res setColor(ServerPlayer actor, int rgb) {
        NationsData d = data(actor.getServer());
        Nation n = own(d, actor);
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!n.leader.equals(actor.getUUID())) return Res.fail("nusmp.nation.m.leader_only");
        n.color = rgb & 0xFFFFFF;
        d.touch(n.id);
        return Res.ok("nusmp.nation.m.color_set", String.format(Locale.ROOT, "%06X", n.color));
    }

    // ---------------------------------------------------------------- capital
    /** May this player place the capital block here? (Called before the block is placed; also by setCapital.) */
    public static Res canPlaceCapital(ServerPlayer p, ServerLevel level, BlockPos pos) {
        if (!NationsConfig.enabled()) return Res.fail("nusmp.nation.m.disabled");
        NationsData d = data(p.getServer());
        Nation n = own(d, p);
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!n.leader.equals(p.getUUID())) return Res.fail("nusmp.nation.m.leader_only");
        if (n.hasCapital) return Res.fail("nusmp.nation.m.has_capital");
        if (com.newuniverse.nusmp.dungeon.DungeonGuard.on(level)) return Res.fail("nusmp.nation.m.not_in_dungeon");
        int radius = NationsConfig.get(NationsConfig.CAPITAL_RADIUS);
        String dim = level.dimension().location().toString();
        for (Nation o : d.nations.values()) {
            if (o != n && o.hasCapital && o.capitalDim.equals(dim) && GuardRules.inZone(pos.getX(), pos.getY(), pos.getZ(), o.cx, o.cy, o.cz, radius * 2, 256)) return Res.fail("nusmp.nation.m.too_close", o.name);
        }
        return Res.ok("nusmp.nation.m.capital_ok");
    }

    public static Res setCapital(ServerPlayer p, ServerLevel level, BlockPos pos) {
        Res can = canPlaceCapital(p, level, pos);
        if (!can.ok()) return can;
        NationsData d = data(p.getServer());
        Nation n = own(d, p);
        n.hasCapital = true;
        n.capitalDim = level.dimension().location().toString();
        n.cx = pos.getX();
        n.cy = pos.getY();
        n.cz = pos.getZ();
        d.touch(n.id);
        Msg.broadcast(p.getServer(), n, "nusmp.nation.m.capital_set", pos.getX(), pos.getY(), pos.getZ());
        return Res.ok("nusmp.nation.m.capital_set", pos.getX(), pos.getY(), pos.getZ());
    }

    public static void clearCapital(MinecraftServer s, Nation n) {
        if (!n.hasCapital) return;
        n.hasCapital = false;
        GuardService.discardAll(s, n.id);
        data(s).touch(n.id);
        Msg.broadcast(s, n, "nusmp.nation.m.capital_lost");
    }

    private static void removeBannerBlock(MinecraftServer s, Nation n) {
        if (!n.hasCapital) return;
        for (ServerLevel lv : s.getAllLevels()) {
            if (!lv.dimension().location().toString().equals(n.capitalDim)) continue;
            BlockPos pos = new BlockPos(n.cx, n.cy, n.cz);
            if (lv.isLoaded(pos) && lv.getBlockState(pos).is(NationBlocks.BANNER.get())) lv.removeBlock(pos, false);
        }
    }

    // ---------------------------------------------------------------- admin
    public static Res adminAddXp(MinecraftServer s, String nationId, long xp) {
        Nation n = data(s).nations.get(nationId);
        if (n == null) return Res.fail("nusmp.nation.m.no_such_nation", nationId);
        addXp(s, n, xp, "admin");
        return Res.ok("nusmp.nation.m.xp_added", xp, n.name);
    }

    public static Res adminDeposit(MinecraftServer s, String nationId, long coins) {
        Nation n = data(s).nations.get(nationId);
        if (n == null) return Res.fail("nusmp.nation.m.no_such_nation", nationId);
        long moved = coins >= 0 ? Treasury.credit(s, n, Ledger.Kind.ADMIN, null, coins, "admin") : Treasury.debit(s, n, Ledger.Kind.ADMIN, null, -coins, "admin");
        return Res.ok("nusmp.nation.m.admin_money", moved, n.name);
    }

    /** Keeps the saved name of a member fresh (called on login). */
    public static void onLogin(ServerPlayer p) {
        NationsData d = data(p.getServer());
        Nation n = d.nationOf(p.getUUID());
        if (n != null && !p.getGameProfile().getName().equals(n.names.get(p.getUUID()))) { n.names.put(p.getUUID(), p.getGameProfile().getName()); d.touch(n.id); }
    }

    /** Resolves a member by remembered name (case-insensitive). */
    public static UUID memberByName(Nation n, String name) {
        if (n == null || name == null) return null;
        for (UUID id : n.members.keySet()) if (n.nameOf(id).equalsIgnoreCase(name)) return id;
        return null;
    }
}
