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
import org.pfeifer.imageread.partiton.Partition;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class Ext4Volume {
    private final Partition partition;
    private final Ext4Superblock superBlock;
    private final BlockGroupDescriptors descriptors;
    private final Ext4Bitmap bitmap;
    
    public Ext4Volume(Partition partition) throws IOException {
        this.partition = partition;
        superBlock = new Ext4Superblock(this);
        descriptors = new BlockGroupDescriptors(this);
        bitmap = new Ext4Bitmap(this);
    }

    /**
     * @return the partition
     */
    public Partition getPartition() {
        return partition;
    }

    /**
     * @return the superBlock
     */
    public Ext4Superblock getSuperBlock() {
        return superBlock;
    }

    /**
     * @return the descriptors
     */
    public BlockGroupDescriptors getDescriptors() {
        return descriptors;
    }

    /**
     * @return the bitmap
     */
    public Ext4Bitmap getBitmap() {
        return bitmap;
    }
}
