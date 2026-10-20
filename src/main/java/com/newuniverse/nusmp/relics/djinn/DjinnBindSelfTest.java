package com.newuniverse.nusmp.relics.djinn;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.relics.RelicComponents2;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItem;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 1.101.0, the self-test {@code djinn.bc_bind} (run by the CI server smoke test, see {@link SelfTests}): a Djinn Seal is bound to a Legend-grade weapon vessel that has a
 * Better Combat file, from every gesture (the Seal used from the main hand and from the off hand, the inventory click, the command, the Djinn key), with the off hand
 * answering the way Better Combat makes it answer ({@link BcPlayer}: EMPTY while a two-handed weapon is in either hand). Each time: the bond is made, the vessel is
 * stamped and found, and the Djinn weapon overlays the vessel and gives it back. Everything is undone afterwards (the bond, the ledger, the state record).
 */
public final class DjinnBindSelfTest {
    private DjinnBindSelfTest() {}

    public static void register() { SelfTests.register("djinn.bc_bind", DjinnBindSelfTest::run); }

    /** Better Combat 2.4.0's presets with {@code two_handed: true} (tools/bettercombat/builtin_animations.json), the ones this mod's weapon files use. */
    private static final Set<String> TWO_HANDED_PRESETS = Set.of("claymore", "double_axe", "heavy_axe", "glaive", "halberd", "hammer", "katana", "scythe", "spear", "staff", "battlestaff");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    /** Does this stack's item have a weapon file that makes it two-handed under Better Combat (its own flag, else its parent preset's)? */
    static boolean twoHanded(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(s.getItem());
        if (!"nusmp".equals(key.getNamespace())) return false;
        JsonObject o = file(key.getPath());
        if (o == null) return false;
        JsonObject a = o.has("attributes") && o.get("attributes").isJsonObject() ? o.getAsJsonObject("attributes") : new JsonObject();
        if (a.has("two_handed")) return a.get("two_handed").getAsBoolean();
        String parent = o.has("parent") ? o.get("parent").getAsString() : "";
        return TWO_HANDED_PRESETS.contains(parent.substring(parent.indexOf(':') + 1));
    }

