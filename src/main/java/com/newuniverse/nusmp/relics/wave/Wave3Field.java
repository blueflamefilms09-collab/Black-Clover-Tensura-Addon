package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.wave.Wave3Rules.CharmBook;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.MarbleField;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Slots;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Step;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Wave 3: the things that stay in the world after an ability has run, kept in three short bounded lists and stepped from the server tick:
 * the patches of Mad Marbles (at most 32 in the world, 3 per player; looked at every 5 ticks), the Thermo-Gloves' steam clouds (at most 6;
 * every 10 ticks) and the creatures a charm, a calm or a wish has won over (at most 48; every second). Nothing here is saved: a restart clears it.
 */
final class Wave3Field {
    private Wave3Field() {}

    private record Pour(ResourceKey<Level> dim, MarbleField field) {}

    private record Cloud(ResourceKey<Level> dim, Vec3 at, double radius, long endAt, UUID owner) {}

    private static final Slots<Pour> POURS = new Slots<>(32);
    private static final Slots<Cloud> CLOUDS = new Slots<>(6);
    private static final CharmBook CHARMS = new CharmBook(48);

    static final int POURS_PER_PLAYER = 3;

    // ---------------------------------------------------------------- Mad Marbles
    /** Pours a patch of marbles centred on this point of the ground. */
    static void pour(ServerPlayer p, Vec3 ground) {
        ServerLevel sl = p.serverLevel();
        UUID me = p.getUUID();
        if (POURS.count(x -> me.equals(x.field().owner)) >= POURS_PER_PLAYER) {
            for (Iterator<Pour> it = POURS.items().iterator(); it.hasNext(); ) {
                if (me.equals(it.next().field().owner)) {
                    it.remove();
                    break;
                }
            }
        }
        POURS.add(new Pour(sl.dimension(), new MarbleField(ground.x, ground.y, ground.z, sl.getGameTime(), me)));
    }

    static int pours() { return POURS.size(); }

