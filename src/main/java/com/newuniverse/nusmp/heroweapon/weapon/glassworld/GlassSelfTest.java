package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import com.newuniverse.nusmp.heroweapon.HeroComponents;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroTrigger;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Why: nothing of a hero weapon can be played by its author, so each of the four weapons of this package registers a SELF-TEST ({@code hero.weapon.<pkg>}) that
 * the CI server smoke test runs in the real game. They all do the same thing, so it is written once here. It checks the weapon's registry (every skill id carries
 * the weapon's prefix and is unique, every skill matches its row of the number table, every form of the weapon's forms file names only skills that exist, at most
 * one ultimate to a form, only real input slots), then binds the weapon to a stand-in player (a {@code FakePlayer}), puts the item in its hand, stands two
 * dummy zombies in front of it and casts every skill of every form once, then calls every hook of the logic (tick, hit, damage taken, kill, form change, summon,
 * dismiss) once, all inside try and catch: a skill may refuse (it returns false) but it must never throw. Everything it made is removed again and the bond is
 * released, so the run leaves the world as it found it.
 */
public final class GlassSelfTest {
    private GlassSelfTest() {}

    /** What a weapon's own extra steps get to work with: the base form's context, a dummy foe standing 3.5 blocks in front, the level, and the list to report problems in. */
    public record Probe(HeroCtx ctx, LivingEntity foe, ServerLevel level, List<String> bad) {}

    /**
     * Runs the test of one weapon.
     *
     * @param server the running server
     * @param kind   the weapon
     * @param table  the weapon's number table, the rows its skills must match
     * @param extra  a weapon's own extra steps, run once after the generic casts; may be null
     */
    public static SelfTests.Result run(MinecraftServer server, HeroKind kind, List<Budget.Spec> table, Consumer<Probe> extra) {
        List<String> bad = new ArrayList<>();
        if (!HeroLogics.has(kind)) return SelfTests.Result.fail(kind + " has no registered logic");
        HeroLogic logic = HeroLogics.of(kind);
        List<HeroSkill> skills = HeroLogics.skillsOf(kind);
        String prefix = kind.name().toLowerCase(Locale.ROOT) + ".";

        Set<String> ids = new LinkedHashSet<>();
        for (HeroSkill s : skills) {
            if (!s.id().startsWith(prefix)) bad.add("skill " + s.id() + " must start with " + prefix);
            if (!ids.add(s.id())) bad.add("skill " + s.id() + " is listed twice");
            if (s.name() == null || s.name().isBlank() || s.desc() == null || s.desc().isBlank()) bad.add("skill " + s.id() + " has no name or description");
            if (HeroLogics.skill(s.id()) != s) bad.add("skill " + s.id() + " is not the skill the registry holds under that id");
        }
        if (skills.size() < 6) bad.add("only " + skills.size() + " skills, a vassal weapon needs at least 6");

        List<HeroForm> forms = HeroForms.of(kind);
        if (forms.isEmpty() || !HeroForm.BASE_ID.equals(forms.get(0).id())) bad.add("the first form is not the base form");
        int ultimateForms = 0;
        for (HeroForm f : forms) {
            int ultimates = 0;
            for (var e : f.skills().entrySet()) {
                if (HeroTrigger.bySlot(e.getKey()) == null) bad.add("form " + f.id() + " uses the slot " + e.getKey() + ", which is not an input");
                HeroSkill s = HeroLogics.skill(e.getValue());
                if (s == null) bad.add("form " + f.id() + " lists the skill " + e.getValue() + ", which does not exist");
                else {
                    if (!s.id().startsWith(prefix)) bad.add("form " + f.id() + " lists the skill " + s.id() + " of another weapon");
                    if (s.ultimate()) ultimates++;
                }
            }
            if (ultimates > 1) bad.add("form " + f.id() + " lists " + ultimates + " ultimates, the most is one");
            if (ultimates == 1) ultimateForms++;
        }
        if (!bad.isEmpty()) return SelfTests.Result.fail(firstFew(bad));

        ServerLevel level = server.overworld();
        FakePlayer fp = SelfTests.player(level, "hero_" + kind.pkg() + "_tester", 0.5, 100.0, 0.5);
        fp.setYRot(0.0f);
        fp.setXRot(0.0f);
        HeroBonds.release(fp);
        String refusal = HeroBonds.bind(fp, kind);
        if (refusal != null) return SelfTests.Result.fail("the stand-in wielder could not be bound to the " + kind.display() + ": " + refusal);

        Zombie near = null, far = null;
        int casts = 0, hooks = 0;
        try {
            HeroBond bond = HeroBonds.of(fp);
            ItemStack stack = HeroItems.stack(kind);
            stack.set(HeroComponents.OWNER.get(), fp.getUUID().toString());
            fp.setItemInHand(InteractionHand.MAIN_HAND, stack);
            bond.setSummoned(true);
            near = dummy(level, 0.5, 100.0, 3.5);
            far = dummy(level, 1.5, 100.0, 5.5);

            HeroCtx base = new HeroCtx(fp, bond, forms.get(0), stack);
            for (HeroSkill s : skills) {
                Budget.Spec row = row(table, s.id());
                if (row == null) {
                    bad.add("skill " + s.id() + " has no row in the number table");
                    continue;
                }
                if (s.cooldownTicks(base) != row.cooldown()) bad.add(s.id() + " rests " + s.cooldownTicks(base) + " ticks but its row says " + row.cooldown());
                if (Math.abs(s.magoiShare() - row.cost()) > 1e-9) bad.add(s.id() + " costs " + s.magoiShare() + " but its row says " + row.cost());
                if (s.ultimate() != (row.kind() == Budget.Kind.ULTIMATE)) bad.add(s.id() + " ultimate flag " + s.ultimate() + " disagrees with its row");
            }
            for (Budget.Spec r : table) if (!ids.contains(r.id())) bad.add("the number table has the row " + r.id() + ", which no skill uses");

            // every skill of the weapon once with the base form, then every skill each form lists with that form
            for (HeroSkill s : skills) casts += cast(base, s, near, far, bad);
            for (HeroForm f : forms) {
                HeroCtx c = new HeroCtx(fp, bond, f, stack);
                for (String id : new LinkedHashSet<>(f.skills().values())) {
                    HeroSkill s = HeroLogics.skill(id);
                    if (s != null) casts += cast(c, s, near, far, bad);
                }
                hooks += hooks(logic, c, f, near, level, bad);
            }
            if (extra != null) {
                try {
                    extra.accept(new Probe(base, near, level, bad));
                } catch (Throwable t) {
                    bad.add("the weapon's own extra steps threw " + t);
                }
            }
        } finally {
            if (near != null) near.discard();
            if (far != null) far.discard();
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            HeroBonds.of(fp).setSummoned(false);
            HeroBonds.release(fp);
            fp.setHealth(fp.getMaxHealth());
            fp.removeAllEffects();
            if (logic instanceof Forgetful forgetful) forgetful.forget(fp.getUUID());
        }
        if (!bad.isEmpty()) return SelfTests.Result.fail(firstFew(bad));
        return SelfTests.Result.ok(skills.size() + " skills in " + forms.size() + " forms (" + ultimateForms + " with an ultimate), " + casts + " casts and " + hooks
                + " hook calls with a stand-in wielder beside two dummy zombies, none threw");
    }

