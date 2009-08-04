/**
 * This package contains classes useful for reading in Palm OS database files
 * (PDB and PRC) and accessing the components and data within. PalmIO is part of
 * the Weasel Reader project, but it is a separate package and does not require
 * Weasel Reader.<br>
 * <br>
 * $Id$<br>
 * <br>
 * Copyright (C) 2008-2009 John Gruenenfelder<br>
 * johng@as.arizona.edu<br>
 * <a href="http://weaselreader.org/PalmIO">PalmIO web site</a><br>
 * <br>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.<br>
 * <br>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.<br>
 * <br>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.WeaselReader.PalmIO;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;



/**
 * Base class for reading Palm database files. PalmDB extends RandomAccessFile
 * because it needs the various readXXX methods, as in DataInputStream, but it
 * is also necessary to randomly seek which is not available in DataInputStream.
 * <br><br>
 * This class is useful for only the basic structure inherent in any Palm
 * database. New classes should extend this class to handle features/formats
 * specific to databases used by applications, such as the ZtxtDB class which
 * reads zTXT Palm databases (which are read by Weasel Reader and others).<br>
 * <br>
 * The Palm database header is a packed structure of Big Endian values. Its
 * structure conforms to the following C struct definitions:<br>
 * <br>
 * typedef struct {<br>
 * UInt8 name[dmDBNameLength];<br>
 * UInt16 attributes;<br>
 * UInt16 version;<br>
 * UInt32 creationDate;<br>
 * UInt32 modificationDate;<br>
 * UInt32 lastBackupDate;<br>
 * UInt32 modificationNumber;<br>
 * LocalID appInfoID;<br>
 * LocalID sortInfoID;<br>
 * UInt32 type;<br>
 * UInt32 creator;<br>
 * UInt32 uniqueIDSeed;<br>
 * RecordListType recordList;<br>
 * } DatabaseHdrType;<br>
 *<br>
 * typedef struct {<br>
 * LocalID nextRecordListID;<br>
 * UInt16 numRecords;<br>
 * UInt16 firstEntry;<br>
 * } RecordListType;<br>
 *<br>
 * typedef struct {<br>
 * LocalID localChunkID;<br>
 * UInt8 attributes;<br>
 * UInt8 uniqueID[3];<br>
 * } RecordEntryType;<br>
 *<br>
 * All of these values are unsigned. A LocalID is a type of Palm OS pointer
 * which is equivalent to a UInt32. See the comments on the private fields of
 * this class for more information on what these values mean.
 * 
 * @author John Gruenenfelder
 * @version $Id$
 */
public class PalmDB extends RandomAccessFile {

  /**
   * Database is read-only while in Palm memory.
   */
  public static final int   DB_FLAG_READ_ONLY        = 0x0002;


  /**
   * Set if the appInfoArea has changed. Treated separately because the
   * appInfoArea is not a normal database record. This flag should never be set
   * in a database file; it has meaning only on a device.
   */
  public static final int   DB_FLAG_DIRTY_APPINFO    = 0x0004;


  /**
   * Set if the database should be backed up during the next sync procedure.
   */
  public static final int   DB_FLAG_BACKUP           = 0x0008;


  /**
   * Set if it is acceptable for a database with the same name but higher
   * version number to be installed over this database.
   */
  public static final int   DB_FLAG_NEWER_OKAY       = 0x0010;


  /**
   * Set if the device must be reset after this database has been installed.
   */
  public static final int   DB_FLAG_RESET_ON_INSTALL = 0x0020;


  /**
   * Set if the device should not permit this database to be beamed or copied to
   * another device. A form of simple and easily bypassed copy control.
   */
  public static final int   DB_FLAG_NO_COPY          = 0x0040;


  /**
   * If set, Record is marked as "secret" and some Palm apps can selectively
   * hide secret records unless a password is entered. Just a convenience
   * feature as it offers no real security. This is the only record flag which
   * should ever need to be (optionally) set in the database file. The other
   * flags only have meaning at runtime on the device.
   */
  public static final short REC_FLAG_SECRET          = 0x10;


