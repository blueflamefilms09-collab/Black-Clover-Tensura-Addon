package com.newuniverse.nusmp.avatar.delve;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarManager;
import com.newuniverse.nusmp.avatar.Stance;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.avatar.betrayal.BetrayalDimensionHandler;
import com.newuniverse.nusmp.avatar.goal.AvatarTargeting;
import com.newuniverse.nusmp.avatar.inventory.AvatarGear;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Why: sending an Avatar into the Labyrinth and getting it back is the part of delving that touches the world, so it is kept in one place away from the goal that
 * decides when to go. It reads the Mega-Dungeon only through its facade and its plain saved data (the dimension, the sectors of a road, a sector's arrival cell, its
 * rooms), picks the floor the creator last reached (never deeper than the cap), writes the trip record into the Avatar's own persistent data (so it survives the
 * dimension change, which re-creates the entity from its NBT, and a restart), moves the Avatar with the same dimension transition the betrayal tracker uses, keeps its
 * chunks loaded with the short portal ticket while it is there, and on the way back puts the Avatar beside its creator (or at the spot it left from when the creator is
 * away), hands over its valuables and tells the creator how it went. It does NOT walk through the Gateway: the Avatar opens its own way (a space portal effect) and the
 * Directional Lock, which is a rule for players, is not asked. Server thread only.
 */
public final class AvatarDelve {
    private static final Logger LOG = LogUtils.getLogger();

    private AvatarDelve() {}

    /** The trip record in the Avatar's persistent data, and the game time before which it will not set out again. */
    public static final String TRIP = "nusmp_av_trip", REST = "nusmp_av_delve_rest";
    /** The creator's own memory of the deepest floor of a road ({@code MegaTravel} writes it; repeated here as plain strings so this package depends only on the facade). */
    private static final String LANDING_HOLY = "nusmp_mega_landing_holy", LANDING_ABYSSAL = "nusmp_mega_landing_abyssal";

    // ---------------------------------------------------------------- state
    /** Is this Avatar on a trip into the dungeon right now? (Only a loyal one: a betrayal or an awakening cancels the errand.) */
    public static boolean onTrip(AvatarEntity a) { return a != null && a.isLoyal() && a.getPersistentData().contains(TRIP, Tag.TAG_COMPOUND); }

    static CompoundTag trip(AvatarEntity a) { return a.getPersistentData().getCompound(TRIP); }

    static long restUntil(AvatarEntity a) { return a.getPersistentData().getLong(REST); }

    // ---------------------------------------------------------------- the dungeon
    /** The dungeon's level, or null when the server does not have it (switched off, or not loaded). */
    public static ServerLevel dungeon(MinecraftServer server) { return MegaDungeon.level(server); }

    static Polarity road() { return AvatarAiConfig.delveAbyssal() ? Polarity.ABYSSAL : Polarity.HOLY; }

    private static boolean playable(Sector s) { return s.state != Sector.State.GENERATING && s.state != Sector.State.FAILED; }

    /** The deepest chain index the creator has reached on the road (0 when they never went). */
    static int creatorDeepest(ServerPlayer creator, Polarity road) {
        return Math.max(0, creator.getPersistentData().getInt(road == Polarity.ABYSSAL ? LANDING_ABYSSAL : LANDING_HOLY));
    }

    /**
     * The sector to arrive at: the floor the creator last reached on the configured road, but not beyond the newest finished one and not beyond the configured cap, in the
     * chain of the creator's party (the dungeon is instanced since 1.58.0). Null when the dungeon does not exist, the creator's party has no floors or the road has no
     * finished sector. The Avatar never wakes the dungeon itself.
     */
    public static Sector target(MinecraftServer server, ServerPlayer creator) {
        if (server == null || creator == null || dungeon(server) == null) return null;
        MegaData data = MegaData.get(server);
        int party = PartyService.knownParty(creator);                  // 1.58.0: the dungeon is instanced; the Avatar delves in the floors of its creator's party and nobody else's
        if (party < 0) return null;
        Polarity road = road();
        int count = 0;
        for (Sector s = data.chainSector(party, road, 0); s != null && playable(s); s = data.chainSector(party, road, count)) count++;
        int idx = DelveRules.landingIndex(creatorDeepest(creator, road), count, AvatarAiConfig.delveMaxChain());
        return idx < 0 ? null : data.chainSector(party, road, idx);
    }

