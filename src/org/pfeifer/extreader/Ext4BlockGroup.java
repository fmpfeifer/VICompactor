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

package org.pfeifer.extreader;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class Ext4BlockGroup {

    private static Set<Long> powersOf357 = null;
    private final Ext4Volume volume;
    private final long blockGroupNumber;
    private final long blocksInGroup;
    
    public Ext4BlockGroup(Ext4Volume volume, long number) {
        this.volume = volume;
        this.blockGroupNumber = number;
        blocksInGroup = volume.getSuperBlock().getBlocksPerGroup();
    }    

    private static boolean hasSuperBlockCopy(long number) {
        return number == 0 || getPowersOf357().contains(number);
    }
    
    private static Set<Long> getPowersOf357() {
        if (powersOf357 == null) {
            powersOf357 = new HashSet<>();
            putPowersOf(powersOf357, 3);
            putPowersOf(powersOf357, 5);
            putPowersOf(powersOf357, 7);
        }
        return powersOf357;
    }
    
    private static void putPowersOf(Set<Long> where, long base) {
        long n = 1;
        while (n < Long.MAX_VALUE / base) {
            where.add(n);
            n *= base;
        }
    }
}
