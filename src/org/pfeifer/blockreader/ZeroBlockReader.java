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
import java.util.Arrays;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class ZeroBlockReader extends BlockReader {

    private final long size;

    public ZeroBlockReader(long size) {
        this.size = size;
    }

    @Override
    public byte get(long pos) throws IOException {
        if (pos < 0 || pos >= size) {
            throw new IOException("out of bounds in zero byte reader");
        }
        return (byte) 0;
    }

    @Override
    public int get(byte [] buff, long pos, int destPos, int length) {
        Arrays.fill(buff, destPos, destPos + length, (byte) 0);
        return length;
    }

    @Override
    public long length() throws IOException {
        return size;
    }

    @Override
    public void close() throws IOException {
    }
    
    @Override
    public boolean blockIsZero(long pos, int size) throws IOException {
        return true;
    }
    
}
