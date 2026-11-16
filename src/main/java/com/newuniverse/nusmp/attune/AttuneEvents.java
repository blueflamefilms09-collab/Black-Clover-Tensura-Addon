package com.newuniverse.nusmp.attune;

import com.mojang.brigadier.context.CommandContext;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Why: the game events of meditation, each cheap for everybody: the damage event returns at once unless the setting is on and the dealer is a player (then {@link AttunePowers#out}: a few attachment
 * reads and one map lookup per registered spirit); logout, death, a change of dimension and the server stopping drop the player's session; login tells a player whose saved attunement now clashes which
 * power is suppressed. The command {@code /meditate} (everyone, own powers only): no argument opens the screen, {@code status} lists the powers, {@code clear} forgets the attunement; operators
 * (permission 2) have {@code status <player>} and {@code clear <player>}. Nothing runs per tick.
 */
public final class AttuneEvents {
    private AttuneEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOW, AttuneEvents::onDamage);                // with the 3.74.0 power raise (the same priority); AttuneService.factor aims at a total, so their order does not matter
        bus.addListener(AttuneEvents::onLogin);
        bus.addListener(AttuneEvents::onLogout);
        bus.addListener(AttuneEvents::onDimension);
        bus.addListener(AttuneEvents::onDeath);
        bus.addListener(AttuneEvents::onStopping);
        bus.addListener(AttuneEvents::onCommands);
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity().level().isClientSide || e.getAmount() <= 0f || !AttuneConfig.enabled()) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer dealer) || dealer == e.getEntity()) return;
        SafeCast.tick("attune.damage", () -> e.setAmount(AttuneService.apply(dealer, e.getEntity(), e.getAmount())));
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !AttuneConfig.enabled()) return;
        AttuneStore.State s = AttuneStore.peek(p);
        if (s == null || s.slots.size() < 2) return;
        SafeCast.tick("attune.login", () -> {
            for (String line : AttuneService.status(p)) if (line.endsWith("SUPPRESSED")) AttuneService.say(p, "attune.msg.suppressed", "Clashing powers: %s. Meditate (/meditate) to settle them.", ChatFormatting.YELLOW, line.strip());
        });
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { AttuneService.forget(e.getEntity().getUUID()); }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { AttuneService.forget(e.getEntity().getUUID()); }

    private static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) AttuneService.forget(p.getUUID()); }

    private static void onStopping(ServerStoppingEvent e) { AttuneService.clearAll(); }

    // ------------------------------------------------------------------ /meditate
    private static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("meditate")
                .executes(c -> open(c, c.getSource().getPlayerOrException()))
                .then(Commands.literal("status").executes(c -> status(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2)).executes(c -> status(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("clear").executes(c -> clear(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2)).executes(c -> clear(c, EntityArgument.getPlayer(c, "player"))))));
    }

    private static int open(CommandContext<CommandSourceStack> c, ServerPlayer p) { return AttuneService.open(p) ? 1 : 0; }

    private static int status(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        String text = String.join("\n", AttuneService.status(p));
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        AttuneService.clear(p);
        c.getSource().sendSuccess(() -> Component.literal("Attunement of " + p.getName().getString() + " cleared."), false);
        return 1;
    }
}
