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

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class ByteArrayBlockReader extends BlockReader {

    private byte[] array;

    public ByteArrayBlockReader(byte[] array) {
        this.array = array;
    }

    @Override
    public byte get(long pos) {
        return array[(int) pos];
    }
    
    @Override
    public long length() {
        return array.length;
    }

    @Override
    public void close() {
        array = null;
    }

    @Override
    public int get(byte[] buffer, long pos, int destPos, int length) {
        int read;

        if (pos + length > length()) {
            read = (int) (length() - pos);
        } else {
            read = length;
        }

        System.arraycopy(array, (int) pos, buffer, destPos, read);

        return read;
    }
}
