package com.newuniverse.nusmp.heroweapon.forms;

import com.newuniverse.nusmp.heroweapon.HeroComponents;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: nothing here can be played by its author, so the CI server smoke test (tools/ci/server_smoke.py, prefix {@code hero}) runs two self-tests in the real game.
 * {@code hero.forms} checks the data the loader read from the real data pack against the real registries: every shipped file is clean, every item and tag that a
 * form asks for exists (a tag must hold at least one item), every skill id is registered, a weapon that has skills has a forms file, the stray files are reported,
 * and the packets' codecs round-trip on a real game buffer. {@code hero.absorb} drives the whole mechanic with a stand-in player bonded to a vassal weapon and
 * four forms of its own (planted for the test and removed after): the gem refuses a stranger and takes a leather ration (capped to what is needed), learns the
 * form, takes a tag ration (planks), keeps the sealed curse form sealed until its series is opened, applies the active form's stats to the player and to the item,
 * gives the permanent bonus when mastery fills, cycles forms, honours the technique lock, takes every modifier off on dismiss and on release, and resets.
 */
final class FormsSelfTests {
    private FormsSelfTests() {}

    static void register() {
        SelfTests.register("hero.forms", FormsSelfTests::forms);
        SelfTests.register("hero.absorb", FormsSelfTests::absorb);
    }

    // ================================================================================================ hero.forms
    private static SelfTests.Result forms(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        int files = 0, forms = 0, requirements = 0;
        Set<String> used = new HashSet<>();
        for (HeroKind k : HeroKind.values()) {
            List<HeroForm> list = HeroForms.of(k);
            if (list.isEmpty() || !HeroForm.BASE_ID.equals(list.get(0).id())) bad.add(k + " has no base form first");
            FormsSchema.Parsed p = FormsStore.reports().get(k);
            boolean hasSkills = !HeroLogics.skillsOf(k).isEmpty();
            if (p == null) {
                if (hasSkills) bad.add(k.itemId() + " has " + HeroLogics.skillsOf(k).size() + " skills but no forms file (data/nusmp/hero/forms/" + k.itemId() + ".json)");
                continue;
            }
            files++;
            forms += p.forms().size();
            if (!p.clean()) bad.add(k.itemId() + ".json: " + p.report().replace('\n', ' '));
            for (HeroForm f : list) {
                for (String skill : f.skills().values()) {
                    used.add(skill);
                    HeroSkill s = HeroLogics.skill(skill);
                    if (s == null) bad.add(k.itemId() + " form " + f.id() + " uses the unknown skill " + skill);
                }
                for (HeroForm.Requirement r : f.requires()) {
                    requirements++;
                    String problem = resolve(r);
                    if (problem != null) bad.add(k.itemId() + " form " + f.id() + ": " + problem);
                }
            }
        }
        for (String s : FormsStore.stray()) bad.add("stray file " + s + " matches no weapon");
        String wire = wire();
        if (wire != null) bad.add(wire);
        if (!bad.isEmpty()) return problems(bad);
        int unused = 0;
        for (HeroKind k : HeroKind.values()) for (HeroSkill s : HeroLogics.skillsOf(k)) if (FormsStore.hasLoaded(k) && !used.contains(s.id())) unused++;
        return SelfTests.Result.ok(files + " of " + HeroKind.values().length + " weapons ship a forms file (" + forms + " forms, " + requirements + " requirements resolve in the registry, "
                + unused + " skills unused by any form); the packets round-trip");
    }

