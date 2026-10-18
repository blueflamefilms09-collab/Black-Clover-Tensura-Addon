package com.newuniverse.nusmp.heroweapon.weapon.projectile;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.kit.VbSkill;
import com.newuniverse.nusmp.heroweapon.weapon.kit.WeaponSelfTest;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.List;

import static com.newuniverse.nusmp.heroweapon.weapon.projectile.ProjectileNumbers.*;

/**
 * Why: the behaviour of the Vassal Projectile (the Seven Star Weapon of thrown blades). The series gives it a semi-transparent knife that becomes chakrams,
 * kunai, hatchets and boomerangs, the throws Air Strike Throw, Second Throw, Dritte Throw, Tornado Throw, Bolas Blast, Knife Rain and Shadow Bind, and the
 * power-up method Over Custom (customisation paid for with money); here that becomes a thrower's kit of ten techniques in which every blade that hits MARKS its
 * foe and Recall Blades cashes the marks in, with a boomerang, a ricochet and a field of falling blades for the rest. Every number is {@link ProjectileNumbers}';
 * the live skills are built from that table; Over Custom shortens every cooldown by a tenth (never below the floor of its kind). The melee stab of the knife
 * marks too. The constructor registers the self-test {@code hero.weapon.projectile} and the two clean-up listeners once.
 */
public final class ProjectileLogic implements HeroLogic {
    private static boolean wired;

    private final List<HeroSkill> skills;

    public ProjectileLogic() {
        this.skills = List.of(
                skill(INSTANT_THROW, ProjectileSpells::instantThrow),
                skill(BOOMERANG, ProjectileSpells::boomerang),
                skill(RICOCHET, ProjectileSpells::ricochet),
                skill(KNIFE_RAIN, ProjectileSpells::knifeRain),
                skill(TORNADO_THROW, ProjectileSpells::tornadoThrow),
                skill(BOLAS_BLAST, ProjectileSpells::bolasBlast),
                skill(DRITTE_THROW, ProjectileSpells::dritteThrow),
                skill(RECALL, ProjectileSpells::recall),
                skill(SHADOW_BIND, ProjectileSpells::shadowBind),
                skill(THOUSAND_BLADES, ProjectileSpells::thousandBlades));
        wire();
    }

    private static VbSkill skill(Budget.Spec spec, VbSkill.Body body) {
        return VbSkill.of(spec, ProjectileText.name(spec.id()), ProjectileText.desc(spec.id()), body)
                .withCooldown((c, row) -> ProjectileNumbers.cooldown(row, c.form.has("over_custom")));
    }

    private static synchronized void wire() {
        if (wired) return;
        wired = true;
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            ProjectileState.clear();
            VbSkill.forgetLogged();
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> ProjectileState.forget(e.getEntity().getUUID()));
        SelfTests.register("hero.weapon.projectile", ProjectileLogic::selfTest);
    }

    private static SelfTests.Result selfTest(MinecraftServer server) {
        return WeaponSelfTest.run(server, HeroKind.PROJECTILE, ALL, null, fp -> ProjectileState.forget(fp.getUUID()));
    }

    @Override
    public HeroKind kind() { return HeroKind.PROJECTILE; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onTick(HeroCtx c) { ProjectileState.prune(c.player.getUUID(), c.level.getGameTime()); }

    /** The knife in the hand marks whatever it stabs (the same mark the thrown blades leave). */
    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) { ProjectileSpells.mark(c, target); }

    @Override
    public void onDismiss(HeroCtx c) { ProjectileState.forget(c.player.getUUID()); }
}
