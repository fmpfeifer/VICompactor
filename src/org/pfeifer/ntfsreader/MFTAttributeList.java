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
import java.util.HashSet;
import java.util.Set;
import org.pfeifer.blockreader.BlockReader;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class MFTAttributeList extends MFTDataAttribute {

    public MFTAttributeList(NTFSVolume volume, BlockReader attribute) throws IOException {
        super(volume,attribute);
    }

    public Long[] getChildRecords(long baseRecord) throws IOException {
        Set<Long> child = new HashSet<Long>(1);

        int pos = 0;

        while (pos < attributeData.length() ) {
            int thisAttrType = attributeData.getInt(pos);
            int tam = attributeData.getShort(pos + 0x04);
            long baseFileRef = attributeData.getNumber(pos + 0x10, 6);

            pos += tam;
            if ( thisAttrType <= 0 || tam == 0 ) {
                break;
            }
            if ( baseFileRef != baseRecord && thisAttrType != 0x20 ) {
                child.add(baseFileRef);
            }
            
        }

        return child.toArray(new Long[child.size()]);
    }
}
