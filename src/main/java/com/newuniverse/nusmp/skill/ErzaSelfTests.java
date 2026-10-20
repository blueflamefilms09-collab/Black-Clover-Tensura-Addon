package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.item.WeaponEngravings;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.requip.Armour;
import com.newuniverse.nusmp.requip.ArmourQuality;
import com.newuniverse.nusmp.requip.ArmourSets;
import com.newuniverse.nusmp.requip.RequipItems;
import com.newuniverse.nusmp.requip.RequipMagicRules;
import com.newuniverse.nusmp.requip.RequipRules;
import com.newuniverse.nusmp.requip.RequipSystem;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: Requip: The Knight cannot be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs
 * {@code skill.erza_requip}, which checks, in one line each: the skill is registered as an Ultimate with one mode per armour and a Dismiss mode, each with its own id; every armour's attribute modifiers
 * go on a stand-in player and come off again leaving the base values; the resistances and the Adamantine / guard rules reduce a real damage source as the table says; every armour piece has its geo
 * model, texture and glow map in the jar and every weapon its mesh, skin, models and icon; the weapons come out engraved with Tensura engravings; and the book's command path teaches the skill once
 * (and says so the second time). Registered from {@code requip.RequipSystem.init}.
 */
public final class ErzaSelfTests {
    private ErzaSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("skill.erza_requip", ErzaSelfTests::run);
        SelfTests.register("skill.erza_requip2", ErzaSelfTests::runArmourQuality);       // 1.99.0: the Djinn-quality remake of the armour models, measured from the jar
    }

    private static Result run(MinecraftServer server) {
        ServerPlayer p = SelfTests.player(server.overworld(), "ErzaRequip", 0.5, 100.0, 0.5);
        String why;
        if ((why = skill()) != null) return Result.fail("skill: " + why);
        if ((why = modifiers(p)) != null) return Result.fail("modifiers: " + why);
        if ((why = damage(p)) != null) return Result.fail("damage: " + why);
        if ((why = assets()) != null) return Result.fail("assets: " + why);
        if ((why = engravings(server)) != null) return Result.fail("engravings: " + why);
        if ((why = teaching(p)) != null) return Result.fail("teaching: " + why);
        return Result.ok(Armour.values().length + " armours, " + ArmourSets.allModelIds().size() + " piece models, " + RequipItems.weaponIds().size() + " weapons, book teaches once");
    }

    /**
     * 1.99.0 (the Djinn-quality remake): measures every armour from the files in the jar and holds it to the Djinn minimum ({@link ArmourQuality#BAR}: pieces, drawn faces, glow faces, painted texels, palette,
     * texel density, skin area), checks that every piece name resolves to its model part, that no heavier piece forges before a lighter one, that no delay is outside 0..12 ticks, that every animated piece
     * has its idle clip file, and that every piece model has a root bone. One line: the smallest armour of the set and its numbers.
     */
    private static Result runArmourQuality(MinecraftServer server) {
        ArmourQuality.Source src = path -> ErzaSelfTests.class.getClassLoader().getResourceAsStream(path);
        List<String> bad = new ArrayList<>();
        int pieces = 0, minFaces = Integer.MAX_VALUE;
        String smallest = "";
        double minDensity = Double.MAX_VALUE;
        for (Armour a : Armour.values()) {
            try {
                ArmourQuality.Metrics m = ArmourQuality.measure(a, src);
                bad.addAll(ArmourQuality.violations(a.id(), m, ArmourQuality.BAR));
                pieces += m.pieces();
                if (m.faces() < minFaces) { minFaces = m.faces(); smallest = a.id(); }
                minDensity = Math.min(minDensity, m.density());
            } catch (java.io.IOException e) {
                bad.add(a.id() + ": " + e.getMessage());
            }
            for (ArmourSets.Piece p : ArmourSets.of(a)) {
                if (ArmourSets.anchorOfName(p.name()) != p.anchor()) bad.add(a.id() + "/" + p.name() + " resolves to the wrong model part");
                if (p.delay() < 0f || p.delay() > 12f) bad.add(a.id() + "/" + p.name() + " delay " + p.delay());
                if (p.clip() != null && ErzaSelfTests.class.getClassLoader().getResource("assets/nusmp/animations/entity/" + ArmourSets.modelId(a, p) + ".animation.json") == null)
                    bad.add(a.id() + "/" + p.name() + " has no animation file");
                for (ArmourSets.Piece q : ArmourSets.of(a))
                    if (ArmourSets.weightOf(p.name()) > ArmourSets.weightOf(q.name()) && p.delay() < q.delay()) bad.add(a.id() + ": " + p.name() + " forges before the lighter " + q.name());
            }
        }
        if (!bad.isEmpty()) return Result.fail(bad.size() + " problems, first: " + bad.get(0));
        return Result.ok(Armour.values().length + " armours, " + pieces + " pieces, the smallest is " + smallest + " with " + minFaces + " faces (Djinn minimum " + ArmourQuality.BAR.faces() + "), density from "
                + String.format("%.1f", minDensity) + " texels per model pixel (Djinn " + ArmourQuality.BAR.density() + ")");
    }

    private static String skill() {
        ErzaRequipSkill s = NUSkills.ERZA_REQUIP.get();
        if (s.getType() != Skill.SkillType.ULTIMATE) return "not an Ultimate";
        ManasSkillInstance inst = s.createDefaultInstance();
        if (s.getModes(inst) != RequipMagicRules.MODES) return "mode count " + s.getModes(inst);                  // the 14 armours, Dismiss, then the Equip Magic modes appended after them
        if (RequipMagicRules.MODES < Armour.values().length + 1) return "the base modes were cut";
        Set<String> ids = new HashSet<>();
        for (int m = 0; m < s.getModes(inst); m++) {
            if (!ids.add(s.getModeId(inst, m))) return "duplicate mode id " + s.getModeId(inst, m);
            if (s.getModeName(inst, m).getString().isEmpty()) return "mode " + m + " has no name";
        }
        if (!s.getModeId(inst, RequipRules.DISMISS_MODE).endsWith("dismiss")) return "the last mode is not Dismiss";
        return null;
    }

    private static String modifiers(ServerPlayer p) {
        double speed0 = p.getAttributeValue(Attributes.MOVEMENT_SPEED), armor0 = p.getAttributeValue(Attributes.ARMOR), attack0 = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (Armour a : Armour.values()) {
            ErzaRequipFx.applyModifiers(p, a, 0.0);
            double speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED), armor = p.getAttributeValue(Attributes.ARMOR);
            boolean ok = Math.abs(speed - speed0 * (1.0 + a.stats().speed())) < 1e-6 && Math.abs(armor - Math.max(0.0, armor0 + a.stats().armor())) < 1e-6
                    && ErzaRequipFx.hasModifiers(p, a, 0.0);
            ErzaRequipFx.clearModifiers(p);
            if (!ok) return a.id() + " did not apply as the table says (speed " + speed + ", armor " + armor + ")";
            if (Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed0) > 1e-9 || Math.abs(p.getAttributeValue(Attributes.ARMOR) - armor0) > 1e-9
                    || Math.abs(p.getAttributeValue(Attributes.ATTACK_DAMAGE) - attack0) > 1e-9) return a.id() + " left a modifier behind";
        }
        return null;
    }

    private static String damage(ServerPlayer p) {
        ErzaRequipSkill s = NUSkills.ERZA_REQUIP.get();
        ManasSkillInstance inst = s.createDefaultInstance();
        inst.getOrCreateTag().putInt(ErzaRequipSkill.WORN, Armour.FLAME_EMPRESS.ordinal());
        Changeable<Float> fire = Changeable.of(10f);
        s.onTakenDamage(inst, p, p.damageSources().onFire(), fire);
        float want = RequipRules.afterResist(Armour.FLAME_EMPRESS, Armour.DamageClass.FIRE, 10f, false);
        if (Math.abs(fire.get() - want) > 1e-4) return "Flame Empress took " + fire.get() + " of 10 fire damage, the table says " + want;
        inst.getOrCreateTag().putInt(ErzaRequipSkill.WORN, Armour.HEART_KREUZ.ordinal());
        Changeable<Float> plain = Changeable.of(10f);
        s.onTakenDamage(inst, p, p.damageSources().generic(), plain);
        want = RequipRules.afterResist(Armour.HEART_KREUZ, Armour.DamageClass.PHYSICAL, 10f, false);
        if (Math.abs(plain.get() - want) > 1e-4) return "Heart Kreuz took " + plain.get() + " of 10, the table says " + want;
        inst.getOrCreateTag().remove(ErzaRequipSkill.WORN);
        Changeable<Float> none = Changeable.of(10f);
        s.onTakenDamage(inst, p, p.damageSources().generic(), none);
        if (none.get() != 10f) return "no armour must not change damage";
        return null;
    }

    private static String assets() {
        List<String> missing = new ArrayList<>();
        for (String id : ArmourSets.allModelIds()) {
            res("assets/nusmp/geo/entity/" + id + ".geo.json", missing);
            res("assets/nusmp/textures/entity/" + id + ".png", missing);
            res("assets/nusmp/textures/entity/" + id + "_glow.png", missing);
        }
        for (String w : RequipItems.weaponIds()) {
            res("assets/nusmp/weapon_meshes/" + w + ".txt", missing);
            res("assets/nusmp/textures/entity/weapon/" + w + ".png", missing);
            res("assets/nusmp/models/item/" + w + ".json", missing);
            res("assets/nusmp/models/item/" + w + "_icon.json", missing);
            res("assets/nusmp/textures/item/" + w + ".png", missing);
        }
        res("assets/nusmp/models/item/titanias_requip.json", missing);
        res("assets/nusmp/textures/item/titanias_requip.png", missing);
        res("assets/nusmp/textures/skill/erza_requip.png", missing);
        if (missing.isEmpty()) return null;
        return "missing " + String.join(", ", missing.subList(0, Math.min(6, missing.size()))) + (missing.size() > 6 ? " (+" + (missing.size() - 6) + ")" : "");
    }

    private static void res(String path, List<String> missing) {
        if (ErzaSelfTests.class.getClassLoader().getResource(path) == null) missing.add(path);
    }

    private static String engravings(MinecraftServer server) {
        for (String w : RequipItems.weaponIds()) {
            Item item = RequipItems.weapon(w);
            if (item == null) return w + " is not registered";
            ItemStack s = new ItemStack(item);
            if (!WeaponEngravings.engrave(s, server.registryAccess())) return w + " has no engravings listed";
            if (s.getEnchantments().isEmpty()) return w + " came out without any Tensura engraving";
        }
        return null;
    }

    private static String teaching(ServerPlayer p) {
        var skills = SkillAPI.getSkillsFrom(p);
        skills.forgetSkill(NUSkills.ERZA_REQUIP.getId());
        String why = RequipSystem.teach(p, true);
        if (why != null) return "the command path refused: " + why;
        if (skills.getSkill(NUSkills.ERZA_REQUIP.getId()).isEmpty()) return "the skill was not learned";
        if (RequipSystem.teach(p, true) == null) return "a second teaching must be refused";
        skills.forgetSkill(NUSkills.ERZA_REQUIP.getId());
        return null;
    }
}
