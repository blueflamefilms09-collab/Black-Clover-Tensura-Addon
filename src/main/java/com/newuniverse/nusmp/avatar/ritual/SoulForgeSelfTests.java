package com.newuniverse.nusmp.avatar.ritual;

import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarSoulForgeConfig;
import com.newuniverse.nusmp.avatar.aptitude.Aptitude;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeCatalog;
import com.newuniverse.nusmp.avatar.aptitude.ForgeChoices;
import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkill;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkills;
import com.newuniverse.nusmp.avatar.aptitude.ForgeValidator;
import com.newuniverse.nusmp.avatar.item.AvatarItems;
import com.newuniverse.nusmp.avatar.item.SoulForgeBlocks;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Why: nothing of the Soul Forge can be played by its author, so the parts that can be proved without a person are proved by the real game: the CI server smoke
 * test boots the mod and runs these (key prefix {@code avatar}; the lead adds it to SMOKE_EXPECT). They check that the block, its item, the menu and the two
 * recipes exist in the real registries, that the aptitude and skill data loaded through the real reload listeners, that the skills resolve against the real
 * Tensura registry (the ones that do not are NAMED in the result, never hidden), that the pure validator accepts an honest request and refuses a hacked one when
 * fed the live catalogs, that the ritual lock is a transient modifier which is not saved and is lifted again, and that a ritual can start for a stand-in player
 * and be cancelled without leaving anything behind. They never open a screen, never create an Avatar and never touch a real player.
 */
public final class SoulForgeSelfTests {
    private SoulForgeSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("avatar.forge_registry", SoulForgeSelfTests::registry);
        SelfTests.register("avatar.forge_data", SoulForgeSelfTests::data);
        SelfTests.register("avatar.forge_recipes", SoulForgeSelfTests::recipes);
        SelfTests.register("avatar.forge_skills", SoulForgeSelfTests::skills);
        SelfTests.register("avatar.forge_validator", SoulForgeSelfTests::validator);
        SelfTests.register("avatar.forge_lock", SoulForgeSelfTests::lock);
        SelfTests.register("avatar.forge_ritual", SoulForgeSelfTests::ritual);
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    // ---------------------------------------------------------------- the block, the item, the menu
    private static Result registry(MinecraftServer server) {
        if (!BuiltInRegistries.BLOCK.containsKey(id("soul_forge"))) return Result.fail("block nusmp:soul_forge is not registered");
        if (!BuiltInRegistries.ITEM.containsKey(id("soul_forge"))) return Result.fail("item nusmp:soul_forge is not registered");
        if (!BuiltInRegistries.MENU.containsKey(id("soul_forge"))) return Result.fail("menu nusmp:soul_forge is not registered");
        if (!BuiltInRegistries.ITEM.containsKey(id("genesis_core"))) return Result.fail("item nusmp:genesis_core is not registered");
        int light = SoulForgeBlocks.SOUL_FORGE.get().defaultBlockState().getLightEmission();
        if (light <= 0) return Result.fail("the Soul Forge gives no light (" + light + ")");
        return Result.ok("block, item, menu and Genesis Core registered; light " + light);
    }

    // ---------------------------------------------------------------- the data
    private static Result data(MinecraftServer server) {
        int apt = AptitudeCatalog.size(), skills = ForgeSkills.all().size();
        if (apt < 24) return Result.fail("only " + apt + " aptitudes loaded (the brief asks for 24 or more): did the reload listener run?");
        if (skills < 20) return Result.fail("only " + skills + " forge skills loaded");
        Aptitude time = AptitudeCatalog.byId("time"), anti = AptitudeCatalog.byId("anti_magic");
        if (time == null || anti == null) return Result.fail("Time or Anti-Magic is missing");
        if (time.rarity().ordinal() < Aptitude.Rarity.EPIC.ordinal() || anti.rarity().ordinal() < Aptitude.Rarity.EPIC.ordinal()) return Result.fail("Time and Anti-Magic must be Epic or Legendary");
        return Result.ok(apt + " aptitudes, " + skills + " forge skills loaded");
    }

