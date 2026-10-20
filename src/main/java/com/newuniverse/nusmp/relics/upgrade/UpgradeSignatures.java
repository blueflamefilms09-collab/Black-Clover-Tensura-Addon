package com.newuniverse.nusmp.relics.upgrade;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Why: every act of an upgraded weapon draws a signature effect of its own on top of the act's old effect (the shape the act always had stays). The server only says which one: it sends the
 * effect {@link VfxShape#UPGRADE_SIGNATURE} whose seed carries the signature id of (item, act) ({@link UpgradeItems#signatureId}); the client looks the recipe up in the item's spec
 * (assets/nusmp/visual_upgrade/&lt;id&gt;.json) and draws it. The packet follows the caster; {@code to} is the point the caster looks at (24 blocks at most). Called by
 * {@code RelicEngine} right after an act's body has run; it changes nothing in the game, never throws, and goes through {@link VfxSpawn#send} (so the per-tick effect budget applies).
 */
public final class UpgradeSignatures {
    private UpgradeSignatures() {}

    private static final Logger LOG = LoggerFactory.getLogger("nusmp");
    private static boolean failedOnce;

    /** Duration of the carrying effect, ticks: every recipe's last component ends before it (tests check it). */
    public static final int DURATION = UpgradeItems.SIGNATURE_DURATION;

    public static void afterAct(ServerPlayer p, RelicDef def, Act act) {
        if (p == null || def == null || act == null || !UpgradeItems.has(def.id())) return;
        try {
            ServerLevel level = p.serverLevel();
            Vec3 to = p.pick(24.0, 1.0f, false).getLocation();
            int sig = UpgradeItems.signatureId(def.id(), act.key());
            VfxSpawn.send(level, new VfxPayload(VfxShape.UPGRADE_SIGNATURE.ordinal(), p.position(), to, 0xFFFFFFFF, DURATION, 1f, p.getId(), UpgradeItems.packSeed(sig, level.random.nextInt())));
        } catch (RuntimeException ex) {
            if (!failedOnce) {
                failedOnce = true;
                LOG.error("[nusmp] the signature effect of {} / {} failed (logged once)", def.id(), act.key(), ex);
            }
        }
    }
}
