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
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;



/**
 * This class supports reading a Weasel Reader zTXT database file. The
 * underlying Palm OS PDB structure is handled by PalmDB while this class
 * handles the data specific to the zTXT format. The ZtxtDB class represents the
 * zTXT format version 1.44 which is the final Weasel Reader for Palm OS zTXT
 * format. Any future zTXT format will not be contained within a Palm OS
 * database.<br>
 * <br>
 * <b>Limitations:</b><br>
 * <ul>
 * <li>Does not support writing or updating an existing zTXT file.  Along with
 * not being able to generate a new zTXT PDB, this also means that simpler
 * actions, such as adding a bookmark or annotation, are also not
 * supported.</li>
 * <li>Does not support non-random access zTXT files.  The earliest zTXT
 * format required that all document text be decompressed in one pass thus
 * requiring extra temporary storage.  Nearly all existing zTXTs are of a newer
 * version of the format and allow random access of the document text in 8kB
 * chunks.  Palm OS Weasel Reader 1.60 and later dropped support for non-random
 * access zTXTs.</li>
 * <li>Does not support zTXTs with non-uniform data records. Many PalmDoc
 * databases contain data records which do not decompress to the same length and
 * the zTXT header contains a flag to indicate the same for this format.
 * However, no current converter generates zTXTs in this manner so it is
 * unlikely any exist in the wild.</li>
 * </ul><br>
 * <b>Format:</b><br><br>
 * A zTXT database is an e-book format that contains a 32 byte header in record
 * 0, a series of zLib compressed data records which hold the document text, an
 * optional record with a bookmark list, and an optional record with an
 * annotation index which is followed by one record for each annotation.<br>
 * <br>
 * A more detailed description and explanation of the zTXT format can be found
 * on Weasel Reader's <a href="http://weaselreader.org/ztxt_format.php">zTXT
 * format</a> reference page.<br>
 * <br>
 * The zTXT header (version 1.44) is defined by the following C structure:<br>
 * typedef struct zTXT_record0Type {<br>
 * UInt16 version;<br>
 * UInt16 numRecords;<br>
 * UInt32 size;<br>
 * UInt16 recordSize;<br>
 * UInt16 numBookmarks;<br>
 * UInt16 bookmarkRecord;<br>
 * UInt16 numAnnotations;<br>
 * UInt16 annotationRecord;<br>
 * UInt8 flags;<br>
 * UInt8 reserved;<br>
 * UInt32 crc32;<br>
 * UInt8 padding[0x20 - 24];<br>
 * } zTXT_record0;<br>
 *<br>
 * Definition of bits in the flags byte of the zTXT header:<br>
 * typedef enum {<br>
 * ZTXT_RANDOMACCESS = 0x01,<br>
 * ZTXT_NONUNIFORM = 0x02<br>
 * } zTXT_flag;<br>
 * <br>
 * 
 * @author John Gruenenfelder
 * @version $Id$
 */
public class ZtxtDB extends PalmDB {

  /**
   * Weasel Reader database creator ID. This is a four byte character literal.
   * Use PalmDB.stringToID to get the numerical value.
   */
  public static final String WEASEL_CREATOR_ID  = "GPlm";


  /**
   * Weasel Reader database type ID. This is a four byte character literal. Use
   * PalmDB.stringToID to get the numerical value.
   */
  public static final String WEASEL_TYPE_ID     = "zTXT";


  /**
   * The maximum length of a zTXT bookmark or annotation title. A title of the
   * maximum length will not be NUL terminated in the database.
   */
  public static final int    MAX_TITLE_LENGTH   = 20;


  /**
   * The maximum length of a zTXT annotation.  An annotation of the maximum
   * length will not be NUL terminated in the database.
   */
  public static final int    MAX_ANNOTATION_LENGTH = 4096;


  /**
   * The highest zTXT format version recognized by this class.
   */
  public static final int    ZTXT_VERSION       = 0x012C;


  /**
   * If this flag is set, this zTXT supports random access of the compressed
   * text records.
   */
  public static final short  ZTXT_RANDOMACCESS = 0x01;


