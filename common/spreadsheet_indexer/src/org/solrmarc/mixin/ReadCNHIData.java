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

public class ReadCNHIData extends ReadSpreadsheetData
{    
    public static void main(String[] args) 
    {
        ReadCNHIData cnhi = new ReadCNHIData();
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
        doctypes.add("klugeruhe");
        doctypes.add("cnhi");
        
        addResult(result, "doc_type_facet", doctypes);
        addResult(result, "availability_display", "This item is available for use only in the Eleanor Crowder Bjoring Center for Nursing Historical Inquiry, housed in the University of Virginia School of Nursing, McLeod Hall #1010. For information on viewing this item, please visit the <a href=\"https://www.nursing.virginia.edu/research/cnhi/\">Bjoring Center for Nursing Historical Inquiry</a> web page.");
        addResult(result, "source_facet", "Bjoring Center for Nursing Historical Inquiry");
        addResult(result, "library_facet", "Bjoring Center for Nursing Historical Inquiry");
        addResult(result, "shadowed_location_facet", "VISIBLE");
        addResult(result, "language_facet", "English");
        
        // format_facet column 20
        boolean is_video = false;
        Set<String> formatSet = new LinkedHashSet<String>();
        if (!parts[20].equals("")) 
        {
            String format = parts[20].trim();
            if (format.equals("DVD"))                  { is_video = true; formatSet.add("Video"); formatSet.add("DVD"); }
            else if (format.equals("Videocassette"))   { is_video = true; formatSet.add("Video"); formatSet.add("VHS"); }
            else if (format.equals("Book"))            { formatSet.add("Book"); }
            else if (format.equals("Pamphlet"))        { formatSet.add("Book"); }
            else if (format.equals("Journal"))         { formatSet.add("Journal/Magazine"); }
            else if (format.equals("Dissertation"))    { formatSet.add("Thesis/Dissertation"); }
            else if (format.equals("Other"))           { formatSet.add("Thesis/Dissertation"); }
            else                                       { System.err.println("No Format Found: "+ id + " : " + parts[20]); showAllParts(parts); }
            addResult(result, "format_facet", formatSet);

        }
        
        // title_display title_text  title_sort_facet, subtitle_text subtitle_display column 1
        if (!parts[1].equals("")) 
        {
            String[] title = DataUtil.cleanData(parts[1]).split(":", 2);
            title[0] = DataUtil.cleanData(title[0]);
            addResult(result, "title_display", title[0]);
            addResult(result, "title_text", title[0]);
            if (title.length > 1 && title[1].length() > 0)
            {
                title[1] = DataUtil.cleanData(title[1]);
                addResult(result, "subtitle_display", title[1]);
                addResult(result, "subtitle_text", title[1]);
            }
            String titleSort = parts[1];
            titleSort = titleSort.toLowerCase();
            titleSort = titleSort.replaceAll("[^-a-z0-9: ]", " ");
            titleSort = titleSort.replaceAll("[ ]?:[ ]?", ":");
            titleSort = titleSort.replaceAll("[ ][ ]+", " ").trim();
            titleSort = titleSort.replaceAll("^(the |a |le )", "");
            addResult(result, "title_sort_facet", titleSort);
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
        addResult(result, "author_text", authors);
        addResult(result, "author_display", authors);
        addResult(result, "author_facet", authors);
        String firstAuthor = null;
        try {
            firstAuthor = authors.iterator().next().trim();
            firstAuthor = firstAuthor.toLowerCase().replaceAll("[^-a-z0-9 ]", " ").replaceAll("[ ][ ]+", " ");
            addResult(result, "author_sort_facet", firstAuthor);
        }
        catch (NoSuchElementException nsee)
        {
            // eat it.
        }
        
        // edition -> edition_display
        addResult(result, "edition_display", parts[12]);
        
        String year = null;
        // year_multisort_i column 14
        if (!parts[14].equals("")) 
        { 
            year = parts[14].replaceAll("^[^0-9]*([0-9][0-9][0-9][0-9]).*", "$1"); 
            if (year.matches("^[0-9][0-9][0-9][0-9]$"))
            { 
                addResult(result, "year_multisort_i", year);
                addResult(result, "published_date_display", year);
            }
        }
        
        // published info
        if (!parts[13].isEmpty() && !parts[14].isEmpty())
        {
            String published = parts[13].trim() + ", " + parts[14].trim();
            addResult(result, "published_display", published.trim());
            addResult(result, "published_text", published.trim());
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

        addResult(result, "subject_text",subjects);
        addResult(result, "subject_facet", subjects);

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

        addResult(result, "isbn_text", isbnSet);
        addResult(result, "isbn_display", isbnSet);

        // LC call number
        if (!parts[16].isEmpty())
        {
            String lcnum = parts[16].trim();
            super.handleLCNum(result, lcnum, id, firstAuthor, year);
        }
    return(result);
}

}
