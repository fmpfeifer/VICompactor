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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.pfeifer.blockreader.ByteArrayBlockReader;
import org.pfeifer.blockreader.BlockReader;
import org.pfeifer.blockreader.LimitBlockReader;
import org.pfeifer.blockreader.ZeroBlockReader;
import org.pfeifer.vfs.VFSAttributes;
import org.pfeifer.vfs.VFSObject;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 */
public class MFTFile implements VFSObject {

    // TODO: Implement parsing $ATTRIBUTE_LIST
    // MFTRecord record;
    NTFSVolume volume;
    //private List<MFTRecordAttribute> attributes = new ArrayList<MFTRecordAttribute>();
    private long mftIndex;
    //private int sequenceNumber;
    private int offsetFirstAttribute;
    private int flags;
    //private int parentIndex;
    private long baseRecord;
    private int nextAttrId;
    private String fileName = "";
    private long parentDirectory = -1;
    private boolean valid = false;
    FixupBlockReader mftRecordReader;
    private long fileSize = -1;
    private WeakReference<MFTDataAttribute> unnamedDataAttribute = null;
    private Long[] childRecords = null;
    //private boolean hasSubitems = false;

    public MFTFile(NTFSVolume volume, long recordEntry) throws IOException {
        //this.volume = volume;
        //record = volume.getMFT().getRecord(recordEntry);
        this.volume = volume;
        this.mftIndex = recordEntry;
        int mftRecordSize = volume.getMftRecordSize();
        mftRecordReader = new FixupBlockReader(volume.getMFT().getTotalFileData(), recordEntry * mftRecordSize, mftRecordSize);

        parse();
    }

    /**
     * constructor used to create the MFT File, before having the MFT
     * this constructor make a MFTFile carving the MFT Record directly
     * from disk, given the offset found in the $Boot file.
     */
    MFTFile(NTFSVolume volume, boolean createMFTFile /*ignored*/) throws IOException {
        this.volume = volume;
        BlockReader blockData = volume.getDataBlock();
        int recordSize = volume.getMftRecordSize();
        long offset = volume.getFirstClusterOfMft() * volume.getClusterSize();
        byte[] data = new byte[recordSize];

        if (blockData.get(data, offset) == recordSize) {
            //record = new MFTRecord(volume, data, 0); //creates the record for entry 0 - $MFT

            this.mftIndex = 0;
            int mftRecordSize = volume.getMftRecordSize();

            mftRecordReader = new FixupBlockReader(new ByteArrayBlockReader(data), 0, mftRecordSize);
            parse();
        }
    }

    private void parse() throws IOException {
        String file = mftRecordReader.getString(0, 4);
        if (file.equals("FILE")) {
            valid = true;
            mftRecordReader.activateFixup();
        } else {
            return;
        }

        //sequenceNumber = data.getShort(0x10);
        offsetFirstAttribute = mftRecordReader.getShort(0x14);
        flags = mftRecordReader.getShort(0x16);
        //baseRecord = data.getLong(0x20);
        baseRecord = mftRecordReader.getNumber(0x20, 6);
        nextAttrId = mftRecordReader.getShort(0x28);

        if (baseRecord == 0) {
            List<MFTRecordAttribute> attrList = parseAttributes(0x20); // find $attr_list
            if (attrList.size() > 0) {
                childRecords = ((MFTAttributeList) attrList.get(0)).getChildRecords(mftIndex);
            }
        } else {
            valid = false; //consider child mft records not valid
        }

        if (valid) {
            //parseAttributes(mftRecordReader);
            List<MFTRecordAttribute> attrs = parseAttributes(0x30); //parse filename attributes
            MFTFileNameAttribute fileNameAttribute = null;
            for (MFTRecordAttribute attr : attrs) {
                if (fileNameAttribute == null || fileNameAttribute.getFileName() == null ||
                        "".equals(fileNameAttribute.getFileName())) {
                    fileNameAttribute = (MFTFileNameAttribute) attr;
                } else {
                    int oldns = fileNameAttribute.getFileNamespace();
                    int newns = ((MFTFileNameAttribute) attr).getFileNamespace();
                    if (oldns == 2) {
                        oldns = 3;
                    } else if (oldns == 3) {
                        oldns = 2;
                    }
                    if (newns == 2) {
                        newns = 3;
                    } else if (newns == 3) {
                        newns = 2;
                    }
                    if (newns < oldns) {
                        fileNameAttribute = (MFTFileNameAttribute) attr;
                    }
                }
            }
            if (fileNameAttribute != null) {
                fileName = fileNameAttribute.getFileName();
                parentDirectory = fileNameAttribute.getParentDirectory();
            }
        }
    }

    /*private void parseAttributes(BlockReader data) throws IOException {
    int offset = offsetFirstAttribute;
    int length;
    int attrType = data.getInt(offset);

    while (attrType != 0xffffffff) {
    length = data.getInt(offset + 0x04);
    LimitBlockReader attrData = new LimitBlockReader(data, offset, length);
    MFTRecordAttribute attr = buildAttribute(attrData, attrType);
    if (attr != null) {
    //attributes.add(attr);
    }
    offset += length;
    attrType = data.getInt(offset);
    }
    }*/
    private List<MFTRecordAttribute> parseAttributes(int attrType) throws IOException {
        List<MFTRecordAttribute> resp = new ArrayList<>(2);
        int offset = offsetFirstAttribute;
        int length;
        int thisAttrType = mftRecordReader.getInt(offset);

        while (thisAttrType != 0xffffffff) {
            length = mftRecordReader.getInt(offset + 0x04);
            if (attrType == thisAttrType) {
                LimitBlockReader attrData = new LimitBlockReader(mftRecordReader, offset, length);
                switch (attrType) {
                    case 0x30: //$FILENAME_ATTRIBUTE
                        resp.add(new MFTFileNameAttribute(volume, attrData));
                        break;

                    case 0x80: //$DATA_ATTRIBUTE
                        resp.add(new MFTDataAttribute(volume, attrData));
                        break;

                    case 0x20: //$ATTRIBUTE_LIST
                        resp.add(new MFTAttributeList(volume, attrData));
                        break;
                }
            }
            offset += length;
            thisAttrType = mftRecordReader.getInt(offset);
        }

        if (childRecords != null) {
            for (long record : childRecords) {
                MFTFile f = new MFTFile(volume, record);
                resp.addAll(f.parseAttributes(attrType));
            }
        }

        return resp;
    }