    private static JsonObject file(String path) {
        try (InputStream in = DjinnBindSelfTest.class.getResourceAsStream("/data/nusmp/weapon_attributes/" + path + ".json")) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    /** A stand-in player whose off hand answers like Better Combat's: EMPTY while the main-hand or the off-hand stack is two-handed. The inventory itself is untouched. */
    private static final class BcPlayer extends FakePlayer {
        BcPlayer(ServerLevel level, GameProfile profile) { super(level, profile); }

        @Override
        public ItemStack getItemBySlot(EquipmentSlot slot) {
            if (slot == EquipmentSlot.OFFHAND && (twoHanded(getInventory().getSelected()) || twoHanded(getInventory().offhand.get(0)))) return ItemStack.EMPTY;
            return super.getItemBySlot(slot);
        }
    }

    private static ItemStack vessel(String path, UUID owner) {
        Item item = BuiltInRegistries.ITEM.getOptional(id(path)).orElse(null);
        if (item == null) return ItemStack.EMPTY;
        ItemStack st = new ItemStack(item);
        RelicDef d = Relics.defOf(st);
        if (d == null || !d.isVessel() || !d.grade().djinnCapable() || file(path) == null) return ItemStack.EMPTY;
        st.set(RelicComponents2.ATTUNED.get(), owner);
        return st;
    }

    private static void clear(ServerPlayer p) {
        for (int i = 0; i < 41; i++) {
            if (i < 36) p.getInventory().items.set(i, ItemStack.EMPTY);
            else if (i < 40) p.getInventory().armor.set(i - 36, ItemStack.EMPTY);
            else p.getInventory().offhand.set(0, ItemStack.EMPTY);
        }
        p.getInventory().selected = 0;
    }

    static SelfTests.Result run(MinecraftServer server) throws Exception {
        ServerLevel level = server.overworld();
        UUID uid = UUID.nameUUIDFromBytes("nusmp-selftest:djinn_bc_bind".getBytes(StandardCharsets.UTF_8));
        BcPlayer p = new BcPlayer(level, new GameProfile(uid, "djinn_bc_bind"));
        p.setPos(0.5, 120, 0.5);
        p.setHealth(p.getMaxHealth());
        DjinnLedger ledger = DjinnLedger.get(server);
        Djinn d = null;
        for (Djinn c : new Djinn[]{Djinn.MURMUR, Djinn.CAMIO, Djinn.BALAM, Djinn.CROCELL}) if (ledger.state(c) == DjinnLedger.State.SLEEPING) { d = c; break; }
        if (d == null) return SelfTests.Result.ok("skipped: every Djinn the test may use is in play on this world");

        ItemStack two = ItemStack.EMPTY, one = ItemStack.EMPTY;
        for (String s : new String[]{"noon_cleaver", "calamity_scythe", "magic_emperors_black_halberd", "blackrazor", "staff_of_the_magi", "hammer_of_thunderbolts"}) {
            two = vessel(s, uid);
            if (!two.isEmpty() && twoHanded(two)) break;
            two = ItemStack.EMPTY;
        }
        for (String s : new String[]{"vorpal_sword", "holy_avenger", "luck_blade", "frostbrand"}) {
            one = vessel(s, uid);
            if (!one.isEmpty() && !twoHanded(one)) break;
            one = ItemStack.EMPTY;
        }
        if (two.isEmpty() || one.isEmpty()) return SelfTests.Result.fail("no two-handed or no one-handed Legend weapon vessel with a Better Combat file was found (two=" + two + ", one=" + one + ")");

        CompoundTag state = p.getData(DjinnAttachments.STATE);
        CompoundTag backup = state.copy();
        GameType before = p.gameMode.getGameModeForPlayer();
        List<String> done = new ArrayList<>();
        try {
            for (String k : new ArrayList<>(state.getAllKeys())) state.remove(k);
            p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);                       // the price and the trial are not what is tested here
            try {
                Bonds.canBind(p, Bonds.Kind.DJINN);                                          // what the ritual asks of the Tensura side; a stand-in that cannot answer is not a failure of the fix
                com.newuniverse.nusmp.mana.ManaLaw.stage(p);
            } catch (RuntimeException env) {
                return SelfTests.Result.ok("skipped: the stand-in cannot answer the bond or stage lookups (" + env.getClass().getSimpleName() + ")");
            }
            String[] names = {"seal in main hand, vessel in off hand", "seal in off hand, vessel in main hand", "inventory click", "command: slot", "command: off hand", "Djinn key"};
            for (int g = 0; g < names.length; g++) {
                // the vessel for this gesture: a two-handed weapon wherever Better Combat's lie can bite, a one-handed one where the gesture cannot work otherwise
                boolean needsOneHanded = g == 1;
                ItemStack v = (needsOneHanded ? one : two).copy();
                ItemStack seal = DjinnItems.sealStack(d);
                clear(p);
                switch (g) {
                    case 0 -> { p.getInventory().items.set(0, seal); p.getInventory().offhand.set(0, v); }
                    case 1 -> { p.getInventory().items.set(0, v); p.getInventory().offhand.set(0, seal); }
                    case 2 -> { p.getInventory().items.set(7, v); }
                    case 3 -> { p.getInventory().items.set(0, seal); p.getInventory().items.set(12, v); }
                    case 4 -> { p.getInventory().items.set(0, seal); p.getInventory().offhand.set(0, v); }
                    default -> { p.getInventory().items.set(0, seal); p.getInventory().offhand.set(0, v); }
                }
                if (g == 0 || g == 4 || g == 5) {
                    if (!p.getItemInHand(InteractionHand.OFF_HAND).isEmpty() && twoHanded(v))
                        return SelfTests.Result.fail("the stand-in does not model Better Combat: the off hand shows " + p.getItemInHand(InteractionHand.OFF_HAND));
                    if (twoHanded(v) && !p.getOffhandItem().isEmpty()) return SelfTests.Result.fail("getOffhandItem should be hidden (Better Combat's rule), shows " + p.getOffhandItem());
                }
                switch (g) {
                    case 0 -> ((DjinnSealItem) seal.getItem()).use(level, p, InteractionHand.MAIN_HAND);
                    case 1 -> ((DjinnSealItem) seal.getItem()).use(level, p, InteractionHand.OFF_HAND);
                    case 2 -> {
                        ItemStack carried = DjinnItems.sealStack(d);
                        Slot slot = new Slot(p.getInventory(), 7, 0, 0);
                        if (!((DjinnSealItem) carried.getItem()).overrideStackedOnOther(carried, slot, ClickAction.SECONDARY, p)) return SelfTests.Result.fail("the inventory click was not taken over");
                        seal = carried;
                    }
                    case 3 -> DjinnBinding.fromInventory(p, BindGesture.Gesture.COMMAND, DjinnBinding.Pick.SLOT, 12, d);
                    case 4 -> DjinnBinding.fromInventory(p, BindGesture.Gesture.COMMAND, DjinnBinding.Pick.OFF, 0, null);
                    default -> DjinnBinding.fromInventory(p, BindGesture.Gesture.KEY, DjinnBinding.Pick.AUTO, 0, null);       // what DjinnEngine.onKey runs for J with a Seal in the pack
                }
                DjinnState st = DjinnState.of(p);
                if (!st.owns() || st.djinn() != d) return SelfTests.Result.fail(names[g] + ": the Djinn was not bound (state owns=" + st.owns() + ")");
                if (g == 2 || g == 3) {                                                              // bound in a pack slot: it counts once it is held (as it always did)
                    for (int i = 0; i < 36; i++) {
                        ItemStack s = p.getInventory().items.get(i);
                        if (VesselAccess.boundDjinn(s) == d) { p.getInventory().items.set(0, s); if (i != 0) p.getInventory().items.set(i, ItemStack.EMPTY); break; }
                    }
                }
                ItemStack found = VesselAccess.findBound(p, st.serial());
                if (found.isEmpty()) return SelfTests.Result.fail(names[g] + ": the bound vessel is not found on the player");
                if (VesselAccess.boundDjinn(found) != d) return SelfTests.Result.fail(names[g] + ": the vessel found is not stamped");
                if (!(found.getItem() == v.getItem())) return SelfTests.Result.fail(names[g] + ": the stamped stack is not the vessel");

                // the Djinn weapon overlays the vessel in the selected slot and gives it back
                p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
                clear(p);
                p.getInventory().items.set(2, found.copy());
                p.getInventory().selected = 2;
                state.putInt("phase", DjinnPhaseRules.HALF);
                DjinnWeaponService.apply(p);
                if (!(p.getInventory().items.get(2).getItem() instanceof DjinnWeaponItem)) return SelfTests.Result.fail(names[g] + ": the Djinn weapon did not take the vessel's slot");
                if (VesselAccess.findBound(p, st.serial()).isEmpty()) return SelfTests.Result.fail(names[g] + ": the overlaid vessel is not found (the Equip would end)");
                DjinnWeaponService.release(p);
                state.putInt("phase", DjinnPhaseRules.OFF);
                ItemStack back = p.getInventory().items.get(2);
                if (back.getItem() != v.getItem() || VesselAccess.boundDjinn(back) != d) return SelfTests.Result.fail(names[g] + ": the vessel did not come back bound: " + back);

                DjinnBinding.releaseOwned(p);                                                 // the bond ends: ledger, state and the stamp
                ledger.reset(d);
                if (VesselAccess.boundDjinn(back) != null) return SelfTests.Result.fail(names[g] + ": the stamp stayed after the release");
                p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
                done.add(names[g]);
            }
            // a refused gesture says why, and nothing is bound
            clear(p);
            p.getInventory().items.set(0, DjinnItems.sealStack(d));
            p.getInventory().offhand.set(0, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            if (DjinnBinding.fromInventory(p, BindGesture.Gesture.COMMAND, DjinnBinding.Pick.OFF, 0, null) || DjinnState.of(p).owns()) return SelfTests.Result.fail("an iron sword was bound as a vessel");
            return SelfTests.Result.ok(done.size() + " gestures bind a Seal to a Better Combat weapon vessel (" + String.join("; ", done) + "), the overlay works, an iron sword is refused");
        } finally {
            p.gameMode.changeGameModeForPlayer(before);
            clear(p);
            ledger.reset(d);
            for (String k : new ArrayList<>(state.getAllKeys())) state.remove(k);
            state.merge(backup);
        }
    }
}
