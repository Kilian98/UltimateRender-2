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
import java.io.File;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author Kilian Brenner visit me on <aklio.de>
 */
public class CopyImage extends ConnectionThread {

    File destination;
    File location;

    @Override
    public void run() {

        if (!isServer) {
            try {
                establishClientConnection(Constants.copyRenderedImage);
                sendObject(destination);
                sendFile(location);
                System.out.println("Sending File: " + location + " to " + destination);

            } catch (IOException | ClassNotFoundException | TimeoutException ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                destination = (File) readObject();
                readFile(destination);
                System.out.println("Read File: " + destination);
            } catch (IOException | ClassNotFoundException | TimeoutException ex) {
                ex.printStackTrace();
            }
        }

        close();

    }

    public CopyImage(String ip, int port, File location, File destination) {
        super(ip, port);
        this.location = location;
        this.destination = destination;
    }

    public CopyImage(Socket s) throws IOException {
        super(s);
    }

}
