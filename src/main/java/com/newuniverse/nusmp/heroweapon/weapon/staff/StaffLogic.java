package com.newuniverse.nusmp.heroweapon.weapon.staff;

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

import static com.newuniverse.nusmp.heroweapon.weapon.staff.StaffNumbers.*;

/**
 * Why: the behaviour of the Vassal Staff (the Seven Star Weapon of casting). The series gives it accelerated magic casting, a Magic Rank system for spells and
 * the skills Fenrir Rod, Float Mirror and Magic Prism; here that becomes a mage's kit of ten techniques (a piercing bolt, a chain spark, a rune field, a burst,
 * a prism that multiplies bolts, a mirror that sends shots back, a haste of casting, a mark that makes foes weaker to the staff, a heal, and a bombardment as
 * the ultimate) played around a casting RHYTHM: spells cast one after another wait a little less ({@link CastRhythm}), and Fenrir Rod quickens all of them.
 * Every number is {@link StaffNumbers}'; the live skills are built from that table, so the dispatcher's cooldown and cost are the table's. The staff keeps
 * only short-lived memory (see {@link StaffState}), cleared on server stop, logout and when the weapon is put away. The constructor registers the self-test
 * {@code hero.weapon.staff} and the two clean-up listeners once.
 */
public final class StaffLogic implements HeroLogic {
    private static boolean wired;

    private final List<HeroSkill> skills;

    public StaffLogic() {
        this.skills = List.of(
                spell(ARCANE_BOLT, StaffSpells::arcaneBolt, true),
                spell(CHAIN_SPARK, StaffSpells::chainSpark, true),
                spell(RUNE_FIELD, StaffSpells::runeField, true),
                spell(ARCANE_BURST, StaffSpells::arcaneBurst, true),
                spell(MAGIC_PRISM, StaffSpells::magicPrism, true),
                spell(FLOAT_MIRROR, StaffSpells::floatMirror, true),
                spell(FENRIR_ROD, StaffSpells::fenrirRod, false),
                spell(ANALYZE, StaffSpells::analyze, true),
                spell(HEALING_LIGHT, StaffSpells::healingLight, true),
                spell(BOMBARDMENT, StaffSpells::bombardment, true));
        wire();
    }

    /** Builds a live skill from its table row; every cast of a staff spell counts for the rhythm, and all but Fenrir Rod wait the hasted cooldown. */
    private static VbSkill spell(Budget.Spec spec, VbSkill.Body body, boolean hasted) {
        VbSkill.Body counted = c -> {
            boolean used = body.cast(c);
            if (used) StaffState.rhythm(c.player.getUUID()).cast(c.level.getGameTime());
            return used;
        };
        VbSkill s = VbSkill.of(spec, StaffText.name(spec.id()), StaffText.desc(spec.id()), counted);
        if (!hasted) return s;
        return s.withCooldown((c, row) -> Budget.scaled(row.cooldown(), StaffState.rhythm(c.player.getUUID()).factor(c.level.getGameTime(), c.form.has("quick_chant")),
                Budget.floor(row.kind())));
    }

    private static synchronized void wire() {
        if (wired) return;
        wired = true;
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            StaffState.clear();
            VbSkill.forgetLogged();
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> StaffState.forget(e.getEntity().getUUID()));
        SelfTests.register("hero.weapon.staff", StaffLogic::selfTest);
    }

    private static SelfTests.Result selfTest(MinecraftServer server) {
        return WeaponSelfTest.run(server, HeroKind.STAFF, ALL, null, fp -> StaffState.forget(fp.getUUID()));
    }

    @Override
    public HeroKind kind() { return HeroKind.STAFF; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onTick(HeroCtx c) { StaffState.prune(c.player.getUUID(), c.level.getGameTime()); }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) {
        if (fullSwing && c.form.has("quick_chant")) StaffState.rhythm(c.player.getUUID()).beat(c.level.getGameTime());
    }

    @Override
    public void onDismiss(HeroCtx c) { StaffState.forget(c.player.getUUID()); }
}
