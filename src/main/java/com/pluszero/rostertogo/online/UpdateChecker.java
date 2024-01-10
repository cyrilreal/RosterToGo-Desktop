package com.pluszero.rostertogo.online;

import com.pluszero.rostertogo.Utils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;

/**
 *
 * @author Cyril
 */
public class UpdateChecker extends Task<Integer> {

    private static final String URL_HOST = "http://rostertogo.free.fr/download/buildnumber.txt";
    public static final String MSG_PROCESS_FINISHED = "Processus terminé";
    public static final int MSG_CONNECTION_ERROR = -1;

    private HttpURLConnection conn;
    private String content;

    public UpdateChecker() {

    }

    @Override
    protected Integer call() throws Exception {

        try {
            URL url;
            url = new URL(URL_HOST);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);

            content = Utils.stringFromInputStream(conn.getInputStream());
            content = content.replaceAll("[^\\d]", "");
        } catch (MalformedURLException ex) {
            Logger.getLogger(UpdateChecker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UpdateChecker.class.getName()).log(Level.SEVERE, null, ex);
            return MSG_CONNECTION_ERROR;
        }
        return Integer.valueOf(content);
    }
}
