package com.newuniverse.nusmp.relics.djinn;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponEngrave;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItem;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItems;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponRules;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Self-tests of the canon Belial (owner notes), prefix {@code belial.}, run by the CI server smoke test in the real game: {@code belial.wiring} (the scythe items, their lang, the
 * damage type with its tags, the engravings), {@code belial.sever} (a sever takes health, never more than the per-second cap, a boss-like target is cut down) and
 * {@code belial.exile} (an exiled mob is held and cannot be hurt, comes back with its flags, is immune for a moment, the caps hold). Each cleans up after itself.
 */
public final class BelialSelfTests {
    private BelialSelfTests() {}

    public static void register() {
        SelfTests.register("belial.wiring", BelialSelfTests::wiring);
        SelfTests.register("belial.sever", BelialSelfTests::sever);
        SelfTests.register("belial.exile", BelialSelfTests::exile);
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("anime_multiverse", path); }

    private static Zombie zombie(ServerLevel l, double x, double y, double z, double health) {
        Zombie z0 = EntityType.ZOMBIE.create(l);
        z0.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        z0.setHealth((float) health);
        z0.setPersistenceRequired();
        z0.moveTo(x, y, z, 0f, 0f);
        l.addFreshEntity(z0);
        return z0;
    }

    private static SelfTests.Result wiring(MinecraftServer server) throws Exception {
        JsonObject lang;
        try (InputStream in = BelialSelfTests.class.getResourceAsStream("/assets/anime_multiverse/lang/en_us.json")) {
            if (in == null) return SelfTests.Result.fail("en_us.json is not in the jar");
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        for (boolean dark : new boolean[]{false, true}) {
            String path = DjinnWeaponRules.itemPath(Djinn.BELIAL, dark, true);
            if (!BuiltInRegistries.ITEM.containsKey(id(path))) return SelfTests.Result.fail("the scythe " + path + " is not registered");
            if (!BuiltInRegistries.ITEM.containsKey(id(DjinnWeaponRules.itemPath(Djinn.BELIAL, dark)))) return SelfTests.Result.fail("the old Belial weapon is gone (it must stay registered)");
            if (!(DjinnWeaponItems.item(Djinn.BELIAL, dark, true) instanceof DjinnWeaponItem w) || !w.canon() || w.dark() != dark) return SelfTests.Result.fail("the item " + path + " is not the canon scythe");
            if (!lang.has("item.anime_multiverse." + path)) return SelfTests.Result.fail("no lang for " + path);
        }
        for (String k : new String[]{"spirit_sever", "exile"})
            if (!lang.has("djinn_weapon.act." + k) || !lang.has("djinn_weapon.act." + k + ".desc")) return SelfTests.Result.fail("no lang for the act " + k);
        var reg = server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> h = reg.getHolder(BelialSever.TYPE).orElse(null);
        if (h == null) return SelfTests.Result.fail("the damage type spirit_sever is not in the registry");
        if (!h.is(DamageTypeTags.BYPASSES_ARMOR) || !h.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !h.is(DamageTypeTags.BYPASSES_SHIELD) || !h.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)
                || !h.is(DamageTypeTags.BYPASSES_RESISTANCE) || !h.is(DamageTypeTags.BYPASSES_EFFECTS) || !h.is(DamageTypeTags.BYPASSES_COOLDOWN))
            return SelfTests.Result.fail("the spirit_sever damage type is missing from one of the bypass tags");
        ItemStack st = new ItemStack(DjinnWeaponItems.item(Djinn.BELIAL, false, true));
        DjinnWeaponEngrave.engrave(st, Djinn.BELIAL, false, server.registryAccess());
        if (st.getEnchantments().size() < 2) return SelfTests.Result.fail("the scythe got " + st.getEnchantments().size() + " engravings");
        if (DjinnExtremeTypes.typeId("belial") == null) return SelfTests.Result.fail("no Extreme damage type for Belial");
        return SelfTests.Result.ok("belial scythe wired (2 items, lang, damage type + 7 tags, " + st.getEnchantments().size() + " engravings), canon=" + BelialCanon.on() + ", exile=" + BelialConfig.exile());
    }

