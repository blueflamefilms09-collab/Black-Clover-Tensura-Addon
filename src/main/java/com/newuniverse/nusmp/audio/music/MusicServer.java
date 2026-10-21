package com.newuniverse.nusmp.audio.music;

import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the server half of the themed music. Once a second (every 20th tick) it works out, for each player in the Mega-Dungeon or the Hub, the layer they should hear
 * ({@link MusicRules}) and the theme and road of the floor, and sends a {@link MusicNet.State} when it differs from what that player was last sent (and every five seconds
 * regardless, so a client that missed one, or one that joined late, is never silent for long). A player who leaves both dimensions is sent {@link MusicState#OFF} once, so the
 * client fades the music out; after that nothing is sent. The work per player per second is one sector lookup, one safe-room lookup and one entity scan of a box around them
 * (monsters that hunt them, bosses close by), the same order of cost as the theme's ambient pulse; the scan only runs inside the dungeon. It reads only; it never changes the
 * world and never touches hunger.
 */
public final class MusicServer {
    private MusicServer() {}

    /** Ticks between two evaluations, and between two keep-alives. */
    public static final int EVERY = 20, KEEP_ALIVE = 100;

    private record Sent(MusicState state, int tick) {}

    private static final Map<UUID, Sent> SENT = new HashMap<>();

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        if (now % EVERY != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            try {
                evaluate(server, p, now);
            } catch (RuntimeException ignored) {
                // music must never disturb the tick
            }
        }
    }

    private static void evaluate(MinecraftServer server, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        boolean hub = Labyrinth.inSanctuary(level);
        boolean mega = Labyrinth.inMega(level);
        Sent last = SENT.get(p.getUUID());
        MusicState next;
        if (!hub && !mega) {
            if (last == null || last.state().layer() == MusicLayer.OFF) return;       // already off: say nothing
            next = MusicState.OFF;
        } else if (p.isSpectator()) {
            next = MusicState.OFF;
        } else if (hub) {
            next = new MusicState(MusicLayer.HUB, "", 1);
        } else {
            next = inDungeon(server, level, p);
        }
        if (last != null && last.state().equals(next) && now - last.tick() < KEEP_ALIVE) return;
        SENT.put(p.getUUID(), new Sent(next, now));
        PacketDistributor.sendToPlayer(p, new MusicNet.State(next));
    }

    private static MusicState inDungeon(MinecraftServer server, ServerLevel level, ServerPlayer p) {
        Sector s = MegaData.get(server).sectorAt(p.blockPosition());
        String theme = s == null || s.theme == null ? "" : s.theme.id();
        int polarity = s == null || s.polarity == null ? 1 : s.polarity.sign();
        boolean rift = RiftRules.slotAt(p.getX(), p.getZ()) >= 0;
        boolean safe = MegaDungeon.inSafeZone(level, p.blockPosition());
        boolean boss = false;
        int hunters = 0;
        AABB box = p.getBoundingBox().inflate(MusicRules.BOSS_RANGE, 20.0, MusicRules.BOSS_RANGE);
        double hunterSq = MusicRules.HUNTER_RANGE * MusicRules.HUNTER_RANGE, bossSq = MusicRules.BOSS_RANGE * MusicRules.BOSS_RANGE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            double d = e.distanceToSqr(p);
            if (d <= bossSq && BossIdentity.isBoss(e)) {
                boss = true;
            } else if (e instanceof Mob m && d <= hunterSq && m.getTarget() == p && (MegaDungeon.isDungeonMonster(m) || m instanceof Enemy)) {
                hunters++;
            }
        }
        return new MusicState(MusicRules.decide(false, boss, rift, safe, hunters), theme, polarity);
    }

    public static void forget(UUID player) { SENT.remove(player); }

    public static void clear() { SENT.clear(); }
}
