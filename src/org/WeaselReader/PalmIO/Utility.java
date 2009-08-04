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

import java.io.PrintStream;
import java.util.Date;


/**
 * A class of utility methods primarily focused on manipulating data within
 * Palm OS database files. There are methods for converting Palm ID values, Palm
 * date values, reading/writing unsigned 32bit int values, and pretty-printing
 * a byte array.
 *
 * @author John Gruenenfelder
 * @version $Id$
 */
public class Utility {

  /**
   * Convert a Palm OS database 'ID' from a four byte value to a four character
   * String so it can be printed. Use this to get a usable String from dbTypeID
   * or dbCreatorID.
   * 
   * @param id the ID value to be converted.
   * @return a String representation of the given Palm database ID.
   */
  public static String idToString(long id)
    {
      byte[] name = new byte[4];
  
      name[0] = (byte) ((0xFF000000L & id) >> 24);
      name[1] = (byte) ((0x00FF0000L & id) >> 16);
      name[2] = (byte) ((0x0000FF00L & id) >> 8);
      name[3] = (byte) (0x000000FFL & id);
  
      return new String(name);
    }



  /**
   * Convert a String representation of a Palm OS database 'ID' back into a
   * standard four byte literal value.
   * 
   * @param id the ID String to convert.
   * @return a four byte literal ID stored in a long.
   */
  public static long stringToID(String id)
    {
      byte[] arr = new byte[4];
      arr[0] = (byte) id.charAt(0);
      arr[1] = (byte) id.charAt(1);
      arr[2] = (byte) id.charAt(2);
      arr[3] = (byte) id.charAt(3);
  
      return fromUInt32(arr, 0);
    }



  /**
   * Convert an unsigned 32 bit value (UInt32) to a long without losing the
   * original unsigned value.
   * 
   * @param buf the array containing the value to be converted.
   * @param i the index of the array element to be converted.
   * @return a converted UInt32 value stored in a long.
   */
  public static long fromUInt32(byte buf[], int i)
    {
      long uInt32 = 0;
      int firstByte;
      int secondByte;
      int thirdByte;
      int fourthByte;
  
      firstByte = (0x000000FF & (int) buf[i]);
      secondByte = (0x000000FF & (int) buf[i + 1]);
      thirdByte = (0x000000FF & (int) buf[i + 2]);
      fourthByte = (0x000000FF & (int) buf[i + 3]);
      uInt32 = ((long) ((firstByte << 24)
          | (secondByte << 16)
          | (thirdByte << 8)
          | fourthByte) & 0xFFFFFFFFL);
  
      return uInt32;
    }



  /**
   * Put an unsigned 32 bit value stored in a long into a byte array at the
   * specified index.
   * 
   * @param val a long containing a UInt32 value to be stored.
   * @param buf the byte array in which to store the UInt32 value.
   * @param i the index of the array element to store the new value in.
   */
  public static void toUInt32(long val, byte buf[], int i)
    {
      buf[i] = (byte) ((val & 0xFF000000L) >> 24);
      buf[i + 1] = (byte) ((val & 0x00FF0000L) >> 16);
      buf[i + 2] = (byte) ((val & 0x0000FF00L) >> 8);
      buf[i + 3] = (byte) (val & 0x000000FFL);
    }



  /**
   * Convert a Java Date (which uses the standard UNIX time epoch) into a
   * seconds count in the Palm epoch suitable for use in any of the time fields
   * in the PalmDB class. <br>
   * The Palm epoch is the same as the original Macintosh epoch<br>
   * &nbsp;&nbsp; January 1, 1904 00:00:00 GMT<br>
   * while the standard UNIX epoch is<br>
   * &nbsp;&nbsp; January 1, 1970 00:00:00 GMT
   * 
   * @param time a point in time represented by a Java Date object.
   * @return the time parameter converted into a count of seconds from the
   *         standard Palm OS epoch.
   */
  public static long toPalmEpoch(Date time)
    {
      return (time.getTime() / 1000) + PalmDB.PALM_CTIME_OFFSET;
    }



  /**
   * Convert a Palm OS date (a long count of seconds) from the Palm OS epoch to
   * a Date in the standard UNIX epoch.
   * 
   * @param palmTime a point in time expressed as a seconds count in the Palm OS
   *          epoch.
   * @return a Java Date object with the time converted to the UNIX epoch.
   */
  public static Date toUNIXEpoch(long palmTime)
    {
      return new Date((palmTime - PalmDB.PALM_CTIME_OFFSET) * 1000);
    }



  /**
   * Output a block of data in mixed hex/ASCII format.  Each line begins with
   * the address offset for that particular line of data, followed by 16 data
   * bytes output in hex, followed by those same bytes output in ASCII if the
   * byte represents a printable character, otherwise a period is output.  This
   * format was borrowed from the ncurses tool 'hexedit'.
   *
   * @param out the stream to output to, such as System.out for standard output.
   * @param bytesPerLine the number of data bytes to print per line.  This
   *          value will be rounded down to a multiple of four.
   * @param data the byte array containing the data to be printed.
   * @param startOffset the offset in the data array at which to begin.
   * @param length the number of bytes from the data array to print.
   * @param address the memory address this block of data represents.  Or, just
   *          pass 0 for it to count from the beginning of the startOffset.
   *          This is only for readability and convenience and has no effect
   *          on how the data is output.
   */
  public static void printDataBlock(PrintStream out, int bytesPerLine,
                                    byte[] data, int startOffset, int length,
                                    long address)
    {
      int bytesLeft = length;
      int byteCnt = startOffset;
      int lineCnt = 0;
  
      // Round bytesPerLine down to a multiple of four
      bytesPerLine -= (bytesPerLine % 4);
  
      while (bytesLeft > 0)
        {
          // Print the address in hex at the start of each line of data
          out.printf("%08X ", address + (bytesPerLine * lineCnt));
  
          // Print bytesPerLine data bytes in hex, zero padded, with a space
          // between each byte
          int cntSave = byteCnt;
          int limit = bytesPerLine;
          if (bytesLeft < bytesPerLine)
            limit = bytesLeft;
          for (int i = 0; i < limit; i++)
            {
              if ((i % 4) == 0)
                out.print(' ');
              out.printf(" %02X", data[byteCnt++]);
            }
  
          // Pad the hex section if fewer than bytesPerLine bytes were output
          if (limit < bytesPerLine)
            for (int i = limit; i < bytesPerLine; i++)
              {
                if ((i % 4) == 0)
                  out.print(' ');
                out.print("   ");
              }
  
          // Restore the byte counter so we can output the same data again,
          // this time as ASCII
          byteCnt = cntSave;
          out.printf("  ");
  
          // Output each byte as an ASCII character, but only if it represents
          // a printable character
          for (int i = 0; i < limit; i++)
            {
              char c = (char)data[byteCnt++];
              if ((c < 0x20) || (c >= 0x7F))
                c = '.';
              out.print(c);
              bytesLeft--;
            }
  
          out.print('\n');
          lineCnt++;
        }
    }
}
