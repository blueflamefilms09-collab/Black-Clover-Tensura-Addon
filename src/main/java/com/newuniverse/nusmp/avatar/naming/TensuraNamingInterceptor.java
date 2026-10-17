package com.newuniverse.nusmp.avatar.naming;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarManager;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import dev.architectury.event.EventResult;
import io.github.manasmods.tensura.event.TensuraEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;

/**
 * Why: a player can also name the Avatar with Tensura's own naming (the same ritual they use on any monster), and the owner wants that to count: the D&D stat surge,
 * the race evolution by JSON and the Soul-Bond Lock must apply however the name was given. This is the interceptor of Tensura's {@code NAMING_EVENT} the brief asks
 * for. It only reacts when the target is an Avatar of the very player naming it; it rejects an invalid or duplicate name BEFORE Tensura spends anything (the event is
 * cancelled and the player is told), it remembers the Avatar's EP from before Tensura's own gain, and it lets Tensura finish its naming and then, the same tick, lets
 * {@link TensuraNamingEngine#apply} add the Avatar parts on top: the bond, the evolution, the surge as a floor (the larger of Tensura's gain and ours, never both), the
 * creator's tax and sleep. Registered once at common setup and fully defensive: if the event or its signature is not what this was written against, it logs once and
 * the naming from the Avatar's own menu still works. {@code INameEvolution} (Tensura's own evolve-on-name for its monsters) is deliberately NOT implemented by the
 * Avatar: its defaults are unknown and would evolve the Avatar a second time.
 */
public final class TensuraNamingInterceptor {
    private static final Logger LOG = LogUtils.getLogger();

    private TensuraNamingInterceptor() {}

    public static void register() {
        try {
            TensuraEntityEvents.NAMING_EVENT.register((target, namer, a, b, type, name) -> {
                try {
                    if (!(target instanceof AvatarEntity av) || !(namer instanceof ServerPlayer p) || !AvatarConfig.enabled()) return EventResult.pass();
                    if (av.creatorUuid() == null || !av.creatorUuid().equals(p.getUUID()) || av.isNamed()) return EventResult.pass();
                    String wanted = NameRules.clean(name.get());
                    var others = new ArrayList<>(AvatarManager.get(p.getServer()).names(p.getUUID()));
                    others.remove(av.avatarName());
                    NameRules.Verdict v = NameRules.check(wanted, others);
                    if (v != NameRules.Verdict.OK) {
                        p.sendSystemMessage(Component.literal(NameRules.message(v)).withStyle(ChatFormatting.RED));
                        return EventResult.interruptFalse();
                    }
                    name.set(wanted);
                    double epBefore = TensuraBridge.baseMaxEp(av);                  // before Tensura adds its own gain
                    // run after Tensura finished its own naming this tick
                    p.getServer().execute(() -> TensuraNamingEngine.apply(p, av, wanted, false, epBefore));
                } catch (RuntimeException ex) {
                    LOG.warn("[nusmp] Avatar naming interceptor failed: {}", ex.toString());
                }
                return EventResult.pass();
            });
        } catch (Throwable t) {
            LOG.warn("[nusmp] Could not hook Tensura's naming event ({}); Avatars can still be named from their menu", t.toString());
        }
    }
}
