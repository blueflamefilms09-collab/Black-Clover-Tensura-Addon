package com.newuniverse.nusmp.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the Grimoire Daemons (the pages, the turrets, the arch daemon and the rule stones) were written for ONE boss class, the legacy
 * {@link ZagredBossEntity}, and asked it directly for its phase, its foes and its damage. The remade Zagred ({@code arc/zagred/ZagredBoss}) keeps his
 * daemons (the art, the behaviour and the back-of-the-book weak spot are the best part of the old fight), but it is a different class, so the daemons
 * now ask this small interface instead. Both bosses implement it; nothing about the legacy boss's behaviour changed (its answers are the calls the
 * daemons made before).
 */
public interface DaemonMaster {
    /** The boss entity itself: the daemons follow its target, hover round it, are allied to it and vanish with it. */
    Mob daemonMaster();

    /** The act of the fight, 1 to 5 (the legacy boss answers its phase, 1 to 4): the arch daemon casts Tensura spells from the third. */
    int daemonAct();

    /** The foes of the master within {@code radius} of a point: the only ones a daemon's bolt, circle or well may hurt. */
    List<LivingEntity> daemonFoes(Vec3 at, double radius);

    /** A daemon's heavy blow on a foe, in the master's own damage law (the legacy boss's word damage, the remade boss's scaled magic damage). */
    void daemonBlast(LivingEntity victim, float raw);

    /** An arch daemon fell: its anchor on the master is gone. */
    void archFell();

    /** A rule stone of Overwrite was broken. */
    void stoneBroken();
}
