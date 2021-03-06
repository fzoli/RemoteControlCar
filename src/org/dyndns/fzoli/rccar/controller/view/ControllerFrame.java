package org.dyndns.fzoli.rccar.controller.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.dyndns.fzoli.rccar.controller.ControllerModels.ClientControllerData;
import static org.dyndns.fzoli.rccar.controller.ControllerModels.getData;
import org.dyndns.fzoli.rccar.controller.ControllerWindows;
import static org.dyndns.fzoli.rccar.controller.ControllerWindows.IC_ARROWS;
import static org.dyndns.fzoli.rccar.controller.ControllerWindows.IC_CHAT;
import static org.dyndns.fzoli.rccar.controller.ControllerWindows.IC_MAP;
import org.dyndns.fzoli.rccar.controller.ControllerWindows.WindowType;
import static org.dyndns.fzoli.rccar.controller.Main.getString;
import org.dyndns.fzoli.rccar.controller.resource.R;
import org.dyndns.fzoli.rccar.model.Command;
import org.dyndns.fzoli.rccar.model.controller.HostState;
import org.dyndns.fzoli.ui.LookAndFeelIcon;
import org.dyndns.fzoli.ui.OptionPane;
import org.dyndns.fzoli.ui.RoundedPanel;
import org.dyndns.fzoli.util.OSUtils;
import org.imgscalr.Scalr;

/**
 * A jármű főablaka.
 * Tartalmazza a kameraképet, a vezérlőgombokat,
 * valamint a jármű pillanatnyi sebességét és akkumulátorszintjét.
 */
public class ControllerFrame extends JFrame implements RelocalizableWindow {
    
    /**
     * A képkockát megjelenítő címke.
     */
    private JLabel lbImage;

    /**
     * Vezérlőgomb.
     */
    private JButton btControl;

    /**
     * Ablakmegjelenítő- és elrejtő gombok.
     */
    private JToggleButton btChat, btMap, btArrow;

    /**
     * Dudálás gomb.
     */
    private JButton btHorn;
    
    /**
     * Sebességnövekedés aktiváló/deaktiváló gomb.
     */
    private JToggleButton btIncrease;

    /**
     * Pillanatnyi sebességet mutató címke.
     */
    private JLabel lbSpeed;
    
    /**
     * Akkumulátor-szintet mutató folyamatjelző.
     */
    private JProgressBar pbAccu;
    
    /**
     * Toolbar.
     */
    private JToolBar tb;
    
    /**
     * Oszlopszámláló az elrendezés-menedzser megszorításához.
     */
    private int colCounter = 0;
    
    /**
     * Vezérlőgomb ikonja, amikor átadható a vezérlés.
     */
    private static final ImageIcon IC_CONTROLLER1 = R.getImageIcon("controller1.png");

    /**
     * Vezérlőgomb ikonja, amikor kérhető a vezérlés.
     */
    private static final ImageIcon IC_CONTROLLER2 = R.getImageIcon("controller2.png");

    /**
     * Vezérlőgomb ikonja, amikor a vezérlés kérés visszavonható.
     */
    private static final ImageIcon IC_CONTROLLER3 = R.getImageIcon("controller3.png");
    
    /**
     * Növekedést jelző ikon.
     */
    private static final ImageIcon IC_INCREASE = R.getImageIcon("increase.png");

    /**
     * Dudálás gomb ikonja.
     */
    private static final ImageIcon IC_HORN = R.getImageIcon("horn.png");
    
    /**
     * Teljesen fekete képkocka.
     */
    private static final ImageIcon IC_BLACK_BG = new ImageIcon(new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB) {
        {
            Graphics g = getGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    });
    
    /**
     * Az üzenetet megjelenítő komponens.
     */
    private final JLabel LB_MSG = new JLabel("", SwingConstants.CENTER) {

        /**
         * A szöveg módosulása esetén az üzenetmegjelenítő panelt középre igazítja.
         */
        @Override
        public void setText(String text) {
            super.setText(text);
            if (PANEL_MSG != null && lbImage != null) {
                Dimension size = lbImage.getSize();
                Dimension size2 = PANEL_MSG.getPreferredSize();
                PANEL_MSG.setBounds(size.width / 2 - size2.width / 2, size.height / 2 - size2.height / 2, size2.width, size2.height);
                PANEL_MSG.revalidate();
                PANEL_MSG.repaint();
            }
        }
        
    };
    
