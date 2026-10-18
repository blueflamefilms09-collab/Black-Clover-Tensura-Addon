package com.newuniverse.nusmp.heroweapon.weapon.strike;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing of the hero weapons can be played by its author, so each weapon has a SELF-TEST that the CI server smoke test runs in the real game ({@code hero.weapon.<pkg>},
 * the smoke test expects the prefix {@code hero}). It checks, in order: the forms file the mod ships passes {@link FormsCheck} (read from the jar, so it works before the forms
 * system exists); the forms the game really loaded (once that system is merged) all name skills the weapon has; every skill is registered with the right ultimate flag, a name,
 * a text and a sane cooldown; and then a stand-in player is bonded to the weapon, given the summoned weapon in hand, and casts EVERY skill of EVERY form once beside a fresh dummy
 * zombie, then runs each hook of the logic once, all inside try/catch, asserting that nothing throws (a technique that refuses is fine). The stand-in releases the bond and every
 * dummy is removed at the end. Delayed parts of a technique find the bond gone and do nothing. One implementation for the four striking weapons keeps the four tests identical.
 */
public final class StrikeSelfTest {
    private StrikeSelfTest() {}

    /** Registers {@code hero.weapon.<pkg>} for a weapon; call it from the weapon logic's constructor. The dummy foe does not think. */
    public static void register(HeroKind kind, FormsCheck.Known known) { register(kind, known, false); }

    /**
     * The same, and {@code aware} lets the dummy foe keep its mind. The Whip needs it: its Command refuses a monster without AI (there is nothing to command in one), so with a
     * dummy that does not think the Command would only ever be tested on its way out.
     */
    public static void register(HeroKind kind, FormsCheck.Known known, boolean aware) {
        SelfTests.register("hero.weapon." + kind.pkg(), server -> run(server, kind, known, aware));
    }

    private static SelfTests.Result run(MinecraftServer server, HeroKind kind, FormsCheck.Known known, boolean aware) {
        if (!HeroLogics.has(kind)) return SelfTests.Result.fail(kind + " has no registered logic");
        List<String> bad = new ArrayList<>();
        shippedFile(kind, known, bad);
        List<HeroForm> forms = HeroForms.of(kind);
        loadedForms(forms, known, bad);
        registered(kind, known, bad);
        int[] casts = new int[2];                                   // cast, accepted
        castEverything(server, kind, forms, aware, bad, casts);
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
        return SelfTests.Result.ok(kind.display() + ": " + known.skillIds().size() + " skills, " + forms.size() + " forms loaded, " + casts[0] + " casts without an error (" + casts[1] + " accepted)");
    }

    // ------------------------------------------------------------------ the data
    private static void shippedFile(HeroKind kind, FormsCheck.Known known, List<String> bad) {
        String path = "/data/nusmp/hero/forms/" + kind.itemId() + ".json";
        try (InputStream in = StrikeSelfTest.class.getResourceAsStream(path)) {
            if (in == null) {
                bad.add("the forms file " + path + " is missing from the mod");
                return;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String p : FormsCheck.problems(root, known)) bad.add("forms file: " + p);
        } catch (IOException | RuntimeException e) {
            bad.add("the forms file " + path + " cannot be read: " + e);
        }
    }

    /** The forms the game loaded: while the forms system is still the stub there is only a synthetic base form, which is not an error. */
    private static void loadedForms(List<HeroForm> forms, FormsCheck.Known known, List<String> bad) {
        if (forms.isEmpty() || !HeroForm.BASE_ID.equals(forms.get(0).id())) bad.add("the loaded forms do not start with the base form");
        for (HeroForm f : forms) {
            int ultimates = 0;
            for (String id : f.skills().values()) {
                HeroSkill s = HeroLogics.skill(id);
                if (s == null) bad.add("form " + f.id() + " names the unknown skill " + id);
                else if (s.ultimate()) ultimates++;
            }
            if (ultimates > 1) bad.add("form " + f.id() + " lists " + ultimates + " ultimates");
            for (String p : f.passives()) if (!known.passives().contains(p)) bad.add("form " + f.id() + " has the passive " + p + ", which the logic does not implement");
        }
    }