  /**
   * If this flag is set then not all data records in this zTXT will necessarily
   * be of size recordSize but may be slightly different.
   */
  public static final short  ZTXT_NONUNIFORM    = 0x02;


  /**
   * The zTXT format version for this document. Consists of two unsigned bytes.
   * A value of 0x012C would be interpreted as version 1.44.
   */
  private int                zTXTVersion;


  /**
   * The number of text data records in this zTXT file. Does not include record
   * 0 or any bookmark or annotation records.
   */
  private int                numDataRecords;


  /**
   * The total size of the text data when uncompressed.
   */
  private long               dataSize;


  /**
   * The size of an uncompressed data record. All data records will decompress
   * to this size except for the last which may be smaller.
   */
  private int                recordSize;


  /**
   * The number of bookmarks present in this zTXT document.
   */
  private int                numBookmarks;


  /**
   * The index of the record containing the bookmark list/index. If there are no
   * bookmarks in this zTXT, this record will not exist and this value must be
   * zero.
   */
  private int                bookmarkRecordIndex;


  /**
   * The number of annotations present in this zTXT document.
   */
  private int                numAnnotations;


  /**
   * The index of the record containing the annotation index. If there are no
   * annotations in this zTXT, this record will not exist and this value must be
   * zero.
   */
  private int                annotationRecordIndex;


  /**
   * Flags to indicate features of this zTXT document. There are currently only
   * two defined: ZTXT_RANDOM_ACCESS and ZTXT_NONUNIFORM. Older versions of
   * Weasel on Palm OS supported non-random access zTXTs, but this is no longer
   * the case and all supported zTXTs will need ZTXT_RANDOM_ACCESS to be set.
   * There are also no known non-uniform zTXTs so this feature is not currently
   * supported either by this class or by Weasel Reader.
   */
  private short              zTXTFlags;


  /**
   * A CRC32 value computed over all of the uncompressed text data. The CRC32
   * function used to generate this value is that provided by zLib.
   */
  private long               crc32;


  /**
   * The bookmarks for this zTXT document.
   */
  private Bookmarks          bookmarks;


  /**
   * The annotations for this zTXT document.
   */
  private Annotations        annotations;


  /**
   * The decompression information for this zTXT database.  This data must be
   * primed by called initializeDecompression and may be ended by calling
   * endDecompression.
   */
  private Inflater           decompressor;

  private String mEncode;

  /**
   * A collection of annotations.  An annotation is essentially a bookmark with
   * an extra block of text.  The annotation index is identical in format to
   * the bookmark index.  In a zTXT database file, the text of each annotation
   * would occupy the records following the index record.  The annotation text
   * has a maximum size of 4096 bytes.
   */
  public class Annotations extends Bookmarks {
	  
    /**
     * The text of each annotation in a zTXT.
     */
    private final String[] annotationText;




    /**
     * Create a new annotation collection.  The annotation collection is
     * read-only once created.
     *
     * @param offsets an int array containing the byte offsets for each
     *    annotation.
     * @param titles a String array containing the titles for each annotation.
     * @param annotationText a String array containing the text for each
     *    annotation.
     */
    public Annotations(int[] offsets, String[] titles, String[] annotationText)
      {
        super(offsets, titles);
        this.annotationText = annotationText;
      }



    /**
     * Get the block of text associated with the given annotation.
     *
     * @param index the index of the annotation for which to get the text.
     * @return the text block for the given annotation.
     */
    public String getAnnotationText(int index)
      {
        return annotationText[index];
      }



    /**
     * Get the array containing the annotation text blocks.
     *
     * @return a String array containing the text blocks of all annotations.
     */
    public String[] getAnnotationTextArray()
      {
        return annotationText;
      }
  }



