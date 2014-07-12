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
import org.pfeifer.blockreader.BlockReader;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class BlockGroupDescriptor {
    private final long blockGroupNumber;
    private long blockBitmapLocation;
    private long inodeBitmapLocation;
    private long inodeTableLocation;
    private int freeBlockCount;
    private int freeInodeCount;
    private int usedDirCount;
    private final int flags;

    BlockGroupDescriptor(long number, BlockReader r, long offset, boolean is64) throws IOException {
        this.blockGroupNumber = number;
        blockBitmapLocation = r.getUnsignedInt(offset + 0x00);
        inodeBitmapLocation = r.getUnsignedInt(offset + 0x04);
        inodeTableLocation = r.getUnsignedInt(offset + 0x08);
        freeBlockCount = r.getUnsignedShort(offset + 0x0c);
        freeInodeCount = r.getUnsignedShort(offset + 0x0e);
        usedDirCount = r.getUnsignedShort(offset + 0x10);
        flags = r.getUnsignedShort(offset + 0x12);
        if ( is64 ) {
            blockBitmapLocation |= r.getUnsignedInt(offset + 0x20) << 32;
            inodeBitmapLocation |= r.getUnsignedInt(offset + 0x24) << 32;
            inodeTableLocation |= r.getUnsignedInt(offset + 0x28) << 32;
            freeBlockCount |= r.getUnsignedShort(offset + 0x2c) << 16;
            freeInodeCount |= r.getUnsignedShort(offset + 0x2e) << 16;
            usedDirCount |= r.getUnsignedShort(offset + 0x30) << 16;
        }
    }
    
    
    public boolean isInodeTableInitialized() {
        return ( flags & 0x1 ) == 0;
    }
    
    public boolean isBlockBitmapInitialized() {
        return ( flags & 0x2 ) == 0;
    }
    
    public boolean isInodeTableZeroed() {
        return ( flags & 0x4 ) == 0;
    }

    /**
     * @return the blockGroupNumber
     */
    public long getBlockGroupNumber() {
        return blockGroupNumber;
    }

    /**
     * @return the blockBitmapLocation
     */
    public long getBlockBitmapLocation() {
        return blockBitmapLocation;
    }

    /**
     * @return the inodeBitmapLocation
     */
    public long getInodeBitmapLocation() {
        return inodeBitmapLocation;
    }

    /**
     * @return the inodeTableLocation
     */
    public long getInodeTableLocation() {
        return inodeTableLocation;
    }

    /**
     * @return the freeBlockCount
     */
    public int getFreeBlockCount() {
        return freeBlockCount;
    }

    /**
     * @return the freeInodeCount
     */
    public int getFreeInodeCount() {
        return freeInodeCount;
    }

    /**
     * @return the usedDirCount
     */
    public int getUsedDirCount() {
        return usedDirCount;
    }

    /**
     * @return the flags
     */
    public int getFlags() {
        return flags;
    }
}
