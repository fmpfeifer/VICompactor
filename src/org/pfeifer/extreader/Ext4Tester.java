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

import java.io.FileOutputStream;
import java.io.IOException;
import org.pfeifer.blockreader.BlockReader;
import org.pfeifer.blockreader.BufferedBlockReader;
import org.pfeifer.blockreader.FileBlockReader;
import org.pfeifer.blockreader.VDIBlockReader;
import org.pfeifer.imageread.partiton.Partition;
import org.pfeifer.imageread.partiton.Volume;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class Ext4Tester {

    public static void main(String[] args) throws IOException {
        VDIBlockReader reader = new VDIBlockReader(new BufferedBlockReader(new FileBlockReader("D:/VBox/UbuntuVM.vdi"), 2 * 1024 * 1024));
        testVDI(reader);
        //dumpData(reader, 1024*1024*10, "D:/ext4.raw");
    }
    
    public static void dumpData(BlockReader reader, long size, String out) throws IOException { 
        FileOutputStream fout = new FileOutputStream(out);
        byte [] buffer = new byte[32*1024];
        long n = 0;
        while (n < size) {
            n += reader.get(buffer, n);
            fout.write(buffer);
        }
        fout.close();
    }

    public static void testVDI(VDIBlockReader reader) throws IOException {
        Volume vol = new Volume(reader);
        for (Partition p : vol.getPartitionScheme().getAllocatedPartitions()) {
            try {
                Ext4Volume evol = new Ext4Volume(p);
                Ext4Superblock sb = evol.getSuperBlock();
                
                System.out.println("Partition start: " + p.getFirstSectorLBA());

                System.out.println("BlockSize: " + sb.getBlockSize());
                System.out.println("ClusterSize: " + sb.getClusterSize());
                System.out.println("InodeSize: " + sb.getInodeSize());
                System.out.println("MaxMountCountFSCK: " + sb.getMaxMountCountFsck());
                System.out.println("MountCountFSCK: " + sb.getMaxMountCountFsck());
                System.out.println("Volume Name: " + sb.getVolumeName());
                System.out.println("Last Mounted: " + sb.getLastMounted());
                System.out.println("");
            } catch (IOException e) {
            }
        }
    }
}