  /**
   * Create a new ZtxtDB and load the specified zTXT document.
   * 
   * @param inputFile a zTXT document database to read from disk.
   * @throws IOException if an I/O error occurs while reading the PDB header or
   *           the zTXT header.
   * @throws DataFormatException if the input file is not a zTXT database (its
   *            typeID is not 'zTXT')
   */
  public ZtxtDB(File pdbFile,String encode) throws IOException, DataFormatException
    {
      // Read in standard Palm PDB header values
      super(pdbFile);
      mEncode = encode;
      // Creators of a database may vary, but if the input file is a zTXT then
      // the type ID must be "zTXT".  If it's not then this probably isn't
      // really a zTXT.
      if (WEASEL_TYPE_ID.compareTo(Utility.idToString(getDbTypeID())) != 0)
        throw new DataFormatException(
            "ZtxtDB: input file is not a zTXT database");

      // Read in zTXT header values stored in PDB record 0
      readzTXTHeader();

      // Read bookmark record into memory
      readBookmarks();

      // Read annotation index record into memory
      readAnnotations();

      // Initialize any other fields
      decompressor = null;
    }



  /**
   * @return the size of the data records in this zTXT document.
   */
  public int getRecordSize()
    {
      return recordSize;
    }



  /**
   * @return the zTXT format option flags.
   */
  public short getzTXTFlags()
    {
      return zTXTFlags;
    }



  /**
   * @return the zTXT format version for this document.
   */
  public int getzTXTVersion()
    {
      return zTXTVersion;
    }



  /**
   * Convert the two version bytes into a readable string.
   *
   * @return the zTXT version as a readable String.
   */
  public String getzTXTVersionString()
    {
      int major = (zTXTVersion & 0x0000FF00) >> 8;
      int minor = (zTXTVersion & 0x000000FF);

      return String.format("%d.%02d", major, minor);
    }



  /**
   * @return the number of text data records.
   */
  public int getNumDataRecords()
    {
      return numDataRecords;
    }



  /**
   * @return the total size of the uncompressed text data.
   */
  public long getDataSize()
    {
      return dataSize;
    }



  /**
   * @return the number of bookmarks present.
   */
  public int getNumBookmarks()
    {
      return numBookmarks;
    }



  /**
   * @return the index of the bookmark list record, if present.
   */
  public int getBookmarkRecordIndex()
    {
      return bookmarkRecordIndex;
    }



  /**
   * @return the number of annotations present.
   */
  public int getNumAnnotations()
    {
      return numAnnotations;
    }



  /**
   * @return the index of the annotation index record, if present.
   */
  public int getAnnotationRecordIndex()
    {
      return annotationRecordIndex;
    }



  /**
   * @return the computed CRC32 value for this zTXT's data.
   */
  public long getCRC32()
    {
      return crc32;
    }



  /**
   * @return the bookmark collection for this zTXT.  If this zTXT has no
   *          bookmarks, returns null.
   */
  public Bookmarks getBookmarks()
    {
      return bookmarks;
    }



  /**
   * @return the annotation collection for this zTXT.  If this zTXT has no
   *          annotations, returns null.
   */
  public Annotations getAnnotations()
    {
      return annotations;
    }



  /**
   * Validate the CRC32 stored in the zTXT header against the CRC32 computed
   * over the text data records in the database.  zTXT databases use the CRC32
   * algorithm from zLib which is available in java.util.zip.CRC32.
   *
   * @return true if the stored CRC32 matches that computed from the zTXT's
   *      text data records or false if the CRC32 does not match or if the
   *      stored CRC32 is zero.
   */
  public boolean validateCRC32()
    {
      if (crc32 == 0)
        return false;

      try
        {
          if (computeCRC32() == crc32)
            return true;
        }
      catch (Exception e)
        {
          // An exception means the file is probably bad so the CRC32 is
          // almost certainly bad as well.
          return false;
        }

      return false;
    }



