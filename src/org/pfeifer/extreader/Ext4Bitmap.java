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
import java.util.Arrays;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class Ext4Bitmap {

    private final Ext4Volume volume;
    private long blockGroupInUse = -1;
    private final byte[] blockGroupBitmap;
    private final int blocksPerGroup;

    public Ext4Bitmap(Ext4Volume volume) throws IOException {
        this.volume = volume;
        blockGroupBitmap = new byte[volume.getSuperBlock().getBlockSize()];
        blocksPerGroup = (int) volume.getSuperBlock().getBlocksPerGroup();
    }

    public boolean isBlockInUse(long blockNumber) throws IOException {
        adjustBitmap(blockNumber);
        int blockInBG = (int) (blockNumber % blocksPerGroup);
        return ((blockGroupBitmap[blockInBG >> 3] >> (blockInBG & 7)) & 1) == 1;
    }

    private void adjustBitmap(long block) throws IOException {
        long group = block / volume.getSuperBlock().getBlocksPerGroup();
        if (group != blockGroupInUse) {
            blockGroupInUse = group;
            fillBlockGroupBitmap();
        }
    }

    private void fillBlockGroupBitmap() throws IOException {
        if (blockGroupInUse != -1) {
            if (volume.getDescriptors().getDescriptor(blockGroupInUse).isBlockBitmapInitialized()) {
                volume.getPartition().getPartitionData().get(blockGroupBitmap,
                        volume.getDescriptors().getDescriptor(blockGroupInUse).getBlockBitmapLocation()
                        * volume.getSuperBlock().getBlockSize());
            } else {
                Arrays.fill(blockGroupBitmap, (byte) 0);
            }
        }
    }
}
