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
import java.util.UUID;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class GPT extends PartitionScheme {

    private int gptRevision;
    private int gptHeaderSize;
    private int gptHeaderCRC;
    private long firstUsableLBA;
    private long lastUsableLBA;
    private UUID diskUUID;
    private long gptPartitionEntriesLBA;
    private int gptPartitionEntriesCount;
    private int gptPartitionEntrySize;
    private int gptPartitionArrayCRC;

    public GPT(Volume volume) throws IOException {
        super(volume);
        if (!identify(volume)) {
            throw new IOException("GPT not identified");
        }
        
        init();
    }
    
    private void init() throws IOException {
        Partition protectiveMBR = new Partition(getVolume(), 0, 1, 0);
        protectiveMBR.setAllocatedPartition(false);
        allPartitions.add(protectiveMBR);

        Partition gptHeader = new Partition(getVolume(), 1, 1, 0);
        gptHeader.setAllocatedPartition(false);
        allPartitions.add(gptHeader);

        parseGPTHeader(gptHeader.getPartitionData());

        Partition partitionArray = new Partition(getVolume(),
                gptPartitionEntriesLBA,
                gptPartitionEntriesCount * gptPartitionEntrySize / getVolume().getSectorSize(),
                0);
        partitionArray.setAllocatedPartition(false);
        allPartitions.add(partitionArray);

        parsePartitionArray(partitionArray.getPartitionData());

        fillGaps();
    }

    private void parseGPTHeader(BlockReader gptHeader) throws IOException {
        gptRevision = gptHeader.getInt(0x08);
        gptHeaderSize = gptHeader.getInt(0x0c);
        gptHeaderCRC = gptHeader.getInt(0x10);
        firstUsableLBA = gptHeader.getLong(0x28);
        lastUsableLBA = gptHeader.getLong(0x30);
        diskUUID = gptHeader.getUUID(0x38);
        gptPartitionEntriesLBA = gptHeader.getLong(0x48);
        gptPartitionEntriesCount = gptHeader.getInt(0x50);
        gptPartitionEntrySize = gptHeader.getInt(0x54);
        gptPartitionArrayCRC = gptHeader.getInt(0x58);
    }

    private void parsePartitionArray(BlockReader partitionArray) throws IOException {
        byte[] buffer = new byte[gptPartitionEntrySize - 0x38];
        for (int i = 0; i < gptPartitionEntriesCount; i++) {
            int offset = i * gptPartitionEntrySize;
            partitionArray.get(buffer, offset, 0, 16);
            if (!isAllZero(buffer, 16)) {
                UUID partitionTypeUUID = partitionArray.getUUID(offset);
                UUID uniquePartitionUUID = partitionArray.getUUID(offset + 0x10);
                long firstLBA = partitionArray.getLong(offset + 0x20);
                long lastLBA = partitionArray.getLong(offset + 0x28);
                long flags = partitionArray.getLong(offset + 0x30);
                partitionArray.get(buffer, offset + 0x38);
                String partitionName = new String(buffer, "UTF-16LE");
                Partition p = new Partition(getVolume(), firstLBA, lastLBA - firstLBA + 1, i);
                p.setProperty("name", partitionName);
                p.setProperty("partitionTypeUUID", partitionTypeUUID.toString());
                p.setProperty("uniquePartitionUUID", uniquePartitionUUID.toString());
                p.setProperty("flags", Long.toString(flags));
                switch (partitionTypeUUID.toString()) {
                    case "16e3c9e3-5c0b-b84d-817d-f92df00215ae":
                    case "a2a0d0eb-e5b9-3344-87c0-68b6b72699c7":
                        p.setProperty("windowsPartition", "true");
                        break;
                    case "af3dc60f-8384-7247-8e79-3d69d8477de4":
                        p.setProperty("linuxPartition", "true");
                        break;
                }
                allocatedPartitions.add(p);
                allPartitions.add(p);
            }
        }
    }

    private static boolean isAllZero(byte[] buffer, int count) {
        while (count > 0) {
            count--;
            if (buffer[count] != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean identify(Volume volume) throws IOException {
        /* TODO: improove partition scheme identification (protective mbr
         partition entry, GPT Header checksum, etc.
         */
        if (!MBR.identify(volume)) {
            return false;
        }
        String strSig = volume.getVolumeData().getString(volume.getSectorSize(), 8, "UTF-8");
        return strSig.equals("EFI PART");
    }

    /**
     * @return the gptRevision
     */
    public int getGptRevision() {
        return gptRevision;
    }

    /**
     * @return the gptHeaderSize
     */
    public int getGptHeaderSize() {
        return gptHeaderSize;
    }

    /**
     * @return the gptHeaderCRC
     */
    public int getGptHeaderCRC() {
        return gptHeaderCRC;
    }

    /**
     * @return the firstUsableLBA
     */
    public long getFirstUsableLBA() {
        return firstUsableLBA;
    }

    /**
     * @return the lastUsableLBA
     */
    public long getLastUsableLBA() {
        return lastUsableLBA;
    }

    /**
     * @return the diskUUID
     */
    public UUID getDiskUUID() {
        return diskUUID;
    }

    /**
     * @return the gptPartitionEntriesLBA
     */
    public long getGptPartitionEntriesLBA() {
        return gptPartitionEntriesLBA;
    }

    /**
     * @return the gptPartitionEntriesCount
     */
    public int getGptPartitionEntriesCount() {
        return gptPartitionEntriesCount;
    }

    /**
     * @return the gptPartitionEntrySize
     */
    public int getGptPartitionEntrySize() {
        return gptPartitionEntrySize;
    }

    /**
     * @return the gptPartitionArrayCRC
     */
    public int getGptPartitionArrayCRC() {
        return gptPartitionArrayCRC;
    }

}
