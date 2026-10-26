package com.newuniverse.nusmp.entity.veldanava.skills;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.SummonerSkill;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The CI server smoke test of Solomon (prefix {@code solomon} in SMOKE_EXPECT): {@code solomon.rules} (the mode table, the numbers, the pure helpers and the caps), {@code solomon.skill} (registered, learned once, ten
 * modes with the right names, the mask carries Solomon's code and only Solomon's passives read it, icon and lang are in the jar) and {@code solomon.conditions} (Infinite Grimoire's modifiers and chant, the
 * Realm's registry and field, the hazard and foresight handlers, Mana Skin without a pool, the cast log and the copy, the sweep of Convergence and Erasure, the pillars landing and the terrain budget). They run
 * on the throw-away world and clean up after themselves.
 */
public final class SolomonSelfTests {
    private SolomonSelfTests() {}

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("solomon.rules", SolomonSelfTests::rules);
        SelfTests.register("solomon.skill", SolomonSelfTests::skill);
        SelfTests.register("solomon.conditions", SolomonSelfTests::conditions);
    }

    private static Result rules(MinecraftServer server) {
        JudgmentRules.Spec s = SolomonRules.SOLOMON;
        if (s.count() != JudgmentRules.MODES) return Result.fail("expected " + JudgmentRules.MODES + " modes, found " + s.count());
        int toggles = 0, actives = 0, held = 0;
        for (JudgmentRules.Mode m : s.modes()) {
            if (m.index() != s.modes().indexOf(m)) return Result.fail("mode " + m.id() + " has the wrong index");
            if (m.toggle()) toggles++;
            else if (m.kind() == JudgmentRules.Kind.HELD) held++;
            else actives++;
            if (!m.toggle() && m.masteredCost() > m.cost()) return Result.fail(m.id() + " costs more once mastered");
            if (!m.toggle() && (m.cost() <= 0 || m.cooldownSeconds() <= 0)) return Result.fail(m.id() + " is free or has no cooldown");
            if (!m.toggle() && m.masteredCooldownSeconds() > m.cooldownSeconds()) return Result.fail(m.id() + " cools down slower once mastered");
        }
        if (toggles != 4 || held != 1 || actives != 5) return Result.fail("the shape is " + toggles + "/" + actives + "/" + held + " (expected 4/5/1 with Realm of Magic held)");
        int[][] costs = {{4, 4500, 3000}, {5, 2000, 1200}, {6, 2500, 1500}, {7, 12000, 8500}, {8, 15000, 10000}, {9, 80000, 80000}};
        for (int[] c : costs)
            if (s.mode(c[0]).cost() != c[1] || s.mode(c[0]).masteredCost() != c[2]) return Result.fail(s.mode(c[0]).id() + " does not cost " + c[1] + "/" + c[2]);
        if (s.mode(5).kind() != JudgmentRules.Kind.CROUCH_PRESS || s.mode(8).kind() != JudgmentRules.Kind.CROUCH_PRESS) return Result.fail("Convergence and Erasure are not crouch + press");
        if (s.mode(6).kind() != JudgmentRules.Kind.HELD || !s.mode(6).perSecond()) return Result.fail("Realm of Magic is not the held mode paid per second");
        if (s.mode(9).cooldownSeconds() != 240 || s.mode(9).kind() != JudgmentRules.Kind.PRESS) return Result.fail("The 72 Pillars is not a press with a 240 s cooldown");
        if (JudgmentRules.code("solomon") != SolomonRules.CODE || JudgmentRules.spec("solomon") != s || !JudgmentRules.claimable("solomon")) return Result.fail("Solomon is not claimable code 4 of the spec list");

        // pure helpers
        SolomonRules.Skin none = SolomonRules.manaSkin(5f, 20f, 0f, 1000.0), lethal = SolomonRules.manaSkin(30f, 20f, 0f, 1000.0), part = SolomonRules.manaSkin(30f, 20f, 0f, 100.0), dry = SolomonRules.manaSkin(30f, 20f, 0f, 0.0);
        if (none.cost() != 0.0 || none.damage() != 5f) return Result.fail("Mana Skin touched a blow that does not kill");
        if (lethal.cost() != 300.0 || lethal.damage() != 0f) return Result.fail("a lethal blow is not paid 10 magicules a point");
        if (part.cost() != 100.0 || part.damage() != 20f) return Result.fail("a pool too small does not pay what it can");
        if (dry.cost() != 0.0 || dry.damage() != 30f) return Result.fail("Mana Skin acted with no pool");
        if (!SolomonRules.hazard("in_fire") || !SolomonRules.hazard("lava") || !SolomonRules.hazard("freeze") || SolomonRules.hazard("mob_attack") || SolomonRules.hazard("fall") || SolomonRules.hazard(null)) return Result.fail("the hazard words are wrong");
        if (!SolomonRules.elementalProjectile("small_fireball") || !SolomonRules.elementalProjectile("lightning_bolt") || SolomonRules.elementalProjectile("arrow") || SolomonRules.elementalProjectile("snowball") || SolomonRules.elementalProjectile(null)) return Result.fail("the projectile words are wrong");
        if (SolomonRules.absorbGain(100000.0) != SolomonRules.ABSORB_CAP || SolomonRules.absorbGain(1000.0) != 10.0 || SolomonRules.absorbGain(-5.0) != 0.0) return Result.fail("the absorption is not capped");
        if (SolomonRules.convergenceGain(1000, 0, 0, 10000.0) != 2500.0 || SolomonRules.convergenceGain(2, 1, 1, 1.0E6) != 2.0 * 250.0 + 400.0 + 1500.0 || SolomonRules.convergenceGain(-3, 0, 0, 100.0) != 0.0) return Result.fail("the convergence gain is wrong");
        if (SolomonRules.chantTicks(40, false, false, false) != 40 || SolomonRules.chantTicks(40, true, false, false) != 20 || SolomonRules.chantTicks(40, true, true, false) != 14 || SolomonRules.chantTicks(40, true, true, true) != 1 || SolomonRules.chantTicks(3, true, true, false) != 2 || SolomonRules.chantTicks(1, true, true, false) != 1) return Result.fail("the chant numbers are wrong");
        if (SolomonRules.spellCost(100.0, false, false) != 100.0 || SolomonRules.spellCost(100.0, true, false) != 50.0 || SolomonRules.spellCost(100.0, true, true) != 0.0) return Result.fail("the spell price is wrong");

        // the pillars: the 72 are shared out whole, land inside the span, spread inside the radius and the seed carries the count
        for (int targets = 1; targets <= SolomonRules.PILLAR_TARGETS_MAX; targets++) {
            int sum = 0;
            for (int k = 0; k < targets; k++) sum += SolomonRules.pillarsOnTarget(k, targets);
            if (sum != SolomonRules.PILLARS) return Result.fail(targets + " targets get " + sum + " pillars, not 72");
        }
        if (SolomonRules.pillarsOnTarget(8, 8) != 0 || SolomonRules.pillarsOnTarget(-1, 3) != 0) return Result.fail("a target that is not marked gets pillars");
        int last = -1;
        for (int k = 0; k < 72; k++) {
            int t = SolomonRules.pillarSpawnTick(k, 72);
            if (t < last || t >= SolomonRules.PILLAR_SPAN_TICKS) return Result.fail("pillar " + k + " does not fall inside the span in order");
            last = t;
            double[] o = SolomonRules.pillarOffset(0xABCDEFL, k, SolomonRules.PILLAR_SPREAD);
            if (Math.hypot(o[0], o[1]) > SolomonRules.PILLAR_SPREAD + 1.0E-9) return Result.fail("pillar " + k + " lands outside the spread");
            if (SolomonRules.pillarElement(77L, k) < 0 || SolomonRules.pillarElement(77L, k) >= SolomonRules.PILLAR_ELEMENTS) return Result.fail("a pillar has no element");
        }
        long seed = SolomonRules.packSeed(0x123456789L, 18);
        if (SolomonRules.pillarsOfSeed(seed) != 18 || SolomonRules.pillarOffset(seed, 5, 6.0)[0] != SolomonRules.pillarOffset(seed, 5, 6.0)[0] || SolomonRules.pillarOffset(seed, 4, 6.0)[0] != 0.0) return Result.fail("the seed does not carry the count or the offsets are not stable");
        if (SolomonRules.pillarDamage(0) != SolomonRules.PILLAR_DAMAGE || SolomonRules.pillarDamage(SolomonRules.PILLAR_MAX_HITS) != 0.0) return Result.fail("a creature can be hit by too many pillars");
        if (SolomonRules.PILLAR_SPAN_TICKS / SolomonRules.PILLAR_PULSE_TICKS > 40 || SolomonRules.PILLAR_TARGETS_MAX > 8 || SolomonRules.TERRAIN_MAX_BLOCKS > 1000 || SolomonRules.PILLAR_IMPACT_MAX_CREATURES > 24
                || SolomonRules.ERASURE_MAX_PROJECTILES > 128 || SolomonRules.CONVERGENCE_MAX_PROJECTILES > 128 || SolomonRules.LOG_SIZE > 128 || SolomonRules.REALM_MAX_ENEMIES > 48
                || SolomonRules.REALM_MAX_TICKS > JudgmentFields.MAX_TICKS || SolomonRules.PRESENCE_MAX > 16) return Result.fail("a per-press cap is not small");
        Object[] split = SolomonCasts.split("a:b|7");
        if (split == null || !"a:b".equals(split[0]) || (Integer) split[1] != 7 || SolomonCasts.split("nobar") != null || SolomonCasts.split("a|x") != null || SolomonCasts.split(null) != null) return Result.fail("a stored spell is not read back");
        return Result.ok("10 modes (4/5/1), costs, cooldowns, Mana Skin, hazards, absorption, chant, price, the 72 pillars' sharing, timing, spread and the caps hold");
    }

    private static Result skill(MinecraftServer server) throws Exception {
        ServerLevel sl = server.overworld();
        BlockPos at = sl.getSharedSpawnPos().above(3);
        FakePlayer p = SelfTests.standIn(sl, "solomon_skill", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        VeldanavaSkills.revoke(p);
        try {
            var holder = VeldanavaSkills.holder("solomon");
            if (holder == null || !(holder.get() instanceof SolomonSkill)) return Result.fail("Solomon is not registered");
            if (!VeldanavaSkills.grant(p, "solomon")) return Result.fail("Solomon could not be granted");
            Optional<ManasSkillInstance> inst = SkillAPI.getSkillsFrom(p).getSkill(holder.getId());
            if (inst.isEmpty()) return Result.fail("the granted skill is not on the player");
            ManasSkillInstance i = inst.get();
            if (!(i.getSkill() instanceof SolomonSkill sk)) return Result.fail("the instance is not a Solomon skill");
            if (sk.getModes(i) != JudgmentRules.MODES) return Result.fail("the skill reports " + sk.getModes(i) + " modes");
            for (JudgmentRules.Mode m : SolomonRules.SOLOMON.modes())
                if (!m.id().equals(sk.getModeId(i, m.index()))) return Result.fail("mode " + m.index() + " is '" + sk.getModeId(i, m.index()) + "' not '" + m.id() + "'");
            if (VeldanavaSkills.grant(p, "solomon")) return Result.fail("a second grant went through");
            if (VeldanavaSkills.grant(p, "jibril")) return Result.fail("a second skill was granted to the same player");

            JudgmentState.write(p, JudgmentState.compose(4, 0b1001, false));
            if (!JudgmentState.onFor(p, 4, 0) || JudgmentState.onFor(p, 4, 1) || !JudgmentState.onFor(p, 4, 3)) return Result.fail("Solomon's mask reads wrong");
            if (JudgmentState.on(p, 0) || JudgmentState.onFor(p, 3, 0) || JudgmentState.onFor(p, 3, 3)) return Result.fail("Solomon's toggles switched another skill's passives on");
            if (VeldanavaGuard.spatiallyFree(p) || VeldanavaGuard.mindImmune(p)) return Result.fail("Solomon's toggles switched Israfil's guards on");
            if (!SolomonHooks.antiMagicImmune(p)) return Result.fail("Infinite Grimoire does not make its owner immune");
            JudgmentState.write(p, JudgmentState.compose(3, 0b1001, false));
            if (SolomonHooks.antiMagicImmune(p)) return Result.fail("Jibril's toggles made someone immune to anti-magic");
            JudgmentState.write(p, 0);

            String icon = "assets/nusmp/textures/skill/veldanava/solomon.png";
            if (SolomonSelfTests.class.getClassLoader().getResource(icon) == null) return Result.fail("missing " + icon);
            JsonObject lang;
            try (InputStream in = SolomonSelfTests.class.getResourceAsStream("/assets/nusmp/lang/en_us.json")) {
                if (in == null) return Result.fail("no en_us.json");
                lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            for (String k : new String[]{"nusmp.skill.solomon", "nusmp.skill.solomon.description", "nusmp.skill.solomon.mastered"}) if (!lang.has(k)) return Result.fail("missing lang key " + k);
            for (JudgmentRules.Mode m : SolomonRules.SOLOMON.modes())
                if (!lang.has("nusmp.skill.mode.solomon." + m.id())) return Result.fail("missing lang key for mode " + m.id());
            return Result.ok("registered, learned once, 10 modes named, the mask keeps the skills apart, icon and lang are in the jar");
        } finally {
            JudgmentState.write(p, 0);
            VeldanavaSkills.revoke(p);
        }
    }

    private static Result conditions(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        BlockPos at = sl.getSharedSpawnPos().above(3);
        FakePlayer p = SelfTests.standIn(sl, "solomon_cond", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        Mob z = EntityType.ZOMBIE.create(sl);
        Mob summon = EntityType.ZOMBIE.create(sl);
        Arrow foeArrow = new Arrow(EntityType.ARROW, sl), ownArrow = new Arrow(EntityType.ARROW, sl);
        if (z == null || summon == null) return Result.fail("could not make a test creature");
        z.moveTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5);
        sl.addFreshEntity(z);
        Vec3 center = new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        boolean griefing = sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        BlockPos platform = new BlockPos(at.getX(), sl.getMaxBuildHeight() - 24, at.getZ());
        try {
            // Infinite Grimoire: the two modifiers come and go, the chant is shorter and a mastered one makes spells half price
            SolomonState.applyMagic(p, true, false);
            if (!SolomonState.hasMagicModifier(p)) return Result.fail("Infinite Grimoire gave no modifier");
            SolomonState.applyMagic(p, true, true);
            SolomonState.applyMagic(p, false, false);
            if (SolomonState.hasMagicModifier(p)) return Result.fail("Infinite Grimoire left its modifier behind");
            if (SolomonHooks.chantTicks(p, 40) != 40 || SolomonHooks.spellCost(p, 100.0) != 100.0) return Result.fail("someone without the skill had their chant or price changed");
            JudgmentState.write(p, JudgmentState.compose(4, 0b0001, false));
            if (SolomonHooks.chantTicks(p, 40) != 20 || SolomonHooks.spellCost(p, 100.0) != 100.0) return Result.fail("Infinite Grimoire's chant or price is wrong");
            JudgmentState.write(p, JudgmentState.compose(4, 0b0001, true));
            if (SolomonHooks.chantTicks(p, 40) != 14 || SolomonHooks.spellCost(p, 100.0) != 50.0) return Result.fail("a mastered Infinite Grimoire's chant or price is wrong");
            JudgmentState.write(p, 0);

            // the Realm: a standing one covers its owner, the chant is one tick, the spells are free; its field ends it
            SolomonState.realm(p);
            if (SolomonState.realmCount() != 1 || !SolomonState.realmCovers(p) || SolomonHooks.chantTicks(p, 40) != 1 || SolomonHooks.spellCost(p, 100.0) != 0.0) return Result.fail("the Realm does not free the chant and the price of its owner");
            if (SolomonState.realmCovers(z)) return Result.fail("the Realm covers an enemy");
            SolomonState.endRealm(p.getUUID());
            if (SolomonState.realmCovers(p)) return Result.fail("an ended Realm still covers");
            JudgmentFields.clearAll();
            JudgmentFields.add(p, sl, p, center, SolomonRules.REALM_RADIUS, 100000, new SolomonSkill.Realm(0.0), SolomonSkill.REALM_KIND);
            JudgmentFields.Field rf = JudgmentFields.find(p.getUUID(), SolomonSkill.REALM_KIND);
            if (rf == null || rf.endsAt - sl.getGameTime() > JudgmentFields.MAX_TICKS + 1) return Result.fail("the Realm is not a capped field");
            SolomonState.realm(p);
            if (JudgmentFields.clearKind(server, p.getUUID(), SolomonSkill.REALM_KIND) != 1 || SolomonState.realmCount() != 0) return Result.fail("ending the Realm left the record standing");

            // hazards and foresight on the blow about to land (the handlers are driven directly: a stand-in player is invulnerable to a real hit)
            JudgmentState.write(p, JudgmentState.compose(4, 0b1000, false));
            var fire = new LivingIncomingDamageEvent(p, new DamageContainer(sl.damageSources().inFire(), 4f));
            SolomonEvents.onIncoming(fire);
            var plain = new LivingIncomingDamageEvent(p, new DamageContainer(sl.damageSources().mobAttack(z), 4f));
            SolomonEvents.onIncoming(plain);
            JudgmentState.write(p, 0);
            if (!fire.isCanceled()) return Result.fail("Elemental Sovereignty let a flame through");
            if (plain.isCanceled()) return Result.fail("Elemental Sovereignty stopped an ordinary blow");
            JudgmentState.write(p, JudgmentState.compose(4, 0b0010, false));
            var spell = new LivingIncomingDamageEvent(p, new DamageContainer(sl.damageSources().indirectMagic(z, z), 8f));
            SolomonEvents.onIncoming(spell);
            JudgmentState.write(p, 0);
            if (spell.isCanceled() || Math.abs(spell.getAmount() - 6f) > 1.0E-4f) return Result.fail("the foreseen share of a spell is wrong (" + spell.getAmount() + ")");
            JudgmentState.write(p, JudgmentState.compose(4, 0b0100, false));
            var pre = new LivingDamageEvent.Pre(p, new DamageContainer(sl.damageSources().mobAttack(z), 50f));
            SolomonEvents.onDamagePre(pre);
            JudgmentState.write(p, 0);
            if (pre.getNewDamage() != 50f && magiculeIsZero(p)) return Result.fail("Mana Skin changed a blow with no pool behind it");
            JudgmentState.write(p, JudgmentState.compose(4, 0b1000, false));
            p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            boolean poisoned = p.hasEffect(MobEffects.POISON);
            JudgmentState.write(p, 0);
            p.removeAllEffects();
            if (poisoned) return Result.fail("poison took hold of an owner of Elemental Sovereignty");

            // the absorption counter and the foresight message gap are bounded
            int taken = 0;
            for (int k = 0; k < 30; k++) if (SolomonState.takeAbsorb(p)) taken++;
            if (taken != SolomonRules.ABSORB_PER_SECOND) return Result.fail("a wearer absorbed " + taken + " projectiles in a second");
            p.getPersistentData().remove(SolomonState.LAST_FORESIGHT);
            if (!SolomonState.foresightMessage(p) || SolomonState.foresightMessage(p)) return Result.fail("the foresight message is not spaced");
            p.getPersistentData().remove(SolomonState.LAST_FORESIGHT);

            // the cast log: bounded, per caster bounded, and a copy needs a spell that can be replayed
            SolomonCasts.clearAll();
            UUID other = UUID.randomUUID();
            GrimoireBook book = null;
            for (ManasSkill sk : SkillAPI.getSkillRegistry()) {
                if (sk instanceof GrimoireBook b && b.getRegistryName() != null && b.page(0) != null && b.page(0).weaponTag() == null) { book = b; break; }
            }
            if (book == null) return Result.fail("no grimoire is registered to test the copy with");
            String spellId = SolomonCasts.spell(book.getRegistryName().toString(), 0);
            for (int k = 0; k < 100; k++) SolomonCasts.add(other, spellId, sl.dimension(), center.add(5.0, 0.0, 0.0), sl.getGameTime());
            if (SolomonCasts.logSize() != SolomonRules.LOG_SIZE || SolomonCasts.lastOf(other).size() != SolomonRules.OWN_LOG) return Result.fail("the cast log is not bounded");
            if (!SolomonCasts.replayable(spellId) || SolomonCasts.replayable("nusmp:nothing|0") || SolomonCasts.replayable(spellId + "9999")) return Result.fail("the replayable test is wrong");
            SolomonCasts.Entry found = SolomonCasts.findToCopy(p);
            if (found == null || !spellId.equals(found.spell())) return Result.fail("the copy did not find the spell cast nearby");
            SolomonCasts.add(p.getUUID(), spellId, sl.dimension(), center, sl.getGameTime());
            SolomonCasts.clearAll();
            SolomonCasts.add(other, spellId, sl.dimension(), center.add(500.0, 0.0, 0.0), sl.getGameTime());
            if (SolomonCasts.findToCopy(p) != null) return Result.fail("the copy reached a spell cast 500 blocks away");
            SolomonCasts.clearAll();
            SolomonCasts.storeCopy(p, spellId);
            if (!SolomonCasts.hasCopy(p) || !spellId.equals(SolomonCasts.copy(p))) return Result.fail("the copy was not stored");
            SolomonCasts.dropCopy(p);
            if (SolomonCasts.hasCopy(p)) return Result.fail("the copy was not dropped");

            // the sweep: the enemy's arrow and summon go, the caster's own arrow stays, the casters are named
            foeArrow.setOwner(z);
            foeArrow.moveTo(center.x + 6.0, center.y + 1.0, center.z);
            ownArrow.setOwner(p);
            ownArrow.moveTo(center.x - 6.0, center.y + 1.0, center.z);
            sl.addFreshEntity(foeArrow);
            sl.addFreshEntity(ownArrow);
            summon.moveTo(center.x, center.y, center.z + 6.0);
            summon.addTag(SummonerSkill.SUMMON_TAG);
            summon.getPersistentData().putUUID(SummonerSkill.OWNER_KEY, UUID.randomUUID());
            sl.addFreshEntity(summon);
            if (SolomonSweep.summonOwner(summon) == null || SolomonSweep.summonOwner(z) != null) return Result.fail("a summon is not told from a creature");
            SolomonSweep.Result swept = SolomonSweep.sweep(p, SolomonRules.CONVERGENCE_RADIUS, 10, 10, 0, false);
            if (swept.projectiles() != 1 || foeArrow.isAlive() || !ownArrow.isAlive() || !summon.isAlive()) return Result.fail("the convergence took " + swept.projectiles() + " (own arrow alive " + ownArrow.isAlive() + ", summon alive " + summon.isAlive() + ")");
            if (!swept.casters().contains(z)) return Result.fail("the convergence did not name the caster of the arrow");
            SolomonSweep.Result erased = SolomonSweep.sweep(p, SolomonRules.ERASURE_RADIUS, 10, 10, 10, true);
            if (erased.summons() != 1 || summon.isAlive() || !ownArrow.isAlive()) return Result.fail("the erasure took " + erased.summons() + " summons (own arrow alive " + ownArrow.isAlive() + ")");

            // the terrain: a platform in the sky is dug within the budget, never when mobGriefing is off
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int dy = 0; dy >= -2; dy--) sl.setBlock(platform.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 2);
            sl.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, server);
            int dug = SolomonTerrain.crater(sl, platform, SolomonRules.TERRAIN_PER_PILLAR);
            if (dug <= 0 || dug > SolomonRules.TERRAIN_PER_PILLAR) return Result.fail("a crater removed " + dug + " blocks (budget " + SolomonRules.TERRAIN_PER_PILLAR + ")");
            sl.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
            if (SolomonTerrain.crater(sl, platform, SolomonRules.TERRAIN_PER_PILLAR) != 0 || SolomonTerrain.allowed(sl)) return Result.fail("terrain was dug with mobGriefing off");
            sl.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, server);
            if (SolomonTerrain.crater(sl, platform.offset(0, -10, 0), 5) != 0) return Result.fail("air was counted as dug");
            if (SolomonTerrain.surface(sl, center.x + 100000.0, center.z + 100000.0) != null) return Result.fail("terrain was looked for in a chunk that is not loaded");

            // the pillars: with every landing tick due, one pulse lands all of them, hurts the enemy and stays inside the budget
            sl.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
            z.setHealth(z.getMaxHealth());
            float before = z.getHealth();
            long seed = SolomonRules.packSeed(99L, 8);
            SolomonPillars pillars = new SolomonPillars(List.of(new SolomonPillars.Target(z.position(), 8, seed)), sl.getGameTime() - 300L);
            JudgmentFields.clearAll();
            JudgmentFields.add(p, sl, null, center, SolomonRules.PILLAR_RANGE, 200, pillars, SolomonPillars.KIND);
            JudgmentFields.Field pf = JudgmentFields.find(p.getUUID(), SolomonPillars.KIND);
            if (pf == null) return Result.fail("the pillars are not a field");
            pillars.pulse(p, sl, pf);
            if (pillars.impacts() != 8) return Result.fail("the pillars landed " + pillars.impacts() + " times, not 8");
            if (z.getHealth() >= before && z.isAlive()) return Result.fail("the pillars did no harm");
            if (pillars.terrainRemoved() != 0) return Result.fail("the pillars dug with mobGriefing off");
            pillars.pulse(p, sl, pf);
            if (pillars.impacts() != 8) return Result.fail("a pillar landed twice");
            if (JudgmentFields.clearKind(server, p.getUUID(), SolomonPillars.KIND) != 1) return Result.fail("the pillars' field could not be ended");

            // forgetting: the toggles' modifiers and registries go
            SolomonState.applyMagic(p, true, false);
            SolomonState.realm(p);
            SolomonState.clear(p);
            if (SolomonState.hasMagicModifier(p) || SolomonState.realmCount() != 0) return Result.fail("clearing the skill left a modifier or a realm behind");
            return Result.ok("modifiers, chant, price, Realm, hazards, foresight, Mana Skin, cast log, copy, sweep, terrain and pillars hold");
        } finally {
            JudgmentFields.clearAll();
            SolomonState.clearAll();
            SolomonState.clear(p);
            SolomonCasts.dropCopy(p);
            p.removeAllEffects();
            p.getPersistentData().remove(SolomonState.LAST_FORESIGHT);
            JudgmentState.write(p, 0);
            sl.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(griefing, server);
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int dy = 0; dy >= -2; dy--) sl.setBlock(platform.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
            z.discard();
            summon.discard();
            foeArrow.discard();
            ownArrow.discard();
            VeldanavaSkills.revoke(p);
        }
    }

    private static boolean magiculeIsZero(FakePlayer p) { return com.newuniverse.nusmp.avatar.TensuraBridge.magicule(p) <= 0.0; }
}
