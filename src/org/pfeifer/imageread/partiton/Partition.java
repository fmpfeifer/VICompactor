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

import java.util.HashMap;
import java.util.Map;
import org.pfeifer.blockreader.BlockReader;
import org.pfeifer.blockreader.LimitBlockReader;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class Partition {

    private final BlockReader partitionData;
    private final int partitionNumber;
    private final long firstSectorLBA;
    private final long size;
    private final Volume volume;
    private boolean allocatedPartition;
    private final Map<String, String> properties;

    public Partition(Volume volume, long firstSectorLBA, long sectorCount, int number) {
        partitionData = new LimitBlockReader(volume.getVolumeData(), 
                firstSectorLBA * volume.getSectorSize(),
                sectorCount * volume.getSectorSize());
        partitionNumber = number;
        this.firstSectorLBA = firstSectorLBA;
        this.size = sectorCount * volume.getSectorSize();
        this.volume = volume;
        allocatedPartition = true;
        properties = new HashMap<>();
    }

    /**
     * @return the partitionData
     */
    public BlockReader getPartitionData() {
        return partitionData;
    }

    /**
     * @return the partitionNumber
     */
    public int getPartitionNumber() {
        return partitionNumber;
    }

    @Override
    public String toString() {
        return "part_" + getPartitionNumber();
    }

    /**
     * @return the firstSectorLBA
     */
    public long getFirstSectorLBA() {
        return firstSectorLBA;
    }

    /**
     * @return the size
     */
    public long getSize() {
        return size;
    }

    /**
     * @return the volume
     */
    public Volume getVolume() {
        return volume;
    }

    /**
     * @return the allocatedPartition
     */
    public boolean isAllocatedPartition() {
        return allocatedPartition;
    }

    /**
     * @param allocatedPartition the allocatedPartition to set
     */
    public void setAllocatedPartition(boolean allocatedPartition) {
        this.allocatedPartition = allocatedPartition;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
}
