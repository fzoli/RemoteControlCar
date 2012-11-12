package org.dyndns.fzoli.ui.systemtray;

import java.awt.AWTException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 *
 * @author zoli
 */
class AwtTrayIcon implements TrayIcon {

    private final java.awt.TrayIcon icon;
    
    private final java.awt.SystemTray tray;
    
    private boolean visible = true;
    
    private ActionListener evt;
    
    public AwtTrayIcon(java.awt.SystemTray tray, java.awt.TrayIcon icon) {
        this.icon = icon;
        this.tray = tray;
        icon.setImageAutoSize(true);
    }

    @Override
    public PopupMenu createPopupMenu() {
        java.awt.PopupMenu menu = new java.awt.PopupMenu();
        icon.setPopupMenu(menu);
        return new AwtPopupMenu(icon, menu);
    }

    @Override
    public void setOnClickListener(final Runnable r) {
        if (r != null) {
            icon.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                        r.run();
                    }
                }
                
            });
        }
    }

    @Override
    public void displayMessage(String title, String msg, IconType ic) {
        displayMessage(title, msg, ic, null);
    }
    
    @Override
    public void displayMessage(String title, String msg, IconType ic, final Runnable onClick) {
        java.awt.TrayIcon.MessageType type = null;
        switch (ic) {
            case INFO:
                type = java.awt.TrayIcon.MessageType.INFO;
                break;
            case WARNING:
                type = java.awt.TrayIcon.MessageType.WARNING;
                break;
            case ERROR:
                type = java.awt.TrayIcon.MessageType.ERROR;
        }
        icon.displayMessage(title, msg, type);
        if (evt != null) {
            icon.removeActionListener(evt);
        }
        if (onClick != null) {
            evt = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    //TODO: csak akkor fusson le, ha az üzenetre kattintottak és nem az ikonra duplán
                    onClick.run();
                }

            };
            icon.addActionListener(evt);
        }
    }

    @Override
    public void setImage(InputStream in) {
        try {
            icon.setImage(ImageIO.read(in));
        }
        catch (IOException ex) {
            ;
        }
    }

    @Override
    public void setToolTip(String text) {
        icon.setToolTip(text);
    }

    @Override
    public void setVisible(boolean b) {
        if (visible ^ b) {
            try {
                if (b) tray.add(icon);
                else tray.remove(icon);
                visible = !visible;
            }
            catch (AWTException ex) {
                ;
            }
        }
    }
    
}
