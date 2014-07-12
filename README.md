VICompactor
===========

VICompactor is a java tool to compact Virtual Disk Images.
Now it is able to compact VirtualBox VDI images.
The program understands the partitions and filesystems inside the virtual disk, and, using the allocation bitmap, discards all the unused data inside the file.
In the current version, the program can only understand NTFS and ext4 filesystems, but other filesystems (like ext2/3 and FAT32) are in the TODO list.

Limitations:
 - only understands NTFS and ext4; ext2/3 should work, but is untested.
 - No command line interface yet (planned).
 - It creates a new file, backing up the old one with a "_orig" suffix. In place compact is planned.