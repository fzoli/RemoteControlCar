package org.dyndns.fzoli.rccar.controller;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/**
 * A kapcsolatbeállító ablak súgója.
 * @author zoli
 */
public class ConfigHelpDialog extends JDialog {

    private static final JLabel taHelp = new JLabel("<html>Lorem ipsum dolor sit amet, consectetur adipiscing elit.<br>Donec consectetur vulputate quam, interdum laoreet<br>nunc elementum vel. Donec eget pharetra dolor.<br><br>Lorem ipsum dolor sit amet, consectetur adipiscing elit.<br>Donec consectetur vulputate quam, interdum laoreet<br>nunc elementum vel. Donec eget pharetra dolor.</html>");
    
    public ConfigHelpDialog(Dialog owner) {
        super(owner, "Súgó");
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setModalityType(ModalityType.APPLICATION_MODAL);
        
        taHelp.setOpaque(true);
        taHelp.setBackground(Color.WHITE);
        taHelp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JScrollPane sp = new JScrollPane(taHelp);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp);
        
        pack();
        setMinimumSize(new Dimension(getSize().width + 30, 1));
        setLocationRelativeTo(owner);
    }
    
}