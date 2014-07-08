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
import java.io.UnsupportedEncodingException;
import org.pfeifer.blockreader.BlockReader;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class MFTFileNameAttribute extends MFTRecordAttribute {

    private String fileName;
    private long parentDirectory;
    private int filenameFlags;
    private int fileNamespace;
    private long realSize;

    public MFTFileNameAttribute(NTFSVolume volume, BlockReader data) throws IOException {
        super(volume, data);
    }

    @Override
    protected void parseResidentAttr(BlockReader data) throws IOException {
        parentDirectory = data.getNumber(offsetToAttr + 0x00, 6); // only first 6 bytes are used, next 2 are fix up code

        realSize = data.getLong(offsetToAttr + 0x30);

        filenameFlags = data.getInt(offsetToAttr + 0x38);
        fileNamespace = data.getUnsignedByte(offsetToAttr + 0x41);
        int filenameLength = data.getUnsignedByte(offsetToAttr + 0x40);
        fileName = data.getString(offsetToAttr + 0x42, filenameLength * 2, "UTF-16LE");
        //TODO: parse dates and times
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isDirectory() {
        return (filenameFlags & 0x10000000) != 0;
    }

    public long getParentDirectory() {
        return parentDirectory;
    }

    /**
     * @return the fileNamespace
     */
    public int getFileNamespace() {
        return fileNamespace;
    }

    public long getRealSize() {
        return realSize;
    }
}