  /**
   * Compute a CRC32 value over the text data records in the database.  zTXT
   * databases use the CRC32 algorithm from zLib which is available in
   * java.util.zip.CRC32.
   *
   * @return the computed CRC32 value for this zTXT database.
   * @throws IOException if an I/O error occurred while computing the CRC32
   *    value.
   */
  public long computeCRC32() throws IOException
    {
      CRC32 compCRC32 = new CRC32();

      for (int i = 0; i < numDataRecords; i++)
        {
          try
            {
              compCRC32.update(readRecord(i + 1));
            }
          catch (ArrayIndexOutOfBoundsException e)
            {
              throw new IOException(
                  "computeCRC32: Index out of bounds reading record "
                  + (i + 1));
            }
          catch (IOException e)
            {
              throw new IOException("computeCRC32: error reading record "
                  + (i + 1));
            }
        }

      return compCRC32.getValue();
    }



  /**
   * Initialize the decompression stream.  For random access to work, the first
   * text record must be decompressed first.  After that, any other text record
   * may be decompressed in any order.  This is necessary because the first
   * compressed record contains data important to the zLib format.  The
   * decompression stream will remain open until this object is discarded or
   * until the endDecompression method is called.
   *
   * @throws IOException if an I/O error occurs while reading the first record.
   * @throws ArrayIndexOutOfBoundsException if the first text record does not
   *           exist.
   * @throws DataFormatException if the zLib formatted data in the input text
   *           record is invalid.
   */
  public void initializeDecompression() throws ArrayIndexOutOfBoundsException,
                                        IOException, DataFormatException
    {
      // Read the first text record
      byte[] textData = readRecord(1);

      // Decompress the data
      decompressor = new Inflater();
      decompressor.setInput(textData);
      byte[] output = new byte[recordSize];
      decompressor.inflate(output);
    }



  /**
   * End the decompression of data and clean up any data used by the Inflater
   * object.
   */
  public void endDecompression()
    {
      decompressor.end();
      decompressor = null;
    }



  /**
   * Read the specified text data record and decompress it.
   *
   * @param index the index of the text data record to be read, counting from
   *    zero.
   * @return a String containing the decompressed text.
   * @throws IOException if an I/O error occurs while reading the requested
   *            record.
   * @throws ArrayIndexOutOfBoundsException if the requested record index does
   *           not exist.
   * @throws DataFormatException if the Inflater is not initialized or if the
   *           zLib formatted data in the input text record is invalid.
   */
  public String readTextRecord(int index) throws ArrayIndexOutOfBoundsException,
                                          IOException, DataFormatException
    {
      if (decompressor == null)
        throw new DataFormatException("readTextRecord(" + index
            + "): zLib inflater not yet initialized.");

      if ((index < 0) || (index >= numDataRecords))
        throw new ArrayIndexOutOfBoundsException("readTextRecord(" + index
            + "): record index is out of bounds.");

      // Read input data
      byte[] textData = readRecord(index + 1);

      // If the first text record is being read again then the Inflater must
      // be reset since this record is the beginning of the compressed data
      // stream.
      if (index == 0)
        decompressor.reset();

      // Decompress the data
      decompressor.setInput(textData);
      byte[] output = new byte[recordSize];
      int resultLength = decompressor.inflate(output);

      // Create the output string
      return new String(output, 0, resultLength, mEncode);
    }



  /**
   * Show something semi-useful for this zTXT when the object is printed.
   *
   * @return a String representation of this zTXT.
   */
  @Override
  public String toString()
    {
      return "zTXT \"" + getDbName() + "\" (version " + getzTXTVersionString()
              + ")";
    }



  /**
   * Close any open resources before garbage collection.  Specifically, clean
   * up the decompression data.
   */
  @Override
  protected void finalize() throws Throwable
    {
      try {
        if (decompressor != null)
          decompressor.end();
      } finally {
        super.finalize();
      }
    }



