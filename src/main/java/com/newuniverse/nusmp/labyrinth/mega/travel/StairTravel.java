package com.newuniverse.nusmp.labyrinth.mega.travel;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.ExpansionQueue;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import com.newuniverse.nusmp.labyrinth.mega.gen.StairLinks;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;

/**
 * Why: in the floor model (1.58.0) a floor is a level of its own, and the only way from one level to the next is a stair: the staircase of a stairwell ends in a
 * landing pad with a STAIR_EXIT marker above its middle, and walking onto the pad takes the player, and nobody else, to the linked floor: the forward stairs to the
 * arrival stairwell of the floor after, the stairs that lead back to the forward stairwell of the floor before (or out of the Labyrinth, to Floor 0, from the first floor).
 * This is that mover. Four times a second it looks at every player in the Mega-Dungeon; a player standing on the pad of the stairs of the floor they are on
 * ({@link StairLinks#onPad}) is moved. The rules, in order: the Directional Lock is asked first ({@code MegaDungeon.mayEnter}, which tells the player why it refuses);
 * the floor the stairs lead to must be a place (not still being generated, not failed): until then the player is told the stairs are still forming and the floor is queued,
 * so the line is a promise and not a wall; the arrival is the target floor's pad (the ARRIVAL marker of its stairwell), where the way back is the pad of the same stairwell,
 * so a party that has just arrived is never standing on a pad (they are a staircase away from it); and every use, accepted or refused, starts a short cooldown so a refusal
 * is spoken once every few seconds, not every look, and a player is never bounced. The deepest floor reached is remembered the way the Landings were ({@code MegaTravel.remember}), so the Hub's gate takes
 * a returning player to where they left off. The feedback is a short title with its fade ("Floor +4", "You climb the stairs"), chat is kept for refusals. Server thread only.
 * The dungeon is instanced per party (1.58.0): the floor a stair leads to is the next floor of the SAME party's chain ({@code Sector.party} of the floor the player stands on),
 * so a player can only ever climb into floors of their own party, whoever else is in the dungeon.
 */
public final class StairTravel {
    private static final Logger LOG = LogUtils.getLogger();
    /** Ticks between two looks at the players in the dungeon (the pad is 3 blocks wide: a quarter of a second cannot be walked across). */
    private static final int EVERY = 4;
    /** Ticks a player cannot use a stair again after using one (or being refused): long enough to read the line, short enough to try again. */
    private static final long USED_COOLDOWN = 60L, REFUSED_COOLDOWN = 80L;
    /** The persistent key of the cooldown (game time, long). */
    public static final String UNTIL = "nusmp_mega_stair_until";
    private static boolean warned;

