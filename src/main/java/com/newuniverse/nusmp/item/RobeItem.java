package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.NUConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * A piece of a Magic Knight robe set, rendered as a real 3D GeckoLib model (hoods, hats, capes,
 * robe skirts) with a gently swaying cape. The chest piece carries the set bonus; wearing all
 * four pieces boosts it by 50%.
 */
public class RobeItem extends ArmorItem implements GeoItem {
    public enum Kind {
        JUNIOR("simple"), SENIOR("robe"), GOLDEN_DAWN("mantle"), BLACK_BULL("mantle"), SILVER_EAGLE("robe"),
        CRIMSON_LION("mantle"), CORAL_PEACOCK("robe"), HEART("coat"), DIAMOND("simple"), SPADE("robe"), DEVIL("devil");
        public final String style;
        Kind(String style) { this.style = style; }
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public final Kind kind;
    public final String setId;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RobeItem(Holder<ArmorMaterial> material, Kind kind, ArmorItem.Type type, String setId) {
        super(material, type, new Item.Properties().stacksTo(1).durability(type.getDurability(18)));
        this.kind = kind;
        this.setId = setId;
    }

    // ---------------------------------------------------------------- GeckoLib
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<RobeItem> renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T entity, ItemStack stack,
                    @Nullable EquipmentSlot slot, @Nullable HumanoidModel<T> original) {
                if (renderer == null) renderer = new GeoArmorRenderer<>(new RobeModel());
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 10, state -> state.setAndContinue(IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ---------------------------------------------------------------- tooltip
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        if (getType() != ArmorItem.Type.CHESTPLATE) {
            tip.add(Component.literal("Part of the " + Component.translatable("item.nusmp." + setId).getString() + " set").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("Full set: chest bonus +50%").withStyle(ChatFormatting.GOLD));
            return;
        }
        tip.add(Component.literal("Set bonus (robe worn):").withStyle(ChatFormatting.GOLD));
        int k = NUConfig.GEAR_ELEMENT_BONUS.get();
        switch (kind) {
            case JUNIOR -> line(tip, "+" + NUConfig.GEAR_REGEN_JUNIOR.get() + "% max magicule regenerated per second");
            case SENIOR -> { line(tip, "+" + NUConfig.GEAR_REGEN_SENIOR.get() + "% max magicule regenerated per second"); line(tip, "+" + NUConfig.GEAR_SENIOR_DAMAGE.get() + "% grimoire damage"); }
            case GOLDEN_DAWN -> { line(tip, "+" + k + "% Light and Star damage"); line(tip, "You glow for 5 s after a kill"); }
            case BLACK_BULL -> { line(tip, "+" + k + "% Dark and Shadow damage"); line(tip, "-" + NUConfig.GEAR_RECOIL_REDUCTION.get() + "% self-damage from pages"); }
            case SILVER_EAGLE -> { line(tip, "+" + k + "% Mercury and Steel damage"); line(tip, "-" + NUConfig.GEAR_EAGLE_REDUCTION.get() + "% damage taken"); }
            case CRIMSON_LION -> { line(tip, "+" + k + "% Flame, Explosion and Magma damage"); line(tip, "Fire Resistance during Spirit Dive"); }
            case CORAL_PEACOCK -> { line(tip, "+" + k + "% Water, Ice and Mist damage"); line(tip, "Dolphin's Grace"); }
            case HEART -> line(tip, "-" + NUConfig.GEAR_HEART_COST.get() + "% grimoire magicule cost (Mana Method)");
            case DIAMOND -> { line(tip, "-" + NUConfig.GEAR_DIAMOND_COOLDOWN.get() + "% grimoire cooldowns"); line(tip, "Haste I (tool speed)"); }
            case SPADE -> { line(tip, "+" + NUConfig.GEAR_SPADE_DAMAGE.get() + "% grimoire damage"); line(tip, "-" + NUConfig.GEAR_SPADE_REGEN_PENALTY.get() + "% max magicule per second").withStyle(ChatFormatting.RED); }
            case DEVIL -> {
                line(tip, "Only five-leaf or triple-spade mages can wear this").withStyle(ChatFormatting.DARK_RED);
                line(tip, "+" + NUConfig.GEAR_DEVIL_DAMAGE.get() + "% grimoire damage, Resistance I");
                line(tip, "Wither unless you paid a devil price in the last " + NUConfig.GEAR_DEVIL_GRACE.get() + " s").withStyle(ChatFormatting.RED);
            }
        }
        tip.add(Component.literal("Full set (hood, robe, leggings, boots): bonuses +50%").withStyle(ChatFormatting.AQUA));
    }

    private static net.minecraft.network.chat.MutableComponent line(List<Component> tip, String s) {
        var c = Component.literal("  " + s).withStyle(ChatFormatting.GRAY);
        tip.add(c);
        return c;
    }
}
