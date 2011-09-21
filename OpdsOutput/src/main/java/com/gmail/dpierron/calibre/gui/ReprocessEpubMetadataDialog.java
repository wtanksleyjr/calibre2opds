package com.gmail.dpierron.calibre.gui;

import com.gmail.dpierron.calibre.configuration.ConfigurationManager;
import com.gmail.dpierron.calibre.datamodel.Book;
import com.gmail.dpierron.calibre.datamodel.DataModel;
import com.gmail.dpierron.calibre.datamodel.Tag;
import com.gmail.dpierron.calibre.opds.Catalog;
import com.gmail.dpierron.calibre.opds.i18n.Localization;
import com.gmail.dpierron.calibre.opf.OpfOutput;
import com.gmail.dpierron.tools.Helper;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * @author David
 */
public class ReprocessEpubMetadataDialog extends javax.swing.JDialog {
  private final static Logger logger = Logger.getLogger(Catalog.class);
  int maxScale;
  double to30000;
  int pos;
  int position;
  boolean stopThread = false;
  boolean removeCss = false;
  File defaultStyleSheet = null;
  String onlyForTag = null;

  /**
   * Creates new form GenerateCatalogDialog
   */
  public ReprocessEpubMetadataDialog(java.awt.Frame parent, boolean modal, boolean removeCss, File defaultStyleSheet, String onlyForTag) {
    super(parent, modal);
    setLocationRelativeTo(parent);
    initComponents();
    this.removeCss = removeCss;
    this.defaultStyleSheet = defaultStyleSheet;
    this.onlyForTag = onlyForTag;
  }

  public void start() {
    Runnable runnable = new Runnable() {
      public void run() {
        try {
          ReprocessEpubMetadataDialog.this.run();
        } finally {
          ReprocessEpubMetadataDialog.this.setVisible(false);
        }
      }
    };
    new Thread(runnable).start();
    setVisible(true);
  }

  private void run() {
    jProgress.setValue(0);
    ConfigurationManager.INSTANCE.getCurrentProfile().getDatabaseFolder();
    DataModel.INSTANCE.preloadDataModel();
    java.util.List<Book> books = null;
    if (Helper.isNullOrEmpty(onlyForTag))
      books = DataModel.INSTANCE.getListOfBooks();
    else {
      Tag theTag = null;
      for (Tag tag : DataModel.INSTANCE.getListOfTags()) {
        if (tag.getName().equalsIgnoreCase(onlyForTag)) {
          theTag = tag;
          break;
        }
      }
      if (theTag != null)
        books = DataModel.INSTANCE.getMapOfBooksByTag().get(theTag);
    }
    setMaxScale(books.size());
    for (Book book : books) {
      if (stopThread)
        break;
      lblBookTitle.setText(book.getTitle());
      OpfOutput opfOutput = new OpfOutput(book, removeCss, defaultStyleSheet);
      try {
        opfOutput.processEPubFile();
      } catch (Exception e) {
        logger.error(e);
        String message = Localization.Main.getText("gui.error.tools.processEpubMetadataOfAllBooks", book.getTitle(), e.getMessage());
        JOptionPane.showMessageDialog(this, message);
        message = Localization.Main.getText("gui.error.tools.processEpubMetadataOfAllBooks2");
        int result = JOptionPane.showConfirmDialog(this, message, "", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION)
          return;
      }
      incPosition();
    }
    if (!stopThread) {
      String message = Localization.Main.getText("gui.finished.tools.processEpubMetadataOfAllBooks");
      JOptionPane.showMessageDialog(this, message);
    }
  }

  @Override
  public void pack() {
    super.pack();
    Rectangle oldbounds = getBounds();
    oldbounds.width += 10;
    oldbounds.height += 10;
    setBounds(oldbounds);
  }

  public void setMaxScale(long maxScale) {
    if (maxScale > 30000) {
      to30000 = 30000d / maxScale;
      this.maxScale = 30000;
    } else
      this.maxScale = (int) maxScale;
    jProgress.setMaximum(this.maxScale);
  }

  public void incPosition() {
    position++;
    int newPos;
    if (to30000 > 0)
      newPos = (int) (position * to30000);
    else
      newPos = position;
    if (newPos > pos) {
      jProgress.setValue(newPos + 8);
      pos = newPos;
    }
  }

  public void errorOccured(String message, Throwable error) {
    String msg;
    String title;
    if (error != null) {
      title = message;
      if (Helper.isNullOrEmpty(title))
        title = error.getClass().getName();
      msg = Helper.getStackTrace(error);
    } else {
      msg = message;
      title = "";
    }
    JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    logger.error(message, error);
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed"
  // <editor-fold defaultstate="collapsed"
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    jProgress = new javax.swing.JProgressBar();
    cmdCancel = new javax.swing.JButton();
    lblBookTitle = new javax.swing.JLabel();

    setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    setTitle(Localization.Main.getText("gui.tools.processEpubMetadataOfAllBooks")); // NOI18N
    getContentPane().setLayout(new java.awt.GridBagLayout());

    jProgress.setMaximum(61);
    jProgress.setPreferredSize(new java.awt.Dimension(300, 19));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    getContentPane().add(jProgress, gridBagConstraints);

    cmdCancel.setText(Localization.Main.getText("gui.cancel")); // NOI18N
    cmdCancel.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cmdCancelActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    getContentPane().add(cmdCancel, gridBagConstraints);

    lblBookTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    lblBookTitle.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    getContentPane().add(lblBookTitle, gridBagConstraints);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void cmdCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdCancelActionPerformed
    stopThread = true;
  }//GEN-LAST:event_cmdCancelActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton cmdCancel;
  private javax.swing.JProgressBar jProgress;
  private javax.swing.JLabel lblBookTitle;
  // End of variables declaration//GEN-END:variables

}
