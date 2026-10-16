package com.newuniverse.nusmp.vfx.client.portal;

/**
 * Why: the gate is drawn in a fixed back-to-front order and each part has its own texture and blend; naming the parts lets one
 * composer feed both the plain vertex buffer (preview, low tier) and the direct GL drawer (shader path) without either knowing
 * about the other. {@code texture} is the name under textures/particle, {@code additive} the blend.
 */
public enum PortalPass {
    BACKDROP("portal_core", false),
    SHELL("portal_shell", true),
    HORIZON("portal_swirl", true),
    RIM("portal_rim", true),
    RUNES("portal_runes", true),
    BOLT("arc_bolt", true),
    THREAD("portal_thread", true);

    public final String texture;
    public final boolean additive;

    PortalPass(String texture, boolean additive) { this.texture = texture; this.additive = additive; }
}
