package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleRules.Phase;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeConfig;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeDef;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeDefs;
import com.newuniverse.nusmp.labyrinth.vault.RelicPedestalBlockEntity;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: this is the line the whole Crucible stands on: "taking ONE relic forfeits the rest, and the maximum output is exactly one relic per region".
 * It is installed in front of {@code RelicPedestalBlockEntity.take} as a hook (additive, so a Spire pedestal and one an admin placed work exactly as
 * before: the hook answers null for any pedestal that is not in a sector's saved pedestal list). For a Crucible pedestal it judges the click with
 * {@link CrucibleRules#take} and, on a legal pick, does SIX things in this order, and the order is the guarantee: (1) it writes Phase CHOSEN, the
 * chooser, the chosen id and the drawn catastrophe into the sector FIRST, so whatever fails afterwards, and whoever clicks next (a second player in
 * the same tick, a double click), finds the vault already taken and gets "too late": the rule is enforced by data and ordering, not by trust;
 * (2) it issues ONLY the chosen relic through the God of Order's ledger ({@code UniqueLedger.issue}); if the ledger refuses because the relic went
 * live meanwhile (another vault, an admin) it rolls ONE replacement and retries once, else tells the player and ends the vault; (3) it gives the
 * stack (inventory, else the floor); (4) in a {@code finally}, so even an exception cannot leave a relic on a pedestal, it empties EVERY pedestal of
 * the vault in the same call (the others crumble to dust, the offered relics stay unissued and so stay in the world's pool); (5) it fires the quest
 * hook and starts the catastrophe, which then runs on its own clock; (6) it marks the vault RESOLVED and tells {@code MegaDungeon.crucibleResolved}, which
 * releases the next sector (its theme is the road of the relic just taken): the catastrophe is an aftermath, not part of the choice. A failure inside the
 * hook is logged and answered with "nothing given": it never falls through to the old pick, which would hand a preview (a relic that was never issued) to
 * the player.
 * <p>
 * The {@link #mayDrop drop guard} is the same rule for the other door: a Crucible pedestal that is removed must NOT drop the relic it shows, because
 * what it shows is a preview; the old drop-on-removal exists to save ISSUED relics.
 */
public final class CrucibleHook {
    private CrucibleHook() {}

    private static final Logger LOG = LogUtils.getLogger();

    // ---------------------------------------------------------------- the take hook
    /** {@code RelicPedestalBlockEntity.TakeHook}: null = not a Crucible pedestal (the old behaviour runs), TRUE = a relic was given, FALSE = handled, nothing given. */
    public static Boolean onTake(ServerPlayer player, RelicPedestalBlockEntity be) {
        ServerLevel level;
        Sector s;
        List<BlockPos> slots;
        int slot;
        try {
            if (!(be.getLevel() instanceof ServerLevel sl) || !MegaDungeon.in(sl)) return null;
            level = sl;
            s = MegaDungeon.sectorAt(sl, be.getBlockPos());
            if (s == null) return null;
            slots = CruciblePedestals.ordered(s);
            slot = slots.indexOf(be.getBlockPos());
            if (slot < 0) return null;                                              // a pedestal that is not this sector's Crucible: the old behaviour
        } catch (RuntimeException notOurs) {
            return null;
        }
        if (!ThemeConfig.get(ThemeConfig.CRUCIBLE_ENABLED)) return null;
        try {
            return pick(level, s, slots, slot, player);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] mega crucible: a pick in sector {} failed: {}", s.id, e.toString());
            return Boolean.FALSE;                                                   // fail closed: never fall through to the old pick with a preview in the pedestal
        }
    }

    private static Boolean pick(ServerLevel level, Sector s, List<BlockPos> slots, int slot, ServerPlayer player) {
        Sector.Crucible c = s.crucible;
        CrucibleRules.Pick r = CrucibleRules.take(Phase.of(c.phase.name()), slot, c.offered);
        switch (r.verdict()) {
            case NOT_OFFERED -> { tell(player, "not_offered"); return Boolean.FALSE; }
            case TOO_LATE -> { tell(player, "too_late"); return Boolean.FALSE; }
            case EMPTY_SLOT, UNKNOWN_SLOT -> { tell(player, "empty_slot"); return Boolean.FALSE; }
            case PICK -> { }
        }
        MegaData data = MegaData.get(level.getServer());
        ThemeDef theme = ThemeDefs.get(s.theme);
        BlockPos center = CruciblePedestals.center(s);
        BlockPos pickedAt = slots.get(slot);

        // the ids on offer in this vault, remembered before the lock empties their slots: a replacement must not be one of the relics that just crumbled
        Set<String> shown = new HashSet<>();
        for (String id : c.offered) if (id != null && !id.isEmpty()) shown.add(id);

        // ---- 1. the lock: CHOSEN is written before anything else can fail or anybody else can click
        c.phase = Sector.Crucible.Phase.valueOf(CrucibleRules.afterPick().name());
        c.chosen = r.relicId();
        c.chooser = player.getUUID();
        c.since = level.getGameTime();
        for (int i : r.crumble()) if (i < c.offered.size()) c.offered.set(i, "");
        c.catastrophe = CrucibleRules.chooseCatastrophe(theme.catastrophes(), s.seed, player.getUUID(), CursedSpirits.available());
        data.setDirty();

        boolean delivered = false;
        ItemStack stack = ItemStack.EMPTY;
        boolean swapped = false;
        boolean refused = false;
        try {
            // ---- 2. issue ONLY the chosen relic through the ledger
            RelicDef chosen = Relics.byId(r.relicId());
            stack = chosen == null ? ItemStack.EMPTY : RelicCatalog.newStack(chosen);
            boolean issued = !stack.isEmpty() && UniqueLedger.issue(level, chosen.id(), stack);
            if (!issued) {                                                          // it went live meanwhile: one replacement, one retry
                Set<String> excluded = CrucibleOffer.offeredElsewhere(data, s);
                excluded.addAll(shown);
                RelicDef alt = CrucibleOffer.replacement(level, r.relicId(), excluded);
                if (alt != null) {
                    stack = RelicCatalog.newStack(alt);
                    issued = !stack.isEmpty() && UniqueLedger.issue(level, alt.id(), stack);
                    if (issued) {
                        chosen = alt;
                        swapped = true;
                        c.chosen = alt.id();
                        data.setDirty();
                    }
                }
            }
            if (!issued) {
                refused = true;
                stack = ItemStack.EMPTY;
            } else {
                // ---- 3. give it: inventory first, else at the player's feet (the old pick's rule)
                Component name = stack.getHoverName();
                if (!player.getInventory().add(stack) && !stack.isEmpty()) Block.popResource(level, player.blockPosition(), stack);
                delivered = true;
                try {
                    announce(level, center, player, name, swapped, chosen.id());
                } catch (RuntimeException chatFailed) {
                    LOG.warn("[nusmp] mega crucible: could not announce the pick in sector {}: {}", s.id, chatFailed.toString());
                }
                NusmpEvents.fire(player, "mega.crucible_choice", chosen.id());                // never throws: the quest hook swallows its own errors
            }
        } finally {
            // ---- 4. whatever happened, no pedestal of this vault holds a relic any more
            try {
                Vec3 at = Vec3.atCenterOf(pickedAt);
                for (int i : r.crumble()) CrucibleOffer.crumbleEffects(level, slots.get(i));
                if (delivered) {
                    Fx.spray(level, FxThemes.DUNGEON_CRUCIBLE, FxOld.END_ROD, at.x, at.y + 0.9, at.z, 24, 0.25, 0.25, 0.25, 0.04);
                    Fx.spray(level, FxThemes.DUNGEON_CRUCIBLE, FxOld.ENCHANT, at.x, at.y + 1.2, at.z, 40, 0.4, 0.4, 0.4, 0.6);
                    level.playSound(null, pickedAt, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.5f);
                    level.playSound(null, pickedAt, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.0f);
                }
                CrucibleOffer.clearAll(level, s, true);
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] mega crucible: could not finish emptying the pedestals of sector {}: {}", s.id, e.toString());
            }
        }
        if (refused) {
            c.chosen = "";                                                          // nobody got a relic: the vault ends like an empty one, and the next theme does not follow a relic nobody holds
            data.setDirty();
            tellChat(player, "refused");
            end(level, s);
            return Boolean.FALSE;
        }
        // ---- 5. the catastrophe starts (the quest event fired above); from here it runs on its own clock
        CatastropheRunner.start(level, s, c.catastrophe, player, center, true);
        // ---- 6. the vault is done the moment the relic is in hand: RESOLVED and MegaDungeon.crucibleResolved release the next sector, whose theme is the relic's road
        end(level, s);
        return Boolean.TRUE;
    }

    /** CHOSEN becomes RESOLVED (the pick is made, or the ledger refused) and the listeners hear it: the next sector may be built. */
    private static void end(ServerLevel level, Sector s) {
        Sector.Crucible c = s.crucible;
        c.phase = Sector.Crucible.Phase.valueOf(CrucibleRules.afterCatastrophe(Phase.of(c.phase.name())).name());
        MegaData.get(level.getServer()).setDirty();
        MegaDungeon.crucibleResolved(level, s.id);
    }

    private static void announce(ServerLevel level, BlockPos center, ServerPlayer chooser, Component relic, boolean swapped, String relicId) {
        Component line = Component.translatable("message.nusmp.mega.crucible.chosen", chooser.getDisplayName(), relic).withStyle(ChatFormatting.GOLD);
        Component road = ThemeConfig.get(ThemeConfig.SHOW_ROADS)
                ? Component.translatable("message.nusmp.mega.crucible.road", NextTheme.describe(NextTheme.roadOfRelic(relicId))).withStyle(ChatFormatting.AQUA) : null;
        for (ServerPlayer p : CruciblePedestals.playersInBox(level, center)) {
            p.displayClientMessage(line, false);
            p.displayClientMessage(Component.translatable("message.nusmp.mega.crucible.crumble").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            if (road != null) p.displayClientMessage(road, false);
        }
        if (swapped) chooser.displayClientMessage(Component.translatable("message.nusmp.mega.crucible.swapped").withStyle(ChatFormatting.GRAY), true);
    }

    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.nusmp.mega.crucible." + key).withStyle(ChatFormatting.GRAY), true);
    }

    private static void tellChat(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.nusmp.mega.crucible." + key).withStyle(ChatFormatting.RED), false);
    }

    // ---------------------------------------------------------------- the drop guard
    /**
     * {@code RelicPedestalBlock.DropGuard}: may the relic a removed pedestal shows be dropped? Never for a Crucible pedestal (in every phase, what it
     * shows is a preview of a relic that was never issued; the chosen relic is handed to the player directly and is never held on a pedestal);
     * always for any other pedestal (the old behaviour: an issued relic must not be lost with its block).
     */
    public static boolean mayDrop(Level level, BlockPos pos) {
        try {
            if (!(level instanceof ServerLevel sl) || !MegaDungeon.in(sl) || pos == null) return true;
            Sector s = MegaDungeon.sectorAt(sl, pos);
            return s == null || !s.pedestals.contains(pos);
        } catch (RuntimeException e) {
            return true;
        }
    }
}