  /**
   * On a device, it means the record is open for writing. Should never be set
   * for a record in a database file.
   */
  public static final short REC_FLAG_BUSY            = 0x20;


  /**
   * Set if the record has been modified and must be backed up during the next
   * sync procedure.
   */
  public static final short REC_FLAG_DIRTY           = 0x40;


  /**
   * Set if the record has been marked for deletion. During the next sync
   * procedure, this record will be removed from the on-device database and will
   * be saved in some other location on the PC side.
   */
  public static final short REC_FLAG_DELETE          = 0x80;


  /**
   * The length of the database name field in the Palm OS database header.
   */
  public static final int   DB_NAME_LENGTH           = 0x20;


  /**
   * The length of a Palm OS database header up to and including the numRecords
   * field. Does not include the length of the record entry array which follows.
   */
  public static final int   DB_HEADER_LENGTH         = DB_NAME_LENGTH + 0x30;


  /**
   * This is the offset from the Palm OS epoch to the standard UNIX epoch. It
   * corresponds to 2,082,844,800 seconds. Add this value to a UNIX time value
   * to get the corresponding Palm OS time value.
   */
  public static final int   PALM_CTIME_OFFSET        = 0x7C25B080;


  /**
   * The Palm database name. Required to be unique on a Palm OS device and
   * limited to a length of 0x20 characters. Name may occupy all 0x20 bytes in
   * which case it will not be NUL terminated.
   */
  private String            dbName;


  /**
   * Palm database flags. Stored in a UInt16.
   */
  private int               flags;


  /**
   * Database version number, defined by the application. Stored in a UInt16.
   */
  private int               version;


  /**
   * Creation time in seconds of the database. Time is based on the Palm OS
   * epoch of January 1, 1904. Value is stored in a UInt32.
   */
  private long              creationTime;


  /**
   * Last modification time in seconds of the database. Time is based on the
   * Palm OS epoch of January 1, 1904. Value is stored in a UInt32.
   */
  private long              modificationTime;


  /**
   * Time of the last database backup in seconds. Time is based on the Palm OS
   * epoch of January 1, 1904. Value is stored in a UInt32.
   */
  private long              lastBackupTime;


  /**
   * Database's modification number, defined by the application. Value is stored
   * in a UInt32.
   */
  private long              modificationNumber;


  /**
   * Byte offset from beginning of the file to the appInfoArea block. If one is
   * present it will be the first block in the data area. Rarely used. Value
   * type is a "LocalID" stored in a UInt32.
   */
  private long              applicationInfoIDPtr;


  /**
   * Byte offset from beginning of the file to the sortInfoArea block. If one is
   * present it will occur immediately after the appInfoArea (if present).
   * Rarely, if ever, used and Palm OS does not support backing up this area
   * during a sync. Value type is a "LocalID" stored in a UInt32.
   */
  private long              sortInfoIDPtr;


  /**
   * Palm OS database type ID string. Stored as a four byte literal, such as
   * 'zTXT' or 'appl'.
   */
  private long              dbTypeID;


  /**
   * Palm OS database creator ID string. Stored as a four byte literal, such as
   * 'GPlm' or 'PALM'.
   */
  private long              dbCreatorID;


  /**
   * Used as the base index for the record LocalID values. Record uniqueIDs are
   * not guaranteed to persist through a sync or backup so this value is fairly
   * arbitrary. Value is stored in a UInt32.
   */
  private long              uniqueIDSeed;


  /**
   * In memory a Palm database can have multiple record list structures, though
   * in practice there is never more than one. This would be the localID of the
   * next record list, but in a database file it should always be 0. Value type
   * is a "LocalID" stored in a UInt32.
   */
  private long              nextRecordListIDPtr;


  /**
   * Number of records in this database. Stored in a UInt16.
   */
  private int               numRecords;


  /**
   * This array contains the byte offsets for the beginning of each record in
   * this database. Offsets are counted from the start of the file. Value type
   * is a "LocalID" stored in a UInt32.
   */
  private Vector<Long>      recordOffsets;


