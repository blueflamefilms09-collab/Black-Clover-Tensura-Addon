package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.book.KotodamaWords;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 0.47 Kotodama "Trident": an otherworldly trident spoken into being, the way Material Creation makes a weapon at its highest
 * tier. A real physical weapon: very heavy melee blows that also cut the spirit, and right-click hurls a void bolt along your
 * sight line (it pierces). Bound to its speaker: it is gone after 60 s, if dropped, or in anyone else's hands.
 */
public class OtherworldTridentItem extends SwordItem {
    static final String OWNER = "nusmp_koto_owner", UNTIL = "nusmp_koto_until";
    public static final int LIFE_TICKS = 1200;

    public OtherworldTridentItem() {
        super(Tiers.NETHERITE, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 14, -2.6f)));
    }

    /** A trident bound to this player for 60 s. */
    public static ItemStack bound(ServerPlayer p) {
        ItemStack s = new ItemStack(NUItems.OTHERWORLD_TRIDENT.get());
        long until = p.level().getGameTime() + LIFE_TICKS;
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> { t.putUUID(OWNER, p.getUUID()); t.putLong(UNTIL, until); });
        return s;
    }

    static CompoundTag data(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 10 != 0) return;
        CompoundTag t = data(stack);
        if (!t.hasUUID(OWNER)) return;                                       // an unbound one (commands) never fades
        if (!holder.getUUID().equals(t.getUUID(OWNER)) || level.getGameTime() >= t.getLong(UNTIL)) {
            if (level instanceof ServerLevel sl) VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, holder.position().add(0, 1, 0), holder.position(), KotodamaWords.VIOLET, 20, 0.5f);
            stack.setCount(0);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp) {
            Vec3 eye = sp.getEyePosition(), dir = sp.getViewVector(1f);
            VfxSpawn.send(sp.serverLevel(), VfxShape.KOTO_TRIDENT, eye.add(dir), eye.add(dir.scale(30)), KotodamaWords.VIOLET, 14, 1f);
            SpellRuntime.bolt(sp, eye.add(dir), dir.scale(2.5), 0.8, 14, true, null, (b, t) -> {
                KotodamaWords.hurt(sp, t, 14f);
                KotodamaWords.spirit(sp, t, 2);
            }, (b, at) -> VfxSpawn.send(sp.serverLevel(), VfxShape.KOTO_SHATTER, at, at, KotodamaWords.VIOLET, 20, 0.8f));
            sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.2f, 0.6f);
            player.getCooldowns().addCooldown(this, 30);
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) KotodamaWords.spirit(attacker, target, 1);
        return super.hurtEnemy(stack, target, attacker);
    }

    /** Dropped, it comes apart. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!data(stack).hasUUID(OWNER)) return false;
        entity.discard();
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.nusmp.otherworld_trident.desc").withStyle(ChatFormatting.DARK_PURPLE));
    }

    public static UUID ownerOf(ItemStack s) { CompoundTag t = data(s); return t.hasUUID(OWNER) ? t.getUUID(OWNER) : null; }
}
