package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.elite.hub.CoinMath;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Why: the thirty meals, tonics and elixirs are real food, with the vanilla {@code FOOD} component, so the game itself eats them (the animation, the sound, the
 * hunger and saturation, the effects with their chance, the bowl or bottle handed back); the item class only adds what vanilla cannot: an elixir restores a share of
 * the drinker's magicules or aura through Tensura's storage and puts itself on a short cooldown, and the tooltip lists the buffs and what the item sells for.
 * THE HUNGER BAN: a meal only ever raises the food bar (its nutrition is at least one and never negative), no effect of the roster is a hunger effect (the factory skips
 * {@code minecraft:hunger} even if one were written, and a unit test fails if one is), and nothing here touches the exhaustion of a player. Every food is always edible
 * (a buff you cannot drink at full hunger is a buff nobody uses), and drinks use the drinking animation.
 */
public class LootFoodItem extends Item {
    private final LootRoster.FoodDef def;

    public LootFoodItem(Properties props, LootRoster.FoodDef def) {
        super(props);
        this.def = def;
    }

    public LootRoster.FoodDef def() { return def; }

    /** The item properties of a food of the roster. */
    public static Properties props(LootRoster.FoodDef def) {
        List<FoodProperties.PossibleEffect> fx = new ArrayList<>();
        for (LootRoster.EffectDef e : def.effects()) {
            Holder<MobEffect> h = effect(e.effect());
            if (h != null) fx.add(new FoodProperties.PossibleEffect(new MobEffectInstance(h, Math.max(1, e.seconds()) * 20, Math.max(0, e.amp())), 1.0F));
        }
        int nutrition = Math.max(1, def.nutrition());
        Optional<ItemStack> back = Optional.ofNullable(returns(def.id()));
        FoodProperties food = new FoodProperties(nutrition, nutrition * Math.max(0.0F, def.saturation()) * 2.0F, true, (float) Math.max(0.5, def.eat()), back, fx);
        Rarity rarity = def.value() >= 150 ? Rarity.RARE : def.value() >= 40 ? Rarity.UNCOMMON : Rarity.COMMON;
        return new Properties().food(food).stacksTo(def.elixir() || def.nutrition() <= 1 ? 16 : 64).rarity(rarity);
    }

    /** What eating hands back: the bowl of a stew, the bottle of a tonic. */
    private static ItemStack returns(String id) {
        return switch (id) {
            case "dungeon_ration_stew", "mandrake_stew", "bone_broth", "cave_mushroom_soup", "ember_chili", "orc_haunch_roast" -> new ItemStack(Items.BOWL);
            case "mana_elixir", "greater_mana_elixir", "aura_tonic", "wisp_draught", "starlit_nectar", "sages_elixir", "healers_vial", "fortitude_flask", "swiftfoot_tonic",
                 "berserkers_draught", "bulwark_tonic", "spirit_tea" -> new ItemStack(Items.GLASS_BOTTLE);
            default -> null;
        };
    }

    /** The effect holder of an id of the roster; never the hunger effect; null if the effect does not exist. */
    static Holder<MobEffect> effect(String id) {
        if (id.equals("minecraft:hunger")) return null;
        if (id.equals("nusmp:mana_surge")) return LootEffects.MANA_SURGE;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
    }

    private boolean drink() { return def.elixir() || def.nutrition() <= 1; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return drink() ? UseAnim.DRINK : UseAnim.EAT; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack out = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer sp && def.elixir()) {
            ManaSips.restore(sp, def.mana(), def.aura());
            sp.getCooldowns().addCooldown(this, 60 + (def.mana() + def.aura()) * 4);
        }
        return out;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("item.nusmp." + def.id() + ".lore", def.lore()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (def.mana() > 0) tip.add(LootText.text("tooltip.nusmp.loot.restores_mana", def.mana()).withStyle(ChatFormatting.AQUA));
        if (def.aura() > 0) tip.add(LootText.text("tooltip.nusmp.loot.restores_aura", def.aura()).withStyle(ChatFormatting.GOLD));
        for (LootRoster.EffectDef e : def.effects()) {
            Holder<MobEffect> h = effect(e.effect());
            if (h == null) continue;
            MobEffectInstance inst = new MobEffectInstance(h, Math.max(1, e.seconds()) * 20, Math.max(0, e.amp()));
            Component name = Component.translatable(inst.getDescriptionId());
            if (e.amp() > 0) name = Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + e.amp()));
            tip.add(LootText.text("tooltip.nusmp.loot.effect", name, MobEffectUtil.formatDuration(inst, 1.0F, 20.0F)).withStyle(ChatFormatting.BLUE));
        }
        tip.add(LootText.text("tooltip.nusmp.loot.value", CoinMath.describe(def.value())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