  /**
   * This array contains the attribute flags for each record in the database.
   * See the REC_FLAG_* values for the meanings of these flags. Value is stored
   * in a byte after the record's offset value.
   */
  private Vector<Integer>   recordFlags;


  /**
   * This array contains the unique IDs for each record in the database. For a
   * newly created database file, these will all be zero. On the device, each of
   * these values will be unique. For a database backed up off a device, these
   * values may or may not be set. In any case, they have meaning only to Palm
   * OS and should not be used by anyone else. Value type is a "LocalID" and,
   * together with the recordFlags byte, is stored in a single UInt32. The
   * recordFlags byte occupies the MSB and the uniqueID bytes occupy the three
   * LSBs.
   */
  private Vector<Integer>   recordIDs;



  /**
   * Create a complete PalmDB object from the contents of the specified pdb
   * file.
   * 
   * @param pdbFile an existing pdb file to load and parse into a PalmDB.
   * @throws IOException if an error occurs while reading the header.
   */
  public PalmDB(File pdbFile) throws IOException
    {
      super(pdbFile, "r");
      readHeader();
    }



  /**
   * @return the database name
   */
  public String getDbName()
    {
      return dbName;
    }



  /**
   * @return the database flags
   */
  public int getFlags()
    {
      return flags;
    }



  /**
   * @return the database version
   */
  public int getVersion()
    {
      return version;
    }



  /**
   * @return the database creation time in seconds, measured from the standard
   *         Palm OS epoch
   */
  public long getCreationTime()
    {
      return creationTime;
    }



  /**
   * @return the database modification time in seconds, measured from the
   *         standard Palm OS epoch
   */
  public long getModificationTime()
    {
      return modificationTime;
    }



  /**
   * @return the time of the last database backup in seconds, measured from the
   *         standard Palm OS epoch
   */
  public long getLastBackupTime()
    {
      return lastBackupTime;
    }



  /**
   * @return the application defined modification number
   */
  public long getModificationNumber()
    {
      return modificationNumber;
    }



  /**
   * @return the database type ID, stored as an int but interpreted as a four
   *         byte character literal, such as 'appl'
   */
  public long getDbTypeID()
    {
      return dbTypeID;
    }



  /**
   * @return the database creator ID, stored as an int but interpreted as a four
   *         byte character literal, such as 'GPlm'
   */
  public long getDbCreatorID()
    {
      return dbCreatorID;
    }



  /**
   * @return the unique ID seed used by Palm OS when generating record unique ID
   *         values
   */
  public long getUniqueIDSeed()
    {
      return uniqueIDSeed;
    }



  /**
   * @return the number of records in this database
   */
  public int getNumRecords()
    {
      return numRecords;
    }



  /**
   * @return the applicationInfoID pointer for this database
   */
  public long getApplicationInfoIDPtr()
    {
      return applicationInfoIDPtr;
    }



  /**
   * @return the sortInfoID pointer for this database
   */
  public long getSortInfoIDPtr()
    {
      return sortInfoIDPtr;
    }



  /**
   * @return the LocalID of the next record list structure in this database
   */
  public long getNextRecordListIDPtr()
    {
      return nextRecordListIDPtr;
    }



  /**
   * @return the vector of record byte offsets with one element for each record
   *         in this database
   */
  public Vector<Long> getRecordOffsets()
    {
      return recordOffsets;
    }



  /**
   * @return the vector of record flags with one element for each database
   *         record
   */
  public Vector<Integer> getRecordFlags()
    {
      return recordFlags;
    }



  /**
   * @return the vector of unique record ID values with one element for each
   *         record in this database
   */
  public Vector<Integer> getRecordIDs()
    {
      return recordIDs;
    }



