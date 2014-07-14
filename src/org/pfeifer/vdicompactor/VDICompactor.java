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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.pfeifer.blockreader.BlockReader;
import org.pfeifer.blockreader.BufferedBlockReader;
import org.pfeifer.blockreader.ConcatenatedBlockReader;
import org.pfeifer.blockreader.FileBlockReader;
import org.pfeifer.blockreader.VDIBlockReader;
import org.pfeifer.extreader.Ext4Volume;
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
     * @param directory
     * @param searchDir
     * @throws IOException
     */
    public void compactVDI(String file, String directory, boolean searchDir) throws IOException {
        VDIBlockReader vdi1 = new VDIBlockReader(new BufferedBlockReader(new FileBlockReader(file)));
        VDIBlockReader parent = null;
        Map<UUID, VDINode> vdis = new HashMap<>();
        if (vdi1.getUuidParent().getLeastSignificantBits() != 0
                || vdi1.getUuidParent().getMostSignificantBits() != 0) {
            File dir = new File(directory);
            searchVDI(dir, vdis);
            if (searchDir) {
                File f = new File(file);
                dir = f.getParentFile();
                searchVDI(dir, vdis);
            }
            parent = getParent(vdi1.getUuidParent(), vdis);
            if (parent == null) {
                throw new IOException("Parent VDI not found.");
            }
        }
        vdi1.close();
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
            BlockReader reader = new BufferedBlockReader(new FileBlockReader(origFile));
            VDIBlockReader vdiReader = new VDIBlockReader(reader);
            if (parent != null) {
                vdiReader.setParent(parent);
            }
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
                String linuxPart = p.getProperty("linuxPartition");

                if (windowsPart != null && windowsPart.equals("true")) {
                    try {
                        r = createNTFSPartition(p);
                    } catch (IOException e) {
                        try {
                            r = createExt4Partition(p);
                        } catch (IOException ex) {
                            r = p.getPartitionData();
                        }
                    }
                } else if (linuxPart != null && linuxPart.equals("true")) {
                    try {
                        r = createExt4Partition(p);
                    } catch (IOException ex) {
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
            event.setCompleted((int) (pos * 1000L / length));
            fireProgressEvent(event);
        }

        writer.close();
    }

    private AllocationAwareBlockReader createExt4Partition(Partition p) throws IOException {
        Ext4Volume ext = new Ext4Volume(p);
        if (ext.getSuperBlock().isMetaBG()) {
            throw new IOException("MetaBG not implemented yet.");
        }
        ExtAllocationBitmap bitmap = new ExtAllocationBitmap(ext);
        return new AllocationAwareBlockReader(p.getPartitionData(), bitmap);
    }

    private AllocationAwareBlockReader createNTFSPartition(Partition p) throws IOException {
        NTFSVolume ntfs = new NTFSVolume(p);
        NTFSAllocationBitmap bitmap = new NTFSAllocationBitmap(ntfs);
        return new AllocationAwareBlockReader(p.getPartitionData(), bitmap);
    }

    private void searchVDI(File dir, Map<UUID, VDINode> vdis) throws IOException {
        if (dir.isDirectory()) {
            File[] files;
            files = dir.listFiles(new FileFilter() {

                @Override
                public boolean accept(File file) {
                    if (file.isDirectory()) {
                        return true;
                    }
                    String s = file.getName();
                    int i = s.lastIndexOf('.');

                    if (i > 0 && i < s.length() - 1) {
                        String ext = s.substring(i + 1).toLowerCase();
                        if (ext.equals("vdi")) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            for (File f : files) {
                if (f.isDirectory()) {
                    searchVDI(f, vdis);
                } else {
                    VDIBlockReader reader = new VDIBlockReader(
                            new BufferedBlockReader(
                                    new FileBlockReader(f.getCanonicalPath())));
                    UUID uuid = reader.getUuidThisVDI();
                    reader.close();
                    VDINode node = new VDINode(uuid, f);
                    if (!vdis.containsKey(uuid)) {
                        vdis.put(uuid, node);
                    }
                }
            }
        }
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

    private VDIBlockReader getParent(UUID uuid, Map<UUID, VDINode> vdis) throws IOException {
        VDIBlockReader resp = null;
        if (vdis.containsKey(uuid)) {
            File fresp = vdis.get(uuid).file;
            resp = new VDIBlockReader(new BufferedBlockReader(
                    new FileBlockReader(fresp.getCanonicalPath())));
            UUID puuid = resp.getUuidParent();
            if (puuid.getMostSignificantBits() != 0 || puuid.getLeastSignificantBits() != 0) {
                VDIBlockReader parent = getParent(puuid, vdis);
                if (parent == null) {
                    resp.close();
                    resp = null;
                } else {
                    resp.setParent(parent);
                }
            }
        }
        return resp;
    }

    private static class VDINode {

        UUID uuid;
        File file;

        public VDINode(UUID uuid, File file) {
            this.uuid = uuid;
            this.file = file;
        }
    }

}
