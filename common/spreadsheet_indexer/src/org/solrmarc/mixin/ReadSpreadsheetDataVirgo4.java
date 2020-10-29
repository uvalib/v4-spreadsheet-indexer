package org.solrmarc.mixin;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.Map;

import org.solrmarc.callnum.CallNumUtils;
import org.solrmarc.driver.Boot;

public abstract class ReadSpreadsheetDataVirgo4 extends ReadSpreadsheetData
{

    public void handleLCNum(Map<String, Map<String,String>> result, String lcnum, String id, String firstAuthor, String year)
    {
        addResult(result, "call_number_tsearch_stored", lcnum);
        if (!lcnum.startsWith("Audio") && !lcnum.startsWith("audio") && !lcnum.startsWith("Video"))
        {
            String firstAuthorCutter = (firstAuthor == null) ? null : org.solrmarc.callnum.Utils.getCutterFromAuthor(firstAuthor);
            String uniquishLCNum = getUniquishLCCallNumber(lcnum, firstAuthorCutter, year);
            addResult(result, "lc_call_number_e_stored", uniquishLCNum);
            try
            {
                String call_numFacet = getCallNumberPrefixNew(id, lcnum, "translation_maps/call_number_detail_map.properties", "0");
                if (call_numFacet != null)
                {
                    addResult(result, "call_number_narrow_f_stored", call_numFacet);
                }
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                
                String shelfkey = getUniquishLCShelfKey(uniquishLCNum, null,  null, id);
                String reverse_shelfkey = CallNumUtils.getReverseShelfKey(shelfkey);
                addResult(result, "shelfkey", shelfkey);
                addResult(result, "reverse_shelfkey", reverse_shelfkey);
            }
            catch (IllegalArgumentException iae)
            {
                System.err.println("shelfkey exception: "+ iae.getMessage());
            }
        }
    }

    /**
     *   Find the location of where this class is running from
     *   When run normally this would be the main solrmarc jar
     *   when run from classdirs in eclipse, is is the project location
     *
     *   @return  String - location of where this class is running from.  Used
     *                      as default search location for local configuration
     *                      files (As a side effect, sets System Property
     *                      solrmarc.jar.dir to this same value so it can be
     *                      referenced in log4j.properties)
     */
    public static String getDefaultHomeDir()
    {
        CodeSource codeSource = Boot.class.getProtectionDomain().getCodeSource();
        String jarDir;
        File jarFile = null;
        try
        {
            jarFile = new File(codeSource.getLocation().toURI().getPath());
        }
        catch (URISyntaxException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (jarFile.getName().endsWith(".jar"))
        {
            jarDir = jarFile.getParentFile().getPath();
        }
        else
        {
            // Not running from a jar. Probably running from eclipse or other
            // IDE
            jarDir = new File(".").getAbsoluteFile().getParentFile().getAbsolutePath();
        }
        System.setProperty("jar.dir", jarDir);
        return(jarDir);
    }

}