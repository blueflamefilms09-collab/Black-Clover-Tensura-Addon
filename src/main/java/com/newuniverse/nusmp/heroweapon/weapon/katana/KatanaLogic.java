package com.newuniverse.nusmp.heroweapon.weapon.katana;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Forgetful;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.GlassKit;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.GlassSelfTest;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.List;
import java.util.UUID;

/**
 * Why: the behaviour of the Vassal Katana: a precise duelist's weapon (long thin slashes, the draw cut of iaido, a parry window that returns damage, a dodge,
 * a flurry, a phantom second blade) whose rhythm is the TEMPO built by chaining different techniques. The nine techniques are in {@link KatanaSkills}, their
 * numbers in {@link KatanaNumbers}, their words in {@link KatanaText}, the tempo in {@link TempoChain} and the short windows in {@link KatanaSession}; this
 * class is the part the shared machinery calls: it lists the skills, answers a blow aimed at the wielder while a parry or an evade is open (a parry stops at
 * most 60 percent of the wielder's health, answers with a cut and jumps the tempo to full), adds the riders to a real melee hit (the phantom blade's cut, Keen
 * Opening after four quiet seconds, the Riposte after a parry) and never answers its own techniques' damage. It also registers the self-test
 * {@code hero.weapon.katana}, which includes a parry round, and clears its per-wielder state when a wielder leaves or the server stops.
 */
public final class KatanaLogic implements HeroLogic, Forgetful {
    private static final int PINK = 0xFFFFB8D6, WHITE = 0xFFF4F7FF;

    private final List<HeroSkill> skills = KatanaSkills.all();

    public KatanaLogic() {
        SelfTests.register("hero.weapon.katana", server -> GlassSelfTest.run(server, HeroKind.KATANA, KatanaNumbers.table(), KatanaLogic::parryRound));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> KatanaState.clearAll());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> KatanaState.forget(e.getEntity().getUUID()));
    }

    @Override
    public HeroKind kind() { return HeroKind.KATANA; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) {
        if (GlassKit.striking()) return;                              // the katana's own techniques are not melee blows
        KatanaSession s = KatanaSkills.session(c);
        long now = c.level.getGameTime();
        boolean opening = fullSwing && c.form.has(KatanaNumbers.P_KEEN_OPENING) && s.quiet(now, KatanaNumbers.OPENING_IDLE);
        boolean riposte = fullSwing && c.form.has(KatanaNumbers.P_RIPOSTE) && s.takeRiposte(now);
        boolean echo = s.takeEcho(now);
        s.acted(now);                                                 // before any rider strikes, so a rider can never be counted as a quiet swing
        Vec3 centre = target.getBoundingBox().getCenter();
        if (opening) {
            GlassKit.strike(c, target, KatanaNumbers.OPENING_RAW);
            c.fx(VfxShape.SLASH_WAVE, centre.add(0.0, 0.7, 0.0), centre, WHITE, 8, 0.9f);
        }
        if (riposte) {
            GlassKit.strike(c, target, KatanaNumbers.RIPOSTE_RAW);
            c.fx(VfxShape.SLASH_WAVE, centre.add(0.7, 0.2, 0.0), centre, PINK, 8, 0.9f);
        }
        if (echo) {
            GlassKit.strike(c, target, KatanaNumbers.ECHO_RAW);
            c.fx(VfxShape.SLASH_WAVE, centre.add(-0.7, 0.4, 0.0), centre, PINK, 8, 0.8f);
            c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.5f, 1.8f);
        }
    }

    @Override
    public float onDamaged(HeroCtx c, DamageSource source, float amount) {
        if (!c.summoned() || amount <= 0f) return amount;
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == c.player) return amount;
        KatanaSession s = KatanaSkills.session(c);
        long now = c.level.getGameTime();
        float max = c.player.getMaxHealth();
        if (s.counterOpen(now)) {
            s.closeCounter();
            float through = ParryRules.passThrough(amount, max);
            if (GlassKit.isTarget(c.player, attacker) && attacker.distanceTo(c.player) <= KatanaNumbers.COUNTER_REACH) {
                if (GlassKit.strike(c, attacker, ParryRules.counterRaw(amount))) GlassKit.push(c, attacker, c.player.position(), KatanaNumbers.COUNTER_SHOVE);
            }
            s.chain().perfect(now);
            s.acted(now);
            if (c.form.has(KatanaNumbers.P_RIPOSTE)) s.openRiposte(now, KatanaNumbers.RIPOSTE_TICKS);
            Vec3 chest = GlassKit.chest(c.player);
            c.fx(VfxShape.STAR_MAGIC, chest, attacker.getBoundingBox().getCenter(), WHITE, 14, 1.6f);
            c.fx(VfxShape.WIND_RING, c.player.position(), chest, PINK, 12, 0.7f);
            c.sound(SoundEvents.SHIELD_BLOCK, 1.0f, 1.5f);
            c.say(through > 0f ? "Parried, but it was too heavy to stop all of it. Tempo full." : "Parried. Tempo full.");
            return through;
        }
        if (s.evadeOpen(now)) {
            s.closeEvade();
            c.fx(VfxShape.STAR_MAGIC, GlassKit.chest(c.player), GlassKit.chest(c.player).add(0.0, 0.5, 0.0), WHITE, 10, 1.0f);
            c.sound(SoundEvents.PHANTOM_FLAP, 0.8f, 1.9f);
            c.say("Evaded.");
            return ParryRules.passThrough(amount, max);
        }
        return amount;
    }

    @Override
    public void onFormChanged(HeroCtx c, HeroForm from, HeroForm to) {
        KatanaSkills.session(c).reset();
    }

    @Override
    public void onDismiss(HeroCtx c) {
        forget(c.player.getUUID());
    }

    @Override
    public void forget(UUID wielder) {
        KatanaState.forget(wielder);
    }

    /** The self-test's own round: a parry stops a small blow once and not a second, an evade does the same, and a huge blow only partly. */
    private static void parryRound(GlassSelfTest.Probe p) {
        HeroCtx c = p.ctx();
        HeroLogic logic = HeroLogics.of(HeroKind.KATANA);
        HeroSkill stance = HeroLogics.skill(KatanaNumbers.COUNTER_STANCE), evade = HeroLogics.skill(KatanaNumbers.RELEASE_EVADE);
        if (stance == null || evade == null) {
            p.bad().add("the parry round could not find the stances");
            return;
        }
        DamageSource blow = p.level().damageSources().mobAttack(p.foe());
        stance.cast(c);
        float stopped = logic.onDamaged(c, blow, 5.0f);
        if (stopped != 0.0f) p.bad().add("an open counter stance let " + stopped + " of a 5 point blow through");
        float second = logic.onDamaged(c, blow, 5.0f);
        if (second != 5.0f) p.bad().add("the counter stance stopped a second blow (" + second + " got through)");
        evade.cast(c);
        float dodged = logic.onDamaged(c, blow, 5.0f);
        if (dodged != 0.0f) p.bad().add("an open evade let " + dodged + " of a 5 point blow through");
        stance.cast(c);
        float huge = logic.onDamaged(c, blow, 1000.0f);
        if (!(huge > 0.0f && huge < 1000.0f)) p.bad().add("a huge blow should be stopped only in part, but " + huge + " got through");
        KatanaSkills.session(c).reset();
    }
}
