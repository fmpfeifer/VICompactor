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
import java.util.HashMap;
import java.util.Map;
import org.pfeifer.blockreader.BlockReader;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class BlockGroupDescriptors {

    private final Ext4Volume volume;
    private final Map<Long, BlockGroupDescriptor> descriptors;

    public BlockGroupDescriptors(Ext4Volume volume) throws IOException {
        this.volume = volume;
        descriptors = new HashMap<>();
        init();
    }

    private void init() throws IOException {
        Ext4Superblock sb = volume.getSuperBlock();
        int offsetBlockDesc = sb.getBlockSize() == 1024 ? 2 : 1;
        offsetBlockDesc *= sb.getBlockSize();
        int descSize = 32;
        if (sb.is64bits()) {
            descSize = 64; //use padding
        }
        long totalGroups = sb.getTotalBlockCount() / sb.getBlocksPerGroup();
        BlockReader r = volume.getPartition().getPartitionData();

        if (!sb.isMetaBG()) {
            for (long i = 0; i < totalGroups; i++) {
                descriptors.put(i, new BlockGroupDescriptor(i, r, offsetBlockDesc + i * descSize, descSize == 64));
            }
        } else {
            // FIXME: this code is wrong !!!!
            // Size of META_BG
            long meta_bg_size_bg = (sb.getBlockSize() / descSize);
            long meta_bg_size_blocks = meta_bg_size_bg * sb.getBlocksPerGroup();
            int total_meta_bg = (int) (sb.getTotalBlockCount() / meta_bg_size_blocks);
            if (total_meta_bg * meta_bg_size_blocks < sb.getTotalBlockCount()) {
                total_meta_bg += 1;
            }
            long i = 0;
            for (int mbg = 0; mbg < total_meta_bg; mbg++) {
                for (int j = 0; j < meta_bg_size_bg; j++) {
                    if ( j == 1 && sb.getBlockSize() == 1024) {
                        offsetBlockDesc -= 1024;
                    }
                    long offsetMG = j * meta_bg_size_blocks * sb.getBlockSize();
                    descriptors.put(i, new BlockGroupDescriptor(i, r, offsetMG + offsetBlockDesc + j * descSize, descSize == 64));
                    i++;
                }
            }
        }
    }

    public BlockGroupDescriptor getDescriptor(long n) {
        return descriptors.get(n);
    }
}
