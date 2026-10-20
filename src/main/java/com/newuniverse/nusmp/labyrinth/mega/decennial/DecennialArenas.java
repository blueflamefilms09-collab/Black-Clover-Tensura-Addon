package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialData.Site;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaFrame;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaThemes;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.labyrinth.seal.GatekeeperDoorBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: a hall is not only blocks. It has a party in it, interactive parts the party may break (and the Dungeon Guard must let it), a seal on the entry that holds while the fight lasts, hazard
 * spots the lair actions aim at, and a state that must come back to normal when the party is wiped. This is the runtime of all of that, and the one class that knows how a hall stands in the world:
 * which halls exist ({@link DecennialData}), which one a person is near (it is then "live": its element cells are indexed, handed to the Dungeon Guard's break rule and sent to the person's client),
 * what breaking an element does (adds step out, a pillar topples on its strip, a vessel shatters, a weak point hurts the boss), when the entry closes and when it MUST open again (the boss falls,
 * the party is wiped, the boss is gone, or the seal has held too long: the anti soft-lock), and how the hall resets. All of it happens inside the guard's internal span through {@link ArenaPlacer},
 * and a hall nobody is near costs nothing at all (no index, no cells, no tick).
 */
public final class DecennialArenas {
    private DecennialArenas() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, Live> LIVE = new HashMap<>();
    private static final Map<UUID, String> SYNCED = new HashMap<>();
    private static volatile int pendingBuilds;
    /** A person within this many blocks of a hall's edge makes it live; no person within {@link #FAR} of its middle for {@link #IDLE_TICKS} lets it go. */
    private static final int NEAR = 24, FAR = 96, IDLE_TICKS = 600;

    // ================================================================ the live state of one hall
    /** The runtime of one hall a person is near. */
    static final class Live {
        final Site site;
        final BossArenaStructure plan;
        final ArenaFrame frame;
        final Map<Long, Integer> cells = new HashMap<>();
        final Map<String, List<double[]>> zones = new HashMap<>();
        int version;
        long lastNear;

        Live(Site site, BossArenaStructure plan) {
            this.site = site;
            this.plan = plan;
            this.frame = site.frame();
            if (DecennialConfig.get(DecennialConfig.ELEMENTS)) {
                for (BossArenaStructure.Element e : plan.elements()) {
                    if (e.locked || site.broken.contains(e.id)) continue;
                    for (BossArenaStructure.Placed c : e.cells) cells.put(ArenaPlacer.worldPos(frame, c.x(), c.y(), c.z()).asLong(), e.id);
                }
            }
            for (BossArenaStructure.Zone z : plan.zones()) {
                BlockPos p = ArenaPlacer.worldPos(frame, z.x(), 1, z.z());
                zones.computeIfAbsent(z.kind(), k -> new ArrayList<>()).add(new double[] {p.getX() + 0.5, p.getY(), p.getZ() + 0.5, z.r()});
            }
        }

        BossArenaStructure.Element element(int id) {
            List<BossArenaStructure.Element> all = plan.elements();
            if (id >= 0 && id < all.size() && all.get(id).id == id) return all.get(id);
            for (BossArenaStructure.Element e : all) if (e.id == id) return e;
            return null;
        }

        long[] cellArray() {
            long[] out = new long[Math.min(cells.size(), DecennialNet.MAX_CELLS)];
            int i = 0;
            for (long c : cells.keySet()) {
                if (i >= out.length) break;
                out[i++] = c;
            }
            return out;
        }

        /** The pillars that can still be brought down. */
        List<BossArenaStructure.Element> pillars() {
            List<BossArenaStructure.Element> out = new ArrayList<>();
            if (!DecennialConfig.get(DecennialConfig.ELEMENTS)) return out;
            for (BossArenaStructure.Element e : plan.elements()) {
                if (e.kind == BossArenaStructure.Kind.PILLAR && e.effect == BossArenaStructure.Effect.TOPPLE && !e.locked && !site.broken.contains(e.id) && e.strip.length == 4) out.add(e);
            }
            return out;
        }

        /** The strip a toppled pillar crushes, in world columns: {minX, minZ, maxX, maxZ} (blocks, inclusive). */
        double[] strip(BossArenaStructure.Element e) {
            BlockPos a = ArenaPlacer.worldPos(frame, e.strip[0], 1, e.strip[1]), b = ArenaPlacer.worldPos(frame, e.strip[2], 1, e.strip[3]);
            return new double[] {Math.min(a.getX(), b.getX()), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()) + 1.0, Math.max(a.getZ(), b.getZ()) + 1.0};
        }

