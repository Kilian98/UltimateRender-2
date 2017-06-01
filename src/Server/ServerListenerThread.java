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
package Server;

import Exceptions.TimeoutException;
import helpers.Actions;
import static helpers.Actions.parseInt;
import helpers.Constants;
import helpers.Information;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 *
 * @author kilian
 */
public class ServerListenerThread extends ConnectionThread {

    ServerSocket sSocket;
    int id = -1;

    public ServerListenerThread(int port) {
        super("", port);
    }

    @Override
    public void run() {

        try {
            sSocket = new ServerSocket(port);
            Information.setsSocket(sSocket);
        } catch (IOException ex) {
            Actions.showError("An error occured while starting the server", ex);
            return;
        }

        System.out.println("Server started");
        Information.setServerState("Server running...");

        while (!Information.isStopServer()) {

            try {
                changeSocket(sSocket.accept());
                sendLine("Accepted");
                id = parseInt(readLine());

                switch (readLine()) {
                    case Constants.requestBlenderFiles:
                        copyBlenderFiles();
                        break;
                    case Constants.checkForConnection:
                        checkForConnection();
                        break;
                    case Constants.requestNewJob:
                        copyNewJob();
                        break;
                    case Constants.sendInformation:
                        sendInfo();
                        break;
                    case Constants.copyRenderedImage:
                        copyImage();
                        System.out.println("Got transmit image attempt");
                        break;
                }

            } catch (SocketException | TimeoutException e) {
                System.out.println("Server stopped!");
                System.out.println(e.getMessage());
                Information.setServerState("Server stopped");
            } catch (IOException ex) {
//                ex.printStackTrace();
            } catch (ClassNotFoundException ex) {
            }

        }

    }

    private void changeSocket(Socket s) throws IOException {
        try {
            socket = s;
            sIn = socket.getInputStream();
            sOut = socket.getOutputStream();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    private void copyBlenderFiles() {

        try {
            CopyBlenderFileThread t = new CopyBlenderFileThread(socket);
            startThread(t);
        } catch (IOException | NullPointerException ex) {
            ex.printStackTrace();
        }

    }

    private void checkForConnection() {

        try {
            CheckConnection t = new CheckConnection(socket);
            startThread(t);
        } catch (IOException | NullPointerException ex) {
            ex.printStackTrace();
        }

    }

    private void copyNewJob() {

        try {
            CopyRenderTask t = new CopyRenderTask(socket);
            startThread(t);
        } catch (IOException | NullPointerException ex) {
            ex.printStackTrace();
        }

    }

    private void sendInfo() {
        try {
            CopyInformation t = new CopyInformation(socket);
            startThread(t);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void copyImage() {
        try {
            CopyImage t = new CopyImage(socket);
            startThread(t);
        } catch (IOException | NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    private void startThread(ConnectionThread t) {
        if (!(t instanceof CheckConnection) && id != -1) {
            Information.getComputer(id).addConnection(t);
            System.out.println("Server added connection");
        }
        t.start();
        id = -1;
    }
}
