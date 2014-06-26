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
import java.io.InputStream;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class BlockReaderInputStream extends InputStream {

    private final BlockReader reader;
    private long pos = 0;
    private long mark = 0;

    public BlockReaderInputStream(BlockReader reader) {
        this.reader = reader;
    }

    @Override
    public int read() throws IOException {
        int read = -1;
        if (pos < reader.length()) {
            read = reader.getUnsignedByte(pos++);
        }
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = -1;
        if (pos < reader.length()) {
            read = reader.get(b, pos, off, len);
            if (read > 0) {
                pos += read;
            }
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n < 0) {
            return 0;
        }
        long read;

        if (pos + n > reader.length()) {
            read = (int) (reader.length() - pos);
        } else {
            read = n;
        }

        pos += read;

        return read;
    }

    @Override
    public void mark(int readlimit) {
        mark = pos;
    }

    @Override
    public void reset() throws IOException {
        pos = mark;
    }

    @Override
    public boolean markSupported() {
        return true;
    }
}
