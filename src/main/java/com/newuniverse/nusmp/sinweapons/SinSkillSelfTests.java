package com.newuniverse.nusmp.sinweapons;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.SinWeaponSkill;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Why: the five Berserk weapon skills cannot be played by their author, so what can be proved without a person is proved by the real game: the CI server smoke test runs {@code berserk.skills} on a dedicated server
 * with a stand-in player. It checks that the five skills are registered as Lord class NUSkills with two modes, that a carried unbound weapon is bound to its carrier and gives its skill once (a second one stays
 * unbound, another player's stack is never touched or taken), that the technique gate follows the setting {@code soulBound}, that the Technique mode fires like the right click (a zombie ahead is slowed and the item
 * cooldown is paid), and that Manifest / Store stores the held bound weapon, waits out its cooldown, forges one bound weapon and never a second while one is carried. No client class.
 */
public final class SinSkillSelfTests {
    private SinSkillSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("berserk.skills", SinSkillSelfTests::skills);
    }

    private static int count(Inventory inv, SinRules.Kind k) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).getItem() instanceof SinWeaponItem w && w.kind() == k) n++;
        return n;
    }

    private static Result skills(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (SinRules.Kind k : SinRules.Kind.values()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("anime_multiverse", SinSkillRules.skillId(k));
            ManasSkill skill = SkillAPI.getSkillRegistry().get(id);
            if (!(skill instanceof SinWeaponSkill s) || s.sinKind() != k) return Result.fail(id + " is not the registered skill of " + k.id);
            if (s.ultimateClass() != UltimateClass.LORD) return Result.fail(id + " is not a Lord class Ultimate");
            if (s.getModes(s.createDefaultInstance()) != 4) return Result.fail(id + " has not four modes (technique, manifest, two extra techniques)");
        }
        double y = level.getMaxBuildHeight() - 40;
        ServerPlayer fake = SelfTests.standIn(level, "berserk_skills", 0.5, y, 0.5);
        ServerPlayer other = SelfTests.standIn(level, "berserk_skills_other", 40.5, y, 40.5);
        fake.setYRot(0f);
        fake.setXRot(0f);
        Zombie dummy = EntityType.ZOMBIE.create(level);
        if (dummy == null) return Result.fail("no zombie");
        Inventory inv = fake.getInventory();
        SinRules.Kind sloth = SinRules.Kind.SLOTH, envy = SinRules.Kind.ENVY;
        try {
            cleanup(fake);
            // an unbound stack the player holds binds to its carrier and gives the skill once
            ItemStack axe = new ItemStack(SinWeaponBoot.SLOTH.get());
            fake.setItemInHand(InteractionHand.MAIN_HAND, axe);
            if (SinSkillService.ownerText(axe) != null) return Result.fail("a fresh stack was not unbound");
            SinSkillService.scan(fake);
            if (!fake.getUUID().toString().equals(SinSkillService.ownerText(axe))) return Result.fail("the carried stack was not bound to its carrier");
            if (!SinSkillService.has(fake, sloth)) return Result.fail("binding did not give the skill of the weapon");
            if (SinSkillService.has(fake, envy)) return Result.fail("binding the axe gave the skill of another weapon");
            // a second unbound stack of the same weapon stays unbound (one bound weapon per player per kind)
            ItemStack second = new ItemStack(SinWeaponBoot.SLOTH.get());
            inv.setItem(10, second);
            SinSkillService.scan(fake);
            if (SinSkillService.ownerText(second) != null) return Result.fail("a second axe was bound to a carrier who has one");
            inv.setItem(10, ItemStack.EMPTY);
            // a stack bound to another player is never touched, never taken, and does not give the skill
            ItemStack foreign = new ItemStack(SinWeaponBoot.ENVY.get());
            SinSkillService.bindStack(foreign, other);
            inv.setItem(11, foreign);
            SinSkillService.scan(fake);
            if (!other.getUUID().toString().equals(SinSkillService.ownerText(foreign))) return Result.fail("another player's stack was rebound");
            if (count(inv, envy) != 1) return Result.fail("another player's stack was taken");
            if (SinSkillService.has(fake, envy)) return Result.fail("carrying another player's weapon gave its skill");
            boolean bound = SinWeaponConfig.soulBound();
            if (SinSkillService.mayUse(fake, foreign) == bound) return Result.fail("the technique gate of a foreign stack does not follow soulBound=" + bound);
            if (!SinSkillService.mayUse(fake, axe)) return Result.fail("the owner cannot use the technique of their own weapon");
            inv.setItem(11, ItemStack.EMPTY);

            // the Technique mode fires like the right click: the zombie ahead is slowed and the item cooldown is paid
            dummy.setNoAi(true);
            dummy.setPos(0.5, y, 3.5);
            level.addFreshEntity(dummy);
            dummy.removeAllEffects();
            SinSkillService.technique(fake, sloth);
            if (dummy.getActiveEffects().isEmpty()) return Result.fail("the Technique mode did not slow the zombie ahead");
            if (!fake.getCooldowns().isOnCooldown(SinWeaponBoot.SLOTH.get())) return Result.fail("the Technique mode did not pay the weapon's cooldown");
            // the first extra technique (mode 3, Stillness): it freezes the zombie ahead, runs on its own clock and not twice in a row
            dummy.removeAllEffects();
            SinSkillService.ability(fake, sloth, 1);
            if (dummy.getActiveEffects().isEmpty()) return Result.fail("the extra technique Stillness did not freeze the zombie ahead");
            dummy.removeAllEffects();
            SinSkillService.ability(fake, sloth, 1);
            if (!dummy.getActiveEffects().isEmpty()) return Result.fail("Stillness ran twice inside its own cooldown");
            dummy.discard();

            // Manifest / Store: the held bound weapon is stored (stack removed), the cooldown holds the next forge, then one bound weapon is forged and not a second
            SinSkillService.manifest(fake, sloth);
            if (count(inv, sloth) != 0 || !fake.getMainHandItem().isEmpty()) return Result.fail("Manifest did not store the held weapon");
            SinSkillService.manifest(fake, sloth);
            if (count(inv, sloth) != 0) return Result.fail("Manifest forged again inside its cooldown");
            fake.getPersistentData().putLong(SinSkillService.forgeKey(sloth), 0L);
            SinSkillService.manifest(fake, sloth);
            ItemStack forged = fake.getMainHandItem();
            if (count(inv, sloth) != 1 || !(forged.getItem() instanceof SinWeaponItem)) return Result.fail("Manifest did not forge one weapon into the empty hand");
            if (!fake.getUUID().toString().equals(SinSkillService.ownerText(forged))) return Result.fail("the forged weapon is not bound to its owner");
            fake.getPersistentData().putLong(SinSkillService.forgeKey(sloth), 0L);
            fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            inv.setItem(13, forged);
            SinSkillService.manifest(fake, sloth);
            if (count(inv, sloth) != 1) return Result.fail("Manifest forged a second weapon while one was carried");
            return Result.ok("five Ultimate skills registered with four modes, an unbound weapon binds once and gives its skill, a foreign stack is untouched, the technique gate follows soulBound=" + bound
                    + ", Technique fires like the right click, an extra technique runs on its own clock, Manifest stores, waits and forges one bound weapon");
        } finally {
            dummy.discard();
            cleanup(fake);
            cleanup(other);
        }
    }

    /** Leaves the stand-ins as they were found: no weapons, no forge clocks, none of the five skills. */
    private static void cleanup(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).getItem() instanceof SinWeaponItem) inv.setItem(i, ItemStack.EMPTY);
        Skills skills;
        try {
            skills = SkillAPI.getSkillsFrom(p);
        } catch (RuntimeException noSkills) {
            skills = null;
        }
        for (SinRules.Kind k : SinRules.Kind.values()) {
            p.getPersistentData().remove(SinSkillService.forgeKey(k));
            for (int slot = 1; slot <= SinRules.ABILITIES_PER_KIND; slot++) p.getPersistentData().remove(SinWeaponItem.abilityKey(k, slot));
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("anime_multiverse", SinSkillRules.skillId(k));
            if (skills != null && skills.getSkill(id).isPresent()) skills.forgetSkill(id, Component.literal("self-test"));
        }
    }
}
