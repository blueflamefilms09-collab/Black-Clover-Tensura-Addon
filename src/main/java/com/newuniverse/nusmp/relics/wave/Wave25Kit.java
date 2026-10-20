package com.newuniverse.nusmp.relics.wave;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the small glue the wave 25 relics share (transient attribute modifiers under this wave's own ids, the hotbar line, the day number, the item
 * id of a stack, the Rukh alignment of a theme). Statics only; server side.
 */
final class Wave25Kit {
    private Wave25Kit() {}

    static Optional<Holder<Attribute>> attribute(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h);
    }

    /** Switches a transient modifier of this wave on (with this amount) or off. Transient: never saved, the once-a-second step puts it back. */
    static void setMod(LivingEntity e, String attributeId, String name, double amount, AttributeModifier.Operation op, boolean on) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("nusmp", "w25_" + name);
        attribute(attributeId).ifPresent(h -> {
            AttributeInstance inst = e.getAttribute(h);
            if (inst == null) return;
            AttributeModifier have = inst.getModifier(id);
            if (!on) {
                if (have != null) inst.removeModifier(id);
                return;
            }
            if (have != null && have.amount() == amount) return;
            if (have != null) inst.removeModifier(id);
            inst.addTransientModifier(new AttributeModifier(id, amount, op));
        });
    }

    /** A short line over the hotbar (for things that happen by themselves). */
    static void hint(ServerPlayer p, String text) { p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GOLD), true); }

    static long today(ServerLevel any) { return any.getServer().overworld().getDayTime() / Wave25Tune.DAY; }

    static String itemId(ItemStack s) { return BuiltInRegistries.ITEM.getKey(s.getItem()).toString(); }

    static String typeId(LivingEntity e) { return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(); }

    static boolean live(Player p) { return p.isAlive() && !p.isSpectator(); }

    static FxTone light() { return FxTone.LIGHT; }

    static FxTone dark() { return FxTone.DARK; }
}
