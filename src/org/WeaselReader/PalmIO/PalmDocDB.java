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
import java.io.IOException;
import java.util.Vector;
import java.util.zip.DataFormatException;



/**
 * This class supports reading PalmDoc (also known as AportisDoc) formatted Palm
 * databases. A PalmDoc database file is similar to a zTXT file, but it uses a
 * simple proprietary compression algorithm that is very quick but does not
 * compress well. It also supports bookmarks, each one stored in a separate
 * record following the text records. PalmDoc databases do not support
 * annotations.<br>
 * <br>
 * There are two versions of PalmDoc files, one with uncompressed text records
 * (version 1) and one with compressed text records (version 2). Unfortunately,
 * due to a number of buggy PalmDoc conversion programs, there are a large
 * number of broken PalmDoc files available in the wild. The most common
 * breakage is decompressed records which do not decompress to the proper record
 * size (normally 4096 bytes) which requires that a table of true record sizes
 * be built so that such a PalmDoc may be navigated accurately. The other form
 * of breakage is an incorrect record count in the PalmDoc header.<br>
 * <br>
 * The PalmDoc header in record 0 is described by the following C language
 * structure:<br>
 * typedef struct palmdoc_record0Type<br>
 * {<br>
 * UInt16 wVersion;<br>
 * UInt16 spare;<br>
 * UInt32 munged_dwStoryLen;<br>
 * UInt16 munged_wNumRecs;<br>
 * UInt16 wRecSize;<br>
 * UInt32 dwSpare2;<br>
 * } palmdoc_record0;<br>
 * <br>
 * The information and C code for the PalmDoc format comes from Weasel Reader
 * which in turn took the information from another Palm OS book reader,
 * CSpotRun. CSpotRun is copyright Bill Clagett (bill@32768.com) and is also
 * released under the GNU GPL.  The source for CSpotRun was at one time
 * available at <a href="http://www.32768.com/">Bill Clagett's page</a>.
 * 
 * @author John Gruenenfelder
 * @version $Id$
 */
public class PalmDocDB extends PalmDB {

  /**
   * PalmDoc database creator ID. This is a four byte character literal.
   * Use PalmDB.stringToID to get the numerical value.
   */
  public static final String PALMDOC_CREATOR_ID = "REAd";


  /**
   * PalmDoc database type ID. This is a four byte character literal.
   * Use PalmDB.stringToID to get the numerical value.
   */
  public static final String PALMDOC_TYPE_ID = "TEXt";


  /**
   * PalmDoc version flag for uncompressed data.
   */
  public static final int PALMDOC_UNCOMPRESSED = 0x01;


  /**
   * PalmDoc version flag for compressed data.
   */
  public static final int PALMDOC_COMPRESSED = 0x02;


  /**
   * Maximum length for a PalmDoc bookmark title. A title of the maximum length
   * will not be NUL terminated in the database.
   */
  public static final int MAX_TITLE_LENGTH = 16;


  /**
   * PalmDoc format version.  Version 1 contains uncompressed text records and
   * version 2 contains compressed text records.
   */
  private int palmDocVersion;


  /**
   * Length of uncompressed text data in a PalmDoc database.  Due to bugs in
   * PalmDoc converters, this number may not always be correct.
   */
  private long dataSize;


  /**
   * The number of text records in a PalmDoc database.  Due to broken
   * converters, this value also seems to have a habit of being wrong.
   */
  private int numDataRecords;


  /**
   * The record size for an uncompressed PalmDoc text record.  All records are
   * supposed to decompress to this length, but there are many PalmDoc files in
   * the wild where this is not true.  This can occur when a PalmDoc file is
   * edited on a device.  The avoid recompressing all data which follows it,
   * only this record will be recompressed resulting in a record with an odd
   * size.
   */
  private int recordSize;


  /**
   * The number of bookmarks in this PalmDoc file.  This is not stored in the
   * header and must be computed.  The number of bookmarks is given by:<br>
   *   totalRecords - numDataRecords - 1
   */
  private int numBookmarks;


  /**
   * The first bookmark record, if any.  PalmDoc database do not have a bookmark
   * index in one place.  Instead, this record and each record after it,
   * contains a single bookmark up to numBookmarks in total.  If there are no
   * bookmarks in this PalmDoc, this value will be zero.
   */
  private int                bookmarkRecordIndex;


  /**
   * The bookmarks for this zTXT document.
   */
  private Bookmarks          bookmarks;


  /**
   * Lengths of each text record in the database.  Because of various flaws
   * and situations, PalmDoc databases can come to have non-uniform record
   * lengths. In order to properly position the reader within the document, it
   * is necessary to calculate the record lengths ahead of time.
   */
  private int recordLengths[];

