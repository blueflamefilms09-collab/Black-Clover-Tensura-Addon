package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.item.NUItems;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

/**
 * 1.19.0: the food of the Living Dungeon (docs/living_dungeon.md section 4, "Delicious in Dungeon"). Meat cut from a carcass is <b>raw
 * monster meat</b>, and raw monster meat is toxic: magicule sickness, unless the eater has Predator or Gluttony. Cooked in a furnace, a
 * smoker or a campfire it becomes a meal that feeds well and bends a wizard's magic for a few minutes ({@link MealBuffs}). Meat that was
 * hacked out with something blunt is ruined and good for nothing. Holy water or honey purifies raw meat into plain safe food.
 */
public final class DungeonFood {
    private DungeonFood() {}

    // ---------------------------------------------------------------- raw cuts (kind 0..4 as in CarcassBlock.KIND)
    public static final DeferredItem<Item> RAW_SLIME_JELLY = raw("raw_slime_jelly", 2, 0.1f, 400);
    public static final DeferredItem<Item> RAW_ARMOR_FLANK = raw("raw_armor_flank", 4, 0.3f, 600);
    public static final DeferredItem<Item> RAW_BASILISK_FILLET = raw("raw_basilisk_fillet", 5, 0.4f, 900);
    public static final DeferredItem<Item> RAW_DIREWOLF_HAUNCH = raw("raw_direwolf_haunch", 5, 0.4f, 900);
    public static final DeferredItem<Item> RAW_MONSTER_MEAT = raw("raw_monster_meat", 3, 0.2f, 600);

    /** Hacked out with something blunt: ruined. */
    public static final DeferredItem<Item> RUINED_MEAT = NUItems.ITEMS.register("ruined_meat", () -> new Item(new Item.Properties().food(
            new FoodProperties.Builder().nutrition(1).saturationModifier(0f).effect(() -> new MobEffectInstance(MobEffects.HUNGER, 400, 1), 1f)
                    .effect(() -> new MobEffectInstance(DungeonEffects.MAGICULE_SICKNESS, 900, 1), 1f).build())));

    /** Raw meat made safe by holy water or honey: plain food, no buff. */
    public static final DeferredItem<Item> PURIFIED_MEAT = NUItems.ITEMS.register("purified_monster_meat", () -> new Item(new Item.Properties().food(
            new FoodProperties.Builder().nutrition(5).saturationModifier(0.4f).build())));

    // ---------------------------------------------------------------- cooked meals
    public static final DeferredItem<Item> CANDIED_SLIME_JELLY = meal("candied_slime_jelly", 5, 0.5f, 1.00, 0.85, 6000,
            "Clear-headed: spells cost 15% less mana for 5 minutes", () -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
    public static final DeferredItem<Item> GRILLED_ARMOR_FLANK = meal("grilled_armor_flank", 8, 0.7f, 1.05, 1.00, 4800,
            "Shell-hardened: spells hit 5% harder for 4 minutes, and the skin toughens", () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2400, 0));
    public static final DeferredItem<Item> ROAST_BASILISK_FILLET = meal("roast_basilisk_fillet", 10, 0.8f, 1.20, 1.00, 3600,
            "Venom in the blood: spells hit 20% harder for 3 minutes, and poison cannot take hold", () -> new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0));
    public static final DeferredItem<Item> ROAST_DIREWOLF_HAUNCH = meal("roast_direwolf_haunch", 10, 0.8f, 1.10, 0.95, 3600,
            "Pack hunger: faster and stronger, spells hit 10% harder for 3 minutes", () -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 0));
    public static final DeferredItem<Item> SEARED_MONSTER_MEAT = meal("seared_monster_meat", 7, 0.6f, 1.00, 1.00, 0,
            "Plain, filling, and not poisonous", () -> new MobEffectInstance(MobEffects.SATURATION, 20, 0));

    private static DeferredItem<Item> raw(String id, int nutrition, float saturation, int sickTicks) {
        return NUItems.ITEMS.register(id, () -> new RawMeat(new Item.Properties().food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation)
                .effect(() -> new MobEffectInstance(DungeonEffects.MAGICULE_SICKNESS, sickTicks, 0), 1f).build()).rarity(Rarity.UNCOMMON)));
    }

    private static DeferredItem<Item> meal(String id, int nutrition, float saturation, double power, double cost, int buffTicks, String text,
                                           java.util.function.Supplier<MobEffectInstance> effect) {
        return NUItems.ITEMS.register(id, () -> new Meal(new Item.Properties().food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation)
                .effect(effect, 1f).build()).rarity(Rarity.RARE), power, cost, buffTicks, text));
    }

    public static void init() {}

    /** The creatures' raw cut for a carcass kind. */
    public static Item rawFor(int kind) {
        return switch (kind) {
            case 0 -> RAW_SLIME_JELLY.get();
            case 1 -> RAW_ARMOR_FLANK.get();
            case 2 -> RAW_BASILISK_FILLET.get();
            case 3 -> RAW_DIREWOLF_HAUNCH.get();
            default -> RAW_MONSTER_MEAT.get();
        };
    }

    /** Predator and Gluttony eat anything. */
    static boolean cannotBePoisoned(Player p) {
        try {
            var skills = SkillAPI.getSkillsFrom(p);
            for (String id : new String[]{"predator", "gluttony"})
                if (skills.getSkill(ResourceLocation.fromNamespaceAndPath("tensura", id)).isPresent()) return true;
        } catch (Throwable noSkills) { /* Tensura's skill list is not there: nobody is exempt */ }
        return false;
    }

    /** Raw monster meat: the sickness it carries is wiped for a Predator or a Gluttony. */
    public static final class RawMeat extends Item {
        RawMeat(Item.Properties props) { super(props); }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
            ItemStack out = super.finishUsingItem(stack, level, eater);
            if (!level.isClientSide && eater instanceof Player p && cannotBePoisoned(p)) {
                p.removeEffect(DungeonEffects.MAGICULE_SICKNESS);
                p.displayClientMessage(Component.literal("Your hunger takes the magicules in stride.").withStyle(ChatFormatting.GRAY), true);
            }
            return out;
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.literal("Raw monster meat: toxic (magicule sickness). Cook it, or purify it with holy water or honey.").withStyle(ChatFormatting.DARK_RED));
        }
    }

    /** A cooked meal: feeds, and bends the eater's magic for a while. */
    public static final class Meal extends Item {
        private final double power, cost;
        private final int buffTicks;
        private final String text;

        Meal(Item.Properties props, double power, double cost, int buffTicks, String text) {
            super(props);
            this.power = power;
            this.cost = cost;
            this.buffTicks = buffTicks;
            this.text = text;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
            ItemStack out = super.finishUsingItem(stack, level, eater);
            if (!level.isClientSide && eater instanceof Player p && buffTicks > 0) {
                MealBuffs.give(p, power, cost, buffTicks);
                p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GOLD), true);
            }
            return out;
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.literal(text).withStyle(ChatFormatting.GOLD));
        }
    }
}
