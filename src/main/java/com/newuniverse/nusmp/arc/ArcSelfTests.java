package com.newuniverse.nusmp.arc;

import com.mojang.serialization.DataResult;
import com.newuniverse.nusmp.arc.dialogue.DialogueBook;
import com.newuniverse.nusmp.arc.dialogue.DialogueContext;
import com.newuniverse.nusmp.arc.dialogue.DialogueLoader;
import com.newuniverse.nusmp.arc.dialogue.DialogueSituation;
import com.newuniverse.nusmp.arc.dialogue.TalkPlan;
import com.newuniverse.nusmp.arc.story.ArcStory;
import com.newuniverse.nusmp.arc.story.StoryAttachments;
import com.newuniverse.nusmp.arc.story.StoryData;
import com.newuniverse.nusmp.arc.story.StoryRules;
import com.newuniverse.nusmp.boss.BossBuffTable;
import com.newuniverse.nusmp.boss.BossEntities;
import com.newuniverse.nusmp.boss.BossRegistry;
import com.newuniverse.nusmp.boss.rules.BossSkillRules;
import com.newuniverse.nusmp.charter.CharterItems;
import com.newuniverse.nusmp.charter.CharterTable;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: nothing of the arcs can be played by its author, so each part has a SELF-TEST that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}, keys {@code arc.*}; the smoke script expects the prefix {@code arc}): the roster is consistent, every boss has an entity
 * type with attributes, every NPC has a dialogue file that parses and a greeting for a newcomer, the story gate and the stored story behave (with a
 * stand-in player), every boss with a grimoire has a Charter row, an item and real Tensura skills, the tier profiles obey the boss rules, and a real boss
 * and a real NPC can be made and are recognised by the boss registry, the Charter table and the buff engine. With an empty roster (before the characters are
 * merged) the tests still pass and say so, so the prefix is always reported. A test that spawns something removes it again.
 */
public final class ArcSelfTests {
    private ArcSelfTests() {}

    private static final String PLAYER = "arc_tester";

    public static void register() {
        SelfTests.register("arc.roster", ArcSelfTests::roster);
        SelfTests.register("arc.entities", ArcSelfTests::entities);
        SelfTests.register("arc.dialogue", ArcSelfTests::dialogue);
        SelfTests.register("arc.story", ArcSelfTests::story);
        SelfTests.register("arc.charter", ArcSelfTests::charter);
        SelfTests.register("arc.profiles", ArcSelfTests::profiles);
        SelfTests.register("arc.spawn", ArcSelfTests::spawn);
    }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static SelfTests.Result problems(List<String> p) {
        return fail(p.size() + " problem(s): " + String.join(" | ", p.subList(0, Math.min(6, p.size()))));
    }

    // ================================================================ roster
    private static SelfTests.Result roster(MinecraftServer server) {
        List<ArcCharacter> all = ArcRoster.all();
        List<String> bad = new ArrayList<>();
        for (ArcCharacter c : all) {
            for (String p : c.prerequisites()) if (ArcRoster.byId(p) == null) bad.add(c.id() + " follows '" + p + "', who is not in the roster");
            if (StoryRules.cyclic(c, ArcRoster::byId)) bad.add(c.id() + " is among its own prerequisites");
            if (c.isBoss() && c.floorMax() > 0 && c.floorMin() > c.floorMax()) bad.add(c.id() + " has floors " + c.floorMin() + " to " + c.floorMax());
            if (c.isNpc() && (c.floorMax() > 0 || !c.prerequisites().isEmpty() && c.hasCharter())) bad.add(c.id() + " is an NPC with boss-only data");
            if (c.isBoss() && !BossRegistry.known(c.id())) bad.add(c.id() + " is not in the boss registry (ArcStory.init did not wire it)");
        }
        if (!bad.isEmpty()) return problems(bad);
        return ok(all.isEmpty() ? "the roster is empty (no character is merged yet)" : all.size() + " characters (" + ArcRoster.bosses().size() + " bosses, " + ArcRoster.npcs().size() + " NPCs), prerequisites resolve, no loops");
    }

