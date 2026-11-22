package com.newuniverse.nusmp.mixin;

import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroWeaponItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 4.100.0 (docs/heroweapon/enchanting_menus.md): vanilla's {@code ItemStack.isEnchantable} says no to any stack that already carries an enchantment, and every hero or vassal weapon carries its Tensura
 * engravings from the moment it is made, so the enchanting table never took one. For a hero weapon the answer is now the item's own ({@link HeroWeaponItem#isEnchantable}), when the server setting
 * {@code heroweapon.enchantMenus} is on; everything else is left to vanilla. Optional (require = 0): if the method ever changes the game still runs, only the table is off. Cost: one instanceof per call.
 */
@Mixin(ItemStack.class)
public abstract class HeroWeaponEnchantableMixin {
    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true, require = 0)
    private void anime_multiverse$heroWeaponEnchantable(CallbackInfoReturnable<Boolean> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getItem() instanceof HeroWeaponItem item && HeroConfig.enchantMenus()) cir.setReturnValue(item.isEnchantable(self));
    }
}
