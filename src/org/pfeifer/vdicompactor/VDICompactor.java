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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.pfeifer.blockreader.BlockReader;
import org.pfeifer.blockreader.BufferedBlockReader;
import org.pfeifer.blockreader.ConcatenatedBlockReader;
import org.pfeifer.blockreader.FileBlockReader;
import org.pfeifer.blockreader.VDIBlockReader;
import org.pfeifer.ntfsreader.NTFSVolume;
import org.pfeifer.imageread.partiton.Partition;
import org.pfeifer.imageread.partiton.Volume;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class VDICompactor {

    private final Set<CompactProgressListener> progressListeners = new LinkedHashSet<>();

    /**
     * 
     * @param file
     * @throws IOException 
     */
    public void compactVDI(String file) throws IOException {
        int i = file.lastIndexOf('.');
        String origFile = null;
        if (i > 0 && i < file.length() - 1) {
            String ext = file.substring(i + 1);
            String base = file.substring(0, i);
            origFile = base + "_orig." + ext;
            //Rename
            File f1 = new File(file);
            File f2 = new File(origFile);
            if (!f1.renameTo(f2)) {
                throw new IOException("Error renaming file...");
            }
        }
        if (origFile != null) {
            BlockReader reader = new BufferedBlockReader(new FileBlockReader(origFile), 1024*1024*2);
            VDIBlockReader vdiReader = new VDIBlockReader(reader);
            compactVDI(vdiReader, file);
        }
    }

    private void compactVDI(VDIBlockReader vdiReader, String destFile) throws IOException {
        Volume volume = new Volume(vdiReader);
        List<BlockReader> readers = new ArrayList<>();
        for (Partition p : volume.getPartitionScheme().getAllPartitions()) {
            BlockReader r;
            if (p.isAllocatedPartition()) {
                String windowsPart = p.getProperty("windowsPartition");

                if (windowsPart != null && windowsPart.equals("true")) {
                    try {
                        NTFSVolume ntfs = new NTFSVolume(p.getPartitionData());
                        NTFSAllocationBitmap bitmap = new NTFSAllocationBitmap(ntfs);
                        r = new AllocationAwareBlockReader(p.getPartitionData(), bitmap);
                    } catch (IOException e) {
                        r = p.getPartitionData();
                    }
                } else {
                    r = p.getPartitionData();
                }
            } else {
                r = p.getPartitionData();
            }
            readers.add(r);
        }
        ConcatenatedBlockReader vdiData = new ConcatenatedBlockReader(readers);
        VDIWriter writer = new VDIWriter(vdiReader, new File(destFile));

        long pos = 0L;
        int chunkSize;
        byte[] buffer = new byte[vdiReader.getBlockSize()];
        CompactProgressEvent event = new CompactProgressEvent(this, vdiData.length(), 0);
        fireProgressEvent(event);
        long length = vdiData.length();
        while (pos < length) {
            chunkSize = (int) Math.min(vdiReader.getBlockSize(), length - pos);
            if (vdiData.blockIsUnallocated(pos, chunkSize)) {
                writer.writeFree(pos, chunkSize);
                pos += chunkSize;
            } else if (vdiData.blockIsZero(pos, chunkSize)) {
                writer.writeZero(pos, chunkSize);
                pos += chunkSize;
            } else {
                int read = vdiData.get(buffer, pos);
                writer.write(buffer, pos, read);
                pos += read;
            }
            event.setCompleted((int)(pos * 1000L / length));
            fireProgressEvent(event);
        }

        writer.close();
    }

    private void fireProgressEvent(CompactProgressEvent event) {
        for (CompactProgressListener listener : progressListeners) {
            listener.onProgress(event);
        }
    }

    public void addCompactProgressListener(CompactProgressListener listener) {
        progressListeners.add(listener);
    }

    public void removeCompactProgressListener(CompactProgressListener listener) {
        progressListeners.remove(listener);
    }

}
