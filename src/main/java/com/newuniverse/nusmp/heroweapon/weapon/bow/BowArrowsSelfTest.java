package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.heroweapon.HeroComponents;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: the real arrows of the Legendary Bow cannot be played by their author, so the server smoke test runs this self-test in the real game
 * ({@code hero.bow.arrows}). It checks the paper (every bow form has a row in the arrow table, the arrow entity type is registered and is in the
 * {@code minecraft:arrows} tag), then the play: a stand-in player is bonded to the Bow, a husk stands in front of them, an arrow is fired at it and its hit is
 * made; BOTH parts of the damage must have landed (a physical hit and a magic hit, and the husk lost health), the arrow must hold no pickup, and then more
 * arrows than the per-wielder cap are asked for: exactly the cap may exist, the rest are refused, and every place is given back when the arrows go. With
 * {@code heroweapon.bow.realArrows} off, the self-test only checks that no arrow is spawned. Everything it spawned is removed and the bond is released.
 */
final class BowArrowsSelfTest {
    private BowArrowsSelfTest() {}

    private static final String PLAYER = "hsb_bow_arrow_tester";

    static SelfTests.Result run(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        for (HeroForm f : HeroForms.of(HeroKind.BOW)) if (!BowArrowForms.has(f.id())) bad.add("the bow form " + f.id() + " has no arrow row");
        if (!HeroArrowEntities.HERO_ARROW.get().is(EntityTypeTags.ARROWS)) bad.add("the hero arrow is not in the minecraft:arrows tag");
        if (!bad.isEmpty()) return fail(bad);

        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        if (!level.hasChunkAt(spawn)) return SelfTests.Result.ok("paper only: the spawn chunk is not loaded");
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
        ServerPlayer fp = SelfTests.player(level, PLAYER, spawn.getX() + 0.5, ground, spawn.getZ() + 0.5);
        if (HeroBonds.of(fp).isBound()) HeroBonds.release(fp);
        String refused = HeroBonds.bind(fp, HeroKind.BOW);
        if (refused != null) return SelfTests.Result.ok("paper only: the stand-in could not bind (" + refused + ")");
        Husk dummy = null;
        List<HeroArrow> fired = new ArrayList<>();
        try {
            HeroBond bond = HeroBonds.of(fp);
            bond.setSummoned(true);
            ItemStack stack = HeroItems.stack(HeroKind.BOW);
            stack.set(HeroComponents.OWNER.get(), fp.getStringUUID());
            fp.setItemInHand(InteractionHand.MAIN_HAND, stack);
            fp.setYRot(0f);
            fp.setXRot(0f);
            HeroForm form = new HeroForm(HeroKind.BOW, "selftest", "Self test", "", 1, List.of(), List.of(), 0, Map.of("attack_damage", 4.0), Map.of(), Map.of(), new ArrayList<>(BowIds.PASSIVES), "base", false, null, 0);
            HeroCtx c = new HeroCtx(fp, bond, form, stack);

            dummy = EntityType.HUSK.create(level);
            if (dummy == null) return fail(List.of("could not create the dummy"));
            dummy.moveTo(fp.getX(), ground, fp.getZ() + 6.0, 180f, 0f);
            dummy.setNoAi(true);
            dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);           // a body that two hits cannot kill, so both parts can be read
            dummy.setHealth(200.0f);
            level.addFreshEntity(dummy);

            int baseline = ArrowBudget.count(fp.getUUID());
            Vec3 muzzle = BowFx.muzzle(c, c.look());
            if (!HeroConfig.realArrows()) {
                HeroArrow none = BowArrows.fire(c, muzzle, c.look(), 3.0f, 0.0f, 5.0, ArrowKind.LIGHT, 0);
                if (none != null) {
                    none.discard();
                    return fail(List.of("realArrows is off but an arrow was spawned"));
                }
                return SelfTests.Result.ok("realArrows is off: no arrow is spawned (the hitscan bow)");
            }

            // one arrow, both parts of the damage
            HeroArrow a = BowArrows.fire(c, muzzle, dummy.getBoundingBox().getCenter().subtract(muzzle), 3.0f, 0.0f, 5.0, ArrowKind.LIGHT, 0);
            if (a == null) return fail(List.of("an arrow was refused with no arrows out"));
            fired.add(a);
            if (a.pickup != AbstractArrow.Pickup.DISALLOWED) bad.add("the hero arrow can be picked up");
            if (ArrowBudget.count(fp.getUUID()) != baseline + 1) bad.add("firing did not take one place in the budget");
            float before = dummy.getHealth();
            a.strike(dummy, dummy.position());
            if (!(a.lastPhysical > 0f)) bad.add("the physical part of the arrow did not land (" + a.lastPhysical + ")");
            if (HeroConfig.bowMagicShareScale() > 0.0 && !(a.lastMagic > 0f)) bad.add("the magic part of the arrow did not land (" + a.lastMagic + ")");
            if (!(dummy.getHealth() < before)) bad.add("the husk lost no health");
            if (!a.isRemoved()) bad.add("an arrow that struck its foe for good did not end");
            if (ArrowBudget.count(fp.getUUID()) != baseline) bad.add("the place of a finished arrow was not given back");
            fired.clear();

            // the cap: exactly the per-wielder cap may exist, the rest are refused, and every place comes back
            int cap = HeroConfig.bowArrowsPerPlayer();
            int made = 0;
            Vec3 up = new Vec3(0.0, 1.0, 0.0);
            for (int i = 0; i < cap + 3; i++) {
                HeroArrow x = BowArrows.fire(c, muzzle.add(0.0, 2.0, 0.0), up, 0.2f, 0.0f, 1.0, ArrowKind.LIGHT, BowArrows.STRAIGHT);
                if (x != null) {
                    fired.add(x);
                    made++;
                }
            }
            int expected = cap - baseline;
            if (made != expected && ArrowBudget.total() < HeroConfig.bowArrowsGlobal() - cap) bad.add("asked for " + (cap + 3) + " arrows, got " + made + ", expected " + expected + " (cap " + cap + ")");
            if (ArrowBudget.count(fp.getUUID()) > cap) bad.add("the per-wielder cap was passed");
            for (HeroArrow x : fired) x.discard();
            fired.clear();
            if (ArrowBudget.count(fp.getUUID()) != baseline) bad.add("the places were not given back after removing the arrows (" + ArrowBudget.count(fp.getUUID()) + ")");

            if (!bad.isEmpty()) return fail(bad);
            return SelfTests.Result.ok(BowArrowForms.count() + " form rows; one arrow dealt " + a.lastPhysical + " physical and " + a.lastMagic + " magic; cap " + cap + " held");
        } finally {
            for (HeroArrow x : fired) x.discard();
            if (dummy != null) dummy.discard();
            fp.removeAllEffects();
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            fp.setPos(spawn.getX() + 0.5, ground, spawn.getZ() + 0.5);
            HeroBonds.release(fp);
        }
    }

    private static SelfTests.Result fail(List<String> bad) {
        return SelfTests.Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))) + (bad.size() > 6 ? " (+" + (bad.size() - 6) + " more)" : ""));
    }
}