    /** Where to stand on arrival: the sector's arrival cell, else the middle of its first landing or safe room; null when it has none. */
    public static BlockPos pad(Sector s) {
        if (s.arrival != null) return s.arrival;
        for (Room r : s.rooms) if (r.kind == RoomKind.LANDING || r.kind.isSafe()) return r.center();
        return null;
    }

    /** The sector the trip is in (by the saved id), or null. */
    static Sector sector(AvatarEntity a) {
        MinecraftServer server = a.level().getServer();
        return server == null || !onTrip(a) ? null : MegaData.get(server).sector(trip(a).getInt("Sector"));
    }

    /** Holds the chunks round {@code at} loaded for the next five seconds (the vanilla portal ticket; it expires by itself, nothing is left behind). */
    static void keepLoaded(ServerLevel level, BlockPos at) {
        level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(at), 3, at);
    }

    // ---------------------------------------------------------------- the limits (checked by the watcher goal, which never stops, not by the walking goal, which a fight pushes aside)
    /**
     * Once a second while the Avatar is on a trip: keeps the place loaded and ends the trip when a limit says so ({@link DelveRules#mustReturn}). It is called from the
     * watcher goal because the walking goal is stopped by every fight, and a limit must hold in the middle of one. The entity may be a different instance afterwards:
     * the caller must touch nothing more.
     */
    public static void watch(AvatarEntity a) {
        if (!onTrip(a) || !(a.level() instanceof ServerLevel sl)) return;
        if (MegaDungeon.in(sl)) keepLoaded(sl, a.blockPosition());
        DelveRules.Reason why = verdict(a, sl);
        if (why != DelveRules.Reason.NONE) finish(a, why);
    }

    private static DelveRules.Reason verdict(AvatarEntity a, ServerLevel sl) {
        ServerPlayer creator = a.creatorPlayer();
        long elapsed = sl.getGameTime() - trip(a).getLong("Start");
        double hp = a.getHealth() / Math.max(1.0f, a.getMaxHealth());
        boolean enabled = AvatarAiConfig.delveEnabled() && dungeon(sl.getServer()) != null && MegaDungeon.in(sl);
        boolean told = a.stance() != Stance.DELVE || (creator != null && creator.level() == sl);              // called back, or the creator came in themselves
        boolean full = a.inventory().freePockets() <= 1;
        DelveRules.Status st = new DelveRules.Status(hp, elapsed, full, told, creator == null, enabled);
        return DelveRules.mustReturn(st, AvatarAiConfig.delveReturnHealth(), DelveRules.budgetTicks(AvatarAiConfig.delveMinutes()), outmatched(a, sl));
    }

    /** Is this a monster of the dungeon (or any hostile) that the Avatar may attack? */
    static boolean hostile(AvatarEntity a, LivingEntity e) {
        return e != a && e.isAlive() && !(e instanceof Player) && (e instanceof Enemy || MegaDungeon.isDungeonMonster(e) || BossIdentity.isBoss(e)) && AvatarTargeting.canHarm(a, e);
    }

    /** Is something near that is too strong for the Avatar (a boss stronger than it, anything over three times its EP) and close, or already after it? */
    private static boolean outmatched(AvatarEntity a, ServerLevel sl) {
        double mine = TensuraBridge.maxEp(a);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, a.getBoundingBox().inflate(14), x -> hostile(a, x))) {
            if (DelveRules.worthEngaging(mine, TensuraBridge.maxEp(e), BossIdentity.isBoss(e))) continue;
            if (e.distanceToSqr(a) < 100.0 || (e instanceof Mob m && m.getTarget() == a)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- going in
    /**
     * Takes the Avatar into the dungeon at the sector's pad. Returns the Avatar that now stands there (a NEW instance: the old one is gone after the transition), or
     * null when it could not go (the trip record is removed again and nothing changed).
     */
    static AvatarEntity depart(AvatarEntity a, ServerLevel mega, Sector s, ServerPlayer creator) {
        BlockPos pad = pad(s);
        if (pad == null) return null;
        mega.getChunk(pad.getX() >> 4, pad.getZ() >> 4);                                 // load (and, for a fresh floor, make) the arrival chunk before anybody stands in it
        keepLoaded(mega, pad);
        CompoundTag t = new CompoundTag();
        t.putLong("Start", a.level().getGameTime());
        t.putString("Road", road().id());
        t.putInt("Sector", s.id);
        t.putInt("Floor", s.chainIndex);
        t.putString("HomeDim", a.level().dimension().location().toString());
        t.putDouble("HomeX", a.getX());
        t.putDouble("HomeY", a.getY());
        t.putDouble("HomeZ", a.getZ());
        t.putInt("Items0", a.inventory().countInPockets(x -> true));
        t.putIntArray("Visited", new int[0]);
        a.getPersistentData().put(TRIP, t);
        String floor = (s.firstFloor() > 0 ? "+" : "") + s.firstFloor();
        Entity moved = a.changeDimension(new DimensionTransition(mega, new Vec3(pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5), Vec3.ZERO, a.getYRot(), 0f, DimensionTransition.DO_NOTHING));
        if (!(moved instanceof AvatarEntity av)) {
            a.getPersistentData().remove(TRIP);
            return null;
        }
        AvatarManager.get(mega.getServer()).update(av);
        if (creator != null) creator.sendSystemMessage(Component.literal(av.avatarName() + " sets out for the Labyrinth (floor " + floor + ").").withStyle(ChatFormatting.GOLD));
        return av;
    }

    // ---------------------------------------------------------------- coming back
    /**
     * Ends the trip: removes the record, starts the rest period, puts the Avatar beside its creator (or at the spot it left from when the creator is away), hands the
     * valuables over and tells the creator how it went. Returns the Avatar that stands there now (the old instance is gone if the way back crossed a dimension).
     */
    static AvatarEntity finish(AvatarEntity a, DelveRules.Reason why) {
        CompoundTag t = trip(a).copy();
        long now = a.level().getGameTime();
        a.getPersistentData().remove(TRIP);
        a.getPersistentData().putLong(REST, now + AvatarAiConfig.delveRestTicks());
        a.setTarget(null);
        a.getNavigation().stop();
        int gathered = Math.max(0, a.inventory().countInPockets(x -> true) - t.getInt("Items0"));
        ServerPlayer creator = a.creatorPlayer();
        AvatarEntity back = a;
        try {
            AvatarEntity moved = creator != null ? BetrayalDimensionHandler.teleportNear(a, creator) : home(a, t);
            if (moved != null) back = moved;
        } catch (RuntimeException soft) {
            LOG.warn("[nusmp] An Avatar could not come back from the dungeon: {}", soft.toString());
        }
        int handed = 0;
        if (creator != null && AvatarAiConfig.lootShareWithCreator() && back.level() == creator.level() && back.distanceToSqr(creator) < 24.0 * 24.0) handed = AvatarGear.handOver(back, creator);
        if (creator != null) {
            String load = gathered > 0 ? "It brought back " + gathered + (gathered == 1 ? " item" : " items") : "It found nothing";
            creator.sendSystemMessage(Component.literal(back.avatarName() + " is back from the Labyrinth: " + DelveRules.describe(why) + ". " + load
                    + (handed > 0 ? ", and handed " + handed + (handed == 1 ? " stack" : " stacks") + " to you." : ".")).withStyle(ChatFormatting.GOLD));
        }
        return back;
    }

    /** Back to the spot it set out from (the creator is away). */
    private static AvatarEntity home(AvatarEntity a, CompoundTag t) {
        MinecraftServer server = a.level().getServer();
        ResourceLocation id = ResourceLocation.tryParse(t.getString("HomeDim"));
        ServerLevel dest = id == null || server == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (dest == null && server != null) dest = server.overworld();
        if (dest == null) return null;
        Vec3 pos = new Vec3(t.getDouble("HomeX"), t.getDouble("HomeY"), t.getDouble("HomeZ"));
        if (dest == a.level()) {
            a.teleportTo(pos.x, pos.y, pos.z);
            return a;
        }
        dest.getChunk(((int) Math.floor(pos.x)) >> 4, ((int) Math.floor(pos.z)) >> 4);
        Entity moved = a.changeDimension(new DimensionTransition(dest, pos, Vec3.ZERO, a.getYRot(), a.getXRot(), DimensionTransition.DO_NOTHING));
        return moved instanceof AvatarEntity av ? av : null;
    }
}