    private static void registered(HeroKind kind, FormsCheck.Known known, List<String> bad) {
        String prefix = kind.name().toLowerCase(java.util.Locale.ROOT) + ".";
        for (String id : known.skillIds()) {
            HeroSkill s = HeroLogics.skill(id);
            if (s == null) {
                bad.add("the skill " + id + " is not registered");
                continue;
            }
            if (!id.startsWith(prefix)) bad.add(id + " must start with " + prefix);
            if (s.ultimate() != known.ultimateIds().contains(id)) bad.add(id + " has the wrong ultimate flag");
            if (s.name() == null || s.name().isBlank() || s.desc() == null || s.desc().isBlank()) bad.add(id + " has no name or text");
            if (s.cooldownTicks(null) < 40) bad.add(id + " has a cooldown under 40 ticks");
            if (s.magoiShare() < 0.0 || s.magoiShare() > 0.1) bad.add(id + " has a strange cost " + s.magoiShare());
        }
        for (HeroSkill s : HeroLogics.skillsOf(kind)) if (!known.skillIds().contains(s.id())) bad.add("the logic registers " + s.id() + ", which the numbers table does not know");
    }

    // ------------------------------------------------------------------ the casts
    private static void castEverything(MinecraftServer server, HeroKind kind, List<HeroForm> forms, boolean aware, List<String> bad, int[] count) {
        if (forms.isEmpty()) return;                                          // already reported by loadedForms
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        if (!level.hasChunkAt(spawn)) return;                                // nowhere loaded to stand: the data checks above still ran
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        ServerPlayer p = SelfTests.player(level, "hero_" + kind.pkg(), spawn.getX() + 0.5, y, spawn.getZ() + 0.5);
        p.setYRot(0f);
        p.setXRot(0f);
        p.setOnGround(true);
        String why = HeroBonds.bind(p, kind);
        if (why != null) {
            bad.add("the stand-in player could not bind the weapon: " + why);
            return;
        }
        HeroBond bond = HeroBonds.of(p);
        ItemStack stack = HeroItems.stack(kind);
        stack.set(HeroComponents.OWNER.get(), p.getUUID().toString());
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        bond.setSummoned(true);
        List<LivingEntity> spawned = new ArrayList<>();
        try {
            HeroLogic logic = HeroLogics.of(kind);
            List<Cast> plan = new ArrayList<>();                              // every skill under every form that lists it, and under the base form if no form does
            for (HeroForm f : forms) for (String id : f.skills().values()) {
                HeroSkill s = HeroLogics.skill(id);
                if (s != null) plan.add(new Cast(s, f));
            }
            for (HeroSkill s : HeroLogics.skillsOf(kind)) if (plan.stream().noneMatch(x -> x.skill() == s)) plan.add(new Cast(s, forms.get(0)));
            for (Cast cast : plan) {
                LivingEntity dummy = dummy(level, p, aware);
                spawned.add(dummy);
                p.setOnGround(true);
                HeroCtx c = new HeroCtx(p, bond, cast.form(), stack);
                try {
                    if (cast.skill().cast(c)) count[1]++;
                    count[0]++;
                } catch (Throwable t) {
                    bad.add(cast.skill().id() + " under " + cast.form().id() + " threw " + t);
                }
                dummy.discard();
            }
            HeroForm form = forms.get(0);
            LivingEntity dummy = dummy(level, p, aware);
            spawned.add(dummy);
            HeroCtx c = new HeroCtx(p, bond, form, stack);
            try {
                logic.onSummon(c);
                logic.onTick(c);
                logic.onHit(c, dummy, 4f, true);
                logic.onHit(c, dummy, 2f, false);
                logic.onDamaged(c, p.damageSources().mobAttack(dummy), 5f);
                logic.onBlocked(c, p.damageSources().mobAttack(dummy), 5f);
                logic.onKill(c, dummy);
                logic.onRelease(c, 20);
                logic.onFormChanged(c, form, form);
                logic.onDismiss(c);
            } catch (Throwable t) {
                bad.add("a hook of the logic threw " + t);
            }
            dummy.discard();
        } finally {
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            bond.setSummoned(false);
            HeroBonds.release(p);
            for (LivingEntity e : spawned) if (!e.isRemoved()) e.discard();
        }
    }

    /** One cast of the test: a skill under a form. */
    private record Cast(HeroSkill skill, HeroForm form) {}

    /** A zombie standing a few blocks in front of the stand-in player; it does not think unless {@code aware}. It is discarded right after the cast, before its mind ever runs. */
    private static LivingEntity dummy(ServerLevel level, ServerPlayer p, boolean aware) {
        Zombie z = EntityType.ZOMBIE.create(level);
        Vec3 f = Strike.flatLook(p);
        z.moveTo(p.getX() + f.x * 2.6, p.getY(), p.getZ() + f.z * 2.6, 0f, 0f);
        z.setNoAi(!aware);
        z.setOnGround(true);
        level.addFreshEntity(z);
        return z;
    }
}
