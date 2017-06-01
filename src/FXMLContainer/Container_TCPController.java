/* 
 * Copyright (C) 2017 kilian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package FXMLContainer;

import Server.CheckConnection;
import Server.Computer;
import static helpers.Actions.isOnlyNumbers;
import static helpers.Actions.parseInt;
import helpers.Information;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import Server.ServerListenerThread;
import helpers.Actions;

/**
 * FXML Controller class
 *
 * @author kilian
 */
public class Container_TCPController implements Initializable {

    @FXML
    private TextField tb_Port;
    @FXML
    private TextField tb_ipAdress;
    @FXML
    private Button btn_toggle;
    @FXML
    private RadioButton rb_Server;
    @FXML
    private ToggleGroup one;
    @FXML
    private RadioButton rb_Client;
    @FXML
    private Label lbl_ServerStatus;

    TCPState tCPState = TCPState.Server;
    @FXML
    private Button btn_Test;

    private void tb_Port_onChange(String oldValue, String newValue) {

        if (!isOnlyNumbers(newValue) || parseInt(newValue) >= 65535) {
            tb_Port.setText(oldValue);
        }

    }

    @FXML
    private void btn_Test_onAction(ActionEvent event) {
    }

    public enum TCPState {
        Server,
        ServerRunning,
        Client,
        ClientRunning
    }

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        tb_ipAdress.setText(getIPAdress(true));
        tb_Port.setText("1235");

        tb_Port.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            tb_Port_onChange(oldValue, newValue);
        });

    }

    @FXML
    private void rb_onAction(ActionEvent event) {

        Information.setServerState("OFF");

        if (rb_Server.isSelected()) {
            tb_ipAdress.setDisable(true);
            tb_ipAdress.setText(getIPAdress(true));
            tCPState = TCPState.Server;
            btn_toggle.setText("Start server");
        } else if (rb_Client.isSelected()) {
            tb_ipAdress.setDisable(false);
            tb_ipAdress.setText(getIPAdress(false));
            tCPState = TCPState.Client;
            btn_toggle.setText("Connect to Server");
        }

    }

    @FXML
    private void btn_toggle_onAction(ActionEvent event) {

        switch (tCPState) {
            case Server:
                startServer();
                break;
            case ServerRunning:
                stopServer();
                break;
            case Client:
                startClient();
                break;
            case ClientRunning:
                stopClient();
                break;
        }

    }

    private void setTCPState(TCPState state) {

        boolean disable = state == TCPState.ClientRunning || state == TCPState.ServerRunning;

        rb_Client.setDisable(disable);
        rb_Server.setDisable(disable);
        tb_Port.setDisable(disable);
        tb_ipAdress.setDisable(disable);

        tCPState = state;

    }

    private void startServer() {
        btn_toggle.setText("Stop server");
        Information.getLocalComputer().setType(Computer.ComputerType.server);

        Information.setStopServer(false);
        new ServerListenerThread(parseInt(tb_Port.getText())).start();

        setTCPState(TCPState.ServerRunning);
    }

    private void stopServer() {
        btn_toggle.setText("Start server");
        Information.getLocalComputer().setType(Computer.ComputerType.standalone);
        Information.setStopServer_or_Client(true);
        Actions.closeStream(Information.getsSocket());
        for (int i = 0; i < Information.getConnectedComputers().size(); i++) {
            Information.getConnectedComputers().get(i).shutdownConnection();
        }
        setTCPState(TCPState.Server);
        tb_ipAdress.setDisable(true);
    }

    private void startClient() {

        Information.getLocalComputer().setData(tb_ipAdress.getText(), parseInt(tb_Port.getText()));

        if (Information.threadsRunning()) {
            Actions.showAlert("Error", "There are rendering tasks running", "Please stop rendering tasks or wait untill all tasks are finished");
        }

        System.out.println("Starting client...");

        try {
            btn_toggle.setText("Stop connection");

            CheckConnection checkThread = new CheckConnection(tb_ipAdress.getText(), parseInt(tb_Port.getText()));
            checkThread.start();

            int cntr = 0;
            boolean timeout = false;

            System.out.println("Waiting for Server...");

            while (!checkThread.isConnected()) {
                cntr++;
                Thread.sleep(100);

                if (!checkThread.isAlive()) {
                    Actions.showAlert("Error", "Could not connect to server", "Unknown error -- is there a server running in the same\nnetwork as you are?");
                    timeout = true;
                    break;
                }
                if (cntr >= 60) {
                    Actions.showAlert("Error", "Connection timeout", "Could not connect to Server");
                    checkThread.close();
                    timeout = true;
                    break;
                }
            }
            if (!timeout) {
                Actions.showAlert("Connection succesful", "You are now connected to the Server", "Server: " + tb_ipAdress.getText()
                        + " on Port: " + tb_Port.getText());
                Information.getLocalComputer().setType(Computer.ComputerType.client);
                Information.getLocalComputer().connectTo(checkThread);
            }

        } catch (InterruptedException ex) {
            Actions.showError("Could not connect to Server", ex);
        }

        setTCPState(TCPState.ClientRunning);

    }

    private void stopClient() {

        System.out.println("Stoping client...");

        btn_toggle.setText("Connect to server");

        Information.setStopServer_or_Client(true);
        Information.getLocalComputer().shutdownConnection();

        setTCPState(TCPState.Client);
    }

    /**
     * Get the Host IP-Adress of your Computer
     *
     * @return the IP-Adress that starts with 192
     */
    private String getIPAdress(boolean server) {
        try {
            Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
            for (; n.hasMoreElements();) {
                NetworkInterface e = n.nextElement();

                Enumeration<InetAddress> a = e.getInetAddresses();
                for (; a.hasMoreElements();) {
                    InetAddress addr = a.nextElement();

                    if (addr.getHostAddress().startsWith("192")) {
                        if (server) {
                            return addr.getHostAddress();
                        } else {
                            return splitIP(addr.getHostAddress());
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            return "<Socket Exception: Type in your IP manually>";
        }

        return "<Local IP not found: Type in your IP manually>";
    }

    private String splitIP(String ip) {

        String[] str = ip.split("\\.");
        String tmp = "";

        for (int i = 0; i <= str.length - 2; i++) {
            tmp += str[i] + ".";
        }
        return tmp + "XX";

    }

    public Label getLbl_ServerStatus() {
        return lbl_ServerStatus;
    }

}
