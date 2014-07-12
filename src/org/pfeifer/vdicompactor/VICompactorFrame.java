/*
 Copyright 2014-2014
 Fabio Melo Pfeifer

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.pfeifer.vdicompactor;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class VICompactorFrame extends javax.swing.JFrame {

    /**
     * Creates new form VICompactorFrame
     */
    public VICompactorFrame() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lbFile = new javax.swing.JLabel();
        txtFile = new javax.swing.JTextField();
        btnChoose = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        btnCompact = new javax.swing.JButton();
        chkSearchCurrentDirectory = new javax.swing.JCheckBox();
        lbAdditionalDir = new javax.swing.JLabel();
        txtDir = new javax.swing.JTextField();
        btnChooseDir = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Virtual Image Compactor");

        lbFile.setText("File:");

        btnChoose.setText("...");
        btnChoose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChooseActionPerformed(evt);
            }
        });

        btnCompact.setText("Compact");
        btnCompact.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompactActionPerformed(evt);
            }
        });

        chkSearchCurrentDirectory.setSelected(true);
        chkSearchCurrentDirectory.setText("Search current directory for parent VDIs");

        lbAdditionalDir.setText("Additional directory to search for parent VDIs:");

        btnChooseDir.setText("...");
        btnChooseDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChooseDirActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lbFile)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(txtFile))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnChoose))
                    .addComponent(chkSearchCurrentDirectory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbAdditionalDir)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnCompact))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(txtDir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnChooseDir)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbFile)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnChoose))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkSearchCurrentDirectory)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbAdditionalDir)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnChooseDir))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnCompact)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnChooseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnChooseActionPerformed
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                }
                String s = file.getName();
                int i = s.lastIndexOf('.');

                if (i > 0 && i < s.length() - 1) {
                    String ext = s.substring(i + 1).toLowerCase();
                    if (ext.equals("vdi")) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "VDI Files";
            }
        }
        );
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            txtFile.setText(f.getAbsolutePath());
        }
    }//GEN-LAST:event_btnChooseActionPerformed

    private void btnCompactActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompactActionPerformed
        final VDICompactor compactor = new VDICompactor();
        progressBar.setMaximum(1000);
        progressBar.setMinimum(0);
        compactor.addCompactProgressListener(new CompactProgressListener() {

            @Override
            public void onProgress(final CompactProgressEvent event) {
                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        progressBar.setValue(event.getCompleted());
                    }

                });

            }
        });

        SwingWorker worker = new SwingWorker() {

            private boolean success = false;

            @Override
            protected Object doInBackground() throws Exception {
                try {
                    compactor.compactVDI(txtFile.getText(), txtDir.getText(), 
                            chkSearchCurrentDirectory.isSelected());
                    success = true;
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(VICompactorFrame.this,
                            "Error: " + e.getMessage(),
                            "Error processing file", JOptionPane.ERROR_MESSAGE);
                } catch (Throwable x) {
                    x.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(VICompactorFrame.this, "File compacted.");
                }
            }

        };
        worker.execute();
    }//GEN-LAST:event_btnCompactActionPerformed

    private void btnChooseDirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnChooseDirActionPerformed
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Directories only";
            }
        }
        );
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            txtDir.setText(f.getAbsolutePath());
        }
    }//GEN-LAST:event_btnChooseDirActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VICompactorFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new VICompactorFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnChoose;
    private javax.swing.JButton btnChooseDir;
    private javax.swing.JButton btnCompact;
    private javax.swing.JCheckBox chkSearchCurrentDirectory;
    private javax.swing.JLabel lbAdditionalDir;
    private javax.swing.JLabel lbFile;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JTextField txtDir;
    private javax.swing.JTextField txtFile;
    // End of variables declaration//GEN-END:variables
}