    /*private MFTRecordAttribute buildAttribute(BlockReader attrData, int attrType) throws IOException {
    MFTRecordAttribute attr = null;

    switch (attrType) {
    case 0x30: //$FILE_NAME
    attr = new MFTFileNameAttribute(volume, attrData);
    if (fileNameAttribute == null || fileNameAttribute.getFileName() == null ||
    "".equals(fileNameAttribute.getFileName())) {
    fileNameAttribute = (MFTFileNameAttribute) attr;
    } else {
    int oldns = fileNameAttribute.getFileNamespace();
    int newns = ((MFTFileNameAttribute) attr).getFileNamespace();
    if (oldns == 2) {
    oldns = 3;
    } else if (oldns == 3) {
    oldns = 2;
    }
    if (newns == 2) {
    newns = 3;
    } else if (newns == 3) {
    newns = 2;
    }
    if (newns < oldns) {
    fileNameAttribute = (MFTFileNameAttribute) attr;
    }
    }
    break;
    case 0x80: //$DATA
    MFTDataAttribute da = new MFTDataAttribute(volume, attrData);
    attr = da;
    if (da.getAttrName() == null || da.getAttrName().equals("")) {
    dataAttr = da; //get unamed data attribute
    }
    break;
    }

    return attr;
    }*/
    public boolean isInUse() {
        return (flags & 0x01) != 0;
    }

    @Override
    public boolean isDirectory() {
        return (flags & 0x02) != 0;
    }

    @Override
    public String getFileName() {
        String resp = "";
        if (isValid()) {
            /*if (fileNameAttribute != null && fileNameAttribute.getFileName() != null) {
            resp = fileNameAttribute.getFileName();
            }*/
            return fileName;
        }
        return resp;
    }

    @Override
    public BlockReader getTotalFileData() throws IOException {
        BlockReader fileData = null;

        MFTDataAttribute da = getUnnamedDataAttribute();
        if (getUnnamedDataAttribute() != null) {
            fileData = da.getFileData();
        }

        return fileData;
    }

    private MFTDataAttribute getUnnamedDataAttribute() throws IOException {
        MFTDataAttribute resp = null;

        if (unnamedDataAttribute != null) {
            resp = unnamedDataAttribute.get();
        }

        if (resp == null) {
            List<MFTRecordAttribute> dataAttributes = parseAttributes(0x80); // get data attributes
            for (MFTRecordAttribute attr : dataAttributes) {
                MFTDataAttribute da = (MFTDataAttribute) attr;
                if (da.getAttrName() == null || da.getAttrName().equals("")) {
                    resp = da;
                    fileSize = da.getAttrLength();
                    break;
                }
            }

            unnamedDataAttribute = new WeakReference<>(resp);
        }


        return resp;
    }

    private List<MFTDataAttribute> getNamedDataAttributes() throws IOException {
        List<MFTDataAttribute> resp = new ArrayList<>(2);

            List<MFTRecordAttribute> dataAttributes = parseAttributes(0x80); // get data attributes
            for (MFTRecordAttribute attr : dataAttributes) {
                MFTDataAttribute da = (MFTDataAttribute) attr;
                if (da.getAttrName() != null && !da.getAttrName().equals("")) {
                    resp.add(da);
                }
            }


        return resp;
    }

    public boolean isValid() {
        return valid;
    }

    /**
     * @return the parentIndex
     */
    public long getParentDirectory() {
        return parentDirectory;
    }

    @Override
    public long getSize() throws IOException {
        if (fileSize < 0) {
            getUnnamedDataAttribute();
            if (fileSize < 0) {
                fileSize = 0;
            }
        }
        return fileSize;
    }

    /// FIXME: implement
    @Override
    public VFSObject getParent() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasSubItems() throws IOException {
        return getNamedDataAttributes().size() > 0;
    }

    @Override
    public VFSObject getSubitem(int index) throws IOException {
        return new MFTAlternateDataStream(this, getNamedDataAttributes().get(index));
    }

    @Override
    public int getSubitemCount() throws IOException{
        return getNamedDataAttributes().size();
    }

    @Override
    public BlockReader getFileSlack() throws IOException {
        BlockReader totalFileData = getTotalFileData();
        long size = getSize();
        BlockReader slack;
        if (totalFileData.length() > size) {
            slack = new LimitBlockReader(totalFileData, size, totalFileData.length() - size);
        } else {
            slack = new ZeroBlockReader(0);
        }
        return slack;
    }

    @Override
    public BlockReader getFileData() throws IOException {
        BlockReader totalFileData = getTotalFileData();
        long size = getSize();
        return new LimitBlockReader(totalFileData, 0, size);
    }

    @Override
    public VFSAttributes getAttributes() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
