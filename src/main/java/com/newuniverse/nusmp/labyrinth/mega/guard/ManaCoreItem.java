package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the Mana Core is the one thing a sector's Labyrinth Guardian leaves behind, and it is a KEY with three jobs: to be unmistakable
 * (epic rarity, the foil sheen, a glow outline on the ground so it can be found across the hall), to be unlosable (the dropped item never
 * despawns, cannot be burnt, blasted or killed, and a core that falls into a void gap is carried back to its sector's altar, because a
 * party that loses the only key to the next sector is stuck), and to be honest about what it opens (the tooltip names the sector and
 * coloured by the sector's theme; a blank core from the creative tab says it is dormant). It is a plain {@link Item} whose only state is the
 * {@link SectorCore} data component, so it needs no registry of live cores: the door just reads the stack it is clicked with
 * ({@link SectorDoor}). Used in the air it points at its door ({@link CoreHint}), for a party that fled the hall with it. It has no recipe
 * and no loot table: the only sources are the Guardian Altar (and its re-forge) and the admin command.
 */
public class ManaCoreItem extends Item {
    public ManaCoreItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    // ---------------------------------------------------------------- the stack
    /** A core for this sector, with the theme written in for the tooltip. */
    public static ItemStack forSector(int sector, RegionTheme theme) {
        ItemStack stack = new ItemStack(GuardRegistry.MANA_CORE.get());
        stack.set(GuardRegistry.SECTOR_CORE.get(), new SectorCore(sector, theme == null ? "" : theme.id()));
        return stack;
    }

    /** The data of a core stack, or null for a blank core or a stack of anything else. */
    public static SectorCore coreOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ManaCoreItem)) return null;
        return stack.get(GuardRegistry.SECTOR_CORE.get());
    }

    /** The sector a stack names, or -1 when it is not a core or names none. */
    public static int sectorOf(ItemStack stack) {
        SectorCore c = coreOf(stack);
        return c == null ? -1 : c.sector();
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    // ---------------------------------------------------------------- tooltip
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        SectorCore c = coreOf(stack);
        if (c == null) {
            tooltip.add(Component.translatable("item.nusmp.mana_core.tooltip.dormant").withStyle(ChatFormatting.GRAY));
        } else {
            RegionTheme theme = c.theme().isEmpty() ? null : RegionTheme.fromId(c.theme());
            int rgb = GuardianScaling.themeColor(theme);
            tooltip.add(Component.translatable("item.nusmp.mana_core.tooltip.sector", c.sector()).withStyle(s -> s.withColor(rgb)));
            if (theme != null) {
                tooltip.add(Component.translatable("item.nusmp.mana_core.tooltip.theme", Component.translatable("message.nusmp.mega.mgd.theme." + theme.id()))
                        .withStyle(s -> s.withColor(rgb)));
            }
            tooltip.add(Component.translatable("item.nusmp.mana_core.tooltip.unseals", c.sector()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.translatable("item.nusmp.mana_core.tooltip.forged").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    // ---------------------------------------------------------------- use: which way is the seal
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        try {
            player.displayClientMessage(hint(level, player, coreOf(stack)), true);
        } catch (RuntimeException e) {
            GuardLog.failed("core hint", e);                                               // a click that throws would disconnect the player
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /** The action-bar line: dormant, silent outside the dungeon, lost, right here, or "N blocks to the north east". */
    private static Component hint(Level level, Player player, SectorCore core) {
        if (core == null) return Component.translatable("message.nusmp.mega.mgd.core_dormant").withStyle(ChatFormatting.GRAY);
        if (!Labyrinth.inMega(level) || !(level instanceof ServerLevel sl)) return Component.translatable("message.nusmp.mega.mgd.core_silent").withStyle(ChatFormatting.GRAY);
        Sector sector = MegaData.get(sl.getServer()).sector(core.sector());
        BlockPos door = sector == null ? null : sector.gatekeeper;
        if (door == null) return Component.translatable("message.nusmp.mega.mgd.core_lost").withStyle(ChatFormatting.GRAY);
        double dx = door.getX() + 0.5 - player.getX(), dz = door.getZ() + 0.5 - player.getZ();
        int dist = CoreHint.distance(dx, dz);
        if (dist <= CoreHint.HERE) return Component.translatable("message.nusmp.mega.mgd.core_here").withStyle(ChatFormatting.LIGHT_PURPLE);
        return Component.translatable("message.nusmp.mega.mgd.core_pull", dist, Component.translatable("message.nusmp.mega.mgd.dir." + CoreHint.direction(dx, dz)))
                .withStyle(s -> s.withColor(GuardianScaling.themeColor(sector.theme)));
    }

    // ---------------------------------------------------------------- the dropped core: never lost
    /**
     * Called every tick of an item entity that holds a core (NeoForge's item-extension hook, the pattern of the Charter Grimoire): it never
     * despawns, nothing can destroy it, it glows through walls, it sparkles now and then, and one that has fallen out of the dungeon's floor
     * is put back on its sector's altar. Returns false so the entity ticks normally.
     */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        protect(entity);
        if (!(entity.level() instanceof ServerLevel sl)) return false;
        try {
            long t = sl.getGameTime();
            if (t % 10 == 0) Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.END_ROD, entity.getX(), entity.getY() + 0.3, entity.getZ(), 1, 0.15, 0.2, 0.15, 0.01);
            if (t % 40 == 0 && Labyrinth.inMega(sl) && entity.getY() < MegaGrid.ORIGIN_Y - 6) rescue(sl, stack, entity);
        } catch (RuntimeException e) {
            GuardLog.failed("core update", e);                                             // an item entity that throws in its update would crash the tick
        }
        return false;
    }

    /** The three flags that make a core indestructible and findable (also applied the moment the entity joins the world). */
    public static void protect(ItemEntity entity) {
        entity.setUnlimitedLifetime();
        entity.setInvulnerable(true);
        if (!entity.hasGlowingTag()) entity.setGlowingTag(true);
    }

    /** A core below the dungeon's lowest room fell through a gap: back to the top of its sector's altar if that chunk is loaded (else the re-forge covers it). */
    private static void rescue(ServerLevel sl, ItemStack stack, ItemEntity entity) {
        int id = sectorOf(stack);
        if (id < 0) return;
        Sector sector = MegaData.get(sl.getServer()).sector(id);
        BlockPos at = sector == null ? null : sector.guardianSpawn != null ? sector.guardianSpawn.above() : sector.arrival;
        if (at == null || !sl.isLoaded(at)) return;
        entity.setPos(at.getX() + 0.5, at.getY() + 0.4, at.getZ() + 0.5);
        entity.setDeltaMovement(Vec3.ZERO);
        Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.REVERSE_PORTAL, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 20, 0.3, 0.4, 0.3, 0.05);
    }
}
