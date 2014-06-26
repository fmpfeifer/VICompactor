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

import org.pfeifer.blockreader.ZeroBlockReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.pfeifer.blockreader.BlockReader;
import org.pfeifer.blockreader.ByteArrayBlockReader;
import org.pfeifer.blockreader.ConcatenatedBlockReader;
import org.pfeifer.blockreader.LimitBlockReader;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class DataRunsBlockReader extends BlockReader {

    private long[] pos;
    private DataRun[] dataRuns;
    private long[] chunkSize;
    private long length = 0;
    private BlockReader currentReader = null;
    private int currentIndex = 0;
    private long windowStart = 0;
    private long windowEnd = 0;
    private final NTFSVolume volume;
    private boolean zeroSize = false;
    private final boolean compressed;

    public DataRunsBlockReader(NTFSVolume volume, BlockReader dataRuns, int dataRunsOffset, boolean compressed) throws IOException {
        this.volume = volume;
        this.compressed = compressed;
        parseDataRuns(dataRuns, dataRunsOffset);
    }

    private void parseDataRuns(BlockReader bytesDataRuns, int dataRunsOffset) throws IOException {
        List<DataRun> runs = new ArrayList<>();
        int nextRun = dataRunsOffset;
        long lastRunOffset = 0;
        byte bheader;
        while ((bheader = bytesDataRuns.get(nextRun)) != 0) {
            int currentRun = nextRun;
            //byte bheader = bytesDataRuns.get(currentRun);
            int offsetBytes = (bheader & 0xf0) >> 4;
            int lengthRun = (bheader & 0x0f);
            long dataRunLength = bytesDataRuns.getNumber(currentRun + 1, lengthRun);
            long dataRunOffset = 0;
            if (offsetBytes > 0) {
                dataRunOffset = lastRunOffset + bytesDataRuns.getNumber(currentRun + 1 + lengthRun, offsetBytes);
                lastRunOffset = dataRunOffset;
            }
            nextRun = currentRun + 1 + lengthRun + offsetBytes;
            DataRun run = new DataRun(dataRunOffset, dataRunLength, offsetBytes == 0);
            runs.add(run);
        }

        if (runs.isEmpty()) {
            zeroSize = true;
            length = 0;
        } else {
            pos = new long[runs.size()];
            dataRuns = new DataRun[runs.size()];
            chunkSize = new long[runs.size()];
            length = 0;

            for (int i = 0; i < pos.length; i++) {
                dataRuns[i] = runs.get(i);
                length += (chunkSize[i] = dataRuns[i].length * volume.getClusterSize());
                pos[i] = length;
            }

            windowEnd = pos[0];

            if (compressed) {
                processCompressedRuns();
            }

            alocateFile();
        }

    }

    private void processCompressedRuns() throws IOException {
        if (!zeroSize) {
            List<DataRunChunkCompressed> newDataRuns = new ArrayList<>();
            int i = 0;
            int chunks = 0;
            DataRun processing = dataRuns[i++];
            List<DataRun> ck = new ArrayList<>();
            boolean cont = true;
            do {
                if (processing.length > 16 - chunks) {
                    DataRun[] sp = splitDataRun(processing, 16 - chunks);
                    ck.add(sp[0]);
                    processing = sp[1];
                    chunks = 16;
                } else if (processing.length == 16 - chunks) {
                    ck.add(processing);
                    chunks = 16;
                    if (i < dataRuns.length) {
                        processing = dataRuns[i++];
                    } else {
                        cont = false;
                    }
                } else {
                    ck.add(processing);
                    chunks += processing.length;
                    if (i < dataRuns.length) {
                        processing = dataRuns[i++];
                    } else {
                        newDataRuns.add(new DataRunChunkCompressed(chunks, ck));
                        ck = new ArrayList<>();
                        cont = false;
                    }
                }
                if (chunks == 16) {
                    newDataRuns.add(new DataRunChunkCompressed(chunks, ck));
                    ck = new ArrayList<>();
                    chunks = 0;
                }
            } while (cont);


            pos = new long[newDataRuns.size()];
            dataRuns = new DataRun[newDataRuns.size()];
            chunkSize = new long[newDataRuns.size()];
            length = 0;

            for (i = 0; i < pos.length; i++) {
                dataRuns[i] = newDataRuns.get(i);
                length += (chunkSize[i] = dataRuns[i].length * volume.getClusterSize());
                pos[i] = length;
            }

            windowEnd = pos[0];

        }
    }

    private DataRun[] splitDataRun(DataRun dataRun, long first) {
        DataRun[] resp = new DataRun[2];
        resp[0] = new DataRun(dataRun.start, first, dataRun.sparse);
        resp[1] = new DataRun(dataRun.start + first, dataRun.length - first, dataRun.sparse);
        return resp;
    }

    private void alocateFile() throws IOException {
        if (currentReader != null) {
            currentReader.close();
        }

        currentReader = dataRuns[currentIndex].getRunBlockReader();
    }

    @Override
    public byte get(long pos) throws IOException {
        if (zeroSize) {
            throw new IOException("No data to return");
        }
        adjustPosition(pos);
        return currentReader.get(pos - windowStart);
    }

    @Override
    public long length() throws IOException {
        return length;
    }

    @Override
    public void close() throws IOException {
        if (!zeroSize) {
            currentReader.close();
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

        int b;
        int x = destPos;
        int l;
        int q = read;

        while (q > 0) {
            adjustPosition(pos);

            b = (int) (pos - windowStart);
            l = (int) (windowEnd - pos);

            l = Math.min(l, q);

            currentReader.get(resp, b, x, l);

            x += l;
            pos += l;
            q -= l;
        }


        return read;
    }

    private void adjustPosition(long at) throws IOException {
        int oldIndex = currentIndex;
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
            if (oldIndex != currentIndex) {
                alocateFile();
            }
        }
    }

    private class DataRun {

        public long start;
        public long length;
        public boolean sparse;

        public DataRun(long start, long length, boolean sparse) {
            this.start = start;
            this.length = length;
            this.sparse = sparse;
        }

        public BlockReader getRunBlockReader() throws IOException {
            BlockReader block;
            if (sparse) {
                block = new ZeroBlockReader(length * volume.getClusterSize());
            } else {
                block = new LimitBlockReader(volume.getDataBlock(), start * volume.getClusterSize(),
                        length * volume.getClusterSize());
            }
            return block;
        }
    }

    private class DataRunChunkCompressed extends DataRun {

        DataRun[] runs;

        public DataRunChunkCompressed(long length, List<DataRun> dataRuns) {
            super(0, length, false);
            runs = dataRuns.toArray(new DataRun[dataRuns.size()]);
        }

        @Override
        public BlockReader getRunBlockReader() throws IOException {
            int sparseChunks = 0;
            int nonSparseChunks = 0;
            BlockReader reader;
            for (DataRun r : runs) {
                if (r.sparse) {
                    sparseChunks++;
                } else {
                    nonSparseChunks++;
                }
            }
            BlockReader[] ckReaders = new BlockReader[runs.length];
            int i = 0;
            for (DataRun r : runs) {
                ckReaders[i++] = r.getRunBlockReader();
            }
            if (nonSparseChunks == 0 || sparseChunks == 0) { // all sparse or uncompressed
                reader = new ConcatenatedBlockReader(ckReaders);
            } else {
                //byte[] src = new byte[volume.getClusterSize() * 16];
                byte[] dst = new byte[volume.getClusterSize() * 16];
                BlockReader tmpReader = new ConcatenatedBlockReader(ckReaders);
                //tmpReader.get(src);
                //ntfs_decompress(src, dst);
                try {
                    ntfs_decompress(tmpReader, dst);
                    reader = new ByteArrayBlockReader(dst);
                } catch (IOException e) {
                    //e.printStackTrace(); //FIXME: ignorando excecoes ao descomprimir
                    reader = new ZeroBlockReader(dst.length);
                }
            }

            return reader;
        }
    }
    /** this part was adapted from ntfsprogs source code(http://www.linux-ntfs.org/) */
    /* Token types and access mask. */
    private static final int NTFS_SYMBOL_TOKEN = 0;
    private static final int NTFS_PHRASE_TOKEN = 1;
    private static final int NTFS_TOKEN_MASK = 1;
    /* Compression sub-block constants. */
    private static final int NTFS_SB_SIZE_MASK = 0x0fff;
    private static final int NTFS_SB_SIZE = 0x1000;
    private static final int NTFS_SB_IS_COMPRESSED = 0x8000;

    private static void debug(String dbg) {
        //System.out.println(dbg);
    }

    private static int le16_to_cpu(byte[] src, int pos) {
        int resp = ((src[pos] & 0xff) | ((src[pos + 1] & 0xff) << 8));
        return resp;
    }

    private static int ntfs_decompress(byte[] src, byte[] dest) throws IOException {

        // references to compressed data
        int cb_end = src.length;
        int cb_pos = 0;
        int cb_sb_start;
        int cb_sb_end;

        //variables for uncompressed data / dest
        int dest_pos = 0;
        int dest_end = dest.length;
        int dest_sb_start;
        int dest_sb_end;

        // variables for tag and token parsing
        byte tag;       // current tag
        int token;      // loop counter for the eight tokens in tag
        debug("Entering, cb_size = " + cb_end);
        do { //do_next_sb:
            if (cb_pos == cb_end || le16_to_cpu(src, cb_pos) == 0 || dest_pos == dest_end) {
                debug("Completed. Returning success (0).");
                break;
            }

            /* Setup offset for the current sub-block destination. */
            dest_sb_start = dest_pos;
            dest_sb_end = dest_pos + NTFS_SB_SIZE;

            /* Check that we are still within allowed boundaries. */
            if (dest_sb_end > dest_end) {
                throw new IOException("Decompression error - overflow");
            }

            /* Does the minimum size of a compressed sb overflow valid range? */
            if (cb_pos + 6 > cb_end) {
                throw new IOException("Decompression error - overflow");
            }

            /* Setup the current sub-block source pointers and validate range. */
            cb_sb_start = cb_pos;
            cb_sb_end = cb_sb_start + (le16_to_cpu(src, cb_pos) & NTFS_SB_SIZE_MASK) + 3;
            if (cb_sb_end > cb_end) {
                throw new IOException("Decompression error - overflow");
            }

            /* Now, we are ready to process the current sub-block (sb). */
            if ((le16_to_cpu(src, cb_pos) & NTFS_SB_IS_COMPRESSED) == 0) { //nao comprimido
                debug("Found uncompressed sub-block.");
                /* This sb is not compressed, just copy it into destination. */
                /* Advance source position to first data byte. */
                cb_pos += 2;
                /* An uncompressed sb must be full size. */
                if (cb_sb_end - cb_pos != NTFS_SB_SIZE) {
                    throw new IOException("Decompression error - overflow");
                }
                /* Copy the block and advance the source position. */
                System.arraycopy(src, cb_pos, dest, dest_pos, NTFS_SB_SIZE);
                //memcpy(dest, cb, NTFS_SB_SIZE);
                cb_pos += NTFS_SB_SIZE;
                /* Advance destination position to next sub-block. */
                dest_pos += NTFS_SB_SIZE;
                //continue;
            } else {
                cb_pos += 2;
                do { //do_next_tag
                    if (cb_pos == cb_sb_end) {
                        /* Check if the decompressed sub-block was not full-length. */
                        if (dest_pos < dest_sb_end) {
                            int nr_bytes = dest_sb_end - dest_pos;
                            debug("Filling incomplete sub-block with zeroes.");

                            /* Zero remainder and update destination position. */
                            Arrays.fill(dest, dest_pos, dest_pos + nr_bytes, (byte) 0);
                            dest_pos += nr_bytes;

                        }
                        //goto do_next_sb:
                        break;
                    } else {

                        /* Check we are still in range. */
                        if (cb_pos > cb_sb_end || dest_pos > dest_sb_end) {
                            throw new IOException("Decompression error - overflow");
                        }
                        /* Get the next tag and advance to first token. */
                        tag = src[cb_pos++];
                        /* Parse the eight tokens described by the tag. */


                        /* Parse the eight tokens described by the tag. */
                        for (token = 0; token < 8; token++, tag >>= 1) {
                            int lg, pt, length, max_non_overlap;
                            int i;
                            int dest_back_addr;

                            /* Check if we are done / still in range. */
                            if (cb_pos >= cb_sb_end || dest_pos > dest_sb_end) {
                                break;
                            }
                            /* Determine token type and parse appropriately.*/
                            if ((tag & NTFS_TOKEN_MASK) == NTFS_SYMBOL_TOKEN) {
                                /*
                                 * We have a symbol token, copy the symbol across, and
                                 * advance the source and destination positions.
                                 */
                                dest[dest_pos++] = src[cb_pos++];
                                //*  dest++ = *  cb++;
                                /* Continue with the next token. */
                                continue;
                            }
                            /*
                             * We have a phrase token. Make sure it is not the first tag in
                             * the sb as this is illegal and would confuse the code below.
                             */
                            if (dest_pos == dest_sb_start) {
                                throw new IOException("Decompression error - overflow");
                            }
                            /*
                             * Determine the number of bytes to go back (p) and the number
                             * of bytes to copy (l). We use an optimized algorithm in which
                             * we first calculate log2(current destination position in sb),
                             * which allows determination of l and p in O(1) rather than
                             * O(n). We just need an arch-optimized log2() function now.
                             */
                            lg = 0;
                            for (i = dest_pos - dest_sb_start - 1; i >= 0x10; i >>= 1) {
                                lg++;
                            }
                            /* Get the phrase token into i. */
                            pt = le16_to_cpu(src, cb_pos);
                            /*
                             * Calculate starting position of the byte sequence in
                             * the destination using the fact that p = (pt >> (12 - lg)) + 1
                             * and make sure we don't go too far back.
                             */
                            dest_back_addr = dest_pos - (pt >> (12 - lg)) - 1;
                            if (dest_back_addr < dest_sb_start) {
                                throw new IOException("Decompression error - overflow");
                            }
                            /* Now calculate the length of the byte sequence. */
                            length = (pt & (0xfff >> lg)) + 3;
                            /* Verify destination is in range. */
                            if (dest_pos + length > dest_sb_end) {
                                throw new IOException("Decompression error - overflow");
                            }
                            /* The number of non-overlapping bytes. */
                            max_non_overlap = dest_pos - dest_back_addr;
                            if (length <= max_non_overlap) {
                                /* The byte sequence doesn't overlap, just copy it. */
                                System.arraycopy(dest, dest_back_addr, dest, dest_pos, length);
                                //memcpy(dest, dest_back_addr, length);
                                /* Advance destination pointer. */
                                dest_pos += length;
                            } else {
                                /*
                                 * The byte sequence does overlap, copy non-overlapping
                                 * part and then do a slow byte by byte copy for the
                                 * overlapping part. Also, advance the destination
                                 * pointer.
                                 */
                                //memcpy(dest, dest_back_addr, max_non_overlap);
                                System.arraycopy(dest, dest_back_addr, dest, dest_pos, max_non_overlap);
                                dest_pos += max_non_overlap;
                                dest_back_addr += max_non_overlap;
                                length -= max_non_overlap;
                                while (length-- != 0) {
                                    dest[dest_pos++] = dest[dest_back_addr++];
                                    //*  dest++ = *  dest_back_addr++;
                                }
                            }
                            /* Advance source position and continue with the next token. */
                            cb_pos += 2;
                        }
                        /* No tokens left in the current tag. Continue with the next tag. */
                        //goto do_next_tag;
                    }
                } while (true);
            }

        } while (true);

        return 0;
    }

    /*private static int le16_to_cpu(BlockReader src, int pos) throws IOException {
    int resp = ((src.get(pos) & 0xff) | ((src.get(pos + 1) & 0xff) << 8));
    return resp;
    }*/
    private static int ntfs_decompress(BlockReader src, byte[] dest) throws IOException {

        // references to compressed data
        int cb_end = (int) src.length();
        int cb_pos = 0;
        int cb_sb_start;
        int cb_sb_end;

        //variables for uncompressed data / dest
        int dest_pos = 0;
        int dest_end = dest.length;
        int dest_sb_start;
        int dest_sb_end;

        // variables for tag and token parsing
        byte tag;       // current tag
        int token;      // loop counter for the eight tokens in tag
        debug("Entering, cb_size = " + cb_end);
        do { //do_next_sb:
            if (cb_pos == cb_end || src.getUnsignedShort(cb_pos) == 0 || dest_pos == dest_end) {
                debug("Completed. Returning success (0).");
                break;
            }

            /* Setup offset for the current sub-block destination. */
            dest_sb_start = dest_pos;
            dest_sb_end = dest_pos + NTFS_SB_SIZE;

            /* Check that we are still within allowed boundaries. */
            if (dest_sb_end > dest_end) {
                throw new IOException("Decompression error - overflow");
            }

            /* Does the minimum size of a compressed sb overflow valid range? */
            if (cb_pos + 6 > cb_end) {
                throw new IOException("Decompression error - overflow");
            }

            /* Setup the current sub-block source pointers and validate range. */
            cb_sb_start = cb_pos;
            cb_sb_end = cb_sb_start + (src.getUnsignedShort(cb_pos) & NTFS_SB_SIZE_MASK) + 3;
            if (cb_sb_end > cb_end) {
                throw new IOException("Decompression error - overflow");
            }

            /* Now, we are ready to process the current sub-block (sb). */
            if ((src.getUnsignedShort(cb_pos) & NTFS_SB_IS_COMPRESSED) == 0) { //nao comprimido
                debug("Found uncompressed sub-block.");
                /* This sb is not compressed, just copy it into destination. */
                /* Advance source position to first data byte. */
                cb_pos += 2;
                /* An uncompressed sb must be full size. */
                if (cb_sb_end - cb_pos != NTFS_SB_SIZE) {
                    throw new IOException("Decompression error - overflow");
                }
                /* Copy the block and advance the source position. */
                //System.arraycopy(src, cb_pos, dest, dest_pos, NTFS_SB_SIZE);
                src.get(dest, cb_pos, dest_pos, NTFS_SB_SIZE);
                //memcpy(dest, cb, NTFS_SB_SIZE);
                cb_pos += NTFS_SB_SIZE;
                /* Advance destination position to next sub-block. */
                dest_pos += NTFS_SB_SIZE;
                //continue;
            } else {
                cb_pos += 2;
                do { //do_next_tag
                    if (cb_pos == cb_sb_end) {
                        /* Check if the decompressed sub-block was not full-length. */
                        if (dest_pos < dest_sb_end) {
                            int nr_bytes = dest_sb_end - dest_pos;
                            debug("Filling incomplete sub-block with zeroes.");

                            /* Zero remainder and update destination position. */
                            Arrays.fill(dest, dest_pos, dest_pos + nr_bytes, (byte) 0);
                            dest_pos += nr_bytes;

                        }
                        //goto do_next_sb:
                        break;
                    } else {

                        /* Check we are still in range. */
                        if (cb_pos > cb_sb_end || dest_pos > dest_sb_end) {
                            throw new IOException("Decompression error - overflow");
                        }
                        /* Get the next tag and advance to first token. */
                        tag = src.get(cb_pos++);
                        /* Parse the eight tokens described by the tag. */


                        /* Parse the eight tokens described by the tag. */
                        for (token = 0; token < 8; token++, tag >>= 1) {
                            int lg, pt, length, max_non_overlap;
                            int i;
                            int dest_back_addr;

                            /* Check if we are done / still in range. */
                            if (cb_pos >= cb_sb_end || dest_pos > dest_sb_end) {
                                break;
                            }
                            /* Determine token type and parse appropriately.*/
                            if ((tag & NTFS_TOKEN_MASK) == NTFS_SYMBOL_TOKEN) {
                                /*
                                 * We have a symbol token, copy the symbol across, and
                                 * advance the source and destination positions.
                                 */
                                dest[dest_pos++] = src.get(cb_pos++);
                                //*  dest++ = *  cb++;
                                /* Continue with the next token. */
                                continue;
                            }
                            /*
                             * We have a phrase token. Make sure it is not the first tag in
                             * the sb as this is illegal and would confuse the code below.
                             */
                            if (dest_pos == dest_sb_start) {
                                throw new IOException("Decompression error - overflow");
                            }
                            /*
                             * Determine the number of bytes to go back (p) and the number
                             * of bytes to copy (l). We use an optimized algorithm in which
                             * we first calculate log2(current destination position in sb),
                             * which allows determination of l and p in O(1) rather than
                             * O(n). We just need an arch-optimized log2() function now.
                             */
                            lg = 0;
                            for (i = dest_pos - dest_sb_start - 1; i >= 0x10; i >>= 1) {
                                lg++;
                            }
                            /* Get the phrase token into i. */
                            pt = src.getUnsignedShort(cb_pos);
                            //pt = le16_to_cpu(src, cb_pos);
                            /*
                             * Calculate starting position of the byte sequence in
                             * the destination using the fact that p = (pt >> (12 - lg)) + 1
                             * and make sure we don't go too far back.
                             */
                            dest_back_addr = dest_pos - (pt >> (12 - lg)) - 1;
                            if (dest_back_addr < dest_sb_start) {
                                throw new IOException("Decompression error - overflow");
                            }
                            /* Now calculate the length of the byte sequence. */
                            length = (pt & (0xfff >> lg)) + 3;
                            /* Verify destination is in range. */
                            if (dest_pos + length > dest_sb_end) {
                                throw new IOException("Decompression error - overflow");
                            }
                            /* The number of non-overlapping bytes. */
                            max_non_overlap = dest_pos - dest_back_addr;
                            if (length <= max_non_overlap) {
                                /* The byte sequence doesn't overlap, just copy it. */
                                System.arraycopy(dest, dest_back_addr, dest, dest_pos, length);
                                //memcpy(dest, dest_back_addr, length);
                                /* Advance destination pointer. */
                                dest_pos += length;
                            } else {
                                /*
                                 * The byte sequence does overlap, copy non-overlapping
                                 * part and then do a slow byte by byte copy for the
                                 * overlapping part. Also, advance the destination
                                 * pointer.
                                 */
                                //memcpy(dest, dest_back_addr, max_non_overlap);
                                System.arraycopy(dest, dest_back_addr, dest, dest_pos, max_non_overlap);
                                dest_pos += max_non_overlap;
                                dest_back_addr += max_non_overlap;
                                length -= max_non_overlap;
                                while (length-- != 0) {
                                    dest[dest_pos++] = dest[dest_back_addr++];
                                    //*  dest++ = *  dest_back_addr++;
                                }
                            }
                            /* Advance source position and continue with the next token. */
                            cb_pos += 2;
                        }
                        /* No tokens left in the current tag. Continue with the next tag. */
                        //goto do_next_tag;
                    }
                } while (true);
            }

        } while (true);

        return 0;
    }
}
