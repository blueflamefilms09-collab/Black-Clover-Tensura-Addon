package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.relics.tier.Tiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/** What the four relic item classes share: the grade's look and the held-button logic (the attunement ritual, charging an act). */
final class RelicItemSupport {
    private RelicItemSupport() {}

    /** How long a relic can be held down (an hour: it never runs out on its own). */
    static final int USE_TICKS = 72000;

    /** The vanilla rarity closest to a grade (its colour is overridden by the grade colour in the name). */
    static Rarity rarity(Grade g) {
        return switch (g) {
            case NORMAL -> Rarity.COMMON;
            case SPECIAL -> Rarity.UNCOMMON;
            case RARE, UNIQUE -> Rarity.RARE;
            default -> Rarity.EPIC;
        };
    }

    /** The item's name in the grade's colour. */
    static Component name(Component base, RelicDef def) { return base.copy().withStyle(def.grade().colour()); }

    /** 1.29.0: the name in the colour of the stack's EFFECTIVE grade (a raised relic takes the colour of the grade it was raised to). */
    static Component name(Component base, RelicDef def, ItemStack stack) { return base.copy().withStyle(Tiers.effectiveGrade(stack, def).colour()); }

    /** 1.29.0: the glint from LEGEND up, by the effective grade. */
    static boolean foil(ItemStack stack, RelicDef def) { return Tiers.effectiveGrade(stack, def).atLeast(Grade.LEGEND); }

    /** While the use button is held on a relic: the attunement clock, or a charge message. Server only. */
    static void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining, RelicDef def) {
        if (level.isClientSide || !(user instanceof ServerPlayer p)) return;
        int held = USE_TICKS - remaining;
        if (def.look() == Look.SHIELD && !ShieldUseRules.ritualRuns(p.isShiftKeyDown())) return;      // 1.110.0: a raised shield is just a block unless the player sneaks (the ritual)
        if (def.needsAttunement() && !Attune.isMine(stack, p)) {
            RelicEngine.attuneTick(p, stack, def, held);
            return;
        }
        if (def.acts().isEmpty()) return;
        Act a = RelicEngine.pickAct(def, p.isShiftKeyDown());
        if (a.holdTicks() > 0 && held % 10 == 0 && held <= a.holdTicks()) {
            int pct = Math.min(100, held * 100 / a.holdTicks());
            p.displayClientMessage(Component.literal(a.displayName() + "  " + pct + "%").withStyle(ChatFormatting.AQUA), true);
        }
    }

    /** The use button is let go. */
    static void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining, RelicDef def) {
        if (level.isClientSide || !(user instanceof ServerPlayer p)) return;
        if (def.look() == Look.SHIELD && !p.isShiftKeyDown()) return;                // 1.110.0: lowering a shield is not a broken ritual, and fires no held act
        if (def.needsAttunement() && !Attune.isMine(stack, p)) {
            RelicEngine.tell(p, "Attunement broken: hold sneak and use for the full five seconds.", ChatFormatting.GRAY);
            return;
        }
        RelicEngine.released(p, stack, def, USE_TICKS - remaining);
    }
}
