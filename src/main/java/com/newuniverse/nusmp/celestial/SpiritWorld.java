package com.newuniverse.nusmp.celestial;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Spirit World is where the Celestial Spirit King is met. It is modelled on the pocket realms of the Mega-Dungeon (a private cell per fight, built on demand, tracked in
 * saved data so a restart does not strand anybody, collapsed when it is over) but lives in a dimension of its own, {@code nusmp:spirit_world}, a void under the star sky of the
 * End. Each fight gets a cell 256 blocks from the next: a disc of white marble and gold, twelve zodiac inlays and twelve pillars of starlight round the rim, a gold circle in the
 * middle (the safe place of the king's Judgement), walls and a roof of invisible barrier, and the way in on the west rim. The king is made there ({@link SpiritKingEntity}), scaled
 * by the EP of the people who came and the size of the party; when he falls every participant is rewarded ({@link KingRewards}), and the realm closes a short while later and
 * sends everybody back where they came from. If every challenger is dead or gone, the king leaves and the realm closes. Server thread only.
 */
public final class SpiritWorld extends SavedData {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String NAME = "nusmp_spirit_world";
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "spirit_world"));
    /** The player tag that remembers where they came from. */
    public static final String RETURN = "nusmp_celestial_return";

    public static final int FLOOR_Y = 100, RADIUS = 26, STRIDE = 256, MAX_SLOTS = 16, HEIGHT = 30;

    /** One live fight. */
    public static final class Realm {
        public int slot;
        public long createdAt, expiresAt, closeAt = -1;
        public boolean won, lost;
        public final List<UUID> members = new ArrayList<>();
        public UUID king;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("Slot", slot);
            t.putLong("Created", createdAt);
            t.putLong("Expires", expiresAt);
            t.putLong("Close", closeAt);
            t.putBoolean("Won", won);
            t.putBoolean("Lost", lost);
            ListTag list = new ListTag();
            for (UUID u : members) list.add(StringTag.valueOf(u.toString()));
            t.put("Members", list);
            if (king != null) t.putString("King", king.toString());
            return t;
        }

        static Realm load(CompoundTag t) {
            Realm r = new Realm();
            r.slot = t.getInt("Slot");
            r.createdAt = t.getLong("Created");
            r.expiresAt = t.getLong("Expires");
            r.closeAt = t.contains("Close") ? t.getLong("Close") : -1;
            r.won = t.getBoolean("Won");
            r.lost = t.getBoolean("Lost");
            ListTag list = t.getList("Members", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) try { r.members.add(UUID.fromString(list.getString(i))); } catch (IllegalArgumentException ignored) { }
            if (t.contains("King")) try { r.king = UUID.fromString(t.getString("King")); } catch (IllegalArgumentException ignored) { }
            return r;
        }
    }

    private final Map<Integer, Realm> realms = new LinkedHashMap<>();

    public static SpiritWorld get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(SpiritWorld::new, SpiritWorld::load, null), NAME);
    }

    private static SpiritWorld load(CompoundTag t, HolderLookup.Provider lookup) {
        SpiritWorld w = new SpiritWorld();
        ListTag list = t.getList("Realms", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Realm r = Realm.load(list.getCompound(i));
            w.realms.put(r.slot, r);
        }
        return w;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        for (Realm r : realms.values()) list.add(r.save());
        t.put("Realms", list);
        return t;
    }

    public Map<Integer, Realm> realms() { return realms; }

    public static ServerLevel level(MinecraftServer server) { return server == null ? null : server.getLevel(DIMENSION); }

    public static boolean in(Level l) { return l != null && l.dimension().equals(DIMENSION); }

    public static int centerX(int slot) { return slot * STRIDE; }

    public static final int CENTER_Z = 0;

    public static Vec3 center(int slot) { return new Vec3(centerX(slot) + 0.5, FLOOR_Y + 1, CENTER_Z + 0.5); }

    public static Vec3 entry(int slot) { return new Vec3(centerX(slot) - RADIUS + 4.5, FLOOR_Y + 1, CENTER_Z + 0.5); }

    // ================================================================ events
    public static void register(IEventBus gameBus) { gameBus.addListener(SpiritWorld::onTick); }

    private static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 20 != 0) return;
        ServerLevel level = level(e.getServer());
        if (level == null) return;
        SpiritWorld w = get(e.getServer());
        if (!w.realms.isEmpty()) w.tick(level);
    }

    // ================================================================ the challenge
    /** Opens a realm for {@code challenger} and the allies beside them and takes them in. Returns false (and says why) when it could not. */
    public static boolean enter(ServerPlayer challenger, List<ServerPlayer> group) {
        MinecraftServer server = challenger.getServer();
        ServerLevel level = level(server);
        if (level == null) {
            challenger.displayClientMessage(Component.translatableWithFallback("message.nusmp.celestial.noworld", "The Spirit World is closed: its dimension is not loaded.").withStyle(ChatFormatting.RED), false);
            return false;
        }
        SpiritWorld w = get(server);
        int slot = w.freeSlot();
        if (slot < 0) {
            challenger.displayClientMessage(Component.translatableWithFallback("message.nusmp.celestial.full", "Every cell of the Spirit World is in use. Wait a little.").withStyle(ChatFormatting.RED), false);
            return false;
        }
        long now = server.overworld().getGameTime();
        Realm r = new Realm();
        r.slot = slot;
        r.createdAt = now;
        r.expiresAt = now + KingRules.REALM_TICKS;
        build(level, slot);
        double ep = 0;
        for (ServerPlayer p : group) ep += BalanceLaw.epScale(p);
        ep /= Math.max(1, group.size());
        SpiritKingEntity king = CelestialEntities.SPIRIT_KING.get().create(level);
        if (king == null) {
            clear(level, slot);
            challenger.displayClientMessage(Component.translatableWithFallback("message.nusmp.celestial.noking", "The King does not answer.").withStyle(ChatFormatting.RED), false);
            return false;
        }
        Vec3 c = center(slot);
        king.moveTo(c.x + 6, c.y, c.z, 90f, 0f);
        king.configure(ep, group.size(), c, slot);
        king.finalizeSpawn(level, level.getCurrentDifficultyAt(king.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null);
        king.setPersistenceRequired();
        level.addFreshEntity(king);
        r.king = king.getUUID();
        for (ServerPlayer p : group) {
            r.members.add(p.getUUID());
            CelestialData d = CelestialAttachments.of(p);
            d.fought();
            d.setKingCooldown(now + KingRules.refightCooldown(false));
            d.setGateOpenUntil(0);
            CompoundTag back = new CompoundTag();
            back.putString("Dim", p.level().dimension().location().toString());
            back.putDouble("X", p.getX());
            back.putDouble("Y", p.getY());
            back.putDouble("Z", p.getZ());
            back.putFloat("Yaw", p.getYRot());
            back.putFloat("Pitch", p.getXRot());
            p.getPersistentData().put(RETURN, back);
            Vec3 at = entry(slot);
            p.teleportTo(level, at.x, at.y, at.z, -90f, 0f);
            p.resetFallDistance();
            p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.celestial.enter",
                    "The Gate closes behind you. The Celestial Spirit King waits in the middle of the stars.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            CelestialNet.sync(p);
        }
        w.realms.put(slot, r);
        w.setDirty();
        level.playSound(null, BlockPos.containing(c), SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 2.0f, 0.7f);
        return true;
    }

    private int freeSlot() {
        for (int i = 0; i < MAX_SLOTS; i++) if (!realms.containsKey(i)) return i;
        return -1;
    }

    // ================================================================ the clock
    void tick(ServerLevel level) {
        long now = level.getServer().overworld().getGameTime();
        List<Realm> gone = new ArrayList<>();
        for (Realm r : realms.values()) {
            List<ServerPlayer> in = playersIn(level, r.slot);
            if (r.closeAt >= 0) {
                long left = r.closeAt - now;
                if (left > 0 && left % 100 < 20 && left <= 400) for (ServerPlayer p : in) p.displayClientMessage(Component.translatableWithFallback("message.nusmp.celestial.closing", "The Spirit World is closing: %s s.", Math.max(1, left / 20)).withStyle(ChatFormatting.GRAY), true);
                if (left <= 0) gone.add(r);
                continue;
            }
            Entity king = r.king == null ? null : level.getEntity(r.king);
            boolean alive = king != null && king.isAlive();
            boolean anyoneFighting = false;
            for (ServerPlayer p : in) if (p.isAlive() && !p.isSpectator()) anyoneFighting = true;
            if (alive && !anyoneFighting && now - r.createdAt > 200) {                         // everybody is dead or gone: the king keeps the Gate
                r.lost = true;
                r.closeAt = now + 100;
                if (king != null) king.discard();
                setDirty();
            } else if (!alive && !r.won && now - r.createdAt > 200) {                          // the king vanished some other way: close
                r.closeAt = now + 100;
                setDirty();
            } else if (now >= r.expiresAt) {
                r.lost = true;
                r.closeAt = now + 100;
                if (king != null) king.discard();
                setDirty();
            }
        }
        for (Realm r : gone) collapse(level, r);
    }

    static List<ServerPlayer> playersIn(ServerLevel level, int slot) {
        double cx = centerX(slot);
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (Math.abs(p.getX() - cx) < STRIDE / 2.0 && Math.abs(p.getZ() - CENTER_Z) < STRIDE / 2.0) out.add(p);
        return out;
    }

    /** The king has fallen: reward every member, set the long cooldown, close the realm shortly. */
    public static void kingDown(SpiritKingEntity king) {
        if (!(king.level() instanceof ServerLevel level)) return;
        SpiritWorld w = get(level.getServer());
        Realm r = w.realms.get(king.realmSlot());
        if (r == null) return;
        long now = level.getServer().overworld().getGameTime();
        r.won = true;
        r.closeAt = now + 20 * 25;
        for (ServerPlayer p : playersIn(level, r.slot)) {
            if (!r.members.contains(p.getUUID())) continue;
            KingRewards.victory(p);
            Component leave = Component.translatableWithFallback("message.nusmp.celestial.leave", "[Return to the world]").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/multiverse celestial leave")));
            p.sendSystemMessage(leave);
        }
        w.setDirty();
    }

    // ================================================================ leaving and collapsing
    /** Sends a player back to where they came from (or the world spawn when it is gone). */
    public static void leave(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        CompoundTag back = p.getPersistentData().getCompound(RETURN);
        ServerLevel to = null;
        double x, y, z;
        float yaw = 0, pitch = 0;
        if (!back.isEmpty()) {
            to = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(back.getString("Dim"))));
            x = back.getDouble("X"); y = back.getDouble("Y"); z = back.getDouble("Z"); yaw = back.getFloat("Yaw"); pitch = back.getFloat("Pitch");
        } else { x = 0; y = 0; z = 0; }
        if (to == null) {
            to = server.overworld();
            BlockPos s = to.getSharedSpawnPos();
            x = s.getX() + 0.5; y = s.getY(); z = s.getZ() + 0.5;
        }
        p.getPersistentData().remove(RETURN);
        p.teleportTo(to, x, y, z, yaw, pitch);
        p.resetFallDistance();
    }

    private void collapse(ServerLevel level, Realm r) {
        for (ServerPlayer p : playersIn(level, r.slot)) leave(p);
        for (Entity e : level.getEntitiesOfClass(Entity.class, bounds(r.slot), x -> !(x instanceof ServerPlayer))) e.discard();
        clear(level, r.slot);
        realms.remove(r.slot);
        setDirty();
    }

    // ================================================================ the realm itself
    static AABB bounds(int slot) {
        double cx = centerX(slot);
        return new AABB(cx - RADIUS - 5, FLOOR_Y - 3, CENTER_Z - RADIUS - 5, cx + RADIUS + 5, FLOOR_Y + HEIGHT + 3, CENTER_Z + RADIUS + 5);
    }

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** The disc, its inlays, its pillars and its invisible walls and roof. */
    static void build(ServerLevel level, int slot) {
        int cx = centerX(slot), cz = CENTER_Z;
        for (int x = cx - RADIUS - 5; x <= cx + RADIUS + 5; x += 16) for (int z = cz - RADIUS - 5; z <= cz + RADIUS + 5; z += 16) level.getChunk(x >> 4, z >> 4);
        clear(level, slot);
        for (Entity stray : level.getEntitiesOfClass(Entity.class, bounds(slot), e -> !(e instanceof ServerPlayer))) stray.discard();
        BlockState marble = Blocks.SMOOTH_QUARTZ.defaultBlockState(), dark = Blocks.POLISHED_DIORITE.defaultBlockState(), gold = Blocks.GOLD_BLOCK.defaultBlockState(),
                lapis = Blocks.LAPIS_BLOCK.defaultBlockState(), ame = Blocks.AMETHYST_BLOCK.defaultBlockState(), lamp = Blocks.SEA_LANTERN.defaultBlockState(),
                pillar = Blocks.QUARTZ_PILLAR.defaultBlockState(), barrier = Blocks.BARRIER.defaultBlockState();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            double r = Math.sqrt(dx * dx + dz * dz);
            if (r > RADIUS) continue;
            BlockState s = marble;
            if (r <= 5.0) s = r >= 4.0 ? gold : Blocks.WHITE_CONCRETE.defaultBlockState();       // the safe circle: a gold rim round white
            else if (r >= RADIUS - 1.5) s = lapis;
            else if (Math.abs(r - 18) < 0.7) s = ((int) Math.floor((Math.atan2(dz, dx) + Math.PI) / (Math.PI / 12))) % 2 == 0 ? gold : ame;   // twelve inlays on the zodiac ring
            else if (((dx + dz) & 1) == 0) s = dark;
            set(level, cx + dx, FLOOR_Y, cz + dz, s);
            if (r <= RADIUS - 0.5) set(level, cx + dx, FLOOR_Y - 1, cz + dz, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        }
        for (int i = 0; i < 12; i++) {                                                          // twelve pillars of starlight on the rim
            double a = i * Math.PI / 6;
            int px = cx + (int) Math.round(Math.cos(a) * (RADIUS - 2)), pz = cz + (int) Math.round(Math.sin(a) * (RADIUS - 2));
            for (int h = 1; h <= 8; h++) set(level, px, FLOOR_Y + h, pz, pillar);
            set(level, px, FLOOR_Y + 9, pz, lamp);
            set(level, px, FLOOR_Y + 10, pz, Blocks.END_ROD.defaultBlockState());
        }
        for (int a = 0; a < 360; a += 1) {                                                      // the invisible wall
            int wx = cx + (int) Math.round(Math.cos(Math.toRadians(a)) * (RADIUS + 1)), wz = cz + (int) Math.round(Math.sin(Math.toRadians(a)) * (RADIUS + 1));
            for (int h = 1; h <= HEIGHT; h++) set(level, wx, FLOOR_Y + h, wz, barrier);
        }
        for (int dx = -RADIUS; dx <= RADIUS; dx++) for (int dz = -RADIUS; dz <= RADIUS; dz++)
            if (dx * dx + dz * dz <= (RADIUS + 1) * (RADIUS + 1)) set(level, cx + dx, FLOOR_Y + HEIGHT, cz + dz, barrier);        // and the roof
    }

    static void clear(ServerLevel level, int slot) {
        AABB b = bounds(slot);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = (int) b.minX; x < (int) b.maxX; x++) for (int z = (int) b.minZ; z < (int) b.maxZ; z++) for (int y = (int) b.minY; y < (int) b.maxY; y++) {
            m.set(x, y, z);
            if (!level.getBlockState(m).isAir()) level.setBlock(m, AIR, 2 | 16 | 32);
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState s) { level.setBlock(new BlockPos(x, y, z), s, 2 | 16); }

    /** Spawns the door of light at a player's feet (the Gate) for the ritual. */
    public static void showGate(ServerPlayer p) {
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 at = CelestialProps.standingPlace(p.serverLevel(), p.position().add(look.scale(4.0)), p.getY());
        MagicProps.spawn(p.serverLevel(), PropKind.CELESTIAL_2, at, p.getYRot() + 180f, 1.5f, 20 * 14, 0, p);
    }
}
