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

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class MFTDataAttribute extends MFTRecordAttribute {

    private static final ZeroBlockReader zeroSizeFileData = new ZeroBlockReader(0);

    public MFTDataAttribute(NTFSVolume volume, BlockReader data) throws IOException {
        super(volume, data);
    }

    public BlockReader getFileData() {
        return attributeData;
    }

    @Override
    protected void parseResidentAttr(BlockReader data) throws IOException {
        if (attrLength > 0) {
            attributeData = new LimitBlockReader(data, offsetToAttr, attrLength);
        } else {
            attributeData = zeroSizeFileData;
        }
    }
}