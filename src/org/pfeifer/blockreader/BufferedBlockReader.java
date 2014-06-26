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
public class BufferedBlockReader extends BlockReader {

    private final int bufferSize;
    private byte[] buffer;
    private byte[] oldbuffer;
    private final BlockReader underlying;
    private long bufferPos = 0;
    private long bufferEnd = 0;
    private long oldBufferPos = 0;
    private long oldBufferEnd = 0;
    private boolean avoidDoubleBuffering = true;

    public BufferedBlockReader(BlockReader underlying) {
        this.underlying = underlying;
        bufferSize = 16 * 1024;
        allocateBuffers();
        seek(underlying.getPosition());
    }

    public BufferedBlockReader(BlockReader underlying, int bufferSize) {
        this.underlying = underlying;
        this.bufferSize = bufferSize;
        allocateBuffers();
    }

    private void allocateBuffers() {
        buffer = new byte[bufferSize];
        oldbuffer = new byte[bufferSize];
    }

    @Override
    public byte get(long pos) throws IOException {
        if (pos >= bufferEnd || pos < bufferPos) {
            if (!(pos >= oldBufferEnd || pos < oldBufferPos)) {
                return oldbuffer[(int) (pos - oldBufferPos)];
            }
            fillBuffer(pos - pos % bufferSize);
        }
        return buffer[(int) (pos - bufferPos)];
    }

    @Override
    public long length() throws IOException {
        return underlying.length();
    }

    @Override
    public void close() throws IOException {
        underlying.close();
        buffer = oldbuffer = null;
    }

    @Override
    public int get(byte[] resp, long pos, int destPos, int length) throws IOException {
        //nao buferiza se for muita coisa, passa direto para o buffer do cliente
        //para evitar um arraycopy
        if (avoidDoubleBuffering && length > bufferSize) {
            return underlying.get(resp, pos, destPos, length);
        }

        int read = 0;

        if (length <= 8) { //se for pouca coisa, pega um byte de cada vez que eh mais rapido
            for (int i = 0; i < length; i++) {
                resp[i + destPos] = get(pos + i);
                read++;
            }
        } else {
            if (pos + length > length()) {
                read = (int) (length() - pos);
            } else {
                read = length;
            }

            byte[] currBuff;
            long b, l;
            int x = destPos;
            int q = read;

            while (q > 0) {
                if (pos >= bufferPos && pos < bufferEnd) {
                    currBuff = buffer;
                    b = pos - bufferPos;
                    l = bufferEnd - pos;
                } else if (pos >= oldBufferPos && pos < oldBufferEnd) {
                    currBuff = oldbuffer;
                    b = pos - oldBufferPos;
                    l = oldBufferEnd - pos;
                } else {
                    fillBuffer(pos - pos % bufferSize);
                    currBuff = buffer;
                    b = pos - bufferPos;
                    l = bufferEnd - pos;
                }

                l = Math.min(l, q);

                System.arraycopy(currBuff, (int) b, resp, x, (int) l);
                x += l;
                pos += l;
                q -= l;
            }
        }

        return read;
    }

    private void fillBuffer(long pos) throws IOException {
        oldBufferPos = bufferPos;
        oldBufferEnd = bufferEnd;

        byte[] bufferTemp = oldbuffer;
        oldbuffer = buffer;
        buffer = bufferTemp;

        bufferEnd = underlying.get(buffer, pos, 0, buffer.length) + pos;
        bufferPos = pos;
    }

    /**
     * @return the avoidDoubleBuffering
     */
    public boolean isAvoidDoubleBuffering() {
        return avoidDoubleBuffering;
    }

    /**
     * @param avoidDoubleBuffering the avoidDoubleBuffering to set
     */
    public void setAvoidDoubleBuffering(boolean avoidDoubleBuffering) {
        this.avoidDoubleBuffering = avoidDoubleBuffering;
    }
    
    @Override
    public boolean blockIsUnallocated(long pos, int size) throws IOException {
        return underlying.blockIsUnallocated(pos, size);
    }
}
