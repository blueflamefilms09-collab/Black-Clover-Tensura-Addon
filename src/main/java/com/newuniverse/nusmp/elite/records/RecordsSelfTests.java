package com.newuniverse.nusmp.elite.records;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.records.rule.BuiltinSynergies;
import com.newuniverse.nusmp.elite.records.rule.Bonuses;
import com.newuniverse.nusmp.elite.records.rule.Catalog;
import com.newuniverse.nusmp.elite.records.rule.CondType;
import com.newuniverse.nusmp.elite.records.rule.Defs.RecordDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.SynergyDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.TitleDef;
import com.newuniverse.nusmp.elite.records.rule.Engine;
import com.newuniverse.nusmp.elite.records.rule.RecordData;
import com.newuniverse.nusmp.elite.records.rule.StatNames;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the unit tests prove the pure rules; what only the real game can prove is that the data files agree with the REAL registries (every attribute, effect, entity, biome, structure and skill a
 * record or title names exists), that the NBT bridge survives hostile tags, and that the whole server chain works on a real player object: a counter set through the public {@link RecordEvents} API
 * unlocks a record, the record earns a title, the title can be made active, its attribute modifier appears on the real attribute and is removed again, a synergy of held titles applies its bonus and
 * drops it when a title is taken away, the custom stats are readable, and the four {@code EliteHooks} answer. Keys {@code elite.records.*}, run by the CI server smoke test like the other self-tests.
 * A fake player has no connection, so nothing is sent to it (the service skips every packet and message for such a player); everything else runs for real, and everything is put back in a
 * {@code finally}.
 */
public final class RecordsSelfTests {
    private RecordsSelfTests() {}

    public static void register() {
        SelfTests.register("elite.records.data", RecordsSelfTests::data);
        SelfTests.register("elite.records.nbt", RecordsSelfTests::nbt);
        SelfTests.register("elite.records.chain", RecordsSelfTests::chain);
    }

    private static SelfTests.Result fail(List<String> bad) {
        return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
    }

    // ================================================================ elite.records.data
    private static SelfTests.Result data(MinecraftServer server) {
        Catalog cat = RecordsCatalog.get();
        if (cat.isEmpty()) return SelfTests.Result.fail("the catalog is empty: the data reload listener did not run or found no files");
        List<String> bad = new ArrayList<>(RecordsCatalog.problems());
        if (cat.records().size() < 120) bad.add("fewer than 120 records: " + cat.records().size());
        if (cat.titles().size() < 60) bad.add("fewer than 60 titles: " + cat.titles().size());
        if (cat.synergies().size() < 15) bad.add("fewer than 15 synergies: " + cat.synergies().size());
        List<Bonuses> all = new ArrayList<>();
        for (TitleDef t : cat.titles()) all.add(t.bonuses());
        for (SynergyDef s : cat.synergies()) all.add(s.bonuses());
        for (Bonuses b : all) {
            for (Bonuses.Attr a : b.attributes()) if (TitleApplier.attribute(a.attribute()) == null) bad.add("unknown attribute " + a.attribute());
            for (Bonuses.Effect e : b.effects()) if (TitleApplier.effect(e.effect()) == null) bad.add("unknown effect " + e.effect());
        }
        var biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
        var structures = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (RecordDef r : cat.records()) {
            var c = r.condition();
            switch (c.type()) {
                case KILL_ENTITY -> {
                    List<String> ids = new ArrayList<>(c.strings("entities"));
                    if (!c.str("entity", "").isEmpty()) ids.add(c.str("entity", ""));
                    for (String id : ids) if (!anyKey(BuiltInRegistries.ENTITY_TYPE.keySet(), id)) bad.add(r.id() + ": no entity matches " + id);
                }
                case BIOME_DISCOVERY -> {
                    String id = c.str("biome", "");
                    if (!id.isEmpty() && !anyKey(biomes.keySet(), id)) bad.add(r.id() + ": no biome " + id);
                }
                case STRUCTURE_DISCOVERY -> {
                    String id = c.str("structure", "");
                    if (!id.isEmpty() && !anyKey(structures.keySet(), id)) bad.add(r.id() + ": no structure matches " + id);
                }
                case SKILL_LEARNED -> {
                    String id = c.str("skill", "");
                    ResourceLocation rl = id.isEmpty() ? null : ResourceLocation.tryParse(id);
                    if (!id.isEmpty() && (rl == null || SkillAPI.getSkillRegistry().get(rl) == null)) bad.add(r.id() + ": no skill " + id);
                }
                default -> { }
            }
        }
        for (CondType t : CondType.values()) {
            boolean used = false;
            for (RecordDef r : cat.records()) if (r.condition().type() == t) { used = true; break; }
            if (!used) bad.add("no record uses the condition type " + t.id());
        }
        if (!bad.isEmpty()) return fail(bad);
        return SelfTests.Result.ok(cat.records().size() + " records, " + cat.titles().size() + " titles, " + cat.synergies().size() + " synergies ("
                + BuiltinSynergies.all().size() + " built in); every attribute, effect, entity, biome, structure and skill they name exists");
    }

