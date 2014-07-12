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
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * FIXME: this class only works if file size < Integer.MAX_VALUE.
 * An implementation to map regions of file is needed here.
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class MappedFileBlockReader extends BlockReader {

    private final RandomAccessFile file;
    private MappedByteBuffer buffer;
    private final long length;

    public MappedFileBlockReader(String fileName) throws IOException {
        file = new RandomAccessFile(fileName, "r");
        length = file.length();
        buffer = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, length);
    }

    @Override
    public int get(byte[] dest, long pos, int destPos, int length) throws IOException {
        int read;

        if (pos + length > length()) {
            read = (int) (length() - pos);
        } else {
            read = length;
        }

        buffer.position((int) pos);
        buffer.get(dest, destPos, read);

        return read;
    }

    @Override
    public byte get(long pos) throws IOException {
        return buffer.get((int) pos);
    }

    @Override
    public long length() throws IOException {
        return length;
    }

    @Override
    public void close() throws IOException {
        buffer = null;
        file.close();
    }
}
