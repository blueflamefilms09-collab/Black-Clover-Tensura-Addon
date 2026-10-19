package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.hub.HubTravel;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyService;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the instanced "Pocket Dimension" of a Type A rift. A real second dimension per rift would have to be created at run time, which NeoForge 1.21 does not support without a restart; so the
 * mod does what the Hub's plaza and the dungeon's own regions do: it gives every pocket a CELL of a far-away field of the Mega-Dungeon's dimension ({@link RiftRules#slotAt}), 192 blocks
 * apart and millions of blocks from any floor, builds the realm there on demand ({@link PocketBuilder}), tracks it here (saved, so a restart does not strand it), and clears it when it
 * collapses, freeing the cell. A pocket belongs to the rift that opened it: every player who steps into THAT rift joins the same realm, anyone else gets a realm of their own, and nobody can
 * walk from one cell into another (the cells are sealed rooms). It lives {@code pocketMinutes}, warns at 60, 30, 10, 5, 3, 2 and 1 seconds, and at the end throws everyone inside back to the place
 * they came in from (or their waypoint, or Floor 0), then removes everything it built and every entity in the cell. The state is a few numbers per pocket; the data lives in the overworld's
 * storage like the dungeon's own. Server thread only.
 */
public final class PocketRealm extends SavedData {
    public static final String NAME = "nusmp_rift_pockets";
    /** Persistent data of a player inside a pocket: where they came from (dimension, position, rotation), and which slot. */
    public static final String RETURN = "nusmp_pocket_return";

    /** One live pocket. */
    public static final class Pocket {
        public int slot, floor;
        public String variant = "boss";
        public long seed, createdAt, expiresAt;
        public boolean bossDead, solved;
        public int pressed;
        public BlockPos[] plates = new BlockPos[0];
        public List<BlockPos> bars = new ArrayList<>();
        public BlockPos exit = BlockPos.ZERO, bossSpawn = BlockPos.ZERO, entry = BlockPos.ZERO;
        long lastLeft;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("Slot", slot);
            t.putInt("Floor", floor);
            t.putString("Variant", variant);
            t.putLong("Seed", seed);
            t.putLong("Created", createdAt);
            t.putLong("Expires", expiresAt);
            t.putBoolean("BossDead", bossDead);
            t.putBoolean("Solved", solved);
            t.putInt("Pressed", pressed);
            t.putLong("Exit", exit.asLong());
            t.putLong("BossSpawn", bossSpawn.asLong());
            t.putLong("Entry", entry.asLong());
            t.putLongArray("Plates", java.util.Arrays.stream(plates).mapToLong(BlockPos::asLong).toArray());
            t.putLongArray("Bars", bars.stream().mapToLong(BlockPos::asLong).toArray());
            return t;
        }

        static Pocket load(CompoundTag t) {
            Pocket p = new Pocket();
            p.slot = t.getInt("Slot");
            p.floor = t.getInt("Floor");
            p.variant = t.getString("Variant");
            p.seed = t.getLong("Seed");
            p.createdAt = t.getLong("Created");
            p.expiresAt = t.getLong("Expires");
            p.bossDead = t.getBoolean("BossDead");
            p.solved = t.getBoolean("Solved");
            p.pressed = t.getInt("Pressed");
            p.exit = BlockPos.of(t.getLong("Exit"));
            p.bossSpawn = BlockPos.of(t.getLong("BossSpawn"));
            p.entry = BlockPos.of(t.getLong("Entry"));
            p.plates = java.util.Arrays.stream(t.getLongArray("Plates")).mapToObj(BlockPos::of).toArray(BlockPos[]::new);
            p.bars = new ArrayList<>();
            for (long l : t.getLongArray("Bars")) p.bars.add(BlockPos.of(l));
            return p;
        }

