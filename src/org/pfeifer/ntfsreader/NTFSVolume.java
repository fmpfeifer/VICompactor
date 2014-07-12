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
import org.pfeifer.imageread.partiton.Partition;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class NTFSVolume {

    private final Partition partition;
    private final BlockReader dataBlock;
    private final MFT mft;
    private int bytesPerSector;
    private int sectorsPerCluster;
    private int clusterSize;
    private long firstClusterOfMFT;
    private int mftRecordSize;
    private long volumeSerialNumber;
    private String sysId;
    private static final boolean debug = false;

    public NTFSVolume(Partition partition) throws IOException {
        this.partition = partition;
        this.dataBlock = partition.getPartitionData();
        parseBootRecord();
        mft = new MFT(this);
    }

    private void parseBootRecord() throws IOException {
        sysId = dataBlock.getString(3, 8, "UTF-8");

        if (!"NTFS    ".equals(sysId)) {
            throw new IOException("Invalid NTFS OEM ID.");
        }

        bytesPerSector = dataBlock.getShort(0x0b);
        sectorsPerCluster = dataBlock.getUnsignedByte(0x0d);
        clusterSize = bytesPerSector * sectorsPerCluster;

        firstClusterOfMFT = dataBlock.getLong(0x30);
        mftRecordSize = dataBlock.get(0x40); //only first byte - signed
        if (mftRecordSize < 0) {
            mftRecordSize = 1 << (-mftRecordSize);
        } else {
            mftRecordSize *= clusterSize;
        }

        volumeSerialNumber = dataBlock.getLong(0x48);

        if (debug) {
            System.out.println("SysId: " + sysId);
            System.out.println("BytesPerSector: " + bytesPerSector);
            System.out.println("SectorsPerCluster: " + sectorsPerCluster);
            System.out.println("ClusterSize: " + clusterSize);
            System.out.println("FirstClusterOfMFT: " + firstClusterOfMFT);
            System.out.println("mftRecordSize: " + mftRecordSize);
        }
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public BlockReader getDataBlock() {
        return dataBlock;
    }

    public long getFirstClusterOfMft() {
        return firstClusterOfMFT;
    }

    public int getMftRecordSize() {
        return mftRecordSize;
    }

    MFT getMFT() {
        return mft;
    }

    public int getBytesPerSector() {
        return bytesPerSector;
    }

    /**
     * @return the volumeSerialNumber
     */
    public long getVolumeSerialNumber() {
        return volumeSerialNumber;
    }

    /**
     * @return the partition
     */
    public Partition getPartition() {
        return partition;
    }
}
