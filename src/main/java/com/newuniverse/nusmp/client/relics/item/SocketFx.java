package com.newuniverse.nusmp.client.relics.item;

import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.socket.SocketCatalog;
import com.newuniverse.nusmp.relics.socket.SocketComponents;
import com.newuniverse.nusmp.relics.socket.SocketData;
import com.newuniverse.nusmp.relics.socket.SocketDef;
import com.newuniverse.nusmp.relics.socket.SocketFxKind;
import com.newuniverse.nusmp.relics.socket.SocketService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * The client half of the sockets (1.29.0): ambient particles round a relic that carries a bound Spirit or Devil, and the tooltip lines. The particles
 * are one client tick's work, not a draw call, so the renderer stays free: per tick at most {@link SocketFxRules#MAX_HOLDERS} holders and
 * {@link SocketFxRules#MAX_PARTICLES_PER_TICK} particles, only holders within the view distance (and 24 blocks), staggered by entity id, one burst every
 * 2 to 8 ticks by the quality option, and nothing at all at quality 0. Nothing is allocated per tick but what the particle engine itself takes: the
 * particle options are built once per socket, the slots array is static and the lookups never box.
 * <p>
 * The quality is {@code relicfx.quality}, the shared relic effects option created by the worn-relic layer; it is read reflectively (a few likely homes,
 * as a static method or a config value) and every failure means {@link SocketFxRules#DEFAULT_QUALITY}, so this class neither depends on nor breaks
 * without it. The look it finds is cached for ten seconds.
 */
public final class SocketFx {
    private SocketFx() {}

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET};
    private static final double[] SLOT_HEIGHT = {0.95, 0.95, 1.7, 1.1, 0.15};

    /** The particles of one socket: the family's own particle (or null) and a coloured dust mote. */
    private record Look(ParticleOptions base, ParticleOptions dust) {}

    private static final IdentityHashMap<SocketDef, Look> LOOKS = new IdentityHashMap<>();

    public static void init(IEventBus gameBus) {
        gameBus.addListener(SocketFx::onClientTick);
        gameBus.addListener(SocketFx::onTooltip);
    }

    private static Look look(SocketDef d) {
        Look l = LOOKS.get(d);
        if (l != null) return l;
        int rgb = d.rgb();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), 0.8f);
        ParticleOptions base = switch (d.fx()) {
            case EMBER -> ParticleTypes.SMALL_FLAME;
            case DROPLET -> ParticleTypes.FALLING_WATER;
            case WISP -> ParticleTypes.CLOUD;
            case MOTE -> null;
            case LIGHT -> ParticleTypes.END_ROD;
            case SMOKE -> ParticleTypes.SMOKE;
        };
        l = new Look(base, dust);
        LOOKS.put(d, l);
        return l;
    }

    // ---------------------------------------------------------------- the particles
    private static int quality = SocketFxRules.DEFAULT_QUALITY;
    private static long qualityAt = Long.MIN_VALUE;

    private static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;
        long now = level.getGameTime();
        if (now - qualityAt >= 200 || now < qualityAt) {
            quality = SocketQuality.read();
            qualityAt = now;
        }
        if (quality <= 0) return;
        double maxSq = SocketFxRules.maxDistanceSq(mc.options.getEffectiveRenderDistance());
        int holders = 0, spawned = 0;
        List<net.minecraft.client.player.AbstractClientPlayer> players = level.players();
        RandomSource rng = level.random;
        for (int i = 0; i < players.size() && holders < SocketFxRules.MAX_HOLDERS && spawned < SocketFxRules.MAX_PARTICLES_PER_TICK; i++) {
            Player p = players.get(i);
            if (p.isSpectator() || p.isInvisible() || p.distanceToSqr(mc.player) > maxSq || !SocketFxRules.due(now, p.getId(), quality)) continue;
            boolean any = false;
            for (int s = 0; s < SLOTS.length && spawned < SocketFxRules.MAX_PARTICLES_PER_TICK; s++) {
                ItemStack st = p.getItemBySlot(SLOTS[s]);
                if (!SocketComponents.hasAny(st)) continue;
                SocketDef d = SocketCatalog.byId(SocketComponents.firstId(st));
                if (d == null) continue;
                any = true;
                spawned += burst(level, rng, p, d, s);
            }
            if (any) holders++;
        }
    }

    /** One burst round a slot of a holder; returns how many particles it spawned. */
    private static int burst(ClientLevel level, RandomSource rng, Player p, SocketDef d, int slot) {
        Look l = look(d);
        double yaw = Math.toRadians(p.yBodyRot);
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        double side = slot == 0 ? 0.45 : slot == 1 ? -0.45 : 0.0, fwd = slot <= 1 ? 0.35 : 0.0;
        double x = p.getX() - cos * side - sin * fwd + (rng.nextDouble() - 0.5) * 0.3;
        double z = p.getZ() - sin * side + cos * fwd + (rng.nextDouble() - 0.5) * 0.3;
        double y = p.getY() + SLOT_HEIGHT[slot] * (p.isCrouching() ? 0.85 : 1.0) + (rng.nextDouble() - 0.5) * 0.4;
        int n = 0;
        if (l.base() != null) {
            int count = SocketFxRules.perBurst(quality);
            boolean falling = d.fx() == SocketFxKind.DROPLET;
            for (int i = 0; i < count; i++) {
                level.addParticle(l.base(), x, y, z, (rng.nextDouble() - 0.5) * 0.02, falling ? 0.0 : 0.02 + rng.nextDouble() * 0.02, (rng.nextDouble() - 0.5) * 0.02);
                n++;
            }
        }
        level.addParticle(l.dust(), x, y, z, 0.0, 0.01, 0.0);
        return n + 1;
    }

    // ---------------------------------------------------------------- the tooltip
    private static void onTooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (s.isEmpty() || Relics.defOf(s) == null) return;
        int slots = SocketService.slotsOf(s);
        if (slots <= 0) return;
        SocketData data = SocketComponents.get(s);
        List<Component> tip = e.getToolTip();
        for (SocketData.Entry en : data.entries()) {
            SocketDef d = SocketCatalog.byId(en.id());
            if (d == null) continue;
            tip.add(Component.translatable("socket.nusmp.tip.bound", Component.translatable(d.nameKey()).withColor(d.rgb()), d.act().name()).withStyle(ChatFormatting.GRAY));
        }
        int free = slots - data.size();
        if (free > 0) tip.add(Component.translatable("socket.nusmp.tip.free", free).withStyle(ChatFormatting.DARK_GRAY));
    }

    // ---------------------------------------------------------------- relicfx.quality, found reflectively
    /** Looks for the shared effects quality option without depending on it. */
    static final class SocketQuality {
        private SocketQuality() {}

        private static final String[] HOMES = {
                "com.newuniverse.nusmp.client.relics.RelicFxOptions", "com.newuniverse.nusmp.client.relics.worn.RelicFxOptions",
                "com.newuniverse.nusmp.client.relics.RelicFxConfig", "com.newuniverse.nusmp.client.relics.worn.RelicFxConfig",
                "com.newuniverse.nusmp.client.relics.RelicFxQuality", "com.newuniverse.nusmp.client.RelicFxOptions"};
        private static final String[] METHODS = {"quality", "getQuality", "qualityLevel"}, FIELDS = {"QUALITY", "quality"};
        private static boolean probed;
        private static Method method;
        private static Field field;

        static int read() {
            try {
                String prop = System.getProperty("nusmp.relicfx.quality");
                if (prop != null) return SocketFxRules.parseQuality(prop);
                if (!probed) probe();
                Object v = method != null ? method.invoke(null) : field != null ? field.get(null) : null;
                if (v != null && !(v instanceof Number) && !(v instanceof Boolean) && !(v instanceof Enum<?>) && !(v instanceof CharSequence)) {
                    try {
                        v = v.getClass().getMethod("get").invoke(v);                   // a config value
                    } catch (ReflectiveOperationException ignored) {
                        // use the object's own text
                    }
                }
                return SocketFxRules.parseQuality(v);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError none) {
                return SocketFxRules.DEFAULT_QUALITY;
            }
        }

        private static void probe() {
            probed = true;
            for (String home : HOMES) {
                Class<?> c;
                try {
                    c = Class.forName(home);
                } catch (ReflectiveOperationException | LinkageError absent) {
                    continue;
                }
                for (String m : METHODS) {
                    try {
                        method = c.getMethod(m);
                        return;
                    } catch (ReflectiveOperationException ignored) {
                        // next
                    }
                }
                for (String f : FIELDS) {
                    try {
                        field = c.getField(f);
                        return;
                    } catch (ReflectiveOperationException ignored) {
                        // next
                    }
                }
            }
        }
    }
}
