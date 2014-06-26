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

import org.pfeifer.blockreader.datastructure.SelfBalancingBST;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Fabio Melo Pfeife <fmpfeifer@gmail.com>
 */
public class ConcatenatedBlockReader extends BlockReader {

    private final SelfBalancingBST<Node> readers;
    private final long length;
    private Node currentNode;

    public ConcatenatedBlockReader(BlockReader[] blockReaders) throws IOException {
        readers = new SelfBalancingBST<>();
        long l = 0;
        for (BlockReader reader : blockReaders) {
            Node node = new Node(l, l + reader.length(), reader);
            readers.put(l, node);
            l += reader.length();
        }
        length = l;
        currentNode = readers.searchLessOrEqual(0);
    }

    public ConcatenatedBlockReader(List<BlockReader> blockReaders) throws IOException {
        this(blockReaders.toArray(new BlockReader[blockReaders.size()]));
    }

    private void adjustPosition(long at) {
        if (at < currentNode.windowStart || at >= currentNode.windowEnd) {
            currentNode = readers.searchLessOrEqual(at);
        }
    }
    
    @Override
    public byte get(long pos) throws IOException {
        adjustPosition(pos);
        return currentNode.reader.get(pos - currentNode.windowStart);
    }

    @Override
    public long length() throws IOException {
        return length;
    }

    @Override
    public void close() throws IOException {
        // FIXME: close all
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
        int q = read;

        while (q > 0) {
            adjustPosition(pos);
            b = (pos - currentNode.windowStart);
            l = (currentNode.windowEnd - pos);

            l = Math.min(l, q);

            currentNode.reader.get(resp, b, destPos, (int) l);

            destPos += (int) l;
            pos += (int) l;
            q -= (int) l;
        }

        return read;
    }

    @Override
    public boolean blockIsZero(long pos, int size) throws IOException {
        int analizing;
        while (size > 0) {
            adjustPosition(pos);
            analizing = (int) Math.min(size, currentNode.windowEnd - pos);

            boolean resp = currentNode.reader.blockIsZero(pos - currentNode.windowStart, analizing);
            if (resp == false) {
                return false;
            }
            size -= analizing;
            pos += analizing;
        }
        return true;
    }
    
    @Override
    public boolean blockIsUnallocated(long pos, int size) throws IOException {
        int analizing;
        while (size > 0) {
            adjustPosition(pos);
            analizing = (int) Math.min(size, currentNode.windowEnd - pos);

            boolean resp = currentNode.reader.blockIsUnallocated(pos - currentNode.windowStart, analizing);
            if (resp == false) {
                return false;
            }
            size -= analizing;
            pos += analizing;
        }
        return true;
    }

    private static class Node {

        final long windowStart;
        final long windowEnd;
        final BlockReader reader;

        public Node(long windowStart, long windowEnd, BlockReader reader) {
            this.windowStart = windowStart;
            this.windowEnd = windowEnd;
            this.reader = reader;
        }
    }

}
