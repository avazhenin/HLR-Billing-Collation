/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hlr.billing.collation;

import javax.swing.SwingUtilities;

/**
 *
 * @author vazhenin
 */
public class HLRBillingCollation {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                System.setProperty("sun.java2d.noddraw", Boolean.TRUE.toString());
                new MainFrame(args).setVisible(true);
            }
        });
    }

}