        /** The spots of a zone kind as world points {x, y, z, radius} (empty when the hall has none). */
        List<double[]> zoneSpots(String kind) { return kind == null || kind.isEmpty() ? List.of() : zones.getOrDefault(kind, List.of()); }

        /** The world point at the middle of the hall's floor. */
        double[] middle() { return new double[] {frame.cx() + 0.5, frame.y0() + 1.0, frame.cz() + 0.5}; }

        boolean contains(double x, double y, double z) { return frame.contains((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z), plan.height); }
    }

    // ================================================================ sites: registering and building
    static DecennialData data(MinecraftServer server) { return DecennialData.get(server); }

    static String dimOf(ServerLevel sl) { return sl.dimension().location().toString(); }

    static ServerLevel levelOf(MinecraftServer server, Site s) {
        try {
            return server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(s.dim)));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Records a hall to be built in a floor of the Mega-Dungeon (a new one, or a regeneration of a hall the sector already had: it starts over). */
    static Site registerSector(ServerLevel sl, int sectorId, DecennialProfile p, BossArenaStructure plan, ArenaFrame frame) {
        DecennialData d = data(sl.getServer());
        Site old = d.siteOfSector(sectorId);
        if (old != null) {
            discard(old);
            if (!old.built) pendingBuilds--;
        }
        Site s = new Site(DecennialData.keyOfSector(sectorId));
        fill(s, sl, p, plan, frame);
        s.sectorId = sectorId;
        d.put(s);
        pendingBuilds++;
        return s;
    }

    /** Records a hall an admin stands up alone (built whole by the caller). */
    static Site registerStandalone(ServerLevel sl, DecennialProfile p, BossArenaStructure plan, ArenaFrame frame) {
        DecennialData d = data(sl.getServer());
        Site s = new Site(d.newStandaloneKey());
        fill(s, sl, p, plan, frame);
        s.standalone = true;
        s.built = false;
        d.put(s);
        return s;
    }

    private static void fill(Site s, ServerLevel sl, DecennialProfile p, BossArenaStructure plan, ArenaFrame f) {
        s.level = p.level();
        s.theme = plan.theme;
        s.dim = dimOf(sl);
        s.cx = f.cx();
        s.y0 = f.y0();
        s.cz = f.cz();
        s.size = f.size();
        s.rot = f.rot();
        s.cursor = 0;
        s.built = false;
    }

    private static void discard(Site s) {
        Live l = LIVE.remove(s.key);
        if (l != null) for (long c : l.cells.keySet()) BreakableCells.serverRemove(c);
    }

    /** Recounts the halls still to be built (at server start). A hall an admin was standing up when the server stopped is forgotten (the facade builds those whole, so a half hall has no owner to finish it). */
    static void recount(MinecraftServer server) {
        DecennialData d = data(server);
        int n = 0;
        for (Site s : d.unfinished()) {
            if (s.standalone) d.remove(s.key);
            else n++;
        }
        pendingBuilds = n;
    }

    /** One step of the build of one unfinished hall (called every server tick; free when nothing is pending). */
    static void stepBuilds(MinecraftServer server) {
        if (pendingBuilds <= 0) return;
        DecennialData d = data(server);
        for (Site s : d.unfinished()) {
            if (s.standalone) continue;                                    // an admin's hall is built whole by the caller
            ServerLevel sl = levelOf(server, s);
            BossArenaStructure plan = ArenaThemes.build(s.theme, s.size);
            if (sl == null || plan == null) {
                if (plan == null) {
                    s.built = true;                                        // a hall whose theme no longer exists is given up (the old hall stays)
                    pendingBuilds--;
                    d.setDirty();
                    LOG.warn("[nusmp] The hall of {} names the unknown arena theme '{}': the old hall stays.", s.key, s.theme);
                }
                continue;
            }
            try {
                if (s.cursor == 0) ArenaPlacer.clearDecor(sl, s.frame(), plan.height);
                s.cursor = ArenaPlacer.step(sl, plan, s.frame(), s.cursor, DecennialConfig.get(DecennialConfig.ARENA_BLOCKS_PER_TICK));
                if (s.cursor >= ArenaPlacer.total(plan)) {
                    s.built = true;
                    pendingBuilds--;
                    LOG.info("[nusmp] The hall '{}' of level {} is built for {} at ({}, {}, {}) turned {}.", s.theme, s.level, s.key, s.cx, s.y0, s.cz, s.rot);
                }
                d.setDirty();
            } catch (RuntimeException e) {
                LOG.error("[nusmp] A hall could not be built ({}): {}", s.key, e.toString());
                s.built = true;
                pendingBuilds--;
            }
            return;                                                        // one hall per tick
        }
    }

    // ================================================================ going live
    /** The live hall of a site, made when first asked for (null when its theme is gone or the hall is not built yet). */
    static Live live(Site s) {
        if (s == null || !s.built) return null;
        Live l = LIVE.get(s.key);
        if (l != null) return l;
        BossArenaStructure plan = ArenaThemes.build(s.theme, s.size);
        if (plan == null) return null;
        l = new Live(s, plan);
        LIVE.put(s.key, l);
        for (long c : l.cells.keySet()) BreakableCells.serverAdd(c);
        return l;
    }

    static Live liveIfAny(String key) { return LIVE.get(key); }

    /** The live hall (made if needed) whose site has this key, or null. */
    static Live liveByKey(MinecraftServer server, String key) {
        if (key == null || key.isEmpty()) return null;
        Live l = LIVE.get(key);
        return l != null ? l : live(data(server).site(key));
    }

    /** The live hall whose footprint holds the column, or null (does not wake a sleeping hall). */
    static Live liveAt(int x, int z) {
        for (Live l : LIVE.values()) if (l.frame.containsColumn(x, z)) return l;
        return null;
    }

    static void drop(Live l) {
        for (long c : l.cells.keySet()) BreakableCells.serverRemove(c);
        LIVE.remove(l.site.key);
    }

    /** Server stop: forget every live hall (the saved data stays). */
    static void shutdown() {
        LIVE.clear();
        SYNCED.clear();
        BreakableCells.serverClear();
    }

    // ================================================================ the boss of a hall
    static LabyrinthGuardianEntity bossOf(ServerLevel sl, Site s) {
        if (s == null || s.boss == null) return null;
        Entity e = sl.getEntity(s.boss);
        return e instanceof LabyrinthGuardianEntity g && g.isAlive() ? g : null;
    }

    /** The boss told its hall who it is. */
    static void attachBoss(MinecraftServer server, String key, UUID boss) {
        Site s = data(server).site(key);
        if (s == null || boss.equals(s.boss)) return;
        s.boss = boss;
        data(server).setDirty();
    }

    // ================================================================ the pulse: wake halls near people, sync, sleep, keep the seal honest
    /** Once a second. */
    static void pulse(MinecraftServer server) {
        DecennialData d = data(server);
        if (d.sites().isEmpty()) return;
        long now = server.overworld().getGameTime();
        List<UUID> seen = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Site near = null;
            String dim = dimOf(p.serverLevel());
            for (Site s : d.sites()) {
                if (!s.built || !s.dim.equals(dim)) continue;
                int h = s.size / 2 + NEAR;
                if (Math.abs(p.getX() - s.cx) <= h && Math.abs(p.getZ() - s.cz) <= h && Math.abs(p.getY() - s.y0) < 64) {
                    near = s;
                    break;
                }
            }
            seen.add(p.getUUID());
            if (near == null) {
                if (SYNCED.remove(p.getUUID()) != null) DecennialNet.send(p, new long[0]);
                continue;
            }
            Live l = live(near);
            if (l == null) continue;
            l.lastNear = now;
            String mark = near.key + "#" + l.version;
            if (!mark.equals(SYNCED.get(p.getUUID()))) {
                DecennialNet.send(p, l.cellArray());
                SYNCED.put(p.getUUID(), mark);
            }
        }
        SYNCED.keySet().retainAll(seen);
        // sleep the halls nobody is near, and keep every seal honest
        for (Live l : new ArrayList<>(LIVE.values())) {
            if (l.lastNear == 0) l.lastNear = now;
            if (!l.site.sealed && now - l.lastNear > IDLE_TICKS) drop(l);
        }
        for (Site s : d.sites()) {
            if (!s.sealed) continue;
            ServerLevel sl = levelOf(server, s);
            if (sl == null) continue;
            Live l = live(s);
            if (l == null) {
                s.sealed = false;
                d.setDirty();
                continue;
            }
            int maxTicks = DecennialConfig.get(DecennialConfig.SEAL_MAX_MINUTES) * 1200;
            LabyrinthGuardianEntity boss = bossOf(sl, s);
            if (boss == null || s.defeated || now - s.sealedAt > maxTicks) openSeal(sl, l, boss == null ? "gone" : s.defeated ? "defeated" : "timeout");
        }
    }

    // ================================================================ the seal
    /** True when nobody stands in the entry's doorway (so closing it cannot trap or crush a person in it). */
    static boolean doorwayClear(ServerLevel sl, Live l) {
        int[] loc = new int[3];
        for (Player p : DecennialFx.inHall(sl, l.frame, l.plan.height)) {
            l.frame.local((int) Math.floor(p.getX()), (int) Math.floor(p.getY()), (int) Math.floor(p.getZ()), loc);
            if (BossArenaStructure.inDoorway(loc[0], Math.max(1, loc[1]), loc[2], l.plan.size) && loc[2] >= l.plan.size - 2) return false;
        }
        return true;
    }

    static boolean closeSeal(ServerLevel sl, Live l) {
        if (!DecennialConfig.get(DecennialConfig.SEAL) || l.site.sealed || l.site.defeated || l.plan.sealCells().isEmpty()) return false;
        if (!doorwayClear(sl, l)) return false;
        ArenaPlacer.setSeal(sl, l.plan, l.frame, true);
        l.site.sealed = true;
        l.site.sealedAt = sl.getGameTime();
        data(sl.getServer()).setDirty();
        sl.playSound(null, BlockPos.containing(l.middle()[0], l.middle()[1], l.middle()[2]), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.6f, 0.5f);
        say(sl, l, "sealed", "The doors of the hall seal behind you.", ChatFormatting.DARK_RED);
        return true;
    }

    static void openSeal(ServerLevel sl, Live l, String why) {
        if (!l.site.sealed) return;
        ArenaPlacer.setSeal(sl, l.plan, l.frame, false);
        l.site.sealed = false;
        data(sl.getServer()).setDirty();
        sl.playSound(null, BlockPos.containing(l.middle()[0], l.middle()[1], l.middle()[2]), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.4f, 0.7f);
        say(sl, l, "unsealed", "The seal of the hall breaks.", ChatFormatting.GRAY);
        LOG.info("[nusmp] The seal of {} opened ({}).", l.site.key, why);
    }

    // ================================================================ breaking elements
    /** A person broke a block of a live hall: true when it was an element and the break was carried out (the caller cancels the vanilla break). */
    public static boolean breakAt(ServerLevel sl, BlockPos pos, ServerPlayer breaker) {
        Live l = liveAt(pos.getX(), pos.getZ());
        if (l == null) return false;
        Integer id = l.cells.get(pos.asLong());
        if (id == null) return false;
        BossArenaStructure.Element e = l.element(id);
        if (e == null) return false;
        breakElement(sl, l, e, breaker, -1.0);
        return true;
    }

    /**
     * Brings an element down. {@code lairDamage} is -1 for a person's break (the damage is the element's own, small) or the damage a lair action brings a pillar down with. Spawns the adds the budget
     * grants, hurts the boss by the weak point's share, topples a pillar on its strip, bursts a vessel.
     */
    static void breakElement(ServerLevel sl, Live l, BossArenaStructure.Element e, ServerPlayer breaker, double lairDamage) {
        if (l.site.broken.contains(e.id)) return;
        // the burst of block particles, then the blocks go
        int shown = 0;
        for (BossArenaStructure.Placed c : e.cells) {
            if (shown++ >= 14) break;
            BlockPos wp = ArenaPlacer.worldPos(l.frame, c.x(), c.y(), c.z());
            BlockState st = sl.getBlockState(wp);
            if (!st.isAir()) sl.levelEvent(2001, wp, Block.getId(st));
        }
        l.site.broken.add(e.id);
        for (BossArenaStructure.Placed c : e.cells) {
            long key = ArenaPlacer.worldPos(l.frame, c.x(), c.y(), c.z()).asLong();
            l.cells.remove(key);
            BreakableCells.serverRemove(key);
        }
        l.version++;
        ArenaPlacer.removeElement(sl, l.frame, e);
        data(sl.getServer()).setDirty();
        double[] at = centre(l, e);
        LabyrinthGuardianEntity boss = bossOf(sl, l.site);
        double atk = boss == null ? 8.0 : DecennialFx.baseAttack(boss);
        switch (e.effect) {
            case ADDS -> {
                say(sl, l, "element." + (e.kind == BossArenaStructure.Kind.ROOT_NODE ? "root_node" : "tombstone"), e.kind == BossArenaStructure.Kind.ROOT_NODE
                        ? "The root node bursts: lesser devils pour out!" : "The tombstone cracks, and something answers.", ChatFormatting.DARK_PURPLE);
                spawnAdds(sl, boss, at, e.adds, breaker);
                Fx.spray(sl, FxThemes.DUNGEON_DECENNIAL, FxOld.SOUL, at[0], at[1] + 0.5, at[2], 18, 0.5, 0.6, 0.5, 0.04);
            }
            case TOPPLE -> {
                say(sl, l, "element.pillar", "The pillar breaks, and falls!", ChatFormatting.GRAY);
                for (BossArenaStructure.Placed r : e.rubble) ArenaPlacer.setCell(sl, l.frame, r.x(), r.y(), r.z(), r.spec());
                crush(sl, l, e, boss, lairDamage >= 0 ? lairDamage : atk * 0.35);
                sl.playSound(null, BlockPos.containing(at[0], at[1], at[2]), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.2f, 0.7f);
            }
            default -> {
                if (boss != null) for (Player p : DecennialFx.near(sl, at[0], at[1], at[2], 3.5)) DecennialFx.hurt(boss, p, atk * 0.12, true);
                Fx.spray(sl, FxThemes.DUNGEON_DECENNIAL, FxOld.EXPLOSION, at[0], at[1] + 0.6, at[2], 2, 0.3, 0.3, 0.3, 0.0);
                sl.playSound(null, BlockPos.containing(at[0], at[1], at[2]), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.4f, 0.8f);
            }
        }
        if (e.bossShare > 0.0 && boss != null && breaker != null) {
            boss.hurt(sl.damageSources().playerAttack(breaker), (float) (boss.getMaxHealth() * e.bossShare));
            say(sl, l, "weak_point", "A weak point breaks: " + boss.getName().getString() + " staggers.", ChatFormatting.GOLD, boss.getName());
        }
    }

    /** The pillar's strip is crushed: every person standing on it takes the damage, once. */
    private static void crush(ServerLevel sl, Live l, BossArenaStructure.Element e, LabyrinthGuardianEntity boss, double damage) {
        if (e.strip.length != 4 || boss == null) return;
        double[] s = l.strip(e);
        for (Player p : DecennialFx.inHall(sl, l.frame, l.plan.height)) {
            if (p.getX() >= s[0] - 0.3 && p.getX() <= s[2] + 0.3 && p.getZ() >= s[1] - 0.3 && p.getZ() <= s[3] + 0.3) {
                DecennialFx.hurt(boss, p, damage, false);
                DecennialFx.push(p, (s[0] + s[2]) / 2, (s[1] + s[3]) / 2, 0.5, 0.3);
            }
        }
        Fx.spray(sl, FxThemes.DUNGEON_DECENNIAL, FxOld.CLOUD, (s[0] + s[2]) / 2, l.frame.y0() + 1.2, (s[1] + s[3]) / 2, 30, Math.max(0.5, (s[2] - s[0]) / 2), 0.3, Math.max(0.5, (s[3] - s[1]) / 2), 0.05);
    }

    /** A lair action brings a pillar down. */
    static void topple(ServerLevel sl, Live l, BossArenaStructure.Element e, double damage) {
        breakElement(sl, l, e, null, damage);
    }

    private static double[] centre(Live l, BossArenaStructure.Element e) {
        double x = 0, y = 1e9, z = 0;
        for (BossArenaStructure.Placed c : e.cells) {
            BlockPos p = ArenaPlacer.worldPos(l.frame, c.x(), c.y(), c.z());
            x += p.getX() + 0.5;
            z += p.getZ() + 0.5;
            y = Math.min(y, p.getY());
        }
        int n = Math.max(1, e.cells.size());
        return new double[] {x / n, y + 0.5, z / n};
    }

    /** Adds step out of a broken element, as many as the boss's budget grants (none while the boss is not there to be reinforced). */
    private static void spawnAdds(ServerLevel sl, LabyrinthGuardianEntity boss, double[] at, int wanted, ServerPlayer target) {
        if (boss == null || !boss.decennial().active()) return;
        List<String> pool = boss.decennial().profile().adds().pool();
        if (pool.isEmpty()) return;
        int granted = boss.decennial().grantExtra(boss, sl, wanted);
        for (int i = 0; i < granted; i++) {
            String kind = pool.get(sl.random.nextInt(pool.size()));
            double a = sl.random.nextDouble() * Math.PI * 2;
            AddKinds.spawn(sl, kind, at[0] + Math.cos(a) * 1.0, at[1], at[2] + Math.sin(a) * 1.0, boss.decennial().profile().depth(), boss.getUUID(), target);
        }
    }

    // ================================================================ reset, defeat
    /** The party is wiped (or the fight was given up): the broken elements are back, the rubble gone, the seal open. */
    static void reset(ServerLevel sl, Live l) {
        Site s = l.site;
        boolean any = !s.broken.isEmpty();
        for (int id : new ArrayList<>(s.broken)) {
            BossArenaStructure.Element e = l.element(id);
            if (e == null) continue;
            if (e.locked && s.defeated) continue;
            ArenaPlacer.restoreElement(sl, l.frame, e);
            s.broken.remove(id);
            if (!e.locked) for (BossArenaStructure.Placed c : e.cells) {
                long key = ArenaPlacer.worldPos(l.frame, c.x(), c.y(), c.z()).asLong();
                l.cells.put(key, e.id);
                BreakableCells.serverAdd(key);
            }
        }
        l.version++;
        if (s.sealed) openSeal(sl, l, "reset");
        data(sl.getServer()).setDirty();
        if (any) say(sl, l, "reset", "The hall resets. The fallen are forgotten, and the seals are restored.", ChatFormatting.GRAY);
    }

    /** The boss fell: the seal opens, the adds are gone, the great gate (if the hall has one) opens, and an admin's hall opens its Gatekeeper. */
    static void defeated(ServerLevel sl, Live l, LabyrinthGuardianEntity boss) {
        Site s = l.site;
        s.defeated = true;
        for (Mob m : AddKinds.of(sl, new AABB(l.frame.cx() - l.plan.size, l.frame.y0() - 6, l.frame.cz() - l.plan.size, l.frame.cx() + l.plan.size, l.frame.y0() + l.plan.height, l.frame.cz() + l.plan.size), boss.getUUID())) {
            Fx.spray(sl, FxThemes.DUNGEON_DECENNIAL, FxOld.SOUL, m.getX(), m.getY() + 0.6, m.getZ(), 8, 0.2, 0.4, 0.2, 0.02);
            m.discard();
        }
        if (s.sealed) openSeal(sl, l, "defeated");
        boolean gate = false;
        for (BossArenaStructure.Element e : l.plan.elements()) {
            if (e.kind != BossArenaStructure.Kind.GATE || s.broken.contains(e.id)) continue;
            ArenaPlacer.removeElement(sl, l.frame, e);
            s.broken.add(e.id);
            gate = true;
        }
        if (s.standalone && !s.gateOpen) {
            BlockPos door = ArenaPlacer.worldPos(l.frame, l.plan.c, 1, 1);
            if (GatekeeperDoorBlock.openConnected(sl, door) > 0) {
                s.gateOpen = true;
                gate = true;
            }
        }
        l.version++;
        data(sl.getServer()).setDirty();
        if (gate) {
            say(sl, l, "gate_open", "The great gate groans open. Take the Mana Core to the Gatekeeper.", ChatFormatting.LIGHT_PURPLE);
            sl.playSound(null, BlockPos.containing(l.middle()[0], l.middle()[1], l.middle()[2]), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.6f, 0.4f);
        }
    }

    // ================================================================ messages
    static void say(ServerLevel sl, Live l, String key, String fallback, ChatFormatting colour, Object... args) {
        Component line = Component.translatableWithFallback("message.nusmp.mega.decennial." + key, fallback, args).withStyle(colour);
        for (Player p : DecennialFx.inHall(sl, l.frame, l.plan.height)) p.displayClientMessage(line, true);
    }

    /** Test helper: forgets everything the runtime holds in memory (the self-tests build and clear halls and must not leak a live hall). */
    static void forget(Site s) {
        discard(s);
        for (Map.Entry<UUID, String> e : new ArrayList<>(SYNCED.entrySet())) if (e.getValue().startsWith(s.key + "#")) SYNCED.remove(e.getKey());
    }
}
