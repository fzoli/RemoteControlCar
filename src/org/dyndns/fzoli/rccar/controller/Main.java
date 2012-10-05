package org.dyndns.fzoli.rccar.controller;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.SplashScreen;
import javax.swing.JFrame;

/**
 * A vezérlő indító osztálya.
 * @author zoli
 */
public class Main {
    
    /**
     * Még mielőtt lefutna a main metódus, megjelenik egy töltés-jelző.
     */
    static {
        showSplashScreen();
    }
    
    /**
     * Megjeleníti a töltésjelzőt.
     */
    private static void showSplashScreen() {
        final SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) {
            Graphics2D g = splash.createGraphics();
            if (g != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.BLACK);
                g.setFont(new Font("Arial", Font.PLAIN, 12));
                printSimpleString(g, "Kapcsolódás a szerverhez...", 300, 185);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                printSimpleString(g, "Mobile-RC", 300, 35);
                splash.update();
            }
        }
    }
    
    /**
     * A felületre középre igazítva írja ki a megadott szöveget.
     * @param g a felület, amire kirajzolódik a szöveg
     * @param s a kirajzolandó szöveg
     * @param width a felület szélessége pixelben
     * @param y a magasság koordináta
     */
    private static void printSimpleString(Graphics2D g, String s, int width, int y) {
        int len = (int) g.getFontMetrics().getStringBounds(s, g).getWidth();
        int start = width / 2 - len / 2;
        g.drawString(s, start, y);
    }
    
    /**
     * A vezérlő main metódusa.
     * Teszt: SplashScreen
     */
    public static void main(String[] args) throws InterruptedException {
        for(int i = 0; i < 100; i++) {
            Thread.sleep(100);
        }
        new JFrame() {
            {
                setSize(200, 100);
                setTitle("Teszt vége");
                setLocationRelativeTo(this);
                setDefaultCloseOperation(EXIT_ON_CLOSE);
            }
        }.setVisible(true);
    }
    
}
