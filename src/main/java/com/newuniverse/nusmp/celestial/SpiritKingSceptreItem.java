package com.newuniverse.nusmp.celestial;

import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the unique item of the Spirit King's reward. A sceptre crowned with a star: using it pours "Starlight Mend" over the holder and every player and pet within 8
 * blocks (a heal through the balance law's healing cap, a few seconds of Regeneration and Absorption) with a 30 second cooldown. It is engraved like every weapon (see
 * item/WeaponEngravings) and drawn as a 3D model in hand.
 */
public final class SpiritKingSceptreItem extends Item {
    public SpiritKingSceptreItem() { super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p) || !(level instanceof ServerLevel sl)) return InteractionResultHolder.pass(stack);
        if (p.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        p.getCooldowns().addCooldown(this, 600);
        float heal = (float) (6.0 * BalanceLaw.epScale(p));
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(p.position(), p.position()).inflate(8))) {
            boolean friend = e == p || (e instanceof Player o && !o.isSpectator() && o.isAlliedTo(p)) || (e instanceof net.minecraft.world.entity.TamableAnimal t && t.isOwnedBy(p));
            if (!friend || !e.isAlive()) continue;
            BalanceLaw.heal(e, heal);
            e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            Fx.spray(sl, FxThemes.SKILL_CELESTIAL, FxOld.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 14, 0.4, 0.7, 0.4, 0.04);
        }
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.3f);
        p.displayClientMessage(Component.translatableWithFallback("message.nusmp.celestial.sceptre", "Starlight Mend.").withStyle(ChatFormatting.AQUA), true);
        return InteractionResultHolder.success(stack);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("tooltip.nusmp.celestial.sceptre", "Starlight Mend: heals you and your allies within 8 blocks (30 s cooldown).").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.celestial.sceptre2", "Left to the one who bested the Celestial Spirit King.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
