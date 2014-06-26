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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class FileBlockReader extends BlockReader {

    private RandomAccessFile file;
    private final FileChannel channel;

    public FileBlockReader(String file) throws IOException {
        this.file = new RandomAccessFile(file, "r");
        channel = this.file.getChannel();
    }

    @Override
    public byte get(long pos) throws IOException {
        file.seek(pos);
        return file.readByte();
    }

    @Override
    public short getUnsignedByte(long pos) throws IOException {
        file.seek(pos);
        return (short) file.readUnsignedByte();
    }

    @Override
    public int get(byte[] resp, long pos, int destPos, int length) throws IOException {
        file.seek(pos);
        ByteBuffer buff = ByteBuffer.wrap(resp, destPos, length);
        return channel.read(buff);
    }

    @Override
    public long length() throws IOException {
        return file.length();
    }

    @Override
    public void close() throws IOException {
        file.close();
        file = null;
    }
}
