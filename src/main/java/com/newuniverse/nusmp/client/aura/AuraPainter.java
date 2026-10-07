package com.newuniverse.nusmp.client.aura;

/** 0.53: draws one aura over one player, once per frame while it lasts. See {@link AuraContext} and {@link AuraRender}. */
@FunctionalInterface
public interface AuraPainter {
    void paint(AuraContext c);
}
