package com.newuniverse.nusmp.labyrinth.mega.party;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.floors.FloorsConfig;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemParty;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

/**
 * Why: the registration entry point of the dungeon parties (1.58.0), in the shape of every other Mega-Dungeon registry so {@code MegaSystem} wires the whole feature with the
 * two calls it makes for the others. {@code init} runs while the mod is constructed and registers what needs no server: the party layer as the System's way of finding the
 * members of a party ({@code SystemParty.setProvider}: a floor quest and a kill count for the teammates who are there, not for whoever happens to stand near) and the
 * in-game self-tests ({@code party.*}). {@code register} receives the game bus: the guard that keeps a party's floors to that party, and one look at the server's start that
 * writes down where the lanes are and warns when the world border of the dungeon would cut them off. It registers no block and no item, so it has no ordering constraint
 * against {@code NUItems.ITEMS}. The server config {@link PartyConfig#SPEC} and the {@link #command} node are attached by the lead.
 */
public final class PartyRegistry {
    private static final Logger LOG = LogUtils.getLogger();

    private PartyRegistry() {}

    public static void init(IEventBus modBus) {
        SystemParty.setProvider(PartyService::questMembers);
        PartySelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        PartyGuard.register(gameBus);
        gameBus.addListener(PartyRegistry::onServerStarted);
    }

    /** The {@code party} admin node ({@code list | of | tp | set}); the lead attaches it under {@code /multiverse mega}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() { return PartyCommand.command(); }

    /**
     * Writes down how the dungeon is split at the start and warns when the lanes do not fit the world border: the border of a dimension follows the Overworld's in vanilla, and a
     * server that keeps a small border has always cut off the Abyssal road (z = 300 000); with parties the lanes reach further, so the owner is told which z they need. Never throws.
     */
    private static void onServerStarted(ServerStartedEvent e) {
        try {
            if (!PartyService.instanced()) {
                LOG.info("[nusmp] Dungeon parties: the dungeon is shared (party.mode = {}, floor model = {}); everybody walks party 0.", PartyConfig.mode().id(), FloorsConfig.get(FloorsConfig.PER_SECTOR) <= 1);
                return;
            }
            int max = PartyService.maxParties();
            int far = PartyLanes.maxZ(max), near = PartyLanes.minZ(max);
            LOG.info("[nusmp] Dungeon parties: up to {} parties, lanes from z = {} (Holy road) to z = {} (Abyssal road).", max, near, far);
            for (Sector s : MegaData.get(e.getServer()).sectors()) {
                if (!s.isFloor() && !PartyLanes.insideBand(s.region.minZ(), s.region.maxZ(), s.party)) {
                    LOG.warn("[nusmp] Sector {} (the legacy ten-floor model) winds through z {}..{}, farther than half a lane step ({}) from its lane: it could meet the floors of another party. Set party.mode to shared to be safe.",
                            s.id, s.region.minZ(), s.region.maxZ(), MegaGrid.PARTY_STEP / 2);
                }
            }
            ServerLevel mega = MegaDungeon.level(e.getServer());
            if (mega == null) return;
            if (!mega.getWorldBorder().isWithinBounds(new BlockPos(0, 64, far)) || !mega.getWorldBorder().isWithinBounds(new BlockPos(0, 64, near))) {
                LOG.warn("[nusmp] The world border of the Mega-Dungeon does not contain the lanes of {} parties (z = {} to z = {}). Parties whose lane is outside it take border damage; widen the border or lower party.maxParties.", max, near, far);
            }
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] The party lanes could not be checked against the world border: {}", ex.toString());
        }
    }
}
