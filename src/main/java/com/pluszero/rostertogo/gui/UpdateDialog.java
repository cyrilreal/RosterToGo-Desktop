/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.gui;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 *
 * @author Cyril
 */
public class UpdateDialog {
        
    public static UpdateDialog NewInstance() {
        UpdateDialog dialog = new UpdateDialog();
        return dialog;
    }
    
    public UpdateDialog() {
        showDialog();
    }
    
    private void showDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText("Nouvelle version disponible");
        
        // Create the content, plain and hypertext.
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);
        
        Text title = new Text("Pour la télécharger, rendez vous sur le site de RosterToGo");
        vbox.getChildren().add(title);
        
        Hyperlink hyperlink = new Hyperlink("http://rostertogo.free.fr");
        hyperlink.setOnAction((ActionEvent t) -> {
            try {
                java.awt.Desktop.getDesktop().browse(new URI(hyperlink.getText()));
            } catch (URISyntaxException ex) {
                Logger.getLogger(UpdateDialog.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(UpdateDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        vbox.getChildren().add(hyperlink);
        
        alert.getDialogPane().setContent(vbox);
        alert.showAndWait();
    }
}
