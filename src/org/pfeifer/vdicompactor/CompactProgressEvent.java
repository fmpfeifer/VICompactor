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
package org.pfeifer.vdicompactor;

import java.util.EventObject;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class CompactProgressEvent extends EventObject {

    private long totalSize = 0;
    private int completed = 0;
    
    public CompactProgressEvent(Object source) {
        super(source);
    }
    
    public CompactProgressEvent(Object source, long totalSize, int completed) {
        super(source);
        this.totalSize = totalSize;
        this.completed = completed;
    }

    /**
     * @return the totalSize
     */
    public long getTotalSize() {
        return totalSize;
    }

    /**
     * @param totalSize the totalSize to set
     */
    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    /**
     * @return the completed
     */
    public int getCompleted() {
        return completed;
    }

    /**
     * @param completed the completed to set
     */
    public void setCompleted(int completed) {
        this.completed = completed;
    }
    
}
