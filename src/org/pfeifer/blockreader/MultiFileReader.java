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

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class MultiFileReader extends BlockReader {

    private final long[] pos;
    private final File[] files;
    private final long[] chunkSize;
    private long length = 0;
    private BlockReader currentReader = null;
    private int currentIndex = 0;
    private long windowStart = 0;
    private long windowEnd = 0;

    public MultiFileReader(String directory, String prefix) throws IOException {
        List<File> list = new ArrayList<>();

        int i = 0;
        DecimalFormat suffixFormatter = new DecimalFormat("000");

        while (true) {
            File test = new File(directory + "/" + prefix + "." + suffixFormatter.format(++i));
            if (!test.exists()) {
                break;
            }
            list.add(test);
        }

        pos = new long[list.size()];
        files = new File[list.size()];
        chunkSize = new long[list.size()];
        length = 0;

        for (i = 0; i < pos.length; i++) {
            files[i] = list.get(i);
            length += (chunkSize[i] = files[i].length());
            pos[i] = length;
        }

        windowEnd = pos[0];

        alocateFile();
    }

    private void alocateFile() throws IOException {
        if (currentReader != null) {
            currentReader.close();
        }

        currentReader = new FileBlockReader(files[currentIndex].getCanonicalPath());

        System.out.println("Reading file: " + files[currentIndex].getCanonicalPath());
    }

    @Override
    public byte get(long pos) throws IOException {
        adjustPosition(pos);
        return currentReader.get(pos - windowStart);
    }

    @Override
    public long length() throws IOException {
        return length;
    }

    @Override
    public void close() throws IOException {
        if (currentReader != null) {
            currentReader.close();
        }
    }

    private void adjustPosition(long at) {
        if (at < windowStart || at >= windowEnd) {
            int current = 0;
            for (int i = 0; i < pos.length; i++) {
                if (at < pos[i]) {
                    current = i;
                    break;
                }
            }
            currentIndex = current;

            if (current == 0) {
                windowStart = 0;
                windowEnd = pos[current];
            } else {
                windowStart = pos[current - 1];
                windowEnd = pos[current];
            }
            try {
                alocateFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public int get(byte[] resp, long pos, int destPos, int length) throws IOException {
        int read;

        if (pos + length > length()) {
            read = (int) (length() - pos);
        } else {
            read = length;
        }

        long b, l;
        int x = destPos;
        int q = read;

        while (q > 0) {
            adjustPosition(pos);

            b = (pos - windowStart);
            l = (windowEnd - pos);

            l = Math.min(l, q);

            currentReader.get(resp, b, x, (int) l);

            x += (int) l;
            pos += (int) l;
            q -= (int) l;
        }


        return read;
    }
}
