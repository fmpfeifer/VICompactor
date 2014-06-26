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
package org.pfeifer.ntfsreader;

import java.io.IOException;
import org.pfeifer.blockreader.BlockReader;
import org.pfeifer.blockreader.LimitBlockReader;
import org.pfeifer.blockreader.ZeroBlockReader;
import org.pfeifer.vfs.VFSAttributes;
import org.pfeifer.vfs.VFSObject;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class MFTAlternateDataStream implements VFSObject {

    private final MFTFile parent;
    private final MFTDataAttribute dataAttr;

    public MFTAlternateDataStream(MFTFile parent, MFTDataAttribute attr) {
        this.parent = parent;
        this.dataAttr = attr;
    }

    public VFSObject getParent() {
        return parent;
    }

    public long getSize() throws IOException {
        return dataAttr.getAttrLength();
    }

    public String getFileName() {
        return dataAttr.getAttrName();
    }

    public boolean isDirectory() {
        return false;
    }

    public boolean hasSubItems() {
        return false;
    }

    public VFSObject getSubitem(int index) {
        return null;
    }

    public int getSubitemCount() {
        return 0;
    }

    public BlockReader getTotalFileData() throws IOException {
        BlockReader fileData = null;

        if (dataAttr != null) {
            fileData = dataAttr.getFileData();
        }

        return fileData;
    }

    public BlockReader getFileSlack() throws IOException {
        BlockReader totalFileData = getTotalFileData();
        long size = getSize();
        BlockReader slack;
        if (totalFileData.length() > size) {
            slack = new LimitBlockReader(totalFileData, size, totalFileData.length() - size);
        } else {
            slack = new ZeroBlockReader(0);
        }
        return slack;
    }

    public BlockReader getFileData() throws IOException {
        BlockReader totalFileData = getTotalFileData();
        long size = getSize();
        return new LimitBlockReader(totalFileData, 0, size);
    }

    public VFSAttributes getAttributes() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
