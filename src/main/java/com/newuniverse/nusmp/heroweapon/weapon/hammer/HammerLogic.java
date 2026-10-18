package com.newuniverse.nusmp.heroweapon.weapon.hammer;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.strike.FormsCheck;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Strike;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeFx;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeSelfTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Why: the behaviour of the {@link HeroKind#HAMMER} weapon (the Vassal Hammer, a Seven Star Weapon of the world of the shield hero, the impact weapon that supports the shield
 * hero). It lists the seven techniques ({@link HammerSkills}) and adds what a blow of the hammer does on its own: every blow builds the foe's poise ({@link HammerPoise}) so a
 * foe that is hit enough breaks; the passives of the forms are the signature effects the canon power-up, Weapon Form Synthesis, copies from form to form
 * ({@link HammerSynthesis}): Heavy Blows (every form), Rubble (stone: a full swing splashes), Anvil Stance (iron: sneaking blunts the blows you take), Crushing Weight (the top
 * form: every third full swing cracks armour) and Synthesis itself (the top form keeps the signature of every form you learned). Registers its self-test
 * {@code hero.weapon.hammer} from the constructor. Server thread only; no hook throws (the dispatcher guards them as well).
 */
public final class HammerLogic implements HeroLogic {
    private static final FormsCheck.Known KNOWN = new FormsCheck.Known("vassal_hammer", HammerNumbers.IDS, HammerNumbers.ULTIMATES, HammerNumbers.PASSIVES, 3, 5, false);
    /** The wielder's count of full swings since the last cracked armour (saved with the player, harmless if lost). */
    private static final String SWINGS = "nusmp_hv_sw";

    private final List<HeroSkill> skills = HammerSkills.all();

    public HammerLogic() { StrikeSelfTest.register(HeroKind.HAMMER, KNOWN); }

    @Override
    public HeroKind kind() { return HeroKind.HAMMER; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    /** True when the passive is in force for the wielder's active form, either its own or one it borrows through Synthesis. */
    private static boolean has(HeroCtx c, String passive) { return HammerSynthesis.effective(passive, c.form.passives(), c.bond.unlocked()); }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) {
        if (Strike.inHit() || target == null) return;                          // a technique's own blow is not a swing
        if (has(c, HammerNumbers.HEAVY_BLOWS)) HammerPoise.add(c, target, fullSwing ? HammerNumbers.FULL_SWING_POISE : HammerNumbers.SWING_POISE);
        if (!fullSwing) return;
        if (has(c, HammerNumbers.RUBBLE)) rubble(c, target);
        if (has(c, HammerNumbers.CRUSHING_WEIGHT)) crush(c, target);
    }

    /** Rubble: a full swing throws stone chips at the foes standing close to the one struck. */
    private static void rubble(HeroCtx c, LivingEntity target) {
        int n = 0;
        for (LivingEntity f : Strike.foesNear(c.player, target.position(), HammerNumbers.RUBBLE_RADIUS, HammerNumbers.RUBBLE_TARGETS + 1)) {
            if (f == target || n >= HammerNumbers.RUBBLE_TARGETS) continue;
            if (Strike.hit(c, f, HammerNumbers.RUBBLE_RAW, false)) {
                HammerPoise.add(c, f, HammerNumbers.RUBBLE_POISE);
                n++;
            }
        }
        if (n > 0) StrikeFx.burst(c.level, target.position(), HammerPoise.AMBER, 10);
    }

    /** Crushing Weight: every third full swing cracks the armour of the foe struck. */
    private static void crush(HeroCtx c, LivingEntity target) {
        CompoundTag d = c.player.getPersistentData();
        int n = d.getInt(SWINGS) + 1;
        if (n >= HammerNumbers.CRUSH_EVERY) {
            n = 0;
            if (target.isAlive()) {
                HammerPoise.crackArmor(target, HammerNumbers.CRUSH_ARMOR, 0.0, HammerNumbers.CRUSH_TICKS);
                StrikeFx.sound(c.level, target.position(), SoundEvents.SHIELD_BREAK, 0.5f, 1.2f);
            }
        }
        d.putInt(SWINGS, n);
    }

    /** Anvil Stance: while the hammer is out and the wielder sneaks, a blow from another creature lands at 82 percent. Falls, fire and the void are not blunted. */
    @Override
    public float onDamaged(HeroCtx c, DamageSource source, float amount) {
        if (amount <= 0f || source.getEntity() == null || !c.summoned()) return amount;
        if (c.player.isShiftKeyDown() && has(c, HammerNumbers.ANVIL_STANCE)) return amount * (float) HammerNumbers.ANVIL_STANCE_FACTOR;
        return amount;
    }
}
