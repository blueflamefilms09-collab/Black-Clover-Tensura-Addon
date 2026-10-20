package com.newuniverse.nusmp.relics.djinn;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.relics.djinn.weapon.ActLibrary;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItem;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItems;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponRules;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponService;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The self-test {@code djinn.core_rebuild} (run by the CI server smoke test in the real game, see {@link SelfTests}): the thirty-six Djinn are all wired
 * (seal, light and dark weapon registered, a weapon row, the engravings exist in Tensura, the lang, the ledger, the sync packet), and the vessel overlay
 * holds in a real inventory with a real stack: an enchanted, named, bound weapon vessel in the selected slot of a full inventory stays "on the body" under
 * the Djinn weapon, is in no other slot, survives the hotbar scroll, and comes back to its slot equal to the original.
 */
public final class DjinnSelfTests {
    private DjinnSelfTests() {}

    public static void register() {
        SelfTests.register("djinn.core_rebuild", DjinnSelfTests::coreRebuild);
        DjinnBindSelfTest.register();                                                            // 1.101.0: djinn.bc_bind
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    static SelfTests.Result coreRebuild(MinecraftServer server) throws Exception {
        SelfTests.Result wired = wiring(server);
        if (!wired.ok()) return wired;
        SelfTests.Result overlay = overlay(server);
        if (!overlay.ok()) return overlay;
        return SelfTests.Result.ok(wired.detail() + "; " + overlay.detail());
    }

    // ---------------------------------------------------------------- the thirty-six are wired
    private static SelfTests.Result wiring(MinecraftServer server) throws Exception {
        Djinn[] all = Djinn.values();
        if (all.length != 36) return SelfTests.Result.fail("expected 36 Djinn, found " + all.length);
        List<String> bad = new ArrayList<>();
        JsonObject lang;
        try (InputStream in = DjinnSelfTests.class.getResourceAsStream("/assets/nusmp/lang/en_us.json")) {
            if (in == null) return SelfTests.Result.fail("en_us.json is not in the jar");
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        DjinnLedger ledger = DjinnLedger.get(server);
        CompoundTag saved = ledger.save(new CompoundTag(), server.registryAccess());
        int weapons = 0;
        for (Djinn d : all) {
            if (!BuiltInRegistries.ITEM.containsKey(id("djinn_seal_" + d.id()))) bad.add("no seal item for " + d.id());
            if (!(DjinnItems.seal(d) instanceof DjinnSealItem seal) || seal.djinn() != d) bad.add("seal of " + d.id());
            for (boolean dark : new boolean[]{false, true}) {
                String path = DjinnWeaponRules.itemPath(d, dark);
                if (!BuiltInRegistries.ITEM.containsKey(id(path))) bad.add("no weapon item " + path);
                if (!(DjinnWeaponItems.item(d, dark) instanceof DjinnWeaponItem w) || w.djinn() != d || w.dark() != dark) bad.add("weapon " + path);
                else weapons++;
                if (!lang.has("item.nusmp." + path)) bad.add("no lang for " + path);
            }
            if (!lang.has("item.nusmp.djinn_seal_" + d.id())) bad.add("no lang for the seal of " + d.id());
            DjinnWeaponRules.Spec spec = DjinnWeaponRules.spec(d);
            if (spec == null) {
                bad.add("no weapon row for " + d.id());
                continue;
            }
            for (int a = 0; a < 2; a++) {
                String key = spec.act(a).key();
                if (!lang.has("djinn_weapon.act." + key) || !lang.has("djinn_weapon.act." + key + ".desc")) bad.add("no lang for the act " + key);
                if (d.ordinal() >= 12 && !ActLibrary.has(key)) bad.add("no generic act " + key);
            }
            if (!saved.contains(d.id(), 10)) bad.add("the ledger does not save " + d.id());
            if (ledger.state(d) == null) bad.add("the ledger has no state for " + d.id());
            int pts = DjinnMagic.points(d, Vec3.ZERO, new Vec3(0, 0, 1), RandomSource.create(7)).size();
            int want = switch (d.extreme().shape()) { case BOLTS -> 9; case COLUMNS -> 8; case RAIN -> Djinn.Shape.RAIN.steps(); default -> 0; };
            if (pts != want) bad.add(d.id() + ": " + pts + " marks for a " + d.extreme().shape() + ", wanted " + want);
            if (DjinnMagic.steps(d.extreme().shape()) != d.extreme().shape().steps()) bad.add(d.id() + ": steps");
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            DjinnNet.Sync.STREAM_CODEC.encode(buf, new DjinnNet.Sync(5, 3, d.ordinal() + 1, 100, 50, 10, 40, 0, 0, 0, 0));
            DjinnNet.Sync back = DjinnNet.Sync.STREAM_CODEC.decode(buf);
            if (Djinn.byOrdinal(back.djinn() - 1) != d) bad.add("the sync packet loses " + d.id());
            buf.release();
        }
        ServerLevel level = server.overworld();
        var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ServerPlayer p = SelfTests.player(level, "djinn_engrave", 0.5, 120, 0.5);
        for (Djinn d : all)
            for (boolean dark : new boolean[]{false, true}) {
                ItemStack st = DjinnWeaponService.make(p, d, dark, true);
                int want = DjinnWeaponRules.engravings(d, dark).size();
                int got = st.getEnchantments().size();
                if (got < want) {
                    StringBuilder miss = new StringBuilder();
                    for (DjinnWeaponRules.Engraving e : DjinnWeaponRules.engravings(d, dark))
                        if (enchants.get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("tensura", e.id()))).isEmpty()) miss.append(e.id()).append(' ');
                    bad.add(DjinnWeaponRules.itemPath(d, dark) + " has " + got + " of " + want + " engravings (Tensura lacks: " + miss.toString().trim() + ")");
                }
            }
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problems: " + String.join("; ", bad.subList(0, Math.min(6, bad.size()))));
        return SelfTests.Result.ok("36 Djinn wired (" + weapons + " weapons, " + ActLibrary.keys().size() + " generic acts, lang, ledger, packet, engravings)");
    }

