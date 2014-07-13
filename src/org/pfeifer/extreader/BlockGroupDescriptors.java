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
        long offsetBlockDesc = sb.getBlockSize() == 1024 ? 2 : 1;
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
            // TODO: code totally untested
            // Size of META_BG
            long meta_bg_size_bg = (sb.getBlockSize() / descSize); //number of groups in bg
            long meta_bg_size_bytes = meta_bg_size_bg * sb.getBlocksPerGroup() * sb.getBlockSize();
            long firstMetaBG = sb.getFirstMetaBg();
            for (long i = 0; i < totalGroups; i++) {
                if (i < firstMetaBG) {
                    descriptors.put(i, new BlockGroupDescriptor(i, r, offsetBlockDesc + i * descSize, descSize == 64));
                } else {
                    long meta_bg_nr = ( i - firstMetaBG ) / meta_bg_size_bg;
                    offsetBlockDesc = sb.getFirstMetaBg() * sb.getBlockGroupSizeBytes() + meta_bg_nr * meta_bg_size_bytes + sb.getBlockSize();
                    long off2 = (i - firstMetaBG ) % meta_bg_size_bg;
                    descriptors.put(i, new BlockGroupDescriptor(i, r, offsetBlockDesc + off2 * descSize, descSize == 64 ));
                }
            }
        }
    }
    
    public BlockGroupDescriptor getDescriptor(long n) {
        return descriptors.get(n);
    }
}
