package org.dyndns.fzoli.rccar.controller;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.ParseException;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.dyndns.fzoli.rccar.controller.resource.R;
import org.dyndns.fzoli.ui.FilePanel;
import org.dyndns.fzoli.ui.RegexPatternFormatter;

/**
 * A vezérlő konfigurációját beállító dialógusablak.
 * @author zoli
 */
public class ConfigEditorDialog extends JDialog {
    
    /**
     * A dialógusablak lapfüleinek tartalma ebbe a panelbe kerül bele.
     * Mindegyik panel átlátszó.
     * @see GridBagLayout
     */
    private static class ConfigPanel extends JPanel {
        
        public ConfigPanel() {
            setOpaque(false);
        }
        
    }
    
    /**
     * Crt fájlszűrő.
     */
    private static final FileNameExtensionFilter fnefCrt = new FileNameExtensionFilter("Tanúsítvány (*.crt)", new String[] {"crt"});
    
    /**
     * Key fájlszűrő.
     */
    private static final FileNameExtensionFilter fnefKey = new FileNameExtensionFilter("Tanúsítvány kulcs (*.key)", new String[] {"key"});
    
    /**
     * IP címre és hosztnévre és egyéb egyedi címekre is egész jól használható reguláris kifejezés.
     */
    private static final Pattern ptAddress = Pattern.compile("^[a-z\\d]{1}[\\w\\.\\d]{0,18}[a-z\\d]{1}$", Pattern.CASE_INSENSITIVE);
    
    /**
     * Port validálására használt reguláris kifejezés.
     * Minimum 1 és maximum 5 karakter, csak szám.
     */
    private static final Pattern ptPort = Pattern.compile("^[\\d]{1,5}$", Pattern.CASE_INSENSITIVE);
    
    /**
     * A konfiguráció, amit használ az ablak.
     */
    private final Config CONFIG;
    
    /**
     * A szerver címe írható át benne.
     */
    private final JTextField tfAddress = new JFormattedTextField(createAddressFormatter());
    
    /**
     * A szerver portja írható át benne.
     */
    private final JTextField tfPort = new JFormattedTextField(createPortFormatter());
    
    /**
     * A kiállító fájl tallózó panele.
     */
    private final FilePanel fpCa = new FilePanel(this, "Kiállító") {
        {
            setFileFilter(fnefCrt);
        }
    };
    
    /**
     * A tanúsítvány fájl tallózó panele.
     */
    private final FilePanel fpCert = new FilePanel(this, "Tanúsítvány") {
        {
            setFileFilter(fnefCrt);
        }
    };
    
    /**
     * A tanúsítvány kulcs-fájl tallózó panele.
     */
    private final FilePanel fpKey = new FilePanel(this, "Kulcs") {
        {
            setFileFilter(fnefKey);
        }
    };
    
    /**
     * Az ablak bezárásakor lefutó eseménykezelő.
     * Meghívja az {@code onClosing} metódust.
     */
    private final WindowAdapter closeListener = new WindowAdapter() {

        @Override
        public void windowClosing(WindowEvent e) {
            onClosing();
        }
        
    };
    
