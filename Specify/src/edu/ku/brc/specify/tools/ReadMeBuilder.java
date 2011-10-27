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
/**
 * 
 */
package edu.ku.brc.specify.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilderFactory;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import edu.ku.brc.helpers.XMLHelper;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rods
 * @author Paul J. Morris
 * @author David Lowery
 *
 * @code_status Beta
 *
 * Created Date: Jul 8, 2009
 * 
 * Produce a release notes readme.html file listing bugs from an xml view of bugzilla bugs.
 * 
 * Placed in packaging/readme.html, the release notes will be included by install4j.
 * 
 * File bugs in bugzilla under a version number of the software, then, when ready to release 
 * the next version, search for bugs under the version number, and save the search results
 * as xml from bugzilla.  Point this application at the bugzilla xml file, and it will generate
 * a short list of the resolved bugs as a release notes document. 
 *
 */
public class ReadMeBuilder
{

    protected Vector<BugInfo> list = new Vector<BugInfo>();
    
    public ReadMeBuilder()
    {
        super();
    }

    /**
     * Brings up a dialog for user to pick an xml file that contains a list of bugzilla bugs, 
     * processes this file, and writes a list of the resolved bugs in readme.html
     */
    public void process()
    {
        JFileChooser chooser    = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (path != null)
            {
                try
                {
                    Element root = XMLHelper.readFileToDOM4J(new File(path));
                    if (root != null)
                    {
                        for (Object bugObj : root.selectNodes("bug"))
                        {
                            Element node = (Element)bugObj;
                            
                            String bugId = ((Node)node.selectObject("bug_id")).getText();
                            String bug_status = ((Node)node.selectObject("bug_status")).getText();
                            // Exclude bugs that haven't been resolved.
                            if (bug_status.equals("RESOLVED")) { 
                                list.add(new BugInfo(
                                		((Node)node.selectObject("short_desc")).getText(), 
                                		((Node)node.selectObject("delta_ts")).getText(), 
                                		bugId,
                                		((Node)node.selectObject("resolution")).getText(),
                                		((Node)node.selectObject("assigned_to")).getText()
                                		));
                                System.out.println(bugId);
                            } else { 
                            	System.out.println("Excluding " + bugId + " " + bug_status);
                            }
                        }
                    }
                    
                    Collections.sort(list);
                    
                    PrintWriter pw = new PrintWriter(XMLHelper.getConfigDir(".." + File.separator + "packaging" + File.separator + "readme.html"));
                    //TODO: Why start by closing the document?  Is this for install4j? 
                    pw.append("</body>\n</html>\n");
                    pw.append("<html>\n");
                    pw.append("<head>\n");
                    pw.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n");
                    pw.append("<title>Release Notes</title>\n");
                    pw.append("</head>\n");
    
                    pw.append("<body>\n");
                    pw.append("<h3>Release Notes</h3>\n");
                    pw.append("<h3>"+getTitle()+"</h3>\n");
                    pw.append("<br />\n");
                    pw.append("<h3>Resolved Issues</h3>\n");
                    pw.append("<ol>\n");
                    
                    for (BugInfo bi : list)
                    {
                        pw.append("<li>");
                        pw.append(bi.getResolution());
                        pw.append("&nbsp;&nbsp;");
                        pw.append(bi.getText());
                        pw.append("&nbsp;&nbsp;");
                        pw.append(bi.getDate());
                        pw.append("&nbsp;&nbsp;(");
                        pw.append(bi.getNum());
                        pw.append("&nbsp;&nbsp;");
                        pw.append(bi.getAssigned_to());
                        pw.append(").</li>\n");
                    }
                    pw.append("</ol>\n");
                    for (int i=0;i<4;i++)
                    {
                        pw.append("<BR>\n");
                    }
                    pw.append("</body>\n</html>\n");
                    
                    pw.close();
                    
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                
                UIRegistry.showLocalizedMsg("Done.");
                System.exit(0);
            }
        }
    }
    /**
     * Alternative to Specify.getTitle() used before changes. This uses the HUH
     * install4j configuration to obtain a version number.
     * 
     * @return
     */
    private String getTitle() {
    	String title = "";
	    try {
	    	SAXReader reader = new SAXReader();
	        Document document = reader.read(new FileInputStream(XMLHelper.getConfigDir(".." + File.separator + "packaging" + File.separator + "huh-linux.install4j")));
	        Node node = document.selectSingleNode("/install4j/application");
	        // TODO: auto-increment version #
	        title = node.valueOf("@name") + " " + node.valueOf("@version");
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
			return title;
	}

	/**
     * @param args
     */
    public static void main(String[] args)
    {
        
        
        SwingUtilities.invokeLater(new Runnable() {
            @SuppressWarnings("synthetic-access") //$NON-NLS-1$
          public void run()
            {
                ReadMeBuilder rmb = new ReadMeBuilder();
                rmb.process();
            }
        });

    }

    /**
     * Proxy object for a subset of bugzilla bug concepts.
     *
     */
    class BugInfo implements Comparable<BugInfo>
    {
        protected String text;
        protected String date;
        protected String num;
        protected String resolution;
		protected String assigned_to;
        
        public BugInfo(String text, String date, String num, String resolution, String assigned_to)
        {
            super();
            this.text = text;
            this.date = date;
            this.num = num;
            this.resolution = resolution;
            this.assigned_to = assigned_to;
        }
        /**
         * @return the text
         */
        public String getText()
        {
            return text;
        }
        /**
         * @return the date
         */
        public String getDate()
        {
            return date;
        }
        /**
         * @return the num
         */
        public String getNum()
        {
            return num;
        }
        /**
		 * @return the resolution
		 */
		public String getResolution() {
			return resolution;
		}
		/**
		 * @return the assigned_to
		 */
		public String getAssigned_to() {
			return assigned_to;
		}
        
        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(BugInfo o)
        {
            int rv = date.compareTo(o.date);
            if (rv == 0)
            {
                rv = num.compareTo(o.num);
            }
            return rv;
        }
    }
}
