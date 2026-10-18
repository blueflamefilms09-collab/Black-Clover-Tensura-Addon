package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Why: the Legendary Spear (Motoyasu Kitamura's weapon, whose canon sin is Lust) is the longest-reaching melee weapon of the four Holy Weapons and the one
 * with the unique property of Time Reversal. This is its logic: the nineteen techniques ({@link SpearSkills}), and the hooks the shared machinery calls for
 * every bonded wielder, each doing only what a form's passives ask for: Memory of the Hours writes the wielder's past once a second, Featherlight takes
 * the fall away, Victor's Vigor heals a little on a kill, Fervor makes blows grow in the Lust forms, and the counter stance turns a blow into a thrust.
 * Every hook is guarded (a failure is logged once and the hook gives up for that call; the game never sees an exception from here). The constructor, which
 * runs once at start-up, registers the self-test {@code hero.weapon.spear} and the cleaning of the wielders' memory at logout and server stop.
 */
public final class SpearLogic implements HeroLogic {
    private static boolean started;

    private final List<HeroSkill> skills = SpearSkills.build();

    public SpearLogic() { start(); }

    private static synchronized void start() {
        if (started) return;
        started = true;
        SpearTime.registerEvents();
        SelfTests.register("hero.weapon.spear", SpearSelfTest::run);
    }

    @Override
    public HeroKind kind() { return HeroKind.SPEAR; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onTick(HeroCtx c) {
        try {
            if (c.form.has(SpearIds.TIME_MEMORY)) SpearTime.remember(c);
            SpearLust.tick(c);
        } catch (RuntimeException e) {
            Tasks.warnOnce("spear onTick", e);
        }
    }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) {
        try {
            SpearLust.onHit(c);
        } catch (RuntimeException e) {
            Tasks.warnOnce("spear onHit", e);
        }
    }

    @Override
    public float onDamaged(HeroCtx c, DamageSource source, float amount) {
        try {
            return SpearGuard.onDamaged(c, source, amount);
        } catch (RuntimeException e) {
            Tasks.warnOnce("spear onDamaged", e);
            return amount;
        }
    }

    @Override
    public void onKill(HeroCtx c, LivingEntity victim) {
        try {
            if (c.summoned() && c.form.has(SpearIds.KILL_VIGOR)) c.heal(c.player, (float) (c.player.getMaxHealth() * SpearNumbers.KILL_VIGOR));
        } catch (RuntimeException e) {
            Tasks.warnOnce("spear onKill", e);
        }
    }

    @Override
    public void onFormChanged(HeroCtx c, HeroForm from, HeroForm to) {
        try {
            SpearLust.clear(c);
        } catch (RuntimeException e) {
            Tasks.warnOnce("spear onFormChanged", e);
        }
    }

    @Override
    public void onDismiss(HeroCtx c) {
        try {
            SpearLust.clear(c);
            SpearTime.forget(c.player.getUUID());
        } catch (RuntimeException e) {
            Tasks.warnOnce("spear onDismiss", e);
        }
    }
}
