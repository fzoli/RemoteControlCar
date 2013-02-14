package org.dyndns.fzoli.rccar.bridge;

import java.io.IOException;
import java.io.PrintStream;
import java.security.KeyStoreException;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.dyndns.fzoli.rccar.bridge.resource.R;
import org.dyndns.fzoli.rccar.bridge.socket.BridgeHandler;
import static org.dyndns.fzoli.rccar.controller.SplashScreenLoader.closeSplashScreen;
import static org.dyndns.fzoli.rccar.controller.SplashScreenLoader.setSplashMessage;
import org.dyndns.fzoli.rccar.ui.UIUtil;
import static org.dyndns.fzoli.rccar.ui.UIUtil.showPasswordInput;
import org.dyndns.fzoli.rccar.ui.UncaughtExceptionHandler;
import static org.dyndns.fzoli.rccar.ui.UncaughtExceptionHandler.showException;
import org.dyndns.fzoli.socket.SSLSocketUtil;
import static org.dyndns.fzoli.ui.UIUtil.setSystemLookAndFeel;
import org.dyndns.fzoli.ui.systemtray.SystemTrayIcon;
import org.dyndns.fzoli.ui.systemtray.TrayIcon.IconType;

/**
 * A híd indító osztálya.
 * @author zoli
 */
public class Main {
    
    /**
     * Általános rendszerváltozók.
     */
    private static final String LS = System.getProperty("line.separator");
    
    /**
     * A híd konfigurációja.
     */
    public static final Config CONFIG = getConfig();
    
    /**
     * A szótár.
     */
    private static final ResourceBundle STRINGS = createResource(CONFIG.getLanguage());
    
    /**
     * Üzenettípus.
     */
    private static final String VAL_MESSAGE = getString("bridge_message"), VAL_ERROR = getString("bridge_error");
    
    /**
     * Több helyen is használt szövegek.
     */
    public static final String VAL_WARNING = getString("warning"), VAL_CONN_LOG = getString("conn_log");
    
    /**
     * A szerver socket referenciája arra kell, hogy eseménykezelővel ki lehessen lépni.
     */
    private static SSLServerSocket SERVER_SOCKET;
    
    /**
     * Még mielőtt lefutna a main metódus, beállítódik a rendszer LAF, a saját kivételkezelő, a rendszerikon és az erőforrás-felszabadító szál.
     */
    static {
        setSplashMessage(getString("please_wait")); //TODO: erre nem lesz szükség
        setSystemLookAndFeel();
        setExceptionHandler();
        applyConfig();
        setSystemTrayIcon();
        addShutdownHook();
        closeSplashScreen(); //TODO: erre nem lesz szükség
    }
    
    /**
     * Beállítja a híd kivételkezelő metódusát.
     * Ha a rendszerikonok támogatva vannak, dialógusablak jeleníti meg a nem kezelt kivételeket,
     * egyébként nem változik az eredeti kivételkezelés.
     */
    private static void setExceptionHandler() {
        UncaughtExceptionHandler.apply(R.getBridgeImage());
    }
    
    /**
     * A konfiguráció alkalmazása.
     * Megnézi, hogy a csendes indulás be van-e állítva a konfig fájlban és ha igen,
     * a figyelmeztetéseket kikapcsolja, majd értelmezi a paramétereket, amik ezt felüldefiniálhatják.
     */
    private static void applyConfig() {
        if (CONFIG.isQuiet()) {
            BridgeHandler.setWarnEnabled(false);
            ConnectionAlert.setLogEnabled(false);
        }
    }
    
    /**
     * Beállítja a rendszerikont, ha a konfiguráció nem tiltja.
     * Hozzáadja a kapcsolatjelzés és kilépés menüopciót beállítja az ikont és megjeleníti azt.
     */
    private static void setSystemTrayIcon() {
        if (CONFIG.isHidden()) return;
        if (SystemTrayIcon.init() && SystemTrayIcon.isSupported()) {
            // az ikon beállítása
            SystemTrayIcon.setIcon(getString("app_name"), R.getBridgeImageStream());
            
            // kapcsolatjelzés beállító opció hozzáadása
            SystemTrayIcon.addCheckboxMenuItem(VAL_CONN_LOG, ConnectionAlert.isLogEnabled(), new Runnable() {

                @Override
                public void run() {
                    // naplózás beállítása az ellenkezőjére, mint volt
                    ConnectionAlert.setLogEnabled(!ConnectionAlert.isLogEnabled());
                }
                
            });

            // figyelmeztetés beállító opció hozzáadása
            SystemTrayIcon.addCheckboxMenuItem(VAL_WARNING, BridgeHandler.isWarnEnabled(), new Runnable() {

                @Override
                public void run() {
                    // warn beállítása az ellenkezőjére, mint volt
                    BridgeHandler.setWarnEnabled(!BridgeHandler.isWarnEnabled());
                }
                
            });
            
            // szeparátor hozzáadása a menühöz
            SystemTrayIcon.addMenuSeparator();

            // kilépés opció hozzáadása
            SystemTrayIcon.addMenuItem(getString("exit"), new Runnable() {

                /**
                 * Ha a kilépésre kattintottak.
                 */
                @Override
                public void run() {
                    // a program kilép
                    System.exit(0);
                }
                
            });
        }
    }
    
