/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.text.Text;

/**
 *
 * @author Cyril
 */
public class SimpleDialog {

    private final String title;
    private final String header;
    private final String content;

    public static SimpleDialog NewInstance(String title, String header, String content) {
        SimpleDialog dialog = new SimpleDialog(title, header, content);
        return dialog;
    }

    public static SimpleDialog NewInstance(String title, String content) {
        SimpleDialog dialog = new SimpleDialog(title, content);
        return dialog;
    }

    public SimpleDialog(String title, String header, String content) {
        this.title = title;
        this.header = header;
        this.content = content;
        showDialog();
    }

    public SimpleDialog(String title, String content) {
        this.title = title;
        this.content = content;
        this.header = null;
        showDialog();
    }

    private void showDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setGraphic(null);
        Text text = new Text(content);
        text.setWrappingWidth(320);
        alert.getDialogPane().setContent(text);
        alert.getDialogPane().setPadding(new Insets(8));
        //alert.setContentText(content);
        alert.showAndWait();
    }
}
