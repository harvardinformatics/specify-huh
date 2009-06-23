/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ku.brc.services.filteredpush;

import org.apache.log4j.Logger;

public class FilteredPushServer
{
    private static Logger log = Logger.getLogger(FilteredPushServer.class);
    
    private FilteredPushListenerIFace listener;

    private Thread sleeper;

    public FilteredPushServer(FilteredPushListenerIFace listener)
    {
        this.listener = listener;
        run();
    }
    
    public boolean connect()
    {
        return true;
    }

    public void disconnect()
    {
        if (sleeper != null && sleeper.isAlive()) sleeper.interrupt();
    }
    
    /** Wait for 10 seconds and then send an annotation message
     * @param args
     * @throws IOException 
     */
    private void run()
    {
        // wait 10 seconds and then send an annotation
        
        sleeper = new Thread()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(10000);
                }
                catch (InterruptedException e)
                {
                    return;
                }
                
                final String annotationMessage = "This is an annotation";
                
                getListener().notification(
                        new FilteredPushEvent()
                        {
                            public String getMessage()
                            {
                                return annotationMessage;
                            }
                        });
            }
        };
        sleeper.start();
    }

    private FilteredPushListenerIFace getListener()
    {
        return listener;
    }
    
    public static void main(String[] args)
    {
        System.out.println("Main");

        FilteredPushListenerIFace fpClient =
            new FilteredPushListenerIFace()
        {
            @Override
            public void notification(FilteredPushEvent e)
            {
                System.out.println("Got message: " + e.getMessage());

            }

        };

        FilteredPushServer fpServer = new FilteredPushServer(fpClient);
    }
}
