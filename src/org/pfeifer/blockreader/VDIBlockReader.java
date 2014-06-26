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
package org.pfeifer.blockreader;

import java.io.IOException;
import java.util.UUID;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class VDIBlockReader extends BlockReader {

    private static final int VDI_IMAGE_SIGNATURE = 0xbeda107f;
    private final BlockReader rawData;
    private int version;
    private int signature;
    private int imageType;
    private int offsetBlocks;
    private int offsetData;
    private int sectorSize;
    private long diskSize;
    private int blockSize;
    private int blockExtra;
    private int blocksInHDD;
    private int blocksAllocated;
    private UUID uuidThisVDI;
    private UUID uuidLastSnap;
    private UUID uuidLink;
    private UUID uuidParent;
    private int[] indexMap;
    private ZeroBlockReader zeroPool;
    private int shiftOffsetToIndex;
    private int blockMask;
    private BlockReader parent;

    public VDIBlockReader(BlockReader rawData) throws IOException {
        this(rawData, null);
    }

    public VDIBlockReader(BlockReader rawData, VDIBlockReader parent) throws IOException {
        this.rawData = rawData;
        this.parent = parent;
        parseHeaders();
    }

    public VDIBlockReader(String file, VDIBlockReader parent) throws IOException {
        this(new BufferedBlockReader(new FileBlockReader(file)), parent);
        //this(new FileBlockReader(file), parent);
    }

    public VDIBlockReader(String file) throws IOException {
        this(new BufferedBlockReader(new FileBlockReader(file)));
        //this(new FileBlockReader(file));
    }

    @Override
    public byte get(long pos) throws IOException {
        int fblock = indexMap[(int) (pos >> shiftOffsetToIndex)];
        if (fblock == -2) { // se -1: não alocado, pode retornar qquer coisa. se -2: zero: tem que retornar zero
            return 0;
        } else if (fblock == -1) {
            return parent.get(pos);
        }
        long fpos = offsetData + (((long) fblock) * getBlockSize()) + (pos & blockMask) + blockExtra;

        return rawData.get(fpos);
    }

    @Override
    public int get(byte[] resp, long pos, int destPos, int length) throws IOException {
        int read = 0;

        if (pos + length > length()) {
            length = (int) (length() - pos);
        }

        int x = destPos;
        int l;
        int q = length;
        int fblock;
        BlockReader source;

        while (q > 0) {
            l = Math.min((int) (getBlockSize() - (pos & blockMask)), q);
            fblock = indexMap[(int) (pos >> shiftOffsetToIndex)];

            //source = fblock  ? zeroPool : rawData;
            if (fblock == -2) {
                source = zeroPool;
            } else if (fblock == -1) {
                source = parent;
            } else {
                source = rawData;
            }

            if (fblock < 0) { // pega do parent ou zero
                read += source.get(resp, pos, destPos + read, l);
            } else {
                long fpos = offsetData + (((long) fblock) * getBlockSize())
                        + (pos & blockMask) + blockExtra;
                read += source.get(resp, fpos, x, l);
            }

            x += l;
            pos += l;
            q -= l;
        }

        return read;
    }

    @Override
    public long length() throws IOException {
        return getDiskSize();
    }

    @Override
    public void close() throws IOException {
        rawData.close();
    }

    /**
     * Parse parametes of VDI from image header References: -
     * http://forums.virtualbox.org/viewtopic.php?t=8046 -
     * https://www.virtualbox.org/browser/vbox/trunk/src/VBox/Storage
     *
     * @throws IOException
     */
    private void parseHeaders() throws IOException {
        // check signature, etc..
        signature = rawData.getInt(0x40);
        if (signature != VDI_IMAGE_SIGNATURE) {
            throw new IOException("VDI signature doesn't match!");
        }

        version = rawData.getInt(0x44);
        if (version >> 16 == 1) { //major version 1
            // read data
            imageType = rawData.getInt(0x40 + 12);
            offsetBlocks = rawData.getInt(0x150 + 4);
            offsetData = rawData.getInt(0x150 + 8);
            sectorSize = rawData.getInt(0x160 + 8);
            diskSize = rawData.getLong(0x170);
            blockSize = rawData.getInt(0x170 + 8);
            blockExtra = rawData.getInt(0x170 + 12);
            blocksInHDD = rawData.getInt(0x180);
            blocksAllocated = rawData.getInt(0x180 + 4);
            uuidThisVDI = rawData.getUUID(0x188);
            uuidLastSnap = rawData.getUUID(0x198);
            uuidLink = rawData.getUUID(0x1a8);
            uuidParent = rawData.getUUID(0x1b8);
        } else if (version >> 16 == 0) { // major version 0 - nao foi testado
            imageType = rawData.getInt(0x40 + 4);

            sectorSize = rawData.getInt(0x15C - 4);
            diskSize = rawData.getLong(0x160);
            blockSize = rawData.getInt(0x160 + 8);
            blockExtra = 0;
            blocksInHDD = rawData.getInt(0x16C);
            blocksAllocated = rawData.getInt(0x170);
            uuidThisVDI = rawData.getUUID(0x174);
            uuidLastSnap = rawData.getUUID(0x184);
            uuidLink = rawData.getUUID(0x194);
            uuidParent = new UUID(0, 0);

            //calculate from parameters
            offsetBlocks = 512;
            offsetData = 512 + blocksInHDD * 4;
        } else {
            throw new IOException("VDI Version not supported");
        }

        blockMask = getBlockSize() - 1;
        shiftOffsetToIndex = getPowerOfTwo(getBlockSize());
        zeroPool = new ZeroBlockReader(getDiskSize());

        if (parent == null) {
            parent = zeroPool;
        }

        indexMap = new int[blocksInHDD];
        for (int i = 0; i < blocksInHDD; i++) {
            indexMap[i] = rawData.getInt(offsetBlocks + 4 * i);
        }
    }

    /**
     * translated from VDIHDDCore.cpp -
     * http://www.virtualbox.org/browser/trunk/src/VBox/Devices/Storage/VDIHDDCore.cpp
     *
     * @param number
     * @return log2(number)
     */
    private static int getPowerOfTwo(long number) {
        if (number == 0) {
            return 0;
        }
        int power2 = 0;
        while ((number & 1) == 0) {
            number >>= 1;
            power2++;
        }

        return number == 1 ? power2 : 0;
    }

    /**
     * @return the blockSize
     */
    public int getBlockSize() {
        return blockSize;
    }

    /**
     * @return the diskSize
     */
    public long getDiskSize() {
        return diskSize;
    }

    @Override
    public boolean blockIsZero(long pos, int length) throws IOException {
        int l;
        int q = length;
        int fblock;
        BlockReader source;

        while (q > 0) {
            l = Math.min((int) (getBlockSize() - (pos & blockMask)), q);
            fblock = indexMap[(int) (pos >> shiftOffsetToIndex)];

            //source = fblock  ? zeroPool : rawData;
            if (fblock == -2) {
                source = zeroPool;
            } else if (fblock == -1) {
                source = parent;
            } else {
                source = rawData;
            }

            if (fblock < 0) { // pega do parent ou zero
                if (!source.blockIsZero(pos, l)) {
                    return false;
                }
            } else {
                long fpos = offsetData + (((long) fblock) * getBlockSize())
                        + (pos & blockMask) + blockExtra;
                if (!source.blockIsZero(fpos, l)) {
                    return false;
                }
            }
            pos += l;
            q -= l;
        }

        return true;
    }
    
    @Override
    public boolean blockIsUnallocated(long pos, int length) throws IOException {
        int l;
        int q = length;
        int fblock;

        while (q > 0) {
            l = Math.min((int) (getBlockSize() - (pos & blockMask)), q);
            fblock = indexMap[(int) (pos >> shiftOffsetToIndex)];

            //source = fblock  ? zeroPool : rawData;
            if (fblock != -1) {
                return false;
            }

            pos += l;
            q -= l;
        }

        return true;
    }

    /**
     * @return the rawData
     */
    public BlockReader getRawData() {
        return rawData;
    }

    /**
     * @return the offsetBlocks
     */
    public int getOffsetBlocks() {
        return offsetBlocks;
    }

    /**
     * @return the offsetData
     */
    public int getOffsetData() {
        return offsetData;
    }

    /**
     * @return the sectorSize
     */
    public int getSectorSize() {
        return sectorSize;
    }

    /**
     * @return the blocksInHDD
     */
    public int getBlocksInHDD() {
        return blocksInHDD;
    }

    /**
     * @return the blocksAllocated
     */
    public int getBlocksAllocated() {
        return blocksAllocated;
    }

    /**
     * @return the parent
     */
    public BlockReader getParent() {
        return parent;
    }

    /**
     * @return the uuidParent
     */
    public UUID getUuidParent() {
        return uuidParent;
    }
}
