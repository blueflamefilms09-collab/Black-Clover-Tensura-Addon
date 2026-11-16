package com.newuniverse.nusmp.mixin;

import com.newuniverse.nusmp.saiyan.SaiyanBlood;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Saiyan-blooded evolution (docs/saiyan_prodigy.md): Tensura's Human lists its next evolutions in {@code getNextEvolutions}; this adds the Saiyan to that list (when the server setting
 * {@code blood.bloodEvolution} is on), so the evolution screen offers it and Tensura's own evolution packet, which checks the list and the progress on the server, accepts it. Optional (require = 0, a pseudo
 * target): if Tensura changes the method the game still runs, the Saiyan is then only reachable by command and the race menu as before. The list is copied before it is changed, so a list Tensura shares is
 * never written to. Cost: one list copy per call of the evolution screen.
 */
@Pseudo
@Mixin(targets = "io.github.manasmods.tensura.race.human.HumanRace", remap = false)
public abstract class HumanRaceEvolutionMixin {
    @Inject(method = "getNextEvolutions", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void anime_multiverse$saiyanBlood(ManasRaceInstance instance, LivingEntity entity, CallbackInfoReturnable<List<ManasRace>> cir) {
        List<ManasRace> now = cir.getReturnValue();
        if (now == null) return;
        List<ManasRace> copy = new ArrayList<>(now);
        SaiyanBlood.addEvolution(copy);
        if (copy.size() != now.size()) cir.setReturnValue(copy);
    }
}