  private String encode;

  /**
   * Create a new PalmDocDB and load the specified PalmDoc file.
   *
   * @param pdbFile a PalmDoc document database to be read from disk.
   * @throws IOException if an I/O error occurs while reading the PDB header or
   *           the PalmDoc header.
   * @throws DataFormatException if the input file is not a PalmDoc database
   *           (its typeID is not 'TEXt').
   */
  public PalmDocDB(File pdbFile,String encode) throws IOException, DataFormatException
    {
      // Read in standard Palm PDB header values
      super(pdbFile);
      this.encode = encode;
      // Creators of a database may vary, but if the input file is a PalmDoc
      // then the type ID must be "TEXt".  If it's not then this probably isn't
      // really a PalmDoc file.
      if (PALMDOC_TYPE_ID.compareTo(Utility.idToString(getDbTypeID())) != 0)
        throw new DataFormatException(
            "PalmDocDB: input file is not a PalmDoc database");

      // Read in PalmDoc header values stored in PDB record 0
      readPalmDocHeader();

      // Read bookmark record into memory
      readBookmarks();

      // Pre-calculate record lengths for accurate positioning within text
      calculateRecordLengths();
    }



  /**
   * @return the PalmDoc format version.  Possible values are either
   *            PALMDOC_UNCOMPRESSED or PALMDOC_COMPRESSED.
   */
  public int getPalmDocVersion()
    {
      return palmDocVersion;
    }



  /**
   * @return the total size of the uncompressed text data records.
   */
  public long getDataSize()
    {
      return dataSize;
    }



  /**
   * @return the number of text data records.
   */
  public int getNumDataRecords()
    {
      return numDataRecords;
    }



  /**
   * @return the size of an uncompressed text data record.
   */
  public int getRecordSize()
    {
      return recordSize;
    }



  /**
   * @return the number of bookmarks in this PalmDoc database.
   */
  public int getNumBookmarks()
    {
      return numBookmarks;
    }



  /**
   * @return the index of the first record containing a bookmark, if any.
   */
  public int getBookmarkRecordIndex()
    {
      return bookmarkRecordIndex;
    }



  /**
   * @return the bookmark collection for this PalmDoc.  If this PalmDoc has no
   *          bookmarks, returns null.
   */
  public Bookmarks getBookmarks()
    {
      return bookmarks;
    }



  /**
   * @return the record lengths array containing the uncompressed lengths of
   *          each text record.
   */
  public int[] getRecordLengths()
    {
      return recordLengths;
    }



  /**
   * Read the specified text record and decompress if necessary.
   *
   * @param index the index of the text data record to be read, counting from
   *    zero.
   * @return a String containing the (decompressed) text.
   * @throws IOException if an I/O error occurs while reading input record.
   * @throws ArrayIndexOutOfBoundsException if the requested record does not
   *          actually exist.
   */
  public String readTextRecord(int index) throws ArrayIndexOutOfBoundsException,
                                          IOException
    {
      // Determine size of requested text record
      int dataSize = recordLengths[index];

      // Fetch the data
      byte[] recData = readRecord(index + 1);

      // Decompress data if necessary
      if (palmDocVersion == PALMDOC_COMPRESSED)
        recData = decompressBuffer(recData, dataSize);

      return new String(recData,encode);
    }



  /**
   * Read the PalmDoc header data from record zero.
   *
   * @throws IOException if an I/O error occurs while reading from the database.
   */
  private void readPalmDocHeader() throws IOException
    {
      Vector<Long> recOffs = getRecordOffsets();
      seek(recOffs.get(0));

      // Read header values
      palmDocVersion = readUnsignedShort();

      // Read 16bits of padding
      readUnsignedShort();

      dataSize = readUInt32();
      numDataRecords = readUnsignedShort();
      recordSize = readUnsignedShort();
    }



  /**
   * Load the bookmark index into memory for quicker access.
   *
   * @throws IOException if an I/O error occurs reading a bookmark record.
   * @throws ArrayIndexOutOfBoundsException if a bookmark record should exist
   *            but cannot be found.
   */
  private void readBookmarks() throws ArrayIndexOutOfBoundsException,
                               IOException
    {
      // First compute the number of bookmarks in this database
      numBookmarks = getNumRecords() - numDataRecords - 1;

      // If there are no bookmarks, do nothing
      if (numBookmarks == 0)
        {
          bookmarkRecordIndex = 0;
          bookmarks = null;
          return;
        }
      else
        {
          // Mark the first record containing a bookmark
          bookmarkRecordIndex = numDataRecords + 1;
        }

      // Read all of the bookmark data
      int[] offsets = new int[numBookmarks];
      String[] titles = new String[numBookmarks];
      for (int i = 0; i < numBookmarks; i++)
        {
          byte[] bmrkData = readRecord(bookmarkRecordIndex + i);
          titles[i] = new String(bmrkData, 0, MAX_TITLE_LENGTH,encode);
          offsets[i] = (int)Utility.fromUInt32(bmrkData, MAX_TITLE_LENGTH);
        }
    }



