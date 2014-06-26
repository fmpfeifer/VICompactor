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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public abstract class PartitionScheme {
    protected final List<Partition> allPartitions;
    protected final List<Partition> allocatedPartitions;
    private final Volume volume;
    
    protected PartitionScheme(Volume volume) {
        this.volume = volume;
        allPartitions = new ArrayList<>();
        allocatedPartitions = new ArrayList<>();
    }
    
    /**
     * @return the volume
     */
    public Volume getVolume() {
        return volume;
    }
    
    
    public List<Partition> getAllocatedPartitions() {
        return allocatedPartitions;
    }

    public List<Partition> getAllPartitions() {
        return allPartitions;
    }
    
    protected void fillGaps() throws IOException {
        PartitionComparator comparator = new PartitionComparator();
        allPartitions.sort(comparator);
        allocatedPartitions.sort(comparator);
        List<Partition> toAdd = new ArrayList<>();
        int sectorSize = getVolume().getSectorSize();
        if (!allPartitions.isEmpty()) {
            Iterator<Partition> it = allPartitions.iterator();
            Partition last = it.next();
            while (it.hasNext()) {
                Partition p = it.next();
                if (last.getFirstSectorLBA() * sectorSize + last.getSize()
                        < p.getFirstSectorLBA() * sectorSize) {
                    Partition gap = new Partition(getVolume(),
                            last.getFirstSectorLBA() + last.getSize() / sectorSize,
                            p.getFirstSectorLBA() - last.getFirstSectorLBA() - last.getSize() / sectorSize,
                            0);
                    gap.setAllocatedPartition(false);
                    toAdd.add(gap);
                }
                last = p;
            }
            if (last.getFirstSectorLBA() * sectorSize + last.getSize() < getVolume().getVolumeData().length()) {
                Partition gap = new Partition(getVolume(),
                        last.getFirstSectorLBA() + last.getSize() / sectorSize,
                        getVolume().getVolumeData().length() / sectorSize - last.getFirstSectorLBA() - last.getSize() / sectorSize,
                        0);
                gap.setAllocatedPartition(false);
                toAdd.add(gap);
            }
            if (!toAdd.isEmpty()) {
                allPartitions.addAll(toAdd);
                allPartitions.sort(comparator);
            }
        }
    }
}
