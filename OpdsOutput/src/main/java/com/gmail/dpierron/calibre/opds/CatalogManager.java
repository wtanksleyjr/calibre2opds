package com.gmail.dpierron.calibre.opds;

import com.gmail.dpierron.calibre.cache.CachedFile;
import com.gmail.dpierron.calibre.configuration.ConfigurationManager;
import com.gmail.dpierron.calibre.configuration.DeviceMode;
import com.gmail.dpierron.calibre.datamodel.Book;
import com.gmail.dpierron.calibre.datamodel.filter.BookFilter;
import com.gmail.dpierron.tools.Helper;
import com.gmail.dpierron.calibre.opds.secure.SecureFileManager;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CatalogManager {
  private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CatalogManager.class);
  private File catalogFolder;
  private BookFilter featuredBooksFilter;
  private List<CachedFile> listOfFilesToCopy = new LinkedList<CachedFile>();
  private List<String> listOfFilesPathsToCopy = new LinkedList<String>();
  private Map<String, Book> mapOfBookByPathToCopy = new HashMap<String, Book>();
  private Map<String, String> mapOfCatalogFolderNames = new HashMap<String, String>();
  private List<File> bookEntriesFiles = new LinkedList<File>();

  public CatalogManager() {
    super();
  }

  /**
   * Get the current catalog folder
   * @return
   */
  public File getCatalogFolder() {
    return catalogFolder;
  }

  /**
   * Set the catalog folder given the parth to the parent
   * The name takes into account teh configuration settings and mode
   * @param parentfolder
   */
  public void setCatalogFolder(File parentfolder) {
    catalogFolder = new File(parentfolder, getCatalogFolderName());
    if (!catalogFolder.exists()) {
      // ITIMPI:  The following assert may now be redundant!  Added while investigating a bug
      assert  !catalogFolder.getName().endsWith("_Page");
      catalogFolder.mkdirs();
    }
  }

  /**
   * Get the name of the catalog folder.
   * It will take into account the current mode if relevant
   * @return
   */
  public String getCatalogFolderName() {
    if (ConfigurationManager.INSTANCE.getCurrentProfile().getDeviceMode() == DeviceMode.Nook)
      return Constants.NOOK_CATALOG_FOLDERNAME;
    else
      return  ConfigurationManager.INSTANCE.getCurrentProfile().getCatalogFolderName();
  }

  /**
   *
   * @return
   */
  public List<String> getListOfFilesPathsToCopy() {
    return listOfFilesPathsToCopy;
  }

  /**
   *
   * @param pathToCopy
   * @return
   */
  public Book getBookByPathToCopy(String pathToCopy) {
    return mapOfBookByPathToCopy.get(pathToCopy);
  }

  /**
   *
   * @param file
   */
  void addFileToTheMapOfFilesToCopy(CachedFile file) {
    addFileToTheMapOfFilesToCopy(file, null);
  }

  /**
   *
   * @param file
   * @param book
   */
  void addFileToTheMapOfFilesToCopy(CachedFile file, Book book) {
    final String databasePath = ConfigurationManager.INSTANCE.getCurrentProfile().getDatabaseFolder().getAbsolutePath();
    final int databasePathLength = databasePath.length() + 1;

    if (file == null)
      return;

    if (listOfFilesToCopy.contains(file))
      return;

    String filePath = file.getAbsolutePath();

    if (!filePath.startsWith(databasePath))
      return; // let's not copy files outside the database folder

    String relativePath = filePath.substring(databasePathLength);
    listOfFilesPathsToCopy.add(relativePath);
    mapOfBookByPathToCopy.put(relativePath, book);
    listOfFilesToCopy.add(file);
  }

  /**
   *
   * @param catalogFileName
   * @return
   */
  public String getCatalogFileUrlInItsSubfolder(String catalogFileName) {
    return getCatalogFileUrlInItsSubfolder(catalogFileName, true);
  }

  /**
   *
   * @param catalogFileName
   * @param weAreAlsoInASubFolder
   * @return
   */
  public String getCatalogFileUrlInItsSubfolder(String catalogFileName, boolean weAreAlsoInASubFolder) {
    String catalogFolderName = mapOfCatalogFolderNames.get(catalogFileName);
    if (Helper.isNullOrEmpty(catalogFolderName)) {
      storeCatalogFileInSubfolder(catalogFileName);
      catalogFolderName = mapOfCatalogFolderNames.get(catalogFileName);
    }
    return (weAreAlsoInASubFolder ? "../" : "") + FeedHelper.INSTANCE.urlEncode(catalogFolderName) + "/" + FeedHelper.INSTANCE.urlEncode(catalogFileName);
  }

  /**
   * Givnn the current catalog filename, work out the prefix that needs
   * to be added to a URL to get back to the catalog root folder
   *
   * @param catalogFileName
   * @return Relative path to catalog root
   */
  public String getPathToCatalogRoot(String catalogFileName) {
    return getPathToCatalogRoot(catalogFileName, true);
  }

  /**
   * Givnn the current catalog filename, work out the prefix that needs
   * to be added to a URL to get back to the catalog root folder
   *
   * @param catalogFileName
   * @return Relative path to catalog root
   */
  public String getPathToCatalogRoot(String catalogFileName, boolean weAreAlsoInASubFolder) {
    if (logger.isTraceEnabled()) {
      logger.trace("getPathToCatalogRoot: catalogFileName=" + catalogFileName);
      logger.trace("getPathToCatalogRoot: weAreAlsoInSubFolder=" + weAreAlsoInASubFolder);
    }
    String catalogFolderName = mapOfCatalogFolderNames.get(catalogFileName);
    if (Helper.isNullOrEmpty(catalogFolderName)) {
      storeCatalogFileInSubfolder(catalogFileName);
      catalogFolderName = mapOfCatalogFolderNames.get(catalogFileName);
    }
    // Now derive a relative path to catalog root
    String result;

    result = weAreAlsoInASubFolder ? "../" : "";
    if (logger.isTraceEnabled())
      logger.trace("getPathToCatalogRoot: catalogFolderName=" + catalogFolderName);
    for (int i = 0; -1 != catalogFolderName.indexOf('/', i); i++) {
      result = result + "../";
    }
    result = FeedHelper.INSTANCE.urlEncode(result, true);
    if (logger.isTraceEnabled())
      logger.trace("getPathToCatalogRoot=" + result);
    return result;
  }

  /**
   * Get the Folder that a particular catalog file belongs in
   *
   * Any File extension or 'Page' sections should be removed before
   * we try and determine the folder the particular file belongs in.
   *
   * @param pCatalogFileName
   * @return
   */
  String getFolderName(String pCatalogFileName) {
    if (Helper.isNullOrEmpty(pCatalogFileName))
      return "";

    // Gee if there is any page information or file extensionto remove
    String catalogFileName;
    int pos = pCatalogFileName.indexOf(Constants.PAGE_DELIM);
    if (pos > -1) {
      // Remove the _Page ... part of the name
      catalogFileName = pCatalogFileName.substring(0, pos);
    } else {
      pos = pCatalogFileName.lastIndexOf('.');
      if (pos > -1) {
        // Remove any file extension
        catalogFileName = pCatalogFileName.substring(0, pos);
      } else {
        // ITIMPI - this sounds like an error to me that should be logged?
        System.out.println("");
        catalogFileName = pCatalogFileName;
      }
    }
    // Remove and hash added while encrypting to folder name
    // ITIMPI:  Need to check if this case can occur?
    String temp = SecureFileManager.INSTANCE.decode(catalogFileName);
    if (Helper.isNotNullOrEmpty(temp)) {
      catalogFileName = temp;
    }

    // ITIMPI:  We might want to revisit if this is the best way to
    //          identify the folder that a file is to belocated in?
    //          An alternative wcheme might be more robust?

    pos = catalogFileName.indexOf("_");
    switch (pos) {
      case 32:
      case 24:
      case 16:
      case 8:
          return catalogFileName.substring(0, pos);
      default:
          // Fallthru
    }
/*
    if (catalogFileName.length() >= 32 && !catalogFileName.substring(0, 32).contains("_"))
      return catalogFileName.substring(0, 32);

    if (catalogFileName.length() >= 24 && !catalogFileName.substring(0, 24).contains("_"))
      return catalogFileName.substring(0, 24);

    if (catalogFileName.length() >= 16 && !catalogFileName.substring(0, 16).contains("_"))
      return catalogFileName.substring(0, 16);

    if (catalogFileName.length() >= 8 && !catalogFileName.substring(0, 8).contains("_"))
      return catalogFileName.substring(0, 8);
*/
    pos = catalogFileName.lastIndexOf('_');
    if (pos < 0)
      return "";
    return catalogFileName.substring(0, pos);

  }

  /**
   * Get the full path for a file in a catalog.
   *
   * @param catalogFileName
   * @return
   */
  public File storeCatalogFileInSubfolder(String catalogFileName) {
    String folderName = mapOfCatalogFolderNames.get(catalogFileName);
    if (folderName == null) { // check in which folder this file goes
      folderName = getFolderName(catalogFileName);
      File folder = new File(getCatalogFolder(), folderName);
      if (!folder.exists())  {
        if (folder.getName().endsWith("_Page"))
          assert true;
        folder.mkdirs();
      }
    }
    mapOfCatalogFolderNames.put(catalogFileName, folderName);
    File result = new File(new File(getCatalogFolder(), folderName), catalogFileName);
    return result;
  }

  /**
   * Add a book entry to the given catalog
   *
   * @param bookEntry
   * @return true if book was added because not already there
   *         false if not added because already present
   */
  public boolean addBookEntryFile(File bookEntry) {
    if (bookEntriesFiles.contains(bookEntry))
      return false;

    bookEntriesFiles.add(bookEntry);
    return true;

  }

  /**
   *
   * @return
   */
  public BookFilter getFeaturedBooksFilter() {
    return featuredBooksFilter;
  }

  /**
   *
   * @param featuredBooksFilter
   */
  public void setFeaturedBooksFilter(BookFilter featuredBooksFilter) {
    this.featuredBooksFilter = featuredBooksFilter;
  }
}
