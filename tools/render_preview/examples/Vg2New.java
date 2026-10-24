package examples;

import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.LanceMode;

/** Valkyrie armour v2, the VALKYRIE look with the lance attached at the hip (the defaults). */
public final class Vg2New {
    private Vg2New() {}

    public static void register() { Vg2Common.install(true, Form.VALKYRIE, LanceMode.ATTACHED); }
}
