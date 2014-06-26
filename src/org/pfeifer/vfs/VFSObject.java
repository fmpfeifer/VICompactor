/*
Copyright 2014-2014
Fabio Melo Pfeifer

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package org.pfeifer.vfs;

import java.io.IOException;
import org.pfeifer.blockreader.BlockReader;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public interface VFSObject {

    VFSObject getParent();

    long getSize() throws IOException;

    String getFileName() throws IOException;

    boolean isDirectory() throws IOException;

    boolean hasSubItems() throws IOException;

    VFSObject getSubitem(int index) throws IOException;

    int getSubitemCount() throws IOException;

    BlockReader getTotalFileData() throws IOException;

    BlockReader getFileSlack() throws IOException;

    BlockReader getFileData() throws IOException;

    VFSAttributes getAttributes() throws IOException;
}
