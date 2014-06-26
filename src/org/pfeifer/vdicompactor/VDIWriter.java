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
import java.io.RandomAccessFile;
import java.util.Arrays;
import org.pfeifer.blockreader.VDIBlockReader;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class VDIWriter {

    private final VDIBlockReader original;
    private final RandomAccessFile writer;
    private int[] indexMap;
    private final byte[] buffer = new byte[8192];
    private int allocatedBlocks = 0;

    public VDIWriter(VDIBlockReader original, File destination) throws IOException {
        this.original = original;
        this.writer = new RandomAccessFile(destination, "rw");
        initFile();
    }

    private void initFile() throws IOException {
        int toCopy = original.getOffsetBlocks();
        long pos = 0;
        while (toCopy > 0) {
            int chunk = Math.min(buffer.length, toCopy);
            int read = original.getRawData().get(buffer, pos, 0, chunk);
            writer.seek(pos);
            writer.write(buffer, 0, read);
            pos += read;
            toCopy -= read;
        }
        createIndexMap();
        updateHeader();
        updateIndexMap();
    }

    private void updateHeader() throws IOException {
        writer.seek(0x184);
        writer.writeInt(Integer.reverseBytes(allocatedBlocks));
    }

    private void updateIndexMap() throws IOException {
        writer.seek(original.getOffsetBlocks());
        for (int i = 0; i < indexMap.length; i++) {
            writer.writeInt(Integer.reverseBytes(indexMap[i]));
        }
    }

    private void createIndexMap() {
        indexMap = new int[original.getBlocksInHDD()];
        Arrays.fill(indexMap, -1);
    }

    public void write(byte[] data, long pos, int size) throws IOException {
        int dataOffset = 0;
        while (size > 0) {
            int chunk;
            if (pos % original.getBlockSize() == 0) {
                chunk = Math.min(original.getBlockSize(), size);
            } else {
                chunk = (int) Math.min(pos % original.getBlockSize(), size);
            }
            int block = (int) (pos / original.getBlockSize());
            writeInBlock(data, block, (int) (pos % original.getBlockSize()), dataOffset, chunk);
            pos += chunk;
            size -= chunk;
            dataOffset += chunk;
        }
    }

    private void writeInBlock(byte[] data, int block, int offsetInBlock, int offsetInData, int size) throws IOException {
        if (indexMap[block] < 0) {
            indexMap[block] = allocatedBlocks;
            allocatedBlocks += 1;
            //updateHeader();
            //updateIndexMap();
        }

        writer.seek(original.getOffsetData() + ((long) indexMap[block]) * original.getBlockSize() + offsetInBlock);
        writer.write(data, offsetInData, size);

    }

    private void writeZeroInBlock(int block) {
        indexMap[block] = -2;
    }

    private void writeFreeInBlock(int block) {
        indexMap[block] = -1;
    }

    public void writeZero(long pos, int size) throws IOException {
        while (size > 0) {
            int chunk;
            if (pos % original.getBlockSize() == 0) {
                chunk = Math.min(original.getBlockSize(), size);
            } else {
                chunk = (int) Math.min(pos % original.getBlockSize(), size);
            }
            int block = (int) (pos / original.getBlockSize());
            if (pos % original.getBlockSize() == 0 && chunk == original.getBlockSize()) {
                writeZeroInBlock(block);
            } else {
                byte data[] = new byte[chunk];
                Arrays.fill(data, (byte) 0);
                writeInBlock(data, block, (int) (pos % original.getBlockSize()), 0, chunk);
            }
            pos += chunk;
            size -= chunk;
        }
    }

    public void writeFree(long pos, int size) throws IOException {
        if (pos % original.getBlockSize() != 0) {
            throw new IOException("Block out of aligment.");
        }
        while (size > 0) {
            int chunk = Math.min(original.getBlockSize(), size);
            int block = (int) (pos / original.getBlockSize());
            writeFreeInBlock(block);
            pos += chunk;
            size -= chunk;
        }
    }

    public void updateStructures() throws IOException {
        updateHeader();
        updateIndexMap();
    }

    public void close() throws IOException {
        updateStructures();
        writer.close();
    }

}
