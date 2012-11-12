package org.dyndns.fzoli.ui.systemtray;

/**
 * Rendszerikont biztosító osztály.
 * Ha az SWT elérhető, SWT rendszerikon, egyébként AWT rendszerikon jön létre.
 * @author zoli
 */
public final class SystemTrayProvider {

    /**
     * A létrehozott rendszerikon referenciája.
     */
    private static SystemTray st;
    
    private SystemTrayProvider() {
    }
    
    /**
     * A rendszerikon referenciáját adja vissza.
     * Ha még nincs létrehozva, létrejön a rendszerikon.
     */
    public static SystemTray getSystemTray() {
        if (st == null) {
            if (isSwtTrayAvailable()) st = new SwtSystemTray();
            else st = new AwtSystemTray();
        }
        return st;
    }
    
    /**
     * Megadja, hogy használható-e az SWT rendszerikon.
     */
    private static boolean isSwtTrayAvailable() {
        try {
            Class.forName("org.eclipse.swt.widgets.Tray", false, SystemTrayProvider.class.getClassLoader());
            return true;
        }
        catch (ClassNotFoundException ex) {
            return false;
        }
    }
    
}