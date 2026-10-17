package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.cast.AvatarCastPlanner;
import com.newuniverse.nusmp.avatar.cast.SimulatedPlayerCaster;
import com.newuniverse.nusmp.avatar.cast.VirtualSkillLoadout;
import com.newuniverse.nusmp.avatar.chatter.Rng;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Why: this is what makes an Avatar a mage and not only a swordsman. While it has a target in sight it presses the best ready entry of its virtual skill
 * loadout ({@link VirtualSkillLoadout}: the Ultimate and Unique skill it was made with, its perk, the skills and grimoires of its Magic Aptitudes, then the creator's
 * grimoire and plain Tensura magic), pays from the Avatar's own magicule pool, applies the INT / WIS scaled cooldowns, and shouts the spell's name (a Chunibyo shouts a
 * grander one). With {@code avatar.loadout} switched off it does what it did before the rebuild: one grimoire attack page of the creator's book by distance and
 * personality, or one allowlisted Tensura magic. It takes no movement flag, so the Avatar keeps walking, shooting and fighting with its own goals between casts.
 */
public final class AvatarCastSkillGoal extends Goal {
    private final AvatarEntity avatar;
    private final AvatarMind mind;
    private long nextTry;

    public AvatarCastSkillGoal(AvatarEntity avatar, AvatarMind mind) {
        this.avatar = avatar;
        this.mind = mind == null ? new AvatarMind() : mind;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    /** The 1.51.0 constructor, kept. */
    public AvatarCastSkillGoal(AvatarEntity avatar) { this(avatar, null); }

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
            if (AvatarAiConfig.loadoutEnabled()) pressLoadout(target, now);
            else pressClassic(target, now);
        } catch (RuntimeException soft) {
            nextTry = now + 100;                                                    // a failing spell backs the Avatar off, it never crashes the tick
        }
    }

    /** The rebuilt way: the best ready entry of the virtual skill loadout. */
    private void pressLoadout(LivingEntity target, long now) {
        boolean opening = now - mind.lastEngagedAt > 200;                            // nothing was pressed for ten seconds: a new fight, a buff goes first
        mind.lastEngagedAt = now;
        VirtualSkillLoadout.Pressed pressed = VirtualSkillLoadout.press(avatar, target, opening, rng());
        if (pressed == null) {
            nextTry = now + 40;
            return;
        }
        done(pressed.shout());
    }

    /** The 1.51.0 way, kept for {@code avatar.loadout = false}: one attack page of the creator's grimoire, or one allowlisted Tensura magic. */
    private void pressClassic(LivingEntity target, long now) {
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
    }

    private void done(String spellName) {
        avatar.swing(InteractionHand.MAIN_HAND);
        if (spellName != null) AvatarChatterEngine.say(avatar, ChatEvent.SKILL_CAST, spellName);
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

    static String pretty(String path) { return VirtualSkillLoadout.pretty(path); }
}