    // ---------------------------------------------------------------- the recipes
    private static Result recipes(MinecraftServer server) {
        for (String name : new String[]{"soul_forge", "genesis_core"}) {
            Optional<RecipeHolder<?>> holder = server.getRecipeManager().byKey(id(name));
            if (holder.isEmpty()) return Result.fail("recipe nusmp:" + name + " is not loaded");
            Recipe<?> r = holder.get().value();
            ItemStack out = r.getResultItem(server.registryAccess());
            if (out.isEmpty() || !BuiltInRegistries.ITEM.getKey(out.getItem()).equals(id(name))) return Result.fail("recipe nusmp:" + name + " makes " + out);
        }
        Recipe<?> forge = server.getRecipeManager().byKey(id("soul_forge")).orElseThrow().value();
        ItemStack core = AvatarItems.coreStack();
        boolean needsCore = false;
        for (Ingredient ing : forge.getIngredients()) if (ing.test(core)) needsCore = true;
        if (!needsCore) return Result.fail("the Soul Forge recipe does not need a Genesis Core");
        return Result.ok("both recipes loaded; the forge needs a core");
    }

    // ---------------------------------------------------------------- the skills against the real registry
    private static Result skills(MinecraftServer server) {
        List<ForgeSkill> all = ForgeSkills.all();
        List<String> missing = new ArrayList<>();
        int ok = 0, uniques = 0, ultimates = 0;
        for (ForgeSkill s : all) {
            ForgeSkill r = ForgeSkillResolver.resolve(s);
            if (r == null) { missing.add(s.id()); continue; }
            ok++;
            if (r.kind() == ForgeSkill.Kind.UNIQUE) uniques++; else ultimates++;
        }
        for (ForgeSkill s : all) {
            if (!s.id().startsWith("nusmp:")) continue;
            if (ForgeSkillResolver.resolve(s) == null) return Result.fail("this mod's own skill " + s.id() + " does not resolve (the registry or the id is wrong)");
        }
        if (ok < 8 || uniques == 0) return Result.fail(ok + " of " + all.size() + " skills resolve (" + uniques + " unique, " + ultimates + " ultimate); missing " + shorten(missing));
        return Result.ok(ok + " of " + all.size() + " skills resolve (" + uniques + " unique, " + ultimates + " ultimate)" + (missing.isEmpty() ? "" : "; left out: " + shorten(missing)));
    }

    private static String shorten(List<String> ids) {
        String s = String.join(",", ids.subList(0, Math.min(8, ids.size())));
        return ids.size() > 8 ? s + ",+" + (ids.size() - 8) : s;
    }

    // ---------------------------------------------------------------- the judge, fed the live catalogs
    private static Result validator(MinecraftServer server) {
        ForgeRules rules = AvatarSoulForgeConfig.rules();
        ForgeContext rich = new ForgeContext(0.0, 1.0e9, 1.0e9, 1.0e9, 0.0, 2, false, "", 0L, 0.0);
        Aptitude gift = null;
        for (Aptitude a : AptitudeCatalog.all()) if (a.taxPercent() > 0.0 && (gift == null || a.taxPercent() < gift.taxPercent())) gift = a;
        if (gift == null) return Result.fail("no taxed aptitude in the catalog");
        ForgeSkill unique = null;
        for (ForgeSkill s : ForgeSkillResolver.available()) if (s.kind() == ForgeSkill.Kind.UNIQUE && s.minEp() >= 0.0) { unique = s; break; }
        if (unique == null) return Result.fail("no Unique skill resolves, so the Unique slot would be empty");
        ForgeValidator.Verdict honest = ForgeValidator.validate(new ForgeChoices(List.of(gift.id()), 0L, unique.id(), "", false), rich, rules, AptitudeCatalog::byId, ForgeSkillResolver::lookup);
        if (rules.maxAptitudes() >= 1 && !honest.ok()) return Result.fail("an honest request was refused: " + honest.refusal().text());
        ForgeValidator.Verdict unknown = ForgeValidator.validate(new ForgeChoices(List.of("no_such_aptitude"), 0L, "", "", false), rich, rules, AptitudeCatalog::byId, ForgeSkillResolver::lookup);
        if (unknown.ok() || unknown.refusal().code() != ForgeValidator.Code.UNKNOWN_APTITUDE) return Result.fail("an unknown aptitude was not refused as such");
        ForgeValidator.Verdict faked = ForgeValidator.validate(new ForgeChoices(List.of(), 12345L, "", "", false), rich, rules, AptitudeCatalog::byId, ForgeSkillResolver::lookup);
        if (faked.ok() || faked.refusal().code() != ForgeValidator.Code.ROLL_MISMATCH) return Result.fail("a roll token the server never issued was accepted");
        ForgeContext poor = new ForgeContext(0.0, 0.0, 1000.0, 0.0, 0.0, 0, false, "", 0L, 0.0);
        ForgeValidator.Verdict broke = ForgeValidator.validate(new ForgeChoices(List.of(), 0L, "", "", false), poor, rules, AptitudeCatalog::byId, ForgeSkillResolver::lookup);
        if (broke.ok() || broke.refusal().code() != ForgeValidator.Code.NEED_CORES) return Result.fail("a creator with no Genesis Core was not refused");
        return Result.ok("honest request accepted (" + gift.id() + " + " + unique.id() + "); unknown id, forged token and missing core refused");
    }

