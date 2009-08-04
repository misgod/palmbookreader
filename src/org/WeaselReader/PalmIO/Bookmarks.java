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


/**
 * A collection of bookmarks.  Each bookmark is mark/anchor in the text
 * represented by a byte offset from the beginning of the text, plus a title
 * string.
 *
 * @author John Gruenenfelder
 * @version $Id$
 */
public class Bookmarks {

  /**
   * Bookmark offsets, measured in bytes from the beginning of the document.
   */
  private final int[] offsets;


  /**
   * Bookmark titles.
   */
  private final String[] titles;


  /**
   * The number of bookmarks in this collection.
   */
  public final int length;



  /**
   * Create a new annotation collection.  The annotation collection is
   * read-only once created.
   *
   * @param offsets an int array containing the byte offsets for each
   *    bookmark.
   * @param titles a String array containing the titles for each bookmark.
   */
  public Bookmarks(int[] offsets, String[] titles)
    {
      this.offsets = offsets;
      this.titles = titles;
      length = titles.length;
    }



  /**
   * Get the byte offset for the given bookmark.
   *
   * @param index the index of the bookmark for which to get the offset.
   * @return the byte offset of the given bookmark, measured from the
   *      beginning of the text.
   */
  public int getOffset(int index)
    {
      return offsets[index];
    }



  /**
   * Get the array containing the bookmark offsets.
   *
   * @return an int array containing the offsets of all bookmarks.
   */
  public int[] getOffsetArray()
    {
      return offsets;
    }



  /**
   * Get the title string for the given bookmark.
   *
   * @param index the index of the bookmark for which to get the title.
   * @return the title string of the given bookmark.
   */
  public String getTitle(int index)
    {
      return titles[index];
    }



  /**
   * Get the array containing the bookmark titles.
   *
   * @return a String array containing the titles of all bookmarks.
   */
  public String[] getTitleArray()
    {
      return titles;
    }
}
