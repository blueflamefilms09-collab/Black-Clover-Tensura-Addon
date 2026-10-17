package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.seal.GatekeeperDoorBlock;
import com.newuniverse.nusmp.labyrinth.seal.ShadowKeyItem;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the new {@code kind=sector} of the Gatekeeper door answers to exactly one thing, the Mana Core of its own sector, and the code that
 * says so is kept out of {@code GatekeeperDoorBlock} (which only gained one enum entry and two short branches that call this class) so the
 * existing doors stay byte for byte what they were and the whole sector-door behaviour can be read in one file. A click with a core goes
 * through {@link SectorDoorRules} (is it forged for anything, does this door belong to a sector, is it THIS sector's core, may the sector
 * open); a verdict of OPEN opens the whole 3 x 4 x 2 slab by the existing flood ({@code GatekeeperDoorBlock.openConnected}, 24 blocks, well
 * under its limit of 64), THEN consumes the core (a click that opened nothing costs nothing), fires the quest hook, and calls
 * {@link MegaDungeon#unseal}, which raises the expansion level and asks the generator for the next sector. The Shadow Key is blind to this
 * door (it says so), and nothing else can open it: the block has no neighbour logic for redstone, pushes nothing, has the explosion
 * resistance of bedrock, and the Dungeon Guard holds the dimension.
 */
public final class SectorDoor {
    private SectorDoor() {}

    /** Players this close to the opened door share the credit of clearing the sector: a party opens a seal together, and the door has one clicker. */
    private static final double CREDIT_RADIUS = 32.0;

    /** A right click on a closed sector door with {@code stack}. Called from {@code GatekeeperDoorBlock.useItemOn} for {@code kind=sector} only. */
    public static ItemInteractionResult useItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (state.getValue(GatekeeperDoorBlock.OPEN)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean core = stack.getItem() instanceof ManaCoreItem, key = stack.getItem() instanceof ShadowKeyItem;
        if (level.isClientSide) return core || key ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (key) {                                                                          // the Shadow Key does nothing here
            tell(player, "message.nusmp.gatekeeper.sector_sealed");
            return ItemInteractionResult.CONSUME;
        }
        if (!core || !(level instanceof ServerLevel sl)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return useCore(stack, sl, pos, player);
    }

    private static ItemInteractionResult useCore(ItemStack stack, ServerLevel sl, BlockPos pos, Player player) {
        Sector sector = MegaDungeon.sectorAt(sl, pos);
        boolean ready = sector != null && sector.state != Sector.State.GENERATING && sector.state != Sector.State.FAILED;
        SectorDoorRules.Verdict verdict = SectorDoorRules.check(ManaCoreItem.sectorOf(stack), sector == null ? -1 : sector.id, ready);
        switch (verdict) {
            case DORMANT -> { tell(player, "message.nusmp.mega.mgd.core_dormant"); return ItemInteractionResult.CONSUME; }
            case NOT_HERE, WRONG_SECTOR -> { tell(player, "message.nusmp.mega.mgd.core_wrong_seal"); return ItemInteractionResult.CONSUME; }
            case NOT_READY -> { tell(player, "message.nusmp.mega.mgd.core_not_ready"); return ItemInteractionResult.CONSUME; }
            default -> { }
        }
        int opened = GatekeeperDoorBlock.openConnected(sl, pos);
        if (opened <= 0) return ItemInteractionResult.FAIL;                                // nothing closed in reach: the core is not spent
        if (!player.isCreative()) stack.shrink(1);
        sl.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.2f, 0.5f);
        sl.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.4f);
        sl.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 40, 1.2, 1.4, 0.8, 0.02);
        sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 24, 1.0, 1.4, 0.6, 0.04);
        sl.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 60, 1.4, 1.6, 1.0, 0.8);
        tell(player, "message.nusmp.mega.mgd.door_open");
        creditParty(sl, pos, player, sector.id);
        try {
            MegaDungeon.unseal(sl, sector.id);
        } catch (RuntimeException e) {
            GuardLog.failed("unseal", e);                                                  // a listener of another system must not kick the player: the door is open and the core is spent already
        }
        return ItemInteractionResult.CONSUME;
    }

    /**
     * The quest hook {@code mega.sector_cleared} (argument: the sector id): the player who opened the door and every living player within
     * {@link #CREDIT_RADIUS} blocks of it, each once, with the squad sharing {@code NusmpEvents.fireAll} applies on top (the way a boss kill is credited).
     * A stand-in that is no {@code ServerPlayer} (the in-game self-tests) credits nobody, and the hook never throws.
     */
    private static void creditParty(ServerLevel sl, BlockPos pos, Player opener, int sectorId) {
        try {
            List<ServerPlayer> party = new ArrayList<>();
            if (opener instanceof ServerPlayer sp) party.add(sp);
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(CREDIT_RADIUS), x -> x.isAlive() && !x.isSpectator())) {
                if (!party.contains(p)) party.add(p);
            }
            if (!party.isEmpty()) NusmpEvents.fireAll(party, "mega.sector_cleared", Integer.toString(sectorId));
        } catch (RuntimeException e) {
            GuardLog.failed("quest credit", e);
        }
    }

    private static void tell(Player player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }
}