    /**
     * A program leállítása előtt nem árt az erőforrásokat felszabadítani.
     * Leállításkor ha sikerült a szerver socket létrehozása, bezárja azt,
     * végül naplózza a leállást.
     */
    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                if (SERVER_SOCKET != null) try {
                    SERVER_SOCKET.close();
                }
                catch (IOException ex) {
                    ;
                }
                logInfo(VAL_MESSAGE, getString("log_stop"), false);
            }
            
        }));
    }
    
    /**
     * Egy tályékoztató szöveget jelenít meg a felhasználónak.
     * Ha a grafikus felület elérhető, modális ablakban jelenik meg az üzenet,
     * különben a kimenet streamre megy ki a fejléc és a szöveg.
     * Ha a kimeneti stream System.err, akkor hibaüzenetes ablakikon,
     * egyébként figyelmeztetőikon kerül az ablakra.
     * @param title a fejléc
     * @param text a megjelenő szöveg
     * @param out a kimenet stream
     */
    private static void alert(String title, String text, PrintStream out) {
        UIUtil.alert(title, text, out, R.getBridgeImage(), true);
    }
    
    /**
     * Naplózza az átadott üzenetet és ha kell, meg is jeleníti azt a felhasználónak.
     * @param title az üzenet címsora
     * @param text a naplózandó üzenet
     * @param show true esetén az üzenet megjelenik a naplózás után
     */
    private static void logInfo(String title, String text, boolean show) {
        ConnectionAlert.logMessage(title, text, IconType.INFO, show);
    }
    
    /**
     * A szerver elindítása előtt a konzolon beadott paramétereket feldolgozza.
     * A paraméterek szükségtelenek, ha van grafikus felület a rendszeren.
     * Ha a -v paraméter meg lett adva, a program ki fogja jelezni a figyelmeztetéseket.
     * Ha a -vv paraméter meg lett adva, a program ki fogja jelezni a figyelmeztetéseket és a kapcsolódásokat is.
     * Ha a -m paraméter meg lett adva, a program nem jelez se figyelmeztetéseket, se kapcsolódásokat.
     */
    private static void readArguments(String[] args) {
        if (args.length == 1) {
            if (args[0].equals("-v")) {
                BridgeHandler.setWarnEnabled(true);
                ConnectionAlert.setLogEnabled(false);
            }
            else if (args[0].equals("-vv")) {
                BridgeHandler.setWarnEnabled(true);
                ConnectionAlert.setLogEnabled(true);
            }
            else if (args[0].equals("-m")) {
                BridgeHandler.setWarnEnabled(false);
                ConnectionAlert.setLogEnabled(false);
            }
        }
    }
    
    /**
     * SSL Server socket létrehozása a konfig fájl alapján.
     * Ha valamiért nem sikerül a tanúsítvány használata, jelszó bevitel jelenik meg.
     * A szerver socket sikeres létrehozása után naplózza és közli a felhasználóval, hogy fut a szerver.
     * @param count ha jelszóvédett a tanúsítvány, a hibás próbálkozások számát jelzi (rekurzívan) a naplózáshoz
     * @throws Error ha nem sikerül a szerver socket létrehozása
     */
    private static SSLServerSocket createServerSocket(int count) {
        try {
            SSLServerSocket socket = SSLSocketUtil.createServerSocket(CONFIG.getPort(), CONFIG.getCAFile(), CONFIG.getCertFile(), CONFIG.getKeyFile(), CONFIG.getPassword());
            logInfo(VAL_MESSAGE, getString("log_start"), !CONFIG.isQuiet());
            return socket;
        }
        catch (KeyStoreException ex) {
            if (ex.getMessage().startsWith("failed to extract")) {
                if (count > 0) ConnectionAlert.logMessage(VAL_WARNING, getString("msg_wrong_passwd1") + ' ' + count + ' ' + getString("msg_wrong_passwd2" + (count == 1 ? 'a' : 'b')), IconType.WARNING, false);
                CONFIG.setPassword(showPasswordInput(R.getBridgeImage(), false, true).getPassword());
                return createServerSocket(++count);
            }
            alert(VAL_ERROR, getString("msg_cert_error"), System.err);
            System.exit(1);
            return null;
        }
        catch(Exception ex) {
            alert(VAL_ERROR, getString("msg_port_error") + ": " + CONFIG.getPort() + LS + getString("msg_os") + ": " + ex.getMessage(), System.err);
            System.exit(1);
            return null;
        }
    }
    
    /**
     * A szerver socket elindítása, a program értelme.
     * Ha nem megbízható kapcsolat jön létre, jelzi a felhasználónak.
     * Ha nem várt kivétel képződik, kivételt dob, ami a felhasználó tudtára lesz adva.
     * @throws RuntimeException ha nem várt kivétel képződik
     */
    private static void runServer() {
        final SSLServerSocket ss = createServerSocket(0); // socket szerver létrehozása kezdetben nincs 1 hibás próbálkozás sem a jelszóbevitelnél
        while (!ss.isClosed()) { // ameddig nincs lezárva a socket szerver
            SSLSocket s = null;
            try {
                s = (SSLSocket) ss.accept(); // kliensre várakozik, és ha kapcsolódtak, ...
                new Thread(new BridgeHandler(s)).start(); // ... új szálban kezeli a kapcsolatot
            }
            catch (Exception ex) {
                // ha bármilyen kivétel keletkezik, nem áll le a szerver, csak közli a kivételt
                showException(ex);
            }
        }
    }
    
    /**
     * A szótárból kikeresi a megadott kulcshoz tartozó szót.
     */
    public static String getString(String key) {
        return STRINGS.getString(key);
    }
    
    /**
     * Létrehozza a konfigurációs objektumot.
     * Ha az inicializálása közben hiba történik, üzen a felhasználónak.
     */
    private static Config getConfig() {
        try {
            return Config.getInstance();
        }
        catch (Exception ex) {
            setSystemLookAndFeel();
            ResourceBundle res = createResource(Locale.getDefault());
            alert(VAL_ERROR, res.getString("msg_conf_error1") + LS + res.getString("msg_conf_error2"), System.err);
            System.exit(1);
            return null;
        }
    }
    
    /**
     * Létrehoz egy szótárat a kért nyelvhez és az UIManager-ben megadott, több helyen is használt szövegeket beállítja.
     */
    private static ResourceBundle createResource(Locale locale) {
        return UIUtil.createResource("bridge_lng", locale);
    }
    
    /**
     * A híd main metódusa.
     * Ha a konfiguráció még nem létezik, lérehozza és figyelmezteti a felhasználót, hogy állítsa be és kilép.
     * Ha a konfiguráció létezik, de rosszul paraméterezett, figyelmezteti a felhasználót és kilép.
     * Ha a program nem lépett ki, a híd szerver elkezdi futását.
     */
    public static void main(String[] args) {
        if (Config.FILE_CONFIG.exists() && !Config.FILE_CONFIG.canRead()) { // ha nincs olvasási jog a konfig fájlon
            alert(VAL_ERROR, getString("msg_need_permission") + LS + getString("msg_exit"), System.err);
            System.exit(1); // hibakóddal lép ki
        }
        if (CONFIG.isCorrect()) {
            readArguments(args);
            runServer();
        }
        else {
            final StringBuilder msg = new StringBuilder();
            if (CONFIG.isNew()) {
                msg.append(getString("msg_conf_created1")).append(LS)
                   .append(getString("msg_conf_created2")).append(LS).append(LS)
                   .append(getString("msg_conf_created3")).append(':').append(LS).append(Config.FILE_CONFIG);
                alert(VAL_MESSAGE, msg.toString(), System.out);
                System.exit(0);
            }
            else {
                msg.append(getString("msg_conf_incorrect1")).append(LS).append(LS);
                msg.append(getString("msg_conf_incorrect2")).append(':').append(LS);
                if (CONFIG.getPort() == null) msg.append("- ").append(getString("msg_conf_incorrect3")).append('.').append(LS);
                if (CONFIG.getCAFile() == null) msg.append("- ").append(getString("msg_conf_incorrect4")).append('.').append(LS);
                if (CONFIG.getCertFile() == null) msg.append("- ").append(getString("msg_conf_incorrect5")).append('.').append(LS);
                if (CONFIG.getKeyFile() == null) msg.append("- ").append(getString("msg_conf_incorrect6")).append('.').append(LS);
                alert(VAL_ERROR, msg.toString(), System.err);
                System.exit(1);
            }
        }
    }
    
}
