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
import helpers.Constants;
import helpers.Information;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author kilian
 */
public class CopyInformation extends ConnectionThread {

    private Computer informationComp;
//    boolean transmitted = false;
    boolean error = false;

    public CopyInformation(String ip, int port, Computer infoComp) {
        super(ip, port);
        this.informationComp = infoComp;
    }

    public CopyInformation(Socket s) throws IOException {
        super(s);
    }

    public void getInformationComp() { //for the client

        try {
            establishClientConnection(Constants.sendInformation);

            Computer toSend = new Computer(Computer.ComputerType.client);
            toSend.setFramesRendered(Information.getLocalComputer().getFramesRendered(true));
            toSend.setRenderingThreads(Information.getLocalComputer().getRenderingThreads());
            toSend.deleteConnectionList();
            toSend.setConnection(null);
            toSend.setId(Information.getLocalComputer().getId());

            sendObject(toSend);
            Information.setGlobalComputer((Computer) readObject());
        } catch (IOException | ClassNotFoundException | TimeoutException ex) {
            ex.printStackTrace();
        } finally {
            close();
        }

    }

    @Override
    public void run() {

        try {
            sendObject(Information.getGlobalComputer());

            informationComp = (Computer) readObject();
            Computer tmp = Information.getComputer(informationComp.getId());

            tmp.setFramesRendered(tmp.getFramesRendered() + informationComp.getFramesRendered());
            tmp.setRenderingThreads(informationComp.getRenderingThreads());

        } catch (IOException ex) {
        } catch (ClassNotFoundException | TimeoutException ex) {
        } finally {
            close();
        }
        

    }

}
