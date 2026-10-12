package com.newuniverse.nusmp.wizard;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Tower Wizard: the keeper of a Grimoire Tower. Speak to him with your summons in your pack and he sets the questionnaire that
 * decides your magic, your kingdom and the blood mixed into yours; then your grimoire floats to you. A pointed hat, a robe and a
 * name: he is a witch in the game's own model (stands still, cannot be hurt, never despawns).
 */
public class TowerWizardEntity extends Witch {
    public TowerWizardEntity(EntityType<? extends Witch> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setNoAi(true);
        setCustomName(Component.literal("Tower Wizard"));
        setCustomNameVisible(true);
    }

    @Override public void checkDespawn() { }

    @Override public boolean isInvulnerableTo(DamageSource source) { return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY); }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && hand == InteractionHand.MAIN_HAND) {
            lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, sp.getEyePosition());
            WizardSystem.talk(sp, this);
        }
        return InteractionResult.CONSUME;
    }
}
