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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public abstract class BlockReader {

    private final ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private long position;

    public BlockReader() {
        position = 0;
    }

    /**
     * Get byte in given position
     *
     * @param pos Position
     * @return byte in position
     * @throws IOException
     */
    public abstract byte get(long pos) throws IOException;

    /**
     * Return the size of this data block
     *
     * @return
     * @throws IOException
     */
    public abstract long length() throws IOException;

    /**
     * close the data block
     *
     * @throws IOException
     */
    public abstract void close() throws IOException;

    /**
     * Bulk get method. Subclasses are encouraged to override this method for
     * efficiency.
     *
     * @param buffer Destination buffer
     * @param pos Source start position
     * @param destPos Destination buffer start position
     * @param length How many bytes to transfer
     * @return Actual amount of bytes transferred
     * @throws IOException
     */
    public int get(byte[] buffer, long pos, int destPos, int length) throws IOException {
        int read;

        if (pos + length > length()) {
            read = (int) (length() - pos);
        } else {
            read = length;
        }

        for (int i = 0; i < read; i++) {
            buffer[i + destPos] = get(pos + i);
        }

        return read;
    }

    public int get(byte[] buffer, long pos) throws IOException {
        return get(buffer, pos, 0, buffer.length);
    }

    /**
     * Get byte at given position as unsigned. Upcast to short is necessary.
     *
     * @param pos
     * @return
     * @throws IOException
     */
    public short getUnsignedByte(long pos) throws IOException {
        return (short) (get(pos) & 0xff);
    }

    public short getUnsignedByte() throws IOException {
        return (short) (get(position++) & 0xff);
    }

    public byte get() throws IOException {
        return get(position++);
    }

    public int get(byte[] buffer, int offset, int length) throws IOException {
        int read = get(buffer, position, offset, length);
        position += read;
        return read;
    }

    public int get(byte[] buffer) throws IOException {
        return get(buffer, 0, buffer.length);
    }

    public long getPosition() {
        return position;
    }

    public void seek(long position) {
        this.position = position;
    }

    /**
     * Create an InputStream associated with this BlockReader to provide
     * compatibility.
     *
     * @return
     */
    public InputStream getInputStream() {
        return new BlockReaderInputStream(this);
    }

    /*public void setByteOrder(ByteOrder order) {
     byteOrder = order;
     }

     public ByteOrder getByteOrder() {
     return byteOrder;
     }*/
    public short getShort(long offset) throws IOException {
        return (short) ((get(offset) & 0xff) | (get(offset + 1) << 8));
    }

    public short getShort() throws IOException {
        short resp = getShort(position);
        position += 2;
        return resp;
    }

    public int getUnsignedShort(long offset) throws IOException {
        return (int) (((get(offset) & 0xff))
                | ((get(offset + 1) & 0xff) << 8));
    }

    public int getUnsignedShort() throws IOException {
        int resp = getUnsignedShort(position);
        position += 2;
        return resp;
    }

    public UUID getUUID(long pos) throws IOException {
        byte[] buff = new byte[16];
        get(buff, pos);
        ByteBuffer buffer = ByteBuffer.wrap(buff);
        long mostSig = buffer.getLong();
        long leastSig = buffer.getLong();
        return new UUID(mostSig, leastSig);
    }

    public UUID getUUID() throws IOException {
        UUID resp = getUUID(position);
        position += 16;
        return resp;
    }

    public int getInt(long offset) throws IOException {
        return (int) ((((get(offset) & 0xff))
                | ((get(offset + 1) & 0xff) << 8)
                | ((get(offset + 2) & 0xff) << 16)
                | ((get(offset + 3) & 0xff) << 24)));
    }

    public int getInt() throws IOException {
        int resp = getInt(position);
        position += 4;
        return resp;
    }

    public long getLong(long offset) throws IOException {
        return ((((long) get(offset) & 0xff))
                | (((long) get(offset + 1) & 0xff) << 8)
                | (((long) get(offset + 2) & 0xff) << 16)
                | (((long) get(offset + 3) & 0xff) << 24)
                | (((long) get(offset + 4) & 0xff) << 32)
                | (((long) get(offset + 5) & 0xff) << 40)
                | (((long) get(offset + 6) & 0xff) << 48)
                | (((long) get(offset + 7) & 0xff) << 56));
    }

    public long getLong() throws IOException {
        long resp = getLong(position);
        position += 8;
        return resp;
    }

    public long getUnsignedInt(long offset) throws IOException {
        return ((((long) get(offset) & 0xff))
                | (((long) get(offset + 1) & 0xff) << 8)
                | (((long) get(offset + 2) & 0xff) << 16)
                | (((long) get(offset + 3) & 0xff) << 24));
    }

    public long getUnsignedInt() throws IOException {
        long resp = getUnsignedInt(position);
        position += 4;
        return resp;
    }

    public long getNumber(long offset, int length) throws IOException {
        long resp = 0;

        if (length == 1) {
            resp = get(offset);
        } else if (length == 2) {
            resp = getShort(offset);
        } else if (length == 4) {
            resp = getInt(offset);
        } else if (length == 8) {
            resp = getLong(offset);
        } else if (length == 3) {
            byte b3;
            if (((b3 = get(offset + 2)) & 0x80) == 0) { //positivo
                int i = (int) ((((get(offset) & 0xff))
                        | ((get(offset + 1) & 0xff) << 8)
                        | ((b3 & 0xff) << 16)));
                resp = i;
            } else {
                int i = (int) ((((get(offset) & 0xff))
                        | ((get(offset + 1) & 0xff) << 8)
                        | ((b3 & 0xff) << 16)
                        | 0xff000000));
                resp = i;
            }
        } else if (length == 5) {
            byte b4;
            if (((b4 = get(offset + 4)) & 0x80) == 0) {
                resp = ((((long) get(offset) & 0xff))
                        | (((long) get(offset + 1) & 0xff) << 8)
                        | (((long) get(offset + 2) & 0xff) << 16)
                        | (((long) get(offset + 3) & 0xff) << 24)
                        | (((long) b4 & 0xff) << 32));
            } else {
                resp = ((((long) get(offset) & 0xff))
                        | (((long) get(offset + 1) & 0xff) << 8)
                        | (((long) get(offset + 2) & 0xff) << 16)
                        | (((long) get(offset + 3) & 0xff) << 24)
                        | (((long) b4 & 0xff) << 32)
                        | 0xffffff0000000000L);
            }
        } else if (length == 6) {
            byte b5;
            if (((b5 = get(offset + 5)) & 0x80) == 0) {
                resp = ((((long) get(offset) & 0xff))
                        | (((long) get(offset + 1) & 0xff) << 8)
                        | (((long) get(offset + 2) & 0xff) << 16)
                        | (((long) get(offset + 3) & 0xff) << 24)
                        | (((long) get(offset + 4) & 0xff) << 32)
                        | (((long) b5 & 0xff) << 40));
            } else {
                resp = ((((long) get(offset) & 0xff))
                        | (((long) get(offset + 1) & 0xff) << 8)
                        | (((long) get(offset + 2) & 0xff) << 16)
                        | (((long) get(offset + 3) & 0xff) << 24)
                        | (((long) get(offset + 4) & 0xff) << 32)
                        | (((long) b5 & 0xff) << 40)
                        | 0xffff000000000000L);
            }
        } else if (length == 7) {
            byte b6;
            if (((b6 = get(offset + 6)) & 0x80) == 0) {
                resp = ((((long) get(offset) & 0xff))
                        | (((long) get(offset + 1) & 0xff) << 8)
                        | (((long) get(offset + 2) & 0xff) << 16)
                        | (((long) get(offset + 3) & 0xff) << 24)
                        | (((long) get(offset + 4) & 0xff) << 32)
                        | (((long) get(offset + 5) & 0xff) << 40)
                        | (((long) b6 & 0xff) << 48));
            } else {
                resp = ((((long) get(offset) & 0xff))
                        | (((long) get(offset + 1) & 0xff) << 8)
                        | (((long) get(offset + 2) & 0xff) << 16)
                        | (((long) get(offset + 3) & 0xff) << 24)
                        | (((long) get(offset + 4) & 0xff) << 32)
                        | (((long) get(offset + 5) & 0xff) << 40)
                        | (((long) b6 & 0xff) << 48)
                        | 0xff00000000000000L);
            }
        } else {
            throw new RuntimeException("Invalid number length");
        }

        return resp;
    }

    public long getNumber(int length) throws IOException {
        long resp = getNumber(position, length);
        position += length;
        return resp;
    }

    public long getUnsignedNumber(long offset, int length) throws IOException {
        long resp = 0;

        if (length == 1) {
            resp = get(offset) & 0xff;
        } else if (length == 2) {
            resp = getUnsignedShort(offset);
        } else if (length == 4) {
            resp = getUnsignedInt(offset);
        } else if (length == 8) {
            resp = getLong(offset) & 0x7fffffffffffffffL;
        } else if (length == 3) {

            int i = (int) ((((get(offset) & 0xff))
                    | ((get(offset + 1) & 0xff) << 8)
                    | ((get(offset + 3) & 0xff) << 16)));
            resp = i;

        } else if (length == 5) {

            resp = ((((long) get(offset) & 0xff))
                    | (((long) get(offset + 1) & 0xff) << 8)
                    | (((long) get(offset + 2) & 0xff) << 16)
                    | (((long) get(offset + 3) & 0xff) << 24)
                    | (((long) get(offset + 4) & 0xff) << 32));

        } else if (length == 6) {

            resp = ((((long) get(offset) & 0xff))
                    | (((long) get(offset + 1) & 0xff) << 8)
                    | (((long) get(offset + 2) & 0xff) << 16)
                    | (((long) get(offset + 3) & 0xff) << 24)
                    | (((long) get(offset + 4) & 0xff) << 32)
                    | (((long) get(offset + 5) & 0xff) << 40));

        } else if (length == 7) {

            resp = ((((long) get(offset) & 0xff))
                    | (((long) get(offset + 1) & 0xff) << 8)
                    | (((long) get(offset + 2) & 0xff) << 16)
                    | (((long) get(offset + 3) & 0xff) << 24)
                    | (((long) get(offset + 4) & 0xff) << 32)
                    | (((long) get(offset + 5) & 0xff) << 40)
                    | (((long) get(offset + 6) & 0xff) << 48));

        } else {
            throw new RuntimeException("Invalid number length");
        }

        return resp;
    }

    public long getUnsignedNumber(int length) throws IOException {
        long resp = getUnsignedNumber(position, length);
        position += length;
        return resp;
    }

    /**
     * Verify if block of data is zero. Subclasses are encouraged to override
     * this method if there is a more efficient way of doing this.
     *
     * @param pos Position of the block of data
     * @param size Size of the block of data
     * @return true if block of data is all zeros, false otherwise
     * @throws IOException
     */
    public boolean blockIsZero(long pos, int size) throws IOException {
        for (int i = 0; i < size; i++) {
            if (get(pos + i) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verify if this block is unnalocated. Subclasses with unnalocated blocks
     * should override it.
     *
     * @param pos Position of the data block
     * @param size Size of the date block
     * @return true if this block is unnalocated
     * @throws IOException
     */
    public boolean blockIsUnallocated(long pos, int size) throws IOException {
        return false;
    }

    public String getString(long pos, int size) throws IOException {
        byte[] buff = new byte[size];
        this.get(buff, pos);
        return new String(buff);
    }

    public String getString(long pos, int size, String encoding) throws IOException {
        byte[] buff = new byte[size];
        this.get(buff, pos);
        return new String(buff, encoding);
    }

    public String getNullTerminatedString(long pos, int size) throws IOException {
        int i = 0;
        byte[] buff = new byte[size];
        this.get(buff, pos);
        while (i < size && buff[i] != (byte) 0) {
            i++;
        }
        return new String(buff, 0, i + 1);
    }
}
