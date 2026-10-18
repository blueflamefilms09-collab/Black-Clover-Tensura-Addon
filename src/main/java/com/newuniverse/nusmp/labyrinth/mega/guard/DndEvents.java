package com.newuniverse.nusmp.labyrinth.mega.guard;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Why: the two rules of the D&D layer that must act BEFORE the game does what it was about to do can only live on the game's event bus, because they cancel something the game decided: a
 * harmful effect that is about to be applied to the Guardian ({@code MobEffectEvent.Applicable} lets a handler answer DO_NOT_APPLY, which is the clean way to say "it shrugs this off", with
 * no effect ever appearing and nothing to remove afterwards), and a death that is about to happen ({@code LivingDeathEvent} is cancellable: the Mythic Trait cancels it, restores the health and
 * the boss lives on in its last form). The third listener loads the profiles of the bosses with the datapacks. Each handler leaves at once unless the entity is a Labyrinth Guardian, runs on the
 * server only, and is wrapped so that a mistake in the layer lets the game's own rule stand (an effect applies, a boss dies) and never reaches the server loop. The death listener runs first
 * (HIGHEST) so that nothing else reacts to a death that is not going to happen.
 */
final class DndEvents {
    private DndEvents() {}

    static void register(IEventBus gameBus) {
        gameBus.addListener(DndEvents::onReload);
        gameBus.addListener(EventPriority.HIGHEST, DndEvents::onDeath);
        gameBus.addListener(DndEvents::onEffect);
    }

    private static void onReload(AddReloadListenerEvent e) { e.addListener(new DndProfiles()); }

    private static void onDeath(LivingDeathEvent e) {
        try {
            // /kill and the void (the damage that bypasses invulnerability) always end a boss: the Mythic Trait answers only to a fight
            if (e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
            if (e.getEntity() instanceof LabyrinthGuardianEntity g && g.level() instanceof ServerLevel sl && g.dndMythic(sl)) e.setCanceled(true);
        } catch (RuntimeException ex) {
            GuardLog.failed("dnd mythic", ex);
        }
    }

    private static void onEffect(MobEffectEvent.Applicable e) {
        try {
            if (!(e.getEntity() instanceof LabyrinthGuardianEntity g) || !(g.level() instanceof ServerLevel sl)) return;
            var effect = e.getEffectInstance().getEffect().value();
            if (effect.getCategory() != MobEffectCategory.HARMFUL) return;
            String id = String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(effect));
            if (g.dndResist(sl, id)) e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        } catch (RuntimeException ex) {
            GuardLog.failed("dnd resistance", ex);
        }
    }
}