    // ---------------------------------------------------------------- the overlay in a real inventory
    private static SelfTests.Result overlay(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.player(level, "djinn_overlay", 0.5, 120, 0.5);
        CompoundTag state = p.getData(DjinnAttachments.STATE);
        CompoundTag backup = state.copy();
        UUID serial = UUID.nameUUIDFromBytes("djinn-self-test-vessel".getBytes(StandardCharsets.UTF_8));
        try {
            for (String k : new String[]{"djinn", "serial", "phase", "wloan", "dark", "lentFrom", "lentDjinn"}) state.remove(k);
            state.putString("djinn", Djinn.BAEL.name());
            state.putUUID("serial", serial);
            state.putInt("phase", DjinnPhaseRules.HALF);
            ItemStack vessel = new ItemStack(Items.IRON_SWORD);
            Holder<Enchantment> sharp = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
            vessel.enchant(sharp, 4);
            vessel.set(DataComponents.CUSTOM_NAME, Component.literal("Warden's Edge"));
            vessel.setDamageValue(37);
            VesselAccess.stamp(vessel, Djinn.BAEL, serial);
            ItemStack original = vessel.copy();
            for (int i = 0; i < 36; i++) p.getInventory().items.set(i, new ItemStack(Items.DIRT, 1 + i % 5));      // a full inventory: nothing could be moved to a free slot
            p.getInventory().items.set(3, vessel);
            p.getInventory().selected = 3;

            DjinnWeaponService.apply(p);
            if (!(p.getInventory().items.get(3).getItem() instanceof DjinnWeaponItem)) return SelfTests.Result.fail("the Djinn weapon is not in the selected slot (it holds " + p.getInventory().items.get(3) + ")");
            for (int i = 0; i < 36; i++) if (i != 3 && VesselAccess.boundDjinn(p.getInventory().items.get(i)) != null) return SelfTests.Result.fail("the vessel was moved to slot " + i);
            if (!p.getInventory().offhand.get(0).isEmpty()) return SelfTests.Result.fail("something landed in the offhand");
            ItemStack found = VesselAccess.findBound(p, serial);
            if (found.isEmpty()) return SelfTests.Result.fail("the vessel under the weapon is not found: the Equip would end with 'your vessel left you'");
            if (!ItemStack.matches(found, original)) return SelfTests.Result.fail("the overlaid vessel differs from the original");
            p.getInventory().selected = 6;                                                                       // the hotbar is scrolled
            DjinnWeaponService.apply(p);
            if (!(p.getInventory().items.get(3).getItem() instanceof DjinnWeaponItem)) return SelfTests.Result.fail("the weapon left its slot when the hotbar was scrolled");
            if (VesselAccess.findBound(p, serial).isEmpty()) return SelfTests.Result.fail("the vessel was lost when the hotbar was scrolled");
            DjinnWeaponService.release(p);
            ItemStack back = p.getInventory().items.get(3);
            if (!ItemStack.matches(back, original)) return SelfTests.Result.fail("the vessel did not come back to its slot equal to the original: " + back);
            for (int i = 0; i < 36; i++) if (i != 3 && p.getInventory().items.get(i).getItem() != Items.DIRT) return SelfTests.Result.fail("slot " + i + " changed: " + p.getInventory().items.get(i));
            if (DjinnState.of(p).hasLoan()) return SelfTests.Result.fail("the loan record is still there after the release");
            return SelfTests.Result.ok("a bound enchanted weapon vessel stays overlaid in slot 3 of a full inventory, found, through a scroll, and returns equal");
        } finally {
            for (int i = 0; i < 36; i++) p.getInventory().items.set(i, ItemStack.EMPTY);
            for (String k : new ArrayList<>(state.getAllKeys())) state.remove(k);
            state.merge(backup);
        }
    }
}
