package com.newuniverse.nusmp.dungeon.nexus.nav;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The Dungeon Delving Gear's heartbeat (docs/living_dungeon.md section 3), one handler on the player tick. Every {@value FlameMode#SCAN_EVERY}
 * ticks a Ki-Resonance Lantern in the off-hand is looked after: its fuel burns (once a second, 20 ticks at a time, which is one per tick on
 * average), the cone is scanned for illusion stone and for the nearest descent gate, the flame component is set and the flame is drawn. A
 * lantern anywhere else goes dark (a flame only burns in the off-hand). Every two seconds an Anti-Bird Compass in a hand or on the hotbar
 * learns where the nearest cluster of life is (only inside the Dungeon; elsewhere it has no target). Components are only written when
 * they change, so a stack is not re-sent for nothing.
 */
public final class NavTicker {
    private NavTicker() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static int warned;

    private static final int COMPASS_EVERY = 40, COMPASS_AT = 7;

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator()) return;
        int t = p.tickCount;
        boolean lantern = t % FlameMode.SCAN_EVERY == 0, compass = t % COMPASS_EVERY == COMPASS_AT;
        if (!lantern && !compass) return;
        try {
            if (lantern) lantern(p, t);
            if (compass) compass(p);
        } catch (RuntimeException | LinkageError ex) {
            if (warned++ < 5) LOG.warn("Dungeon Delving Gear tick failed (further failures are not logged): {}", ex.toString());
        }
    }

    // ---------------------------------------------------------------- the lantern
    private static void lantern(ServerPlayer p, int t) {
        ItemStack off = p.getOffhandItem();
        if (t % 20 == 0) darken(p, off);
        if (!off.is(NavItems.KI_RESONANCE_LANTERN.get())) return;
        ServerLevel level = p.serverLevel();
        int fuel = LanternItem.fuelOf(off);
        if (fuel > 0 && t % 20 == 0) {
            fuel = LanternFuel.burn(fuel, 20);
            LanternItem.setFuel(off, fuel);
            if (fuel == 0) p.displayClientMessage(Component.literal("The lantern gutters out. It wants fuel.").withStyle(ChatFormatting.GOLD), true);
        }
        boolean blue = false, red = false;
        if (fuel > 0) {
            blue = LanternSense.illusionAhead(level, p);
            red = !blue && LanternSense.gateAhead(level, p);
        }
        FlameMode mode = FlameMode.choose(fuel > 0, blue, red);
        LanternItem.setFlame(off, mode);
        if (mode.lit()) LanternSense.flame(level, p, mode);
    }

    /** A flame only burns in the off-hand: any other lantern in the inventory is put out. */
    private static void darken(ServerPlayer p, ItemStack off) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s != off && s.is(NavItems.KI_RESONANCE_LANTERN.get())) LanternItem.setFlame(s, FlameMode.OFF);
        }
    }

    // ---------------------------------------------------------------- the compass
    private static void compass(ServerPlayer p) {
        List<ItemStack> held = new ArrayList<>(2);
        ItemStack off = p.getOffhandItem();
        if (off.is(NavItems.ANTI_BIRD_COMPASS.get())) held.add(off);
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(NavItems.ANTI_BIRD_COMPASS.get())) held.add(s);
        }
        if (held.isEmpty()) return;
        ServerLevel level = p.serverLevel();
        BlockPos target = LivingDungeon.in(level) ? CompassSense.cluster(level, p) : null;     // above ground it has nothing to point at
        for (ItemStack s : held) AntiBirdCompassItem.setTarget(s, target);
    }
}