    // ================================================================ entities
    private static SelfTests.Result entities(MinecraftServer server) {
        ServerLevel level = server.overworld();
        List<String> bad = new ArrayList<>();
        if (!DefaultAttributes.hasSupplier(ArcEntities.NPC.get())) bad.add("the NPC type has no attributes");
        for (ArcCharacter c : ArcRoster.bosses()) {
            EntityType<? extends ArcBoss> t = ArcSpawner.typeOf(c.id());
            if (t == null) { bad.add(c.id() + " has no entity type"); continue; }
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(t);
            if (key == null || !key.equals(ResourceLocation.fromNamespaceAndPath("nusmp", c.id()))) bad.add(c.id() + " is registered as " + key);
            if (!DefaultAttributes.hasSupplier(t)) bad.add(c.id() + " has no attributes");
            ArcBoss probe = t.create(level);
            if (probe == null) bad.add(c.id() + " could not be created");
            else if (!c.equals(probe.character())) bad.add(c.id() + ": the boss class answers with another character record");
        }
        for (ArcEntities.Entry e : ArcEntities.bosses()) if (ArcRoster.byId(e.id()) == null) bad.add("the entity " + e.id() + " has no character in the roster (ArcCharacters.bootstrap does not register it)");
        if (!bad.isEmpty()) return problems(bad);
        return ok(ArcEntities.bosses().size() + " boss types and the NPC type have attributes and the right registry ids");
    }

    // ================================================================ dialogue
    private static SelfTests.Result dialogue(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        if (!DialogueLoader.has(DialogueLoader.FALLBACK_ID)) bad.add("data/nusmp/arc/dialogue/" + DialogueLoader.FALLBACK_ID + ".json is missing or unreadable");
        int warnings = 0;
        for (ArcCharacter c : ArcRoster.npcs()) {
            DialogueBook b = DialogueLoader.book(c.id());
            if (b == null) { bad.add(c.id() + " has no dialogue file (data/nusmp/arc/dialogue/" + c.id() + ".json)" + problemsOf(c.id())); continue; }
            warnings += DialogueLoader.problems().getOrDefault(c.id(), List.of()).size();
            DialogueSituation newcomer = DialogueSituation.of(0, ArcSaga.ELF_REINCARNATION.id(), List.of(), 0, true);
            if (TalkPlan.next(true, 0, ctx -> b.has(ctx, newcomer)) == null) bad.add(c.id() + " has no line for a newcomer at stage 0");
        }
        if (!bad.isEmpty()) return problems(bad);
        return ok(DialogueLoader.loaded() + " dialogue files loaded, " + ArcRoster.npcs().size() + " NPCs speak, " + warnings + " warning(s)");
    }

    private static String problemsOf(String id) {
        List<String> p = DialogueLoader.problems().get(id);
        return p == null ? "" : " (" + String.join("; ", p) + ")";
    }