    /**
     * Az indikátort megjelenító címke az üzenetpanelen.
     */
    private final JLabel LB_INDICATOR = new JLabel(R.getIndicatorIcon());
    
    /**
     * Sebesség formázó.
     */
    private static final DecimalFormat DF_SPEED = new DecimalFormat("0.#");
    
    /**
     * A figyelmeztető-ikont megjelenító címke az üzenetpanelen.
     */
    private final JLabel LB_WARNING = new JLabel(LookAndFeelIcon.createIcon(this, "OptionPane.warningIcon", null));
    
    /**
     * Az üzenetet megjelenítő komponens panelje.
     * Az üzenet az indikátor alá kerül és a panelnek kerekített sarkai vannak.
     * A panel kezdetben nem látható.
     */
    private final RoundedPanel PANEL_MSG = new RoundedPanel() {
        
        {
            GridBagConstraints c = new GridBagConstraints();
            setLayout(new GridBagLayout());
            c.gridy = 0;
            c.insets = new Insets(10, 15, 10, 15);
            add(LB_INDICATOR, c);
            add(LB_WARNING, c);
            LB_WARNING.setVisible(false);
            
            c.gridy = 1;
            c.insets = new Insets(0, 15, 10, 15);
            add(LB_MSG, c);
            
            setVisible(false);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            // minimum 150 pixel széles; szélességében 10, hosszúságában 20 pixel többlet
            return new Dimension(Math.max(150, d.width + 10), d.height + 20);
        }
        
    };
    
    /**
     * A járműhöz tartozó ablakok konténere.
     */
    public final ControllerWindows WINDOWS;
    
    /**
     * Konstruktor.
     */
    public ControllerFrame(ControllerWindows windows) {
        WINDOWS = windows;
        getData().setControllerFrame(this);
        initFrame();
        setComponents();
        pack();
    }