    /**
     * Erre a gombra kattintva a konfiguráció elmentődik és bezárul az ablak.
     * De csak akkor, ha érvényesek a beállítások.
     */
    private final JButton btOk = new JButton("OK") {
        {
            addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (checkConfig()) saveConfig();
                }
                
            });
        }
    };
    
    /**
     * Erre a gombra kattintva bezárul az ablak, a konfiguráció nem változik.
     */
    private final JButton btCancel = new JButton("Mégse") {
        {
            addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    unsaveConfig();
                }
                
            });
        }
    };
    
    /**
     * Ezen a panelen állítható be a híd szerver elérési útvonala.
     */
    private final JPanel addressPanel = new ConfigPanel() {
        {
            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.weighty = 1; // teljes helylefoglalás hosszúságban
            c.insets = new Insets(5, 5, 5, 5); // 5 pixeles margó
            c.fill = GridBagConstraints.HORIZONTAL; // teljes helykitöltés horizontálisan (sorkitöltés)
            
            c.gridx = 1; // első oszlop
            c.weightx = 0; // csak annyit foglal, amennyit kell
            
            c.gridy = 0; // nulladik sor
            c.gridwidth = 2; // két oszlopot foglal el a magyarázat
            JLabel lbMsg = new JLabel("<html>Ezen a lapfülen állíthatja be a híd szervernek az elérési útvonalát.</html>");
            lbMsg.setPreferredSize(new Dimension(240, 30)); // két sorba kerül az üzenet, mivel nem fér el egy sorban ezen a méreten
            add(lbMsg, c);
            c.gridwidth = 1; // a többi elem egy oszlopot foglal el
            
            c.gridy = 1; // első sor (1, 1)
            add(new JLabel("Szerver cím:"), c);
            
            c.gridy = 2; // második sor (1, 2)
            add(new JLabel("Szerver port:"), c);
            
            c.gridx = 2; // második oszlop
            c.weightx = 1; // kitölti a maradék helyet
            
            c.gridy = 1; // első sor (2, 1)
            add(tfAddress, c);
            
            c.gridy = 2; // második sor (2, 2)
            add(tfPort, c);
        }
    };
    
    /**
     * Ezen a panelen állítható be a kapcsolathoz használt tanúsítvány.
     */
    private final JPanel certificatePanel = new ConfigPanel() {
        {
            setLayout(new GridLayout(3, 1));
            add(fpCa);
            add(fpCert);
            add(fpKey);
        }
    };
    
    /**
     * Az ablakot teljes egészében kitöltő lapfüles panel.
     */
    private final JTabbedPane tabbedPane = new JTabbedPane() {
        {
            addTab("Útvonal", addressPanel);
            addTab("Tanúsítvány", certificatePanel);
        }
    };
    
    /**
     * Konstruktor.
     * @param config konfiguráció, amit használ az ablak.
     */
    public ConfigEditorDialog(Config config) {
        CONFIG = config;
        initComponents();
        initDialog();
    }
    
    /**
     * Inicializálja az ablakot.
     */
    private void initDialog() {
        addWindowListener(closeListener);
        setTitle("Kapcsolatbeállító");
        setIconImage(R.getIconImage());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH; // mindkét irányban helykitöltés
        c.weightx = 1; // helyfoglalás szélességében ...
        c.weighty = 1; // ... és hosszúságában is
        add(tabbedPane, c);
        c.gridy = 1;
        c.weighty = 0; // minimális helyfoglalás ...
        c.fill = GridBagConstraints.NONE; // ... nincs átméretezés ...
        c.anchor = GridBagConstraints.LAST_LINE_END; // ... és jobb alsó sarokba kerül ...
        JPanel pButton = new JPanel(); // ... a gombokat tartalmazó panel
        pButton.add(btCancel);
        pButton.add(btOk);
        add(pButton, c);
        pack(); // legkisebb méretre állítás ...
        setMinimumSize(getSize()); // ... és ennél a méretnél csak nagyobb lehet az ablak
        setLocationRelativeTo(this); // képernyő közepére igazítás
    }
    
    /**
     * Inicializálja a komponenseket.
     */
    private void initComponents() {
        tabbedPane.setFocusable(false); // zavaró kijelölés jelzés leszedése
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 5 x 5 pixeles margó
        btOk.setPreferredSize(btCancel.getPreferredSize()); // a két gomb egy méretre állítása
    }
    
    /**
     * Betölti a konfigurációt a felület elemeibe.
     */
    private void loadConfig() {
        tfAddress.setText(CONFIG.getAddress());
        tfPort.setText(Integer.toString(CONFIG.getPort()));
        fpCa.setFile(CONFIG.getCAFile());
        fpCert.setFile(CONFIG.getCertFile());
        fpKey.setFile(CONFIG.getKeyFile());
    }
    
    /**
     * Elmenti a konfigurációt és bezárja az ablakot.
     */
    private void saveConfig() {
        CONFIG.setAddress(tfAddress.getText());
        CONFIG.setCAFile(fpCa.getFile());
        CONFIG.setCertFile(fpCert.getFile());
        CONFIG.setKeyFile(fpKey.getFile());
        CONFIG.setPort(Integer.parseInt(tfPort.getText()));
        Config.save(CONFIG);
        dispose();
    }
    
    /**
     * Bezárja az ablakot a konfiguráció mentése nélkül.
     * Ha nem megfelelő a konfiguráció és modális az ablak, a program leáll.
     */
    private void unsaveConfig() {
        if (isModal() && !isConfigValid(CONFIG.getAddress(), Integer.toString(CONFIG.getPort()), CONFIG.getCAFile(), CONFIG.getCertFile(), CONFIG.getKeyFile())) System.exit(0);
        dispose();
    }
    
    /**
     * Megadja, érvényes-e az aktuális beállítás.
     * Érvényes, ha mindhárom fájl be van állítva és mindkét bemenet megfelel a reguláris kifejezésüknek.
     */
    private boolean isConfigValid() {
        return isConfigValid(tfAddress.getText(), tfPort.getText(), fpCa.getFile(), fpCert.getFile(), fpKey.getFile());
    }
    
    /**
     * Megadja, érvényes-e a paraméterben megadott beállítás.
     * Érvényes, ha mindhárom fájl be van állítva és mindkét bemenet megfelel a reguláris kifejezésüknek.
     */
    private static boolean isConfigValid(String address, String port, File ca, File cert, File key) {
        return ptAddress.matcher(address).matches() &&
               ptPort.matcher(port).matches() &&
               ca != null &&
               cert != null &&
               key != null;
    }
    
    /**
     * A cím maszkolására hoz létre egy formázó objektumot.
     */
    private AbstractFormatter createAddressFormatter() {
        RegexPatternFormatter fmAddress = new RegexPatternFormatter(ptAddress) {

            @Override
            public Object stringToValue(String string) throws ParseException {
                // ha a szöveg pontra végződik vagy rövidebb két karakternél, az eredeti szöveg kerül a helyére a szerkesztés befejezésekor
                if (string.length() < 2 || string.endsWith(".")) return CONFIG.getAddress();
                return ((String)super.stringToValue(string)).toLowerCase(); // a szerkesztés befejezésekor minden karaktert kicsire cserél
            }
            
        };
        fmAddress.setAllowsInvalid(false); // nem engedi meg a nem megfelelő értékek beírását
        return fmAddress;
    }
    
    /**
     * A port maszkolására hoz létre egy formázó objektumot.
     */
    private AbstractFormatter createPortFormatter() {
        RegexPatternFormatter fmPort = new RegexPatternFormatter(ptPort) {

            @Override
            public Object stringToValue(String string) throws ParseException {
                try {
                    // ha a szöveg rövidebb 1 karakternél, az eredeti szöveg kerül a helyére a szerkesztés befejezésekor
                    if (string.length() < 1) return CONFIG.getPort();
                    // ha a szöveg nem alakítható egész számmá vagy az intervallumon kívül esik, kivételt keletkezik...
                    int number = Integer.parseInt(string); // ... itt
                    if (number < 1 || number > 65536) throw new Exception(); // ... vagy itt
                }
                catch (Exception ex) {
                    // ParseException kivétel dobása, hogy nem megfelelő az érték
                    throw new ParseException("invalid port", 0);
                }
                // ha eddig nem dobódott kivétel, még a regex kifejezés dobhat kivételt és ha dob, nem frissül a szöveg
                return super.stringToValue(string);
            }
            
        };
        fmPort.setAllowsInvalid(false); // nem engedi meg a nem megfelelő értékek beírását
        return fmPort;
    }
    
    /**
     * Megmondja, modális-e az ablak.
     */
    @Override
    public boolean isModal() {
        return !getModalityType().equals(ModalityType.MODELESS);
    }
    
    /**
     * Beállítja az ablak modalitását.
     */
    @Override
    public void setModal(boolean modal) {
        setModalityType(modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
    }

    /**
     * Megjeleníti vagy elrejti az ablakot.
     * Ha megjelenést kértek, előtérbe kerül az ablak.
     * A konfiguráció frissül az ablak megjelenésekor.
     */
    @Override
    public void setVisible(boolean b) {
        if (b && !isVisible()) loadConfig();
        super.setVisible(b);
        if (b) {
            toFront();
            repaint();
        }
    }
    
    /**
     * Ha a beállítások nem érvényesek, figyelmezteti a felhasználót.
     * @return true, ha érvényesek a beállítások, egyébként false
     */
    private boolean checkConfig() {
        if (!isConfigValid()) {
            JOptionPane.showMessageDialog(this, "A beállítások nem megfelelőek!", "Figyelmeztetés", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }
    
    /**
     * Az ablak bezárásakor ha módosult a konfiguráció és nincs mentve,
     * megkérdi, akarja-e menteni, egyébként biztos, hogy nincs mentés.
     * Ha a konfiguráció nem érvényes, figyelmezteti a felhasználót és nem csinál semmit.
     */
    private void onClosing() {
        if (!checkConfig()) return; // ha a beállítás nem érvényes figyelmeztetés és semmittevés
        getContentPane().requestFocus(); // fókusz átadása az ablaknak, hogy biztosan minden szerkesztés végetérjen
        if (CONFIG.equals(tfAddress.getText(), Integer.parseInt(tfPort.getText()), fpCa.getFile(), fpCert.getFile(), fpKey.getFile())) {
            unsaveConfig(); // a beállítások nem változtak, nincs mentés
        }
        else {
            // a beállítások megváltoztak, legyen mentés?
            String[] opts = new String[] {"Igen", "Nem", "Mégse"}; // az alapértelmezett opció a Mégse
            int sel = JOptionPane.showOptionDialog(this, "Menti a módosításokat?", getTitle(), JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[2]);
            switch (sel) {
                case 0: // Igen, legyen mentés
                    saveConfig();
                    break;
                case 1: // Nem, ne legyen mentés
                    unsaveConfig();
                    break;
                case 2: // Mégse, semmittevés
                    ;
            }
        }
    }
    
}