  /**
   * Calculate the lengths of each text record.  Some PalmDoc files have text
   * record which do not all decompress to a uniform size.  In order to
   * accurately mark a position within the text, it is necessary to know the
   * size of each record up to that point.  This array allows that calculation
   * to be performed quickly.
   *
   * @throws IOException if an I/O error occurs while reading input records.
   * @throws ArrayIndexOutOfBoundsException if a record which should exist is
   *          requested but does not actually exist.
   */
  private void calculateRecordLengths() throws ArrayIndexOutOfBoundsException,
                                        IOException
    {
      recordLengths = new int[numDataRecords];

      for (int i = 0; i < numDataRecords; i++)
        {
          // Get next text record
          byte[] recData = readRecord(i + 1);

          // Calculate and save length of this record
          recordLengths[i] = calculateBufferLength(recData);
        }
    }



  /**
   * Decompress the given buffer using the LZ77-based PalmDoc compression
   * algorithm.
   *
   * @param data a block of PalmDoc data to decompress.
   * @param outputSize the length of the data array when decompressed.
   * @return a byte array containing the uncompressed data.
   */
  private byte[] decompressBuffer(byte[] data, int outputSize)
    {
      byte[] output = new byte[outputSize];
      int i = 0;
      int j = 0;

      while (i < data.length)
        {
          // Get the next compressed input byte
          int c = ((int) data[i++]) & 0x00FF;

          if (c >= 0x00C0)
            {
              // type C command (space + char)
              output[j++] = ' ';
              output[j++] = (byte) (c & 0x007F);
            }
          else if (c >= 0x0080)
            {
              // type B command (sliding window sequence)

              // Move this to high bits and read low bits
              c = (c << 8) | (((int) data[i++]) & 0x00FF);
              // 3 + low 3 bits (Beirne's 'n'+3)
              int windowLen = 3 + (c & 0x0007);
              // next 11 bits (Beirne's 'm')
              int windowDist = (c >> 3) & 0x07FF;
              int windowCopyFrom = j - windowDist;

              windowLen = Math.min(windowLen, outputSize - j);
              while (windowLen-- > 0)
                output[j++] = output[windowCopyFrom++];
            }
          else if (c >= 0x0009)
            {
              // self-representing, no command
              output[j++] = (byte)c;
            }
          else if (c >= 0x0001)
            {
              // type A command (next c chars are literal)
              c = Math.min(c, outputSize - j);
              while (c-- > 0)
                output[j++] = data[i++];
            }
          else
            {
              // c == 0, also self-representing
              output[j++] = (byte)c;
            }
        }

      return output;
    }



  /**
   * Calculate the decompressed length of the given buffer.  This is the same
   * as decompressing the buffer, but without saving the data anywhere.  If
   * the text of this PalmDoc is not compressed, there is no computation
   * involved and the record length is the same as the buffer length.
   *
   * @param data a block of PalmDoc data to calculate the length of.
   * @return the uncompressed length of the given data buffer.
   */
  private int calculateBufferLength(byte[] data)
    {
      // If this PalmDoc isn't compressed then there is nothing to do
      if (palmDocVersion == PALMDOC_UNCOMPRESSED)
        return data.length;

      int i = 0;
      int len = 0;
      while (i < data.length)
        {
          // Get the next compressed input byte
          int c = ((int) data[i++]) & 0x00FF;

          if (c >= 0x00C0)
            {
              // type C command (space + char)
              len += 2;
            }
          else if (c >= 0x0080)
            {
              // type B command (sliding window sequence)

              // Move this to high bits and read low bits
              c = (c << 8) | (((int) data[i++]) & 0x00FF);
              // 3 + low 3 bits (Beirne's 'n'+3)
              len += 3 + (c & 0x0007);
            }
          else if (c >= 0x0009)
            {
              // self-representing, no command
              len++;
            }
          else if (c >= 0x0001)
            {
              // type A command (next c chars are literal)
              len += c;
              i += c;
            }
          else
            {
              // c == 0, also self-representing
              len++;
            }
        }

      return len;
    }
}
