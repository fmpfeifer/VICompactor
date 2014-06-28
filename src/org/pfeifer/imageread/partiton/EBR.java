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
package org.pfeifer.imageread.partiton;

import java.io.IOException;
import org.pfeifer.blockreader.BlockReader;
import org.pfeifer.blockreader.LimitBlockReader;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class EBR {

    private final Volume volume;
    private final long sectorOffset;
    private long firstSectorLBA;
    private long sectorCount;
    private int partitionType;
    private int status;
    private long nextEBRSectorOffset;

    public EBR(Volume volume, long sectorOffset) throws IOException {
        this.volume = volume;
        this.sectorOffset = sectorOffset;
        LimitBlockReader block = new LimitBlockReader(volume.getVolumeData(), 
                sectorOffset * volume.getSectorSize(), volume.getSectorSize());
        parse(block);
    }

    private void parse(BlockReader block) throws IOException {
        // read first entry
        status = block.getUnsignedByte(0x1be);
        partitionType = block.getUnsignedByte(0x1be + 0x04);
        firstSectorLBA = sectorOffset + (block.getInt(0x1be + 0x08) & 0xffffffff);
        sectorCount = block.getInt(0x1be + 0x0c) & 0xffffffff;

        //read second entry
        nextEBRSectorOffset = block.getInt(0x1ce + 0x08) & 0xffffffff;
    }

    /**
     * @return the volume
     */
    public Volume getVolume() {
        return volume;
    }

    public long getNextEBRSectorOffset() {
        long resp = 0;

        if (nextEBRSectorOffset > 0) {
            resp = nextEBRSectorOffset + sectorOffset;
        }

        return resp;
    }

    public EBR getNextEBR() throws IOException {
        EBR next = null;

        if (nextEBRSectorOffset > 0) {
            next = new EBR(volume, getNextEBRSectorOffset());
        }

        return next;
    }

    public int getPartitionType() {
        return partitionType;
    }

    public long getSectorCount() {
        return sectorCount;
    }

    public long getFirstSectorLBA() {
        return firstSectorLBA;
    }

    public int getStatus() {
        return status;
    }
}
