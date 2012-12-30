package org.dyndns.fzoli.rccar.test;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 *
 * @author zoli
 */
public class PositionTest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                JFrame fr1 = new JFrame();
                fr1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                fr1.setSize(200, 100);
                fr1.setLocationRelativeTo(null);
//                fr1.setVisible(true);
                JDialog d1 = new JDialog();
                d1.setSize(200, 50);
//                d1.setVisible(true);
//                System.out.println(fr1.getInsets());
//                System.out.println(d1.getInsets());
                d1.setLocation(fr1.getX(), fr1.getY() + fr1.getHeight());
                JDialog d2 = new JDialog();
                d2.setSize(100, 100);
                d2.setLocation(fr1.getX()+fr1.getWidth(), fr1.getY());
                d2.setVisible(true);
                fr1.setVisible(true);
                d1.setVisible(true);
            }
            
        });
    }
}
