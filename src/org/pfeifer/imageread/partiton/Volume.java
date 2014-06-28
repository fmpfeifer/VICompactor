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

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class Volume {

    private final BlockReader volumeData;
    private int partitionCount;
    private PartitionScheme partitionScheme;
    private final int sectorSize;

    public Volume(BlockReader volumeData) throws IOException {
        this(volumeData, 512);
    }
    
    public Volume(BlockReader volumeData, int sectorSize) throws IOException {
        this.volumeData = volumeData;
        this.sectorSize = sectorSize;
        init();
    }

    private void init() throws IOException {
        if (GPT.identify(this)) {
            partitionScheme = new GPT(this);
        } else if (MBR.identify(this)) {
            partitionScheme = new MBR(this);
        }
        partitionCount = partitionScheme.getAllocatedPartitions().size();
    }

    public BlockReader getVolumeData() {
        return volumeData;
    }

    /**
     * @return the partitionCount
     */
    public int getPartitionCount() {
        return partitionCount;
    }

    /**
     * @return the mbr
     */
    public PartitionScheme getPartitionScheme() {
        return partitionScheme;
    }

    /**
     * @return the sectorSize
     */
    public int getSectorSize() {
        return sectorSize;
    }
}