    // ================================================================ story
    private static SelfTests.Result story(MinecraftServer server) {
        ServerLevel level = server.overworld();
        FakePlayer fp = SelfTests.player(level, PLAYER, 0.5, 100, 0.5);
        StoryData saved = ArcStory.data(fp).copy();
        try {
            ArcStory.reset(fp);
            if (ArcStory.hasDefeated(fp, "selftest_boss")) return fail("a fresh story already holds a defeat");
            if (!ArcStory.defeated(fp, "selftest_boss").fresh()) return fail("the first defeat was not counted");
            if (ArcStory.defeated(fp, "SELFTEST_BOSS").fresh()) return fail("the same defeat counted twice");
            if (!ArcStory.hasDefeated(fp, "selftest_boss")) return fail("a defeat was not stored");
            if (!ArcStory.met(fp, "selftest_npc") || ArcStory.met(fp, "selftest_npc")) return fail("meeting an NPC is not once-only");
            ArcStory.setQuestStage(fp, "selftest_npc", TalkPlan.QUEST_DONE);
            StoryData d = ArcStory.data(fp);
            // the stored form: through the real codec to NBT and back
            DataResult<Tag> enc = StoryAttachments.CODEC.encodeStart(NbtOps.INSTANCE, d);
            Tag tag = enc.result().orElse(null);
            if (tag == null) return fail("the story does not encode: " + enc.error().map(DataResult.Error::message).orElse("?"));
            StoryData back = StoryAttachments.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
            if (!d.equals(back)) return fail("the story changed on the way through the codec: " + d + " -> " + back);
            StoryData empty = StoryAttachments.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).result().orElse(null);
            if (empty == null || !empty.isEmpty()) return fail("an empty tag does not read as an empty story");
            // the gate, on the first boss of the roster that follows somebody
            String gated = "no boss in the roster follows another yet, so the gate was not exercised";
            ArcStory.reset(fp);
            for (ArcCharacter c : ArcRoster.bosses()) {
                if (StoryRules.missing(ArcStory.data(fp), c, id -> ArcRoster.byId(id) != null).isEmpty()) continue;
                if (ArcStory.unlocked(fp, c)) return fail(c.id() + " is open to a player with no story");
                boolean open = ArcStory.gateOpen(List.of(fp), c);
                if (ArcConfig.storyGate() && open) return fail("the gate let " + c.id() + " through");
                if (!ArcConfig.storyGate() && !open) return fail("the gate is switched off but still blocks " + c.id());
                ArcStory.unlock(fp, c);
                if (!ArcStory.unlocked(fp, c)) return fail(c.id() + " is still locked after /arc story unlock");
                if (ArcStory.hasDefeated(fp, c.id())) return fail("unlocking " + c.id() + " also marked it defeated");
                gated = c.id() + " is locked for a newcomer and open after the unlock";
                break;
            }
            return ok("defeat, meet, quest stage, codec round trip and gate hold; " + gated);
        } finally {
            StoryData live = ArcStory.data(fp);
            live.clear();
            for (String s : saved.defeatedList()) live.defeat(s);
            for (String s : saved.metList()) live.meet(s);
            for (Map.Entry<String, Integer> e : saved.questMap().entrySet()) live.setQuestStage(e.getKey(), e.getValue());
            live.setSaga(saved.saga());
        }
    }

    // ================================================================ charter
    private static SelfTests.Result charter(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        int n = 0;
        for (ArcCharacter c : ArcRoster.bosses()) {
            if (!c.hasCharter()) continue;
            n++;
            CharterTable.Boss b = CharterTable.byId(c.charterId());
            if (b == null) { bad.add(c.id() + " has no Charter row"); continue; }
            if (CharterTable.byEntity("nusmp", c.id()) != b) bad.add(c.id() + ": the entity does not resolve to its grimoire");
            if (CharterItems.item(b.id()) == null) bad.add(c.id() + " has no grimoire item");
            if (!BossRegistry.known(c.id()) || BossBuffTable.profile(c.id()) == null) bad.add(c.id() + " has no buff row");
            for (String d : ArcCharterRules.dropped(c.charterSkills())) bad.add(c.id() + " lists the skill '" + d + "', which the Charter laws drop");
            for (String skill : b.skills()) {
                ResourceLocation rl = ResourceLocation.tryParse(skill);
                Object found = null;
                try { found = rl == null ? null : SkillAPI.getSkillRegistry().get(rl); } catch (Throwable noSkills) { /* reported below */ }
                if (found == null) bad.add(c.id() + " lends '" + skill + "', which is not a registered skill");
            }
        }
        if (!bad.isEmpty()) return problems(bad);
        return ok(n + " arc grimoires: table row, item, buff row and every skill id registered");
    }

    // ================================================================ profiles
    private static SelfTests.Result profiles(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        int last = -1;
        for (int tier = ArcBossProfile.MIN_TIER; tier <= ArcBossProfile.MAX_TIER; tier++) {
            BossBuffTable.Profile p = ArcBossProfile.profileFor("selftest_tier_" + tier, tier);
            BossBuffTable.Plan plan = BossBuffTable.plan(p.bossId(), 1.0, false);
            int strength = 0;
            for (Map.Entry<String, Integer> e : p.levels().entrySet()) {
                strength += e.getValue();
                if (BossSkillRules.forbiddenEffect(e.getKey())) bad.add("tier " + tier + " uses the forbidden effect " + e.getKey());
            }
            for (String s : p.skills()) if (BossSkillRules.forbidden(s)) bad.add("tier " + tier + " uses the forbidden skill " + s);
            if (p.levels().isEmpty() && p.skills().isEmpty()) bad.add("tier " + tier + " gives nothing");
            if (strength < last) bad.add("tier " + tier + " is weaker than the tier below");
            last = strength;
            if (plan == null) bad.add("tier " + tier + " has no plan");
        }
        if (!bad.isEmpty()) return problems(bad);
        return ok("the five tier profiles only use what the boss rules allow and never get weaker");
    }

    // ================================================================ spawn
    private static SelfTests.Result spawn(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return ok("skipped: the spawn chunk is not loaded");
        Vec3 where = Vec3.atBottomCenterOf(at.above(2));
        List<String> done = new ArrayList<>();
        ArcNpcEntity npc = null;
        ArcBoss boss = null;
        try {
            // the NPC type works for a blank NPC and for the first real one
            ArcNpcEntity probe = ArcEntities.NPC.get().create(level);
            if (probe == null) return fail("the NPC type cannot create an entity");
            if (probe.removeWhenFarAway(1e9)) return fail("an NPC may despawn");
            if (!probe.isPersistenceRequired()) return fail("an NPC is not persistent");
            if (probe.talking()) return fail("a new NPC is already talking");
            ItemStack egg = ArcItems.eggFor("probe_character");
            if (!"probe_character".equals(ArcItems.characterOf(egg))) return fail("the spawn egg does not keep its character");
            done.add("blank NPC and egg component");
            List<ArcCharacter> npcs = ArcRoster.npcs();
            if (!npcs.isEmpty()) {
                ArcCharacter c = npcs.get(0);
                npc = ArcSpawner.spawnNpc(level, c, where, 0f);
                if (npc == null) return fail("could not place " + c.id());
                if (!c.id().equals(npc.characterId()) || !c.equals(npc.character())) return fail("the placed NPC is not " + c.id());
                if (npc.home() == null) return fail("the placed NPC has no post");
                boolean invulnerable = npc.isInvulnerableTo(level.damageSources().generic());
                if (invulnerable == ArcConfig.npcsMortal()) return fail("npcsMortal=" + ArcConfig.npcsMortal() + " but a generic blow " + (invulnerable ? "is" : "is not") + " ignored");
                if (npc.isInvulnerableTo(level.damageSources().genericKill())) return fail("/kill cannot kill an NPC");
                if (!npc.getName().getString().equals(c.name())) return fail("the NPC's name is '" + npc.getName().getString() + "', not " + c.name());
                CompoundTag tag = new CompoundTag();
                npc.addAdditionalSaveData(tag);
                ArcNpcEntity again = ArcEntities.NPC.get().create(level);
                if (again == null) return fail("the NPC type cannot create a second entity");
                again.readAdditionalSaveData(tag);
                if (!c.id().equals(again.characterId()) || !at.above(2).equals(again.home())) return fail("the NPC's character and post do not survive its save data");
                done.add(c.id() + " placed, named, saved");
            }
            List<ArcCharacter> bosses = ArcRoster.bosses();
            if (!bosses.isEmpty()) {
                ArcCharacter c = bosses.get(0);
                ArcSpawner.Result r = ArcSpawner.summon(level, c, where.add(4, 0, 0), 0f, List.of(), true);
                if (r.outcome() != ArcSpawner.Outcome.SPAWNED) return fail("summoning " + c.id() + " gave " + r.outcome());
                boss = r.boss();
                if (!c.equals(boss.character())) return fail("the summoned boss is not " + c.id());
                if (!c.id().equals(BossEntities.bossId(boss))) return fail(c.id() + " is not recognised as a boss by the boss registry");
                if (CharterTable.byEntity("nusmp", c.id()) == null && c.hasCharter()) return fail(c.id() + " is not recognised by the Charter table");
                if (ArcSpawner.standing(level, c.id(), where).isEmpty()) return fail("a summoned " + c.id() + " is not found standing");
                done.add(c.id() + " summoned and recognised");
            }
            return ok(String.join("; ", done) + (ArcRoster.all().isEmpty() ? " (the roster is empty)" : ""));
        } finally {
            if (npc != null) npc.discard();
            if (boss != null) boss.discard();
        }
    }
}
