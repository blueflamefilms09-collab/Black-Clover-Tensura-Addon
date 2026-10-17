package com.newuniverse.nusmp.arc.hub;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.world.ArcWorldConfig;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.slf4j.Logger;

/**
 * Why: the Hub's own protection ({@code HubRules}) keeps the plaza, the way home and the two gates whole and leaves the rest of the Sanctuary to builders, which
 * is exactly right for a base and exactly wrong for the Chronicle: if one player can take the lectern or a column away, nobody can read the story or be carried
 * to an arena until an admin repairs it. This class is the same protection for the volume of {@link ChronicleSite} (the paved ring to six blocks over it, the
 * rotunda to the top of the obelisk), on the same paths the Hub's rules cover: breaking, placing (single and multi-block), blasts, the left click that starts a crack,
 * the buckets and flames a player carries (asked with a margin: they do their damage a few blocks from the click) and the tools that reshape a block in place.
 * Creative players are exempt, as everywhere in the Sanctuary. It is a separate class (not an edit of {@code HubProtect}, which is the plaza's box) and does
 * nothing while {@code arc.chronicle} is off. Server side only, never throws out of an event, messages at most once every few seconds.
 */
public final class HubSiteGuard {
    private HubSiteGuard() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String TOLD = "nusmp_arc_site_told";
    private static final int MESSAGE_INTERVAL = 60, BUCKET_MARGIN = 8, FIRE_MARGIN = 3;

    private static boolean registered;

    public static synchronized void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(HubSiteGuard::onBreak);
        bus.addListener(HubSiteGuard::onPlace);
        bus.addListener(HubSiteGuard::onExplosion);
        bus.addListener(HubSiteGuard::onLeftClick);
        bus.addListener(HubSiteGuard::onRightClick);
        bus.addListener(HubSiteGuard::onToolModify);
    }

    /** Is this cell inside the volume the site keeps whole (while the switch is on)? */
    public static boolean refuses(BlockPos pos) { return ArcWorldConfig.chronicle() && ChronicleSite.protectedCell(pos.getX(), pos.getY(), pos.getZ()); }

    /** Is this cell within {@code margin} blocks of it? */
    public static boolean refusesNear(BlockPos pos, int margin) { return ArcWorldConfig.chronicle() && ChronicleSite.near(pos.getX(), pos.getY(), pos.getZ(), margin); }

    private static boolean serverHub(Level level) { return level != null && !level.isClientSide() && Labyrinth.inSanctuary(level); }

    private static void onBreak(BlockEvent.BreakEvent e) {
        try {
            if (!(e.getLevel() instanceof Level level) || !serverHub(level)) return;
            Player p = e.getPlayer();
            if ((p != null && p.isCreative()) || !refuses(e.getPos())) return;
            e.setCanceled(true);
            tell(p);
        } catch (RuntimeException ex) {
            LOG.warn("The Chronicle's break guard failed: {}", ex.toString());
        }
    }

    /** Also receives the multi-block placements (beds, doors, tall plants), which extend this event: every block they replace is checked. */
    private static void onPlace(BlockEvent.EntityPlaceEvent e) {
        try {
            LevelAccessor acc = e.getLevel();
            if (!(acc instanceof Level level) || !serverHub(level)) return;
            Entity who = e.getEntity();
            if (who instanceof Player p && p.isCreative()) return;
            boolean refused = refuses(e.getPos());
            if (!refused && e instanceof BlockEvent.EntityMultiPlaceEvent multi) {
                for (BlockSnapshot s : multi.getReplacedBlockSnapshots()) {
                    if (refuses(s.getPos())) {
                        refused = true;
                        break;
                    }
                }
            }
            if (!refused) return;
            e.setCanceled(true);
            if (who instanceof Player p) tell(p);
        } catch (RuntimeException ex) {
            LOG.warn("The Chronicle's place guard failed: {}", ex.toString());
        }
    }

    /** A blast still hurts whoever stands in it; it takes no block of the site. */
    private static void onExplosion(ExplosionEvent.Detonate e) {
        try {
            if (!serverHub(e.getLevel())) return;
            e.getAffectedBlocks().removeIf(HubSiteGuard::refuses);
        } catch (RuntimeException ex) {
            LOG.warn("The Chronicle's explosion guard failed: {}", ex.toString());
        }
    }

    /** Stops the crack animation on a block of the site (the break event alone would let the client start breaking it and then put it back). */
    private static void onLeftClick(PlayerInteractEvent.LeftClickBlock e) {
        try {
            if (!serverHub(e.getLevel()) || e.getEntity().isCreative() || !refuses(e.getPos())) return;
            e.setCanceled(true);
            tell(e.getEntity());
        } catch (RuntimeException ex) {
            LOG.warn("The Chronicle's click guard failed: {}", ex.toString());
        }
    }

    /** A bucket (water and lava flow over the paving) and a flame (fire spreads through a lantern's neighbours): asked with a margin round the block and the one in front of it. */
    private static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        try {
            Level level = e.getLevel();
            if (!serverHub(level) || e.getEntity().isCreative()) return;
            Item item = e.getItemStack().getItem();
            BlockPos hit = e.getPos();
            BlockPos beside = e.getFace() == null ? hit : hit.relative(e.getFace());
            boolean refused;
            if (item instanceof BucketItem) refused = refusesNear(hit, BUCKET_MARGIN) || refusesNear(beside, BUCKET_MARGIN);
            else if (item instanceof FlintAndSteelItem || item instanceof FireChargeItem) refused = refusesNear(hit, FIRE_MARGIN) || refusesNear(beside, FIRE_MARGIN);
            else return;
            if (!refused) return;
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
            tell(e.getEntity());
        } catch (RuntimeException ex) {
            LOG.warn("The Chronicle's right-click guard failed: {}", ex.toString());
        }
    }

    /** An axe, a shovel or a hoe used on a block of the site changes it without a break or place event. */
    private static void onToolModify(BlockEvent.BlockToolModificationEvent e) {
        try {
            if (!(e.getLevel() instanceof Level level) || !serverHub(level)) return;
            Player p = e.getPlayer();
            if ((p != null && p.isCreative()) || !refuses(e.getPos())) return;
            e.setCanceled(true);
            if (!e.isSimulated()) tell(p);
        } catch (RuntimeException ex) {
            LOG.warn("The Chronicle's tool guard failed: {}", ex.toString());
        }
    }

    private static void tell(Player p) {
        if (p == null) return;
        long now = p.level().getGameTime();
        CompoundTag data = p.getPersistentData();
        if (now - data.getLong(TOLD) < MESSAGE_INTERVAL && data.getLong(TOLD) <= now) return;
        data.putLong(TOLD, now);
        p.displayClientMessage(Component.translatableWithFallback("message.nusmp.arc.chronicle.protected", "The Chronicle and the Promenade are protected."), true);
    }
}
