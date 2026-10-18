package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.blackclover.TimedModifiers;
import com.newuniverse.nusmp.elite.skills.rules.IntrinsicRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Mechanical Hands (Intrinsic, designed for this mod; the original addon's source was not available): hands that do more than hands can. Mode 1, Extend Arms: for a minute (two once mastered) your
 * reach is longer (+3 blocks to blocks, +1.5 to entities, a little faster swings). Mode 2, Telekinesis: every dropped item and experience orb within 12 blocks (18 mastered) is pulled to you.
 * Mode 3, Mend: restores 15% (25% mastered) of the durability of the tool in your hand. Numbers: {@link IntrinsicRules.MechanicalHands}.
 */
public class MechanicalHandsSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("mechanical_hands", "Mechanical Hands",
            "Intrinsic. Extend Arms for a longer reach, Telekinesis to pull dropped items and orbs to you, Mend to restore part of the durability of the tool in your hand.");

    private static final ResourceLocation BLOCK_REACH = ResourceLocation.fromNamespaceAndPath("nusmp", "mechanical_hands_block"),
            ENTITY_REACH = ResourceLocation.fromNamespaceAndPath("nusmp", "mechanical_hands_entity"), SWING = ResourceLocation.fromNamespaceAndPath("nusmp", "mechanical_hands_swing");

    public MechanicalHandsSkill() { super(SkillType.INTRINSIC, TEXT, false); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("mechanical_hands.extend", "Extend Arms", IntrinsicRules.MechanicalHands.EXTEND_COST, IntrinsicRules.MechanicalHands.EXTEND_COOLDOWN),
                EliteMode.of("mechanical_hands.pull", "Telekinesis", IntrinsicRules.MechanicalHands.TELEKINESIS_COST, IntrinsicRules.MechanicalHands.TELEKINESIS_COOLDOWN),
                EliteMode.of("mechanical_hands.mend", "Mend", IntrinsicRules.MechanicalHands.MEND_COST, IntrinsicRules.MechanicalHands.MEND_COOLDOWN));
    }

    @Override protected int color() { return 0xFFC0C8D8; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        boolean mastered = masteredBy(instance, player);
        ServerLevel level = player.serverLevel();
        switch (mode) {
            case 0 -> {
                int ticks = IntrinsicRules.MechanicalHands.buffTicks(mastered);
                TimedModifiers.apply(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH, IntrinsicRules.MechanicalHands.blockReach(mastered), ticks, AttributeModifier.Operation.ADD_VALUE);
                TimedModifiers.apply(player, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH, IntrinsicRules.MechanicalHands.entityReach(mastered), ticks, AttributeModifier.Operation.ADD_VALUE);
                TimedModifiers.apply(player, Attributes.ATTACK_SPEED, SWING, 0.15, ticks, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                EliteBridge.sound(level, player.position(), SoundEvents.PISTON_EXTEND, 0.8f, 1.2f);
                EliteBridge.aura(level, player, false);
                EliteBridge.actionbar(player, SkillMsg.t("mechanical_hands.extended", "Your arms extend.").withStyle(ChatFormatting.GRAY));
                return true;
            }
            case 1 -> {
                double r = IntrinsicRules.MechanicalHands.pullRadius(mastered);
                Vec3 to = player.position().add(0, 0.6, 0);
                List<Entity> loot = new ArrayList<>(level.getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(r), e -> (e instanceof ItemEntity || e instanceof ExperienceOrb) && e.isAlive()));
                if (loot.isEmpty()) {
                    EliteBridge.fail(player, SkillMsg.t("mechanical_hands.nothing", "Nothing lies around to pull in."));
                    return false;
                }
                loot.sort(Comparator.comparingDouble(e -> e.distanceToSqr(to)));
                int n = 0;
                for (Entity e : loot) {
                    if (n++ >= IntrinsicRules.MechanicalHands.MAX_PULLED) break;
                    Vec3 d = to.subtract(e.position());
                    double len = d.length();
                    if (len < 0.5) continue;
                    e.setDeltaMovement(d.scale(1.0 / len).scale(IntrinsicRules.MechanicalHands.pullSpeed(len)));
                    e.hurtMarked = true;
                }
                EliteBridge.shape(level, com.newuniverse.nusmp.vfx.VfxShape.GRAVITY_AURA, player.position(), to, 0xFFC0C8D8, 14, 0.7f);
                EliteBridge.sound(level, player.position(), SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.9f);
                return true;
            }
            default -> {
                ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
                int pts = stack.isDamageableItem() ? IntrinsicRules.MechanicalHands.mendPoints(stack.getMaxDamage(), stack.getDamageValue(), mastered) : 0;
                if (pts <= 0) {
                    EliteBridge.fail(player, SkillMsg.t("mechanical_hands.no_wear", "The item in your hand has nothing to mend."));
                    return false;
                }
                stack.setDamageValue(stack.getDamageValue() - pts);
                EliteBridge.sound(level, player.position(), SoundEvents.ANVIL_USE, 0.6f, 1.6f);
                EliteBridge.spray(level, false, player.position().add(0, 1.2, 0), 8, 0.3, 0.3, 0.3, 0.03);
                EliteBridge.actionbar(player, SkillMsg.t("mechanical_hands.mended", "Your hands mend %s durability.", pts).withStyle(ChatFormatting.GRAY));
                return true;
            }
        }
    }
}
