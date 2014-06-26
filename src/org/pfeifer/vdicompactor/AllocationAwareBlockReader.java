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

import java.io.IOException;
import java.util.Arrays;
import org.pfeifer.blockreader.BlockReader;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class AllocationAwareBlockReader extends BlockReader {

    private final BlockReader reader;
    private final AllocationBitmap bitmap;

    public AllocationAwareBlockReader(BlockReader reader, AllocationBitmap bitmap) {
        this.reader = reader;
        this.bitmap = bitmap;
        seek(reader.getPosition());
    }

    @Override
    public byte get(long pos) throws IOException {
        if (bitmap.isAllocated(pos, 1)) {
            return reader.get();
        }
        return 0;
    }

    @Override
    public int get(byte[] buffer, long pos, int destPos, int length) throws IOException {
        int read = 0;
        int toRead;
        while (length > 0) {
            if (pos % bitmap.getBitmapChunckSize() == 0) {
                toRead = Math.min(length, bitmap.getBitmapChunckSize());
            } else {
                toRead = Math.min(length, (int) (pos % bitmap.getBitmapChunckSize()));
            }
            if (bitmap.isAllocated(pos, toRead)) {
                int r;
                r = reader.get(buffer, pos, destPos, toRead);
                read += r;
                pos += r;
                destPos += r;
                length -= r;
            } else {
                Arrays.fill(buffer, destPos, destPos + toRead, (byte) 0);
                read += toRead;
                pos += toRead;
                destPos += toRead;
                length -= toRead;
            }
        }
        return read;
    }

    @Override
    public long length() throws IOException {
        return reader.length();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public boolean blockIsUnallocated(long pos, int size) throws IOException {
        return reader.blockIsUnallocated(pos, size);
    }
    
    @Override
    public boolean blockIsZero(long pos, int size) throws IOException {
        int toCheck;
        while (size > 0) {
            if (pos % bitmap.getBitmapChunckSize() == 0) {
                toCheck = Math.min(size, bitmap.getBitmapChunckSize());
            } else {
                toCheck = Math.min(size, (int) (pos % bitmap.getBitmapChunckSize()));
            }
            if (bitmap.isAllocated(pos, toCheck)) {
                if (!reader.blockIsZero(pos, toCheck)) {
                    return false;
                }
            }
            pos += toCheck;
            size -= toCheck;
        }
        return true;
    }

}
