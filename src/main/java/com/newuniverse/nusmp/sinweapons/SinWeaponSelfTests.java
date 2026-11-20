package com.newuniverse.nusmp.sinweapons;

import com.newuniverse.nusmp.item.WeaponEngravings;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import com.newuniverse.nusmp.vfx.VfxShape;

import java.util.function.Supplier;

/**
 * Why: nothing of the five Berserk weapons can be played by their author, so what can be proved without a person is proved by the real game: the CI server smoke test runs {@code berserk.weapons} on a dedicated
 * server with a stand-in player. It checks that the five items are registered under the ids their models use, that each is engraved once with Tensura engravings, that each technique hits a zombie ahead of the
 * wielder (Sloth slows it, Envy takes one target only) and never its wielder, and that a slam with no one near costs nothing. No client class.
 */
public final class SinWeaponSelfTests {
    private SinWeaponSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("berserk.weapons", SinWeaponSelfTests::weapons);
    }

    private static SinWeaponItem itemOf(SinRules.Kind k) {
        return (SinWeaponItem) switch (k) { case SLOTH -> SinWeaponBoot.SLOTH.get(); case ENVY -> SinWeaponBoot.ENVY.get(); case VANITY -> SinWeaponBoot.VANITY.get(); case CHAKRAMS -> SinWeaponBoot.CHAKRAMS.get(); case HOLY -> SinWeaponBoot.HOLY.get(); };
    }

    /** The zombie 3.5 blocks ahead of the stand-in: the same one while it lives, a fresh one (the holy techniques may kill it, x1.5 against the undead) when it died. */
    private static Zombie alive(ServerLevel level, Zombie dummy, double y) {
        if (dummy.isAlive()) return dummy;
        dummy.discard();
        Zombie fresh = EntityType.ZOMBIE.create(level);
        if (fresh == null) return dummy;
        fresh.setNoAi(true);
        fresh.setPos(0.5, y, 3.5);
        level.addFreshEntity(fresh);
        return fresh;
    }

    private static Result weapons(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (Supplier<? extends Item> s : SinWeaponBoot.renderables()) {
            Item item = s.get();
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            if (!SinRules.IDS.contains(path) || !(item instanceof SinWeaponItem)) return Result.fail(path + " is not a registered Berserk weapon");
            ItemStack stack = new ItemStack(item);
            WeaponEngravings.engrave(stack, level.registryAccess());
            int n = stack.getEnchantments().size();
            if (n == 0) return Result.fail(path + " came out with no Tensura engravings");
            WeaponEngravings.engrave(stack, level.registryAccess());
            if (stack.getEnchantments().size() != n) return Result.fail(path + " was engraved twice");
        }
        double y = level.getMaxBuildHeight() - 40;
        ServerPlayer fake = SelfTests.standIn(level, "berserk_weapons", 0.5, y, 0.5);
        fake.setYRot(0f);
        fake.setXRot(0f);
        Zombie dummy = EntityType.ZOMBIE.create(level);
        Zombie second = EntityType.ZOMBIE.create(level);
        if (dummy == null || second == null) return Result.fail("no zombie");
        try {
            dummy.setNoAi(true);
            second.setNoAi(true);
            dummy.setPos(0.5, y, 3.5);
            second.setPos(0.5, y, 6.5);
            level.addFreshEntity(dummy);
            level.addFreshEntity(second);
            for (SinRules.Kind k : SinRules.Kind.values()) {
                SinWeaponItem item = (SinWeaponItem) switch (k) { case SLOTH -> SinWeaponBoot.SLOTH.get(); case ENVY -> SinWeaponBoot.ENVY.get(); case VANITY -> SinWeaponBoot.VANITY.get(); case CHAKRAMS -> SinWeaponBoot.CHAKRAMS.get(); case HOLY -> SinWeaponBoot.HOLY.get(); };
                dummy.setHealth(dummy.getMaxHealth());
                second.setHealth(second.getMaxHealth());
                dummy.invulnerableTime = 0;
                second.invulnerableTime = 0;
                dummy.removeAllEffects();
                dummy.setRemainingFireTicks(0);
                float hp = fake.getHealth();
                boolean used = item.technique(fake);
                if (!used) return Result.fail(k.technique + " did nothing with a target ahead");
                if (fake.getHealth() < hp) return Result.fail(k.technique + " hurt its wielder");
                if (k == SinRules.Kind.SLOTH) {
                    if (dummy.getActiveEffects().isEmpty()) return Result.fail("Dead Weight did not slow the zombie 3.5 blocks ahead");
                    if (!second.getActiveEffects().isEmpty()) return Result.fail("Dead Weight reached 6.5 blocks (the radius is " + k.reach + ")");
                } else if (dummy.getHealth() >= dummy.getMaxHealth()) {
                    return Result.fail(k.technique + " did not hurt the zombie ahead");
                }
                if (k == SinRules.Kind.ENVY && second.getHealth() < second.getMaxHealth()) return Result.fail("Green-Eyed Shot hurt a second target");
                if (!dummy.isAlive() && k != SinRules.Kind.HOLY) return Result.fail(k.technique + " killed a full-health zombie in one use");     // the holy sword's 1.5x against the undead may
            }
            // the extra techniques (docs/sin_weapons_abilities.md): each hurts or marks the zombie ahead, never its wielder, and none kills a full-health zombie outright (but the holy ones, x1.5 against the undead)
            for (SinRules.Ability a : SinRules.Ability.values()) {
                SinWeaponItem item = itemOf(a.kind);
                dummy = alive(level, dummy, y);
                dummy.setHealth(dummy.getMaxHealth());
                dummy.invulnerableTime = 0;
                dummy.removeAllEffects();
                dummy.setRemainingFireTicks(0);
                float hp = fake.getHealth();
                if (!item.runAbility(fake, a)) return Result.fail(a.english + " did nothing with a target ahead");
                if (fake.getHealth() < hp) return Result.fail(a.english + " hurt its wielder");
                if (a.damage > 0f) {
                    if (dummy.isAlive() && dummy.getHealth() >= dummy.getMaxHealth()) return Result.fail(a.english + " did not hurt the zombie ahead");
                } else if (dummy.getActiveEffects().isEmpty()) {
                    return Result.fail(a.english + " left no effect on the zombie ahead");
                }
                if (!dummy.isAlive() && a.kind != SinRules.Kind.HOLY) return Result.fail(a.english + " killed a full-health zombie in one use");
            }
            // each extra technique has its own clock: a second use at once is refused, the right click's item cooldown is not touched
            SinWeaponItem axe = itemOf(SinRules.Kind.SLOTH);
            fake.getPersistentData().remove(SinWeaponItem.abilityKey(SinRules.Kind.SLOTH, 2));
            dummy = alive(level, dummy, y);
            dummy.setHealth(dummy.getMaxHealth());
            dummy.invulnerableTime = 0;
            if (!axe.ability(fake, 2)) return Result.fail("Crushing Gravity did not run through its own clock");
            if (axe.ability(fake, 2)) return Result.fail("Crushing Gravity ran twice inside its own cooldown");
            if (fake.getCooldowns().isOnCooldown(axe)) return Result.fail("an extra technique paid the item cooldown of the right click");
            // the themed effects: every weapon and style goes out through the effect budget without throwing, and the power round-trips
            Vec3 at = new Vec3(0.5, y + 1.0, 0.5);
            for (SinRules.Kind k : SinRules.Kind.values()) {
                for (int style = 0; style <= SinVfx.STEAL; style++) {
                    SinFx.send(level, k, style, at, at.add(0, 0, 5));
                    float power = SinVfx.power(k, style);
                    if (SinVfx.kindOf(power) != k.ordinal() || SinVfx.styleOf(power) != style) return Result.fail("the effect power of " + k.id + " style " + style + " does not round-trip");
                }
            }
            if (VfxShape.SIN_WEAPON.ordinal() <= VfxShape.GIANT_AVATAR.ordinal()) return Result.fail("SIN_WEAPON is not appended after GIANT_AVATAR");
            dummy.setPos(0.5, y, 40.5);
            second.setPos(0.5, y, 40.5);
            if (SinWeaponBoot.SLOTH.get().technique(fake)) return Result.fail("Dead Weight with nobody near still paid its cooldown");
            if (itemOf(SinRules.Kind.SLOTH).runAbility(fake, SinRules.Ability.SLOTH_STILLNESS)) return Result.fail("Stillness with nobody near still paid its cooldown");
            if (!itemOf(SinRules.Kind.VANITY).runAbility(fake, SinRules.Ability.VANITY_REFLECT)) return Result.fail("a Reflected Thrust at nothing should still cost, like a thrust at nothing");
            return Result.ok("five items registered and engraved once, every technique hits the zombie ahead and not its wielder, Sloth slows within its radius only, Envy hits one target; the ten extra techniques hurt or mark the zombie, run on their own clocks, and the themed effects go out");
        } finally {
            dummy.discard();
            second.discard();
            for (SinRules.Kind k : SinRules.Kind.values()) for (int slot = 1; slot <= SinRules.ABILITIES_PER_KIND; slot++) fake.getPersistentData().remove(SinWeaponItem.abilityKey(k, slot));
        }
    }
}
