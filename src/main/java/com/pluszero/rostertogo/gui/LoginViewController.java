/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.pluszero.rostertogo.gui;

import com.pluszero.rostertogo.MainApp;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Cyril
 */
public class LoginViewController {

    @FXML
    private WebView webView;
    @FXML
    private TextField tfLogin;
    @FXML
    private PasswordField pfPassword;
    @FXML
    private Button btnCopyLogin;
    @FXML
    private Button btnCopyPassword;
    @FXML
    private CheckBox cbxShowPassword;

    private WebEngine we;
    private CookieManager cookieManager;
    private Stage loginStage;
    private MainViewController mainViewController;
    private PasswordFieldSkin pwdfldSkin;

    @FXML
    public void initialize() {
        we = webView.getEngine();
        cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        connect();
    }

    /**
     * Sets the stage of this dialog.
     *
     * @param dialogStage
     */
    public void setStage(Stage loginStage) {
        this.loginStage = loginStage;
    }

    public void setMainViewController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
        pwdfldSkin = new PasswordFieldSkin(pfPassword);
        this.pfPassword.setSkin(pwdfldSkin);
    }

    public void connect() {

//        we.getLoadWorker().stateProperty().addListener((ObservableValue<? extends State> ov, State t, State t1) -> {
//            System.out.println(t1);
//            if (t1 == Worker.State.SUCCEEDED) {
//                we.executeScript("var node = document.getElementById('fromURI'); alert(node);");
//                System.out.println("SUCCEEDED !");
//            }
//        });
        we.locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if (newLocation.equals("https://planning.to.aero/Home")) {
                for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
                    if (cookie.getName().contains("SessionId")) {
                        mainViewController.setSessionId(
                                cookie.getName() + "=" + cookie.getValue());
                    }
                }
                mainViewController.launchConnectionWithToken();
                loginStage.close();
            }
        });

        we.setJavaScriptEnabled(true);
        we.setOnAlert(
                (WebEvent<String> event) -> {
                    System.out.println(event.toString());
                }
        );
        we.setOnError(
                (WebErrorEvent event) -> {
                    System.out.println(event.toString());
                }
        );
        we.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0");
        we.load("https://planning.to.aero/SAML/SingleSignOn");
    }

//    private void listNodesByName(Document doc, String name) {
//        NodeList nl = doc.getElementsByTagName(name);
//        if (nl != null) {
//            System.out.println(nl.toString());
//            for (int i = 0; i < nl.getLength(); i++) {
//                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
//                    Element el = (Element) nl.item(i);
//                    listAllAttributes(el);
//                }
//            }
//        }
//    }
//    private void listAllAttributes(Element element) {
//
//        System.out.println("List attributes for node: " + element.getNodeName());
//
//        // get a map containing the attributes of this node 
//        NamedNodeMap attributes = element.getAttributes();
//
//        // get the number of nodes in this map
//        int numAttrs = attributes.getLength();
//
//        for (int i = 0; i < numAttrs; i++) {
//            Attr attr = (Attr) attributes.item(i);
//
//            String attrName = attr.getNodeName();
//            String attrValue = attr.getNodeValue();
//
//            System.out.println("Found attribute: " + attrName + " with value: " + attrValue);
//        }
//    }
    @FXML
    private void copyLogin() {
        java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(tfLogin.getText());
        clipboard.setContents(stringSelection, null);
    }

    @FXML
    private void copyPassword() {
        java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(pfPassword.getText());
        clipboard.setContents(stringSelection, null);
    }

    private void updatePasswordValue() {

    }

    public void loadPrefs() {
        tfLogin.setText(MainApp.userPrefs.oktaLogin);
        pfPassword.setText(MainApp.userPrefs.oktaPassword);
    }

    /*
    * method is public to be called by the Mainview controller in the onHidden
    * event
     */
    public void savePrefs() {
        MainApp.userPrefs.oktaLogin = tfLogin.getText();
        MainApp.userPrefs.oktaPassword = pfPassword.getText();
    }

    public class PasswordFieldSkin extends TextFieldSkin {

        private static final char DEFAULT_MASKING_CHAR = '\u2022';
        private boolean isMasked = true;

        public PasswordFieldSkin(TextField control) {
            super(control);
        }

        @Override
        protected String maskText(String txt) {

            if (!isMasked) {
                return txt;
            }
            if (getSkinnable() instanceof PasswordField) {
                int n = txt.length();
                StringBuilder passwordBuilder = new StringBuilder(n);
                for (int i = 0; i < n; i++) {
                    passwordBuilder.append(DEFAULT_MASKING_CHAR);
                }
                return passwordBuilder.toString();
            } else {
                return txt;
            }
        }

        public void setIsMasked(boolean isMasked) {
            this.isMasked = isMasked;
            // refresh the field
            getSkinnable().setText(getSkinnable().getText());
        }
    }

    public void toggleMask() {
        if (cbxShowPassword.isSelected()) {
            pwdfldSkin.setIsMasked(false);
        } else {
            pwdfldSkin.setIsMasked(true);
        }
        // refresh field
        pwdfldSkin.getSkinnable().setText(pwdfldSkin.getSkinnable().getText());

    }
}
