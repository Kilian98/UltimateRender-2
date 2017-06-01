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
package helpers;

import java.io.File;

/**
 *
 * @author kilian
 */
public class Paths {

    private static String SettingsPath = "UltimateRender\\settings.ul";
    private static String QueuePath = "UltimateRender\\queue.ul";
    private static String workingDir = "UltimateRender\\tmp";

    public static String getSettingsPath() {
        return SettingsPath.replace("\\", File.separator); //todo: check for running os
    }

    public static String getQueuePath() {
        return QueuePath.replace("\\", File.separator); //todo: check for running os
    }

    /**
     * Returns the working directory and creates it, if it does not exist
     * @return the working directory
     */
    public static String getWorkingDir() {

        String tmp = workingDir.replace("\\", File.separator);
        File f = new File(tmp);

        if (!f.exists()) {
            f.mkdirs();
        }

        return tmp;
    }

}
