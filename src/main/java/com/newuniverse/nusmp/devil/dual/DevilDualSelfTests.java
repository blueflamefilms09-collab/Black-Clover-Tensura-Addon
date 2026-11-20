package com.newuniverse.nusmp.devil.dual;

import com.newuniverse.nusmp.devil.DevilAttribute;
import com.newuniverse.nusmp.devil.DevilConfig;
import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.devil.DevilNet;
import com.newuniverse.nusmp.devil.DevilStore;
import com.newuniverse.nusmp.devil.DevilTier;
import com.newuniverse.nusmp.devil.Sacrifice;
import com.newuniverse.nusmp.devil.WorldDevil;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.skill.DevilArts;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;

import java.util.UUID;

/**
 * Why: nothing of Double Magic can be seen by its author in a game, so the real game checks it: the CI server smoke test boots the mod and runs these (prefix {@code devil.double}, docs/devil_double_magic.md).
 * <ul>
 * <li>{@code rules}: the numbers and tables of the pure classes, every devil of the roster has a rank, an attribute that acts and an art that does something, and the time origins.</li>
 * <li>{@code arts}: one art of every devil attribute is fired on the server at a zombie ahead of a stand-in (the double art with a grimoire hit too): the zombie is hurt by a blow of the devil_magic type and the
 * effect the attribute promises is on it and marked as devil magic.</li>
 * <li>{@code holders}: the holder table (claim, death lapse, absence lapse, reservation, duels, the save round trip).</li>
 * <li>{@code grant}: a contract gives Devil Magic and Devil Bond exactly once, a second sync changes nothing, and the end of the contract takes both.</li>
 * <li>{@code immunity}: a blow of the devil_magic type is devil magic, Adaptability is told not to adapt to it and law domination not to act on it; a plain blow is neither.</li>
 * <li>{@code corruption}: the curve (grows idle and overused, eases with kills, halves at a death, never above the cap), the save round trip and the packet round trip.</li>
 * <li>{@code ritual}: the dark rule, the dark-magic list, the craftable contract (recipe, tag with a fallback, switch).</li>
 * <li>{@code icons}: every grimoire magic and every devil attribute has its emblem file, the still icons and the v2 look textures are in the jar, and the composer gives the same bounded frame twice.</li>
 * </ul>
 */
