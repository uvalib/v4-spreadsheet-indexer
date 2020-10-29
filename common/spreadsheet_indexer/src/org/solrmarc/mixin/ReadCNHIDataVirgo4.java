package org.solrmarc.mixin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.solrmarc.callnum.CallNumUtils;
import org.solrmarc.tools.DataUtil;

public class ReadCNHIDataVirgo4 extends ReadSpreadsheetDataVirgo4
{    
    public static void main(String[] args) 
    {
        ReadCNHIDataVirgo4 cnhi = new ReadCNHIDataVirgo4();
        try
        {
            cnhi.initialize(args);
            cnhi.process();
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public Map<String, Map<String,String>> processInputLine(String[] labels, String[] parts)
    {
        Map<String, Map<String, String>> result = new LinkedHashMap<String, Map<String, String>>();

        // ID column 0
        String id = "cnhi_" + parts[0];
        addResult(result, "id", id);

        Collection<String> doctypes = new LinkedHashSet<String>();
        doctypes.add("cnhi");
        
        addResult(result, "doc_type_f_stored", doctypes);
        addResult(result, "data_source_f_stored", doctypes);
        addResult(result, "availability_a", "This item is available for use only in the Eleanor Crowder Bjoring Center for Nursing Historical Inquiry, housed in the University of Virginia School of Nursing, McLeod Hall #1010. For information on viewing this item, please visit the <a href=\"https://www.nursing.virginia.edu/research/cnhi/\">Bjoring Center for Nursing Historical Inquiry</a> web page.");
        addResult(result, "source_f_stored", "Bjoring Center for Nursing Historical Inquiry");
        addResult(result, "library_f_stored", "Bjoring Center for Nursing Historical Inquiry");
        addResult(result, "shadowed_location_f_stored", "VISIBLE");
        addResult(result, "language_f_stored", "English");
        
        // format_facet column 20
        boolean is_video = false;
        String pool = "catalog";
        Set<String> formatSet = new LinkedHashSet<String>();
        if (!parts[20].equals("")) 
        {
            String format = parts[20].trim();
            if (format.equals("DVD"))                  { is_video = true; formatSet.add("Video"); formatSet.add("DVD");  pool="video";  }
            else if (format.equals("Videocassette"))   { is_video = true; formatSet.add("Video"); formatSet.add("VHS");  pool="video"; }
            else if (format.equals("Book"))            { formatSet.add("Book"); }
            else if (format.equals("Pamphlet"))        { formatSet.add("Book"); }
            else if (format.equals("Journal"))         { formatSet.add("Journal/Magazine"); pool="serials"; }
            else if (format.equals("Dissertation"))    { formatSet.add("Thesis/Dissertation"); pool="serials";}
            else if (format.equals("Other"))           { formatSet.add("Thesis/Dissertation"); pool="serials";}
            else if (format.equals("CD-ROM"))          { formatSet.add("Computer Media"); }
            else                                       { System.err.println("No Format Found: "+ id + " : " + parts[20]); showAllParts(parts); formatSet.add("Book");  }
            addResult(result, "format_f_stored", formatSet);
            addResult(result, "pool_f_stored", pool);
        }
        
        // title_display title_text  title_sort_facet, subtitle_text subtitle_display column 1
        String titleSort = "";
        if (!parts[1].equals("")) 
        {
            String[] title = DataUtil.cleanData(parts[1]).split(":", 2);
            title[0] = DataUtil.cleanData(title[0]);
            addResult(result, "title_tsearch_stored", title[0]);
            if (title.length > 1 && title[1].length() > 0)
            {
                title[1] = DataUtil.cleanData(title[1]);
                addResult(result, "title_sub_tsearch_stored", title[1]);
            }
            titleSort = parts[1];
            titleSort = titleSort.toLowerCase();
            titleSort = titleSort.replaceAll("[^-a-z0-9: ]", " ");
            titleSort = titleSort.replaceAll("[ ]?:[ ]?", ":");
            titleSort = titleSort.replaceAll("[ ][ ]+", " ").trim();
            titleSort = titleSort.replaceAll("^(the |a |le )", "");
            addResult(result, "title_ssort_stored", titleSort);
        }

        addResult(result, "series_title_text", parts[2]);
        addResult(result, "series_title_facet", parts[2]);
        
        // Author Stuff 3,4  5,6  7,8  9,10  11
        Collection<String> authors = new LinkedHashSet<String>();
        if (!parts[3].isEmpty() && !parts[4].isEmpty())
        {
            authors.add(parts[3].trim() + ", " + parts[4].trim());
        }
        if (!parts[5].isEmpty() && !parts[6].isEmpty())
        {
            authors.add(parts[5].trim() + ", " + parts[6].trim());
        }
        if (!parts[7].isEmpty() && !parts[8].isEmpty())
        {
            authors.add(parts[7].trim() + ", " + parts[8].trim());
        }
        if (!parts[9].isEmpty() && !parts[10].isEmpty())
        {
            authors.add(parts[9].trim() + ", " + parts[10].trim());
        }
        if (!parts[11].isEmpty())
        {
            authors.add(parts[11].trim());
        }
        addResult(result, "author_tsearch_stored", authors);
        addResult(result, "author_facet_f_stored", authors);
        String firstAuthor = null;
        try {
            firstAuthor = authors.iterator().next().trim();
            firstAuthor = firstAuthor.toLowerCase().replaceAll("[^-a-z0-9 ]", " ").replaceAll("[ ][ ]+", " ");
            addResult(result, "author_ssort_stored", firstAuthor);
        }
        catch (NoSuchElementException nsee)
        {
            // eat it.
        }
        
        // edition -> edition_display
        addResult(result, "edition_a", parts[12]);
        
        String year = null;
        if (!parts[5].equals("")) 
        { 
            year = parts[5].replaceAll("^[^0-9]*([0-9][0-9][0-9][0-9]).*", "$1"); 
            if (year.matches("^[0-9][0-9][0-9][0-9]$"))
            { 
                addResult(result, "published_date", year+"-01-01T00:00:00.0Z");
                addResult(result, "published_display_a", year);
                addResult(result, "published_daterange", year);
            }
        }
        
        // published info
        if (!parts[13].isEmpty() && !parts[14].isEmpty())
        {
            String published = parts[13].trim() + ", " + parts[14].trim();
            addResult(result, "published_tsearch_stored", published.toString());
        }

        // LCSH1 -> subject_facet subject_text
        Collection<String> subjects = new LinkedHashSet<String>();

        if (!parts[17].isEmpty())
        {
            subjects.add(fixSubject(parts[17]));
        }

        if (!parts[18].isEmpty())
        {
            subjects.add(fixSubject(parts[18]));
        }

        if (!parts[19].isEmpty())
        {
            subjects.add(fixSubject(parts[19]));
        }

        addResult(result, "subject_tsearchf_stored", subjects);

        // ISBN column 15
        String isbnStr[] = parts[15].split(" ");
        Set<String> isbnSet = new LinkedHashSet<String>();
        for (String isbn : isbnStr)
        {
            try { 
                Collection<String> isbns = ISBNNormalizer.filterISBN(Collections.singletonList(isbn), "both");
                isbnSet.addAll(isbns);
            }
            catch (IllegalArgumentException iae)
            {
                // eat it!
            }
        }

        addResult(result, "isbn_isbn_stored", isbnSet);

        // LC call number
        if (!parts[16].isEmpty())
        {
            String lcnum = parts[16].trim();
            super.handleLCNum(result, lcnum, id, firstAuthor, year);
        }
        if (!result.containsKey("work_title2_key_ssort_stored"))
        {
        	String format = (formatSet.size() > 0 ) ? formatSet.iterator().next() : "";
        	String worktitlekey = titleSort + "/" + firstAuthor + "/" + format;
        	addResult(result, "work_title2_key_ssort_stored", worktitlekey );
        }
        if (!result.containsKey("work_title3_key_ssort_stored") && is_video)
        {
        	String worktitlekey = titleSort + "/" + firstAuthor + "/" + "Video";
        	addResult(result, "work_title3_key_ssort_stored", worktitlekey );
        }

    return(result);
}

}