  /**
   * Read the specified record and return it as a byte array.
   * 
   * @param recIndex the zero-based record index to read.
   * @return the requested record data in a byte array.
   * @throws IOException if an I/O error occurs such as all bytes of a record
   *           not being read in.
   * @throws ArrayIndexOutOfBoundsException if the requested record index does
   *           not exist.
   */
  public byte[] readRecord(int recIndex) throws IOException,
                                        ArrayIndexOutOfBoundsException
    {
      if ((recIndex < 0) || (recIndex >= numRecords))
        throw new ArrayIndexOutOfBoundsException("readRecord(" + recIndex
            + "): record index is out of bounds.");

      // Seek to the start of the given record
      seek(recordOffsets.get(recIndex));

      // Compute size of the record
      int recSize;
      if (recIndex < (numRecords - 1))
        {
          // Record is not the last so its size can be computed from the
          // starting offset of the following record.
          recSize =
              (int) (recordOffsets.get(recIndex + 1) - recordOffsets
                  .get(recIndex));
        }
      else
        {
          // The last record in the DB occupies the rest of the space in the
          // file.
          recSize = (int) (length() - recordOffsets.get(recIndex));
        }

      // Read the record data
      byte[] recBytes = new byte[recSize];
      if (read(recBytes) != recSize)
        {
          throw new IOException("readRecord(" + recIndex
              + "): failed to read all bytes in record.");
        }

      return recBytes;
    }



  /**
   * Read an unsigned 32 bit integer from the input stream and zero extend it to
   * a long to not lose any precision.
   * 
   * @return an unsigned 32 bit int zero extended to a long.
   * @throws IOException for any I/O error while reading the input stream.
   */
  public long readUInt32() throws IOException
    {
      byte[] uintBytes = new byte[4];
  
      if (read(uintBytes) != uintBytes.length)
        throw new IOException("readUInt32: failed to read four input bytes.");
  
      return Utility.fromUInt32(uintBytes, 0);
    }



  /**
   * Read a three byte record unique ID value and return it as an int value.
   * 
   * @return a three byte unique ID stored in an int.
   * @throws IOException for any I/O error while reading the input stream.
   */
  public int readUniqueID() throws IOException
    {
      byte[] uniqueIDBytes = new byte[3];
      byte[] intBytes = new byte[4];
  
      if (read(uniqueIDBytes) != uniqueIDBytes.length)
        throw new IOException(
            "readUniqueID: failed to read three input bytes.");
  
      // Both Palm OS and Java are Big Endian so the following int bit
      // manipulation is safe.
      intBytes[0] = 0;
      intBytes[1] = uniqueIDBytes[0];
      intBytes[2] = uniqueIDBytes[1];
      intBytes[3] = uniqueIDBytes[2];
  
      return (int) Utility.fromUInt32(intBytes, 0);
    }



  /**
   * Show something semi-useful for this Palm database when the object is
   * printed.
   *
   * @return a String representation of this database.
   */
  @Override
  public String toString()
    {
      return "Palm database \"" + dbName + "\"";
    }



  /**
   * Read in a Palm OS database header and set the appropriate fields with data
   * from the header. This method will read all values up to the number of
   * records field as well as the record entry array which follows. This data is
   * common to both PDB and PRC files.
   * 
   * @throws IOException if an I/O error occurs while reading the header.
   */
  private void readHeader() throws IOException
    {
      // RandomAccessFile dataIn = pdbDataIn;
  
      // Read the database name
      byte[] name = new byte[DB_NAME_LENGTH];
      if (read(name) != DB_NAME_LENGTH)
        throw new IOException(
            "readHeader: failed to read all bytes in DB title");
      dbName = new String(name);
  
      // Read all the 16/32 bit values
      flags = readUnsignedShort();
      version = readUnsignedShort();
      creationTime = readUInt32();
      modificationTime = readUInt32();
      lastBackupTime = readUInt32();
      modificationNumber = readUInt32();
      applicationInfoIDPtr = readUInt32();
      sortInfoIDPtr = readUInt32();
      dbTypeID = readUInt32();
      dbCreatorID = readUInt32();
      uniqueIDSeed = readUInt32();
      nextRecordListIDPtr = readUInt32();
      numRecords = readUnsignedShort();
  
      // Following the numRecords field is an array of six byte structures,
      // one element for each record in the database.
      int n = numRecords;
      recordOffsets = new Vector<Long>(n);
      recordFlags = new Vector<Integer>(n);
      recordIDs = new Vector<Integer>(n);
  
      for (int i = 0; i < n; i++)
        {
          recordOffsets.add(readUInt32());
          recordFlags.add(readUnsignedByte());
          recordIDs.add(readUniqueID());
        }
    }



