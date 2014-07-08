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
package org.pfeifer.extreader;

import java.io.IOException;
import java.util.UUID;
import org.pfeifer.blockreader.BlockReader;
import org.pfeifer.blockreader.LimitBlockReader;
import org.pfeifer.imageread.partiton.Volume;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class ExtSuperblock {

    private final BlockReader superBlockData;
    private final Volume volume;
    private long totalInodeCount;
    private long totalBlockCount;
    private long reservedBlockCount;
    private long freeBlockCount;
    private long freeInodeCount;
    private long firstDataBlock;
    private int blockSize;
    private int clusterSize;
    private long blocksPerGroup;
    private long inodesPerGroup;
    private long mountTime;
    private long writeTime;
    private int mountsSinceFsck;
    private int maxMountCountFsck;
    private int magic;
    private int state;
    private int errors;
    private int minorRevLevel;
    private long lastCheck;
    private long checkInterval;
    private long creatorOs;
    private int revisionLevel;
    private int defResUid;
    private int defResGid;
    private long firstInode = 0;
    private int inodeSize = 0;
    private int blockGroupNumber = 0;
    private long featureCompat = 0;
    private long featureIncompat = 0;
    private long featureRoCompat = 0;
    private UUID uuid = null;
    private String volumeName = "";
    private String lastMounted = "";
    private long algorithmUsageBitmap = 0;
    private short preallocBlocks = 0;
    private short preallocDirBlocks = 0;
    private short reservedGdtBlocks = 0;
    private UUID journalUuid = null;
    private long journalInode = 0;
    private long journalDev = 0;
    private long lastOrphan = 0;
    private int[] hashSeed = new int[4];
    private short defHashVersion = 0;
    private int descSize = 0;
    private long defaultMountOptions = 0;
    private long firstMetaBg = 0;
    private long mkfsTime = 0;
    private int minExtraSize = 0;
    private int wantExtraSize = 0;
    private long flags = 0;
    private long groupsPerFlex = 0;

    public ExtSuperblock(Volume vol) throws IOException {
        volume = vol;
        superBlockData = new LimitBlockReader(vol.getVolumeData(), 1024, 1024);
        parseSuperBlock();
    }

    private void parseSuperBlock() throws IOException {
        BlockReader sb = superBlockData;
        totalInodeCount = sb.getUnsignedInt(0x00);
        totalBlockCount = sb.getUnsignedInt(0x04);
        reservedBlockCount = sb.getUnsignedInt(0x08);
        freeBlockCount = sb.getUnsignedInt(0x0c);
        freeInodeCount = sb.getUnsignedInt(0x10);
        firstDataBlock = sb.getUnsignedInt(0x14);
        blockSize = 1 << (10 + sb.getUnsignedInt(0x18));
        clusterSize = 1 << sb.getUnsignedInt(0x1c);
        blocksPerGroup = sb.getUnsignedInt(0x20);
        inodesPerGroup = sb.getUnsignedInt(0x28);
        mountTime = sb.getUnsignedInt(0x2c);
        writeTime = sb.getUnsignedInt(0x30);
        mountsSinceFsck = sb.getUnsignedShort(0x34);
        maxMountCountFsck = sb.getUnsignedShort(0x36);
        magic = sb.getUnsignedShort(0x38);
        state = sb.getUnsignedShort(0x3a);
        errors = sb.getUnsignedShort(0x3c);
        minorRevLevel = sb.getUnsignedShort(0x3e);
        lastCheck = sb.getUnsignedInt(0x40);
        checkInterval = sb.getUnsignedInt(0x44);
        creatorOs = sb.getUnsignedInt(0x48);
        revisionLevel = sb.getInt(0x4c);
        defResUid = sb.getUnsignedShort(0x50);
        defResGid = sb.getUnsignedShort(0x52);
        if (revisionLevel >= 1) {
            firstInode = sb.getUnsignedInt(0x54);
            inodeSize = sb.getUnsignedShort(0x58);
        }
        blockGroupNumber = sb.getUnsignedShort(0x5a);
        featureCompat = sb.getUnsignedInt(0x5c);
        featureIncompat = sb.getUnsignedInt(0x60);
        featureRoCompat = sb.getUnsignedInt(0x64);
        uuid = sb.getUUID(0x68);
        

    }
}
