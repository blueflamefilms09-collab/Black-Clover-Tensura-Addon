package com.newuniverse.nusmp.multiverse;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Magic Knight Squads, run on FTB Teams (required; without it squad commands say so and do nothing). One squad = one FTB party
 * team flagged as a Magic Knight squad. Captain = the FTB team owner; Vice = picked by the captain; everyone else a Member.
 * Membership itself (invites, joining, leaving, ownership) goes through FTB Teams' own commands, so FTB's rules apply; this
 * class adds the Magic Knight checks (rank to create / join, max size) and the star score and standings.
 *
 * <p>FTB Teams is reached by reflection through its public API interfaces, so this mod neither needs it to build nor to run.
 */
public final class Squads {
    private Squads() {}

    // ---------------------------------------------------------------- FTB Teams bridge
    static final class Ftb {
        private static Boolean ok;
        private static Method api, getManager, isManagerLoaded, getTeamForPlayer, getTeamByID;
        private static Method teamId, isParty, shortName, owner, members;

        static boolean available() {
            if (ok != null) return ok;
            ok = false;
            if (!ModList.get().isLoaded("ftbteams")) return false;
            try {
                Class<?> apiCls = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
                Class<?> apiItf = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
                Class<?> mgr = Class.forName("dev.ftb.mods.ftbteams.api.TeamManager");
                Class<?> team = Class.forName("dev.ftb.mods.ftbteams.api.Team");
                api = apiCls.getMethod("api");
                getManager = apiItf.getMethod("getManager");
                isManagerLoaded = apiItf.getMethod("isManagerLoaded");
                getTeamForPlayer = mgr.getMethod("getTeamForPlayer", ServerPlayer.class);
                getTeamByID = mgr.getMethod("getTeamByID", UUID.class);
                teamId = team.getMethod("getId");
                isParty = team.getMethod("isPartyTeam");
                shortName = team.getMethod("getShortName");
                owner = team.getMethod("getOwner");
                members = team.getMethod("getMembers");
                ok = true;
            } catch (ReflectiveOperationException e) {
                com.mojang.logging.LogUtils.getLogger().warn("[nusmp] FTB Teams found but its API could not be reached; squads disabled", e);
            }
            return ok;
        }

        private static Object manager() throws ReflectiveOperationException {
            Object a = api.invoke(null);
            if (a == null || !(boolean) isManagerLoaded.invoke(a)) return null;
            return getManager.invoke(a);
        }

        /** The player's FTB party team (not their personal team), if any. */
        static Optional<Object> party(ServerPlayer p) {
            if (!available()) return Optional.empty();
            try {
                Object m = manager();
                if (m == null) return Optional.empty();
                Optional<?> t = (Optional<?>) getTeamForPlayer.invoke(m, p);
                return t.isPresent() && (boolean) isParty.invoke(t.get()) ? Optional.of(t.get()) : Optional.empty();
            } catch (ReflectiveOperationException e) { return Optional.empty(); }
        }

        static Optional<Object> byId(UUID id) {
            if (!available()) return Optional.empty();
            try {
                Object m = manager();
                if (m == null) return Optional.empty();
                Optional<?> t = (Optional<?>) getTeamByID.invoke(m, id);
                return t.map(o -> o);
            } catch (ReflectiveOperationException e) { return Optional.empty(); }
        }

        static UUID id(Object team) { try { return (UUID) teamId.invoke(team); } catch (ReflectiveOperationException e) { return null; } }
        static String name(Object team) { try { return (String) shortName.invoke(team); } catch (ReflectiveOperationException e) { return "?"; } }
        static UUID owner(Object team) { try { return (UUID) owner.invoke(team); } catch (ReflectiveOperationException e) { return null; } }

        @SuppressWarnings("unchecked")
        static Set<UUID> members(Object team) { try { return (Set<UUID>) members.invoke(team); } catch (ReflectiveOperationException e) { return Set.of(); } }

