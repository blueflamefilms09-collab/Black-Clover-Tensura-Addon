package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import net.minecraft.client.gui.GuiGraphics;

/** Why: harness shim: the real preview draws the 3D model; here a flat figure shows where it would stand and how big it would be. */
final class AvatarPreview {
    private AvatarPreview() {}

    static void draw(GuiGraphics g, int cx, int feetY, float pxPerBlock, AppearanceData look, String raceId, float spin, float age) {
        float sc = pxPerBlock * look.scale();
        int head = Math.round(0.5f * sc), body = Math.round(0.75f * sc), legs = Math.round(0.75f * sc), w = Math.round(0.5f * sc);
        int skin = 0xFF000000 | look.skinTone();
        g.pose().pushPose();
        g.pose().translate(0f, 0f, 150f);
        g.fill(cx - w / 2, feetY - legs, cx + w / 2, feetY, 0xFF3A4A78);
        g.fill(cx - w / 2 - 2, feetY - legs - body, cx + w / 2 + 2, feetY - legs, 0xFF000000 | (look.outfitId() * 0x151515 & 0xFFFFFF | 0x404060));
        g.fill(cx - head / 2, feetY - legs - body - head, cx + head / 2, feetY - legs - body, skin);
        g.fill(cx - head / 2, feetY - legs - body - head, cx + head / 2, feetY - legs - body - head + Math.max(2, head / 4), 0xFF000000 | (look.hairId() * 0x0F0F0F + 0x303030));
        int eye = Math.max(1, head / 6);
        g.fill(cx - head / 4, feetY - legs - body - head / 2, cx - head / 4 + eye, feetY - legs - body - head / 2 + eye, 0xFF000000 | look.eyeRgb());
        g.fill(cx + head / 4 - eye, feetY - legs - body - head / 2, cx + head / 4, feetY - legs - body - head / 2 + eye, 0xFF000000 | look.eyeRgb());
        g.pose().popPose();
    }
}
