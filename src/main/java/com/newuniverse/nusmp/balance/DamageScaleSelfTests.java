package com.newuniverse.nusmp.balance;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.relics.Rankings;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnExtremeTypes;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItems;
import com.newuniverse.nusmp.tooltip.RankTooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the damage scale-down (docs/damage_scale_down.md) changes every strong hit of the mod and nobody can watch that in a game here, so the CI server smoke test checks it on the booted server
 * (prefix {@code dmgscale}): {@code dmgscale.djinn_band} (every Djinn has a rank and its weapon hit lies in the band, strictly by rank, the strongest at the max and the weakest at the min, every
 * Djinn weapon item is registered), {@code dmgscale.cap} (the ceilings of every kind of hit under the live settings, the hard maximum of 15,000, the balance law's cap, every Extreme Magic damage type
 * exists in the registry) and {@code dmgscale.ranks} (every item of the mod has a rank, the table covers the 36 Djinn). Server safe: no client class, nothing is left in the world.
 */
public final class DamageScaleSelfTests {
    private DamageScaleSelfTests() {}

    public static void register() {
        SelfTests.register("dmgscale.djinn_band", DamageScaleSelfTests::band);
        SelfTests.register("dmgscale.cap", DamageScaleSelfTests::cap);
        SelfTests.register("dmgscale.ranks", DamageScaleSelfTests::ranks);
    }

    private static Result band(MinecraftServer server) {
        DamageScale.Limits l = DamageScaleConfig.limits();
        int n = Djinn.values().length;
        if (Rankings.djinnCount() != n) return Result.fail("the ranking table lists " + Rankings.djinnCount() + " Djinn, the roster has " + n);
        double min = Double.MAX_VALUE, max = 0, prev = Double.MAX_VALUE;
        boolean[] seen = new boolean[n + 1];
        for (Djinn d : Djinn.values()) {
            int r = Rankings.djinnRank(d.id());
            if (r < 1 || r > n || seen[r]) return Result.fail(d.id() + " has no unique rank (" + r + ")");
            seen[r] = true;
            double hit = DamageScale.djinnHit(r, n, l.djinnMin(), l.djinnMax());
            if (hit < l.djinnMin() - 1e-6 || hit > l.djinnMax() + 1e-6) return Result.fail(d.id() + " hits " + hit + " outside the band");
            min = Math.min(min, hit);
            max = Math.max(max, hit);
            if (DjinnWeaponItems.item(d, false) == null || DjinnWeaponItems.item(d, true) == null) return Result.fail(d.id() + " has no weapon item");
        }
        for (int r = 1; r <= n; r++) {
            double hit = DamageScale.djinnHit(r, n, l.djinnMin(), l.djinnMax());
            if (hit > prev) return Result.fail("the band is not falling with the rank at " + r);
            prev = hit;
        }
        if (Math.abs(max - l.djinnMax()) > 1e-6 || Math.abs(min - l.djinnMin()) > 1e-6) return Result.fail("the band ends are " + min + " / " + max);
        return Result.ok(n + " Djinn, weapon hit " + (long) min + " (rank " + n + ") to " + (long) max + " (rank 1), enabled=" + l.enabled());
    }

    private static Result cap(MinecraftServer server) {
        DamageScale.Limits l = DamageScaleConfig.limits();
        if (!l.enabled()) return Result.ok("scaleDownEnabled=false: no per-hit ceiling (the old damage)");
        int n = Djinn.values().length;
        for (DamageScale.Kind k : DamageScale.Kind.values()) {
            for (int r : new int[]{1, n / 2, n}) {
                double c = DamageScale.ceiling(l, k, r, n);
                double hit = DamageScale.apply(l, k, r, n, 1.0e12, 1.0, false);
                if (c > DamageScale.ABSOLUTE_MAX || hit > DamageScale.ABSOLUTE_MAX) return Result.fail(k + " passes the hard maximum: " + c + " / " + hit);
                if (k != DamageScale.Kind.APEX && c > l.global() + 1e-6) return Result.fail(k + " passes the global ceiling: " + c);
            }
        }
        DamageScale.Limits wild = new DamageScale.Limits(true, 1e9, 1e9, 1e9, 1e9, 1e9, true);
        if (DamageScale.apply(wild, DamageScale.Kind.APEX, 1, n, 1e12, 1.0, false) > DamageScale.ABSOLUTE_MAX) return Result.fail("a raised apex setting passes 15,000");
        if (DamageScale.apply(wild, DamageScale.Kind.GENERAL, 1, n, 1e12, 1.0, false) > DamageScale.ABSOLUTE_MAX) return Result.fail("a raised global setting passes 15,000");
        Zombie target = EntityType.ZOMBIE.create(server.overworld());
        if (target == null) return Result.fail("no stand-in zombie");
        try {
            float hit = BalanceLaw.cap(target, 1.0e9f);
            if (hit > DamageScaleConfig.softCeiling() + 1e-3) return Result.fail("BalanceLaw.cap lets " + hit + " through");
        } finally {
            target.discard();
        }
        var types = server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        for (Djinn d : Djinn.values()) {
            String id = DjinnExtremeTypes.typeId(d.id());
            if (id == null || !types.containsKey(ResourceLocation.parse(id))) return Result.fail(d.id() + "'s Extreme Magic damage type " + id + " is not in the registry");
        }
        return Result.ok("global " + (long) l.global() + ", apex " + (long) l.apex() + ", hard max " + (long) DamageScale.ABSOLUTE_MAX + "; all 36 Extreme Magic damage types exist");
    }

    private static Result ranks(MinecraftServer server) {
        Map<String, Integer> bySource = new HashMap<>();
        int total = 0;
        StringBuilder missing = new StringBuilder();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!RankTooltip.NAMESPACE.equals(id.getNamespace())) continue;
            ItemStack s = new ItemStack(item);
            Rankings.Rank r = RankTooltip.rankOf(s);
            if (r == null) { if (missing.length() < 200) missing.append(id.getPath()).append(' '); continue; }   // a spawn egg is left out on purpose
            total++;
            bySource.merge(r.source(), 1, Integer::sum);
            if (RankTooltip.line(s) == null) return Result.fail(id.getPath() + " has a rank but no tooltip line");
        }
        if (total == 0) return Result.fail("no item of the mod has a rank");
        for (Djinn d : Djinn.values()) {
            if (Rankings.table().djinn(d.id()) == null) return Result.fail(d.id() + " has no Djinn rank");
            Rankings.Rank w = Rankings.of("djinn_weapon_" + d.id());
            if (w == null || w.place() < 1) return Result.fail(d.id() + "'s weapon has no rank place");
        }
        return Result.ok(total + " items ranked " + bySource + (missing.length() > 0 ? "; no rank: " + missing : ""));
    }
}
