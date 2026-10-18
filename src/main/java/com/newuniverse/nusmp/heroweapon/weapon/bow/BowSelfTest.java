package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.heroweapon.HeroComponents;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: nothing of the Legendary Bow can be played by its author, so the server smoke test runs this self-test in the real game ({@code hero.weapon.bow}).
 * It checks the paper first (every row of the numbers table keeps the owner's budget, the logic lists exactly the ids of {@link BowIds} and the registry
 * knows them, every form of the weapon names only registered skills and known passives and holds at most one ultimate), and then the play: a stand-in
 * player is bonded to the Bow with the weapon in hand, a husk stands eight blocks in front of them, EVERY technique is cast once, every kind of armed arrow
 * is loosed from a full draw, a tap and a weak draw are let go, and none may throw; the plain arrow must find the husk in its line (the targeting rules
 * work) and the tap must not shoot. Everything the test spawned is removed and the bond is released, whatever happens.
 */
final class BowSelfTest {
    private BowSelfTest() {}

    private static final String PLAYER = "hsb_bow_tester";

    static SelfTests.Result run(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        HeroLogic logic = HeroLogics.of(HeroKind.BOW);
        for (SkillRow r : BowNumbers.ROWS) bad.addAll(r.problems());
        Set<String> listed = new HashSet<>();
        for (HeroSkill s : logic.skills()) listed.add(s.id());
        if (!listed.equals(new HashSet<>(BowIds.ALL))) bad.add("the logic lists " + listed + " but BowIds has " + BowIds.ALL);
        for (String id : BowIds.ALL) if (HeroLogics.skill(id) == null) bad.add(id + " is not in the skill registry");
        List<HeroForm> forms = HeroForms.of(HeroKind.BOW);
        int ordinary = 0, curse = 0;
        for (HeroForm f : forms) {
            if (f.curse()) curse++;
            else ordinary++;
            int ultimates = 0;
            for (String id : f.skills().values()) {
                HeroSkill s = HeroLogics.skill(id);
                if (s == null) bad.add("form " + f.id() + " names the unknown skill " + id);
                else if (s.ultimate()) ultimates++;
                if (!id.startsWith("bow.")) bad.add("form " + f.id() + " names a skill of another weapon: " + id);
            }
            if (f.skills().containsKey("use")) bad.add("form " + f.id() + " uses the 'use' slot, which is the draw of the bow");
            if (ultimates > 1) bad.add("form " + f.id() + " lists " + ultimates + " ultimates");
            for (String pass : f.passives()) if (!BowIds.PASSIVES.contains(pass)) bad.add("form " + f.id() + " has the unknown passive " + pass);
        }
        if (!bad.isEmpty()) return fail(bad);

        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        String paper = forms.size() + " forms (" + ordinary + " ordinary, " + curse + " curse), " + BowIds.ALL.size() + " skills";
        if (!level.hasChunkAt(spawn)) return SelfTests.Result.ok(paper + "; the casts were skipped: the spawn chunk is not loaded");

        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
        ServerPlayer fp = SelfTests.player(level, PLAYER, spawn.getX() + 0.5, ground, spawn.getZ() + 0.5);
        if (HeroBonds.of(fp).isBound()) HeroBonds.release(fp);
        String refused = HeroBonds.bind(fp, HeroKind.BOW);
        if (refused != null) return SelfTests.Result.ok(paper + "; the casts were skipped: the stand-in could not bind (" + refused + ")");
        Husk dummy = null;
        try {
            HeroBond bond = HeroBonds.of(fp);
            bond.setSummoned(true);
            ItemStack stack = HeroItems.stack(HeroKind.BOW);
            stack.set(HeroComponents.OWNER.get(), fp.getStringUUID());
            fp.setItemInHand(InteractionHand.MAIN_HAND, stack);
            fp.setYRot(0f);
            fp.setXRot(4f);
            List<String> passives = new ArrayList<>(BowIds.PASSIVES);
            HeroForm all = new HeroForm(HeroKind.BOW, "selftest", "Self test", "", 1, List.of(), List.of(), 0, Map.of("attack_damage", 4.0), Map.of(), Map.of(), passives, "base", false, null, 0);
            HeroCtx c = new HeroCtx(fp, bond, all, stack);

            dummy = EntityType.HUSK.create(level);
            if (dummy == null) return fail(List.of("could not create the dummy"));
            dummy.moveTo(fp.getX(), ground, fp.getZ() + 8.0, 180f, 0f);
            dummy.setNoAi(true);
            level.addFreshEntity(dummy);

            // the targeting rules find the husk in the line of a shot
            Vec3 eye = fp.getEyePosition();
            List<LivingEntity> line = Foes.along(c, eye, eye.add(fp.getViewVector(1f).scale(20.0)), 0.45, 1);
            if (!line.contains(dummy)) bad.add("the line of a shot does not find the husk eight blocks ahead (the targeting rules or the aim are wrong)");

            // a tap must not shoot, a full draw must
            logic.onTick(c);
            float before = dummy.getHealth();
            if (!logic.onRelease(c, 1)) bad.add("a tap on the string was not handled");
            if (dummy.getHealth() < before) bad.add("a tap on the string hurt the husk");
            if (!logic.onRelease(c, 25)) bad.add("a full draw was not handled");
            String hit = dummy.getHealth() < before ? "the plain arrow hurt the husk" : "the plain arrow did not hurt the husk (the balance law may have refused it)";

            // every kind of armed arrow, from a full draw (the lock between two shots is lifted by moving the clock of the last shot back)
            for (ArmedMode mode : ArmedMode.values()) {
                if (mode == ArmedMode.NONE) continue;
                BowState.arm(c, mode, level.getGameTime() + BowNumbers.ARMED_TICKS);
                BowState.markShot(c, level.getGameTime() - 100);
                if (!logic.onRelease(c, 25)) bad.add("the " + mode.id() + " arrow was not handled");
                if (BowState.armed(c, level.getGameTime()) != ArmedMode.NONE) bad.add("the " + mode.id() + " arming was not spent by a full draw");
            }
            // a weak draw does not spend a sniper arming
            BowState.arm(c, ArmedMode.SNIPER, level.getGameTime() + BowNumbers.ARMED_TICKS);
            BowState.markShot(c, level.getGameTime() - 100);
            logic.onRelease(c, 6);
            if (BowState.armed(c, level.getGameTime()) != ArmedMode.SNIPER) bad.add("a weak draw spent the sniper arming");
            logic.onTick(c);
            BowState.disarm(c);

            // every technique once
            int accepted = 0, refusedCasts = 0;
            for (HeroSkill s : logic.skills()) {
                try {
                    if (s.cast(c)) accepted++;
                    else refusedCasts++;
                } catch (Throwable t) {
                    bad.add(s.id() + " threw " + t);
                }
            }
            logic.onFormChanged(c, all, all);
            logic.onDismiss(c);
            for (String name : Tasks.failed("skill bow.")) bad.add(name + " threw (the first failure is in the log)");               // a guarded body that threw is only refused, so ask
            for (String name : Tasks.failed("bow ")) bad.add("the hook '" + name + "' threw (the first failure is in the log)");
            if (!bad.isEmpty()) return fail(bad);
            return SelfTests.Result.ok(paper + "; " + accepted + " techniques cast and " + refusedCasts + " refused without an error; " + hit);
        } finally {
            if (dummy != null) dummy.discard();
            fp.removeAllEffects();
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            fp.setPos(spawn.getX() + 0.5, ground, spawn.getZ() + 0.5);
            HeroBonds.release(fp);
        }
    }

    private static SelfTests.Result fail(List<String> bad) {
        return SelfTests.Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))) + (bad.size() > 6 ? " (+" + (bad.size() - 6) + " more)" : ""));
    }
}
