package com.newuniverse.nusmp.relics;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What {@link RelicBowItem} and {@link RelicCrossbowItem} share. A drawn weapon has one use button, and drawing is what it means; the relic's acts
 * therefore go on <b>sneak + use</b> (and on the worn-relic key, which needs nothing from the item). A relic that needs attunement and is not yours
 * cannot be drawn at all: sneak + use holds the five-second ritual, plain use says so.
 * <p>
 * "Act mode" tells the item's {@code onUseTick} / {@code releaseUsing} that the hold in progress is an act (or the ritual) and not a draw, so a long
 * hold does not loose an arrow when the button is let go. It is set in {@code use()} on both sides (the client needs it for the crossbow's use
 * duration) and always overwritten by the next {@code use()}, so a stale entry (a hold ended by {@code stopUsingItem}) is never read.
 */
public final class RelicRangedSupport {
    private RelicRangedSupport() {}

    private static final Map<UUID, Boolean> ACT_MODE = new ConcurrentHashMap<>();

    public static boolean actMode(LivingEntity e) { return e != null && Boolean.TRUE.equals(ACT_MODE.get(e.getUUID())); }

    static void setActMode(LivingEntity e, boolean on) {
        if (on) {
            if (ACT_MODE.size() > 256) ACT_MODE.clear();
            ACT_MODE.put(e.getUUID(), Boolean.TRUE);
        } else {
            ACT_MODE.remove(e.getUUID());
        }
    }

    /**
     * The act half of {@code use()}. Returns the result to hand back when the click was an act, the ritual or a refusal, or null when the player is
     * simply drawing the weapon (the caller then runs the vanilla {@code use}).
     */
    static InteractionResultHolder<ItemStack> intercept(Level level, Player player, InteractionHand hand, RelicDef def) {
        ItemStack stack = player.getItemInHand(hand);
        boolean unattuned = def.needsAttunement() && !Attune.isMine(stack, player);
        boolean sneak = player.isShiftKeyDown();
        setActMode(player, false);
        if (!unattuned && !(sneak && !def.acts().isEmpty())) return null;
        if (unattuned && !sneak) {
            if (!level.isClientSide && player instanceof ServerPlayer p) RelicEngine.useHeld(p, stack, def, hand);      // tells the player to attune
            return InteractionResultHolder.fail(stack);
        }
        setActMode(player, true);                                                                                       // before startUsingItem reads the duration
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        RelicEngine.Outcome o = RelicEngine.useHeld(p, stack, def, hand);
        if (o != RelicEngine.Outcome.HOLD) setActMode(player, false);
        return switch (o) {
            case DONE, HOLD -> InteractionResultHolder.consume(stack);
            case REFUSED -> unattuned ? InteractionResultHolder.fail(stack) : null;          // an act that cannot run now: the crouching archer still shoots
            default -> null;
        };
    }

    /** The hold in progress is an act or the ritual: hand the tick or the release to the relic engine. */
    static boolean handleTick(Level level, LivingEntity user, ItemStack stack, int remaining, RelicDef def) {
        if (!actMode(user)) return false;
        RelicItemSupport.onUseTick(level, user, stack, remaining, def);
        return true;
    }

    static boolean handleRelease(ItemStack stack, Level level, LivingEntity user, int remaining, RelicDef def) {
        if (!actMode(user)) return false;
        setActMode(user, false);
        RelicItemSupport.releaseUsing(stack, level, user, remaining, def);
        return true;
    }

    /** The weapon's melee stats in vanilla's own ids, so the tooltip reads like a sword's (a bow is still a poor club). */
    static ItemAttributeModifiers attributes(RelicDef def) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_damage"),
                        def.meleeDamage() - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_speed"),
                        def.meleeSpeedModifier(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** One tooltip line saying how the acts are used. */
    static void hint(List<Component> tip, RelicDef def, String key) {
        if (def.acts().isEmpty()) return;
        tip.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
    }
}