    // ---------------------------------------------------------------- the lock
    private static Result lock(MinecraftServer server) {
        ServerPlayer p = SelfTests.player(server.overworld(), "AvatarForgeLock", 0.5, 100.0, 0.5);
        try {
            RitualLock.release(p);
            double before = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
            RitualLock.apply(p);
            RitualLock.apply(p);                                                      // twice must not stack
            if (!RitualLock.locked(p)) return Result.fail("the lock did not take");
            double during = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
            if (Math.abs(during) > 1.0e-9) return Result.fail("walking is not frozen: speed " + during);
            if (p.getAttributeValue(Attributes.JUMP_STRENGTH) > 1.0e-9) return Result.fail("jumping is not frozen");
            CompoundTag saved = p.saveWithoutId(new CompoundTag());
            if (saved.toString().contains("soul_forge_lock")) return Result.fail("the lock is written to the save file: a crash would keep it");
            RitualLock.release(p);
            if (RitualLock.locked(p)) return Result.fail("the lock did not lift");
            double after = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
            if (Math.abs(after - before) > 1.0e-9) return Result.fail("walking was not given back: " + before + " -> " + after);
            RitualLock.clean(p);
            RitualLock.clean(p);
            return Result.ok("transient, not saved, lifted cleanly; speed " + before);
        } finally {
            RitualLock.release(p);
        }
    }

    // ---------------------------------------------------------------- start and cancel a ritual
    private static Result ritual(MinecraftServer server) {
        if (!AvatarConfig.enabled()) return Result.ok("skipped: the Avatar system is switched off on this server");
        if (!AvatarSoulForgeConfig.soulForgeUi()) return Result.ok("skipped: creationUi is classic");
        ServerPlayer p = SelfTests.player(server.overworld(), "AvatarForgeRitual", 0.5, 100.0, 0.5);
        SoulForgeRitual.clearAll();
        try {
            p.getInventory().clearContent();
            SoulForgeRitual.interact(p, p.blockPosition());
            if (SoulForgeRitual.of(p) != null) return Result.fail("a ritual started without a Genesis Core");
            if (RitualLock.locked(p)) return Result.fail("the player was locked without a ritual");
            p.getInventory().add(AvatarItems.coreStack());
            SoulForgeRitual.interact(p, p.blockPosition());
            if (SoulForgeRitual.of(p) == null) return Result.fail("no ritual started for a player holding a Genesis Core (limit of Avatars reached?)");
            if (!RitualLock.locked(p)) return Result.fail("the ritual did not lock the player");
            if (SoulForgeRitual.withScreenOpen(p) != null) return Result.fail("the screen is open before the circle has charged");
            SoulForgeRitual.cancel(p, null);
            if (SoulForgeRitual.of(p) != null || SoulForgeRitual.active(p)) return Result.fail("the ritual is still there after a cancel");
            if (RitualLock.locked(p)) return Result.fail("the player is still locked after a cancel");
            if (!AvatarItems.GENESIS_CORE.isBound() || p.getInventory().countItem(AvatarItems.GENESIS_CORE.get()) != 1) return Result.fail("the Genesis Core was taken by a cancelled ritual");
            return Result.ok("started with a core, refused without, cancelled with nothing taken");
        } finally {
            SoulForgeRitual.cancel(p, null);
            RitualLock.release(p);
            p.getInventory().clearContent();
            SoulForgeRitual.clearAll();
        }
    }
}