    private StairTravel() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(StairTravel::onServerTick);
    }

    private static void onServerTick(ServerTickEvent.Post e) {
        try {
            if (e.getServer().getTickCount() % EVERY != 0) return;
            ServerLevel level = MegaDungeon.level(e.getServer());
            if (level == null || level.players().isEmpty()) return;
            MegaData data = MegaData.get(e.getServer());
            for (ServerPlayer p : new ArrayList<>(level.players())) {
                try {
                    look(level, data, p);
                } catch (RuntimeException ex) {
                    if (!warned) {
                        warned = true;
                        LOG.warn("[nusmp] The stair travel failed for {} (further failures are not logged): {}", p.getGameProfile().getName(), ex.toString());
                    }
                }
            }
        } catch (RuntimeException ex) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] The stair travel failed (further failures are not logged): {}", ex.toString());
            }
        }
    }

    /** Looks at one player: is he on the pad of the stairs of the floor he stands on? */
    static void look(ServerLevel level, MegaData data, ServerPlayer p) {
        if (p.isSpectator() || !p.isAlive()) return;
        if (level.getGameTime() < p.getPersistentData().getLong(UNTIL)) return;
        BlockPos at = p.blockPosition();
        Sector s = data.sectorAt(at);
        if (s == null || !s.isFloor() || !SectorGenerator.playable(s)) return;
        if (s.stairForward != null && onPad(p, s.stairForward)) use(level, data, p, s, true);
        else if (s.stairBack != null && onPad(p, s.stairBack)) use(level, data, p, s, false);
    }

    private static boolean onPad(ServerPlayer p, BlockPos exit) {
        return StairLinks.onPad(p.getX(), p.getY(), p.getZ(), exit.getX(), exit.getY(), exit.getZ());
    }

    /** Takes the player along the stairs of floor {@code s}: on ({@code forward}) or back. */
    static void use(ServerLevel level, MegaData data, ServerPlayer p, Sector s, boolean forward) {
        if (!MegaDungeon.mayEnter(p, s.polarity)) {                           // the lock says why
            cool(level, p, REFUSED_COOLDOWN);
            return;
        }
        if (forward) goOn(level, data, p, s);
        else goBack(level, data, p, s);
    }

    private static void goOn(ServerLevel level, MegaData data, ServerPlayer p, Sector s) {
        StairLinks.Dest d = StairLinks.forward(s.chainIndex);
        Sector t = data.chainSector(s.party, s.polarity, d.chainIndex());
        if (t == null) {                                                      // not built yet: say so, and make it so
            String why = ExpansionQueue.get().tryNext(level, s);
            if (why != null) LOG.info("[nusmp] The stairs of sector {} lead nowhere yet and the next floor was not queued: {}", s.id, why);
            say(p, "stairs.forming", "The stairs are still forming. Wait here: the next floor will be ready soon.", ChatFormatting.GRAY);
            cool(level, p, REFUSED_COOLDOWN);
            return;
        }
        if (!SectorGenerator.playable(t)) {
            if (t.state == Sector.State.FAILED) say(p, "stairs.broken", "The stairs lead nowhere: the floor failed to form. An admin can try again with /multiverse mega regen %s.", ChatFormatting.RED, t.id);
            else say(p, "stairs.forming", "The stairs are still forming. Wait here: the next floor will be ready soon.", ChatFormatting.GRAY);
            cool(level, p, REFUSED_COOLDOWN);
            return;
        }
        arrive(level, p, t, SectorGenerator.padOf(t), s.polarity == Polarity.HOLY);
    }

    private static void goBack(ServerLevel level, MegaData data, ServerPlayer p, Sector s) {
        StairLinks.Dest d = StairLinks.back(s.chainIndex);
        if (d.hub()) {                                                        // the first floor's stairs lead out of the Labyrinth: to Floor 0
            cool(level, p, USED_COOLDOWN);
            title(p, Component.translatableWithFallback("message.nusmp.mega.gen2.stairs.hub", "Floor 0"),
                    Component.translatableWithFallback("message.nusmp.mega.gen2.stairs.hub.sub", "You leave the Labyrinth by the stairs."));
            LabyrinthTravel.leaveAny(p);
            return;
        }
        Sector t = data.chainSector(s.party, s.polarity, d.chainIndex());
        if (t == null || !SectorGenerator.playable(t)) {
            say(p, "stairs.lost", "The stairs back are gone. Ask an admin to look at /multiverse mega status.", ChatFormatting.RED);
            cool(level, p, REFUSED_COOLDOWN);
            return;
        }
        BlockPos pad = d.arrivesAtForward() && t.stairForwardPad != null ? t.stairForwardPad : SectorGenerator.padOf(t);
        arrive(level, p, t, pad, s.polarity != Polarity.HOLY);
    }

    /** Sets the player down on {@code pad} of floor {@code t}, with the title and the memory of the deepest floor. {@code climbing} says which way the stairs went (the words only). */
    private static void arrive(ServerLevel level, ServerPlayer p, Sector t, BlockPos pad, boolean climbing) {
        if (pad == null) {
            say(p, "stairs.lost", "The stairs back are gone. Ask an admin to look at /multiverse mega status.", ChatFormatting.RED);
            cool(level, p, REFUSED_COOLDOWN);
            return;
        }
        String floor = FloorMap.label(t.firstFloor());
        LabyrinthTravel.arrive(p, level, pad, null);
        MegaTravel.remember(p, t);
        cool(level, p, USED_COOLDOWN);
        title(p, Component.translatableWithFallback("message.nusmp.mega.gen2.stairs.floor", "Floor %s", floor),
                Component.translatableWithFallback(climbing ? "message.nusmp.mega.gen2.stairs.climb" : "message.nusmp.mega.gen2.stairs.descend",
                        climbing ? "You climb the stairs." : "You go down the stairs."));
        NusmpEvents.fire(p, "mega.stairs", floor);
    }

    // ---------------------------------------------------------------- feedback

    /** A short title with a fade: in 6 ticks, 24 on screen, out in 10. */
    private static void title(ServerPlayer p, Component title, Component subtitle) {
        if (p.connection == null) return;                                      // a stand-in player of a self-test has no connection
        p.connection.send(new ClientboundSetTitlesAnimationPacket(6, 24, 10));
        p.connection.send(new ClientboundSetTitleTextPacket(title.copy().withStyle(ChatFormatting.WHITE)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
    }

    private static void say(ServerPlayer p, String key, String english, ChatFormatting colour, Object... args) {
        if (p.connection == null) return;                                      // a stand-in player of a self-test has no connection
        p.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.gen2." + key, english, args).withStyle(colour), true);
    }

    private static void cool(ServerLevel level, ServerPlayer p, long ticks) {
        p.getPersistentData().putLong(UNTIL, level.getGameTime() + ticks);
    }
}
