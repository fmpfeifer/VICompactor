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

    private final byte[] parseBufferArray;
    private final ByteBuffer parseBuffer;
    private long position;

    public BlockReader() {
        position = 0;
        parseBufferArray = new byte[8];
        parseBuffer = ByteBuffer.wrap(parseBufferArray);
        parseBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    private static final long [] unsMask = new long [] {
        0x00L,
        0xffL,
        0xffffL,
        0xffffffL,
        0xffffffffL,
        0xffffffffffL,
        0xffffffffffffL,
        0xffffffffffffffL,
        0x7fffffffffffffffL
    };

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
        get(parseBufferArray, offset, 0, 2);
        return parseBuffer.getShort(0);
    }

    public short getShort() throws IOException {
        position += 2;
        return getShort(position - 2);
    }

    public int getUnsignedShort(long offset) throws IOException {
        return getShort(offset) & 0x00ffff;
    }

    public int getUnsignedShort() throws IOException {
        position += 2;
        return getUnsignedShort(position - 2);
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
        position += 16;
        return getUUID(position - 16);
    }

    public int getInt(long offset) throws IOException {
        get(parseBufferArray, offset, 0, 4);
        return parseBuffer.getInt(0);
    }

    public int getInt() throws IOException {
        position += 4;
        return getInt(position - 4);
    }

    public long getLong(long offset) throws IOException {
        get(parseBufferArray, offset, 0, 8);
        return parseBuffer.getLong(0);
    }

    public long getLong() throws IOException {
        position += 8;
        return getLong(position - 8);
    }

    public long getUnsignedInt(long offset) throws IOException {
        return getInt(offset) & 0xffffffffL;
    }

    public long getUnsignedInt() throws IOException {
        position += 4;
        return getUnsignedInt(position - 4);
    }

    public long getNumber(long offset, int length) throws IOException {
        long resp;
        switch (length) {
            case 1:
                resp = get(offset);
                break;
            case 2:
                resp = getShort(offset);
                break;
            case 4:
                resp = getInt(offset);
                break;
            case 8:
                resp = getLong(offset);
                break;
            case 3: {
                byte msb;
                short lss;
                if (getByteOrder() == ByteOrder.LITTLE_ENDIAN) {
                    msb = get(offset + 2);
                    lss = getShort(offset);
                } else {
                    msb = get(offset);
                    lss = getShort(offset + 1);
                }
                if ((msb & 0x80) == 0) { //positive
                    resp = ((msb << 16) | (lss & 0xffff));
                } else { //negative
                    resp = ((msb & 0xff) << 16) | (lss & 0xffff) | 0xff000000;
                }
                break;
            }
            case 5: {
                byte msb;
                int lsi;
                if (getByteOrder() == ByteOrder.LITTLE_ENDIAN) {
                    msb = get(offset + 4);
                    lsi = getInt(offset);
                } else {
                    msb = get(offset);
                    lsi = getInt(offset + 1);
                }
                if ((msb & 0x80) == 0) { //positive
                    resp = ((msb << 32) | (lsi & 0xffffffff));
                } else { //negative
                    resp = ((msb & 0xffL) << 32) | (lsi & 0xffffffff) | 0xffffff0000000000L;
                }
                break;
            }
            case 6: {
                short mss;
                int lsi;
                if (getByteOrder() == ByteOrder.LITTLE_ENDIAN) {
                    mss = getShort(offset + 4);
                    lsi = getInt(offset);
                } else {
                    mss = get(offset);
                    lsi = getInt(offset + 2);
                }
                if ((mss & 0x8000) == 0) { //positive
                    resp = ((mss << 32) | (lsi & 0xffffffff));
                } else { //negative
                    resp = ((mss & 0xffffL) << 32) | (lsi & 0xffffffff) | 0xffff000000000000L;
                }
                break;
            }
            case 7: {
                long msi;
                int lsi;
                if (getByteOrder() == ByteOrder.LITTLE_ENDIAN) {
                    msi = getNumber(offset + 4, 3);
                    lsi = getInt(offset);
                } else {
                    msi = getNumber(offset, 3);
                    lsi = getInt(offset + 3);
                }
                if ((msi & 0x800000) == 0) { //positive
                    resp = ((msi << 32) | (lsi & 0xffffffff));
                } else { //negative
                    resp = ((msi & 0xffffffL) << 32) | (lsi & 0xffffffffL) | 0xff00000000000000L;
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Invalid number length");
        }

        return resp;
    }

    public long getNumber(int length) throws IOException {
        position += length;
        return getNumber(position - length, length);
    }
    
    public long getUnsignedNumber(long offset, int length) throws IOException {
        return getNumber(offset, length) & unsMask[length];
    }

    public long getUnsignedNumber(int length) throws IOException {
        position += length;
        return getUnsignedNumber(position - length, length);
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
        while (i < size) {
            i++;
            if (buff[i - 1] == (byte) 0) {
                break;
            }
        }
        return new String(buff, 0, i);
    }

    /**
     * @return the byteOrder
     */
    public ByteOrder getByteOrder() {
        return parseBuffer.order();
    }

    /**
     * @param byteOrder the byteOrder to set
     */
    public void setByteOrder(ByteOrder byteOrder) {
        parseBuffer.order(byteOrder);
    }
}
