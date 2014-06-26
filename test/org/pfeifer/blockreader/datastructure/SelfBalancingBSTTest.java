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
package org.pfeifer.blockreader.datastructure;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class SelfBalancingBSTTest {

    public SelfBalancingBSTTest() {

    }
    
    private SelfBalancingBST<Integer> tree1;

    @Before
    public void setUp() {
        tree1 = new SelfBalancingBST<>();
        tree1.put(1, 1);
        tree1.put(2, 2);
        tree1.put(3, 3);
        tree1.put(4, 4);
        tree1.put(5, 5);
        tree1.put(6, 6);
        tree1.put(7, 7);
        tree1.put(11, 11);
        tree1.put(12, 12);
        tree1.put(13, 13);
        tree1.put(14, 14);
        tree1.put(15, 15);
        tree1.put(16, 16);
        tree1.put(17, 17);

    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSearchLessOrEqual() {
        System.out.println("searchLessOrEqual");

        Integer i1 = tree1.searchLessOrEqual(1);
        Integer i3 = tree1.searchLessOrEqual(3);
        Integer i8 = tree1.searchLessOrEqual(8);
        Integer i10 = tree1.searchLessOrEqual(10);
        Integer i11 = tree1.searchLessOrEqual(11);
        Integer i12 = tree1.searchLessOrEqual(12);
        Integer i17 = tree1.searchLessOrEqual(17);

        assertEquals(i1, (Integer) 1);
        assertEquals(i3, (Integer) 3);
        assertEquals(i8, (Integer) 7);
        assertEquals(i10, (Integer) 7);
        assertEquals(i11, (Integer) 11);
        assertEquals(i12, (Integer) 12);
        assertEquals(i17, (Integer) 17);

    }

}