  /**
   * Useful for loading and testing the PalmDB class from the command line.
   * Prints the header values from a database.  Optionally it will print the
   * values from the record entry array (offsets, flags, ids).  And, also
   * optionally, it can output the contents of a specified record in both
   * hex and printable ASCII.
   * 
   * @param args the first argument is the input filename, the second is an
   *          optional boolean (0/1) toggling whether to print the record entry
   *          array, and the third is an optional index of a record to print.
   */
  public static void main(String args[])
    {
      if (args.length < 1)
        {
          System.out.println(
              "usage: PalmDB datafile.pdb [listRecEntries=0/1] [recordIndex]");
          System.exit(1);
        }

      File f = new File(args[0]);
      PalmDB pdb = null;
      boolean listRecs = false;
      if (args.length > 1)
        listRecs = (Integer.decode(args[1]) == 0 ? false : true);
      int recNum = -1;
      if (args.length > 2)
        recNum = Integer.decode(args[2]);
      byte[] recData = null;

      try
        {
          pdb = new PalmDB(f);

          // Test readRecord
          if (recNum != -1)
            recData = pdb.readRecord(recNum);

          pdb.close();
        }
      catch (FileNotFoundException e)
        {
          System.err.println("Could not find/open database: " + args[0]);
          System.err.println(e.getMessage());
          System.exit(2);
        }
      catch (IOException e)
        {
          System.err.println("Error reading database header: " + args[0]);
          System.err.println(e.getMessage());
          System.exit(3);
        }

      System.out.println("             DB Name: \"" + pdb.getDbName() + "\"");
      System.out.printf("               flags: 0x%04X\n", pdb.getFlags());
      System.out.printf("             version: 0x%04X\n", pdb.getVersion());
      System.out.println("       Creation time: "
          + Utility.toUNIXEpoch(pdb.getCreationTime()));
      System.out.println("   Modification time: "
          + Utility.toUNIXEpoch(pdb.getModificationTime()));
      System.out.println("         Backup time: "
          + Utility.toUNIXEpoch(pdb.getLastBackupTime()));
      System.out.printf("  modificationNumber: 0x%08X\n", pdb
          .getModificationNumber());
      System.out.printf("applicationInfoIDPtr: 0x%08X\n", pdb
          .getApplicationInfoIDPtr());
      System.out.printf("       sortInfoIDPtr: 0x%08X\n", pdb
          .getSortInfoIDPtr());
      System.out.println("             DB Type: \""
          + Utility.idToString(pdb.getDbTypeID()) + "\"");
      System.out.println("          DB Creator: \""
          + Utility.idToString(pdb.getDbCreatorID()) + "\"");
      System.out
          .printf("        uniqueIDSeed: 0x%08X\n", pdb.getUniqueIDSeed());
      System.out.printf(" nextRecordListIDPtr: 0x%08X\n", pdb
          .getNextRecordListIDPtr());
      System.out.println("          numRecords: " + pdb.getNumRecords());

      Vector<Long> recOffs = pdb.getRecordOffsets();
      Vector<Integer> recFlags = pdb.getRecordFlags();
      Vector<Integer> recIDs = pdb.getRecordIDs();
      if (listRecs)
        {
          for (int i = 0; i < pdb.getNumRecords(); i++)
            {
              System.out.printf("record %2d: 0x%08X 0x%02X 0x%06X\n", i,
                  recOffs.get(i), recFlags.get(i), recIDs.get(i));
            }
        }

      if (recNum != -1)
        {
          System.out.printf("Record %d (len=%d, flags=0x%02X, ID=0x%06X):\n",
              recNum, recData.length, recFlags.get(recNum), recIDs.get(recNum));
          Utility.printDataBlock(System.out, 16, recData, 0, recData.length,
              recOffs.get(recNum));
        }

      System.exit(0);
    }
}
