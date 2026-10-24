import com.newuniverse.nusmp.mana.method.BodyRuneRules;
import com.newuniverse.nusmp.mana.method.BrushRules;
import com.newuniverse.nusmp.mana.method.LightningCharge;
import com.newuniverse.nusmp.mana.method.RuneCharge;
import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import com.newuniverse.nusmp.mana.method.RuneCost;
import com.newuniverse.nusmp.mana.method.RuneElement;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil;
import com.newuniverse.nusmp.mana.method.RuneSpec;

import java.util.Locale;

/**
 * Preview helper (not part of the mod), 3.6.0: prints, as one JSON object, what the REAL pure classes decide for a tap, a half hold and a full hold, so the previews draw the game's own numbers
 * and not numbers typed into a script: the lightning strike of {@link LightningCharge}, the cost fraction of {@link RuneCost}, the size of a painted brush array ({@link BrushRules}) and the pictures
 * and duration of a body rune ({@link BodyRuneRules}).
 */
public final class DumpMm36 {
    public static void main(String[] args) {
        int[] holds = {4, 24, 40};
        StringBuilder sb = new StringBuilder("{\"stages\":[");
        double[] off = new double[2];
        RuneSpec fire = new RuneSpec(RuneElement.FIRE, RuneElement.FIRE.defaultMods(), false);
        double own = RuneGeneratorUtil.paramsFor(fire).radius();
        for (int i = 0; i < holds.length; i++) {
            Charge c = RuneCharge.of(holds[i], RuneCharge.DEFAULT_MIN_HOLD, RuneCharge.DEFAULT_MAX_HOLD);
            LightningCharge.Strike st = LightningCharge.strike(c);
            double radius = BrushRules.radius(c);
            BodyRuneRules.Mods m = BodyRuneRules.modsFor(c, BodyRuneRules.DEFAULT_SECONDS);
            if (i > 0) sb.append(',');
            sb.append(String.format(Locale.ROOT, "{\"held\":%d,\"stage\":\"%s\",\"level\":%.4f,\"costFraction\":%.4f,\"bolts\":%d,\"height\":%.3f,\"width\":%.3f,\"ticks\":%d,\"spread\":%.3f,\"offsets\":[",
                    holds[i], c.stage(), RuneCost.level(c), RuneCost.fraction(c, RuneCost.DEFAULT_MIN), st.bolts(), st.height(), st.width(), st.ticks(), st.spread()));
            for (int k = 0; k < st.bolts(); k++) {
                LightningCharge.boltOffset(st, k, off);
                if (k > 0) sb.append(',');
                sb.append(String.format(Locale.ROOT, "[%.4f,%.4f]", off[0], off[1]));
            }
            sb.append(String.format(Locale.ROOT, "],\"radiusMult\":%.4f,\"brushRadius\":%.3f,\"brushLife\":%d,\"brushScale\":%.4f,\"bodyTier\":%d,\"bodyPictures\":%d,\"bodyTicks\":%d}",
                    LightningCharge.radiusMult(new RuneSpec(RuneElement.LIGHTNING, RuneElement.LIGHTNING.defaultMods(), false), c), radius, BrushRules.lifeTicks(c, BrushRules.DEFAULT_SECONDS),
                    BrushRules.pictureScale(radius, own), m.tier(), BodyRuneRules.pictures(m.tier()), m.ticks()));
        }
        sb.append("],\"packs\":{");
        RuneElement[] els = RuneElement.values();
        for (int i = 0; i < els.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(els[i].name()).append("\":").append(new RuneSpec(els[i], els[i].defaultMods(), false).pack());
        }
        sb.append("},\"looks\":{");
        double[] nrm = new double[3], look = new double[3];
        String[] faces = {"down", "up", "north", "south", "west", "east"};
        for (int face = 0; face < 6; face++) {
            BrushRules.normal(face, nrm);
            BrushRules.lookFor(nrm[0], nrm[1], nrm[2], 0.0, look);
            if (face > 0) sb.append(',');
            sb.append(String.format(Locale.ROOT, "\"%s\":[%.5f,%.5f,%.5f]", faces[face], look[0], look[1], look[2]));
        }
        sb.append("},\"ownRadius\":").append(String.format(Locale.ROOT, "%.4f", own)).append('}');
        System.out.println(sb);
    }
}
