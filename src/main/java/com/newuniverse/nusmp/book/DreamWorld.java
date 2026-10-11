package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Dream Magic's pocket dimension (0.40), after the Black Clover wiki: Dorothy Unsworth's Glamour World pulls her targets out of
 * reality into a dream that is hers to shape. Whatever she imagines (says) inside it becomes solid; staying in it wears the
 * dreamers' minds down until they break; and the only ways out are to break the dream itself, with enough coordinated damage
 * from the inside or with spatial / time / anti-magic that tears its fabric, or to outlast it.
 *
 * <p><b>The dimension:</b> {@code nusmp:dream} (data/nusmp/dimension), an empty void with a fixed starry dusk sky and a pastel
 * fog (client {@code DreamSkyClient}). Each dream gets its own arena in it, 512 blocks apart: a disc of pastel wool and lanterns,
 * cotton-candy trees, flowers and cakes, under a dome of iridescent stained glass, all rebuilt when the arena is reused.
 *
 * <p><b>A dream:</b>
 * <ol>
 *   <li>Cast: pastel mist and starlight swirl out from the caster ({@link VfxShape#DREAM_TRANSITION}) and close over everyone
 *       caught; 1.25 s later they are inside, the caster at the south edge, the dreamers spread round the middle, under the
 *       dome ({@link VfxShape#DREAM_DOME}).</li>
 *   <li>Every second each dreamer's <b>dream load</b> rises (shown on their action bar). At 100 their mind breaks: blind,
 *       nauseous, nearly unable to move, magicules drained and hurt, then the load falls back to 30 and climbs again.</li>
 *   <li><b>Manifestation:</b> the caster speaks a thing into being (chat: bear, feast, fire, ice, cage, stars, wall, sleep...),
 *       or casts Imagination Manifestation; swirling smoke solidifies into it ({@link VfxShape#DREAM_MANIFEST}).</li>
 *   <li><b>Breakout:</b> the dreamers' combined damage inside the dream reaching 30 + 12 per dreamer, or any of them
 *       teleporting (ender pearl, chorus fruit, a spatial skill) or casting Spatial, Time or Anti-Magic, shatters it
 *       ({@link VfxShape#DREAM_SHATTER}); the caster reels from the backlash.</li>
 *   <li>It ends when its time runs out, when it shatters, or when the caster dies or leaves; everyone goes back exactly where
 *       they were. A player who logs out inside is sent back when they log in.</li>
 * </ol>
 * Bosses cannot be pulled in. A dream holds at most 8 dreamers.
 */
public final class DreamWorld {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("nusmp", "dream"));
    public static final int PASTEL = 0xFFF4A8E0;
    private static final int SLOTS = 64, SPACING = 512, FLOOR_Y = 100;
    private static final String RETURN = "nusmp_dream_return";

    /** Where someone stood before the dream. */
    private record Return(ResourceKey<Level> dim, Vec3 pos, float yRot, float xRot) {}

    static final class Session {
        final UUID caster;
        final GrimoireBook book;
        final ManasSkillInstance inst;
        final int mode, slot, radius;
        final boolean grand;
        final BlockPos center;
        final long end;
        final Map<UUID, Return> returns = new HashMap<>();
        final Map<UUID, Float> load = new HashMap<>();
        final List<UUID> dreamers = new ArrayList<>();
        float breakDamage;
        long lastManifest = -100;
        int nextManifest;
        boolean over;

        Session(UUID caster, GrimoireBook book, ManasSkillInstance inst, int mode, int slot, int radius, boolean grand, long end) {
            this.caster = caster; this.book = book; this.inst = inst; this.mode = mode; this.slot = slot; this.radius = radius;
            this.grand = grand; this.end = end;
            this.center = new BlockPos(slot * SPACING + SPACING / 2, FLOOR_Y, SPACING / 2);
        }

        float breakNeed() { return 30f + 12f * Math.max(1, dreamers.size()); }
    }

    private static final Map<UUID, Session> BY_CASTER = new HashMap<>();
    private static final Map<UUID, Session> BY_MEMBER = new HashMap<>();
    private static final boolean[] USED = new boolean[SLOTS];
    private static boolean moving;                 // our own teleports must not shatter a dream

    private DreamWorld() {}

    public static boolean inDream(Entity e) { return e.level().dimension() == DIMENSION; }

    public static boolean isDreaming(ServerPlayer caster) { return BY_CASTER.containsKey(caster.getUUID()); }

    // ================================================================ opening a dream
    /**
     * Opens a dream. Grand = Glamour World (Dorothy): a big dome, up to 8 dreamers within 12 blocks, 30 s. Otherwise Dream World:
     * a small dome, the nearest 2 foes within 8 blocks, 15 s.
     */
    public static boolean open(GrimoireBook b, ManasSkillInstance i, ServerPlayer caster, int mode, boolean grand) {
        MinecraftServer server = caster.getServer();
        ServerLevel dream = server.getLevel(DIMENSION);
        if (dream == null) { GrimoireBook.fail(caster, "The dream world will not open here (the nusmp:dream dimension is missing)."); return false; }
        if (inDream(caster) || BY_CASTER.containsKey(caster.getUUID()) || BY_MEMBER.containsKey(caster.getUUID())) {
            GrimoireBook.fail(caster, "You are already dreaming.");
            return false;
        }
        double reach = grand ? 12 : 8;
        List<LivingEntity> caught = new ArrayList<>(GrimoireBook.around(caster, caster.position(), reach));
        caught.removeIf(e -> BalanceLaw.isBoss(e) || BY_MEMBER.containsKey(e.getUUID()) || BY_CASTER.containsKey(e.getUUID()) || inDream(e));
        caught.sort((x, y) -> Double.compare(x.distanceToSqr(caster), y.distanceToSqr(caster)));
        int max = grand ? 8 : 2;
        if (caught.size() > max) caught = new ArrayList<>(caught.subList(0, max));
        if (caught.isEmpty()) { GrimoireBook.fail(caster, "There is no one here to pull into your dream."); return false; }
        int slot = freeSlot();
        if (slot < 0) { GrimoireBook.fail(caster, "Too many dreams are open in the world right now."); return false; }

        int ticks = grand ? 600 : 300;
        Session s = new Session(caster.getUUID(), b, i, mode, slot, grand ? 18 : 11, grand, server.overworld().getGameTime() + 25 + ticks);
        USED[slot] = true;
        BY_CASTER.put(caster.getUUID(), s);
        for (LivingEntity e : caught) BY_MEMBER.put(e.getUUID(), s);           // reserved now, so no one else can take them

        // the transition: pastel mist and starlight swirl out and close over everyone caught
        ServerLevel here = caster.serverLevel();
        VfxSpawn.send(here, VfxShape.DREAM_TRANSITION, caster.position(), caster.position().add(0, 1, 0), PASTEL, 30, (float) reach);
        for (LivingEntity e : caught) VfxSpawn.send(here, VfxShape.DREAM_MANIFEST, e.position(), e.position().add(0, 1, 0), 0xFFC8B0FF, 25, 1.2f);
        here.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4f, 0.6f);
        here.playSound(null, caster.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 1f, 1.3f);
        List<LivingEntity> pulled = caught;
        SpellRuntime.later(here, 25, () -> enter(server, dream, s, caster, pulled));
        return true;
    }

    private static int freeSlot() {
        for (int k = 0; k < SLOTS; k++) if (!USED[k]) return k;
        return -1;
    }

    private static void enter(MinecraftServer server, ServerLevel dream, Session s, ServerPlayer caster, List<LivingEntity> caught) {
        if (s.over || BY_CASTER.get(s.caster) != s) return;
        if (!caster.isAlive() || caster.hasDisconnected()) { abort(s); return; }
        ServerLevel origin = caster.serverLevel();
        buildArena(dream, s);
        Vec3 c = Vec3.atBottomCenterOf(s.center.above());
        moving = true;
        try {
            // the caster, at the south edge of her dream, looking in
            s.returns.put(caster.getUUID(), new Return(caster.level().dimension(), caster.position(), caster.getYRot(), caster.getXRot()));
            remember(caster, caster.level().dimension(), caster.position());
            Vec3 at = c.add(0, 0, s.radius * 0.6);
            caster.teleportTo(dream, at.x, at.y, at.z, 180f, 0f);
            int n = 0;
            for (LivingEntity e : caught) {
                BY_MEMBER.remove(e.getUUID());
                if (!e.isAlive() || e.level() != origin) continue;
                if (e instanceof ServerPlayer sp && sp.hasDisconnected()) continue;
                float ang = Mth.TWO_PI * n / Math.max(1, caught.size()) + Mth.PI;
                Vec3 to = c.add(Mth.sin(ang) * s.radius * 0.35, 0, -Mth.cos(ang) * s.radius * 0.35 - s.radius * 0.1);
                Return back = new Return(e.level().dimension(), e.position(), e.getYRot(), e.getXRot());
                Entity moved;
                if (e instanceof ServerPlayer sp) {
                    remember(sp, back.dim(), back.pos());
                    sp.teleportTo(dream, to.x, to.y, to.z, 0f, 0f);
                    moved = sp;
                } else {
                    moved = e.changeDimension(new DimensionTransition(dream, to, Vec3.ZERO, 0f, 0f, DimensionTransition.DO_NOTHING));
                }
                if (moved == null) continue;
                s.returns.put(moved.getUUID(), back);
                s.dreamers.add(moved.getUUID());
                s.load.put(moved.getUUID(), 0f);
                BY_MEMBER.put(moved.getUUID(), s);
                if (moved instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("You are inside " + caster.getName().getString()
                        + "'s dream. Break it from within, or your mind will break first.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
                n++;
            }
        } finally {
            moving = false;
        }
        if (s.dreamers.isEmpty()) { end(server, s, Ending.EMPTY); return; }
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) (s.end - server.overworld().getGameTime()), 0, true, false));
        caster.displayClientMessage(Component.literal(s.grand ? "Glamour World. Speak, and it becomes real." : "Dream World. Speak, and it becomes real.")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), false);
        int left = (int) (s.end - server.overworld().getGameTime());
        VfxSpawn.send(dream, VfxShape.DREAM_DOME, c, c.add(0, s.radius, 0), PASTEL, left, s.radius);
        VfxSpawn.send(dream, VfxShape.DREAM_TRANSITION, c, c.add(0, 1, 0), PASTEL, 30, s.radius * 0.8f);
        dream.playSound(null, s.center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2f, 0.7f);
    }

    /** The caster could not finish the spell: release the reserved dreamers and the arena. */
    private static void abort(Session s) {
        boolean active = BY_CASTER.remove(s.caster, s);
        BY_MEMBER.values().removeIf(x -> x == s);
        if (active) USED[s.slot] = false;
        s.over = true;
    }

    private static void remember(ServerPlayer p, ResourceKey<Level> dim, Vec3 pos) {
        CompoundTag t = new CompoundTag();
        t.putString("Dim", dim.location().toString());
        t.putDouble("X", pos.x); t.putDouble("Y", pos.y); t.putDouble("Z", pos.z);
        p.getPersistentData().put(RETURN, t);
    }

    // ================================================================ the arena
    private static final Block[] FLOOR = {Blocks.PINK_WOOL, Blocks.WHITE_WOOL, Blocks.LIGHT_BLUE_WOOL, Blocks.MAGENTA_WOOL, Blocks.PINK_WOOL, Blocks.PURPLE_WOOL};
    private static final Block[] GLASS = {Blocks.PINK_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS,
            Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS};
    private static final Block[] FLOWERS = {Blocks.PINK_TULIP, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.WHITE_TULIP};

    /** The dream's ground and sky: rebuilt every time, so whatever the last dream left behind is gone. */
    private static void buildArena(ServerLevel dream, Session s) {
        BlockPos c = s.center;
        int r = s.radius, R = r + 1;
        int flags = Block.UPDATE_CLIENTS;
        for (int cx = (c.getX() - R - 2) >> 4; cx <= (c.getX() + R + 2) >> 4; cx++)
            for (int cz = (c.getZ() - R - 2) >> 4; cz <= (c.getZ() + R + 2) >> 4; cz++) dream.setChunkForced(cx, cz, true);
        RandomSource rand = RandomSource.create(c.asLong() ^ dream.getGameTime());
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -R - 1; dx <= R + 1; dx++) {
            for (int dz = -R - 1; dz <= R + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > R + 1.5) continue;
                // clear the old dream: everything inside the dome
                for (int dy = 0; dy <= R + 1; dy++) {
                    double dd = Math.sqrt(dx * dx + dz * dz + dy * dy);
                    if (dd > R + 1.5) break;
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    BlockState want = dd >= R - 0.5 && dd < R + 0.5 ? GLASS[Math.min(GLASS.length - 1, dy * GLASS.length / Math.max(1, R))].defaultBlockState()
                            : Blocks.AIR.defaultBlockState();
                    if (dream.getBlockState(m) != want) dream.setBlock(m, want, flags);
                }
                if (d > R + 0.5) continue;
                // the ground: rings of pastel wool on a white base, lanterns set into it
                int ring = (int) (d / 2.2);
                BlockState floor = (ring % 3 == 2 && ((int) Math.round(Math.atan2(dz, dx) * 6)) % 4 == 0 ? Blocks.SEA_LANTERN : FLOOR[ring % FLOOR.length]).defaultBlockState();
                dream.setBlock(m.set(c.getX() + dx, c.getY() - 1, c.getZ() + dz), floor, flags);
                dream.setBlock(m.set(c.getX() + dx, c.getY() - 2, c.getZ() + dz), Blocks.WHITE_CONCRETE.defaultBlockState(), flags);
                // flowers and petals
                if (d > 2.5 && d < r - 1.5 && rand.nextFloat() < 0.07f)
                    dream.setBlock(m.set(c.getX() + dx, c.getY(), c.getZ() + dz), FLOWERS[rand.nextInt(FLOWERS.length)].defaultBlockState(), flags);
            }
        }
        // cotton-candy trees and candle cakes round the edge
        int trees = s.grand ? 7 : 4;
        for (int k = 0; k < trees; k++) {
            float a = Mth.TWO_PI * k / trees + rand.nextFloat() * 0.4f;
            int tx = c.getX() + Math.round(Mth.cos(a) * (r - 4)), tz = c.getZ() + Math.round(Mth.sin(a) * (r - 4));
            for (int y = 0; y < 3; y++) dream.setBlock(m.set(tx, c.getY() + y, tz), Blocks.WHITE_WOOL.defaultBlockState(), flags);
            BlockState puff = (k % 2 == 0 ? Blocks.PINK_WOOL : Blocks.LIGHT_BLUE_WOOL).defaultBlockState();
            for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++) for (int oy = 3; oy <= 4; oy++)
                if (Math.abs(ox) + Math.abs(oz) + (oy == 4 ? 1 : 0) <= 2) dream.setBlock(m.set(tx + ox, c.getY() + oy, tz + oz), puff, flags);
            float b = a + Mth.PI / trees;
            dream.setBlock(m.set(c.getX() + Math.round(Mth.cos(b) * (r - 2.5f)), c.getY(), c.getZ() + Math.round(Mth.sin(b) * (r - 2.5f))),
                    Blocks.CANDLE_CAKE.defaultBlockState(), flags);
        }
    }

    private static void releaseArena(ServerLevel dream, Session s) {
        BlockPos c = s.center;
        int R = s.radius + 1;
        for (int cx = (c.getX() - R - 2) >> 4; cx <= (c.getX() + R + 2) >> 4; cx++)
            for (int cz = (c.getZ() - R - 2) >> 4; cz <= (c.getZ() + R + 2) >> 4; cz++) dream.setChunkForced(cx, cz, false);
        USED[s.slot] = false;
    }

    // ================================================================ living in the dream
    public static void onServerTick(ServerTickEvent.Post e) {
        if (BY_CASTER.isEmpty()) return;
        MinecraftServer server = e.getServer();
        long now = server.overworld().getGameTime();
        for (Session s : new ArrayList<>(BY_CASTER.values())) {
            if (s.over || s.dreamers.isEmpty() && s.returns.isEmpty()) continue;        // still opening
            ServerPlayer caster = server.getPlayerList().getPlayer(s.caster);
            if (caster == null || !caster.isAlive() || !inDream(caster)) { end(server, s, Ending.CASTER_GONE); continue; }
            if (now >= s.end) { end(server, s, Ending.FADED); continue; }
            if (now % 20 != 0) continue;
            ServerLevel dream = server.getLevel(DIMENSION);
            if (dream == null) continue;
            float rate = s.grand ? 6f : 8f;
            // Snapshot the dreamer list: mentalBreak() can remove a dreamer or kill them while this tick is running, and the direct
            // iterator from s.dreamers would throw ConcurrentModificationException on the server thread.
            List<UUID> dreamers = new ArrayList<>(s.dreamers);
            for (UUID id : dreamers) {
                if (!s.dreamers.contains(id)) continue;
                Entity ent = dream.getEntity(id);
                if (!(ent instanceof LivingEntity le) || !le.isAlive()) continue;
                float load = s.load.getOrDefault(id, 0f) + rate;
                if (load >= 100f) {
                    mentalBreak(s, caster, le);
                    load = 30f;
                }

                s.load.put(id, load);
                if (le instanceof ServerPlayer sp) sp.displayClientMessage(loadBar(load, s), true);
            }
            // drifting starlight, now and then, so the dream never feels still
            if (now % 60 == 0) {
                Vec3 c = Vec3.atBottomCenterOf(s.center.above());
                RandomSource r = dream.getRandom();
                Vec3 at = c.add((r.nextDouble() - 0.5) * s.radius * 1.4, 0, (r.nextDouble() - 0.5) * s.radius * 1.4);
                VfxSpawn.send(dream, VfxShape.DREAM_MANIFEST, at, at.add(0, 1, 0), 0xFFB8E0FF, 20, 0.6f);
            }
        }
    }

    /** Release static session and forced-chunk state when an integrated or dedicated server closes. */
    public static void onServerStopping(ServerStoppingEvent e) {
        ServerLevel dream = e.getServer().getLevel(DIMENSION);
        for (Session s : new ArrayList<>(BY_CASTER.values())) {
            s.over = true;
            if (dream != null) releaseArena(dream, s);
            else USED[s.slot] = false;
        }
        BY_CASTER.clear();
        BY_MEMBER.clear();
        java.util.Arrays.fill(USED, false);
    }

    private static Component loadBar(float load, Session s) {
        int filled = Math.round(load / 10f);
        StringBuilder bar = new StringBuilder();
        for (int k = 0; k < 10; k++) bar.append(k < filled ? '▮' : '▯');
        int crack = Math.round(Math.min(1f, s.breakDamage / s.breakNeed()) * 100);
        return Component.literal("Dream load " + bar + "  ").withStyle(load > 70 ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal("The dream is " + crack + "% cracked").withStyle(ChatFormatting.AQUA));
    }

    /** The dreamer's mind gives way. */
    private static void mentalBreak(Session s, ServerPlayer caster, LivingEntity t) {
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
        t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2));
        WikiSpells.drainMana(t, 0.1);
        s.book.hurt(s.inst, caster, t, s.mode, 4f + t.getMaxHealth() * 0.05f);
        VfxSpawn.send((ServerLevel) t.level(), VfxShape.DREAM_MANIFEST, t.position(), t.position().add(0, 2, 0), 0xFFFF7AC8, 24, 1.4f);
        if (t instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("Your mind cracks under the dream.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
    }

    // ================================================================ breaking out
    public static void onDamage(LivingDamageEvent.Post e) {
        if (BY_MEMBER.isEmpty() || !(e.getSource().getEntity() instanceof LivingEntity attacker)) return;
        Session s = BY_MEMBER.get(attacker.getUUID());
        if (s == null || s.over || !inDream(attacker)) return;
        s.breakDamage += e.getNewDamage();
        if (s.breakDamage >= s.breakNeed()) {
            MinecraftServer server = attacker.getServer();
            if (server != null) end(server, s, Ending.SHATTERED);
        }
    }

    /** Ender pearls, chorus fruit, spatial skills: tearing space from inside shatters the dream. */
    public static void onTeleport(EntityTeleportEvent e) {
        if (moving || BY_MEMBER.isEmpty()) return;
        Session s = BY_MEMBER.get(e.getEntity().getUUID());
        if (s == null || s.over || !inDream(e.getEntity())) return;
        e.setCanceled(true);
        MinecraftServer server = e.getEntity().getServer();
        if (server != null) end(server, s, Ending.TORN);
    }

    /** A dreamer cast a spell: Spatial, Time and Anti-Magic tear the dream open. */
    public static void onCast(ServerPlayer p, MagicType magic) {
        if (BY_MEMBER.isEmpty() || !inDream(p)) return;
        Session s = BY_MEMBER.get(p.getUUID());
        if (s == null || s.over) return;
        // a second Dream mage competing inside the dream overloads it (wiki: Glamour World collapses)
        if (magic == MagicType.SPATIAL || magic == MagicType.TIME || magic == MagicType.ANTI_MAGIC || magic == MagicType.DREAM) end(p.getServer(), s, Ending.TORN);
    }

    public static void onDeath(LivingDeathEvent e) {
        if (BY_MEMBER.isEmpty() && BY_CASTER.isEmpty()) return;
        UUID id = e.getEntity().getUUID();
        Session s = BY_CASTER.get(id);
        if (s != null && e.getEntity().getServer() != null) { end(e.getEntity().getServer(), s, Ending.CASTER_GONE); return; }
        s = BY_MEMBER.get(id);
        if (s != null) { s.dreamers.remove(id); s.returns.remove(id); BY_MEMBER.remove(id); }
    }

    /** Logged out inside: the dream lets them go (they are sent back when they log in). */
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        Session s = BY_CASTER.get(id);
        if (s != null && e.getEntity().getServer() != null) { end(e.getEntity().getServer(), s, Ending.CASTER_GONE); return; }
        s = BY_MEMBER.remove(id);
        if (s != null) { s.dreamers.remove(id); s.returns.remove(id); }
    }

    /** Woke up still in the dream dimension with no dream around them: back to where they were. */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !inDream(p) || BY_MEMBER.containsKey(p.getUUID()) || BY_CASTER.containsKey(p.getUUID())) return;
        sendBack(p);
    }

    private static void sendBack(ServerPlayer p) {
        CompoundTag t = p.getPersistentData().getCompound(RETURN);
        MinecraftServer server = p.getServer();
        ServerLevel level = null;
        if (t.contains("Dim")) {
            ResourceLocation rl = ResourceLocation.tryParse(t.getString("Dim"));
            if (rl != null) level = server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
        }
        moving = true;
        try {
            if (level != null && level.dimension() != DIMENSION) p.teleportTo(level, t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"), p.getYRot(), p.getXRot());
            else {
                BlockPos spawn = server.overworld().getSharedSpawnPos();
                p.teleportTo(server.overworld(), spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, p.getYRot(), p.getXRot());
            }
        } finally {
            moving = false;
        }
        p.getPersistentData().remove(RETURN);
    }

    // ================================================================ waking up
    enum Ending { FADED, SHATTERED, TORN, CASTER_GONE, EMPTY }

    static void end(MinecraftServer server, Session s, Ending why) {
        if (s.over) return;
        s.over = true;
        ServerLevel dream = server.getLevel(DIMENSION);
        Vec3 c = Vec3.atBottomCenterOf(s.center.above());
        if (dream != null) {
            if (why == Ending.SHATTERED || why == Ending.TORN) {
                VfxSpawn.send(dream, VfxShape.DREAM_SHATTER, c, c.add(0, s.radius, 0), PASTEL, 30, s.radius);
                dream.playSound(null, s.center, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 3f, 0.6f);
                dream.playSound(null, s.center, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 3f, 0.8f);
            } else {
                VfxSpawn.send(dream, VfxShape.DREAM_TRANSITION, c, c.add(0, 1, 0), PASTEL, 30, s.radius * 0.8f);
            }
        }
        ServerPlayer caster = server.getPlayerList().getPlayer(s.caster);
        moving = true;
        try {
            for (Map.Entry<UUID, Return> r : s.returns.entrySet()) {
                Return back = r.getValue();
                ServerLevel to = server.getLevel(back.dim());
                if (to == null) to = server.overworld();
                ServerPlayer sp = server.getPlayerList().getPlayer(r.getKey());
                if (sp != null) {
                    if (inDream(sp)) sp.teleportTo(to, back.pos().x, back.pos().y, back.pos().z, back.yRot(), back.xRot());
                    sp.getPersistentData().remove(RETURN);
                    sp.removeEffect(MobEffects.CONFUSION);
                    continue;
                }
                Entity ent = dream == null ? null : dream.getEntity(r.getKey());
                if (ent != null && ent.isAlive()) ent.changeDimension(new DimensionTransition(to, back.pos(), Vec3.ZERO, back.yRot(), back.xRot(), DimensionTransition.DO_NOTHING));
            }
        } finally {
            moving = false;
        }
        if (caster != null) {
            caster.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            switch (why) {
                case SHATTERED, TORN -> {
                    caster.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                    caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    caster.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                    caster.displayClientMessage(Component.literal(why == Ending.TORN ? "They tore through the fabric of your dream!" : "Your dream shatters from within!")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
                }
                case FADED -> caster.displayClientMessage(Component.literal("The dream fades. Everyone wakes.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                default -> { }
            }
        }
        for (UUID id : s.dreamers) {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null) sp.displayClientMessage(Component.literal(why == Ending.SHATTERED || why == Ending.TORN ? "The dream shatters. You are awake!" : "You wake up.")
                    .withStyle(ChatFormatting.AQUA), false);
        }
        BY_CASTER.remove(s.caster);
        BY_MEMBER.values().removeIf(x -> x == s);
        if (dream != null) releaseArena(dream, s);
        else USED[s.slot] = false;
    }

    // ================================================================ imagination manifestation
    /** What the dreamer can imagine into being; the words are what the caster can say. */
    public enum Manifest {
        TEDDY("a giant stuffed bear", 0xFFE8B890, "bear", "teddy", "stuffed", "plush", "toy"),
        FEAST("a feast", 0xFFFFD6A0, "feast", "food", "cake", "snack", "dinner", "eat"),
        FIRE("a ring of fire", 0xFFFF9A50, "fire", "flame", "burn", "blaze"),
        ICE("an ice prison", 0xFFB8F0FF, "ice", "frost", "freeze", "snow", "cold"),
        CAGE("a bird cage", 0xFFE0E0F0, "cage", "trap", "bird", "prison"),
        STARS("falling stars", 0xFFFFF0A0, "star", "stars", "meteor", "comet", "sky"),
        WALL("a wall of candy", 0xFFFFB8E0, "wall", "shield", "barrier", "candy"),
        SLEEP("a lullaby", 0xFFC8B8FF, "sleep", "pillow", "lullaby", "nap", "dream");

        public final String what;
        public final int color;
        final String[] words;

        Manifest(String what, int color, String... words) { this.what = what; this.color = color; this.words = words; }

        static Manifest parse(String text) {
            String low = " " + text.toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", " ") + " ";
            for (Manifest m : values()) for (String w : m.words) if (low.contains(" " + w + " ") || low.contains(" " + w + "s ")) return m;
            return null;
        }
    }

    /**
     * The caster's words inside her own dream become real (chat). Dorothy also reads minds: what a dreamer says (thinks) can be
     * turned against them, made real by the caster.
     */
    public static void onChat(ServerChatEvent e) {
        ServerPlayer p = e.getPlayer();
        Session own = BY_CASTER.get(p.getUUID());
        Session s = own != null ? own : BY_MEMBER.get(p.getUUID());
        if (s == null || s.over || !inDream(p)) return;
        Manifest m = Manifest.parse(e.getRawText());
        if (m == null) return;
        ServerPlayer caster = p.getServer().getPlayerList().getPlayer(s.caster);
        if (caster == null) return;
        p.getServer().execute(() -> {
            if (own == null) caster.displayClientMessage(Component.literal("You read " + p.getName().getString() + "'s thoughts...").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            manifest(caster, s, m, false);
        });
    }

    /** Imagination Manifestation (the page): the next thing in the caster's imagination, at what she looks at. */
    public static boolean manifestPage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Session s = BY_CASTER.get(p.getUUID());
        if (s == null || s.over || !inDream(p)) { GrimoireBook.fail(p, "You can only make things real inside your own dream."); return false; }
        Manifest m = Manifest.values()[s.nextManifest % Manifest.values().length];
        s.nextManifest++;
        return manifest(p, s, m, true);
    }

    private static boolean manifest(ServerPlayer p, Session s, Manifest m, boolean fromPage) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if (!fromPage && now - s.lastManifest < 40) return false;
        s.lastManifest = now;
        List<LivingEntity> dreamers = new ArrayList<>();
        for (UUID id : s.dreamers) if (level.getEntity(id) instanceof LivingEntity le && le.isAlive()) dreamers.add(le);
        LivingEntity aimed = GrimoireBook.target(p, 30);
        LivingEntity focus = aimed != null && s.dreamers.contains(aimed.getUUID()) ? aimed : nearest(p, dreamers);
        Vec3 at = focus != null ? focus.position() : GrimoireBook.aim(p, 20);
        for (ServerPlayer w : level.getEntitiesOfClass(ServerPlayer.class, new AABB(s.center).inflate(s.radius + 4)))
            w.displayClientMessage(Component.literal("✦ " + p.getName().getString() + " imagines " + m.what + "!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), true);
        VfxSpawn.send(level, VfxShape.DREAM_MANIFEST, at, at.add(0, 2, 0), m.color, 24, m == Manifest.TEDDY || m == Manifest.WALL ? 2.4f : 1.6f);
        level.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.4f);
        int keep = 160;
        switch (m) {
            case TEDDY -> {                                               // a giant stuffed bear drops onto them
                BlockPos base = BlockPos.containing(at);
                bear(level, base, keep);
                for (LivingEntity t : dreamers) if (t.distanceToSqr(at) < 9) {
                    s.book.hurt(s.inst, p, t, s.mode, 10f);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                }
            }
            case FEAST -> {                                               // a table of cakes; the dreamer of the dream is restored
                BlockPos base = BlockPos.containing(p.position().add(p.getLookAngle().multiply(2, 0, 2)));
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                    SpellRuntime.tempBlock(level, base.offset(dx, 0, dz), (dx == 0 && dz == 0 ? Blocks.CANDLE_CAKE : Blocks.CAKE).defaultBlockState(), keep);
                BalanceLaw.heal(p, 8f);
                p.getFoodData().eat(8, 0.8f);
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            }
            case FIRE -> {                                                // a ring of fire round them (campfires: the floor does not burn)
                for (LivingEntity t : dreamers) if (t.distanceToSqr(at) < 16) {
                    for (int k = 0; k < 8; k++) {
                        float a = Mth.TWO_PI * k / 8;
                        SpellRuntime.tempBlock(level, BlockPos.containing(t.position().add(Mth.cos(a) * 2, 0, Mth.sin(a) * 2)), Blocks.CAMPFIRE.defaultBlockState(), 100);
                    }
                    t.igniteForSeconds(5);
                    s.book.hurt(s.inst, p, t, s.mode, 6f);
                }
            }
            case ICE -> {                                                 // an ice shell round each of them
                for (LivingEntity t : dreamers) if (t.distanceToSqr(at) < 25) {
                    BlockPos b = t.blockPosition();
                    for (int dy = 0; dy <= 2; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                        if ((Math.abs(dx) == 1 || Math.abs(dz) == 1 || dy == 2)) SpellRuntime.tempBlock(level, b.offset(dx, dy, dz), Blocks.PACKED_ICE.defaultBlockState(), 100);
                    t.setTicksFrozen(Math.max(t.getTicksFrozen(), 240));
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                }
            }
            case CAGE -> {                                                // a bird cage round the one she looks at
                if (focus != null) {
                    BlockPos b = focus.blockPosition();
                    for (int dy = 0; dy <= 3; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                        boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                        if (dy == 3) SpellRuntime.tempBlock(level, b.offset(dx, dy, dz), Blocks.GOLD_BLOCK.defaultBlockState(), keep);
                        else if (edge) SpellRuntime.tempBlock(level, b.offset(dx, dy, dz), Blocks.IRON_BARS.defaultBlockState(), keep);
                    }
                }
            }
            case STARS -> {                                               // stars fall on them, one after another
                RandomSource r = level.getRandom();
                for (int k = 0; k < 6; k++) {
                    LivingEntity t = dreamers.isEmpty() ? null : dreamers.get(r.nextInt(dreamers.size()));
                    Vec3 hit = (t != null ? t.position() : at).add((r.nextDouble() - 0.5) * 3, 0, (r.nextDouble() - 0.5) * 3);
                    SpellRuntime.later(level, 6 + k * 6, () -> {
                        VfxSpawn.send(level, VfxShape.DREAM_MANIFEST, hit.add(0, 8, 0), hit, 0xFFFFF0A0, 14, 0.9f);
                        level.playSound(null, BlockPos.containing(hit), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1f, 1.6f);
                        for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, new AABB(hit, hit).inflate(2), v -> s.dreamers.contains(v.getUUID())))
                            s.book.hurt(s.inst, p, v, s.mode, 7f);
                    });
                }
            }
            case WALL -> {                                                // a candy wall between her and them
                Vec3 dir = focus != null ? focus.position().subtract(p.position()).multiply(1, 0, 1).normalize() : p.getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 mid = p.position().add(dir.scale(3));
                Vec3 side = new Vec3(-dir.z, 0, dir.x);
                for (int k = -3; k <= 3; k++) for (int dy = 0; dy < 4; dy++)
                    SpellRuntime.tempBlock(level, BlockPos.containing(mid.add(side.scale(k))).above(dy),
                            ((k + dy) % 2 == 0 ? Blocks.PINK_WOOL : Blocks.WHITE_WOOL).defaultBlockState(), keep);
            }
            case SLEEP -> {                                               // a lullaby: every dreamer drifts deeper
                for (LivingEntity t : dreamers) {
                    s.load.merge(t.getUUID(), 25f, Float::sum);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                }
            }
        }
        return true;
    }

    private static LivingEntity nearest(ServerPlayer p, List<LivingEntity> list) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : list) { double d = e.distanceToSqr(p); if (d < bd) { bd = d; best = e; } }
        return best;
    }

    /** A blocky stuffed bear, 5 wide and 7 tall, sitting at base. */
    private static void bear(ServerLevel level, BlockPos base, int ticks) {
        BlockState fur = Blocks.BROWN_WOOL.defaultBlockState(), belly = Blocks.WHITE_WOOL.defaultBlockState(), dark = Blocks.BLACK_WOOL.defaultBlockState(),
                bow = Blocks.PINK_WOOL.defaultBlockState();
        for (int dy = 0; dy < 7; dy++) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            BlockState b = null;
            if (dy <= 3 && Math.abs(dx) + Math.abs(dz) <= 3) b = (dz == -2 && Math.abs(dx) <= 1 && dy >= 1) ? belly : fur;          // body
            else if (dy == 4 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) b = dx == 0 && dz == -1 ? bow : fur;                  // neck + bow
            else if (dy >= 5 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) b = (dy == 5 && dz == -1 && dx != 0) ? dark : fur;     // head + eyes
            else if (dy == 6 && Math.abs(dx) == 2 && dz == 0) b = fur;                                                       // ears
            if (b != null) SpellRuntime.tempBlock(level, base.offset(dx, dy, dz), b, ticks);
        }
    }
}
