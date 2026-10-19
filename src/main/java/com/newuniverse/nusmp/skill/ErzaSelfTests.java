package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.item.WeaponEngravings;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.requip.Armour;
import com.newuniverse.nusmp.requip.ArmourSets;
import com.newuniverse.nusmp.requip.RequipItems;
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

    private static String skill() {
        ErzaRequipSkill s = NUSkills.ERZA_REQUIP.get();
        if (s.getType() != Skill.SkillType.ULTIMATE) return "not an Ultimate";
        ManasSkillInstance inst = s.createDefaultInstance();
        if (s.getModes(inst) != Armour.values().length + 1) return "mode count " + s.getModes(inst);
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
