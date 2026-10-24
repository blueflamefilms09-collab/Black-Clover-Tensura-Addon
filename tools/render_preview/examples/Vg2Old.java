package examples;

import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.LanceMode;

/** The old overlay of ModeArmorLayer (the setting CLASSIC): exactly what the code before this change draws. */
public final class Vg2Old {
    private Vg2Old() {}

    public static void register() { Vg2Common.install(false, Form.VALKYRIE, LanceMode.ATTACHED); }
}
