package com.newuniverse.nusmp.client.relics.item.vu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.DemonSlayerMesh;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import com.newuniverse.nusmp.visual.demonslayer.DsDynamics;
import com.newuniverse.nusmp.visual.genesis.VuRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

/**
 * 1.108.0 (visual upgrade, slice demon_slayer): the Genesis Demon-Slayer drawn by the upgraded pipeline of the Genesis weapons ({@link VuRenderer}, mesh
 * {@code weapon_meshes_vu/demon_slayer_sword.txt}) instead of the 1.0 pixel model of {@link DemonSlayerMesh}, which stays on disk and is drawn again when the client flag
 * {@code visual.upgrade} is off. Called from the top of {@code DemonSlayerRenderer#renderByItem}; false means "draw the old model".
 * <p>
 * What the old renderer did with the stack stays: the stack's {@code MeteorUntil} (Black Meteorite) breaks the blade into four shards ({@link DemonSlayerMesh#fracture}, the same
 * 30-tick curve), its {@code Resonance} (0..4) grows the pommel's clover and its bloom, and {@code BladeGrowthUntil} (Black Divider) draws the blade 1.6 times as large; the first two
 * reach the groups of the mesh through {@link DsDynamics} (the stack data is read without copying: no allocation per frame), the third scales the pose about the item's centre as before.
 * <p>
 * The tick half ({@link #init}): the signature bursts of the three acts for the local player (Black Meteorite and Black Divider from the stack data the server writes when they start,
 * Conceptual Severance from the start of a full-charge swing) through {@link VuFx#onAct}, and the embers of the Resonance and of the fracture, all through the shared client particle budget
 * and at most {@link VuRules#particleBudget} particles a tick (none at low effect quality).
 */
public final class DemonSlayerVu {
    private DemonSlayerVu() {}

    public static final String ID = "demon_slayer_sword";
    private static final float GROWTH = 1.6f;
    private static final ParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.86f, 0.07f, 0.18f), 0.9f), EMBER = new DustParticleOptions(new Vector3f(1.0f, 0.23f, 0.29f), 0.8f);
    private static boolean inited;
    private static long seenMeteor, seenGrowth;
    private static boolean wasSwinging;
    private static float lastStrength = 1f;

    public static synchronized void init() {
        if (inited) return;
        inited = true;
        NeoForge.EVENT_BUS.addListener(DemonSlayerVu::onClientTick);
    }

    // ================================================================ drawing
    /** True when the stack was drawn here (the flag is on and the upgraded mesh is there); false = the caller draws the old model. */
    public static boolean draw(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!VuRenderer.has(ID)) return false;
        Minecraft mc = Minecraft.getInstance();
        float time = mc.level == null ? 0f : mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        CompoundTag data = data(stack);
        long until = data == null ? 0L : data.getLong("MeteorUntil");
        float fracture = until > 0L && mc.level != null ? DemonSlayerMesh.fracture(until - time) : 0f;
        int resonance = data == null ? 0 : data.getInt("Resonance");
        boolean grown = mc.level != null && data != null && data.getLong("BladeGrowthUntil") > time;
        if (grown) {
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.scale(GROWTH, GROWTH, GROWTH);
            pose.translate(-0.5f, -0.5f, -0.5f);
        }
        DsDynamics.begin(fracture, resonance);
        try {
            return VuRenderer.draw(stack, ID, ctx, pose, buffers, light, overlay);
        } finally {
            DsDynamics.end();
            if (grown) pose.popPose();
        }
    }

    /** The inventory slot: with {@code visual.guiModel} on, the upgraded mesh tilted (the old 32x32 icon stays otherwise); at rest, with the stack's resonance. */
    public static boolean drawGui(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!VuRenderer.has(ID) || !VuOptions.guiModel()) return false;
        CompoundTag data = data(stack);
        DsDynamics.begin(0f, data == null ? 0 : data.getInt("Resonance"));
        try {
            return VuRenderer.drawGui(stack, ID, pose, buffers, light, overlay);
        } finally {
            DsDynamics.end();
        }
    }

    /** The stack's custom data without a copy (read only), or null. */
    private static CompoundTag data(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        return cd == null ? null : cd.getUnsafe();
    }

    // ================================================================ the client tick
    private static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer p = mc.player;
        if (level == null || p == null || mc.isPaused() || !VuOptions.upgrade() || !VuOptions.particles()) return;
        ItemStack st = p.getMainHandItem();
        boolean holding = st.is(NUItems.DEMON_SLAYER.get());
        boolean swinging = p.swinging;
        if (!holding) {
            wasSwinging = swinging;
            lastStrength = 1f;
            return;
        }
        long now = level.getGameTime();
        CompoundTag data = data(st);
        // the signature bursts of the three acts (the local player's; the server writes the data when the act starts)
        long meteor = data == null ? 0L : data.getLong("MeteorUntil"), growth = data == null ? 0L : data.getLong("BladeGrowthUntil");
        if (meteor > now && meteor != seenMeteor) {
            seenMeteor = meteor;
            VuFx.onAct(ID, "black_meteorite", meteor);
        }
        if (growth > now && growth != seenGrowth) {
            seenGrowth = growth;
            VuFx.onAct(ID, "black_divider", growth);
        }
        if (swinging && !wasSwinging && lastStrength >= 0.9f) VuFx.onAct(ID, "conceptual_severance", now + 1);
        wasSwinging = swinging;
        lastStrength = p.getAttackStrengthScale(0f);
        // embers: the resonance (a faint rise from the pommel, more the stronger it is) and the fracture (sparks along the blade)
        VfxQuality q = PerfSettings.vfxQuality();
        int per = VuRules.particleBudget(q);
        if (per <= 0) return;
        int res = data == null ? 0 : DsDynamics.clampResonance(data.getInt("Resonance"));
        boolean fractured = meteor > now && DemonSlayerMesh.fracture(meteor - now) > 0f;
        int want = (fractured ? 2 : 0) + (res > 0 && now % Math.max(1, 6 - res) == 0 ? 1 : 0);
        if (want <= 0) return;
        want = PerfBudgets.clientParticles(now, Math.min(per, want));
        if (want <= 0) return;
        Vec3 look = p.getLookAngle();
        double yaw = Math.toRadians(p.yBodyRot), sin = Math.sin(yaw), cos = Math.cos(yaw);
        double hx = p.getX() - cos * 0.45 - sin * 0.35, hz = p.getZ() - sin * 0.45 + cos * 0.35, hy = p.getY() + (p.isCrouching() ? 0.8 : 0.95);
        for (int i = 0; i < want; i++) {
            double len = fractured ? 0.2 + level.random.nextDouble() * 1.1 : -0.1 + level.random.nextDouble() * 0.25;
            double x = hx + look.x * len + (level.random.nextDouble() - 0.5) * 0.2, y = hy + look.y * len * 0.5 + 0.3 + level.random.nextDouble() * 0.4, z = hz + look.z * len + (level.random.nextDouble() - 0.5) * 0.2;
            level.addParticle(fractured ? EMBER : CRIMSON, x, y, z, (level.random.nextDouble() - 0.5) * 0.03, 0.02 + 0.01 * res, (level.random.nextDouble() - 0.5) * 0.03);
        }
    }
}
