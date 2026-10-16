package com.newuniverse.nusmp.avatar.goal;

import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.avatar.betrayal.BetrayalDimensionHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Why: this is what a betrayal is. The Avatar sets its creator as its target and, when they are in another dimension or far away, follows them there with
 * {@link BetrayalDimensionHandler#teleportNear}. It is kept fair: after every teleport the creator gets a grace period (10 seconds by default) in which it
 * does not attack and is told it has arrived, and every blow it lands is capped at a quarter of the victim's maximum health (see the damage guard in
 * AvatarSystem), so a betrayed Avatar can scare and hurt a player but not one-shot them.
 */
public class AvatarHuntCreatorGoal extends Goal {
    private static final double CLOSE = 48.0;
    private final AvatarEntity avatar;
    private long nextTeleport;

    public AvatarHuntCreatorGoal(AvatarEntity avatar) {
        this.avatar = avatar;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() { return avatar.isBetrayed() && avatar.creatorPlayer() != null; }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void stop() { avatar.setTarget(null); }

    @Override
    public void tick() {
        ServerPlayer creator = avatar.creatorPlayer();
        if (creator == null) return;
        long now = avatar.level().getGameTime();
        if (now < avatar.graceUntil()) {                       // the creator's breathing room: it holds back
            avatar.setTarget(null);
            avatar.getNavigation().stop();
            return;
        }
        boolean otherLevel = creator.level() != avatar.level();
        if (otherLevel || avatar.distanceToSqr(creator) > CLOSE * CLOSE) {
            if (now >= nextTeleport) {
                nextTeleport = now + 100;
                AvatarEntity moved = BetrayalDimensionHandler.teleportNear(avatar, creator);
                if (moved != null) {
                    moved.setGraceUntil(now + AvatarConfig.betrayalGraceTicks());
                    moved.setTarget(null);
                    creator.sendSystemMessage(Component.literal(moved.avatarName() + " has found you. You have " + AvatarConfig.betrayalGraceTicks() / 20 + " seconds.")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
                    AvatarChatterEngine.say(moved, ChatEvent.BETRAYAL);
                }
            }
            return;
        }
        if (avatar.getTarget() != creator && creator.isAlive() && !creator.isSpectator()) avatar.setTarget(creator);
    }
}