    private static boolean anyKey(Iterable<ResourceLocation> keys, String pattern) {
        for (ResourceLocation k : keys) if (com.newuniverse.nusmp.elite.records.rule.ConditionEvaluator.idMatches(pattern, k.toString())) return true;
        return false;
    }

    // ================================================================ elite.records.nbt
    private static SelfTests.Result nbt(MinecraftServer server) {
        RecordData d = new RecordData();
        d.unlock("kills_10", 1_700_000_000L);
        d.grantTitle("brawler", 1_700_000_001L);
        d.setActive("brawler");
        d.addCounter("kills_all", 12);
        d.bestMax("ep", 9876.5);
        d.mapInc("kills", "minecraft:zombie", 3);
        d.setAdd("biomes", "minecraft:plains");
        CompoundTag tag = RecordDataNbt.write(d);
        RecordData back = RecordDataNbt.read(tag);
        if (!back.toRaw().equals(d.toRaw())) return SelfTests.Result.fail("a round trip through NBT changed the data: " + back.toRaw() + " vs " + d.toRaw());
        // hostile tags: wrong types in every slot, a list where a compound belongs, a huge junk list
        CompoundTag junk = new CompoundTag();
        junk.putString("rec", "not a compound");
        junk.put("tit", new ListTag());
        junk.putInt("act", 4);
        CompoundTag cnt = new CompoundTag();
        cnt.putString("a", "x");
        cnt.putDouble("b", -3.0);
        cnt.putDouble("c", 2.0);
        junk.put("cnt", cnt);
        ListTag sets = new ListTag();
        sets.add(StringTag.valueOf("oops"));
        junk.put("sets", sets);
        RecordData jd = RecordDataNbt.read(junk);
        if (jd.counter("c") != 2.0 || jd.counter("b") != 0.0 || jd.unlockedCount() != 0) return SelfTests.Result.fail("hostile NBT was not read tolerantly");
        if (!RecordDataNbt.read(new CompoundTag()).isEmpty()) return SelfTests.Result.fail("an empty tag should read as empty data");
        return SelfTests.Result.ok("round trip exact; hostile NBT read without an exception");
    }

