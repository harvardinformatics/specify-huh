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
package edu.ku.brc.helpers;


import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

/**
 *
 * @code_status Unknown (auto-generated)
 *
 * @author rods
 *
 */
public class HTTPGetter implements Runnable
{
    private static final Logger log = Logger.getLogger(HTTPGetter.class);

    public enum ErrorCode {
        NoError, Error, HttpError, NotDoneError, IOError, URLError
    }

    protected ErrorCode    status     = ErrorCode.NoError;
    protected Thread       thread     = null;
    protected String       urlStr     = null;
    
    protected HttpClient   httpClient = null;
    protected GetMethod    method     = null;
    protected InputStream  iStream    = null;


    /**
     * 
     */
    public HTTPGetter()
    {
        this.urlStr = null;
    }

    public HTTPGetter(final String urlStr, final boolean cacheFile)
    {
        this.urlStr = urlStr;
    }

    public String getUrlStr()
    {
        return urlStr;
    }

    public void setUrlStr(String urlStr)
    {
        this.urlStr = urlStr;
    }

    public ErrorCode getStatus()
    {
        return status;
    }

    /**
     * Performs a "generic" HTTP request and fill member variable with results
     * use "getDigirResultsetStr" to get the results as a String
     *
     * @param url URL to be executaed
     * @return returns the input stream for the body
     */
    public InputStream beginHTTPRequest(final String url) throws Exception
    {
        status = ErrorCode.NoError;

        // Create an HttpClient with the MultiThreadedHttpConnectionManager.
        // This connection manager must be used if more than one thread will
        // be using the HttpClient.
        httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());

        GetMethod mthod = new GetMethod(url);
        //method.setQueryString("q=xxx");

        log.debug("getting " + mthod.getURI()); //$NON-NLS-1$
        // execute the method
        httpClient.executeMethod(mthod);
        
        log.debug("Len: "+mthod.getResponseContentLength()); //$NON-NLS-1$
        return iStream = mthod.getResponseBodyAsStream();
    }
    
    /**
     * @return
     * @throws IOException
     */
    public int getLong() throws IOException
    {
        return iStream.read();
    }

    /**
     * Performs a "generic" HTTP request and fill member variable with results
     * use "getDigirResultsetStr" to get the results as a String
     *
     * @param url URL to be executaed
     * @return returns an error code
     */
    public byte[] doHTTPRequest(final String url)
    {
        byte[] bytes = null;
        status = ErrorCode.NoError;

        // Create an HttpClient with the MultiThreadedHttpConnectionManager.
        // This connection manager must be used if more than one thread will
        // be using the HttpClient.
        httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());

        GetMethod mthod = new GetMethod(url);
        try
        {

            log.debug("getting " + mthod.getURI()); //$NON-NLS-1$
            // execute the method
            httpClient.executeMethod(mthod);

            //System.out.println("Get executed"); //$NON-NLS-1$
            // get the response body as an array of bytes
            bytes = mthod.getResponseBody();

            log.debug(bytes.length + " bytes read"); //$NON-NLS-1$

        } catch (ConnectException ce)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HTTPGetter.class, ce);
            log.error("Could not make HTTP connection.  (" //$NON-NLS-1$
                    + ce.toString() + ")"); //$NON-NLS-1$
            status = ErrorCode.HttpError;

        } catch (HttpException he)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HTTPGetter.class, he);
            log.error("Http problem making request.  (" //$NON-NLS-1$
                    + he.toString() + ")"); //$NON-NLS-1$
            status = ErrorCode.HttpError;

        } catch (IOException ioe)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HTTPGetter.class, ioe);
            log.error("IO problem making request.  (" + ioe.toString() //$NON-NLS-1$
                    + ")"); //$NON-NLS-1$
            status = ErrorCode.IOError;

        } catch (Exception e)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HTTPGetter.class, e);
            log.error("Error: " + e); //$NON-NLS-1$
            status = ErrorCode.Error;

        } finally
        {
            // always release the connection after we're done
            mthod.releaseConnection();
            log.debug("Connection released"); //$NON-NLS-1$
        }
        return bytes;
    }

    // ----------------------------------------------------------------
    // -- Runnable Interface
    // ----------------------------------------------------------------
    public void start()
    {
        if (thread == null)
        {
            thread = new Thread(this);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    /**
     *
     *
     */
    public synchronized void stop()
    {
        if (thread != null)
        {
            thread.interrupt();
        }
        thread = null;
        notifyAll();
    }

    /**
     *
     */
    public void run()
    {
        //Thread me = Thread.currentThread();

        doHTTPRequest(urlStr);

        stop();
    }
}
