package com.newuniverse.nusmp.heroweapon.bond;

import com.newuniverse.nusmp.heroweapon.HeroComponents;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: nothing of the bond can be played by its author, so the CI server smoke test runs this self-test ({@code hero.bond}) in the real game with stand-in
 * players: binding (a refusal for no weapon, one weapon per player, a unique Holy Weapon refused to a second holder and free again after a release), the
 * saved form (the bond's NBT round trip, an empty tag, a tag of an unknown weapon, a tag with only the weapon), the copy sent to the client and its codec, the
 * real loan on a stand-in's inventory (a summon puts the projection in the selected slot and displaces what stood there, a second summon is refused, a stray
 * second projection is wiped, a dismiss and a death give the displaced item back and leave no hero weapon, the bond survives both), the stack checks (a bare
 * stack is adopted, a stack of another weapon and someone else's projection dissolve) and the container check (a hero weapon in a crafting grid vanishes). A
 * stand-in is cleaned before and after, and the test removes everything it made.
 */
public final class HeroBondSelfTests {
    private HeroBondSelfTests() {}

    public static void register() { SelfTests.register("hero.bond", HeroBondSelfTests::bond); }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static int heroStacks(ServerPlayer p) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (HeroItems.kindOf(inv.getItem(i)) != null) n++;
        if (HeroItems.kindOf(p.containerMenu.getCarried()) != null) n++;
        return n;
    }

    private static int projections(ServerPlayer p) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (HeroLoan.isProjection(inv.getItem(i))) n++;
        return n;
    }

    private static void clean(ServerPlayer p) {
        if (HeroBonds.has(p)) HeroBonds.release(p);
        p.getInventory().clearContent();
        p.containerMenu.setCarried(ItemStack.EMPTY);
        p.inventoryMenu.getCraftSlots().clearContent();
    }

    private static SelfTests.Result bond(MinecraftServer server) {
        if (!HeroConfig.enabled()) return ok("skipped: heroweapon.enabled is false in the config");
        if (HeroConfig.maxBonds() < 1) return ok("skipped: heroweapon.maxBonds is 0 in the config");
        ServerLevel level = server.overworld();
        FakePlayer a = SelfTests.player(level, "hero_core_a", 0.5, 100, 0.5);
        FakePlayer b = SelfTests.player(level, "hero_core_b", 0.5, 100, 0.5);
        clean(a);
        clean(b);
        List<String> bad = new ArrayList<>();
        try {
            HeroKind kind = HeroKind.HAMMER;
            // ---- binding
            if (HeroBonds.bind(a, null) == null) bad.add("binding no weapon was allowed");
            String why = HeroBonds.bind(a, kind);
            if (why != null) return ok("skipped: the Hammer could not be bound to a stand-in (" + why + ")");
            if (!HeroBonds.has(a) || HeroBonds.kindOf(a) != kind) bad.add("the bond is not there after a bind");
            if (!HeroRegistry.holdersOf(server, kind).contains(a.getUUID())) bad.add("the register does not hold the Hammer for the stand-in");
            if (HeroBonds.bind(a, HeroKind.CLAW) == null) bad.add("a second weapon was bound to a player who already has one");
            if (HeroBonds.kindOf(a) != kind) bad.add("a refused second bind changed the first weapon");

            // ---- the saved form
            HeroBond bd = HeroBonds.of(a);
            bd.addExp(2500L);
            bd.setForm("leaf");
            bd.unlock("leaf");
            bd.addMastery("leaf", 7);
            bd.absorb("minecraft:leather", 12);
            bd.extra("selftest").putInt("n", 5);
            HeroBond back = HeroBond.fromTag(bd.toTag());
            if (back.kind() != kind || back.exp() != 2500L || !"leaf".equals(back.form()) || !back.isUnlocked("leaf") || back.mastery("leaf") != 7
                    || back.absorbedCount("minecraft:leather") != 12 || !back.hasExtra("selftest") || back.extra("selftest").getInt("n") != 5)
                bad.add("the bond changed on the way through its NBT");
            if (HeroBond.fromTag(new CompoundTag()).isBound()) bad.add("an empty tag reads as a bound weapon");
            CompoundTag unknown = new CompoundTag();
            unknown.putString("kind", "nusmp:not_a_weapon");
            if (HeroBond.fromTag(unknown).isBound()) bad.add("a tag of an unknown weapon reads as a bound weapon");
            CompoundTag onlyKind = new CompoundTag();
            onlyKind.putString("kind", HeroKind.FAN.itemId());
            HeroBond sparse = HeroBond.fromTag(onlyKind);
            if (sparse.kind() != HeroKind.FAN || sparse.exp() != 0L || !"base".equals(sparse.form()) || !sparse.isUnlocked("base") || sparse.summoned())
                bad.add("a tag with only the weapon does not read as a fresh bond");

            // ---- the copy for the client
            HeroNet.Sync s = HeroSync.build(a);
            if (!kind.itemId().equals(s.weapon()) || s.exp() != 2500L || !"leaf".equals(s.form()) || !s.unlocked().contains("leaf") || s.absorbedKinds() != 1 || s.absorbedTotal() != 12L)
                bad.add("the copy for the client does not match the bond: " + s);
            boolean masteryOk = false;
            for (HeroNet.Entry e : s.mastery()) if (e.key().equals("leaf") && e.value() == 7) masteryOk = true;
            if (!masteryOk) bad.add("the copy for the client lacks the mastery");
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            HeroNet.Sync.STREAM_CODEC.encode(buf, s);
            if (!s.equals(HeroNet.Sync.STREAM_CODEC.decode(buf))) bad.add("the copy for the client changed on the way through its codec");
            buf.clear();
            HeroNet.Sync.STREAM_CODEC.encode(buf, HeroNet.Sync.none());
            if (!HeroNet.Sync.STREAM_CODEC.decode(buf).isNone()) bad.add("the empty copy does not read as empty");
            bd.setExp(0L);
            bd.setForm("base");

            // ---- the real loan on the stand-in's inventory
            Inventory inv = a.getInventory();
            inv.selected = 0;
            ItemStack marker = new ItemStack(Items.STICK);
            marker.set(DataComponents.CUSTOM_NAME, Component.literal("hero self-test marker"));
            inv.items.set(0, marker.copy());
            if (!HeroSummon.summon(a)) bad.add("a bonded stand-in could not summon the weapon");
            ItemStack hand = a.getMainHandItem();
            if (!HeroLoan.isProjection(hand) || HeroItems.kindOf(hand) != kind) bad.add("the selected slot does not hold the projection after a summon");
            if (!a.getUUID().toString().equals(HeroComponents.owner(hand))) bad.add("the projection does not carry its owner");
            if (!HeroSummon.isSummoned(a)) bad.add("the bond does not say the weapon is out");
            int markers = 0;
            for (int i = 1; i < inv.items.size(); i++) if (ItemStack.matches(marker, inv.items.get(i))) markers++;
            if (markers != 1) bad.add("the item that stood in the slot was not displaced into one other slot (found " + markers + ")");
            if (HeroSummon.summon(a)) bad.add("a second summon was allowed");
            inv.items.set(20, hand.copy());
            HeroLoan.reconcile(a);
            if (projections(a) != 1 || !HeroLoan.isProjection(inv.items.get(0))) bad.add("a stray second projection was not wiped (" + projections(a) + " left)");
            if (!HeroSummon.dismiss(a)) bad.add("a summoned weapon could not be put away");
            if (heroStacks(a) != 0) bad.add("a hero weapon is left in the inventory after a dismiss");
            if (!ItemStack.matches(marker, inv.items.get(0))) bad.add("the displaced item did not come back to its slot");
            if (HeroSummon.dismiss(a)) bad.add("a weapon that is away was put away again");

            // ---- a death: the loan ends before the drops, the bond stays
            HeroSummon.summon(a);
            HeroLoan.onDeath(a);
            if (heroStacks(a) != 0) bad.add("a hero weapon survived the end of the loan at death");
            if (!ItemStack.matches(marker, inv.items.get(0))) bad.add("the displaced item did not come back at death");
            if (HeroBonds.kindOf(a) != kind || HeroSummon.isSummoned(a)) bad.add("a death changed the bond beyond stowing the weapon");

            // ---- the stack checks
            if (HeroConfig.adoptStacks()) {
                ItemStack bare = HeroItems.stack(kind);
                inv.items.set(5, bare);
                HeroLoan.vet(a, bare, kind);
                if (!bare.isEmpty()) bad.add("a bare stack of the player's own weapon was not taken in for adoption");
                HeroLoan.processAdoption(a);
                if (!HeroSummon.isSummoned(a) || projections(a) != 1) bad.add("the adoption of a bare stack did not summon the weapon");
            } else {
                HeroSummon.summon(a);
            }
            ItemStack strange = HeroItems.stack(HeroKind.SWORD);
            inv.items.set(6, strange);
            HeroLoan.vet(a, strange, HeroKind.SWORD);
            if (!strange.isEmpty()) bad.add("a bare stack of another weapon was kept by a player bound to the Hammer");
            ItemStack theirs = HeroItems.stack(kind);
            theirs.set(HeroComponents.OWNER.get(), UUID.nameUUIDFromBytes("someone else".getBytes()).toString());
            inv.items.set(7, theirs);
            HeroLoan.vet(a, theirs, kind);
            if (!theirs.isEmpty()) bad.add("someone else's projection was kept");
            ItemStack mine = inv.items.get(0);
            if (!HeroLoan.isProjection(mine)) mine = HeroSummon.stack(a);
            HeroLoan.vet(a, mine, kind);
            if (mine.isEmpty()) bad.add("the player's own projection was dissolved by the stack check");

            // ---- the container check
            a.inventoryMenu.getCraftSlots().setItem(0, HeroItems.stack(kind));
            HeroLoan.scanMenu(a);
            if (!a.inventoryMenu.getCraftSlots().getItem(0).isEmpty()) bad.add("a hero weapon in a crafting grid was not removed");

            // ---- a release
            if (!HeroBonds.release(a)) bad.add("a release found no bond");
            if (HeroBonds.has(a)) bad.add("the bond is there after a release");
            if (heroStacks(a) != 0) bad.add("a hero weapon is left after a release");
            if (HeroRegistry.holdersOf(server, kind).contains(a.getUUID())) bad.add("the register still holds the Hammer after the release");
            if (HeroBonds.release(a)) bad.add("a second release found a bond");

            // ---- a unique Holy Weapon
            HeroKind holy = HeroKind.SHIELD;
            String first = HeroBonds.bind(a, holy);
            if (first == null) {
                String second = HeroBonds.bind(b, holy);
                if (HeroConfig.holyUnique() && second == null) bad.add("a second player was bound to a Holy Weapon that holyUnique reserves");
                if (!HeroConfig.holyUnique() && second != null) bad.add("holyUnique is off but a second holder was refused: " + second);
                if (second == null) HeroBonds.release(b);
                if (HeroConfig.holyUnique()) {
                    HeroBonds.release(a);
                    if (HeroBonds.bind(b, holy) != null) bad.add("the Holy Weapon was not free again after its holder released it");
                    HeroBonds.release(b);
                }
            }
            if (HeroBonds.has(a)) HeroBonds.release(a);
        } finally {
            clean(a);
            clean(b);
        }
        if (!bad.isEmpty()) return fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
        return ok("bind, one weapon per player, the register, the NBT round trip, the copy for the client and its codec, summon and dismiss on a real inventory, death, the stack checks, the container check and release all hold");
    }
}
