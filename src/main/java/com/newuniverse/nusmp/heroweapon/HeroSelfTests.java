package com.newuniverse.nusmp.heroweapon;

import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: nothing of the hero weapons can be played by its author, so the catalogue has a SELF-TEST that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}, key {@code hero.catalog}; the smoke script expects the prefix {@code hero}): every weapon of the enum has a registered
 * item under {@code nusmp:<itemId>}, a logic whose {@code kind()} matches, skills whose ids start with the weapon's short name and are not repeated, and a
 * non-empty list of forms that starts with the base form and whose parents all exist. Each system adds its own tests under {@code hero.<system>.*} in its own
 * {@code init}. The lead owns this file.
 */
public final class HeroSelfTests {
    private HeroSelfTests() {}

    public static void register() { SelfTests.register("hero.catalog", HeroSelfTests::catalog); }

    private static SelfTests.Result catalog(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        Set<String> items = new HashSet<>(), skillIds = new HashSet<>();
        int skills = 0, forms = 0;
        for (HeroKind k : HeroKind.values()) {
            if (!items.add(k.itemId())) bad.add("item id " + k.itemId() + " is used twice");
            if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", k.itemId()))) bad.add(k + " has no registered item nusmp:" + k.itemId());
            if (!HeroLogics.has(k)) bad.add(k + " has no registered logic");
            else if (HeroLogics.of(k).kind() != k) bad.add(k + " has a logic that says it is " + HeroLogics.of(k).kind());
            String prefix = k.name().toLowerCase() + ".";
            for (HeroSkill s : HeroLogics.skillsOf(k)) {
                skills++;
                if (!s.id().startsWith(prefix)) bad.add("skill " + s.id() + " of " + k + " must start with '" + prefix + "'");
                if (!skillIds.add(s.id())) bad.add("skill id " + s.id() + " is used twice");
            }
            List<HeroForm> list = HeroForms.of(k);
            forms += list.size();
            if (list.isEmpty() || !HeroForm.BASE_ID.equals(list.get(0).id())) bad.add(k + " has no base form first");
            Set<String> ids = new HashSet<>();
            for (HeroForm f : list) ids.add(f.id());
            for (HeroForm f : list) for (String p : f.parents()) if (!ids.contains(p)) bad.add(k + " form " + f.id() + " has the unknown parent " + p);
        }
        if (!bad.isEmpty()) return SelfTests.Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))) + (bad.size() > 6 ? " (+" + (bad.size() - 6) + " more)" : ""));
        return SelfTests.Result.ok(HeroKind.values().length + " weapons, " + skills + " skills, " + forms + " forms");
    }
}
