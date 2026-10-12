package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.dungeon.nexus.nav.CutZone;
import com.newuniverse.nusmp.dungeon.nexus.nav.Organs;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Cutting meat out of a carcass (docs/living_dungeon.md section 4): right-click a Carcass. The tool decides what comes out. A
 * <b>Butcher Cleaver</b> cuts along the anatomy: prime raw cuts, and sometimes a second one. Any other blade (a sword, an axe) cuts
 * what it can: one cut. Anything blunt (a hammer, a pickaxe, a hand) tears the carcass up: <b>ruined meat</b>. Each cut takes one
 * portion of the carcass; the last one takes the carcass.
 */
public final class Harvest {
    private Harvest() {}

    public static final TagKey<Item> BLADES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("nusmp", "butchering_blades"));

    private static boolean cleaver(ItemStack s) { return s.getItem() instanceof MagicWeaponItem w && w.kind == MagicWeaponItem.Kind.BUTCHER_CLEAVER; }

    /** One cut, with no click position: always the meat cut. Returns false if nothing was cut (the block is not a carcass). */
    public static boolean cut(ServerLevel level, BlockPos pos, Player p, ItemStack tool) { return cut(level, pos, p, tool, null); }

    /**
     * One cut. Returns false if nothing was cut (the block is not a carcass). 1.21.0: with a Butcher Cleaver the height of the click
     * decides the cut (docs/living_dungeon.md section 6): the upper third of the carcass is its organs, the lower third hide and bone
     * ({@link Organs}); the middle, a click on top, any other tool and a carcass with no organs are the meat cut below, as ever.
     */
    public static boolean cut(ServerLevel level, BlockPos pos, Player p, ItemStack tool, BlockHitResult hit) {
        var state = level.getBlockState(pos);
        if (!state.is(DungeonBlocks.CARCASS.get())) return false;
        int kind = state.getValue(CarcassBlock.KIND);
        boolean prime = cleaver(tool), blade = prime || tool.is(BLADES) || tool.is(ItemTags.SWORDS) || tool.is(ItemTags.AXES);
        if (prime && hit != null) {
            CutZone zone = Organs.zoneOf(hit, pos, kind);
            if (zone != CutZone.MEAT) return Organs.cut(level, pos, p, tool, kind, zone);
        }
        ItemStack main = blade ? new ItemStack(DungeonFood.rawFor(kind)) : new ItemStack(DungeonFood.RUINED_MEAT.get());
        if (prime && level.getRandom().nextFloat() < 0.4f) main.setCount(2);
        drop(level, pos, main);
        if (!blade) p.displayClientMessage(Component.literal("Hacked out with something blunt, the meat is ruined. A blade would have done better.").withStyle(ChatFormatting.GRAY), true);
        else if (!prime) p.displayClientMessage(Component.literal("A proper Butcher Cleaver would find the prime cuts.").withStyle(ChatFormatting.GRAY), true);
        level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1f, prime ? 1.3f : 0.8f);
        CarcassBlock.nibble(level, pos);
        if (blade && !p.isCreative()) tool.hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
        return true;
    }

    private static void drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity it = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        it.setDefaultPickUpDelay();
        level.addFreshEntity(it);
    }
}