    /**
     * Az ablak komponenseinek létrehozása és a felület létrehozása.
     */
    private void initFrame() {
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setIconImage(R.getIconImage());
        setLayout(new BorderLayout());
        setTitle(getString("app_name"));
        
        lbImage = new JLabel(IC_BLACK_BG) { // amíg nincs MJPEG stream, fekete
            
            /**
             * Átlátszó, fekete keretszín az MJPEG képkockáknak.
             */
            private Color frc = new Color(0, 0, 0, 0.4f);
            
            {
                setBackground(Color.BLACK);
                setOpaque(false); // ha a kép méret kisebb lenne, mint a várt, fekete kitöltés
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // fehér keret rajzolása bal és jobb oldalra, valamint felülre,
                // hogy a toolbar szegélye ne nézzen ki furán
                g.setColor(ControllerFrame.this.getBackground().brighter());
                g.drawRect(0, 0, getWidth() - 1, getHeight());
                // keret rajzolása az MJPEG képkockának
                g.setColor(frc);
                g.drawRect(1, 1, getWidth() - 3, getHeight() - 2);
            }
            
        };
        
        // a mozgóképet és a figyelmeztető panelt tartalmazó panel
        JLayeredPane pCenter = new JLayeredPane() {
            {
                Dimension size = lbImage.getPreferredSize(); // képméret
                Dimension size2 = PANEL_MSG.getPreferredSize(); // üzenetméret
                
                // a panel mérete a kép komponensével egyezik meg
                setPreferredSize(size);
                
                // a kép az alap rétegre, az üzenet a kép fölé kerül
                add(lbImage, JLayeredPane.DEFAULT_LAYER);
                add(PANEL_MSG, JLayeredPane.DRAG_LAYER);
                
                lbImage.setBounds(new Rectangle(0, 0, size.width, size.height)); // a kép teljes helykitöltéssel
                PANEL_MSG.setBounds(size.width / 2 - size2.width / 2, size.height / 2 - size2.height / 2, size2.width, size2.height); // az üzenet panel középen
            }
        };
        
        add(pCenter, BorderLayout.CENTER); // a mozgókép és üzenet panel hozzáadása az ablakhoz

        tb = new JToolBar() {
            
            {
                setLayout(new GridBagLayout());
                setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)); // a paintComponentben húzott vonalak helyét lefoglalja
                setBorderPainted(false); // de nem rajzol oda semmit
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // egyéni szegély rajzolása:
                g.setColor(ControllerFrame.this.getBackground().brighter()); // világosabb színnel
                g.drawRect(0, 0, getWidth() - 1 , getHeight() - 1); // külső
                g.setColor(ControllerFrame.this.getBackground().darker()); // sötétebb színnel
                g.drawRect(1, 1, getWidth() - 3 , getHeight() - 3); // középső
            }
            
        };
        
        add(tb, BorderLayout.SOUTH); // az ablak aljára kerül a toolbar

        btControl = createButton(null, IC_CONTROLLER1, JButton.class); // vezérlés kérő gomb
        addSeparator(); // szeparátor
        btMap = createButton(getString("map"), IC_MAP, JToggleButton.class); // radar ablak láthatóság szabályzó gomb
        btArrow = createButton(getString("controller"), IC_ARROWS, JToggleButton.class); // vezérlő ablak láthatóság szabályzó gomb
        btChat = createButton(getString("chat"), IC_CHAT, JToggleButton.class); // chat ablak láthatóság szabályzó gomb
        addSeparator(); // szeparátor
        btIncrease = createButton(getString("increasing_speed"), IC_INCREASE, JToggleButton.class); // növekvő sebesség aktiváló gomb
        btHorn = createButton(getString("horn"), IC_HORN, JButton.class); // dudálás gomb

        JPanel pStat = new JPanel(); // a statisztika panel ...
        pStat.setOpaque(false); // ... átlátszó és ...
        pStat.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 0)); // ... jobbra igazítva kerülnek rá a komponensek 8 pixel hézaggal
        GridBagConstraints c = getGbc();
        c.fill = GridBagConstraints.HORIZONTAL; // a panel magasságban minimális, hogy a toolbar közepén legyen
        c.weightx = Integer.MAX_VALUE; // a panel a maradék hely teljes kitöltésével ...
        tb.add(pStat, c); // ... hozzáadódik a toolbarhoz, mint utolsó komponens
        
        lbSpeed = new JLabel(" ");
        pStat.add(lbSpeed); // sebesség kijelző inicializálása, hozzáadás az ablakhoz
        
        pbAccu = new JProgressBar(); // akkumulátor-szint kijelző inicializálása
        pbAccu.setString(" ");
        pbAccu.setValue(100);
        pbAccu.setStringPainted(true); // a beállított szöveg jelenjen meg
        pStat.add(pbAccu); // hozzáadás az ablakhoz
        
        addWindowListener(new WindowAdapter() {

            /**
             * A főablak bezárásakor nem lesz kiválasztva jármű.
             * Amikor a szerver megkapja, hogy nincs jármű kiválasztva,
             * elküldi a teljes jármű listát és a kliens megjeleníti a
             * járműválasztó ablakot újra.
             * Ha a jármű offline, megkérdi, hogy biztos-e a kilépésben a felhasználó,
             * mivel a kilépés visszavonhatatlan művelet (legalábbis addig, míg offline a jármű).
             * Mac-en nem teszi fel a kérdést, egyből kilép, mivel a dialógusablakok
             * nem mindig kerülnek előtérbe és zavaróak.
             */
            @Override
            public void windowClosing(WindowEvent e) {
                if (getData().isConnected() != null && !getData().isConnected() && !OSUtils.isOS(OSUtils.OS.MAC)) {
                    int answer = OptionPane.showYesNoDialog(ControllerFrame.this, getString("leave_vehicle_msg1") + "\n" + getString("leave_vehicle_msg2") + "\n\n" + getString("leave_vehicle_msg3"), getString("leave_vehicle"));
                    if (answer == 1) return;
                }
                setVisible(false);
                if (WINDOWS != null) WINDOWS.onMapHiding();
                getData().getSender().setHostName(null);
            }

        });
        
        btControl.addActionListener(new ActionListener() {

            /**
             * A vezérlés kérés/átadás gombra kattintva a kliens üzen a Hídnak, hogy kéri/átadja a vezérlést.
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                setControlling();
            }
            
        });
        
        btHorn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getData().sendCommand(Command.PLAY_HORN);
            }
            
        });
        
        addKeyListener(new KeyAdapter() {

            /**
             * Shift+Enter / Shift+Backspace leütésére a kliens üzen a Hídnak, hogy kéri/átadja a vezérlést.
             */
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.isShiftDown()) {
                    if ((getData().isControlling() == null || !getData().isControlling()) && (getData().isWantControl() == null || !getData().isWantControl())) {
                        if (KeyEvent.VK_ENTER == e.getKeyCode()) setControlling();
                    }
                    else {
                        if (KeyEvent.VK_BACK_SPACE == e.getKeyCode()) setControlling();
                    }
                }
            }
            
        });
    }
    
    /**
     * A kliens üzen a Hídnak, hogy kéri/átadja a vezérlést.
     */
    private void setControlling() {
        if (!btControl.isEnabled()) return;
        if (getData().isControlling() == null || getData().isWantControl() == null) return;
        getData().getSender().setWantControl(getData().isWantControl() && !getData().isControlling() ? false : !getData().isControlling());
    }
    
    /**
     * A felület feliratait újra beállítja.
     * Ha a nyelvet megváltoztatja a felhasználó, ez a metódus hívódik meg.
     */
    @Override
    public void relocalize() {
        setTitle(getString("app_name"));
        btMap.setToolTipText(getString("map"));
        btChat.setToolTipText(getString("chat"));
        btArrow.setToolTipText(getString("controller"));
        btIncrease.setToolTipText(getString("increasing_speed"));
        btHorn.setToolTipText(getString("horn"));
        refreshSpeed();
        refreshBattery();
        refreshControlText();
        refreshMessage();
    }
    
    /**
     * A komponensek alapértelmezéseinek beállítása.
     * - A toolbar nem helyezhető át és a gomboknak nem fest szegélyt, míg nem kerül egér föléjük.
     * - A három másik dialógusablakhoz tartozik három gomb és rájuk kattintva a hozzájuk tartozó ablak jelenk meg vagy tűnik el.
     */
    private void setComponents() {
        tb.setFloatable(false); // a toolbar nem mozgatható
        tb.setRollover(true); // a gomboknak nem fest szegélyt a toolbar
        
        ChangeListener clDialogs = new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (WINDOWS != null) {
                    if (!isVisible()) return; // FONTOS: ha az ablak nem látható, esemény eldobása
                    JToggleButton src = (JToggleButton) e.getSource(); // a gomb, melyre kattintottak
                    WindowType window = null; // a gombhoz tartozó ablak típusa
                    if (src == btArrow) window = WindowType.CONTROLL;
                    else if (src == btChat) window = WindowType.CHAT;
                    else if (src == btMap) window = WindowType.MAP;
                    WINDOWS.setVisible(window, src.isSelected()); // a dialógus ablak láthatóságának módosítása
                }
            }
                           
        };
        
        // mindhárom gombra érvényes a fenti eseményfigyelő
        btArrow.addChangeListener(clDialogs);
        btChat.addChangeListener(clDialogs);
        btMap.addChangeListener(clDialogs);
        
    }

    /**
     * Az ablak elrejtésekor az MJPEG képkocka törlődik, hogy biztosan csak aktuális képkocka jelenjen meg.
     * A jármű kiválasztásakor a szerver újraküldi az utolsó képkockát, hogy az ablak megjelenésével egy időben
     * megjelenjen a kép is, de ezt azért is, mert nem feltétlen streamel a jármű eközben és akkor fekete maradna a kép.
     */
    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (!b) setFrameImage(null);
        pack(); // Windows rendszeren néha nem jó a kezdőméret, ideignlenes megoldásnak jó, de erőforrás pazarló
    }
    
    /**
     * A toolbar elrendezés-menedzserének a megszorítása.
     */
    private GridBagConstraints getGbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = colCounter;
        colCounter++;
        return c;
    }
    
    /**
     * Panelhez gyárt gombot.
     * A panelen lévő gombok nem fókuszálhatóak.
     * @param tb a panel, amihez hozzáadódik a gomb
     * @param text a gomb tooltip szövege
     * @param img a gomb ikonja
     * @param clazz a gomb típusa
     */
    private <T extends AbstractButton> T createButton(String text, ImageIcon img, Class<T> clazz) {
        try {
            GridBagConstraints c = getGbc();
            T bt = clazz.newInstance();
            tb.add(bt, c);
            bt.setIcon(img);
            bt.setToolTipText(text);
            bt.setFocusable(false);
            bt.setMargin(c.insets);
            return bt;
        }
        catch (Exception ex) {
            return null;
        }
    }

    /**
     * Szeparátor hozzáadása a toolbarhoz.
     */
    private void addSeparator() {
        tb.add(new JSeparator(SwingConstants.VERTICAL), getGbc());
    }
    
    /**
     * A vezérlés átadása/kérése gombot frissíti az adatmodel alapján.
     * Az alábbi táblázat alapján dönti el, hogy aktív-e a gomb,
     * és hogy mi legyen a típusa:
     *    vezérli? akarja?  esemény
     *    i        i        lemondás aktív
     *    h        i        visszavonás aktív
     *    i        h        lemondás inaktív
     *    h        h        kérés aktív
     * Ha a jármű csak figyelhető, akkor a gomb biztosan inaktív.
     * @param prevWantControl az előző érték
     * Használt getterek:
     * {@link ClientControllerData#isControlling()}
     * {@link ClientControllerData#isViewOnly()}
     * {@link ClientControllerData#isWantControl()}
     */
    public void refreshControlButton(Boolean prevWantControl) {
        if (getData().isControlling() == null || getData().isViewOnly() == null || getData().isWantControl() == null) return;
        btControl.setIcon(getData().isControlling() ? IC_CONTROLLER1 : getData().isWantControl() ? IC_CONTROLLER3 : IC_CONTROLLER2);
        refreshControlText();
        if (prevWantControl != null && ((!prevWantControl && getData().isWantControl()) || (prevWantControl && !getData().isWantControl() && getData().isControlling())) && !getData().isControlling()) btControl.setEnabled(false);
        else btControl.setEnabled((!getData().isViewOnly() && !(getData().isControlling() && !getData().isWantControl())));
        refreshHornButton();
    }
    
    /**
     * Beállítja a model alapján a dudálás gomb használhatóságát.
     * Ha a jármű elérhető és a kliens vezérli a járművet, akkor használható, egyébként nem.
     */
    public void refreshHornButton() {
        btHorn.setEnabled(getData().isControlling() == null || (getData().isControlling() && getData().isVehicleAvailable(false, true)));
    }
    
    /**
     * A vezérlő gomb feliratát frissíti.
     */
    private void refreshControlText() {
        if (getData().isControlling() == null || getData().isWantControl() == null) return;
        btControl.setToolTipText(getString(getData().isControlling() ? "give_up_control" : getData().isWantControl() ? "undo_control" : "ask_control"));
    }
    
    /**
     * A sebesség feliratot frissíti az adatmodel alapján.
     * Használt getterek:
     * {@link ClientControllerData#isUp2Date()}
     * {@link ClientControllerData#getHostState()}
     * {@link ClientControllerData#isVehicleAvailable()}
     */
    public void refreshSpeed() {
        String text = " "; // üres szöveg helyett egy szóköz, mert az sem látszik, de az elrendezésmenedzsernek számít, hogy üres-e a szöveg
        if (!getData().isUnderTimeout() && getData().isHostUnderTimeout() != null && !getData().isHostUnderTimeout()) {
            HostState hs = getData().getHostState();
            if (hs != null && hs.SPEED != null && getData().isUp2Date() != null && getData().isUp2Date()) text = getString("speed") + ": " + DF_SPEED.format(hs.SPEED) + " km/h";
        }
        lbSpeed.setText(text);
    }
    
    /**
     * Az akkumulátor-töltöttség jelzőt frissíti az adatmodel alapján.
     * Használt getterek:
     * {@link ClientControllerData#getControl()}
     * {@link ClientControllerData#getBatteryLevel()}
     * {@link ClientControllerData#isVehicleAvailable()}
     */
    public void refreshBattery() {
        // setIndeterminate null pointer exceptiont okoz nagyritkán, ha nem az EDT-ben fut
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                boolean zero = getData().getControl() == null || getData().getControl().getX() == 0 && getData().getControl().getY() == 0;
                boolean show = getData().isVehicleAvailable(true, false) && getData().getBatteryLevel() != null && zero;
                pbAccu.setString(show ? (getString("battery") + ": " + getData().getBatteryLevel() + " %") : "");
                if (show) pbAccu.setValue(getData().getBatteryLevel());
                pbAccu.setIndeterminate(!show);
            }
            
        });
    }
    
    /**
     * Az üzenet panel szövegét frissíti az adatmodel alapján.
     * Használt getterek:
     * {@link ClientControllerData#isUnderTimeout()}
     * {@link ClientControllerData#isHostUnderTimeout()}
     * {@link ClientControllerData#isVehicleConnected()}
     * {@link ClientControllerData#isConnected()}
     */
    public void refreshMessage() {
        Boolean htime = getData().isHostUnderTimeout();
        Boolean vconn = getData().isVehicleConnected();
        Boolean tconn = getData().isConnected();
        if (getData().isUnderTimeout()) setProgressMessage(getString("waiting_for_bridge_connection"), true);
        else if (tconn != null && !tconn) setProgressMessage(getString("offline_vehicle"), false);
             else if (htime != null && htime) setProgressMessage(getString("waiting_for_vehicle_connection"), true);
                  else if (vconn != null && !vconn) setProgressMessage(getString("waiting_for_cable_connection"), true); 
                       else setProgressMessage(null, true);
    }
    
    /**
     * Beállítja az aktuális MJPEG képkockát.
     * @param frame a képkocka, ami ha null, fekete kitöltésű kép jelenik meg
     */
    public void setFrameImage(BufferedImage frame) {
        if (frame == null) lbImage.setIcon(IC_BLACK_BG);
        else lbImage.setIcon(new ImageIcon(Scalr.resize(frame, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_WIDTH, 640, Scalr.OP_ANTIALIAS)));
    }
    
    /**
     * Beállítja az üzenetet és megjeleníti azt.
     * @param msg az üzenet. Ha null, akkor az üzenet panel eltűnik.
     * @param indicator true esetén indikátor, false esetén figyelmeztető ikon jelenik meg
     */
    private void setProgressMessage(String msg, boolean indicator) {
        LB_INDICATOR.setVisible(indicator);
        LB_WARNING.setVisible(!indicator);
        if (msg != null) {
            LB_MSG.setText(msg);
            PANEL_MSG.setVisible(true);
        }
        else {
            PANEL_MSG.setVisible(false);
        }
    }
    
    /**
     * Megadja, hogy az ablakhoz tartozó gomb be van-e nyomódva.
     * Ha nincs paraméter megadva, false.
     * @param w az ablak
     */
    public boolean getWindowVisibility(WindowType w) {
        JToggleButton bt = getButton(w);
        if (bt != null) return bt.isSelected();
        return false;
    }
    
    /**
     * Ha valamelyik dialógus megjelenik vagy bezárult, a gombok frissítése.
     * Az alábbi esetekben hívódik meg:
     * - Kezdetben egyes dialógusok láthatók, mások meg nem.
     * - Ha a felhasználó bezárja az egyik ablakot, a hozzá tartozó gomb kijelölését le kell venni.
     * @param w az ablak
     * @param visible true esetén kiválasztódik a gomb
     */
    public void setWindowVisibility(WindowType w, boolean visible) {
        JToggleButton bt = getButton(w);
        if (bt != null) bt.setSelected(visible);
    }
    
    /**
     * Az ablakhoz tartozó gombot adja vissza.
     * @param w az ablak
     * @return ha a paraméter nem null az ablakhoz tartozó gomb, egyébkénz null
     */
    private JToggleButton getButton(WindowType w) {
        if (w != null) {
            switch (w) {
                case CONTROLL:
                    return btArrow;
                case CHAT:
                    return btChat;
                case MAP:
                    return btMap;
            }
        }
        return null;
    }
    
    /**
     * Folyamatos növekedés gomb referenciát ad meg.
     * A vezérlő dialógus használja az alábbi célokból:
     * - eseménykezelőt ad hozzá
     * - ha a jármű sebessége nem állítható, a gombot inaktívvá kell tudni tenni
     */
    public JToggleButton getIncreaseButton() {
        return btIncrease;
    }
    
    /**
     * A főablak gombsorának a magasságát adja meg,
     * hogy a cset dialógusablak pozícióját be lehessen állítani a gombsor fölé.
     */
    public int getToolBarHeight() {
        return tb.getPreferredSize().height;
    }
    
}