  /**
   * Read the zTXT header data from record zero.
   * 
   * @throws IOException if an I/O error occurs while reading from the database.
   */
  private void readzTXTHeader() throws IOException
    {
      Vector<Long> recOffs = getRecordOffsets();
      seek(recOffs.get(0));

      zTXTVersion = readUnsignedShort();
      numDataRecords = readUnsignedShort();
      dataSize = readUInt32();
      recordSize = readUnsignedShort();
      numBookmarks = readUnsignedShort();
      bookmarkRecordIndex = readUnsignedShort();
      numAnnotations = readUnsignedShort();
      annotationRecordIndex = readUnsignedShort();
      zTXTFlags = (short) readUnsignedByte();

      // To maintain data alignment restrictions of the m68k processor, there
      // is a padding byte between the flags and the CRC value.
      readUnsignedByte();

      crc32 = readUInt32();
    }



  /**
   * Load the bookmark array into memory for quicker access.
   *
   * @throws IOException if an I/O error occurs reading the bookmark record.
   * @throws ArrayIndexOutOfBoundsException if the bookmark record is listed
   *            as existing but cannot be found.
   */
  private void readBookmarks() throws ArrayIndexOutOfBoundsException,
                                      IOException
    {
      // Do nothing if there is no list of bookmarks in this zTXT.
      if (bookmarkRecordIndex == 0)
        {
          bookmarks = null;
          return;
        }

      // Read record and parse the data
      byte[] bmrkData = readRecord(bookmarkRecordIndex);
      int[] offsets = new int[numBookmarks];
      String[] titles = new String[numBookmarks];
      parseOffsetsAndTitles(bmrkData, numBookmarks, offsets, titles);

      // Create the new bookmark collection
      bookmarks = new Bookmarks(offsets, titles);
    }



  /**
   * Load the annotation index and annotation text blocks into memory.
   *
   * @throws IOException if an I/O error occurs reading the annotation index
   *            record.
   * @throws ArrayIndexOutOfBoundsException if the annotation index record is
   *            listed as existing but cannot be found.
   */
  private void readAnnotations() throws ArrayIndexOutOfBoundsException,
                                            IOException
    {
      // Do nothing if there is no list of annotation in this zTXT.
      if (annotationRecordIndex == 0)
        {
          annotations = null;
          return;
        }

      // Read record and parse the data
      byte[] annoData = readRecord(annotationRecordIndex);
      int[] offsets = new int[numAnnotations];
      String[] titles = new String[numAnnotations];
      parseOffsetsAndTitles(annoData, numAnnotations, offsets, titles);

      // Read in each annotation and create a String from the data.
      String[] annoArray = new String[numAnnotations];
      for (int i = 0; i < numAnnotations; i++)
        {
          byte[] annoText = readRecord(annotationRecordIndex + 1 + i);
          annoArray[i] = new String(annoText);
        }

      // Create the new annotation collection
      annotations = new Annotations(offsets, titles, annoArray);
    }



  /**
   * Parse the offset/title data in a byte array that is common to both
   * bookmark and annotation indices.  The offsetArray and titleArray arrays
   * must be allocated before this method is called and they should have a
   * length equal to the number of bookmarks/annotations present in the data
   * array to be parsed.
   *
   * @param data the array of data bytes to be parsed.
   * @param numEntries the number of offsets/titles to be parsed from the data
   *          array.
   * @param offsetArray an empty allocated array where byte offset values will
   *          be stored.
   * @param titleArray an empty allocated array where titles will be stored.
   * @throws IOException if the input data array is too small which likely means
   *          some sort of I/O error when the array was read.
   */
  private void parseOffsetsAndTitles(byte[] data, int numEntries,
                                     int[] offsetArray, String[] titleArray)
               throws IOException
    {
      // Make sure all the data is present
      if (data.length < (numEntries * (MAX_TITLE_LENGTH + 4)))
        throw new IOException(
            "parseOffsetsAndTitles: Record does not contain the "
            + "correct number of offsets/titles.");

      // Read each entry in the record.  The format is a four byte offset
      // followed by a 20 byte title.
      for (int i = 0; i < numEntries; i++)
        {
          offsetArray[i] = (int)Utility.fromUInt32(data,
              i * (MAX_TITLE_LENGTH + 4));
          titleArray[i] = new String(data, (i * (MAX_TITLE_LENGTH + 4)) + 4,
              MAX_TITLE_LENGTH);
        }
    }


  
}
