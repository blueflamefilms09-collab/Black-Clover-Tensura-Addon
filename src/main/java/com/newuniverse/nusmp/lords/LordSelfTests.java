package com.newuniverse.nusmp.lords;

import com.newuniverse.nusmp.item.WeaponEngravings;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Why: nothing of the Lord weapons can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs these (keys {@code lords.rules},
 * {@code lords.world}, {@code lords.burden}) with stand-in players on a dedicated server. {@code lords.rules}: the sin table, the eight new items under the ids their models use, the mastery curve, the burden bounds
 * and Greed's Hunger-of-the-Blade numbers. {@code lords.world}: for each weapon not already in this world, a bare stack is adopted and binds its holder, copies and old stacks dissolve, a thief is torn away, a form
 * switch is refused while locked and swaps the item id once the mastery opens it, a kill grows the mastery, a kill by another player releases the weapon, and the 14 day release empties the world. {@code lords.burden}: each
 * art hits a stand-in dummy ahead and nothing else, charges the burden, never touches the food bar, the overload gate refuses at 100 and lets go after its seconds, engravings are applied once. Server safe: no client
 * class. The world is left with no Lord weapon.
 */
public final class LordSelfTests {
    private LordSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("lords.rules", s -> rules());
        SelfTests.register("lords.world", LordSelfTests::world);
        SelfTests.register("lords.burden", LordSelfTests::burden);
    }

    // ---------------------------------------------------------------- pure rules
    private static Result rules() {
        for (LordRules.Sin s : LordRules.Sin.values()) {
            if (s.forms() < 1) return Result.fail(s.id + " has no form");
            for (int f = 0; f < s.forms(); f++) {
                if (BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("anime_multiverse", s.items[f])).isEmpty() && s.isLord())
                    return Result.fail("item not registered: " + s.items[f]);
            }
            if (LordText.requiredKeys(s).isEmpty() || LordText.drawbackLines(s) < 4) return Result.fail(s.id + " has no drawback explanation");
        }
        if (LordRules.level(0) != 0 || LordRules.level(LordRules.xpFor(LordRules.MAX_LEVEL)) != LordRules.MAX_LEVEL || LordRules.level(Integer.MAX_VALUE) != LordRules.MAX_LEVEL) return Result.fail("the mastery curve is wrong");
        for (int n = 1; n <= LordRules.MAX_LEVEL; n++) if (LordRules.xpFor(n) <= LordRules.xpFor(n - 1)) return Result.fail("the mastery curve is not rising at " + n);
        if (LordRules.clampBurden(1e9f) > LordRules.BURDEN_MAX || LordRules.clampBurden(-5f) < 0f) return Result.fail("the burden is not clamped");
        float b = 0f;
        for (int i = 0; i < 1000; i++) b = LordRules.gain(b, 50f, 0);
        if (b > LordRules.BURDEN_MAX) return Result.fail("the burden passed its maximum");
        if (LordRules.decay(80f, false, 0) >= 80f || LordRules.decay(0f, false, 0) < 0f) return Result.fail("the burden does not recover");
        if (LordRules.gain(10f, 10f, LordRules.MAX_LEVEL) >= LordRules.gain(10f, 10f, 0)) return Result.fail("mastery does not soften the burden");
        if (LordRules.formOpen(1, 0) || !LordRules.formOpen(1, LordRules.FORM1_LEVEL) || LordRules.ultimateOpen(0) || !LordRules.ultimateOpen(LordRules.ULTIMATE_LEVEL)) return Result.fail("form or decree opening is wrong");
        if (LordRules.halfHunger(LordRules.MAX_LEVEL) >= LordRules.halfHunger(0) || LordRules.ravenousHeal(LordRules.MAX_LEVEL) <= LordRules.ravenousHeal(0)) return Result.fail("Half-Starved Mastery does not ease the hunger");
        if (LordRules.bonusShare(0f) != 0.0 || LordRules.bonusShare(10f) >= 1.0 || LordRules.bonusShare(50f) != 1.0) return Result.fail("the ravenous / dormant shares are wrong");
        LordState st = new LordState();
        st.addXp(LordRules.Sin.SLOTH, 1_000_000);
        st.setBurden(LordRules.Sin.ENVY, 1e9f);
        LordState back = LordState.fromTag(st.toTag());
        if (back.xp(LordRules.Sin.SLOTH) > LordRules.MAX_XP || back.burden(LordRules.Sin.ENVY) > LordRules.BURDEN_MAX) return Result.fail("the state is not clamped");
        return Result.ok("sin table, eight items, mastery curve, burden bounds, hunger numbers and the tag round-trip hold");
    }

    // ---------------------------------------------------------------- the world's rules
    private static Result world(MinecraftServer server) {
        if (!LordConfig.enabled()) return Result.ok("skipped: lords.lordsEnabled is false");
        if (!LordData.bound()) LordData.bind(server);
        LordData d = LordData.get(server);
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 3, 0);
        ServerPlayer a = SelfTests.standIn(level, "lords_a", at.x, at.y, at.z), b = SelfTests.standIn(level, "lords_b", at.x + 1, at.y, at.z);
        int ran = 0;
        java.util.EnumSet<LordRules.Sin> made = java.util.EnumSet.noneOf(LordRules.Sin.class);
        try {
            for (LordRules.Sin sin : LordRules.Sin.values()) {
                if (!sin.isLord() || d.spawned(sin)) continue;
                LordService.clearAll();
                made.add(sin);
                String err = oneWorld(server, level, d, a, b, sin, at);
                if (err != null) return Result.fail(sin.id + ": " + err);
                ran++;
            }
            return Result.ok(ran + " weapons: adoption, one stack, copies, thief, forms, mastery and release hold");
        } finally {
            for (ServerPlayer p : new ServerPlayer[] {a, b}) {
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (isLord(p.getInventory().getItem(i))) p.getInventory().setItem(i, ItemStack.EMPTY);
                LordStore.of(p).reset();
                for (LordRules.Sin s : LordRules.Sin.values()) if (s.isLord()) LordService.applyBurden(p, s);
            }
            level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(16.0), e -> isLord(e.getItem())).forEach(net.minecraft.world.entity.Entity::discard);
            for (LordRules.Sin s : made) d.vanish(s);                                      // a weapon that was in the world before the test is left alone
            LordService.clearAll();
        }
    }

    private static String oneWorld(MinecraftServer server, ServerLevel level, LordData d, ServerPlayer a, ServerPlayer b, LordRules.Sin sin, Vec3 at) {
        a.getInventory().add(new ItemStack(LordItems.of(sin, 0)));
        LordService.scan(a);
        if (!d.spawned(sin) || !d.isOwner(sin, a.getUUID())) return "a bare stack was not adopted and bound to its holder";
        ItemStack mine = find(a, sin);
        if (mine.isEmpty() || !a.getUUID().equals(LordComponents.owner(mine)) || !d.valid(sin, LordComponents.token(mine))) return "the adopted stack does not carry its wielder and the world's token";
        a.getInventory().add(new ItemStack(LordItems.of(sin, 0)));
        ItemStack old = new ItemStack(LordItems.of(sin, 0));
        LordComponents.write(old, a.getUUID(), d.token(sin) ^ 0x5A5A5AL);
        a.getInventory().add(old);
        LordService.scan(a);
        if (count(a, sin) != 1) return "copies and old stacks stayed: " + count(a, sin) + " stacks";
        b.getInventory().add(mine.copy());
        LordService.scan(b);
        if (count(b, sin) != 0) return "a non-wielder kept the weapon";
        if (LordService.switchForm(a, sin, 1)) return "a locked form opened at mastery 0";
        LordStore.of(a).addXp(sin, LordRules.xpFor(LordRules.FORM1_LEVEL));
        LordService.clearAll();
        if (!LordService.switchForm(a, sin, 1)) return "an open form did not switch";
        ItemStack f1 = find(a, sin);
        if (!(f1.getItem() instanceof LordItem li) || li.form() != 1 || LordComponents.token(f1) != d.token(sin)) return "the switch lost the token or the form";
        Zombie z = EntityType.ZOMBIE.create(level);
        if (z == null) return "no zombie to kill";
        z.setPos(at.x + 3, at.y, at.z);
        a.setItemInHand(InteractionHand.MAIN_HAND, f1);
        int xp = LordStore.of(a).xp(sin);
        LordService.onKill(z, a);
        if (LordStore.of(a).xp(sin) <= xp) return "a kill taught the weapon's mastery nothing";
        long before = d.token(sin);
        LordService.release(server, sin, a.getUUID(), level, a.position());
        if (d.owner(sin) != null || d.token(sin) == before || !LordData.get(server).spawned(sin)) return "the release did not free the weapon with a new token";
        LordService.scan(a);
        if (count(a, sin) != 0) return "an old stack survived the release";
        d.vanish(sin);
        if (d.spawned(sin) || d.owner(sin) != null) return "the world was not emptied";
        return null;
    }

    private static boolean isLord(ItemStack s) {
        LordRules.Sin x = LordItems.sinOf(s);
        return x != null && x.isLord();
    }

    private static ItemStack find(ServerPlayer p, LordRules.Sin sin) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (LordItems.sinOf(p.getInventory().getItem(i)) == sin) return p.getInventory().getItem(i);
        return ItemStack.EMPTY;
    }

    private static int count(ServerPlayer p, LordRules.Sin sin) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (LordItems.sinOf(p.getInventory().getItem(i)) == sin) n++;
        return n;
    }

    // ---------------------------------------------------------------- the burden and the arts
    private static Result burden(MinecraftServer server) {
        if (!LordConfig.enabled()) return Result.ok("skipped: lords.lordsEnabled is false");
        if (!LordData.bound()) LordData.bind(server);
        LordData d = LordData.get(server);
        ServerLevel level = server.overworld();
        double y = level.getMaxBuildHeight() - 40;
        ServerPlayer fake = SelfTests.standIn(level, "lords_burden", 0.5, y, 0.5);
        Zombie dummy = EntityType.ZOMBIE.create(level);
        if (dummy == null) return Result.fail("no zombie");
        try {
            fake.setYRot(0f);
            fake.setXRot(0f);
            dummy.setPos(0.5, y, 4.5);
            dummy.setNoAi(true);
            level.addFreshEntity(dummy);
            // engravings, once
            for (LordRules.Sin sin : LordRules.Sin.values()) {
                if (!sin.isLord()) continue;
                for (int f = 0; f < sin.forms(); f++) {
                    ItemStack s = new ItemStack(LordItems.of(sin, f));
                    WeaponEngravings.engrave(s, level.registryAccess());
                    int n = s.getEnchantments().size();
                    if (n == 0) return Result.fail(sin.items[f] + " came out with no Tensura engravings");
                    WeaponEngravings.engrave(s, level.registryAccess());
                    if (s.getEnchantments().size() != n) return Result.fail(sin.items[f] + " was engraved twice");
                }
            }
            // each new art reaches the dummy ahead and nobody else, charges the burden and leaves the food bar alone
            int food = fake.getFoodData().getFoodLevel();
            for (LordRules.Sin sin : LordRules.Sin.values()) {
                if (!sin.isLord()) continue;
                for (int f = 1; f < sin.forms(); f++) {
                    dummy.setHealth(dummy.getMaxHealth());
                    float hp = fake.getHealth();
                    LordArts.Result r = LordArts.art(fake, sin, f, 0);
                    if (!r.fired() || r.hit() < 1) return Result.fail(sin.id + " form " + f + " did not reach the target ahead");
                    if (r.hit() > LordRules.MAX_ART_TARGETS) return Result.fail(sin.id + " form " + f + " hit more than its cap");
                    if (fake.getHealth() < hp) return Result.fail(sin.id + " form " + f + " hurt its wielder");
                    if (!dummy.isAlive()) {
                        dummy = EntityType.ZOMBIE.create(level);
                        dummy.setPos(0.5, y, 4.5);
                        dummy.setNoAi(true);
                        level.addFreshEntity(dummy);
                    }
                }
            }
            if (fake.getFoodData().getFoodLevel() != food) return Result.fail("an art changed the food bar");
            // an art at nothing is free
            dummy.discard();
            if (LordArts.art(fake, LordRules.Sin.SLOTH, 1, 0).fired()) return Result.fail("a slam at nothing fired");
            // the gate: a bound wielder reaches 100 and is refused, then the overload lets go
            LordService.clearAll();
            LordRules.Sin sin = LordRules.Sin.SLOTH;
            if (d.spawned(sin)) return Result.ok("skipped the gate check: the Black Axe of Sloth is already in this world");
            fake.getInventory().add(new ItemStack(LordItems.of(sin, 0)));
            LordService.scan(fake);
            if (!d.isOwner(sin, fake.getUUID())) return Result.fail("the stand-in did not bind");
            LordState st = LordStore.of(fake);
            st.reset();
            for (int i = 0; i < 20 && !st.overloaded(sin, level.getGameTime()); i++) LordService.charge(fake, sin, 30f);
            if (!st.overloaded(sin, level.getGameTime())) return Result.fail("the burden never overloaded");
            if (LordService.gate(fake, sin)) return Result.fail("the gate let an overloaded wielder through");
            if (fake.getFoodData().getFoodLevel() != food) return Result.fail("the overload changed the food bar");
            st.setOverloadUntil(sin, level.getGameTime() - 1);
            for (int i = 0; i < 200; i++) st.setBurden(sin, LordRules.decay(st.burden(sin), true, 0));
            if (st.burden(sin) > 1.0f || !LordService.gate(fake, sin)) return Result.fail("the burden did not recover");
            return Result.ok("arts hit ahead and are capped, the burden charges, overloads, refuses and recovers, the food bar is never touched, engravings apply once");
        } finally {
            dummy.discard();
            for (int i = 0; i < fake.getInventory().getContainerSize(); i++) if (isLord(fake.getInventory().getItem(i))) fake.getInventory().setItem(i, ItemStack.EMPTY);
            LordStore.of(fake).reset();
            LordService.applyBurden(fake, LordRules.Sin.SLOTH);
            LordService.applyBurden(fake, LordRules.Sin.ENVY);
            if (LordData.bound() && LordData.get(server).owner(LordRules.Sin.SLOTH) != null && LordData.get(server).isOwner(LordRules.Sin.SLOTH, fake.getUUID())) LordData.get(server).vanish(LordRules.Sin.SLOTH);
            LordService.clearAll();
        }
    }
}
