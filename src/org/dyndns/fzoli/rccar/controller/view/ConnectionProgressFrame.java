package org.dyndns.fzoli.rccar.controller.view;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import static org.dyndns.fzoli.rccar.controller.Main.getString;
import static org.dyndns.fzoli.rccar.controller.Main.runClient;
import static org.dyndns.fzoli.rccar.controller.Main.showSettingDialog;
import org.dyndns.fzoli.rccar.controller.resource.R;
import org.dyndns.fzoli.ui.AbstractConnectionProgressFrame;
import org.dyndns.fzoli.ui.IconTextPanel;

/**
 * A vezérlő kapcsolódásjelző- és kezelő ablaka.
 * @author zoli
 */
public class ConnectionProgressFrame extends AbstractConnectionProgressFrame implements RelocalizableWindow {

    /**
     * Az ablakon megjelenő panelek belőle származnak.
     */
    private static class ConnProgPanel extends IconTextPanel {

        public ConnProgPanel(Icon icon, String text) {
            super(icon, text);
            setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0)); // alsó és felső margó 5 pixel
        }
        
    }
    
    /**
     * A kapcsolatok állapotai.
     */
    public static enum Status {
        CONNECTING(R.getIndicatorIcon()),
        CONNECTION_ERROR,
        DISCONNECTED(R.getWarningIcon()),
        CONNECTION_REFUSED(R.getWarningIcon()),
        UNKNOWN_HOST,
        CONNECTION_TIMEOUT,
        HANDSHAKE_ERROR,
        KEYSTORE_ERROR,
        SERVER_IS_NOT_CLIENT;

        /**
         * Konstruktor.
         * Az alapértelmezett ikon a hibát jelző ikon.
         */
        private Status() {
            this(R.getErrorIcon());
        }

        /**
         * Konstruktor.
         * @param icon az állapothoz tartozó ikon
         */
        private Status(Icon icon) {
            ICON = icon;
        }

        /**
         * Az állapothoz tartozó ikon,
         * ami a kaocsolódáskezelő ablakon jelenik meg.
         */
        private final Icon ICON;
        
        /**
         * A szótár alapján adja meg a szöveget.
         */
        public String text() {
            return getString(name().toLowerCase());
        }
        
        /**
         * Legyártja a kapcsolódáskezelő ablakhoz a paneleket.
         */
        private static IconTextPanel[] createPanels() {
            Status[] values = Status.values();
            IconTextPanel[] panels = new IconTextPanel[values.length];
            for (int i = 0; i < panels.length; i++) {
                panels[i] = new ConnProgPanel(values[i].ICON, values[i].text());
            }
            return panels;
        }
        
    };
    
    /**
     * Az ablakon ezek a panelek jelenhetnek meg.
     */
    private static final IconTextPanel[] PANELS = Status.createPanels();
    
    /**
     * Beállítja a kis autó ikont és az indikátor animációt.
     */
    public ConnectionProgressFrame() {
        super(getString("connection_handler"), getString("reconnect"), getString("connection_settings"), getString("exit"), PANELS);
        setIconImage(R.getIconImage());
    }

    /**
     * A felület feliratait újra beállítja.
     * Ha a nyelvet megváltoztatja a felhasználó, ez a metódus hívódik meg.
     */
    @Override
    public void relocalize() {
        setTitle(getString("connection_handler"));
        setExitText(getString("exit"));
        setTryAgainText(getString("reconnect"));
        setConnectionSettingsText(getString("connection_settings"));
        Status[] sa = Status.values();
        for (int i = 0; i < PANELS.length; i++) {
            PANELS[i].setText(sa[i].text());
        }
    }
    
    /**
     * Beállítja a megjelenő panelt és az Újra gombot.
     * Az Újra gomb tiltva lesz {@code Status.CONNECTING} státusz esetén.
     * Ha nincs megadva státusz, az ablak elrejtődik.
     * @param status a kapcsolat egyik állapota
     */
    public void setStatus(Status status) {
        if (status != null) {
            setAgainButtonEnabled(status != Status.CONNECTING);
            setIconTextPanel(status.ordinal());
        }
        setVisible(status != null);
    }
    
    /**
     * Akkor hívódik meg, amikor az Újra gombot kiválasztják.
     */
    @Override
    protected void onAgain() {
        runClient(true, true);
    }

    /**
     * Akkor hívódik meg, amikor az Beállítások gombot kiválasztják.
     */
    @Override
    protected void onSettings() {
        showSettingDialog(false, null);
    }
    
}