    private static SelfTests.Result sever(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.player(level, "belial_sever", 0.5, 120, 0.5);
        Zombie z = zombie(level, 0.5, 120, 3.5, 1000);
        try {
            float before = z.getHealth();
            float first = BelialSever.sever(p, z, 10.0, BelialSeverRules.ACT_FRAC);
            if (first <= 0f || z.getHealth() >= before) return SelfTests.Result.fail("a sever took nothing (" + first + ", health " + z.getHealth() + "/" + before + ")");
            double hitCap = BelialSeverRules.hitCap(BelialSeverRules.Kind.MOB, 1000);
            if (first > hitCap + 1.0) return SelfTests.Result.fail("one sever took " + first + " (hit cap " + hitCap + ")");
            for (int i = 0; i < 30; i++) BelialSever.sever(p, z, 500.0, 1.0);                       // 30 huge hits in the same tick
            double lost = before - z.getHealth();
            double secondCap = BelialSeverRules.secondCap(BelialSeverRules.Kind.MOB, 1000);
            if (lost > secondCap + 2.0) return SelfTests.Result.fail("a target lost " + lost + " in one second (cap " + secondCap + ")");
            if (lost < first - 0.5) return SelfTests.Result.fail("the later hits un-healed the target");
            return SelfTests.Result.ok(String.format(java.util.Locale.ROOT, "sever: one hit %.1f (cap %.0f), 31 hits in one tick %.1f (second cap %.0f), windows=%d", first, hitCap, lost, secondCap, BelialSever.windows()));
        } finally {
            z.discard();
            BelialSever.clearAll();
        }
    }

    private static SelfTests.Result exile(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.player(level, "belial_exile", 0.5, 120, 0.5);
        Zombie z = zombie(level, 0.5, 120, 3.5, 20);
        Zombie extra = zombie(level, 0.5, 120, 5.5, 20);
        try {
            if (!BelialConfig.exile()) return SelfTests.Result.ok("exile is switched off in the server settings: nothing to run");
            boolean wasNoAi = z.isNoAi();
            int ticks = BelialSeverRules.exileTicks(false, true, false);
            BelialSeverRules.Refusal r = BelialExile.start(p, z, ticks);
            if (r != BelialSeverRules.Refusal.OK) return SelfTests.Result.fail("the exile was refused: " + r);
            if (!BelialExile.isExiled(z.getUUID()) || !z.isNoAi() || BelialExile.countOf(p.getUUID()) != 1) return SelfTests.Result.fail("the victim is not held in the other dimension");
            if (BelialExile.start(p, z, ticks) != BelialSeverRules.Refusal.ALREADY) return SelfTests.Result.fail("a victim was exiled twice");
            float hp = z.getHealth();
            z.hurt(p.damageSources().generic(), 5f);
            if (z.getHealth() < hp) return SelfTests.Result.fail("an exiled victim was hurt");
            if (BelialSever.sever(p, z, 50.0, 0.5) != 0f) return SelfTests.Result.fail("an exiled victim was severed");
            if (BelialExile.start(p, extra, ticks) != BelialSeverRules.Refusal.OK) return SelfTests.Result.fail("a second victim was refused");
            BelialExile.endAll(server, true);
            if (BelialExile.activeCount() != 0 || BelialExile.isExiled(z.getUUID())) return SelfTests.Result.fail("the exile did not end");
            if (z.isNoAi() != wasNoAi) return SelfTests.Result.fail("the victim's AI flag was not given back");
            if (z.getPersistentData().contains(BelialExile.MARK)) return SelfTests.Result.fail("the crash-recovery marker was left on the victim");
            if (BelialExile.start(p, z, ticks) != BelialSeverRules.Refusal.IMMUNE) return SelfTests.Result.fail("a victim that just returned could be exiled again at once");
            return SelfTests.Result.ok("exile: held, unhurtable, back with its flags, immune after (" + ticks + " ticks, caps " + BelialSeverRules.MAX_VICTIMS + "/" + BelialSeverRules.MAX_PER_CASTER + "/" + BelialSeverRules.MAX_GLOBAL + ")");
        } finally {
            BelialExile.endAll(server, false);
            z.discard();
            extra.discard();
        }
    }
}