    private static Budget.Spec row(List<Budget.Spec> table, String id) {
        for (Budget.Spec r : table) if (r.id().equals(id)) return r;
        return null;
    }

    private static Zombie dummy(ServerLevel level, double x, double y, double z) {
        Zombie m = EntityType.ZOMBIE.create(level);
        if (m == null) throw new IllegalStateException("a dummy zombie could not be made");
        m.setNoAi(true);
        m.setPersistenceRequired();
        var health = m.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.setBaseValue(2000.0);
        m.setHealth(2000.0f);
        m.setPos(x, y, z);
        level.addFreshEntity(m);
        return m;
    }

    /** One cast inside try and catch; the dummies are healed again afterwards. Returns 1 when it ran (true or false), whatever it answered. */
    private static int cast(HeroCtx c, HeroSkill s, Zombie near, Zombie far, List<String> bad) {
        try {
            s.cast(c);
        } catch (Throwable t) {
            bad.add(s.id() + " threw " + t + where(t));
        }
        near.setHealth(near.getMaxHealth());
        far.setHealth(far.getMaxHealth());
        return 1;
    }

    private static int hooks(HeroLogic logic, HeroCtx c, HeroForm form, Zombie target, ServerLevel level, List<String> bad) {
        int n = 0;
        DamageSource mob = level.damageSources().mobAttack(target), fall = level.damageSources().fall();
        n += hook("onSummon", bad, () -> logic.onSummon(c));
        n += hook("onTick", bad, () -> logic.onTick(c));
        n += hook("onHit", bad, () -> logic.onHit(c, target, 5.0f, true));
        n += hook("onHit (weak)", bad, () -> logic.onHit(c, target, 1.0f, false));
        n += hook("onDamaged (mob)", bad, () -> logic.onDamaged(c, mob, 4.0f));
        n += hook("onDamaged (fall)", bad, () -> logic.onDamaged(c, fall, 6.0f));
        n += hook("onBlocked", bad, () -> logic.onBlocked(c, mob, 4.0f));
        n += hook("onKill", bad, () -> logic.onKill(c, target));
        n += hook("onRelease", bad, () -> logic.onRelease(c, 20));
        n += hook("onFormChanged", bad, () -> logic.onFormChanged(c, form, form));
        n += hook("onDismiss", bad, () -> logic.onDismiss(c));
        target.setHealth(target.getMaxHealth());
        return n;
    }

    private static int hook(String name, List<String> bad, Runnable call) {
        try {
            call.run();
        } catch (Throwable t) {
            bad.add("the hook " + name + " threw " + t + where(t));
        }
        return 1;
    }

    private static String where(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        return st.length == 0 ? "" : " at " + st[0].getClassName().substring(st[0].getClassName().lastIndexOf('.') + 1) + "." + st[0].getMethodName() + ":" + st[0].getLineNumber();
    }

    private static String firstFew(List<String> bad) {
        Set<String> seen = new HashSet<>();
        List<String> unique = new ArrayList<>();
        for (String s : bad) if (seen.add(s)) unique.add(s);
        return unique.size() + " problem(s): " + String.join(" | ", unique.subList(0, Math.min(5, unique.size()))) + (unique.size() > 5 ? " (+" + (unique.size() - 5) + " more)" : "");
    }
}
