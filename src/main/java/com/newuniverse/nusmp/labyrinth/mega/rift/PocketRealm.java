package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.labyrinth.mega.hub.HubTravel;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyService;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemSync;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemText;
import com.newuniverse.nusmp.labyrinth.mega.system.ToastKind;
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
import org.slf4j.Logger;

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
    private static final Logger LOG = LogUtils.getLogger();
    public static final String NAME = "nusmp_rift_pockets";
    /** Persistent data of a player inside a pocket: where they came from (dimension, position, rotation), and which slot. */
    public static final String RETURN = "nusmp_pocket_return";

    /** One live pocket. */
    public static final class Pocket {
        public int slot, floor;
        public String variant = "boss";
        public long seed, createdAt, expiresAt;
        public boolean bossDead, solved;
        /** 1.94.0: the game time the vault was opened (boss dead, puzzle solved, or an admin's skip); -1 while it has not been. */
        public long clearedAt = -1;
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
            if (clearedAt >= 0) t.putLong("ClearedAt", clearedAt);
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
            p.clearedAt = t.contains("ClearedAt") ? t.getLong("ClearedAt") : -1;
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
    /**
     * A player stepped into a pocket rift: build the realm if this rift has none yet, and take them (and the party mates beside them) in. Since 1.94.0: a rift whose slot was freed and given to another
     * rift no longer opens that other rift's realm (the seeds are compared); a rift outside the Mega-Dungeon does not read a sector from its coordinates (they mean nothing there); a missing dimension
     * says so; a build that fails clears what it built; and each player is remembered with the rift they came through, so the way out puts them beside it and not inside it.
     */
    static void enter(ServerLevel from, ServerPlayer p, DimensionalRiftBlockEntity rift) {
        MinecraftServer server = from.getServer();
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null) {
            WaypointService.say(p, "pocket.nodim", "The hidden realm has no ground to stand on: the dungeon's dimension is not loaded.", ChatFormatting.GRAY);
            return;
        }
        Sector here = RiftService.inMega(from) ? MegaData.get(server).sectorAt(rift.getBlockPos()) : null;
        if (here != null && !MegaDungeon.mayEnter(p, here.polarity)) return;                    // the lock says why
        PocketRealm realm = get(server);
        Pocket pocket = rift.slot() >= 0 ? realm.pockets.get(rift.slot()) : null;
        if (pocket != null && pocket.seed != rift.seed()) pocket = null;                         // the slot was freed and given to another rift: this rift's realm is gone
        if (pocket == null) pocket = realm.create(mega, from, rift, here);
        if (pocket == null) {
            WaypointService.say(p, "pocket.full", "Every hidden realm is in use. Try again in a while.", ChatFormatting.GRAY);
            return;
        }
        List<ServerPlayer> group = new ArrayList<>();
        group.add(p);
        for (ServerPlayer o : from.players())
            if (o != p && o.distanceToSqr(p) <= 36 && !o.isSpectator() && PartyService.partyOf(o) == PartyService.partyOf(p) && (here == null || MegaDungeon.mayEnter(o, here.polarity))) group.add(o);
        for (ServerPlayer m : group) {
            CompoundTag r = new CompoundTag();
            r.putString("Dim", m.level().dimension().location().toString());
            r.putDouble("X", m.getX());
            r.putDouble("Y", m.getY());
            r.putDouble("Z", m.getZ());
            r.putFloat("Yaw", m.getYRot());
            r.putFloat("Pitch", m.getXRot());
            r.putInt("Slot", pocket.slot);
            r.putInt("RX", rift.getBlockPos().getX());                                           // 1.94.0: the rift they came through, so the way out can stand clear of it
            r.putInt("RY", rift.getBlockPos().getY());
            r.putInt("RZ", rift.getBlockPos().getZ());
            m.getPersistentData().put(RETURN, r);
            BlockPos e = pocket.entry;
            mega.getChunk(e.getX() >> 4, e.getZ() >> 4);
            move(m, mega, e.getX() + 0.5, e.getY(), e.getZ() + 0.5, -90f, 0f);
            m.resetFallDistance();
            RiftService.title(m, Component.translatableWithFallback("message.nusmp.mega.rift.pocket.title", "A hidden realm"),
                    Component.translatableWithFallback("message.nusmp.mega.rift.pocket." + pocket.variant, pocket.variant.equals("boss") ? "Something old guards what is behind it." : "Press the plates in the order the wall shows."));
            mega.playSound(null, e, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.4f, 1.4f);
            NusmpEvents.fire(m, "mega.pocket", pocket.variant);
        }
    }

    /**
     * Moves a player. A real player goes through {@code teleportTo} (which changes dimension); a stand-in of a self-test (a fake player has no connection, so cannot change dimension and is not moved by the
     * packet) is put down with {@code moveTo} when it is already in that level. False when it could not be moved.
     */
    static boolean move(ServerPlayer p, ServerLevel to, double x, double y, double z, float yaw, float pitch) {
        if (p instanceof net.neoforged.neoforge.common.util.FakePlayer || p.connection == null) {
            if (p.level() != to) return false;
            p.moveTo(x, y, z, yaw, pitch);
            return true;
        }
        p.teleportTo(to, x, y, z, yaw, pitch);
        return true;
    }

    private Pocket create(ServerLevel mega, ServerLevel from, DimensionalRiftBlockEntity rift, Sector s) {
        int slot = RiftRules.firstFreeSlot(pockets.keySet(), RiftRules.MAX_SLOTS);
        if (slot < 0) return null;
        int floor = s == null ? 1 : s.firstFloor();
        Pocket p = new Pocket();
        p.slot = slot;
        p.floor = floor;
        p.seed = rift.seed();
        p.variant = RiftRules.variantFor(rift.seed(), RiftConfig.get(RiftConfig.BOSS_SHARE)).id();
        p.createdAt = mega.getGameTime();
        p.expiresAt = p.createdAt + 20L * 60L * RiftConfig.get(RiftConfig.POCKET_MINUTES);
        try {
            PocketBuilder.build(mega, p, s == null ? com.newuniverse.nusmp.labyrinth.mega.RegionTheme.GRAVITY : s.theme);
        } catch (RuntimeException | LinkageError ex) {
            PocketBuilder.clear(mega, slot);                                                     // 1.94.0: a half-built realm is cleared, not left in the cell
            throw ex;
        }
        pockets.put(slot, p);
        setDirty();
        rift.aimPocket(slot);
        return p;
    }

    /** Where a player is put down when they come out of a pocket, as {x, y, z}: where they stood going in, unless that is inside the tear they went through, then the nearest free spot on a ring round it. */
    static double[] returnSpot(ServerLevel to, CompoundTag r) {
        double x = r.getDouble("X"), y = r.getDouble("Y"), z = r.getDouble("Z");
        if (!r.contains("RX")) return new double[]{x, y, z};
        int rx = r.getInt("RX"), ry = r.getInt("RY"), rz = r.getInt("RZ");
        if (!RiftRules.inTear(rx, ry, rz, x, y, z)) return new double[]{x, y, z};
        for (double[] c : RiftRules.exitRing(rx, rz, x, z, RiftRules.EXIT_RING, r.getFloat("Yaw"))) {
            int bx = (int) Math.floor(c[0]), bz = (int) Math.floor(c[1]);
            for (int fy = ry + 1; fy >= ry - 2; fy--) {
                BlockPos feet = new BlockPos(bx, fy, bz);
                if (free(to, feet)) return new double[]{c[0], fy, c[1]};
            }
        }
        return new double[]{x, y, z};                                                            // closed in on every side: the old spot, the tear's own cooldown still holds
    }

    private static boolean free(ServerLevel l, BlockPos feet) {
        return l.getBlockState(feet).getCollisionShape(l, feet).isEmpty() && l.getBlockState(feet.above()).getCollisionShape(l, feet.above()).isEmpty()
                && !l.getBlockState(feet.below()).getCollisionShape(l, feet.below()).isEmpty() && l.getBlockState(feet).getFluidState().isEmpty();
    }

    /** The exit: back to where the player came from; if that is gone, their waypoint; if that is gone too, Floor 0, and at the very last the labyrinth's own way out. */
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
            double[] at = returnSpot(to, r);
            if (move(p, to, at[0], at[1], at[2], r.getFloat("Yaw"), r.getFloat("Pitch"))) {
                p.resetFallDistance();
                to.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 1.3f);
                return;
            }
        }
        if (WaypointService.returnToWaypoint(p)) return;
        if (HubTravel.enabled() && HubTravel.available(server)) HubTravel.returnToPlaza(p);
        else LabyrinthTravel.leave(p);                                                           // 1.94.0: nothing to return to and no Hub: the labyrinth's own way out (origin, bed or world spawn), not the void under a cleared cell
    }

    // ---------------------------------------------------------------- clearing: the vault opens
    /**
     * The vault of a pocket opens (the secret boss died, the puzzle was solved, the boss was lost, or an admin skipped it): the bars lift, the players inside are told with the System's reward pop-up and the
     * quest event fires. False when it was open already. Every way of clearing goes through here, so they all do the same.
     */
    public boolean clear(ServerLevel level, Pocket pocket, String how) {
        if (pocket.bossDead || pocket.solved) return false;
        boolean boss = pocket.variantKind() == RiftRules.Variant.BOSS;
        if (boss) pocket.bossDead = true;
        else pocket.solved = true;
        pocket.clearedAt = level.getGameTime();
        setDirty();
        PocketBuilder.openVault(level, pocket);
        for (ServerPlayer m : playersIn(level, pocket.slot)) {
            m.displayClientMessage(Component.translatableWithFallback(boss ? "message.nusmp.mega.rift.boss.dead" : "message.nusmp.mega.rift.puzzle.solved",
                    boss ? "The guardian falls. The bars lift. The vault is open." : "The bars lift. The vault is open.").withStyle(ChatFormatting.GOLD), true);
            SystemSync.toast(m, ToastKind.REWARD, SystemText.TOAST_RIFT_CLEARED);
            NusmpEvents.fire(m, "mega.pocket_cleared", pocket.variant);
        }
        LOG.debug("[nusmp] The pocket in slot {} was cleared ({}).", pocket.slot, how);
        return true;
    }

    // ---------------------------------------------------------------- the clock
    /** Every second: warn, collapse the ones that ran out or were left empty, and make sure a secret boss that went missing does not lock the vault for good. */
    void tick(ServerLevel mega) {
        long now = mega.getGameTime();
        List<Pocket> gone = new ArrayList<>();
        for (Pocket p : new ArrayList<>(pockets.values())) {
            long left = p.expiresAt - now;
            int warn = RiftRules.warnSeconds(left, p.lastLeft == 0 ? left + 20 : p.lastLeft);
            p.lastLeft = left;
            List<ServerPlayer> in = playersIn(mega, p.slot);
            if (warn > 0) for (ServerPlayer m : in) m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.pocket.collapse", "The realm is collapsing: %s seconds.", warn)
                    .withStyle(warn <= 10 ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE), true);
            if (!in.isEmpty()) watchBoss(mega, p, now);
            boolean cleared = p.bossDead || p.solved;
            long sinceCleared = p.clearedAt < 0 ? now - p.createdAt : now - p.clearedAt;
            if (left <= 0 || RiftRules.collapseEmpty(in.isEmpty(), cleared, now - p.createdAt, sinceCleared)) gone.add(p);
        }
        for (Pocket p : gone) collapse(mega, p);
    }

    /** A boss pocket with people in it but no secret boss in its arena (it never spawned, or something removed it): the vault opens, the delvers are not locked out of what they came for. */
    private void watchBoss(ServerLevel mega, Pocket p, long now) {
        if (p.variantKind() != RiftRules.Variant.BOSS || p.bossDead || now - p.createdAt < 100) return;
        boolean alive = !mega.getEntitiesOfClass(LabyrinthGuardianEntity.class, PocketBuilder.bounds(p.slot), g -> g.isAlive() && g.getTags().contains(PocketBuilder.BOSS_TAG)).isEmpty();
        if (!alive) clear(mega, p, "boss_lost");
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
