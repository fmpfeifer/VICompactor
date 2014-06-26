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
package org.pfeifer.blockreader;

import java.io.IOException;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class LimitBlockReader extends BlockReader {

    private final long start;
    private final long maxLength;
    private final BlockReader original;

    public LimitBlockReader(BlockReader original, long pos) {
        this.original = original;
        this.start = pos;
        this.maxLength = 0;
    }

    public LimitBlockReader(BlockReader original, long pos, long maxLength) {
        this.original = original;
        this.start = pos;
        this.maxLength = maxLength;
    }

    @Override
    public byte get(long pos) throws IOException {
        /*if (pos < 0 || pos >= maxLength) {
            throw new IndexOutOfBoundsException("LimitByteReader index out of bounds");
        }*/
        return original.get(pos + start);
    }

    @Override
    public int get(byte[] buffer, long pos, int destPos, int length) throws IOException {
        /*if (pos < 0 || pos + length > maxLength) {
            throw new IndexOutOfBoundsException("LimitByteReader index out of bounds");
        }*/
        return original.get(buffer, pos + start, destPos, length);
    }

    @Override
    public long length() throws IOException {
        long resp = original.length() - start;
        if (maxLength >= 0) {
            resp = Math.min(maxLength, resp);
        }
        return resp;
    }
    
    @Override
    public boolean blockIsZero(long pos, int size) throws IOException {
        return original.blockIsZero(pos + start, size);
    }
    
    @Override
    public boolean blockIsUnallocated(long pos, int size) throws IOException {
        return original.blockIsUnallocated(pos + start, size);
    }

    @Override
    public void close() {
    }
}
