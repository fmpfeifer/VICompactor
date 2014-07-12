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

package org.pfeifer.vdicompactor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pfeifer.extreader.Ext4Bitmap;
import org.pfeifer.extreader.Ext4Volume;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class ExtAllocationBitmap implements AllocationBitmap {

    private final Ext4Volume volume;
    private final Ext4Bitmap bitmap;
    private final int blockSize;
    
    public ExtAllocationBitmap(Ext4Volume volume) {
        this.volume = volume;
        this.bitmap = volume.getBitmap();
        this.blockSize = volume.getSuperBlock().getBlockSize();
    }
    
    @Override
    public boolean isAllocated(long pos, int size) {
        try {
            while (size > 0) {
                if (bitmap.isBlockInUse(pos / blockSize)) {
                    return true;
                }
                if (pos % blockSize == 0) {
                    pos += blockSize;
                    size -= blockSize;
                } else {
                    pos += pos % blockSize;
                    size -= pos % blockSize;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ExtAllocationBitmap.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    @Override
    public int getBitmapChunckSize() {
        return blockSize;
    }

}
