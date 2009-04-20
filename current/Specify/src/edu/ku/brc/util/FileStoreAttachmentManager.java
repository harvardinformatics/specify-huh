/* Copyright (C) 2009, University of Kansas Center for Research
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.ku.brc.util;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import edu.ku.brc.specify.datamodel.Attachment;

/**
 * An implementation of AttachmentManagerIface that uses the underlying filesystem to
 * provide storage of the attachments and thumbnails.
 *
 * @author jstewart
 * @code_status Beta
 */
public class FileStoreAttachmentManager implements AttachmentManagerIface
{
    /** The base directory path of where the files will be stored. */
    protected String baseDirectory;
    
    /** The directory inside the base that will store the original files. */
    protected File originalsDir;
    
    /** The directory inside the base that will store the thumbnail files. */
    protected File thumbsDir;
    
    /** 
     * A collection of all files created by calls to setStorageLocationIntoAttachment
     * that have not yet been filled by calls to storeAttachmentFile.
     */
    protected Vector<String> unfilledFiles;
    
    /**
     * Creates a new instance, setting baseDirectory to null.
     * @throws IOException if either of the storage directories is not writable
     */
    public FileStoreAttachmentManager(final File baseDirectory) throws IOException
    {
        setDirectory(baseDirectory);
    }
    
    /**
     * @param baseDir
     * @throws IOException
     */
    public void setDirectory(final File baseDir) throws IOException
    {
        this.baseDirectory = baseDir.getAbsolutePath();
        this.originalsDir  = new File(baseDirectory + File.separator + "originals");
        this.thumbsDir     = new File(baseDirectory + File.separator + "thumbnails");
        
        // create the directories, if they don't already exist
        originalsDir.mkdirs();
        thumbsDir.mkdirs();
        
        // make sure the directories are writable
        if (!originalsDir.canWrite())
        {
            throw new IOException("Storage directory not writable: " + originalsDir.getAbsolutePath());
        }
        if (!thumbsDir.canWrite())
        {
            throw new IOException("Storage directory not writable: " + originalsDir.getAbsolutePath());
        }
        
        unfilledFiles = new Vector<String>();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#getDirectory()
     */
    @Override
    public File getDirectory()
    {
        return new File(this.baseDirectory);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#setStorageLocationIntoAttachment(edu.ku.brc.specify.datamodel.Attachment)
     */
    public void setStorageLocationIntoAttachment(final Attachment attachment)
    {
        String attName    = attachment.getOrigFilename();
        int    lastPeriod = attName.lastIndexOf('.');
        String suffix     = ".att";
        if (lastPeriod!=-1)
        {
            // Make sure the file extension (if any) remains the same so the host
            // filesystem still sees the files as the proper types.  This is simply
            // to make the files browsable from a system file browser.
            suffix = ".att" + attName.substring(lastPeriod);
        }
        try
        {
            // find an unused filename in the originals dir
            File storageFile = File.createTempFile("sp6-", suffix, originalsDir);
            attachment.setAttachmentLocation(storageFile.getName());
            unfilledFiles.add(attachment.getAttachmentLocation());
        }
        catch (IOException e)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(FileStoreAttachmentManager.class, e);
            // TODO What should we do in this case?
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#getOriginal(edu.ku.brc.specify.datamodel.Attachment)
     */
    public File getOriginal(final Attachment attachment)
    {
        String fileLoc = attachment.getAttachmentLocation();
        File storedFile = new File(baseDirectory + File.separator + "originals" + File.separator + fileLoc);
        if (storedFile.exists())
        {
            return storedFile;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#getThumbnail(edu.ku.brc.specify.datamodel.Attachment)
     */
    public File getThumbnail(final Attachment attachment)
    {
        String fileLoc = attachment.getAttachmentLocation();
        File storedFile = new File(baseDirectory + File.separator + "thumbnails" + File.separator + fileLoc);
        if (storedFile.exists())
        {
            return storedFile;
        }
        return null;
    }


    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#storeAttachmentFile(edu.ku.brc.specify.datamodel.Attachment, java.io.File, java.io.File)
     */
    public void storeAttachmentFile(final Attachment attachment, final File attachmentFile, final File thumbnail) throws IOException
    {
        // copy the original into the storage system
        String attachLoc = attachment.getAttachmentLocation();
        File origFile = new File(baseDirectory + File.separator + "originals" + File.separator + attachLoc);
        FileUtils.copyFile(attachmentFile, origFile);
        
        // copy the thumbnail, if any, into the storage system
        if (thumbnail != null)
        {
            File thumbFile = new File(baseDirectory + File.separator + "thumbnails" + File.separator + attachLoc);
            FileUtils.copyFile(thumbnail, thumbFile);
        }
        
        // since we have now made use of the temp file we created earlier, we don't
        // need to keep track of it as an 'unfilled' file to be cleaned up later
        unfilledFiles.remove(attachment.getAttachmentLocation());
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#replaceOriginal(edu.ku.brc.specify.datamodel.Attachment, java.io.File)
     */
    public void replaceOriginal(final Attachment attachment, final File newOriginal, final File newThumbnail) throws IOException
    {
        String attachLoc = attachment.getAttachmentLocation();
        File origFile = new File(baseDirectory + File.separator + "originals" + File.separator + attachLoc);
        File thumbFile = new File(baseDirectory + File.separator + "thumbnails" + File.separator + attachLoc);

        replaceFile(origFile, newOriginal);
        replaceFile(thumbFile, newThumbnail);
    }
    
    /**
     * Replace origFile with newFile, attempting to recover from an exception if it occurs.
     * 
     * @param origFile the original file
     * @param newFile the replacement version
     * @throws IOException a disk IO error occurred during the process
     */
    protected void replaceFile(final File origFile, final File newFile) throws IOException
    {
        File tmpOrig = File.createTempFile("sp6-", ".tmp");
        FileUtils.copyFile(origFile, tmpOrig);
        try
        {
            // try to copy the new attachment into place
            // if this fails, we should try to copy the old original back into place
            FileUtils.copyFile(newFile, origFile);
        }
        catch (IOException e)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(FileStoreAttachmentManager.class, e);
            // if the attachment file differs from the original that we copied to a tmp location...
            if( !FileUtils.contentEquals(origFile, tmpOrig) )
            {
                // copy the tmp version back into place
                FileUtils.copyFile(tmpOrig, origFile);
            }
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#deleteAttachmentFiles(edu.ku.brc.specify.datamodel.Attachment)
     */
    public void deleteAttachmentFiles(final Attachment attachment) throws IOException
    {
        String attachLoc = attachment.getAttachmentLocation();
        File origFile = new File(baseDirectory + File.separator + "originals" + File.separator + attachLoc);
        File thumbFile = new File(baseDirectory + File.separator + "thumbnails" + File.separator + attachLoc);
        
        origFile.delete();
        thumbFile.delete();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#cleanup()
     */
    public void cleanup()
    {
        // delete all of the temp files we created that were never used for storing
        // attachment originals
        for (String unusedFile: unfilledFiles)
        {
            File f = new File(originalsDir.getAbsolutePath() + File.separator + unusedFile);
            f.delete();
        }
    }
}