    private static void stepPours(MinecraftServer server) {
        for (Iterator<Pour> it = POURS.items().iterator(); it.hasNext(); ) {
            Pour pour = it.next();
            MarbleField f = pour.field();
            ServerLevel sl = server.getLevel(pour.dim());
            if (sl == null || f.expired(sl.getGameTime())) {
                it.remove();
                continue;
            }
            if (!sl.hasChunkAt(BlockPos.containing(f.cx, f.cy, f.cz))) continue;
            long now = sl.getGameTime();
            Player owner = server.getPlayerList().getPlayer(f.owner);
            AABB box = new AABB(f.cx - MarbleField.HALF, f.cy - 0.6, f.cz - MarbleField.HALF, f.cx + MarbleField.HALF, f.cy + 1.0, f.cz + MarbleField.HALF);
            List<LivingEntity> on = sl.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && !e.isSpectator() && e.onGround() && !(e instanceof Player pl && pl.isCreative()));
            int handled = 0;
            for (LivingEntity e : on) {
                if (handled >= 8) break;
                if (e.getUUID().equals(f.owner) || (owner != null && e.isAlliedTo(owner))) continue;
                if (!f.contains(e.getX(), e.getY(), e.getZ())) continue;
                Step st = f.step(e.getId(), now);
                if (st == Step.NONE) continue;
                handled++;
                slip(sl, e);
                if (st == Step.POP) pop(sl, e);
            }
        }
    }

    private static void slip(ServerLevel sl, LivingEntity e) {
        Wave3Util.give(e, 30, 6, "minecraft:slowness");
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, e.getX(), e.getY() + 0.1, e.getZ(), 6, 0.3, 0.05, 0.3, 0.1);
        sl.playSound(null, e.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    /** The marbles go off: coloured sparks, and a blinded creature for "1d4 rounds". */
    private static void pop(ServerLevel sl, LivingEntity e) {
        int[] colours = {0xE250D8, 0xFF9A2E, 0x2EE8D8, 0x58E848, 0xB48CFF};
        for (int c : colours) com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.relics.Fx.align(c), e.getX(), e.getY() + 0.5, e.getZ(), 6, 0.45, 0.4, 0.45, 0.05);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, e.getX(), e.getY() + 0.6, e.getZ(), 14, 0.4, 0.4, 0.4, 0.12);
        sl.playSound(null, e.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.9f, 1.3f);
        Wave3Util.give(e, Wave3Rules.blindTicks(1 + sl.getRandom().nextInt(4)), 0, "minecraft:blindness");
    }

    // ---------------------------------------------------------------- steam clouds
    /** A cloud of steam at a point: obscures and slows the caster's foes for a while. */
    static void steam(ServerPlayer p, Vec3 at, double radius, int ticks) {
        ServerLevel sl = p.serverLevel();
        CLOUDS.add(new Cloud(sl.dimension(), at, radius, sl.getGameTime() + ticks, p.getUUID()));
    }

    private static void stepClouds(MinecraftServer server, long t) {
        for (Iterator<Cloud> it = CLOUDS.items().iterator(); it.hasNext(); ) {
            Cloud c = it.next();
            ServerLevel sl = server.getLevel(c.dim());
            if (sl == null || sl.getGameTime() >= c.endAt()) {
                it.remove();
                continue;
            }
            double r = c.radius();
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, c.at().x, c.at().y + 1.0, c.at().z, 10, r * 0.45, 0.8, r * 0.45, 0.01);
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, c.at().x, c.at().y + 0.6, c.at().z, 3, r * 0.4, 0.4, r * 0.4, 0.01);
            if (t % 20 != 0) continue;
            ServerPlayer owner = server.getPlayerList().getPlayer(c.owner());
            int n = 0;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c.at(), c.at()).inflate(r),
                    x -> x.isAlive() && !x.isSpectator() && x.distanceToSqr(c.at()) <= r * r)) {
                if (n++ >= 12) break;
                if (e.getUUID().equals(c.owner()) || (owner != null && e.isAlliedTo(owner))) continue;
                if (e instanceof Player pl && pl.isCreative()) continue;
                Wave3Util.give(e, 60, 0, "minecraft:darkness");
                Wave3Util.give(e, 40, 0, "minecraft:slowness");
            }
        }
    }

    // ---------------------------------------------------------------- charms
    /** The creature stops hunting this player (and, when {@code calm}, everyone) until the time is up or the player strikes it. */
    static void charm(Mob m, ServerPlayer owner, int ticks, boolean calm) {
        long now = m.level().getGameTime();
        CHARMS.add(m.getUUID(), owner.getUUID(), now + ticks, calm);
        release(m, owner, calm);
    }

    /** Drops its target now if it is the one it should spare. */
    private static void release(Mob m, LivingEntity maybeTarget, boolean any) {
        LivingEntity t = m.getTarget();
        if (t != null && (any || t == maybeTarget)) m.setTarget(null);
        try {                                                                           // a vanilla mob's brain has no ATTACK_TARGET memory registered: getMemory throws there
            m.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(x -> {
                if (any || x == maybeTarget) m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            });
        } catch (RuntimeException | LinkageError otherBrain) {
            // a brain of another shape: the plain target slot is already clear
        }
    }

    /** Should this creature leave this target alone right now? */
    static boolean spares(LivingEntity mob, LivingEntity target) {
        return CHARMS.size() > 0 && CHARMS.spares(mob.getUUID(), target.getUUID(), mob.level().getGameTime());
    }

    /** A blow from this attacker frees a creature it had charmed. */
    static void struck(LivingEntity victim, Entity attacker) {
        if (CHARMS.size() > 0 && attacker != null) CHARMS.breakBy(victim.getUUID(), attacker.getUUID());
    }

    private static void stepCharms(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        CHARMS.purge(now);
        for (UUID id : CHARMS.mobs()) {
            Entity found = null;
            for (ServerLevel l : server.getAllLevels()) {
                found = l.getEntity(id);
                if (found != null) break;
            }
            if (found == null) continue;                                               // not loaded: it keeps until its time runs out
            if (!(found instanceof Mob m) || !m.isAlive()) {
                CHARMS.remove(id);
                continue;
            }
            CharmBook.Entry e = CHARMS.get(id);
            if (e == null) continue;
            if (e.calm()) release(m, null, true);
            else {
                ServerPlayer owner = server.getPlayerList().getPlayer(e.owner());
                if (owner != null) release(m, owner, false);
            }
        }
    }

    // ---------------------------------------------------------------- the server tick and the stop
    static void tick(MinecraftServer server) {
        long t = server.getTickCount();
        if (t % 5 == 0 && POURS.size() > 0) stepPours(server);
        if (t % 10 == 0 && CLOUDS.size() > 0) stepClouds(server, t);
        if (t % 20 == 7 && CHARMS.size() > 0) stepCharms(server);
    }

    static void clear() {
        POURS.clear();
        CLOUDS.clear();
        CHARMS.clear();
    }
}
