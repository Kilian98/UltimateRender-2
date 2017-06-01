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
import helpers.Information;
import java.net.Socket;
import Server.Computer.ComputerType;
import static helpers.Actions.parseInt;
import helpers.Constants;
import java.io.IOException;
import java.net.SocketException;
import objects.SimpleString;

/**
 *
 * @author kilian
 */
public class CheckConnection extends ConnectionThread {

    boolean connected = false;
    boolean friendlyShutdown = false; //set to true to stop connection without Socket Exception

    public CheckConnection(Socket s) throws IOException {
        super(s); //server
    }

    public CheckConnection(String ipAddress, int port) {
        super(ipAddress, port); //client
    }

    public boolean isConnected() {
        return connected;
    }

    public void shutdown() {
        friendlyShutdown = true;
    }

    @Override
    public void run() {

        try {

            if (!isServer) {     //Part foir Client Connection

                establishClientConnection(Constants.checkForConnection);
//
//                Information.setLocalComputer((Computer) readObject());
//                Information.getLocalComputer().setSocket(socket);
                connected = true;
                Information.setServerState("Connected");
                String tmpID = readLine();
                System.out.println(tmpID);
                Information.getLocalComputer().setId(parseInt(tmpID));

                while (!friendlyShutdown) {
                    try {
                        sendObject(null);
                        Object tmpObj = readObject();
                        if (tmpObj != null && !(tmpObj instanceof Computer) && ((SimpleString) tmpObj).toString().equals("shutdown")) {
                            System.out.println("friendly shutdown");
                            break;
                        }
                    } catch (TimeoutException | SocketException ex) {
                        System.err.println("The connection shut down (Error)");
                        Information.setServerState("Server Connection Lost");
                        connected = false;
                        Information.getLocalComputer().shutdownConnection();
                        break;
                    }
                    Thread.sleep(1000);
                }

                if (friendlyShutdown) {
                    sendLine("shutdown");
                }
            } else { //Server part

                connected = true;
                Computer pc = new Computer(ComputerType.client);
                sendLine(pc.getId() + "");
//                pc.setIPAddress(socket.getRemoteSocketAddress().toString().split(":")[0].substring(1));
                pc.setType(ComputerType.server);
                pc.setConnection(this);
                Information.addComputerToList(pc);

                while (!friendlyShutdown) {
                    try {
                        sendObject(null);
                        Object tmpObj = readObject();
                        if (tmpObj != null && ((SimpleString) tmpObj).toString().equals("shutdown")) {
                            System.out.println("friendly shutdown");
                            break;
                        }
                    } catch (TimeoutException | IOException ex) {
                        System.err.println("The connection shut down (Error)");
                        connected = false;
                        Information.getLocalComputer().shutdownConnection();
                        break;
                    }
                    Thread.sleep(1000);
                }

                if (friendlyShutdown) {
                    System.out.println("Friendly shutdown attempt by server");
                    sendLine("shutdown");
                }

                Information.removeComputerFromList(pc);

            }

        } catch (IOException | TimeoutException ex) {
            System.err.println("Error or Shutdown!");
            Information.setServerState("ERROR");
            ex.printStackTrace();
        } catch (ClassNotFoundException | InterruptedException ex) {
        } finally {
            connected = false;
            if (!friendlyShutdown) {
                close();
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                }
                close();
            }
        }

    }

}
