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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class ConcatenatedBlockReaderTest {
    
    public ConcatenatedBlockReaderTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

        /**
     * Test of length method, of class ConcatenatedBlockReader.
     * @throws java.io.IOException
     */
    @Test
    public void testLength() throws IOException {
        System.out.println("length");
        ConcatenatedBlockReader instance = new ConcatenatedBlockReader(new BlockReader[] {
            new ZeroBlockReader(10), new ZeroBlockReader(10), new ZeroBlockReader(15)
        });
        long expResult = 35L;
        long result = instance.length();
        assertEquals(expResult, result);
    }

    
    /**
     * Test of get method, of class ConcatenatedBlockReader.
     * @throws java.io.IOException
     */
    @Test
    public void testGet_4args() throws IOException {
        System.out.println("get");
        byte[] resp = new byte[16];
        String p1 = "teste";
        String p2 = "block";
        String p3 = "reader";
        ConcatenatedBlockReader instance = new ConcatenatedBlockReader(new BlockReader[] {
            new ByteArrayBlockReader(p1.getBytes()), new ByteArrayBlockReader(p2.getBytes()),
            new ByteArrayBlockReader(p3.getBytes())
        });
        instance.get(resp, 0L, 0, (int) instance.length());
        assertEquals(new String(resp), "testeblockreader");
    }

    /**
     * Test of blockIsZero method, of class ConcatenatedBlockReader.
     * @throws java.io.IOException
     */
    @Test
    public void testBlockIsZero() throws IOException {
        System.out.println("blockIsZero");
        ZeroBlockReader p1 = new ZeroBlockReader(5);
        ZeroBlockReader p2 = new ZeroBlockReader(5);
        ByteArrayBlockReader p3 = new ByteArrayBlockReader("12345".getBytes());
        ZeroBlockReader p4 = new ZeroBlockReader(5);
        ConcatenatedBlockReader instance = new ConcatenatedBlockReader(new BlockReader[]{
            p1, p2, p3, p4
        });
        assertEquals(instance.blockIsZero(0, 6), true);
        assertEquals(instance.blockIsZero(4, 3), true);
        assertEquals(instance.blockIsZero(7,3), true);
        assertEquals(instance.blockIsZero(8, 4), false);
        assertEquals(instance.blockIsZero(15, 2), true);
        assertEquals(instance.blockIsZero(12, 7), false);
        assertEquals(instance.blockIsZero(1, 12), false);
        assertEquals(instance.blockIsZero(0, 10), true);
    }
    
}