        public RiftRules.Variant variantKind() { return RiftRules.Variant.byId(variant); }
    }

    private final Map<Integer, Pocket> pockets = new LinkedHashMap<>();

    public static PocketRealm get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(PocketRealm::new, PocketRealm::load, null), NAME);
    }

    private static PocketRealm load(CompoundTag t, HolderLookup.Provider lookup) {
        PocketRealm r = new PocketRealm();
        ListTag list = t.getList("Pockets", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Pocket p = Pocket.load(list.getCompound(i));
            r.pockets.put(p.slot, p);
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        for (Pocket p : pockets.values()) list.add(p.save());
        t.put("Pockets", list);
        return t;
    }

    public Map<Integer, Pocket> pockets() { return pockets; }

    public Pocket at(int slot) { return pockets.get(slot); }

    // ---------------------------------------------------------------- entering and leaving
    /** A player stepped into a pocket rift: build the realm if this rift has none yet, and take them (and the party mates beside them) in. */
    static void enter(ServerLevel from, ServerPlayer p, DimensionalRiftBlockEntity rift) {
        MinecraftServer server = from.getServer();
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null) return;
        Sector here = MegaData.get(server).sectorAt(rift.getBlockPos());
        if (here != null && !MegaDungeon.mayEnter(p, here.polarity)) return;                    // the lock says why
        PocketRealm realm = get(server);
        Pocket pocket = rift.slot() >= 0 ? realm.pockets.get(rift.slot()) : null;
        if (pocket == null) pocket = realm.create(mega, from, rift);
        if (pocket == null) {
            WaypointService.say(p, "pocket.full", "Every hidden realm is in use. Try again in a while.", ChatFormatting.GRAY);
            return;
        }
        List<ServerPlayer> group = new ArrayList<>();
        group.add(p);
        for (ServerPlayer o : from.players()) if (o != p && o.distanceToSqr(p) <= 36 && !o.isSpectator() && PartyService.partyOf(o) == PartyService.partyOf(p)) group.add(o);
        for (ServerPlayer m : group) {
            CompoundTag r = new CompoundTag();
            r.putString("Dim", m.level().dimension().location().toString());
            r.putDouble("X", m.getX());
            r.putDouble("Y", m.getY());
            r.putDouble("Z", m.getZ());
            r.putFloat("Yaw", m.getYRot());
            r.putFloat("Pitch", m.getXRot());
            r.putInt("Slot", pocket.slot);
            m.getPersistentData().put(RETURN, r);
            BlockPos e = pocket.entry;
            mega.getChunk(e.getX() >> 4, e.getZ() >> 4);
            m.teleportTo(mega, e.getX() + 0.5, e.getY(), e.getZ() + 0.5, -90f, 0f);
            m.resetFallDistance();
            RiftService.title(m, Component.translatableWithFallback("message.nusmp.mega.rift.pocket.title", "A hidden realm"),
                    Component.translatableWithFallback("message.nusmp.mega.rift.pocket." + pocket.variant, pocket.variant.equals("boss") ? "Something old guards what is behind it." : "Press the plates in the order the wall shows."));
            mega.playSound(null, e, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.4f, 1.4f);
            NusmpEvents.fire(m, "mega.pocket", pocket.variant);
        }
    }

    private Pocket create(ServerLevel mega, ServerLevel from, DimensionalRiftBlockEntity rift) {
        int slot = -1;
        for (int i = 0; i < RiftRules.MAX_SLOTS; i++) if (!pockets.containsKey(i)) { slot = i; break; }
        if (slot < 0) return null;
        MegaData md = MegaData.get(mega.getServer());
        Sector s = md.sectorAt(rift.getBlockPos());
        int floor = s == null ? 1 : s.firstFloor();
        Pocket p = new Pocket();
        p.slot = slot;
        p.floor = floor;
        p.seed = rift.seed();
        p.variant = RiftRules.variantFor(rift.seed(), RiftConfig.get(RiftConfig.BOSS_SHARE)).id();
        p.createdAt = mega.getGameTime();
        p.expiresAt = p.createdAt + 20L * 60L * RiftConfig.get(RiftConfig.POCKET_MINUTES);
        PocketBuilder.build(mega, p, s == null ? com.newuniverse.nusmp.labyrinth.mega.RegionTheme.GRAVITY : s.theme);
        pockets.put(slot, p);
        setDirty();
        rift.aimPocket(slot);
        return p;
    }

    /** 1.94.0 (escape rules): the exit as a public entry for {@code /dungeon unstuck}, so a player in a pocket realm is taken out through the same door as the exit rift. */
    public static void leaveForRescue(ServerLevel level, ServerPlayer p) { leave(level, p); }

    /** The exit: back to where the player came from; if that is gone, their waypoint; if that is gone too, Floor 0. */
    static void leave(ServerLevel level, ServerPlayer p) {
        MinecraftServer server = p.getServer();
        CompoundTag r = p.getPersistentData().getCompound(RETURN);
        p.getPersistentData().remove(RETURN);
        ServerLevel to = null;
        if (!r.isEmpty()) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(r.getString("Dim"));
            if (id != null) to = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id));
        }
        if (to != null && !(Labyrinth.inMega(to) && RiftRules.slotAt(r.getDouble("X"), r.getDouble("Z")) >= 0)) {
            to.getChunk((int) Math.floor(r.getDouble("X")) >> 4, (int) Math.floor(r.getDouble("Z")) >> 4);
            p.teleportTo(to, r.getDouble("X"), r.getDouble("Y"), r.getDouble("Z"), r.getFloat("Yaw"), r.getFloat("Pitch"));
            p.resetFallDistance();
            to.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 1.3f);
            return;
        }
        if (!WaypointService.returnToWaypoint(p) && HubTravel.enabled() && HubTravel.available(server)) HubTravel.returnToPlaza(p);
    }

    // ---------------------------------------------------------------- the clock
    /** Every second: warn, collapse the ones that ran out or were left empty. */
    void tick(ServerLevel mega) {
        long now = mega.getGameTime();
        List<Pocket> gone = new ArrayList<>();
        for (Pocket p : pockets.values()) {
            long left = p.expiresAt - now;
            int warn = RiftRules.warnSeconds(left, p.lastLeft == 0 ? left + 20 : p.lastLeft);
            p.lastLeft = left;
            List<ServerPlayer> in = playersIn(mega, p.slot);
            if (warn > 0) for (ServerPlayer m : in) m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.pocket.collapse", "The realm is collapsing: %s seconds.", warn)
                    .withStyle(warn <= 10 ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE), true);
            boolean empty = in.isEmpty() && now - p.createdAt > 20L * 90L;
            if (left <= 0 || empty) gone.add(p);
        }
        for (Pocket p : gone) collapse(mega, p);
    }

    static List<ServerPlayer> playersIn(ServerLevel mega, int slot) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer m : mega.players()) if (RiftRules.slotAt(m.getX(), m.getZ()) == slot) out.add(m);
        return out;
    }

    /** Throws everybody out, clears the cell and frees the slot. */
    void collapse(ServerLevel mega, Pocket p) {
        for (ServerPlayer m : playersIn(mega, p.slot)) {
            m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.pocket.gone", "The realm folds shut behind you.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            leave(mega, m);
        }
        AABB box = PocketBuilder.bounds(p.slot);
        for (Entity e : mega.getEntitiesOfClass(Entity.class, box, en -> !(en instanceof ServerPlayer))) e.discard();
        PocketBuilder.clear(mega, p.slot);
        pockets.remove(p.slot);
        setDirty();
    }

    /** Collapses a pocket by slot (admin). */
    public boolean collapseSlot(ServerLevel mega, int slot) {
        Pocket p = pockets.get(slot);
        if (p == null) return false;
        collapse(mega, p);
        return true;
    }
}
