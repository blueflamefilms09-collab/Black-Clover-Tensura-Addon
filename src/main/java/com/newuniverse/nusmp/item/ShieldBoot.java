package com.newuniverse.nusmp.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.visual.shields.ShieldModelCheck;
import com.newuniverse.nusmp.visual.shields.ShieldSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.110.0 (docs/visual_upgrade/shield_fix.md): the server end of "every shield of the mod is used like a vanilla shield". Vanilla does the blocking itself as long as the item
 * answers {@code UseAnim.BLOCK}, {@code SHIELD_BLOCK} and a long use duration and starts using in {@code use}; the item classes of the mod do that (MagicWeaponItem,
 * HeroWeaponItem, RelicWeaponItem). What lives here is
 * <ul>
 *   <li><b>Axes still break a modded shield</b>: a blow from something that can disable shields ({@code canDisableShield}: an axe in hand) puts the shield being used on the
 *       100-tick cooldown, whichever item it is. NeoForge's disable already puts the used item on cooldown; this makes the rule independent of that, and leaves the vanilla
 *       shield alone. All state is the server's; nothing is read from a client.</li>
 *   <li><b>The self-tests</b> {@code visual.shields.*} of the CI server smoke test: the class table of the shields, using each from either hand, and the model jsons inside the jar.</li>
 * </ul>
 */
public final class ShieldBoot {
    private ShieldBoot() {}

    /** The cooldown vanilla gives a shield an axe has knocked down, in ticks. */
    static final int DISABLE_TICKS = 100;
    private static boolean booted;

    public static synchronized void init() {
        if (booted) return;
        booted = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, ShieldBoot::onShieldBlock);
        SelfTests.register("visual.shields.items", ShieldBoot::items);
        SelfTests.register("visual.shields.use", ShieldBoot::use);
        SelfTests.register("visual.shields.models", ShieldBoot::models);
    }

    private static void onShieldBlock(LivingShieldBlockEvent e) {
        if (e.isCanceled() || !e.getBlocked() || !(e.getEntity() instanceof ServerPlayer p)) return;
        if (!(e.getDamageSource().getDirectEntity() instanceof LivingEntity attacker) || !attacker.canDisableShield()) return;
        ItemStack used = p.getUseItem();
        if (used.isEmpty() || used.is(Items.SHIELD) || !used.canPerformAction(ItemAbilities.SHIELD_BLOCK)) return;
        p.getCooldowns().addCooldown(used.getItem(), DISABLE_TICKS);
    }

    // ================================================================ self-tests
    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, id)); }

    /** Every shield-class item is registered and its class answers what vanilla's blocking reads. */
    private static SelfTests.Result items(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.player(level, "shield_items", 0.5, 100, 0.5);
        for (String id : ShieldSet.IDS) {
            Item it = item(id);
            if (it == Items.AIR) {
                bad.add(id + " is not registered");
                continue;
            }
            ItemStack s = new ItemStack(it);
            if (it.getUseAnimation(s) != UseAnim.BLOCK) bad.add(id + " does not answer UseAnim.BLOCK");
            if (it.getUseDuration(s, p) < 72000) bad.add(id + " has a use duration under 72000");
            if (!it.canPerformAction(s, ItemAbilities.SHIELD_BLOCK)) bad.add(id + " does not answer SHIELD_BLOCK");
        }
        return result(bad, ShieldSet.IDS.size() + " shields answer BLOCK / 72000 / SHIELD_BLOCK");
    }

    /** Using each shield from the main hand and from the off hand starts the use (the server end of the right-click hold), and lowering it ends it. */
    private static SelfTests.Result use(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.player(level, "shield_use", 0.5, 100, 0.5);
        p.setShiftKeyDown(false);
        for (String id : ShieldSet.IDS) {
            Item it = item(id);
            if (it == Items.AIR) continue;
            for (InteractionHand hand : InteractionHand.values()) {
                try {
                    p.stopUsingItem();
                    p.setItemInHand(hand, new ItemStack(it));
                    InteractionResultHolder<ItemStack> r = p.getItemInHand(hand).use(level, p, hand);
                    boolean using = p.isUsingItem() && p.getUsedItemHand() == hand && p.getUseItem().getUseAnimation() == UseAnim.BLOCK;
                    if (!using) bad.add(id + " does not start blocking from the " + hand.name().toLowerCase() + " (" + r.getResult() + ")");
                } catch (RuntimeException ex) {
                    bad.add(id + " " + hand.name().toLowerCase() + ": " + ex);
                } finally {
                    p.stopUsingItem();
                    p.setItemInHand(hand, ItemStack.EMPTY);
                }
            }
        }
        return result(bad, ShieldSet.IDS.size() + " shields start blocking from either hand");
    }

    /** The model jsons inside the running jar are the vanilla shield preset with the blocking override. */
    private static SelfTests.Result models(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        for (String id : ShieldSet.IDS) bad.addAll(ShieldModelCheck.problems(id, json("/assets/nusmp/models/item/" + id + ".json"), json("/assets/nusmp/models/item/" + id + "_blocking.json")));
        return result(bad, ShieldSet.IDS.size() + " shield models carry vanilla's display and the blocking override");
    }

    private static JsonObject json(String path) {
        try (InputStream in = ShieldBoot.class.getResourceAsStream(path)) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception ex) {
            return null;
        }
    }

    private static SelfTests.Result result(List<String> bad, String ok) {
        if (bad.isEmpty()) return SelfTests.Result.ok(ok);
        return SelfTests.Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))) + (bad.size() > 6 ? " (+" + (bad.size() - 6) + " more)" : ""));
    }
}