        /** Runs an FTB Teams command as the player, so FTB's own permission and rules apply. */
        static void run(ServerPlayer p, String command) {
            p.getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack(), command);
        }
    }

    public static boolean available() { return Ftb.available(); }

    public static final Component REQUIRES_FTB = Component.literal("Magic Knight Squads are not available on this server (team support is not installed).").withStyle(ChatFormatting.RED);

    // ---------------------------------------------------------------- squad records
    /** Which FTB teams are squads, their display name / colour / vice, and each member's last known stars. */
    static final class Data extends SavedData {
        static final class Squad {
            String name = "";
            int color = 0xFFD4AF37;
            UUID vice;
            final Map<UUID, int[]> lastStars = new HashMap<>();    // member -> {gold, black}
        }

        final Map<UUID, Squad> squads = new HashMap<>();

        static Data get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), "nusmp_squads");
        }

        private static Data load(CompoundTag tag, HolderLookup.Provider provider) {
            Data d = new Data();
            for (String k : tag.getAllKeys()) {
                CompoundTag c = tag.getCompound(k);
                Squad s = new Squad();
                s.name = c.getString("Name");
                s.color = c.contains("Color") ? c.getInt("Color") : s.color;
                if (c.hasUUID("Vice")) s.vice = c.getUUID("Vice");
                CompoundTag stars = c.getCompound("Stars");
                for (String m : stars.getAllKeys()) {
                    try { s.lastStars.put(UUID.fromString(m), stars.getIntArray(m)); } catch (IllegalArgumentException ignored) {}
                }
                try { d.squads.put(UUID.fromString(k), s); } catch (IllegalArgumentException ignored) {}
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            squads.forEach((id, s) -> {
                CompoundTag c = new CompoundTag();
                c.putString("Name", s.name);
                c.putInt("Color", s.color);
                if (s.vice != null) c.putUUID("Vice", s.vice);
                CompoundTag stars = new CompoundTag();
                s.lastStars.forEach((m, v) -> stars.putIntArray(m.toString(), v));
                c.put("Stars", stars);
                tag.put(id.toString(), c);
            });
            return tag;
        }
    }

    /** The squad this player is in: (team, squad record), if their FTB party is a flagged squad. */
    record Membership(Object team, UUID teamId, Data.Squad squad) {}

    static Optional<Membership> membership(ServerPlayer p) {
        Optional<Object> t = Ftb.party(p);
        if (t.isEmpty()) return Optional.empty();
        UUID id = Ftb.id(t.get());
        Data.Squad s = id == null ? null : Data.get(p.getServer()).squads.get(id);
        return s == null ? Optional.empty() : Optional.of(new Membership(t.get(), id, s));
    }

    public static String role(ServerPlayer p, Membership m) {
        if (p.getUUID().equals(Ftb.owner(m.team))) return "Captain";
        if (p.getUUID().equals(m.squad.vice)) return "Vice-Captain";
        return "Member";
    }

    // ---------------------------------------------------------------- score & standings
    static int score(MinecraftServer server, UUID teamId, Data.Squad s) {
        Optional<Object> team = Ftb.byId(teamId);
        if (team.isEmpty()) return 0;
        boolean gold = MultiverseConfig.get(MultiverseConfig.SQUAD_GOLD_FEEDS_SCORE), black = MultiverseConfig.get(MultiverseConfig.SQUAD_BLACK_REDUCES_SCORE);
        boolean offline = MultiverseConfig.get(MultiverseConfig.SQUAD_OFFLINE_COUNT);
        int total = 0;
        for (UUID m : Ftb.members(team.get())) {
            ServerPlayer online = server.getPlayerList().getPlayer(m);
            int[] st = online != null ? new int[]{MultiverseProfile.goldStars(online), MultiverseProfile.blackStars(online)} : offline ? s.lastStars.get(m) : null;
            if (st == null || st.length < 2) continue;
            total += (gold ? st[0] : 0) - (black ? st[1] : 0);
        }
        return total;
    }

    public record Standing(UUID teamId, String name, int score, int members) {}

    public static List<Standing> standings(MinecraftServer server) {
        List<Standing> out = new ArrayList<>();
        Data d = Data.get(server);
        d.squads.forEach((id, s) -> Ftb.byId(id).ifPresent(team -> out.add(new Standing(id, s.name, score(server, id, s), Ftb.members(team).size()))));
        out.sort((a, b) -> Integer.compare(b.score, a.score));
        return out;
    }

    /** Remember a member's stars so they still count while they are offline. */
    public static void onStarsChanged(ServerPlayer p) {
        if (!available()) return;
        membership(p).ifPresent(m -> {
            m.squad.lastStars.put(p.getUUID(), new int[]{MultiverseProfile.goldStars(p), MultiverseProfile.blackStars(p)});
            Data.get(p.getServer()).setDirty();
        });
    }

    // ---------------------------------------------------------------- actions (called by /multiverse squad ...)
    public static void create(ServerPlayer p, String name) {
        if (!available()) { p.sendSystemMessage(REQUIRES_FTB); return; }
        if (membership(p).isPresent()) { fail(p, "You are already in a squad."); return; }
        int need = KnightRank.parseStep(MultiverseConfig.get(MultiverseConfig.SQUAD_MIN_RANK_CREATE));
        if (MultiverseProfile.step(p) < need) {
            fail(p, "Founding a squad takes at least " + KnightRank.describe(KnightRank.rankOfStep(need), KnightRank.classOfStep(need)) + ".");
            return;
        }
        Optional<Object> party = Ftb.party(p);
        if (party.isEmpty()) {
            Ftb.run(p, "ftbteams party create " + name);          // FTB makes the team; we flag it below
            party = Ftb.party(p);
            if (party.isEmpty()) { fail(p, "The squad could not be founded. Try again, or ask an admin."); return; }
        } else if (!p.getUUID().equals(Ftb.owner(party.get()))) {
            fail(p, "Only your party's owner can turn it into a squad.");
            return;
        }
        Data d = Data.get(p.getServer());
        Data.Squad s = new Data.Squad();
        s.name = name;
        s.lastStars.put(p.getUUID(), new int[]{MultiverseProfile.goldStars(p), MultiverseProfile.blackStars(p)});
        d.squads.put(Ftb.id(party.get()), s);
        d.setDirty();
        p.getServer().getPlayerList().broadcastSystemMessage(Component.literal("A new Magic Knight squad is founded: " + name + ", Captain " + p.getName().getString() + ".")
                .withStyle(ChatFormatting.GOLD), false);
        MultiverseSync.markDirty(p);
    }

    public static void invite(ServerPlayer p, ServerPlayer target) {
        if (!available()) { p.sendSystemMessage(REQUIRES_FTB); return; }
        Optional<Membership> m = membership(p);
        if (m.isEmpty()) { fail(p, "You are not in a squad."); return; }
        if (Ftb.members(m.get().team).size() >= MultiverseConfig.get(MultiverseConfig.SQUAD_MAX_SIZE)) { fail(p, "Your squad is full."); return; }
        int need = KnightRank.parseStep(MultiverseConfig.get(MultiverseConfig.SQUAD_MIN_RANK_JOIN));
        if (MultiverseProfile.step(target) < need) { fail(p, target.getName().getString() + " does not meet the squad's rank requirement yet."); return; }
        if (membership(target).isPresent()) { fail(p, target.getName().getString() + " is already in a squad."); return; }
        Ftb.run(p, "ftbteams party invite " + target.getGameProfile().getName());
    }

    public static void leave(ServerPlayer p) {
        if (!available()) { p.sendSystemMessage(REQUIRES_FTB); return; }
        Optional<Membership> m = membership(p);
        if (m.isEmpty()) { fail(p, "You are not in a squad."); return; }
        if (p.getUUID().equals(m.get().squad.vice)) { m.get().squad.vice = null; Data.get(p.getServer()).setDirty(); }
        Ftb.run(p, "ftbteams party leave");
        MultiverseSync.markDirty(p);
    }

    public static void setCaptain(ServerPlayer p, ServerPlayer target) {
        if (!available()) { p.sendSystemMessage(REQUIRES_FTB); return; }
        Optional<Membership> m = membership(p);
        if (m.isEmpty() || !p.getUUID().equals(Ftb.owner(m.get().team))) { fail(p, "Only the captain can hand over the squad."); return; }
        Ftb.run(p, "ftbteams party transfer_ownership " + target.getGameProfile().getName());
    }

    public static void setVice(ServerPlayer p, ServerPlayer target) {
        if (!available()) { p.sendSystemMessage(REQUIRES_FTB); return; }
        Optional<Membership> m = membership(p);
        if (m.isEmpty() || !p.getUUID().equals(Ftb.owner(m.get().team))) { fail(p, "Only the captain can name a vice-captain."); return; }
        if (!MultiverseConfig.get(MultiverseConfig.SQUAD_CAPTAIN_PROMOTES_VICE)) { fail(p, "Vice-captains are appointed by the server."); return; }
        if (!Ftb.members(m.get().team).contains(target.getUUID())) { fail(p, target.getName().getString() + " is not in your squad."); return; }
        m.get().squad.vice = target.getUUID();
        Data.get(p.getServer()).setDirty();
        target.sendSystemMessage(Component.literal("You are now Vice-Captain of " + m.get().squad.name + ".").withStyle(ChatFormatting.GOLD));
        MultiverseSync.markDirty(target);
    }

    public static List<Component> info(ServerPlayer p) {
        List<Component> out = new ArrayList<>();
        if (!available()) { out.add(REQUIRES_FTB); return out; }
        Optional<Membership> m = membership(p);
        if (m.isEmpty()) { out.add(Component.literal("You are not in a Magic Knight squad.").withStyle(ChatFormatting.GRAY)); return out; }
        MinecraftServer server = p.getServer();
        Data.Squad s = m.get().squad;
        int score = score(server, m.get().teamId, s);
        int place = 1;
        for (Standing st : standings(server)) { if (st.teamId().equals(m.get().teamId)) break; place++; }
        out.add(Component.literal(s.name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("  score " + score + ", standing #" + place).withStyle(ChatFormatting.WHITE)));
        for (UUID id : Ftb.members(m.get().team)) {
            ServerPlayer on = server.getPlayerList().getPlayer(id);
            String name = on != null ? on.getName().getString() : server.getProfileCache() == null ? id.toString()
                    : server.getProfileCache().get(id).map(gp -> gp.getName()).orElse(id.toString());
            String role = id.equals(Ftb.owner(m.get().team)) ? "Captain" : id.equals(s.vice) ? "Vice-Captain" : "Member";
            out.add(Component.literal(" " + role + ": " + name + (on == null ? " (offline)" : "")).withStyle(ChatFormatting.GRAY));
        }
        return out;
    }

    /** Status panel block. */
    public static CompoundTag summary(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        if (!available()) { t.putString("State", "Requires team support"); return t; }
        Optional<Membership> m = membership(p);
        if (m.isEmpty()) { t.putString("State", "No squad"); return t; }
        t.putString("Name", m.get().squad.name);
        t.putString("Role", role(p, m.get()));
        int place = 1;
        List<Standing> all = standings(p.getServer());
        for (Standing st : all) { if (st.teamId().equals(m.get().teamId)) break; place++; }
        t.putString("Standing", "#" + place + " of " + all.size());
        t.putInt("Score", score(p.getServer(), m.get().teamId, m.get().squad));
        return t;
    }

    /** Every 5 s: keep offline star records fresh and enforce the squad rules on members FTB let in. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 100 != 13 || !available()) return;
        Optional<Membership> m = membership(p);
        if (m.isEmpty()) return;
        int[] now = {MultiverseProfile.goldStars(p), MultiverseProfile.blackStars(p)};
        int[] before = m.get().squad.lastStars.get(p.getUUID());
        if (before == null || before[0] != now[0] || before[1] != now[1]) {
            m.get().squad.lastStars.put(p.getUUID(), now);
            Data.get(p.getServer()).setDirty();
        }
        if (p.getUUID().equals(Ftb.owner(m.get().team))) return;
        int need = KnightRank.parseStep(MultiverseConfig.get(MultiverseConfig.SQUAD_MIN_RANK_JOIN));
        Collection<UUID> members = Ftb.members(m.get().team);
        if (MultiverseProfile.step(p) < need || members.size() > MultiverseConfig.get(MultiverseConfig.SQUAD_MAX_SIZE)) {
            fail(p, "You do not meet " + m.get().squad.name + "'s Magic Knight requirements (rank or squad size). You leave the squad.");
            Ftb.run(p, "ftbteams party leave");
        }
    }

    /** A flagged team that FTB disbanded is forgotten (its record would never be reached again). */
    public static void prune(MinecraftServer server) {
        if (!available()) return;
        Data d = Data.get(server);
        if (d.squads.keySet().removeIf(id -> Ftb.byId(id).isEmpty())) d.setDirty();
    }

    private static void fail(ServerPlayer p, String msg) { p.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED)); }

}
