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
import org.pfeifer.ntfsreader.MFTBitmap;
import org.pfeifer.ntfsreader.NTFSVolume;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class NTFSAllocationBitmap implements AllocationBitmap {

    private final NTFSVolume volume;
    private final MFTBitmap bitmap;
    private final int clusterSize;

    public NTFSAllocationBitmap(NTFSVolume volume) throws IOException {
        this.volume = volume;
        this.bitmap = new MFTBitmap(volume);
        this.clusterSize = this.volume.getClusterSize();
    }

    @Override
    public boolean isAllocated(long pos, int size) {
        try {
            while (size > 0) {
                if (bitmap.isClusterInUse(pos / clusterSize)) {
                    return true;
                }
                if (pos % clusterSize == 0) {
                    pos += clusterSize;
                    size -= clusterSize;
                } else {
                    pos += pos % clusterSize;
                    size -= pos % clusterSize;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(NTFSAllocationBitmap.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    @Override
    public int getBitmapChunckSize() {
        return clusterSize;
    }
}
