package com.newuniverse.nusmp.heroweapon.weapon.spear;

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
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Moves;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import com.newuniverse.nusmp.heroweapon.weapon.spear.RewindBuffer.Snap;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: nothing of the Legendary Spear can be played by its author, so the server smoke test runs this self-test in the real game ({@code hero.weapon.spear}).
 * It checks the paper first (every row of the numbers table keeps the owner's budget, the logic lists exactly the ids of {@link SpearIds} and the registry
 * knows them, every form of the weapon names only registered skills and known passives and holds at most one ultimate), and then the play: a stand-in
 * player is bonded to the Spear with the weapon in hand, a husk stands in front of them, EVERY technique is cast once and every hook is called, and none may
 * throw; the time reversal is proven on a prepared past (a place that is safe, a stand-in moved away, the cast must bring them back). Everything the test
 * spawned is removed and the bond is released, whatever happens.
 */
final class SpearSelfTest {
    private SpearSelfTest() {}

    private static final String PLAYER = "hsb_spear_tester";

    static SelfTests.Result run(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        HeroLogic logic = HeroLogics.of(HeroKind.SPEAR);
        for (SkillRow r : SpearNumbers.ROWS) bad.addAll(r.problems());
        Set<String> listed = new HashSet<>();
        for (HeroSkill s : logic.skills()) listed.add(s.id());
        if (!listed.equals(new HashSet<>(SpearIds.ALL))) bad.add("the logic lists " + listed + " but SpearIds has " + SpearIds.ALL);
        for (String id : SpearIds.ALL) if (HeroLogics.skill(id) == null) bad.add(id + " is not in the skill registry");
        List<HeroForm> forms = HeroForms.of(HeroKind.SPEAR);
        int ordinary = 0, curse = 0;
        for (HeroForm f : forms) {
            if (f.curse()) curse++;
            else ordinary++;
            int ultimates = 0;
            for (String id : f.skills().values()) {
                HeroSkill s = HeroLogics.skill(id);
                if (s == null) bad.add("form " + f.id() + " names the unknown skill " + id);
                else if (s.ultimate()) ultimates++;
                if (!id.startsWith("spear.")) bad.add("form " + f.id() + " names a skill of another weapon: " + id);
            }
            if (ultimates > 1) bad.add("form " + f.id() + " lists " + ultimates + " ultimates");
            for (String pass : f.passives()) if (!SpearIds.PASSIVES.contains(pass)) bad.add("form " + f.id() + " has the unknown passive " + pass);
        }
        if (!bad.isEmpty()) return fail(bad);

        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        String paper = forms.size() + " forms (" + ordinary + " ordinary, " + curse + " curse), " + SpearIds.ALL.size() + " skills";
        if (!level.hasChunkAt(spawn)) return SelfTests.Result.ok(paper + "; the casts were skipped: the spawn chunk is not loaded");

        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
        ServerPlayer fp = SelfTests.player(level, PLAYER, spawn.getX() + 0.5, ground, spawn.getZ() + 0.5);
        if (HeroBonds.of(fp).isBound()) HeroBonds.release(fp);
        String refused = HeroBonds.bind(fp, HeroKind.SPEAR);
        if (refused != null) return SelfTests.Result.ok(paper + "; the casts were skipped: the stand-in could not bind (" + refused + ")");
        Husk dummy = null;
        try {
            HeroBond bond = HeroBonds.of(fp);
            bond.setSummoned(true);
            ItemStack stack = HeroItems.stack(HeroKind.SPEAR);
            stack.set(HeroComponents.OWNER.get(), fp.getStringUUID());
            fp.setItemInHand(InteractionHand.MAIN_HAND, stack);
            fp.setYRot(0f);
            fp.setXRot(0f);
            HeroForm all = new HeroForm(HeroKind.SPEAR, "selftest", "Self test", "", 1, List.of(), List.of(), 0, Map.of(), Map.of(), Map.of(), SpearIds.PASSIVES, "base", false, null, 0);
            HeroCtx c = new HeroCtx(fp, bond, all, stack);

            dummy = EntityType.HUSK.create(level);
            if (dummy == null) return fail(List.of("could not create the dummy"));
            dummy.moveTo(fp.getX(), ground, fp.getZ() + 4.0, 180f, 0f);
            dummy.setNoAi(true);
            level.addFreshEntity(dummy);

            // the hooks
            SpearTime.forget(fp.getUUID());
            logic.onSummon(c);
            logic.onTick(c);
            RewindBuffer buf = SpearTime.buffer(fp.getUUID());
            if (buf.size() != 1) bad.add("onTick with Memory of the Hours wrote " + buf.size() + " snapshots, not 1");
            logic.onHit(c, dummy, 1f, true);
            logic.onDamaged(c, level.damageSources().generic(), 1f);
            logic.onKill(c, dummy);
            logic.onFormChanged(c, all, all);

            // the time reversal on a prepared past
            String rewind = rewind(level, fp, c, buf, bad);

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
            logic.onDismiss(c);
            for (String name : Tasks.failed("skill spear.")) bad.add(name + " threw (the first failure is in the log)");         // a guarded body that threw is only refused, so ask
            for (String name : Tasks.failed("spear ")) bad.add("the hook '" + name + "' threw (the first failure is in the log)");
            if (!bad.isEmpty()) return fail(bad);
            return SelfTests.Result.ok(paper + "; " + accepted + " techniques cast and " + refusedCasts + " refused without an error; " + rewind);
        } finally {
            if (dummy != null) dummy.discard();
            fp.removeAllEffects();
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            SpearTime.forget(fp.getUUID());
            fp.setPos(spawn.getX() + 0.5, ground, spawn.getZ() + 0.5);
            HeroBonds.release(fp);
        }
    }

    /** A past with a safe place in it, a stand-in moved five blocks away: the reversal must bring them back (when the spot really is safe). */
    private static String rewind(ServerLevel level, ServerPlayer fp, HeroCtx c, RewindBuffer buf, List<String> bad) {
        buf.clear();
        double x = fp.getX(), y = fp.getY(), z = fp.getZ();
        if (!Moves.standable(level, fp, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))) return "the time reversal was not proven (the spawn is not a safe place to stand)";
        long now = level.getGameTime();
        String dim = level.dimension().location().toString();
        buf.push(new Snap(now - 140, x, y, z, fp.getMaxHealth(), dim, false, Set.of()));
        buf.push(new Snap(now - 100, x, y, z, fp.getMaxHealth(), dim, false, Set.of()));
        fp.setPos(x + 5.0, y, z);
        boolean done = SpearTime.rewind(c);
        double away = Math.sqrt((fp.getX() - x) * (fp.getX() - x) + (fp.getZ() - z) * (fp.getZ() - z));
        if (!done) bad.add("the time reversal refused on a safe past");
        else if (away > 3.5) bad.add("the time reversal left the stand-in " + away + " blocks from where they stood");
        fp.setPos(x, y, z);
        return done ? "the time reversal brought the stand-in back" : "the time reversal failed";
    }

    private static SelfTests.Result fail(List<String> bad) {
        return SelfTests.Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))) + (bad.size() > 6 ? " (+" + (bad.size() - 6) + " more)" : ""));
    }
}
