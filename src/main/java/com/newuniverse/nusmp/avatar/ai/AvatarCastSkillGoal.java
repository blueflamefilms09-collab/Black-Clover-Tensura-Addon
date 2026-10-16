package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.cast.AvatarCastPlanner;
import com.newuniverse.nusmp.avatar.cast.SimulatedPlayerCaster;
import com.newuniverse.nusmp.avatar.chatter.Rng;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: this is what makes an Avatar a mage and not only a swordsman. While it has a target in sight it picks one of the grimoire attack pages it
 * may cast (see {@code AvatarSkillList}) by distance and personality ({@code AvatarCastPlanner}), casts it through its body double
 * ({@code SimulatedPlayerCaster}, which pays magicules from the Avatar's own pool and applies the INT/WIS-scaled cooldowns), shouts the spell's name
 * (a Chunibyo shouts a grander one), and, when it has no grimoire page ready, falls back to one allowlisted Tensura magic. It takes no movement
 * flag, so the Avatar keeps walking and fighting with its own goals between casts.
 */
public final class AvatarCastSkillGoal extends Goal {
    private final AvatarEntity avatar;
    private long nextTry;

    public AvatarCastSkillGoal(AvatarEntity avatar) {
        this.avatar = avatar;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (avatar.level().isClientSide || !AvatarAiConfig.castEnabled()) return false;
        LivingEntity t = avatar.getTarget();
        if (t == null || !t.isAlive() || avatar.level().getGameTime() < nextTry) return false;
        if (avatar.distanceToSqr(t) > 28 * 28 || !avatar.getSensing().hasLineOfSight(t)) return false;
        return SimulatedPlayerCaster.gapOver(avatar);
    }

    @Override public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        LivingEntity target = avatar.getTarget();
        long now = avatar.level().getGameTime();
        nextTry = now + 10 + avatar.getRandom().nextInt(10);                       // do not re-scan every tick when nothing is ready
        if (target == null) return;
        try {
            SimulatedPlayerCaster caster = SimulatedPlayerCaster.of(avatar);
            if (caster == null) return;
            List<AvatarCastPlanner.Candidate> ready = caster.candidates(avatar);
            boolean tensura = ready.isEmpty() || avatar.getRandom().nextInt(5) == 0;
            if (!tensura) {
                Set<AvatarPersonality> ps = personalities();
                int mode = AvatarCastPlanner.choose(ready, avatar.distanceTo(target), ps, rng());
                String name = mode < 0 ? null : caster.pageName(avatar, mode);
                if (mode >= 0 && caster.castPage(avatar, mode, target) == SimulatedPlayerCaster.Result.OK) {
                    done(name);
                    return;
                }
            }
            ResourceLocation id = SimulatedPlayerCaster.castTensura(avatar, target);
            if (id != null) done(pretty(id.getPath()));
            else nextTry = now + 40;
        } catch (RuntimeException soft) {
            nextTry = now + 100;                                                    // a failing spell backs the Avatar off, it never crashes the tick
        }
    }

    private void done(String spellName) {
        avatar.swing(InteractionHand.MAIN_HAND);
        AvatarChatterEngine.say(avatar, ChatEvent.SKILL_CAST, spellName);
    }

    private Set<AvatarPersonality> personalities() {
        Set<AvatarPersonality> s = java.util.EnumSet.noneOf(AvatarPersonality.class);
        if (avatar.primary() != null) s.add(avatar.primary());
        if (avatar.secondary() != null) s.add(avatar.secondary());
        return s;
    }

    private Rng rng() {
        var r = avatar.getRandom();
        return new Rng() {
            @Override public int nextInt(int bound) { return r.nextInt(bound); }
            @Override public double nextDouble() { return r.nextDouble(); }
        };
    }

    static String pretty(String path) {
        StringBuilder sb = new StringBuilder();
        for (String w : path.toLowerCase(Locale.ROOT).split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }
}
