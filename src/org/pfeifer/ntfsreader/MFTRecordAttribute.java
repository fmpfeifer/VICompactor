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
import org.pfeifer.blockreader.LimitBlockReader;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public abstract class MFTRecordAttribute {

    protected NTFSVolume volume;
    protected int attrType;
    protected int length;
    protected boolean resident;
    private String attrName = "";
    protected int offsetToName;
    protected short nameLength;
    protected short flags;
    protected short attrId;
    protected long attrLength;
    protected short offsetToAttr;
    protected boolean indexed;
    protected short offsetToDataRuns;
    protected BlockReader attributeData;
    protected long allocatedSize;

    protected MFTRecordAttribute(NTFSVolume volume, BlockReader data) throws IOException {
        this.volume = volume;
        parse(data);
    }

    private void parse(BlockReader data) throws IOException {
        parseStandardHeader(data);
    }

    private void parseStandardHeader(BlockReader data) throws IOException {
        attrType = data.getInt(0x00);
        length = data.getInt(0x04);
        resident = data.get(0x08) == 0;
        nameLength = data.getUnsignedByte(0x09);
        offsetToName = data.getShort(0x0a);
        flags = data.getShort(0x0c);
        attrId = data.getShort(0x0e);

        if (nameLength != 0) {
            attrName = data.getString(offsetToName, nameLength * 2, "UTF-16LE");
        }

        if (resident) {
            attrLength = data.getInt(0x10);
            offsetToAttr = data.getShort(0x14);
            parseResidentAttr(data);
        } else {

            parseNonResidentAttr(data);
        }
    }

    protected void parseResidentAttr(BlockReader data) throws IOException {
    }

    protected void parseNonResidentAttr(BlockReader data) throws IOException {
        offsetToDataRuns = data.getShort(0x20);
        attrLength = data.getLong(0x30);
        allocatedSize = data.getLong(0x28);

        attributeData = new LimitBlockReader(new DataRunsBlockReader(volume, data, offsetToDataRuns, isCompressed()), 0, allocatedSize);
    }

    public boolean isResident() {
        return resident;
    }

    /**
     * @return the attrName
     */
    public String getAttrName() {
        return attrName;
    }

    public long getAttrLength() {
        return attrLength;
    }

    public boolean isCompressed() {
        return (flags & 0x0001) != 0;
    }
}
