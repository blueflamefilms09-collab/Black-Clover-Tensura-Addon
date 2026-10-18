package com.newuniverse.nusmp.elite.items.consumable;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.items.EliteItemsConfig;
import com.newuniverse.nusmp.elite.items.EliteItemsData;
import com.newuniverse.nusmp.elite.items.EliteSounds;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the Potential Catalyst, designed here in the style of the SP Books ({@code SPBookItem}): right-click and it is used up, and the player is raised for good. It gives Skill Points through the
 * skill tree's one door (config {@code potentialCatalyst.skillPoints}; skipped while the skill tree is off) AND a permanent maximum-health step (config {@code healthHalfHearts}, default one heart),
 * the "potential" the owner's inventory names. The total number one player can use is limited ({@code maxUses}, default 10) and counted in the player's attachment, so the bonus is an exact function
 * of the count and is rebuilt after a death or a login ({@link #applyHealth}); the modifier id is fixed, so applying it twice never stacks. Mod VFX only: a magic circle, Light Rukh.
 */
public class PotentialCatalystItem extends Item {
    public static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "potential_catalyst_health");

    public PotentialCatalystItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p) || !EliteItemsConfig.enabled()) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        EliteItemsData d = EliteItemsData.of(p);
        int max = EliteItemsConfig.get(EliteItemsConfig.CATALYST_MAX_USES);
        if (!CatalystRules.canUse(d.catalystUses(), max)) {
            p.displayClientMessage(Component.translatableWithFallback("message.nusmp.catalyst.spent", "You can hold no more potential: %s of %s catalysts used.", d.catalystUses(), max).withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        d.setCatalystUses(d.catalystUses() + 1);
        int sp = EliteItemsConfig.get(EliteItemsConfig.CATALYST_SP);
        try {
            if (sp > 0 && SkillTreeApi.isEnabled()) SkillTreeApi.addSp(p, sp, "potential catalyst");
        } catch (RuntimeException | LinkageError ex) {
            // the skill tree is optional here: the health step still counts
        }
        applyHealth(p);
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, c.add(0, 0.1, 0), c.add(0, 1.1, 0), 0xFFFFE9A0, 40, 1.4f);
        RukhFx.burst(sl, c.add(0, 1.0, 0), RukhFx.Alignment.LIGHT, 10, 0.8);
        sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.3f);
        sl.playSound(null, p.blockPosition(), EliteSounds.CRATE_REVEAL.get(), SoundSource.PLAYERS, 0.7f, 1.2f);
        p.displayClientMessage(Component.translatableWithFallback("message.nusmp.catalyst.used", "Your potential opens: %s of %s catalysts used.", d.catalystUses(), max).withStyle(ChatFormatting.GOLD), true);
        if (!p.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    /** Rebuilds the permanent health bonus of the player from their catalyst count (login, respawn, after a use). Safe to call any number of times. */
    public static void applyHealth(ServerPlayer p) {
        AttributeInstance inst = p.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        inst.removeModifier(HEALTH_ID);
        double bonus = CatalystRules.healthBonus(EliteItemsData.of(p).catalystUses(), EliteItemsConfig.get(EliteItemsConfig.CATALYST_HEALTH_HALF_HEARTS));
        if (bonus > 0 && EliteItemsConfig.enabled()) inst.addPermanentModifier(new AttributeModifier(HEALTH_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("tooltip.nusmp.catalyst.line1", "Raises you for good: %s Skill Points and +%s maximum health.",
                EliteItemsConfig.get(EliteItemsConfig.CATALYST_SP), EliteItemsConfig.get(EliteItemsConfig.CATALYST_HEALTH_HALF_HEARTS) / 2.0 + " hearts").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.catalyst.line2", "At most %s can be used by one person.", EliteItemsConfig.get(EliteItemsConfig.CATALYST_MAX_USES)).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.catalyst.use", "Right-click to use it. It is used up.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