public final class DevilDualSelfTests {
    private DevilDualSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("devil.double.rules", DevilDualSelfTests::rules);
        SelfTests.register("devil.double.arts", DevilDualSelfTests::arts);
        SelfTests.register("devil.double.holders", DevilDualSelfTests::holders);
        SelfTests.register("devil.double.grant", DevilDualSelfTests::grant);
        SelfTests.register("devil.double.immunity", DevilDualSelfTests::immunity);
        SelfTests.register("devil.double.corruption", DevilDualSelfTests::corruption);
        SelfTests.register("devil.double.ritual", DevilDualSelfTests::ritual);
        SelfTests.register("devil.double.icons", DevilDualSelfTests::icons);
        SelfTests.register("devil.double.mastery", DevilDualSelfTests::mastery);                                  // Devil Awakening (docs/devil_mastery.md)
    }

    // ---------------------------------------------------------------- Devil Awakening: the kit table, the mastery and every art of every kit
    private static Result mastery(MinecraftServer server) {
        var table = com.newuniverse.nusmp.devil.mastery.DevilKit.table();
        var problems = com.newuniverse.nusmp.devil.mastery.DevilKit.problems(table);
        if (!problems.isEmpty()) return Result.fail("devil_kits.json: " + problems.get(0) + (problems.size() > 1 ? " (+" + (problems.size() - 1) + " more)" : ""));
        if (table.kits().size() != WorldDevil.count()) return Result.fail("the kit table has " + table.kits().size() + " kits for " + WorldDevil.count() + " devils");
        DevilContract c = new DevilContract();
        c.tier = DevilTier.HIGH;
        c.mastery = 37.5f;
        c.masteryDay = 20000;
        c.masteryUsed = 1234f;
        c.awakenReadyAt = 999L;
        c.masteryStash.put(WorldDevil.at(0).id(), 22f);
        DevilContract back = DevilContract.fromTag(DevilContract.toTag(c));
        if (back.mastery != 37.5f || back.masteryDay != 20000 || back.masteryUsed != 1234f || back.awakenReadyAt != 999L || back.masteryStash.get(WorldDevil.at(0).id()) != 22f) return Result.fail("the mastery did not survive its tag");
        if (!new DevilContract().isEmpty() || DevilContract.fromTag(DevilContract.toTag(new DevilContract())).mastery != -1f) return Result.fail("a fresh contract is not empty or its mastery is not unset");
        DevilContract fresh = new DevilContract();
        fresh.tier = DevilTier.LOW;
        float want = com.newuniverse.nusmp.devil.mastery.Mastery.on() && !DevilConfig.legacyAssimilation() ? DevilConfig.masteryStart() : 100f;
        if (com.newuniverse.nusmp.devil.mastery.Mastery.ceiling(fresh) != want) return Result.fail("the ceiling of a fresh contract is " + com.newuniverse.nusmp.devil.mastery.Mastery.ceiling(fresh) + ", not " + want);
        ServerLevel level = server.overworld();
        double y = level.getMaxBuildHeight() - 60;
        ServerPlayer p = SelfTests.standIn(level, "devil_double_mastery", 0.5, y, 0.5);
        p.setYRot(0f);
        p.setXRot(0f);
        int cast = 0;
        try {
            DevilContract pc = DevilStore.of(p);
            pc.tier = DevilTier.HIGH;
            for (var kit : table.kits().values()) {
                WorldDevil d = WorldDevil.byId(kit.devil());
                pc.attribute = d.attribute();
                pc.devil = d.id();
                for (var art : kit.arts()) {
                    Zombie z = EntityType.ZOMBIE.create(level);
                    if (z == null) return Result.fail("no zombie");
                    z.setNoAi(true);
                    z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000.0);
                    z.setHealth(2000f);
                    z.setPos(0.5, y, 2.0);
                    level.addFreshEntity(z);
                    try {
                        int hits = com.newuniverse.nusmp.skill.DevilKitArts.cast(p, pc, art, 100f);
                        if (art.kind().equals("ward") ? hits != 0 : hits > 8) return Result.fail(kit.devil() + " " + art.id() + " hit " + hits + " foes");
                        if (art.kind().equals("strike") && art.range() >= 3 && hits != 1) return Result.fail(kit.devil() + " " + art.id() + " (strike) missed the zombie ahead");
                        if ((art.kind().equals("strike") || art.kind().equals("burst")) && hits == 1 && z.getHealth() >= 2000f) return Result.fail(kit.devil() + " " + art.id() + " did no damage");
                        cast++;
                    } finally {
                        z.discard();
                        p.removeAllEffects();
                    }
                }
            }
            return Result.ok("devil mastery: " + table.kits().size() + " kits valid, the tag round trip, the ceiling, " + cast + " arts of every kit fired bounded on a stand-in");
        } finally {
            DevilContract pc = DevilStore.of(p);
            pc.tier = DevilTier.NONE;
            pc.attribute = DevilAttribute.NONE;
            pc.devil = "";
            p.getPersistentData().remove("nusmp_devil_fx");
        }
    }

    // ---------------------------------------------------------------- rules
    private static Result rules(MinecraftServer server) {
        for (DevilTier t : DevilTier.values()) {
            if (t == DevilTier.NONE) continue;
            if (DualRules.baseDamage(t) <= 0 || DualRules.cost(t, false) <= 0 || DualRules.cost(t, true) <= DualRules.cost(t, false)) return Result.fail(t + " has no damage or no price");
            if (DevilRanks.gradeOf(t) == Grade.NORMAL) return Result.fail(t + " has no grade");
            if (DualRules.damage(t, 3.0, true, 10.0) > DualRules.MAX_RAW) return Result.fail(t + " passed the raw damage ceiling");
        }
        if (DevilRanks.gradeOf(DevilTier.LOW) != Grade.RARE || DevilRanks.gradeOf(DevilTier.MID) != Grade.UNIQUE || DevilRanks.gradeOf(DevilTier.HIGH) != Grade.LEGEND || DevilRanks.gradeOf(DevilTier.SUPREME) != Grade.GOD) return Result.fail("the tier to grade map moved");
        java.util.HashSet<String> places = new java.util.HashSet<>();
        for (int i = 0; i < WorldDevil.count(); i++) {
            WorldDevil d = WorldDevil.at(i);
            com.newuniverse.nusmp.relics.Rankings.Rank r = DevilRanks.of(d);
            if (r.place() <= 0) return Result.fail(d.id() + " has no place in its grade (rankings.json devils)");
            if (!places.add(r.grade() + "#" + r.place())) return Result.fail(d.id() + " shares a place in " + r.grade());
            DualRules.Art art = DualRules.art(d.attribute());
            if (!art.active()) return Result.fail("the art of " + d.attribute() + " (" + d.id() + ") does nothing but damage");
            if (DualRules.asMagic(d.attribute()) == null) return Result.fail(d.attribute() + " has no grimoire name");
        }
        for (DevilAttribute a : DevilAttribute.values()) if (a != DevilAttribute.NONE && !DualRules.art(a).active()) return Result.fail("the art of " + a + " does nothing but damage");
        if (TimeOrigin.of(true, true) != TimeOrigin.BOTH || TimeOrigin.of(false, true) != TimeOrigin.DEVIL || TimeOrigin.of(true, false) != TimeOrigin.GRIMOIRE || TimeOrigin.of(false, false).isTime()) return Result.fail("the time origins moved");
        if (!DualRules.isTime(DevilAttribute.TIME) || DualRules.isTime(DevilAttribute.FLAME)) return Result.fail("only the devil's Time counts as Time");
        if (DualRules.verdict(true, false) != DualRules.Verdict.GRANT || DualRules.verdict(true, true) != DualRules.Verdict.KEEP || DualRules.verdict(false, true) != DualRules.Verdict.REVOKE || DualRules.verdict(false, false) != DualRules.Verdict.KEEP) return Result.fail("the grant verdict moved");
        return Result.ok("devil double rules: " + WorldDevil.count() + " devils ranked with distinct places, every attribute's art acts, tier grades, time origins, grant verdicts");
    }

    // ---------------------------------------------------------------- one art per attribute
    private static Result arts(MinecraftServer server) {
        ServerLevel level = server.overworld();
        double y = level.getMaxBuildHeight() - 60;
        ServerPlayer p = SelfTests.standIn(level, "devil_double_arts", 0.5, y, 0.5);
        p.setYRot(0f);
        p.setXRot(0f);
        int fired = 0;
        try {
            if (!DevilMagic.source(p).is(DevilMagic.DAMAGE_TAG)) return Result.fail("the devil_magic damage type is missing or not in its tag");
            for (DevilAttribute a : DevilAttribute.values()) {
                if (a == DevilAttribute.NONE) continue;
                Zombie z = EntityType.ZOMBIE.create(level);
                if (z == null) return Result.fail("no zombie");
                z.setNoAi(true);
                z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000.0);
                z.setHealth(2000f);
                z.setPos(0.5, y, 3.5);
                level.addFreshEntity(z);
                try {
                    int hits = DevilArts.fire(p, a, DevilTier.HIGH, false, "", 0.0, UltimateClass.DRAGON);
                    if (hits != 1) return Result.fail("the art of " + a + " hit " + hits + " foes, not the one ahead");
                    if (z.getHealth() >= 2000f) return Result.fail("the art of " + a + " did no damage");
                    DualRules.Art art = DualRules.art(a);
                    if (!art.effectId().isEmpty() && !DevilMagic.isDevilEffect(z, art.effectId())) return Result.fail("the effect " + art.effectId() + " of " + a + " was not marked as devil magic");
                    fired++;
                } finally {
                    z.discard();
                }
            }
            Zombie z = EntityType.ZOMBIE.create(level);
            z.setNoAi(true);
            z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000.0);
            z.setHealth(2000f);
            z.setPos(0.5, y, 3.5);
            level.addFreshEntity(z);
            try {
                if (DevilArts.fire(p, DevilAttribute.TIME, DevilTier.SUPREME, true, "BURN", 0.0, UltimateClass.DRAGON) != 1) return Result.fail("the double art hit no foe");
                if (z.getRemainingFireTicks() <= 0) return Result.fail("the double art did not add the grimoire magic's burn");
            } finally {
                z.discard();
            }
            if (DevilArts.fire(p, DevilAttribute.NONE, DevilTier.HIGH, false, "", 0.0, UltimateClass.DRAGON) != 0) return Result.fail("an art without an attribute fired");
            return Result.ok("devil arts: " + fired + " attributes each hurt a zombie with a devil_magic blow and put their marked effect on it; the double art adds the grimoire burn");
        } finally {
            p.getPersistentData().remove("nusmp_devil_fx");
        }
    }

    // ---------------------------------------------------------------- the table
    private static Result holders(MinecraftServer server) {
        UUID a = UUID.nameUUIDFromBytes("devil-double-a".getBytes()), b = UUID.nameUUIDFromBytes("devil-double-b".getBytes());
        HolderRules.Params on = new HolderRules.Params(true, HolderRules.graceTicks(72), HolderRules.absentMillis(30));
        DevilHolders h = new DevilHolders();
        h.claim("lucifero", a, 100L);
        if (!a.equals(h.holderOf("lucifero")) || !"lucifero".equals(h.devilOf(a))) return Result.fail("a claim was not kept");
        if (HolderRules.check(h.slot("lucifero"), b, 200L, 1_000L, on) != HolderRules.Verdict.TAKEN) return Result.fail("a held devil was free to a stranger");
        if (HolderRules.check(h.slot("lucifero"), a, 200L, 1_000L, on) != HolderRules.Verdict.SELF) return Result.fail("the holder was refused");
        h.claim("beelzebub", a, 150L);
        if (h.holderOf("lucifero") != null || !a.equals(h.holderOf("beelzebub"))) return Result.fail("a player held two devils");
        h.died(a, 300L);
        if (HolderRules.check(h.slot("beelzebub"), b, 300L + HolderRules.graceTicks(72) - 1, 1_000L, on) != HolderRules.Verdict.TAKEN) return Result.fail("the death lapse came early");
        if (HolderRules.check(h.slot("beelzebub"), b, 300L + HolderRules.graceTicks(72), 1_000L, on) != HolderRules.Verdict.LAPSED_DEATH) return Result.fail("the death lapse did not come");
        h.renewed(a);
        if (HolderRules.check(h.slot("beelzebub"), b, 300L + HolderRules.graceTicks(72), 1_000L, on) != HolderRules.Verdict.TAKEN) return Result.fail("letting the devil in did not stop the death lapse");
        h.seen(a, 1_000L);
        if (HolderRules.check(h.slot("beelzebub"), b, 0L, 1_000L + HolderRules.absentMillis(30), on) != HolderRules.Verdict.LAPSED_ABSENT) return Result.fail("the absence lapse did not come");
        h.online(a);
        if (HolderRules.check(h.slot("beelzebub"), b, 0L, 1_000L + HolderRules.absentMillis(30), on) != HolderRules.Verdict.TAKEN) return Result.fail("being online did not stop the absence lapse");
        if (HolderRules.check(h.slot("beelzebub"), b, 0L, 0L, new HolderRules.Params(false, 0, 0)) != HolderRules.Verdict.FREE) return Result.fail("the switch off did not free every devil");
        h.reserve("astaroth", b, 5_000L);
        if (HolderRules.check(h.slot("astaroth"), a, 100L, 0L, on) != HolderRules.Verdict.RESERVED_OTHER || HolderRules.check(h.slot("astaroth"), b, 100L, 0L, on) != HolderRules.Verdict.RESERVED_FOR_YOU) return Result.fail("a reservation does not belong to the victor");
        if (!h.openDuel(new DevilHolders.Duel(b, a, "beelzebub", 900L)) || h.openDuel(new DevilHolders.Duel(UUID.randomUUID(), a, "beelzebub", 900L))) return Result.fail("a holder was in two duels");
        if (!HolderRules.duelWon(a, b, a, b, 900L, 800L) || HolderRules.duelWon(a, b, a, b, 900L, 901L) || HolderRules.duelWon(a, b, a, UUID.randomUUID(), 900L, 800L)) return Result.fail("the duel's win rule moved");
        h.dropDuels(a, 0L);
        if (h.duelCount() != 0) return Result.fail("a logout did not drop the duel");
        DevilHolders back = DevilHolders.load(h.save(new CompoundTag(), server.registryAccess()), server.registryAccess());
        if (!a.equals(back.holderOf("beelzebub")) || back.slot("astaroth") == null || back.rowCount() != h.rowCount()) return Result.fail("the table did not survive its save");
        return Result.ok("holder table: claim, one devil per player, death lapse, absence lapse, reservation, duels (cap " + DevilHolders.MAX_DUELS + "), save round trip; oneOfOne=" + DualConfig.oneOfOne());
    }

    // ---------------------------------------------------------------- the grant
    private static Result grant(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.standIn(level, "devil_double_grant", 0.5, level.getMaxBuildHeight() - 40, 0.5);
        DevilContract c = DevilStore.of(p);
        try {
            if (!NUSkills.DEVIL_MAGIC.isBound() || !NUSkills.DEVIL_BOND.isBound()) return Result.fail("the skills are not registered");
            if (!(NUSkills.DEVIL_MAGIC.get().getType() == io.github.manasmods.tensura.ability.skill.Skill.SkillType.ULTIMATE)) return Result.fail("Devil Magic is not an Ultimate");
            if (!(NUSkills.DEVIL_BOND.get().getType() == io.github.manasmods.tensura.ability.skill.Skill.SkillType.INTRINSIC)) return Result.fail("Devil Bond is not intrinsic");
            if (!DevilConfig.enabled() || !DualConfig.ultimate()) return Result.ok("skipped: the devil system or the grant is switched off by the server config");
            c.tier = DevilTier.HIGH;
            c.attribute = DevilAttribute.FLAME;
            DualRules.Verdict v = DevilSkillGrant.sync(p, c);
            if (v == DualRules.Verdict.KEEP && !DevilSkillGrant.has(p)) return Result.ok("skipped: the stand-in has no skill storage");
            if (v != DualRules.Verdict.GRANT && !DevilSkillGrant.has(p)) return Result.fail("a contract did not give Devil Magic");
            if (!DevilSkillGrant.has(p)) return Result.fail("Devil Magic is not in the skill list after the grant");
            if (DualConfig.intrinsic() && !DevilSkillGrant.hasBond(p)) return Result.fail("Devil Bond is not in the skill list after the grant");
            if (DevilConfig.masteryOn() && !DevilSkillGrant.hasAwakening(p)) return Result.fail("Devil Awakening (extra skill) is not in the skill list after the grant");
            if (DevilSkillGrant.sync(p, c) != DualRules.Verdict.KEEP) return Result.fail("a second sync granted again");
            c.tier = DevilTier.NONE;
            c.attribute = DevilAttribute.NONE;
            if (DevilSkillGrant.sync(p, c) != DualRules.Verdict.REVOKE || DevilSkillGrant.has(p) || DevilSkillGrant.hasBond(p) || DevilSkillGrant.hasAwakening(p)) return Result.fail("the end of the contract did not take the skills");
            if (DevilSkillGrant.sync(p, c) != DualRules.Verdict.KEEP) return Result.fail("a sync with no contract changed something");
            return Result.ok("devil skills: a contract gives Devil Magic (Ultimate) and Devil Bond (intrinsic) once, a second sync keeps, the end takes both");
        } finally {
            c.tier = DevilTier.NONE;
            c.attribute = DevilAttribute.NONE;
            DevilSkillGrant.sync(p, c);
        }
    }

    // ---------------------------------------------------------------- the immunities
    private static Result immunity(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.standIn(level, "devil_double_immunity", 0.5, level.getMaxBuildHeight() - 40, 0.5);
        DamageSource art = DevilMagic.source(p), plain = level.damageSources().generic();
        if (!DevilMagic.isDevilMagic(art) || DevilMagic.isDevilMagic(plain)) return Result.fail("the devil_magic damage type is not what marks devil magic");
        if (DualConfig.immuneAdaptability() && (!DevilMagic.adaptationBlocked(art) || DevilMagic.adaptationBlocked(plain))) return Result.fail("Adaptability is not kept from devil magic (or from plain blows)");
        if (DualConfig.immuneLaw() && (DevilMagic.lawDominationApplies(art) || !DevilMagic.lawDominationApplies(plain))) return Result.fail("law domination is not kept off devil magic (or off plain blows)");
        if (!DevilMagicRules.mayAdaptTo(true, false) || !DevilMagicRules.lawDominationApplies(true, false)) return Result.fail("the switch off did not restore the old behaviour");
        if (DevilMagicRules.mayAdaptTo(true, true) || DevilMagicRules.lawDominationApplies(true, true) || !DevilMagicRules.mayAdaptTo(false, true)) return Result.fail("the immunity rules moved");
        if (DevilMagicRules.hostCarries(true, true, 19.9f) || !DevilMagicRules.hostCarries(true, true, 20f) || DevilMagicRules.hostCarries(false, true, 100f)) return Result.fail("the host rule moved");
        Zombie z = EntityType.ZOMBIE.create(level);
        z.setPos(0.5, level.getMaxBuildHeight() - 40, 4.5);
        DevilMagic.markEffect(z, "minecraft:slowness", level.getGameTime() + 100L);
        boolean marked = DevilMagic.isDevilEffect(z, "minecraft:slowness"), other = DevilMagic.isDevilEffect(z, "minecraft:poison");
        if (!marked || other) return Result.fail("an effect was marked wrongly");
        if (DualConfig.immuneLaw() && DevilMagic.lawDominationApplies(z, "minecraft:slowness")) return Result.fail("law domination may act on a devil effect");
        return Result.ok("immunity: devil_magic blows are devil magic, Adaptability is kept from them and law domination off them, plain blows are neither, the switches restore the old rules");
    }

    // ---------------------------------------------------------------- corruption
    private static Result corruption(MinecraftServer server) {
        CorruptionRules.Params p = CorruptionRules.Params.DEFAULT;
        float idle = 0f;
        for (int i = 0; i < 60 * 60; i++) idle = CorruptionRules.step(idle, true, false, false, 100_000L, p);
        if (idle <= 0f || idle > p.cap() + 1e-3) return Result.fail("an hour of idleness gave " + idle);
        float used = 0f;
        for (int i = 0; i < 60 * 60 * 3; i++) used = CorruptionRules.step(used, true, true, true, 100_000L, p);
        if (Math.abs(used - p.cap()) > 1e-3) return Result.fail("overuse did not reach the cap: " + used);
        if (CorruptionRules.step(0f, true, false, false, 100L, p) != 0f) return Result.fail("the corruption grew right after a kill");
        if (CorruptionRules.onKill(500f, p) >= 500f || CorruptionRules.onKill(10f, p) != 0f) return Result.fail("a kill did not ease the corruption");
        if (CorruptionRules.onDeath(800f, p) != 400f) return Result.fail("a death did not halve the corruption");
        if (CorruptionRules.step(500f, false, true, true, 100_000L, p) >= 500f || CorruptionRules.step(500f, true, true, true, 0L, new CorruptionRules.Params(false, 20f, 40f, 80f, 40f, 0.5f, 850f, 60)) >= 500f) return Result.fail("no price or the switch off did not fade the corruption");
        for (int pct = 0; pct <= 100; pct++) {
            float cover = CorruptionRules.cover(pct, 1f);
            if (cover > 0.9f + 1e-6 || CorruptionRules.clearRadius(cover) < CorruptionRules.MIN_CLEAR - 1e-6 || CorruptionRules.ringAlpha(1f, cover) > 0.97f + 1e-6) return Result.fail("the picture left its bounds at " + pct + " percent");
        }
        if (CorruptionRules.cover(100, 0f) != 0f) return Result.fail("strength 0 still covered the screen");
        DevilContract c = new DevilContract();
        c.tier = DevilTier.HIGH;
        c.corruption = 432.5f;
        c.sacrifices.add(Sacrifice.LOST_EYE_RIGHT);
        if (DevilContract.fromTag(DevilContract.toTag(c)).corruption != 432.5f) return Result.fail("the corruption did not survive its tag");
        DevilNet.Sync s = new DevilNet.Sync(3, 4, 50, 1, 4, 10, 20, 43, 372, 120, 45);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DevilNet.Sync.STREAM_CODEC.encode(buf, s);
        if (!DevilNet.Sync.STREAM_CODEC.decode(buf).equals(s)) return Result.fail("the sync packet changed on the wire");
        FriendlyByteBuf corOnly = new FriendlyByteBuf(Unpooled.buffer());
        corOnly.writeByte(3); corOnly.writeByte(4); corOnly.writeByte(50); corOnly.writeByte(1); corOnly.writeByte(4); corOnly.writeVarInt(10); corOnly.writeVarInt(20); corOnly.writeByte(43);
        DevilNet.Sync read = DevilNet.Sync.STREAM_CODEC.decode(corOnly);
        if (read.corruption() != 43 || read.mastery() != 0 || read.awakeSec() != 0 || read.restSec() != 0) return Result.fail("a packet of the corruption shape did not read as no mastery");
        FriendlyByteBuf old = new FriendlyByteBuf(Unpooled.buffer());
        old.writeByte(3); old.writeByte(4); old.writeByte(50); old.writeByte(1); old.writeByte(4); old.writeVarInt(10); old.writeVarInt(20);
        if (DevilNet.Sync.STREAM_CODEC.decode(old).corruption() != 0) return Result.fail("a packet of the old shape did not read as no corruption");
        return Result.ok("corruption: idle and overuse grow it to the cap, kills ease it, a death halves it, no price fades it, the picture keeps a clear window, tag and packet round trips, old packets read");
    }

    // ---------------------------------------------------------------- the ritual and the craftable contract
    private static Result ritual(MinecraftServer server) {
        DarkRitualRules.Dark dark = DarkRitualRules.table();
        if (!dark.magics().contains("DARK") || !dark.magics().contains("CURSE") || dark.skills().isEmpty()) return Result.fail("the dark-magic list did not load");
        if (!DarkRitualRules.isDark(dark, "dark", null) || !DarkRitualRules.isDark(dark, null, "GHOST") || DarkRitualRules.isDark(dark, "FLAME", "FLAME")) return Result.fail("the dark-magic test is wrong");
        if (DarkRitualRules.allow(true, false, false, false, 15, 4) != DarkRitualRules.Why.NONE) return Result.fail("a ritual began in full light with no dark magic");
        if (DarkRitualRules.allow(true, true, false, false, 15, 4) != DarkRitualRules.Why.MAGIC || DarkRitualRules.allow(true, false, true, false, 15, 4) != DarkRitualRules.Why.DIMENSION
                || DarkRitualRules.allow(true, false, false, true, 15, 4) != DarkRitualRules.Why.BIOME || DarkRitualRules.allow(true, false, false, false, 4, 4) != DarkRitualRules.Why.LIGHT
                || DarkRitualRules.allow(false, false, false, false, 15, 4) != DarkRitualRules.Why.OFF) return Result.fail("the dark rule's grounds moved");
        if (DarkRitualRules.lightBroken(6, 4) || !DarkRitualRules.lightBroken(7, 4)) return Result.fail("the light break threshold moved");
        ServerLevel level = server.overworld();
        TagKey<Item> blood = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("anime_multiverse", "devil_contract_blood"));
        long n = BuiltInRegistries.ITEM.getTag(blood).map(set -> set.stream().count()).orElse(0L);
        if (n < 1) return Result.fail("the blood tag has no item, not even the fallback");
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "devil_contract");
        boolean there = server.getRecipeManager().byKey(id).isPresent();
        if (there != DevilConfig.contractCraftable()) return Result.fail("the recipe of the Devil Contract is " + (there ? "present" : "absent") + " but contractCraftable=" + DevilConfig.contractCraftable());
        if (there) {
            var holder = server.getRecipeManager().byKey(id).get();
            if (!holder.value().getResultItem(server.registryAccess()).is(com.newuniverse.nusmp.item.NUItems.DEVIL_CONTRACT.get())) return Result.fail("the recipe makes the wrong item");
        }
        if (ConfigCondition.value("nonsense") || ConfigCondition.value("devil.contractCraftable") != DevilConfig.contractCraftable()) return Result.fail("the data condition is wrong");
        return Result.ok("ritual: dark-magic list loaded (" + dark.magics().size() + " magics, " + dark.skills().size() + " skills), the dark rule's grounds, the blood tag has " + n + " item(s), the craftable recipe follows contractCraftable=" + DevilConfig.contractCraftable());
    }

    // ---------------------------------------------------------------- the emblems, the icons and the v2 look (files only: the server never draws them)
    private static boolean has(String path) { return DevilDualSelfTests.class.getResource("/assets/anime_multiverse/" + path) != null; }

    private static Result icons(MinecraftServer server) {
        int magics = 0, devils = 0;
        for (com.newuniverse.nusmp.blackclover.MagicType m : com.newuniverse.nusmp.blackclover.MagicType.values()) {
            String key = m.name().toLowerCase(java.util.Locale.ROOT);
            if (!com.newuniverse.nusmp.skill.icons.EmblemComposer.validKey(key) || !has(com.newuniverse.nusmp.skill.icons.EmblemComposer.magicPath(key))) return Result.fail("no emblem for the grimoire magic " + m);
            magics++;
        }
        for (DevilAttribute a : DevilAttribute.values()) {
            if (a == DevilAttribute.NONE) continue;
            if (!has(com.newuniverse.nusmp.skill.icons.EmblemComposer.devilPath(a.key()))) return Result.fail("no emblem for the devil attribute " + a);
            devils++;
        }
        for (String f : new String[] {"textures/skill/devil_magic.png", "textures/skill/devil_bond.png", "textures/entity/devil/horns2.png", "textures/entity/devil/wings2.png", "textures/entity/devil/wings2_glow.png",
                "textures/entity/devil/weg2_low.png", "textures/entity/devil/weg2_supreme_slim_glow.png", "textures/entity/devil/horns.png", "textures/entity/devil/weg_low.png"})
            if (!has(f)) return Result.fail("missing file " + f);
        int[] one = com.newuniverse.nusmp.skill.icons.EmblemComposer.frame(null, 0xFF0000, null, 0, 2, com.newuniverse.nusmp.skill.icons.EmblemComposer.FRAMES);
        if (one.length != 1024 || !java.util.Arrays.equals(one, com.newuniverse.nusmp.skill.icons.EmblemComposer.frame(null, 0xFF0000, null, 0, 2, com.newuniverse.nusmp.skill.icons.EmblemComposer.FRAMES))) return Result.fail("the composed frame is not 32 x 32 and repeatable");
        return Result.ok("icons: " + magics + " magic emblems, " + devils + " devil emblems, the still icons and the v2 look (and the old files) are in the jar; the composer is repeatable");
    }
}
