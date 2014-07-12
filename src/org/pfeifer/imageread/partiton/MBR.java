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
public class MBR extends PartitionScheme {

    private int diskSignature;

    public MBR(Volume volume) throws IOException {
        super(volume);

        LimitBlockReader block = new LimitBlockReader(volume.getVolumeData(), 0, 512);
        parse(block);
    }

    public static boolean identify(Volume volume) throws IOException {
        int mbrSignature = volume.getVolumeData().getUnsignedShort(510);
        return mbrSignature == 0xAA55;
    }

    private void parse(BlockReader block) throws IOException {
        if (!identify(getVolume())) {
            throw new IOException("Wrong MBR Signature.");
        }
        Partition mbrPart = new Partition(getVolume(), 0, 1, 0);
        mbrPart.setAllocatedPartition(false);
        mbrPart.setProperty("type", "mbr");
        allPartitions.add(mbrPart);

        diskSignature = block.getInt(0x1b8);
        int offset;
        for (int i = 0; i < 4; i++) {
            offset = 0x1be + 16 * i;
            //status = block.getUnsignedByte(offset);
            //block.get(firstBlockCHS,offset+1);
            int partitionType = block.getUnsignedByte(offset + 0x04);
            //block.get(lastBlockCHS,offset+0x05);
            long firstSectorLBA = block.getInt(offset + 0x08) & 0xffffffff;
            long sectorCount = block.getInt(offset + 0x0C) & 0xffffffff;

            if (sectorCount != 0) {
                parsePartition(firstSectorLBA, sectorCount,
                        partitionType, i + 1);
            }

        }
        fillGaps();
    }

    private void parsePartition(long firstSectorLBA, long sectorCount, int partitionType, int number) throws IOException {
        if (partitionType == 0x0f) { //extended
            EBR extended = new EBR(getVolume(), firstSectorLBA);
            Partition ebrPartition = new Partition(getVolume(), firstSectorLBA, 1, number);
            ebrPartition.setAllocatedPartition(false);
            allPartitions.add(ebrPartition);
            int partNumber = 5;
            do {
                parsePartition(extended.getFirstSectorLBA(), extended.getSectorCount(),
                        extended.getPartitionType(), partNumber);
                extended = extended.getNextEBR();
                partNumber++;
            } while (extended != null);
        } else if (partitionType != 0) {
            Partition newPartition = new Partition(getVolume(), firstSectorLBA,
                    sectorCount, number);
            switch (partitionType) {
                case 0x07: // windows partition
                case 0x17:
                case 0x27:
                case 0x87:
                case 0xb7:
                case 0xc7:
                    newPartition.setProperty("windowsPartition","true");
                    break;
                case 0x83: // linux partition
                case 0x93:
                    newPartition.setProperty("linuxPartition","true");
                    break;
            }
            allocatedPartitions.add(newPartition);
            allPartitions.add(newPartition);
        }
    }

    /**
     * @return the diskSignature
     */
    public int getDiskSignature() {
        return diskSignature;
    }

    public static boolean identifyMBR(BlockReader reader) throws IOException {
        return true;
    }

}