    /** Null when the requirement names something that exists, else what is wrong with it. */
    private static String resolve(HeroForm.Requirement r) {
        if (r.isTag()) {
            ResourceLocation id = ResourceLocation.tryParse(r.tag());
            if (id == null) return "the tag '" + r.tag() + "' is not a valid id";
            return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id)).map(t -> t.size() > 0).orElse(false) ? null : "the item tag #" + r.tag() + " does not exist or is empty";
        }
        ResourceLocation id = ResourceLocation.tryParse(r.item());
        if (id == null) return "the item '" + r.item() + "' is not a valid id";
        if (!BuiltInRegistries.ITEM.containsKey(id) || BuiltInRegistries.ITEM.get(id) == Items.AIR) return "the item " + r.item() + " does not exist";
        return null;
    }

    /** Round-trips both server packets through a real game buffer; null when they agree. */
    private static String wire() {
        HeroKind kind = null;
        for (HeroKind k : HeroKind.values()) if (FormsStore.hasLoaded(k)) { kind = k; break; }
        if (kind == null) kind = HeroKind.SHIELD;
        FormsNet.Defs defs = new FormsNet.Defs(kind, FormsStore.checksum(kind), HeroForms.of(kind));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        FormsNet.Defs.CODEC.encode(buf, defs);
        FormsNet.Defs back = FormsNet.Defs.CODEC.decode(buf);
        if (!defs.kind().equals(back.kind()) || defs.sum() != back.sum() || !defs.forms().equals(back.forms())) return "the forms packet of " + kind.itemId() + " does not survive a round trip";
        if (buf.isReadable()) return "the forms packet left " + buf.readableBytes() + " unread bytes";
        FormsNet.State state = new FormsNet.State(true, kind.itemId(), "base", 12345L, true, false, true, 100, 200, "wrath", List.of("base", "a"), Map.of("a", 5), List.of("a"),
                Map.of("minecraft:leather", 9), List.of("wrath"));
        FriendlyByteBuf sb = new FriendlyByteBuf(Unpooled.buffer());
        FormsNet.State.CODEC.encode(sb, state);
        FormsNet.State again = FormsNet.State.CODEC.decode(sb);
        if (!state.equals(again)) return "the state packet does not survive a round trip";
        return null;
    }

    private static SelfTests.Result problems(List<String> bad) {
        return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(5, bad.size()))) + (bad.size() > 5 ? " (+" + (bad.size() - 5) + " more)" : ""));
    }

    // ================================================================================================ hero.absorb
    /** The forms planted for the test: a leather form with stats and a bonus, a planks form (a tag), and a curse form of Envy that starts sealed. */
    private static final String TEST_FORMS = """
            { "weapon": "vassal_hammer",
              "forms": [
                { "id": "base", "name": "Hammer", "desc": "The starting shape.", "tier": 0, "mastery": 0 },
                { "id": "selftest_leather", "name": "Test Leather", "desc": "A form made for the self-test.", "tier": 1, "parents": ["base"],
                  "requires": [ { "item": "minecraft:leather", "count": 4 } ], "mastery": 40,
                  "stats": { "armor": 2.0, "attack_damage": 3.0, "movement_speed": 0.04 }, "abilityBonus": { "armor": 0.5 } },
                { "id": "selftest_planks", "name": "Test Planks", "desc": "A form made for the self-test.", "tier": 2, "parents": ["selftest_leather"],
                  "requires": [ { "tag": "minecraft:planks", "count": 4 } ], "mastery": 40 },
                { "id": "selftest_envy", "name": "Test Envy", "desc": "A curse form made for the self-test.", "tier": 1, "parents": ["base"],
                  "mastery": 40, "curse": true, "sin": "envy" }
              ] }
            """;

    private static ResourceLocation form(String key) { return FormLoadout.id(FormStats.FORM_PREFIX, key); }

    private static ResourceLocation bonus(String key) { return FormLoadout.id(FormStats.BONUS_PREFIX, key); }

    private static AttributeModifier mod(FakePlayer p, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance inst = p.getAttribute(attribute);
        return inst == null ? null : inst.getModifier(id);
    }

    private static AttributeModifier stackMod(ItemStack stack, ResourceLocation id) {
        for (ItemAttributeModifiers.Entry e : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) if (e.modifier().id().equals(id)) return e.modifier();
        return null;
    }

    private static SelfTests.Result absorb(MinecraftServer server) {
        ServerLevel level = server.overworld();
        FakePlayer fp = SelfTests.player(level, "hero_forms_tester", 0.5, 100, 0.5);
        HeroKind kind = HeroKind.HAMMER;
        FormsSchema.Parsed parsed = FormsSchema.parseText(kind, TEST_FORMS, null);
        if (!parsed.clean()) return SelfTests.Result.fail("the test forms are not clean: " + parsed.report().replace('\n', ' '));
        HeroBond b = HeroBonds.of(fp);
        List<HeroForm> before = FormsStore.swapLoaded(kind, parsed.forms());
        List<String> bad = new ArrayList<>();
        try {
            reset(fp, b);
            b.bind(kind, level.getGameTime());
            boolean auto = FormsConfig.get(FormsConfig.AUTO_LEARN), onlyNeeded = FormsConfig.get(FormsConfig.ABSORB_ONLY_NEEDED), anything = FormsConfig.get(FormsConfig.ABSORB_ANYTHING);

            // a stranger is refused (unless the config lets the gem take anything) and left alone
            fp.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COBBLESTONE, 8));
            if (!anything) {
                if (HeroForms.absorbOffhand(fp)) bad.add("cobblestone was absorbed although no form asks for it");
                if (fp.getOffhandItem().getCount() != 8) bad.add("cobblestone was taken from the off hand");
            }

            // leather: taken as far as the forms need it, the form opens
            fp.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.LEATHER, 6));
            if (!HeroForms.absorbOffhand(fp)) bad.add("the leather was not absorbed");
            int want = onlyNeeded && !anything ? 4 : 6;
            if (b.absorbedCount("minecraft:leather") != want) bad.add("absorbed " + b.absorbedCount("minecraft:leather") + " leather, expected " + want);
            if (fp.getOffhandItem().getCount() != 6 - want) bad.add("the off hand holds " + fp.getOffhandItem().getCount() + " leather, expected " + (6 - want));
            if (!auto && FormLearning.tryLearn(fp, "selftest_leather") != null) bad.add("the Learn route refused the leather form");
            if (!b.isUnlocked("selftest_leather")) bad.add("the leather form was not learned");

            // a tag requirement: any planks count
            fp.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_PLANKS, 5));
            if (!HeroForms.absorbOffhand(fp)) bad.add("the planks were not absorbed through the tag");
            if (!auto && FormLearning.tryLearn(fp, "selftest_planks") != null) bad.add("the Learn route refused the planks form");
            if (!b.isUnlocked("selftest_planks")) bad.add("the planks form (a tag requirement) was not learned");
            fp.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

            // the sealed curse form stays sealed
            if (b.isUnlocked("selftest_envy")) bad.add("the sealed curse form was learned by itself");
            if (FormLearning.learnable(b).contains("selftest_envy")) bad.add("the sealed curse form is offered");
            if (HeroForms.trySetActive(fp, "selftest_envy") == null) bad.add("a form that is not learned could be activated");
            if (HeroForms.trySetActive(fp, "no_such_form") == null) bad.add("an unknown form could be activated");

            // summon: the active form's numbers appear on the player and on the item
            fp.setItemInHand(InteractionHand.MAIN_HAND, HeroItems.stack(kind));
            b.setSummoned(true);
            if (HeroForms.trySetActive(fp, "selftest_leather") != null) bad.add("the learned leather form could not be activated");
            AttributeModifier armor = mod(fp, Attributes.ARMOR, form("armor"));
            if (armor == null || Math.abs(armor.amount() - 2.0) > 1e-9) bad.add("the form's armor modifier is " + armor);
            AttributeModifier speed = mod(fp, Attributes.MOVEMENT_SPEED, form("movement_speed"));
            if (speed == null || speed.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE || Math.abs(speed.amount() - 0.04) > 1e-9) bad.add("the form's speed modifier is " + speed);
            if (mod(fp, Attributes.ATTACK_DAMAGE, form("attack_damage")) != null) bad.add("attack damage was put on the player instead of the item");
            ItemStack hand = fp.getMainHandItem();
            AttributeModifier dmg = stackMod(hand, form("attack_damage"));
            if (dmg == null || Math.abs(dmg.amount() - 3.0) > 1e-9) bad.add("the item's attack damage modifier is " + dmg);
            if (!"base".equals(HeroComponents.look(hand))) bad.add("the look of the item is " + HeroComponents.look(hand));
            int modifiers = fp.getAttribute(Attributes.ARMOR).getModifiers().size();
            ItemAttributeModifiers onItem = hand.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            HeroForms.refresh(fp);
            HeroForms.refresh(fp);
            if (fp.getAttribute(Attributes.ARMOR).getModifiers().size() != modifiers) bad.add("refresh is not idempotent on the player");
            if (!onItem.equals(fp.getMainHandItem().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY))) bad.add("refresh is not idempotent on the item");

            // mastery: filling it gives the permanent bonus once
            int hits = 0;
            while (hits++ < 400 && b.mastery("selftest_leather") < 40) FormMastery.hit(fp);
            if (FormsConfig.get(FormsConfig.MASTERY_PER_HIT) > 0) {
                if (b.mastery("selftest_leather") != 40) bad.add("mastery stopped at " + b.mastery("selftest_leather"));
                if (!b.bonusGranted("selftest_leather")) bad.add("the finished form gave no bonus");
                AttributeModifier perm = mod(fp, Attributes.ARMOR, bonus("armor"));
                if (perm == null || Math.abs(perm.amount() - 0.5) > 1e-9) bad.add("the permanent armor bonus is " + perm);
                FormMastery.hit(fp);
                if (b.mastery("selftest_leather") != 40) bad.add("mastery went past its total");
            }

            // cycling passes learned forms only: leather, planks, then round to the base (the sealed curse form is not learned)
            String next = HeroForms.cycle(fp);
            if (!"selftest_planks".equals(next)) bad.add("cycle gave " + next + " after the leather form, expected selftest_planks");
            next = HeroForms.cycle(fp);
            if (!"base".equals(next)) bad.add("cycle gave " + next + " after the planks form, expected base");
            if (mod(fp, Attributes.ARMOR, form("armor")) != null) bad.add("the base form still carries the leather form's armor");

            // the series opens: the curse form becomes available, can be learned and used
            CompoundTag curse = b.extra("curse");
            ListTag series = new ListTag();
            series.add(StringTag.valueOf("envy"));
            curse.put("series", series);
            List<HeroForm> learned = FormLearning.learnAvailable(fp, null, true);
            if (learned.stream().noneMatch(f -> f.id().equals("selftest_envy"))) bad.add("the curse form did not open with its series");
            if (HeroForms.trySetActive(fp, "selftest_envy") != null) bad.add("the unlocked curse form could not be activated");
            if (!"base".equals(HeroComponents.look(fp.getMainHandItem()))) bad.add("a vassal's curse form must keep the base look");
            curse.remove("series");
            if (!FormTree.sealed(HeroForms.get(kind, "selftest_envy"), CurseLink.series(b))) bad.add("the series key was not removed");

            // the technique lock
            if (FormsConfig.get(FormsConfig.TECHNIQUE_LOCK_TICKS) > 0) {
                FormMastery.skill(fp, null);
                if (HeroForms.trySetActive(fp, "base") == null) bad.add("the form changed right after a technique");
                FormLocks.forget(fp.getUUID());
                if (HeroForms.trySetActive(fp, "base") != null) bad.add("the form could not be changed once the lock was gone");
                FormMastery.skill(fp, null);
                if (!HeroForms.forceActive(fp, "selftest_leather") || !"selftest_leather".equals(b.form())) bad.add("the forced change (for the end of an Overrun) was refused during a technique lock");
                FormLocks.forget(fp.getUUID());
            }

            // dismiss: the form's modifiers go, the permanent bonus stays
            b.setSummoned(false);
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            HeroForms.setActive(fp, "selftest_leather");
            HeroForms.refresh(fp);
            if (mod(fp, Attributes.ARMOR, form("armor")) != null || mod(fp, Attributes.MOVEMENT_SPEED, form("movement_speed")) != null) bad.add("form modifiers stayed after the weapon was put away");
            if (b.bonusGranted("selftest_leather") && mod(fp, Attributes.ARMOR, bonus("armor")) == null) bad.add("the permanent bonus went with the weapon");

            // release: nothing of ours is left
            b.clear();
            HeroForms.refresh(fp);
            for (String key : HeroForm.STAT_KEYS) {
                var attr = FormLoadout.attribute(key);
                if (mod(fp, attr, bonus(key)) != null || mod(fp, attr, form(key)) != null) bad.add("a modifier for " + key + " outlived the bond");
            }

            // reset keeps the weapon and its level but forgets forms, meals and mastery
            b.bind(kind, level.getGameTime());
            b.setExp(500);
            b.absorb("minecraft:leather", 4);
            b.unlock("selftest_leather");
            b.addMastery("selftest_leather", 10);
            b.setForm("selftest_leather");
            FormLearning.resetForms(fp);
            if (b.isUnlocked("selftest_leather") || b.absorbedCount("minecraft:leather") != 0 || b.mastery("selftest_leather") != 0 || !"base".equals(b.form())) bad.add("reset left progress behind");
            if (b.exp() != 500 || b.kind() != kind) bad.add("reset touched the weapon or its experience");
        } finally {
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            fp.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            reset(fp, b);
            FormsStore.restoreLoaded(kind, before);
        }
        if (!bad.isEmpty()) return problems(bad);
        return SelfTests.Result.ok("absorbing, tag requirements, learning, the sealed curse form, stats on the player and the item, mastery and its bonus, cycling, the technique lock, "
                + "dismiss, release and reset all behave");
    }

    /** Leaves the stand-in player and the forms system's memory of it as if the test never ran. */
    private static void reset(FakePlayer fp, HeroBond b) {
        b.clear();
        FormLoadout.clear(fp);
        FormLocks.forget(fp.getUUID());
        HeroAbsorb.forget(fp.getUUID());
        FormsSync.forget(fp.getUUID());
    }
}
