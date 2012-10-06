package org.dyndns.fzoli.rccar.controller;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.text.DefaultFormatter;
import org.dyndns.fzoli.rccar.controller.resource.R;

/**
 * A vezérlő konfigurációját beállító dialógusablak.
 * TODO
 * @author zoli
 */
public class ConfigEditorDialog extends JDialog {

    /**
     * Reguláris kifejezésre illeszkedő szövegformázó.
     */
    private static class RegexPatternFormatter extends DefaultFormatter {

        protected java.util.regex.Matcher matcher;

        public RegexPatternFormatter(Pattern regex) {
          setOverwriteMode(false);
          matcher = regex.matcher(""); // Matcher inicializálása a regexhez
        }

        @Override
        public Object stringToValue(String string) throws ParseException {
            if (string == null)
                return null;
            matcher.reset(string); // Matcher szövegének beállítása

            if (!matcher.matches()) // Ha nem illeszkedik a szöveg, kivételt dob
                throw new ParseException("does not match regex", 0);

            // Ha a szöveg illeszkedett, akkor vissza lehet térni vele
            return super.stringToValue(string);
        }

    }
    
    /**
     * A dialógusablak lapfüleinek tartalma ebbe a panelbe kerül bele.
     * Mindegyik panel {@code GridBagLayout} elrendezésmenedzsert használ és
     * mindegyik panel átlátszó.
     * @see GridBagLayout
     */
    private static class ConfigPanel extends JPanel {
        
        public ConfigPanel() {
            super(new GridBagLayout());
            setOpaque(false);
        }
        
    }
    
    /**
     * A konfiguráció, amit használ az ablak.
     */
    private final Config CONFIG;
    
    private final JTextField tfAddress = new JTextField();
    
    private final JFormattedTextField tfPort = new JFormattedTextField(createPortFormatter());
    
    /**
     * Az ablak bezárásakor lefutó eseménykezelő.
     * Meghívja az {@code onClosing} metódust.
     */
    private WindowAdapter closeListener = new WindowAdapter() {

        @Override
        public void windowClosing(WindowEvent e) {
            onClosing();
        }
        
    };
    
    /**
     * Ezen a panelen állítható be a híd szerver elérési útvonala.
     */
    private final JPanel addressPanel = new ConfigPanel() {
        {
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(5, 5, 5, 5); // 5 pixeles margó
            c.fill = GridBagConstraints.BOTH; // teljes helykitöltés
            
            c.gridx = 1; // első oszlop
            c.weightx = 0; // csak annyit foglal, amennyit kell
            
            c.gridy = 0; // nulladik sor
            c.gridwidth = 2; // két oszlopot foglal el a magyarázat
            add(new JLabel("<html>Ezen a lapfülen állíthatja be a híd szerver elérési útvonalát.</html>"), c);
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
            add(new JLabel("teszt2"));
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
        loadConfig();
        initDialog();
    }
    
    private void initDialog() {
        addWindowListener(closeListener);
        setTitle("Kapcsolatbeállító");
        setIconImage(R.getIconImage());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        add(tabbedPane);
        setSize(300, 210);
        setLocationRelativeTo(this);
    }
    
    private AbstractFormatter createPortFormatter() {
        Pattern ptPort = Pattern.compile("[\\d]{0,5}", Pattern.CASE_INSENSITIVE);
        RegexPatternFormatter fmPort = new RegexPatternFormatter(ptPort) {

            @Override
            public Object stringToValue(String string) throws ParseException {
                try {
                    if (string.isEmpty()) return "";
                    int number = Integer.parseInt(string);
                    if (number < 1 || number > 65536) throw new Exception();
                }
                catch (Exception ex) {
                    throw new ParseException("invalid port", 0);
                }
                return super.stringToValue(string);
            }
            
        };
        fmPort.setAllowsInvalid(false);
        return fmPort;
    }
    
    private void loadConfig() {
        tfAddress.setText(CONFIG.getAddress());
        tfPort.setText(Integer.toString(CONFIG.getPort()));
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
     */
    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            toFront();
            repaint();
        }
    }
    
    /**
     * Az ablak bezárásakor ha módosult a konfiguráció és nincs mentve,
     * megkérdi, akarja-e menteni.
     * TODO
     */
    private void onClosing() {
        System.exit(0);
    }
    
}