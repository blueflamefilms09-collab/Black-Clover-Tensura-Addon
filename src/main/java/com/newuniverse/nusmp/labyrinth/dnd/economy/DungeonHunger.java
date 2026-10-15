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
 * Why: the dungeon's passive resource clock (D&amp;D torches and rations): in the Labyrinth hunger runs faster and the dark hurts, which is
 * what turns the monster meat and the lantern from loot into necessities. It is a per-player server tick sampled once a second
 * ({@code PlayerTickEvent.Post}, {@code tickCount % 20}), not a per-tick cost, and it keeps each player's {@link GloomClock} in a map keyed
 * by UUID (cleared on logout and when the player leaves the dimension) instead of player NBT, so nothing is saved and a relog is a clean slate.
 * Hunger is a multiplier done with {@code causeFoodExhaustion} on the difference since the last sample (so it scales with what the player
 * actually did and never invents hunger for someone standing still); gloom is "block light at the eyes at or below the limit and no light in
 * either hand for N seconds", answered with vanilla Darkness and Weakness I refreshed every second (they expire by themselves five seconds
 * after the player steps into light, so there is no state to restore) and Slowness I at three shanks or fewer. Creative and spectator players
 * are never touched. Only vanilla effects with hidden particles are used: no particle is emitted here, so nothing needs the Rukh API.
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
        GloomClock clock = CLOCKS.computeIfAbsent(p.getUUID(), k -> new GloomClock());

        double drain = DndConfig.get(DndConfig.HUNGER_DRAIN);
        float extra = clock.extraExhaustion(p.getFoodData().getExhaustionLevel(), drain);
        if (extra > 0f) p.causeFoodExhaustion(extra);
        clock.settle(p.getFoodData().getExhaustionLevel());

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
        if (GloomClock.starving(p.getFoodData().getFoodLevel())) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, true));
            if (p.tickCount % 200 == 0) p.displayClientMessage(Component.translatable("message.nusmp.labyrinth_hunger"), true);
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
