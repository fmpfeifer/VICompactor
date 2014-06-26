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
package org.pfeifer.ntfsreader;

import java.io.IOException;
import org.pfeifer.blockreader.BlockReader;
import org.pfeifer.blockreader.LimitBlockReader;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class FixupBlockReader extends LimitBlockReader {

    private byte[] updateSequenceArray;
    private short updateSequenceNumber;
    private boolean activated = false;

    public FixupBlockReader(BlockReader original, long pos, long length) throws IOException {
        super(original, pos, length);
    }

    public void activateFixup() throws IOException {
        if (!activated) {
            int offsetUpdateSequence = getShort(0x04);
            int sizeUpdateSequenceArray = getShort(0x06) * 2 - 2;
            updateSequenceNumber = getShort(offsetUpdateSequence);
            updateSequenceArray = new byte[sizeUpdateSequenceArray];
            super.get(updateSequenceArray, offsetUpdateSequence + 2);
            activated = true;
        }
    }

    @Override
    public byte get(long pos) throws IOException {
        byte resp = super.get(pos);
        if (activated) {

            int intPos = (int) pos;
            int remainder = intPos & 0x1ff;

            if (remainder >= 510) {
                resp = updateSequenceArray[(intPos >> 8) & 0xfffffffe + (remainder & 0x01)];
            }
        }

        return resp;
    }

    @Override
    public int get(byte[] resp, long pos, int destPos, int length) throws IOException {
        int read = 0;
        if (activated) {

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

                int left = read;                 //bytes left to read
                int destCursor = destPos;        //destination cursor
                int bytesToCopy;             //amount to read this pass
                int srcCursor = (int) pos;       //source cursor
                int remainder;

                while (left > 0) {
                    remainder = srcCursor & 0x1ff;
                    if (remainder >= 0x1fe) {
                        // in fixup
                        resp[destCursor] = updateSequenceArray[(srcCursor >> 8) & 0xfffffffe + (remainder & 0x01)];
                        left -= 1;
                        destCursor += 1;
                        srcCursor += 1;
                    } else {
                        // copy buffer
                        bytesToCopy = Math.min(0x1fe - remainder, left);
                        super.get(resp, srcCursor, destCursor, bytesToCopy);
                        destCursor += bytesToCopy;
                        left -= bytesToCopy;
                        srcCursor += bytesToCopy;
                    }

                }
            }
        } else {
            read = super.get(resp, pos, destPos, length);
        }

        return read;
    }
}