    // ================================================================ elite.records.chain
    private static SelfTests.Result chain(MinecraftServer server) {
        if (!RecordsConfig.enabled()) return SelfTests.Result.ok("skipped: the records system is switched off in its config");
        if (RecordsCatalog.get().isEmpty()) return SelfTests.Result.fail("no catalog");
        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, "records_tester", 0.5, 100, 0.5);
        RecordData d = RecordsAttachments.of(p);
        UUID id = p.getUUID();
        double scale = RecordsConfig.bonusScale();
        d.clear();
        TitleApplier.clear(p);
        try {
            // null safety of the public API
            RecordEvents.warKill((net.minecraft.server.level.ServerPlayer) null);
            RecordEvents.calamityKill((UUID) null);
            RecordEvents.forgeCrafted(UUID.randomUUID(), 3);
            if (RecordEvents.customStat((UUID) null, StatNames.KILL_HEAL) != 0.0) return SelfTests.Result.fail("customStat(null) should be 0");

            // a counter set through the public API unlocks a record, and the record earns a title
            RecordEvents.warKill(p);
            Engine.Result r = RecordsService.beat(p, false);
            if (!r.newRecords().contains("warkills_1")) return SelfTests.Result.fail("warKill() did not unlock warkills_1: " + r.newRecords());
            if (!r.newTitles().contains("soldier")) return SelfTests.Result.fail("warkills_1 did not earn the title soldier: " + r.newTitles());
            if (!EliteHooks.recordUnlocked.test(id, "warkills_1") || !EliteHooks.hasTitle.test(id, "soldier") || EliteHooks.heldTitleCount.applyAsInt(id) < 1)
                return SelfTests.Result.fail("the EliteHooks readers do not see the unlock");

            // the title can be made active; its modifier is on the REAL attribute, then gone
            RecordsService.onSetActive(p, "soldier");
            if (!"soldier".equals(EliteHooks.activeTitle.apply(id))) return SelfTests.Result.fail("activeTitle hook: " + EliteHooks.activeTitle.apply(id));
            ResourceLocation armorMod = TitleApplier.modifierId(new Bonuses.Attr("minecraft:generic.armor", 0, Bonuses.Op.ADD_VALUE));
            AttributeInstance armor = p.getAttribute(Attributes.ARMOR);
            if (armor == null || armor.getModifier(armorMod) == null) return SelfTests.Result.fail("the soldier armor modifier is not on the player");
            if (Math.abs(armor.getModifier(armorMod).amount() - 0.5 * scale) > 1e-6) return SelfTests.Result.fail("armor modifier amount " + armor.getModifier(armorMod).amount() + " vs " + 0.5 * scale);
            RecordsService.onSetActive(p, "");
            if (armor.getModifier(armorMod) != null) return SelfTests.Result.fail("the armor modifier stayed after the title was removed");
            RecordsService.onSetActive(p, "brawler");                         // not held: refused, the active title stays empty
            if (!d.active().isEmpty()) return SelfTests.Result.fail("a title that is not held was accepted as active");

            // a custom stat of the active title is readable
            RecordsService.grantTitle(p, "dragon_slayer");
            RecordsService.onSetActive(p, "dragon_slayer");
            double boss = RecordEvents.customStat(id, StatNames.BOSS_DAMAGE_PERCENT);
            if (Math.abs(boss - 5.0 * scale) > 1e-6) return SelfTests.Result.fail("boss_damage_percent of dragon_slayer: " + boss);
            RecordsService.onSetActive(p, "");

            // a synergy of three HELD titles applies its bonus, and drops it when one is taken away
            d.grantTitle("wanderer", 1);
            d.grantTitle("naturalist", 1);
            d.grantTitle("ruin_delver", 1);
            RecordsService.beat(p, false);
            ResourceLocation luckMod = TitleApplier.modifierId(new Bonuses.Attr("minecraft:generic.luck", 0, Bonuses.Op.ADD_VALUE));
            AttributeInstance luck = p.getAttribute(Attributes.LUCK);
            if (luck == null || luck.getModifier(luckMod) == null) return SelfTests.Result.fail("the Pilgrim's Road synergy did not apply its luck bonus");
            d.revokeTitle("ruin_delver");
            RecordsService.beat(p, false);
            if (luck.getModifier(luckMod) != null) return SelfTests.Result.fail("the synergy bonus stayed after a required title was taken away");

            // admin operations
            if (!RecordsService.grantRecord(p, "kills_10") || RecordsService.grantRecord(p, "kills_10")) return SelfTests.Result.fail("grantRecord should succeed once");
            if (!RecordsService.revokeRecord(p, "kills_10")) return SelfTests.Result.fail("revokeRecord failed");
            return SelfTests.Result.ok("warKill -> record -> title -> active title -> attribute modifier on and off; synergy on and off; custom stat; hooks; API null safety");
        } finally {
            d.clear();
            TitleApplier.clear(p);
        }
    }
}
