package com.newuniverse.nusmp.labyrinth.dnd.economy;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.dnd.DndConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the dungeon's passive resource clock (D&amp;D torches): in the Labyrinth the dark hurts, which is what turns the lantern from loot into a
 * necessity. It is a per-player server tick sampled once a second ({@code PlayerTickEvent.Post}, {@code tickCount % 20}), not a per-tick cost, and it
 * keeps each player's {@link GloomClock} in a map keyed by UUID (cleared on logout and when the player leaves the dimension) instead of player NBT, so
 * nothing is saved and a relog is a clean slate.
 * <p>1.99.0, THE HUNGER BAN (the owner's strict global exclusion): until 1.98.0 this class also made hunger run faster in the Labyrinth
 * ({@code causeFoodExhaustion} on the difference since the last sample) and slowed a player at three shanks or fewer. Both are gone: the clock keeps its
 * class, its id and its registration, and it no longer reads or writes the food bar at all (no exhaustion, no slowness for being hungry). Its only pressure
 * left is the gloom, which is light-based. See {@code docs/labyrinth/hunger_ban.md}.
 * <p>Gloom is "block light at the eyes at or below the limit and no light in either hand for N seconds", answered with vanilla Darkness and Weakness I
 * refreshed every second (they expire by themselves five seconds after the player steps into light, so there is no state to restore). Creative and
 * spectator players are never touched. Only vanilla effects with hidden particles are used: no particle is emitted here, so nothing needs the Rukh API.
 */
public final class DungeonHunger {
    private DungeonHunger() {}

    private static final Map<UUID, GloomClock> CLOCKS = new ConcurrentHashMap<>();
    private static final Map<UUID, GloomClock.State> LAST = new ConcurrentHashMap<>();

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        if (!(p.level() instanceof ServerLevel level) || !Labyrinth.in(level) || p.isCreative() || p.isSpectator() || !p.isAlive()) {
            forget(p.getUUID());
            return;
        }
        if (!DndConfig.get(DndConfig.GLOOM)) return;
        sample(p, level);
    }

    /** One second of the clock for a survival player in the Labyrinth: only the gloom (light) is judged; the food bar is never read or written (the hunger ban). */
    public static void sample(ServerPlayer p, ServerLevel level) {
        GloomClock clock = CLOCKS.computeIfAbsent(p.getUUID(), k -> new GloomClock());

        int light = level.getBrightness(LightLayer.BLOCK, BlockPos.containing(p.getEyePosition()));
        boolean dark = GloomClock.isDark(light, DndConfig.get(DndConfig.GLOOM_LIGHT), lightInHand(p));
        int seconds = DndConfig.get(DndConfig.GLOOM_SECONDS);
        GloomClock.State now = clock.step(dark, seconds);
        GloomClock.State before = LAST.getOrDefault(p.getUUID(), GloomClock.State.SAFE);
        LAST.put(p.getUUID(), now);

        if (now == GloomClock.State.GLOOM) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0, false, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, false, true));
            p.displayClientMessage(Component.translatable("message.nusmp.labyrinth_gloom"), true);
        } else if (clock.warningEdge(now, before)) {
            p.displayClientMessage(Component.translatable("message.nusmp.labyrinth_gloom_warning"), true);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { forget(e.getEntity().getUUID()); }

    private static void forget(UUID id) {
        if (CLOCKS.isEmpty() && LAST.isEmpty()) return;
        CLOCKS.remove(id);
        LAST.remove(id);
    }

    /** A light in either hand: a block item whose block emits light, or an item the id test knows ({@link LightItems}). */
    static boolean lightInHand(ServerPlayer p) {
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(h);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().getLightEmission() > 0) return true;
            var id = BuiltInRegistries.ITEM.getKey(s.getItem());
            if (LightItems.isLight(id.getNamespace(), id.getPath())) return true;
        }
        return false;
    }
}
