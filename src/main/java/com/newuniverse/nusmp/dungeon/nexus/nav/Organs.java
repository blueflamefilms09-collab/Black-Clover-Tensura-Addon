package com.newuniverse.nusmp.dungeon.nexus.nav;

import com.newuniverse.nusmp.dungeon.CarcassBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/**
 * Anatomy-based harvesting, the part below the meat (docs/living_dungeon.md section 6): a Butcher Cleaver striking the upper third of a
 * Carcass cuts out its organs (a Venom Gland from a Basilisk or a spider, a Magic Stone from the carcass of a creature above tier 1: the
 * real Tensura item, looked up by id and skipped if Tensura does not have it), striking the lower third it cuts hide and bone (a little
 * bone meal or leather). The middle third, and any click on top, is the meat cut that dungeon.Harvest always made. Every cut takes one
 * portion of the carcass and wears the cleaver, like the meat cut does.
 */
public final class Organs {
    private Organs() {}

    private static final ResourceLocation MAGIC_STONE = ResourceLocation.fromNamespaceAndPath("tensura", "magic_stone");

    /** Where on the carcass the click landed: a carcass with nothing in its organs (or no hide) is cut for meat wherever that part is struck. */
    public static CutZone zoneOf(BlockHitResult hit, BlockPos carcass, int kind) {
        return OrganRules.settle(CutZone.of(hit.getLocation().y - carcass.getY(), hit.getDirection() == Direction.UP), kind);
    }

    /** One cut at the organs or the hide of the carcass at {@code pos}. Returns true if a portion of the carcass was taken (false for the meat zone: that cut is dungeon.Harvest's own). */
    public static boolean cut(ServerLevel level, BlockPos pos, Player p, ItemStack tool, int kind, CutZone zone) {
        if (zone == CutZone.MEAT) return false;
        if (zone == CutZone.ORGANS) organs(level, pos, p, kind);
        else hide(level, pos, p);
        level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1f, zone == CutZone.ORGANS ? 1.5f : 0.7f);
        CarcassBlock.nibble(level, pos);
        if (!p.isCreative()) tool.hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
        return true;
    }

    private static void organs(ServerLevel level, BlockPos pos, Player p, int kind) {
        RandomSource r = level.getRandom();
        OrganRules.Haul haul = OrganRules.organs(kind, r.nextDouble(), r.nextDouble());
        boolean gland = false, stone = false;
        if (haul.gland()) {
            drop(level, pos, new ItemStack(NavItems.VENOM_GLAND.get()));
            gland = true;
        }
        if (haul.stone()) {
            Optional<Item> magicStone = BuiltInRegistries.ITEM.getOptional(MAGIC_STONE);
            if (magicStone.isPresent()) {
                drop(level, pos, new ItemStack(magicStone.get()));
                stone = true;
            }
        }
        String said = gland && stone ? "A venom gland, and a magic stone lodged in the organs."
                : gland ? "A venom gland, still swollen."
                : stone ? "A magic stone, lodged in the organs."
                : "Nothing worth keeping among the organs.";
        p.displayClientMessage(Component.literal(said).withStyle(gland || stone ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
    }

    private static void hide(ServerLevel level, BlockPos pos, Player p) {
        OrganRules.Hide found = OrganRules.hide(level.getRandom().nextDouble());
        switch (found) {
            case BONE_MEAL_1 -> drop(level, pos, new ItemStack(Items.BONE_MEAL, 1));
            case BONE_MEAL_2 -> drop(level, pos, new ItemStack(Items.BONE_MEAL, 2));
            case LEATHER -> drop(level, pos, new ItemStack(Items.LEATHER, 1));
        }
        p.displayClientMessage(Component.literal(found == OrganRules.Hide.LEATHER ? "A scrap of hide." : "Bone, ground to meal.").withStyle(ChatFormatting.GRAY), true);
    }

    private static void drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity it = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        it.setDefaultPickUpDelay();
        level.addFreshEntity(it);
    }
}
