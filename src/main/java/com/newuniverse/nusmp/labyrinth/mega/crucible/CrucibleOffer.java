package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleRules.Phase;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeConfig;
import com.newuniverse.nusmp.labyrinth.vault.RelicPedestalBlockEntity;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicLoot;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the first half of the Crucible's life: deciding WHICH relics the vault offers and putting them on the pedestals WITHOUT issuing any of them.
 * The offer is drawn when the first survival player steps into the vault (not when the sector is generated: at generation there is nobody to
 * see it, the world's unissued relics change over days, and a roll made long before the party arrives would offer relics somebody else has since
 * found). It is always three DISTINCT relics (the {@code offerCount} config, at most the vault's pedestals) of the grades from {@code minGrade}
 * (Unique) up to Genesis, drawn weighted by each grade's loot weight so Genesis stays very rare, whatever the sector's depth or theme: the Crucible is
 * the only place a relic of those grades can be found in the Mega-Dungeon, so a vault is never a lesser prize than the one before it. The relic taken decides
 * the road of the next ten floors, so the three are also chosen to lead to three DIFFERENT roads when the world's unissued relics allow it (a dozen are drawn
 * by weight and {@link CrucibleRules#distinctRoads} keeps the first three with different themes): a choice between three relics of one road is not a choice. It goes through
 * {@code RelicLoot.peekUnissued}, which never touches the God of Order's ledger: the unchosen relics must stay unissued, or taking one would burn the
 * others for ever. Fewer than three unissued relics of those grades left: the vault offers what exists; none: it is resolved empty with a message. The saved offer
 * ({@code Sector.Crucible.offered}, ids in slot order) is the truth and the pedestals only SHOW it, which is why {@link #show} can repair a
 * pedestal that lost its stack (a regenerated sector, an admin's {@code /data}) from the saved ids alone. Relics already on offer in another
 * sector's vault are excluded from the draw, so two vaults never show the same relic (the second would find it gone when it was picked).
 */
final class CrucibleOffer {
    private CrucibleOffer() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<Integer> WARNED = new HashSet<>();

    static void clearWarnings() { WARNED.clear(); }

    // ---------------------------------------------------------------- the draw
    /** The relic ids on offer in the ROLLED vaults of every sector but {@code self}. */
    static Set<String> offeredElsewhere(MegaData data, Sector self) {
        Set<String> out = new HashSet<>();
        for (Sector t : data.sectors()) {
            if (t == self || t.crucible.phase != Sector.Crucible.Phase.ROLLED) continue;
            for (String id : t.crucible.offered) if (id != null && !id.isEmpty()) out.add(id);
        }
        return out;
    }

    /**
     * Up to {@code count} DISTINCT relics of the grades from the configured minimum up to Genesis, weighted by the grades' loot weights, never a pair (a
     * pair is two items, and a pick is exactly one relic) and never one in {@code excluded}; nothing is issued. Fewer come back when fewer qualify.
     */
    static List<RelicDef> pick(ServerLevel level, int count, Set<String> excluded) {
        List<RelicDef> drawn = RelicLoot.peekUnissued(level, level.getRandom(), ThemeConfig.minGrade(), Grade.GENESIS, Math.max(count, CrucibleRules.DRAW_POOL),
                d -> !d.hasPair() && !excluded.contains(d.id()));
        List<String> roads = new java.util.ArrayList<>();
        for (RelicDef d : drawn) roads.add(NextTheme.roadOfRelic(d.id()).theme().id());
        List<RelicDef> out = new java.util.ArrayList<>();
        for (int i : CrucibleRules.distinctRoads(roads, count)) out.add(drawn.get(i));                  // three relics that lead to three different roads, where the world has them
        return out;
    }

    /** One replacement relic for a pick whose relic went live meanwhile: the same grades, never the lost one or one in {@code excluded}; null when none is left. */
    static RelicDef replacement(ServerLevel level, String lostId, Set<String> excluded) {
        List<RelicDef> r = RelicLoot.peekUnissued(level, level.getRandom(), ThemeConfig.minGrade(), Grade.GENESIS, 1,
                d -> !d.hasPair() && !d.id().equals(lostId) && !excluded.contains(d.id()));
        return r.isEmpty() ? null : r.get(0);
    }

    // ---------------------------------------------------------------- roll
    /**
     * Draws and shows the offer of a vault nobody opened. Returns true when the vault left UNOPENED (ROLLED, or RESOLVED when the world had no relic of
     * that depth left). Needs the sector to have pedestals; a sector without any is warned about once and left alone.
     */
    static boolean roll(ServerLevel level, Sector s) {
        Sector.Crucible c = s.crucible;
        if (!CrucibleRules.shouldRoll(Phase.of(c.phase.name()))) return false;
        List<BlockPos> slots = CruciblePedestals.ordered(s);
        if (slots.isEmpty()) {
            if (WARNED.add(s.id)) LOG.warn("[nusmp] mega crucible: sector {} has no pedestal marker, so its vault cannot offer relics", s.id);
            return false;
        }
        MegaData data = MegaData.get(level.getServer());
        int count = CrucibleRules.offerCount(ThemeConfig.get(ThemeConfig.OFFER_COUNT), slots.size());
        List<RelicDef> defs = pick(level, count, offeredElsewhere(data, s));
        c.offered.clear();
        for (int i = 0; i < count; i++) c.offered.add(i < defs.size() ? defs.get(i).id() : "");
        c.chosen = "";
        c.chooser = null;
        c.catastrophe = "";
        c.since = level.getGameTime();
        c.phase = Sector.Crucible.Phase.valueOf(CrucibleRules.afterRoll(defs.size()).name());
        data.setDirty();
        BlockPos center = CruciblePedestals.center(s);
        if (defs.isEmpty()) {
            for (ServerPlayer p : CruciblePedestals.playersInBox(level, center)) p.displayClientMessage(Component.translatable("message.nusmp.mega.crucible.exhausted").withStyle(ChatFormatting.GRAY), false);
            MegaDungeon.crucibleResolved(level, s.id);
            return true;
        }
        show(level, s);
        List<ServerPlayer> inside = CruciblePedestals.playersInBox(level, center);
        for (ServerPlayer p : inside)
            p.displayClientMessage(Component.translatable("message.nusmp.mega.crucible.offered", defs.size()).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
        if (ThemeConfig.get(ThemeConfig.SHOW_ROADS)) {                               // a choice is only a choice when the party knows what each relic leads to
            for (RelicDef d : defs) {
                Component line = Component.translatable("message.nusmp.mega.crucible.offer_line", Component.literal(d.cardName()).withStyle(d.grade().colour()), d.grade().shortName(),
                        NextTheme.describe(NextTheme.roadOfRelic(d.id()))).withStyle(ChatFormatting.GRAY);
                for (ServerPlayer p : inside) p.displayClientMessage(line, false);
            }
        }
        if (center != null) {
            level.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.6f);
            level.sendParticles(ParticleTypes.END_ROD, center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5, 40, 3.0, 1.5, 3.0, 0.02);
        }
        return true;
    }

    // ---------------------------------------------------------------- show / repair
    /** The unissued stack a pedestal shows for a relic id: a plain stack, no serial, not in the ledger. EMPTY when the relic is not registered. */
    static ItemStack preview(String relicId) {
        RelicDef d = Relics.byId(relicId);
        return d == null ? ItemStack.EMPTY : RelicCatalog.newStack(d);
    }

    /**
     * Makes the pedestals of a ROLLED vault show the saved offer: a slot whose pedestal shows something else (or nothing) is refilled from the saved id, a
     * slot with no id is emptied, and a relic that no longer exists in the mod is dropped from the offer. Only loaded pedestals are touched. Returns how
     * many pedestals were changed.
     */
    static int show(ServerLevel level, Sector s) {
        List<BlockPos> slots = CruciblePedestals.ordered(s);
        List<String> offered = s.crucible.offered;
        int changed = 0;
        boolean dirty = false;
        for (int i = 0; i < slots.size(); i++) {
            RelicPedestalBlockEntity be = CruciblePedestals.entity(level, slots.get(i));
            if (be == null) continue;
            String id = i < offered.size() ? offered.get(i) : "";
            if (id == null || id.isEmpty()) {
                if (!be.getHeld().isEmpty()) { be.setHeld(ItemStack.EMPTY); changed++; }
                continue;
            }
            RelicDef d = Relics.byId(id);
            ItemStack stack = d == null ? ItemStack.EMPTY : RelicCatalog.newStack(d);
            if (stack.isEmpty()) {
                offered.set(i, "");                                                  // the relic is gone from the mod: that slot is simply empty
                dirty = true;
                if (!be.getHeld().isEmpty()) be.setHeld(ItemStack.EMPTY);
                continue;
            }
            if (Relics.defOf(be.getHeld()) != d) {
                be.setHeld(stack);
                changed++;
            }
        }
        if (dirty) MegaData.get(level.getServer()).setDirty();
        return changed;
    }

    /**
     * Empties every pedestal of the vault (the pick, a refusal, a reset, a stale preview found in a finished vault). {@code taken} marks them taken (the
     * pick) or fresh (a reset). Effects are the caller's business. Returns how many held a stack.
     */
    static int clearAll(ServerLevel level, Sector s, boolean taken) {
        int cleared = 0;
        for (BlockPos pos : CruciblePedestals.ordered(s)) {
            RelicPedestalBlockEntity be = CruciblePedestals.entity(level, pos);
            if (be == null) continue;
            if (!be.getHeld().isEmpty()) cleared++;
            be.setHeld(ItemStack.EMPTY);
            if (be.isTaken() != taken) be.setTaken(taken);
        }
        return cleared;
    }

    /** How closely a player has to look at a relic to read its name: the cosine of about 15 degrees. */
    static final double INSPECT_COS = 0.965;
    /** How far away (blocks) a relic can be read. */
    static final double INSPECT_RANGE = 7.0;

    /**
     * The slot of the relic a player is looking at (within {@link #INSPECT_COS} of the line to it and {@link #INSPECT_RANGE} blocks), or -1: a choice is only a
     * choice when each relic can be read before it is taken, and a right-click on a pedestal IS the pick, so the name has to come from looking. Pure geometry.
     * @param eye the player's eyes, {@code look} the unit look vector, {@code relics} the centre of each floating relic (null entries: nothing there)
     */
    static int lookedAt(Vec3 eye, Vec3 look, List<Vec3> relics) {
        int best = -1;
        double bestDot = INSPECT_COS;
        for (int i = 0; i < relics.size(); i++) {
            Vec3 at = relics.get(i);
            if (at == null) continue;
            Vec3 to = at.subtract(eye);
            double d = to.length();
            if (d < 0.5 || d > INSPECT_RANGE) continue;
            double dot = to.scale(1.0 / d).dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = i;
            }
        }
        return best;
    }

    /**
     * Shows, on the action bar, the name, the grade and the road of the offered relic the player is looking at ({@code showRoads}). Called by the vault ticker
     * every half second for a player in a ROLLED vault; does nothing for anything else.
     */
    static void inspect(Sector s, ServerPlayer p) {
        if (!ThemeConfig.get(ThemeConfig.SHOW_ROADS)) return;
        List<BlockPos> slots = CruciblePedestals.ordered(s);
        List<String> offered = s.crucible.offered;
        List<Vec3> at = new java.util.ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            boolean there = i < offered.size() && offered.get(i) != null && !offered.get(i).isEmpty();
            at.add(there ? Vec3.atCenterOf(slots.get(i)).add(0, 0.8, 0) : null);                // the floating relic hangs about 1.3 blocks above its block (the centre is half a block up)
        }
        int slot = lookedAt(p.getEyePosition(), p.getLookAngle(), at);
        if (slot < 0) return;
        RelicDef d = Relics.byId(offered.get(slot));
        if (d == null) return;
        p.displayClientMessage(Component.translatable("message.nusmp.mega.crucible.inspect", Component.literal(d.cardName()).withStyle(d.grade().colour()), d.grade().shortName(),
                NextTheme.describe(NextTheme.roadOfRelic(d.id()))), true);
    }

    /** The puff of dust where a relic crumbled (or was taken): a pedestal's floating relic hangs about 1.3 blocks above its block. */
    static void crumbleEffects(ServerLevel level, BlockPos pedestal) {
        Vec3 c = Vec3.atCenterOf(pedestal).add(0, 1.0, 0);
        level.sendParticles(ParticleTypes.POOF, c.x, c.y, c.z, 14, 0.25, 0.3, 0.25, 0.05);
        level.sendParticles(ParticleTypes.ASH, c.x, c.y, c.z, 30, 0.35, 0.45, 0.35, 0.02);
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.FALLING_DUST, net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState()),
                c.x, c.y + 0.4, c.z, 36, 0.4, 0.4, 0.4, 0.0);
        level.playSound(null, pedestal, SoundEvents.DEEPSLATE_BRICKS_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
        level.playSound(null, pedestal, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 0.8f, 0.6f);
    }
}
