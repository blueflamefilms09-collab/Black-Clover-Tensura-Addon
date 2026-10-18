package com.newuniverse.nusmp.heroweapon.weapon.gauntlet;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.kit.VbSkill;
import com.newuniverse.nusmp.heroweapon.weapon.kit.WeaponSelfTest;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.List;
import java.util.UUID;

import static com.newuniverse.nusmp.heroweapon.weapon.gauntlet.GauntletNumbers.*;

/**
 * Why: the behaviour of the Vassal Gauntlet (the Seven Star Weapon of the fists). The series gives it the power-up method Skill Power-up Points and the skills
 * Air Strike Rush, Moonlight Kick, Afterimage Palm, Shaking Quaking Fist, Turtle Crush and Dragon Slaying Conflagration Fist; here that becomes a fighter's kit
 * of nine techniques played around a pool of skill points ({@link PointPool}) that blows, techniques and guarded hits fill and the next technique spends to hit
 * harder, ending in an ultimate that needs a full pool. The iron guard stance, the second wind and the passives make the fist a close-range weapon that is
 * strong while it keeps hitting and weak when it is kept away. Every number is {@link GauntletNumbers}'; the live skills are built from that table. The
 * constructor registers the self-test {@code hero.weapon.gauntlet} and the two clean-up listeners once.
 */
public final class GauntletLogic implements HeroLogic {
    private static boolean wired;

    private final List<HeroSkill> skills;

    public GauntletLogic() {
        VbSkill.Cooldown blossom = (c, row) -> c.form.has("blossom_step") ? Budget.scaled(row.cooldown(), BLOSSOM_COOLDOWN, Budget.floor(row.kind())) : row.cooldown();
        this.skills = List.of(
                skill(RUSH.spec(), GauntletSpells::rush, null),
                skill(MOONLIGHT_KICK.spec(), GauntletSpells::moonlightKick, null),
                skill(AFTERIMAGE_PALM.spec(), GauntletSpells::afterimagePalm, blossom),
                skill(QUAKING_FIST.spec(), GauntletSpells::quakingFist, null),
                skill(TIGER_LEAP.spec(), GauntletSpells::tigerLeap, blossom),
                skill(TURTLE_CRUSH.spec(), GauntletSpells::turtleCrush, null),
                skill(IRON_GUARD, GauntletSpells::ironGuard, null),
                skill(SECOND_WIND, GauntletSpells::secondWind, null),
                skill(DRAGON_SLAYER.spec(), GauntletSpells::dragonSlayer, null));
        wire();
    }

    private static VbSkill skill(Budget.Spec spec, VbSkill.Body body, VbSkill.Cooldown cooldown) {
        VbSkill s = VbSkill.of(spec, GauntletText.name(spec.id()), GauntletText.desc(spec.id()), body);
        return cooldown == null ? s : s.withCooldown(cooldown);
    }

    private static synchronized void wire() {
        if (wired) return;
        wired = true;
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            GauntletState.clear();
            VbSkill.forgetLogged();
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> GauntletState.forget(e.getEntity().getUUID()));
        SelfTests.register("hero.weapon.gauntlet", GauntletLogic::selfTest);
    }

    private static SelfTests.Result selfTest(MinecraftServer server) {
        return WeaponSelfTest.run(server, HeroKind.GAUNTLET, ALL, null, fp -> GauntletState.forget(fp.getUUID()));
    }

    @Override
    public HeroKind kind() { return HeroKind.GAUNTLET; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    /** Shows the pool on the action bar when its number changes (a point earned, or faded away). */
    @Override
    public void onTick(HeroCtx c) {
        UUID who = c.player.getUUID();
        int n = GauntletState.pool(who).points(c.level.getGameTime());
        int shown = GauntletState.shown(who);
        if (n == shown || (n == 0 && shown <= 0)) return;
        GauntletState.setShown(who, n);
        c.say(n > 0 ? "Skill points " + n + "/" + PointPool.MAX : "");
    }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) {
        int n = fullSwing ? POINTS_FULL_SWING : POINTS_HIT;
        if (fullSwing && c.form.has("hardened_fist")) n++;
        GauntletSpells.gain(c, n);
    }

    @Override
    public void onKill(HeroCtx c, LivingEntity victim) { GauntletSpells.gain(c, POINTS_KILL); }

    @Override
    public float onDamaged(HeroCtx c, DamageSource source, float amount) { return GauntletSpells.onDamaged(c, source, amount); }

    @Override
    public void onDismiss(HeroCtx c) { GauntletState.forget(c.player.getUUID()); }
}
