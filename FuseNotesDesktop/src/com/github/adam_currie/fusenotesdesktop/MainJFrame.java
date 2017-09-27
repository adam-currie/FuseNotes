/*
 * The MIT License
 *
 * Copyright 2017 Adam Currie.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.adam_currie.fusenotesdesktop;

import com.github.adam_currie.fusenotesclient.*;
import java.awt.Dialog;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/*
 * Name     MainNotesJFrame
 * Purpose  Main frame for desktop notes application.
 */
public class MainJFrame extends javax.swing.JFrame implements NoteStoreListener{
    
    //todo: encrypt with hardware info as the key
    private final String KEY_PATH = "key.sav";
    private NoteStore notes = null;

    public NoteStore getNoteStore(){
        return notes;
    }

    /*
     * Method                       MainNotesJFrame
     * Description                  constructor
     */
    public MainJFrame(){
        initComponents();
        
        BoxLayout listLayout = new BoxLayout(notesListPanel, BoxLayout.Y_AXIS);
        notesListPanel.setLayout(listLayout);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        addButton = new javax.swing.JButton();
        notesScrollPane = new javax.swing.JScrollPane();
        notesListPanel = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        changePassButton = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(380, 340));

        addButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/setnotesdesktop/img/plusbutton.png"))); // NOI18N
        addButton.setToolTipText(null);
        addButton.setBorder(null);
        addButton.setContentAreaFilled(false);
        addButton.setPreferredSize(new java.awt.Dimension(32, 32));
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(598, Short.MAX_VALUE)
                .addComponent(addButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(addButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        notesListPanel.setLayout(new javax.swing.BoxLayout(notesListPanel, javax.swing.BoxLayout.LINE_AXIS));
        notesScrollPane.setViewportView(notesListPanel);

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        changePassButton.setText("Edit Private Key");
        changePassButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changePassButtonActionPerformed(evt);
            }
        });
        jMenu2.add(changePassButton);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(notesScrollPane, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(notesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 647, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void changePassButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changePassButtonActionPerformed
        showKeyChangeDlg();
    }//GEN-LAST:event_changePassButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        NoteJPanel note = new NoteJPanel(notes.addNote(true), this);
        notesListPanel.add(note);
        notesListPanel.revalidate();
    }//GEN-LAST:event_addButtonActionPerformed

    /*
     * Method               changePassword
     * Description          update the password used for storing and retriveing notes
     * Params          
     *  String password     base64 private key
     * Returns          
     *  boolean             true if password was valid and updates, false otherwise
     */
    public boolean changePassword(String password){
        try{
            notes = new NoteStore(password, this);
            savePassword(password);
            notesListPanel.removeAll();
            notesListPanel.revalidate();
            notesListPanel.repaint();
            return true;
        }catch(SQLException | InvalidKeyException ex){
            Logger.getLogger(MainJFrame.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]){
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try{
            for(javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()){
                if("Nimbus".equals(info.getName())){
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }catch(ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex){
            java.util.logging.Logger.getLogger(MainJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        
        //</editor-fold>
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(() -> {
            MainJFrame frame = new MainJFrame();
            frame.setLocationRelativeTo(null);//center
            frame.setVisible(true);
            frame.initNoteStore();
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JMenuItem changePassButton;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel notesListPanel;
    private javax.swing.JScrollPane notesScrollPane;
    // End of variables declaration//GEN-END:variables
    
    /*
     * Method                       showKeyChangeDlg
     * Description                  show the key change dialog
     */
    private void showKeyChangeDlg(){
        ChangeKeyJPanel keyPanel = null;
        if(notes == null){
            keyPanel = new ChangeKeyJPanel(this);
        }else{
            keyPanel = new ChangeKeyJPanel(this, notes.getPrivateKey());
        }
        
        JDialog dlg = new JDialog(this, "Edit Private Key", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.add(keyPanel);
        dlg.setResizable(false);
        dlg.pack();
        dlg.setLocationRelativeTo(null);//center
        dlg.setVisible(true);
    }
    
    /*
     * Method                   savePassword
     * Description              save the password
     * Params               
     *  String password         password to save
     */
    private void savePassword(String password){
        try (PrintWriter out = new PrintWriter(KEY_PATH)) {
            out.println(password);
        }catch(FileNotFoundException ex){
            Logger.getLogger(MainJFrame.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }

    /*
     * Method                       loadPassword
     * Description                  returns the password in base64
     * Returns          
     *  String                      the base64 password, null if password cannot be found
     */
    private String loadPassword(){
        try (BufferedReader br = new BufferedReader(new FileReader(KEY_PATH))) {
            return br.readLine();
        }catch(FileNotFoundException ex){
            return null;
        }catch(IOException ex){
            return null;
        }
    }
    
    /*
     * Method                       initNoteStore
     * Description                  initialize the NoteStore, load the key or get one from the user
     */
    private void initNoteStore(){
        String key = loadPassword();
        
        try{
            notes = new NoteStore(key, this);
        }catch(InvalidKeyException | NullPointerException ex){
            while(true){
                showKeyChangeDlg();
                if(notes == null){
                    int result = JOptionPane.showConfirmDialog(this, "A key is required to use this application.", "Key Required", JOptionPane.OK_CANCEL_OPTION);
                    if(result == JOptionPane.CANCEL_OPTION){
                        System.exit(0);
                    }
                }else{
                    break;
                }
            }
        }catch(SQLException ex){
            JOptionPane.showMessageDialog(this, ex, "Exception", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    @Override
    public void noteLoaded(Note note){
        SwingUtilities.invokeLater(() -> {            
            NoteJPanel notePanel = new NoteJPanel(note, this);
            notesListPanel.add(notePanel);
            notesListPanel.revalidate();
        });
    }

    @Override
    public void noteUpdateLoaded(Note note){
        throw new UnsupportedOperationException("Not supported yet.");
    }
}