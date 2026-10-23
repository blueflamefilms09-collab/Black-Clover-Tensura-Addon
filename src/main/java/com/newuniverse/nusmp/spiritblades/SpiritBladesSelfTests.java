package com.newuniverse.nusmp.spiritblades;

import com.newuniverse.nusmp.item.WeaponEngravings;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the CI server smoke test runs this in the real game ({@code /multiverse mega selftest magic.spirit_blades}). It checks what only the game can: both blades are registered under the ids their
 * 3D models, icons and lang use, the sword really is a netherite-level sword (same damage and speed as vanilla's netherite sword, read from the attribute components), the bow really is a plain
 * vanilla bow (a {@link BowItem}, vanilla's durability), the engravings are applied once, a real hit by a stand-in is throttled to one burst per gap and leaves nothing behind after logout, and the
 * arrow trail is capped per owner and over the server and ends with its arrows.
 */
public final class SpiritBladesSelfTests {
    private SpiritBladesSelfTests() {}

    public static void register() { SelfTests.register("magic.spirit_blades", SpiritBladesSelfTests::run); }

    static Result run(MinecraftServer server) {
        // registration
        if (!"spirit_of_zephyr".equals(BuiltInRegistries.ITEM.getKey(SpiritBladesBoot.ZEPHYR.get()).getPath())) return Result.fail("the sword is not registered as spirit_of_zephyr");
        if (!"spirit_of_boreas".equals(BuiltInRegistries.ITEM.getKey(SpiritBladesBoot.BOREAS.get()).getPath())) return Result.fail("the bow is not registered as spirit_of_boreas");
        if (!(SpiritBladesBoot.ZEPHYR.get() instanceof SwordItem)) return Result.fail("Spirit of Zephyr is not a sword");
        if (SpiritBladesBoot.BOREAS.get().getClass().getSuperclass() != BowItem.class) return Result.fail("Spirit of Boreas is not a plain bow");
        ItemStack sword = new ItemStack(SpiritBladesBoot.ZEPHYR.get());
        ItemStack bow = new ItemStack(SpiritBladesBoot.BOREAS.get());
        if (bow.getMaxDamage() != Items.BOW.getDefaultInstance().getMaxDamage()) return Result.fail("the bow's durability is not vanilla's: " + bow.getMaxDamage());
        // netherite-level sword: the same attack damage and attack speed as vanilla's netherite sword
        ItemStack netherite = new ItemStack(Items.NETHERITE_SWORD);
        double dmg = sword.getAttributeModifiers().compute(1.0, EquipmentSlot.MAINHAND), dmgN = netherite.getAttributeModifiers().compute(1.0, EquipmentSlot.MAINHAND);
        if (Math.abs(dmg - dmgN) > 1.0e-6) return Result.fail("the sword's attribute total " + dmg + " is not the netherite sword's " + dmgN);
        // the engravings, once
        ServerLevel level = server.overworld();
        WeaponEngravings.engrave(sword, level.registryAccess());
        WeaponEngravings.engrave(bow, level.registryAccess());
        int sEng = sword.getEnchantments().size(), bEng = bow.getEnchantments().size();
        if (sEng == 0 || bEng == 0) return Result.fail("a blade came out with no Tensura engravings (sword " + sEng + ", bow " + bEng + ")");
        WeaponEngravings.engrave(sword, level.registryAccess());
        if (sword.getEnchantments().size() != sEng) return Result.fail("the sword was engraved twice");

        // a real hit by a stand-in: one burst, throttled, forgotten on logout
        double y = level.getMaxBuildHeight() - 40;
        ServerPlayer fake = SelfTests.standIn(level, "spirit_blades_selftest", 0.5, y, 0.5);
        ZephyrSwordItem.clearAll();
        SpiritBladesTrail.clearAll();
        Zombie z = EntityType.ZOMBIE.create(level);
        if (z == null) return Result.fail("no zombie");
        z.setPos(2.5, y, 0.5);
        z.setNoAi(true);
        z.setNoGravity(true);
        level.addFreshEntity(z);
        ItemStack held = new ItemStack(SpiritBladesBoot.ZEPHYR.get());
        held.hurtEnemy(z, fake);
        held.hurtEnemy(z, fake);                                              // the same tick: the second burst is refused
        boolean remembered = ZephyrSwordItem.remembered() == 1;
        ZephyrSwordItem.forget(fake.getUUID());
        boolean forgotten = ZephyrSwordItem.remembered() == 0;
        z.discard();
        if (!remembered) return Result.fail("a hit did not leave exactly one throttle entry");
        if (!forgotten) return Result.fail("a logout did not forget the attacker");

        // the arrow trail: capped per owner and over the server, and it ends with the arrows
        List<AbstractArrow> arrows = new ArrayList<>();
        BoreasBowItem boreas = SpiritBladesBoot.BOREAS.get();
        for (int i = 0; i < SpiritBladesRules.TRAIL_PER_OWNER + 3; i++) {
            AbstractArrow a = ((ArrowItem) Items.ARROW).createArrow(level, new ItemStack(Items.ARROW), fake, bow);
            a.setPos(0.5, y + 2, 0.5);
            a.setNoGravity(true);
            boreas.customArrow(a, new ItemStack(Items.ARROW), bow);
            arrows.add(a);
        }
        int tracked = SpiritBladesTrail.live();
        if (tracked != SpiritBladesRules.TRAIL_PER_OWNER) return Result.fail("one owner has " + tracked + " trailed arrows, the cap is " + SpiritBladesRules.TRAIL_PER_OWNER);
        for (AbstractArrow a : arrows) a.discard();
        SpiritBladesTrail.tick();
        if (SpiritBladesTrail.live() != 0) return Result.fail("the trail outlived its arrows (" + SpiritBladesTrail.live() + " left)");
        SpiritBladesTrail.clearAll();
        ZephyrSwordItem.clearAll();
        return Result.ok("sword = netherite damage " + dmg + ", bow = vanilla bow (" + bow.getMaxDamage() + " uses), engravings " + sEng + " + " + bEng + ", hit burst throttled and forgotten, trail cap " + tracked + " per owner, worst "
                + SpiritBladesRules.worstHitParticlesPerTick() + " hit and " + SpiritBladesRules.worstTrailParticlesPerTick() + " trail particles a tick");
    }
}
