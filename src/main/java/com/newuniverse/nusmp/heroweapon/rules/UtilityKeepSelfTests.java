package com.newuniverse.nusmp.heroweapon.rules;

import com.newuniverse.nusmp.arc.world.ArcWorldItems;
import com.newuniverse.nusmp.arena.ArenaSystem;
import com.newuniverse.nusmp.dragonballs.DbItems;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.gem.GemItems;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why (4.89.4): the hold-and-use items (the Hourglass of the Final Hour, the Chronicle Sigil, the Calling Gem, the Dragon Radar) must never be rejected by a weapon rule. The CI server smoke
 * test runs this ({@code utilkeep.rules}) in the real game on stand-in stacks and a stand-in Holy wielder: the stacks are in the tag, none of them is classed as a weapon, the Holy wielder
 * is not rejected by any of them while a real bow still is, a hero weapon is not a utility, and the main-hand yield rule gives the click on only for a weapon with a ready utility in
 * the off hand. Nothing else is needed: no world, no boss, no second player.
 */
public final class UtilityKeepSelfTests {
    private UtilityKeepSelfTests() {}

    public static void register() { SelfTests.register("utilkeep.rules", UtilityKeepSelfTests::rules); }

    private static SelfTests.Result rules(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        String note = "";
        boolean on = HeroConfig.utilityItemsAlwaysWork();
        List<ItemStack> utilities = List.of(new ItemStack(ArenaSystem.HOURGLASS.get()), new ItemStack(ArcWorldItems.CHRONICLE_SIGIL.get()), GemItems.holyStack(), new ItemStack(DbItems.RADAR.get()));
        for (ItemStack u : utilities) {
            if (!HeroRules.utilityTagged(u)) bad.add(u.getItem() + " is not in #anime_multiverse:hold_use_items");
            if (!HeroRules.utility(u)) bad.add(u.getItem() + " is not read as a utility");
            if (on && HeroRules.classOf(u) != WeaponClass.NONE) bad.add(u.getItem() + " is classed " + HeroRules.classOf(u));
            if (on && HeroRules.weaponLike(u)) bad.add(u.getItem() + " is read as a weapon");
        }
        if (HeroRules.utilityTagged(HeroItems.stack(HeroKind.HAMMER)) || !HeroRules.weaponLike(HeroItems.stack(HeroKind.HAMMER))) bad.add("a hero weapon is in the utility tag or not read as a weapon");
        if (HeroRules.utility(new ItemStack(Items.BOW)) || HeroRules.utility(new ItemStack(Items.IRON_SWORD))) bad.add("a vanilla weapon is read as a utility");
        if (!on) {
            note += "; heroweapon.utilityItemsAlwaysWork is off in the config, so the exemption was not exercised";
        } else {
            ServerLevel level = server.overworld();
            FakePlayer a = SelfTests.player(level, "utilkeep_p1", 0.5, 100, 0.5);
            try {
                if (HeroBonds.has(a)) HeroBonds.release(a);
                if (HeroConfig.enabled() && HeroConfig.holyRejectsOthers() && HeroBonds.bind(a, HeroKind.SWORD) == null) {
                    for (ItemStack u : utilities) if (HeroRules.rejectedClass(a, u) != WeaponClass.NONE) bad.add("the Sword hero was rejected by " + u.getItem());
                    if (HeroRules.rejectedClass(a, new ItemStack(Items.BOW)) != WeaponClass.BOW) bad.add("the Sword hero is no longer rejected by a bow");
                    HeroBonds.release(a);
                } else {
                    note += "; the Sword could not be bound to a stand-in (or the rejection is off), so the wielder was not exercised";
                }
            } finally {
                if (HeroBonds.has(a)) HeroBonds.release(a);
                a.getInventory().clearContent();
            }
        }
        if (!UtilityItems.mainYields(true, false, true, false, true, true) || UtilityItems.mainYields(true, true, true, false, true, true)
                || UtilityItems.mainYields(true, false, true, false, true, false) || UtilityItems.mainYields(false, false, true, false, true, true))
            bad.add("the main-hand yield rule is wrong");
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
        return SelfTests.Result.ok("the hourglass, the sigil, the gem and the radar are in the tag, are no weapon and are never rejected, and the yield rule holds" + note);
    }
}
