package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Why: the server side of the devil's figure (docs/devil_forms.md). {@link DevilRituals} asks it to make the figure when a ritual begins ({@link #spawn}), to say the devil's lines ({@link #speakAt}), to make its
 * offer ({@link #offer}), and to end it on sign ({@link #merge}), refuse and break ({@link #dissolve}); the hooks of logout, death, dimension change and {@code /devil clear} call {@link #discard}. The figure
 * ({@link DevilNpc}) also checks itself every ten ticks ({@link #check}) and vanishes when its owner is gone or it is past its lifetime, so a path nobody thought of can not leave one behind.
 *
 * <p>Where the figure is kept: in the caller's live contract ({@code DevilContract.npc}, its UUID; never saved), not in a map. The server-wide count is made when a ritual begins, by looking at the players
 * who are online (at most one lookup each), never by scanning entities or chunks.
 *
 * <p>Cost (rule 11): one entity per ritual, at most one per player and {@code npc.max} on the server (default 8), living at most {@code npc.lifetimeSeconds} (default 60); four chat lines to the caller only;
 * one count over the online players per ritual begin; one check per figure every 10 ticks. Over the cap, or with {@code npc.enabled = false}, nothing is spawned and the old pictures-only shadow is played.
 */
public final class DevilNpcs {
    private DevilNpcs() {}

    // ---------------------------------------------------------------- facts
    /** The figure a contract points at, if it is still alive in any level; null otherwise. */
    static DevilNpc figure(MinecraftServer server, DevilContract c) {
        if (server == null || c == null || c.npc == null) return null;
        for (ServerLevel l : server.getAllLevels()) {
            if (l.getEntity(c.npc) instanceof DevilNpc n && !n.isRemoved()) return n;
        }
        return null;
    }

    /** How many figures the given players have (a stale pointer is cleared). The server passes the players who are online. */
    static int live(MinecraftServer server, Iterable<? extends ServerPlayer> players) {
        int n = 0;
        for (ServerPlayer q : players) {
            DevilContract c = DevilStore.peek(q);
            if (c == null || c.npc == null) continue;
            if (figure(server, c) != null) n++;
            else c.npc = null;
        }
        return n;
    }

    // ---------------------------------------------------------------- begin
    /** Makes the figure for a ritual that has just begun; false (nothing made) when the setting is off, the cap is reached, the caller has one, or there is no room. */
    static boolean spawn(ServerPlayer p, DevilContract c, WorldDevil d, DevilRitualRun r) {
        if (!DevilConfig.npcEnabled()) return false;
        MinecraftServer server = p.getServer();
        if (server == null) return false;
        DevilNpc old = figure(server, c);
        if (old != null) old.discard();                                                         // one per player: a new ritual replaces the figure that is still dissolving
        c.npc = null;
        if (!DevilNpcRules.mayStart(true, false, live(server, server.getPlayerList().getPlayers()), DevilConfig.npcMax())) return false;
        ServerLevel sl = p.serverLevel();
        double x = 0, z = 0;
        boolean found = false;
        for (int i = 0; i < 3 && !found; i++) {                                                 // three blocks in front; closer when the space is taken
            double dist = i == 0 ? DevilNpcRules.FRONT : i == 1 ? 2.0 : 1.2;
            x = r.x + r.faceX * dist;
            z = r.z + r.faceZ * dist;
            found = sl.noCollision(new AABB(x - 0.35, r.y, z - 0.35, x + 0.35, r.y + 1.9, z + 0.35));
        }
        if (!found) return false;
        DevilNpc n = DevilEntities.NPC.get().create(sl);
        if (n == null) return false;
        float yaw = (float) (Math.atan2(r.faceX, -r.faceZ) * 180.0 / Math.PI);                    // it looks back at the caller
        n.moveTo(x, r.y, z, yaw, 0f);
        n.setOwner(p.getUUID());
        n.setDevil(d.id());
        if (!sl.addFreshEntity(n)) return false;
        c.npc = n.getUUID();
        c.npcAt = DevilContracts.time(p);
        return true;
    }

    // ---------------------------------------------------------------- the lines
    private static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    /** One line of the devil to the caller only (translatable, with the English as the fallback). */
    static void speak(ServerPlayer p, WorldDevil d, int i) {
        String fallback = DevilNpcLines.line(d.id(), i);
        if (fallback.isEmpty()) return;
        MutableComponent text = Component.literal(cap(d.name()) + ": ").withColor(d.color() | 0x404040);
        text.append(Component.translatableWithFallback(DevilNpcLines.key(d.id(), i), fallback).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        p.sendSystemMessage(text);
    }

    /** Called every tick of a running ritual: says line 0, 1 or 2 at its tick (three integer compares; the figure is looked up only on those ticks). */
    static void speakAt(ServerPlayer p, DevilContract c, WorldDevil d, int t) {
        if (c.npc == null) return;
        int ticks = d.world().ticks;
        for (int i = 0; i < 3; i++) {
            if (t == DevilNpcRules.lineTick(i, ticks) && figure(p.getServer(), c) != null) speak(p, d, i);
        }
    }

    // ---------------------------------------------------------------- the offer, the answer
    /** The ritual is over and the devil makes its offer: the figure roars and says its last line. False when there is no figure (the caller plays the old shadow). */
    static boolean offer(ServerPlayer p, DevilContract c, WorldDevil d) {
        DevilNpc n = figure(p.getServer(), c);
        if (n == null) return false;
        n.setState(DevilNpcRules.OFFER);
        speak(p, d, 3);
        return true;
    }

    /** The caller signed: the figure merges into them (the client plays it) and is removed after {@link DevilNpcRules#MERGE_TICKS}. */
    static void merge(ServerPlayer p, DevilContract c, WorldDevil d) {
        DevilNpc n = figure(p.getServer(), c);
        if (n == null) return;
        n.setState(DevilNpcRules.MERGE);
        Vec3 at = n.position().add(0, 1.0, 0);
        VfxSpawn.send(p.serverLevel(), VfxShape.DARKFX_BURST, at, at.add(0, 1, 0), 0xFF000000 | (d.color() & 0xFFFFFF), 24, 1.5f);
    }

    /** The caller refused, the offer ran out or the ritual broke: the figure dissolves. */
    static void dissolve(ServerPlayer p, DevilContract c) {
        DevilNpc n = figure(p.getServer(), c);
        if (n != null && !DevilNpcRules.terminal(n.state())) n.setState(DevilNpcRules.DISSOLVE);
    }

    /** Removes the figure at once (logout, death, a change of dimension, {@code /devil clear}). */
    static void discard(MinecraftServer server, DevilContract c) {
        DevilNpc n = figure(server, c);
        if (n != null) n.discard();
        if (c != null) c.npc = null;
    }

    // ---------------------------------------------------------------- the figure's own check (every 10 ticks)
    static void check(DevilNpc n) {
        if (!(n.level() instanceof ServerLevel sl)) return;
        UUID owner = n.owner();
        ServerPlayer p = owner == null ? null : sl.getServer().getPlayerList().getPlayer(owner);
        boolean here = p != null && p.isAlive() && p.serverLevel() == sl && p.distanceToSqr(n) <= DevilNpcRules.MAX_DISTANCE * DevilNpcRules.MAX_DISTANCE;
        boolean live = false;
        if (here) {
            DevilContract c = DevilStore.peek(p);
            live = c != null && n.getUUID().equals(c.npc) && (c.ritual != null || DevilRituals.offerOpen(c, DevilContracts.time(p)));
        }
        switch (DevilNpcRules.check(here, live, n.state(), n.life(), DevilConfig.npcLifetimeTicks())) {
            case DevilNpcRules.VANISH_NOW -> {
                n.discard();
                DevilContract c = p == null ? null : DevilStore.peek(p);
                if (c != null && n.getUUID().equals(c.npc)) c.npc = null;
            }
            case DevilNpcRules.DISSOLVE_NOW -> n.setState(DevilNpcRules.DISSOLVE);
            default -> { }
        }
    }
}
