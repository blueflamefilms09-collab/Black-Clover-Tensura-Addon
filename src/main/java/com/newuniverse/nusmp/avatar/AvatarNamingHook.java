package com.newuniverse.nusmp.avatar;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.avatar.naming.NameRules;
import com.newuniverse.nusmp.avatar.naming.TensuraNamingEngine;
import dev.architectury.event.EventResult;
import io.github.manasmods.tensura.event.TensuraEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Why: a player can also name the Avatar with Tensura's own naming (the same ritual they use on any monster). The owner wants that to count: the bond, the
 * evolution, the creator's tax and the Naming Sleep must apply however the name was given. This listens on Tensura's naming event; it only reacts when the
 * target is an Avatar of the very player naming it, rejects an invalid or duplicate name before Tensura spends anything, and otherwise lets Tensura do its own
 * EP gain and adds the Avatar parts on top (without a second EP surge). Registered once at common setup and fully defensive: if the event or its signature is
 * not what this was written against, the hook logs once and the GUI naming still works.
 */
final class AvatarNamingHook {
    private AvatarNamingHook() {}

    private static final Logger LOG = LogUtils.getLogger();

    static void register() {
        try {
            TensuraEntityEvents.NAMING_EVENT.register((target, namer, a, b, type, name) -> {
                try {
                    if (!(target instanceof AvatarEntity av) || !(namer instanceof ServerPlayer p) || !AvatarConfig.enabled()) return EventResult.pass();
                    if (av.creatorUuid() == null || !av.creatorUuid().equals(p.getUUID()) || av.isNamed()) return EventResult.pass();
                    String wanted = NameRules.clean(name.get());
                    var others = new java.util.ArrayList<>(AvatarManager.get(p.getServer()).names(p.getUUID()));
                    others.remove(av.avatarName());
                    NameRules.Verdict v = NameRules.check(wanted, others);
                    if (v != NameRules.Verdict.OK) {
                        p.sendSystemMessage(net.minecraft.network.chat.Component.literal(NameRules.message(v)).withStyle(net.minecraft.ChatFormatting.RED));
                        return EventResult.interruptFalse();
                    }
                    name.set(wanted);
                    // run after Tensura finished its own naming this tick
                    p.getServer().execute(() -> TensuraNamingEngine.apply(p, av, wanted, false));
                } catch (RuntimeException ex) {
                    LOG.warn("[nusmp] Avatar naming hook failed: {}", ex.toString());
                }
                return EventResult.pass();
            });
        } catch (Throwable t) {
            LOG.warn("[nusmp] Could not hook Tensura's naming event ({}); Avatars can still be named from their menu", t.toString());
        }
    }
}
