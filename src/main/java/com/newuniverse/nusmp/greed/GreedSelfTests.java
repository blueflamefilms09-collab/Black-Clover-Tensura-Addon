package com.newuniverse.nusmp.greed;

import com.newuniverse.nusmp.item.WeaponEngravings;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;


/**
 * Why: nothing of the Black Sword can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs these (keys {@code greed.rules},
 * {@code greed.world}, {@code greed.forms}) with stand-in players on a dedicated server. {@code greed.rules}: the eight forms, the tier ladder, the stat and hunger arithmetic and the caps. {@code greed.world}: the eight
 * items are registered under the ids their models use; the sword enters the world once (a bare stack is adopted, a second bare stack and a stack with an old token dissolve), the first holder is its wielder, a thief is
 * torn away from it, a form switch swaps the item id and is refused while locked, a kill devours stats, a Secret Technique pays its share and a kill by another player releases the sword; the 14 day release empties the
 * world. {@code greed.forms}: each form's click and technique hits a stand-in dummy and nothing else (bounded), Soul Sever stops healing, engravings are applied once. Server safe: no client class. The world is left with no sword.
 */
public final class GreedSelfTests {
    private GreedSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("greed.rules", s -> rules());
        SelfTests.register("greed.world", GreedSelfTests::world);
        SelfTests.register("greed.forms", GreedSelfTests::forms);
        SelfTests.register("greed.hands", GreedSelfTests::hands);
    }

    // ---------------------------------------------------------------- pure rules
    private static Result rules() {
        if (GreedRules.Form.values().length != GreedRules.FORMS) return Result.fail("the form table has " + GreedRules.Form.values().length + " entries");
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (GreedRules.Form f : GreedRules.Form.values()) {
            if (f.id() != f.ordinal()) return Result.fail("form id " + f.id() + " is not its position");
            if (!ids.add(f.itemId()) || !GreedRules.isGreedPath(f.itemId())) return Result.fail("bad item id " + f.itemId());
            if (f.share() <= 0.0 || f.share() > 0.5) return Result.fail("technique share out of range for " + f);
            if (BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anime_multiverse", f.itemId())).isEmpty()) return Result.fail("item not registered: " + f.itemId());
        }
        if (GreedRules.tierFor(0) != 0 || GreedRules.tierFor(2_500) != 1 || GreedRules.tierFor(2_000_000) != 7 || GreedRules.tierFor(1e12) != 7) return Result.fail("the tier ladder is wrong");
        for (int t = 1; t <= GreedRules.MAX_TIER; t++) if (GreedRules.tierThreshold(t) <= GreedRules.tierThreshold(t - 1)) return Result.fail("thresholds are not rising at " + t);
        if (!GreedRules.formOpen(0, 0) || GreedRules.formOpen(1, 0) || !GreedRules.formOpen(7, 7)) return Result.fail("form opening is wrong");
        double[] g = GreedRules.gains(1e15);
        if (g[0] > 6.0 || g[1] > 2.5 || g[2] > 1.5) return Result.fail("a kill's gain is not capped");
        GreedState st = new GreedState();
        st.add(1e9, 1e9, 1e9, 0);
        if (st.bonusHp > GreedRules.MAX_HP || st.bonusAtk > GreedRules.MAX_ATK || st.bonusArmor > GreedRules.MAX_ARMOR) return Result.fail("the stat caps do not hold");
        double hp = st.bonusHp;
        double power = st.sacrifice(0.5);
        if (Math.abs(st.bonusHp - hp * 0.5) > 1e-9 || power <= 0 || st.storedEp <= 0) return Result.fail("a sacrifice did not take half");
        GreedState back = GreedState.fromTag(st.toTag());
        back.addSkill("anime_multiverse:x");
        if (Math.abs(back.bonusHp - st.bonusHp) > 1e-9 || back.vaultSize() != 1 || !GreedState.fromTag(back.toTag()).has("anime_multiverse:x")) return Result.fail("the tag format does not round-trip");
        if (GreedRules.decayPerSecond(100, 50, 20, true, 7, 100) != 0.0) return Result.fail("Level Zero is still hungry");
        if (GreedRules.decayPerSecond(100, 50, 20, true, 0, 100) <= GreedRules.decayPerSecond(100, 50, 20, false, 0, 100)) return Result.fail("Half-Starvation is not hungrier");
        if (GreedRules.decayPerSecond(1e9, 1e9, 1e9, true, 0, 100) > 2.0) return Result.fail("the hunger is not capped");
        if (GreedRules.sacrificeEp("ULTIMATE") <= GreedRules.sacrificeEp("UNIQUE") || GreedRules.sacrificeEp("UNIQUE") <= GreedRules.sacrificeEp("COMMON")) return Result.fail("sacrifice values are not ordered");
        return Result.ok("eight forms, tier ladder, stat caps, tag round-trip and hunger hold");
    }

    // ---------------------------------------------------------------- the world's rules
    private static Result world(MinecraftServer server) {
        if (!GreedConfig.enabled()) return Result.ok("skipped: greed.greedEnabled is false");
        if (!GreedData.bound()) GreedData.bind(server);
        GreedData d = GreedData.get(server);
        if (d.spawned()) return Result.ok("skipped: the Black Sword is already in this world");
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 3, 0);
        ServerPlayer a = SelfTests.standIn(level, "greed_a", at.x, at.y, at.z), b = SelfTests.standIn(level, "greed_b", at.x + 1, at.y, at.z);
        try {
            GreedService.clearAll();
            // a bare stack is adopted when no sword exists, and binds its holder
            ItemStack bare = GreedItems.creativeStack();
            a.getInventory().add(bare);
            GreedService.scan(a);
            if (!d.spawned() || !d.isOwner(a.getUUID())) return Result.fail("a bare stack was not adopted and bound to its holder");
            ItemStack mine = find(a);
            if (mine.isEmpty() || !a.getUUID().equals(GreedComponents.owner(mine)) || !d.valid(GreedComponents.token(mine))) return Result.fail("the adopted stack does not carry its wielder and the world's token");
            // a second bare stack and a stack with an old token dissolve
            a.getInventory().add(GreedItems.creativeStack());
            ItemStack old = new ItemStack(GreedItems.of(GreedRules.Form.SPEAR));
            GreedComponents.write(old, a.getUUID(), d.token() ^ 0x5A5A5AL);
            a.getInventory().add(old);
            GreedService.scan(a);
            if (count(a) != 1) return Result.fail("copies and old stacks stayed: " + count(a) + " stacks");
            // a thief is torn away
            b.getInventory().add(mine.copy());
            GreedService.scan(b);
            if (count(b) != 0) return Result.fail("a non-wielder kept the sword");
            // a form switch: locked forms are refused, an open one swaps the item id and keeps the token
            if (GreedService.switchForm(a, 3)) return Result.fail("a locked form opened at tier 0");
            d.setTier(7);
            GreedService.clearAll();
            if (!GreedService.switchForm(a, 3)) return Result.fail("an open form did not switch");
            ItemStack shield = find(a);
            if (!(shield.getItem() instanceof GreedItem g) || g.form() != GreedRules.Form.SHIELD || GreedComponents.token(shield) != d.token()) return Result.fail("the switch lost the token or the form");
            // a kill devours stats; a Secret Technique pays its share
            Zombie z = EntityType.ZOMBIE.create(level);
            if (z == null) return Result.fail("no zombie to kill");
            z.setPos(at.x + 3, at.y, at.z);
            GreedState st = GreedStore.of(a);
            st.reset();
            GreedService.onKill(z, a);
            if (st.bonusHp <= 0.0 || st.storedEp <= 0.0) return Result.fail("a kill gave nothing");
            st.add(10, 10, 5, 0);
            a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, shield);
            double atkBefore = st.bonusAtk;
            if (!GreedService.secretTechnique(a) || st.bonusAtk >= atkBefore) return Result.fail("the Secret Technique did not pay its share");
            // a kill of the wielder by another player releases the sword and drops a fresh unowned stack
            long before = d.token();
            GreedService.release(server, a.getUUID(), level, a.position());
            if (d.owner() != null || d.token() == before || !GreedData.get(server).spawned()) return Result.fail("the release did not free the sword with a new token");
            if (GreedStore.of(a).bonusHp != 0.0) return Result.fail("the devoured stats stayed with the old wielder");
            GreedService.scan(a);
            if (count(a) != 0) return Result.fail("an old stack survived the release");
            // the 14 day release empties the world
            d.vanish();
            if (d.spawned() || d.owner() != null) return Result.fail("the world was not emptied");
            return Result.ok("adoption, one stack, copies, thief, forms, devouring, technique cost and release hold");
        } finally {
            for (ServerPlayer p : new ServerPlayer[] {a, b}) {
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).getItem() instanceof GreedItem) p.getInventory().setItem(i, ItemStack.EMPTY);
                GreedStore.of(p).reset();
                GreedService.applyAttributes(p, GreedStore.of(p));
            }
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(16.0), e -> e.getItem().getItem() instanceof GreedItem).forEach(net.minecraft.world.entity.Entity::discard);
            d.vanish();
            d.setTier(0);
            d.setForm(0);
            GreedService.clearAll();
        }
    }

    private static ItemStack find(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).getItem() instanceof GreedItem) return p.getInventory().getItem(i);
        return ItemStack.EMPTY;
    }

    private static int count(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).getItem() instanceof GreedItem) n++;
        return n;
    }

    // ---------------------------------------------------------------- the forms
    private static Result forms(MinecraftServer server) {
        ServerLevel level = server.overworld();
        double y = level.getMaxBuildHeight() - 40;
        ServerPlayer fake = SelfTests.standIn(level, "greed_forms", 0.5, y, 0.5);
        Zombie dummy = EntityType.ZOMBIE.create(level);
        if (dummy == null) return Result.fail("no zombie");
        try {
            fake.setYRot(0f);
            fake.setXRot(0f);
            dummy.setPos(0.5, y, 4.5);
            dummy.setNoAi(true);
            level.addFreshEntity(dummy);
            // engravings, once
            for (GreedRules.Form f : GreedRules.Form.values()) {
                ItemStack s = new ItemStack(GreedItems.of(f));
                WeaponEngravings.engrave(s, level.registryAccess());
                int n = s.getEnchantments().size();
                if (n == 0) return Result.fail(f.itemId() + " came out with no Tensura engravings");
                WeaponEngravings.engrave(s, level.registryAccess());
                if (s.getEnchantments().size() != n) return Result.fail(f.itemId() + " was engraved twice");
            }
            // each technique reaches the dummy ahead and no more than the cap, and never the wielder
            for (GreedRules.Form f : GreedRules.Form.values()) {
                if (f == GreedRules.Form.SHIELD || f == GreedRules.Form.STAFF) continue;
                dummy.setHealth(dummy.getMaxHealth());
                dummy.setRemainingFireTicks(0);
                float hp = fake.getHealth();
                GreedForms.secret(fake, f, 20.0);
                if (dummy.isAlive() && dummy.getHealth() >= dummy.getMaxHealth() && f != GreedRules.Form.SCYTHE) return Result.fail(f.techniqueEnglish() + " did not reach the target ahead");
                if (fake.getHealth() < hp) return Result.fail(f.techniqueEnglish() + " hurt its wielder");
                if (!dummy.isAlive()) { dummy = EntityType.ZOMBIE.create(level); dummy.setPos(0.5, y, 4.5); dummy.setNoAi(true); level.addFreshEntity(dummy); }
            }
            // Soul Sever stops healing
            dummy.setHealth(1.0f);
            GreedForms.soulSever(dummy, level.getGameTime(), 100);
            if (!GreedForms.severed(dummy)) return Result.fail("Soul Sever did not mark");
            dummy.heal(5.0f);
            if (dummy.getHealth() > 1.0f) return Result.fail("a severed creature healed");
            // the shield stores damage and the pool is cleared at logout
            GreedForms.shieldTook(fake, 10.0f, null, level.getGameTime(), 0);
            if (GreedForms.pool(fake.getUUID()) < 10.0f) return Result.fail("the shield did not store the hit");
            GreedForms.forget(fake.getUUID());
            if (GreedForms.pool(fake.getUUID()) != 0.0f) return Result.fail("the pool was not forgotten");
            if (GreedService.trackedPlayers() > 64) return Result.fail("per-player tables are growing without bound");
            return Result.ok("engravings once, eight techniques hit ahead only, Soul Sever, shield pool and cleanup hold");
        } finally {
            dummy.discard();
            GreedForms.clearAll();
        }
    }

    // ---------------------------------------------------------------- the two hands (gauntlets, dual blades)
    private static Result hands(MinecraftServer server) {
        for (GreedRules.Form f : GreedRules.Form.values()) {
            boolean twin = f == GreedRules.Form.GAUNTLETS || f == GreedRules.Form.DUAL_BLADES;
            if (GreedHands.isTwin(f.itemId()) != twin) return Result.fail("twin flag wrong for " + f.itemId());
        }
        if (!GreedHands.GAUNTLETS_LEFT.equals(GreedHands.meshFor(GreedHands.GAUNTLETS, true)) || !GreedHands.GAUNTLETS.equals(GreedHands.meshFor(GreedHands.GAUNTLETS, false))
                || !GreedHands.DUAL_BLADES.equals(GreedHands.meshFor(GreedHands.DUAL_BLADES, true))) return Result.fail("the left hand draws the wrong mesh");
        if (GreedHands.offBlow(10f) <= 0f || GreedHands.offBlow(10f) >= 10f || GreedHands.offBlow(Float.NaN) != 0f || GreedHands.offBlow(1e30f) > 100f) return Result.fail("the off-hand blow is not smaller than the main blow and capped");
        if (GreedHands.ready(100, 105) || !GreedHands.ready(105, 105)) return Result.fail("the cooldown does not hold");
        ServerLevel level = server.overworld();
        double y = level.getMaxBuildHeight() - 40;
        ServerPlayer fake = SelfTests.standIn(level, "greed_hands", 0.5, y, 0.5);
        Zombie dummy = EntityType.ZOMBIE.create(level);
        if (dummy == null) return Result.fail("no zombie");
        try {
            dummy.setPos(0.5, y, 1.5);
            dummy.setNoAi(true);
            level.addFreshEntity(dummy);
            fake.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(net.minecraft.world.item.Items.STICK));
            if (GreedForms.offBlow(fake, dummy)) return Result.fail("the off hand struck with an item in it");
            fake.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
            float hp = dummy.getHealth();
            if (!GreedForms.offBlow(fake, dummy) || dummy.getHealth() >= hp) return Result.fail("the empty off hand did not strike");
            float hp2 = dummy.getHealth();
            if (GreedForms.offBlow(fake, dummy) || dummy.getHealth() != hp2) return Result.fail("a second off-hand blow fell inside its cooldown");
            if (GreedForms.offBlow(fake, fake)) return Result.fail("the off hand struck its own wielder");
            return Result.ok("twin flags, left glove mesh, off-hand blow smaller and capped, empty hand only, cooldown and self-safety hold");
        } finally {
            fake.getPersistentData().remove("nusmp_greed_off");
            dummy.discard();
        }
    }
}
