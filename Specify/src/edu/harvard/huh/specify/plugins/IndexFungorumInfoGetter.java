package edu.harvard.huh.specify.plugins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import edu.ku.brc.helpers.HTTPGetter;
import edu.ku.brc.helpers.XMLHelper;

/*
 * mmk: taken from FishBaseInfoGetter
 *
 */
public class IndexFungorumInfoGetter extends HTTPGetter
{
    private static final Logger log = Logger.getLogger(IndexFungorumInfoGetter.class);

    protected IndexFungorumInfoGetterListener consumer;

    protected String searchText;

    protected File cacheDir = null;
    protected Element   dom      = null;
    protected String    data     = null;


    public IndexFungorumInfoGetter(final IndexFungorumInfoGetterListener consumer,
    		final String searchText, File cacheDir)
    {
    	this.consumer = consumer;
    	this.searchText = searchText;
    	this.cacheDir = cacheDir;    	
    }

    public void setCacheDir(File cacheDir) {
    	this.cacheDir = cacheDir;
    }
    
    /**
     * @return
     */
    public Element getDom()
    {
        return dom;
    }

    /**
     * @param consumer
     */
    public void setConsumer(IndexFungorumInfoGetterListener consumer)
    {
		this.consumer = consumer;
	}
    

    /**
     * Performs a "generic" HTTP request and fill member variable with results use
     * "getDigirResultsetStr" to get the results as a String
     *
     */
    public void getDOMDoc(String searchText)
    {
    	if (searchText == null) {
    		log.error("no search text");
    		status = ErrorCode.Error;
    		return;
    	}
    	
    	try
    	{
    		File cachedFile = getCacheFile(cacheDir, searchText);
    		if (cachedFile.exists()) {
    			dom = XMLHelper.readFileToDOM4J(cachedFile);
    			return;
    		}

    		String url = getUrl(searchText);

    		log.debug("requesting " + url);

    		byte[] bytes = super.doHTTPRequest(url);

    		data = new String(bytes);

    		try {
    			writeCacheFile(cachedFile, data);
    			dom = XMLHelper.readFileToDOM4J(cachedFile);
    		}
    		catch (IOException e) {
    			log.error(e);
    			status = ErrorCode.Error;
    		}

    	}
    	catch (Exception ex)
    	{
    		log.error(ex.getMessage(), ex);
    		ex.printStackTrace();
    		status = ErrorCode.Error;
    	}
    }


    public void run()
    {
    	log.debug("run");

    	getDOMDoc(searchText);

    	if (consumer != null)
    	{
    		if (status == ErrorCode.NoError)
    		{
    			consumer.infoArrived(this);

    		} else
    		{
    			consumer.infoGetWasInError(this);
    		}
    	}
    	stop();
    }
    
    protected String getUrl(String searchText) throws UnsupportedEncodingException {
    	
    	boolean anywhereInText = false;
    	long maxNumber = 10;
    	
    	String url = "http://www.indexfungorum.org/ixfwebservice/fungus.asmx/NameSearch?" +
				"SearchText=" + URLEncoder.encode(searchText, "UTF-8") +
				"&AnywhereInText=" + String.valueOf(anywhereInText) +
				"&MaxNumber=" + String.valueOf(maxNumber);
    	
    	return url;
    }
    
    protected File getCacheFile(File cacheDir, String searchText) {
    	String fileName = searchText.replaceAll("[^\\w]+", "_") + ".xml";
    	File file = new File(cacheDir, fileName);
    	return file;
    }

    protected void writeCacheFile(File cacheFile, String data) throws IOException {
		int inx = data.indexOf("<?");
		if (inx < 0) {
			log.error("data is not xml");
			log.error(data);
			status = ErrorCode.Error;
			return;
		}

		data = data.substring(inx, data.length());

		FileWriter fileWriter = new FileWriter(cacheFile);
		Writer output = new BufferedWriter(fileWriter);
		output.write(data);
		output.flush();
		output.close();
    }
}
