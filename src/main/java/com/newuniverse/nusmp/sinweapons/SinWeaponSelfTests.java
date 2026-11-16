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
            dummy.setPos(0.5, y, 40.5);
            second.setPos(0.5, y, 40.5);
            if (SinWeaponBoot.SLOTH.get().technique(fake)) return Result.fail("Dead Weight with nobody near still paid its cooldown");
            return Result.ok("five items registered and engraved once, every technique hits the zombie ahead and not its wielder, Sloth slows within its radius only, Envy hits one target");
        } finally {
            dummy.discard();
            second.discard();
        }
    }
}
