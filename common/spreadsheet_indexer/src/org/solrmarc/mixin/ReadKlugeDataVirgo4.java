package org.solrmarc.mixin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.solrmarc.callnum.CallNumUtils;
import org.solrmarc.tools.DataUtil;

public class ReadKlugeDataVirgo4 extends ReadSpreadsheetDataVirgo4
{
    Map<String, Map<String, String>> marcRecordMap = new LinkedHashMap<String,  Map<String, String>>();
    Set<String> oclcNumFound = new LinkedHashSet<String>();
    Set<String> idFound = new LinkedHashSet<String>();
    
    public static void main(String[] args) 
    {
        ReadKlugeDataVirgo4 kluge = new ReadKlugeDataVirgo4();
        try
        {
            kluge.initialize(args);
            kluge.process();
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

    public void initialize(String[] args) throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        boolean deleteArg[] = new boolean[args.length];
        int numToDelete = 0;
        Arrays.fill(deleteArg, false);
        String oclcnumLookupFilename = null;
        for (int argoffset = 0; argoffset < args.length ; argoffset++)
        {
            if (args[argoffset].equals("-m") && argoffset < args.length - 1) 
            { 
                deleteArg[argoffset] = true; 
                deleteArg[argoffset+1] = true; 
                oclcnumLookupFilename  = args[argoffset+1];
                numToDelete += 2;
            }
        }
        if (numToDelete > 0)
        {
            String newargs[] = new String[args.length - numToDelete];
            for (int i = 0, j = 0; i < args.length; i++)
            {
                if (!deleteArg[i])  newargs[j++] = args[i];
            }
            args = newargs;
        }
        super.initialize(args);
        if (oclcnumLookupFilename != null)
        {
            File indexTest = new File(oclcnumLookupFilename);
            if (indexTest.exists() && indexTest.canRead())
            {
                
                BufferedReader oclcReader = new BufferedReader(new InputStreamReader(new FileInputStream(indexTest), "UTF-8"));
                String line;
                while ((line = oclcReader.readLine()) != null)
                {
                    String linePieces[] = line.split(" : ", 2);
                    if (linePieces.length == 2)
                    {
                        addResult(marcRecordMap,  linePieces[0], linePieces[1]);
                    }
                }
                oclcReader.close();
            }
        }
    }

    public Map<String, Map<String, String>> processInputLine(String[] labels, String[] parts)
    {
        Map<String,  Map<String, String>> result = new LinkedHashMap<String,  Map<String, String>>();
        
        // ID column 0
        String oclcNum = parts[32];
        String copies = parts[41];
        String id = parts[33];
        boolean duplicate = false;
        if (!oclcNum.isEmpty())
        {
            if (oclcNumFound.contains(oclcNum))
            {
                // early return
                return(result);
            }
            oclcNumFound.add(oclcNum);
        }
        else
        {
            if (!copies.equals("1"))
            {
                String fauxNum = parts[2] + " " + parts[5] + " " + parts[6];
                if (oclcNumFound.contains(fauxNum))
                {
                    // early return
                    return(result);
                }
                oclcNumFound.add(fauxNum);
            }
        }
        if (id != null && !id.equals("Object#") && (id.length() > 0 || oclcNum.length() > 0))
        {
            if (id.contains(";")) 
                id = id.replaceFirst("([^;]*);.*", "$1");
            id = id.replaceAll("[.]",  "_");
            if (idFound.contains(id))
            {
                id = id + "_a";
                duplicate = true;
            }
            else 
            {
                idFound.add(id);
            }
            addResult(result, "id", id);
            if (duplicate)  addResult(result, "marc_error", "Record has duplicate unique identifier");
        }
        else  // early return
        {
            return(result);
        }
        Collection<String> doctypes = new LinkedHashSet<String>();
        doctypes.add("klugeruhe");
        
        addResult(result, "doc_type_f_stored", doctypes);
        addResult(result, "data_source_f_stored", doctypes);
        addResult(result, "availability_a", "This item is available for use only in the Kluge-Ruhe Study Center, housed within the Kluge-Ruhe Aboriginal Art Collection of the University of Virginia. For information on viewing this item, please visit the <a href=\"http://www.kluge-ruhe.org/publications/study-center\">Kluge-Ruhe Study Center</a> web page.");
        addResult(result, "library_f_stored", "Kluge-Ruhe Study Center");
        addResult(result, "source_f_stored", "Kluge-Ruhe Aboriginal Art Collection Study Center");
        addResult(result, "shadowed_location_f_stored", "VISIBLE");
        // location_facet
        if (!parts[35].isEmpty())
        {
            addResult(result, "location_f_stored", parts[35]);
        }
        else
        {
            addResult(result, "location_f_stored", "Study Center");
        }

//        addResult(result, "language_facet", "English");
        
        // year_multisort_i column 14
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

        // format_facet column 20
        boolean is_video = false;
        String pool = "catalog";
        Set<String> formatSet = new LinkedHashSet<String>();
        if (!parts[1].equals("")) 
        {
            String format = parts[1].trim();
            if (format.equals("DVD"))                  { is_video = true; formatSet.add("Video"); formatSet.add("DVD"); pool="video"; }
            else if (format.equals("VHS"))             { is_video = true; formatSet.add("Video"); formatSet.add("VHS"); pool="video"; }
            else if (format.equals("U-matic"))         { is_video = true; formatSet.add("Video"); formatSet.add("Video U-Matic"); pool="video"; }
            else if (format.equals("Book"))            { formatSet.add("Book"); }
            else if (format.equals("Booklet"))         { formatSet.add("Book"); }
            else if (format.equals("Annual Report"))   { formatSet.add("Book"); }
            else if (format.equals("Catalogue"))       { formatSet.add("Book"); }
            else if (format.equals("Exhibition Catalogue"))       { formatSet.add("Book"); }
            else if (format.equals("Reference Book"))  { formatSet.add("Book"); }
            else if (format.equals("Journal"))         { formatSet.add("Journal/Magazine");  pool="serials";}
            else if (format.equals("Newsletter"))      { formatSet.add("Journal/Magazine");  pool="serials"; }
            else if (format.equals("Newseltter"))      { formatSet.add("Journal/Magazine");  pool="serials"; }
            else if (format.equals("Periodical"))      { formatSet.add("Journal/Magazine");  formatSet.add("Periodical");  pool="serials";}
            else if (format.equals("Dissertation"))    { formatSet.add("Thesis/Dissertation"); pool="thesis"; }
            else if (format.equals("Master's Thesis")) { formatSet.add("Thesis/Dissertation"); pool="thesis"; }
            else if (format.equals("Notes")) 		   { formatSet.add("Thesis/Dissertation"); pool="thesis"; }
            else if (format.equals("Map"))             { formatSet.add("Map"); pool="maps"; }
            else if (format.equals("CD"))              { formatSet.add("Sound Recording"); formatSet.add("CD");  pool="music_recordings"; }
            else if (format.equals("CD-ROM"))          { formatSet.add("Computer Media"); pool="catalog"; }
            else if (format.equals("Audio Cassette"))  { formatSet.add("Sound Recording"); formatSet.add("Cassette");  pool="music_recordings"; }
            else if (format.equals("Vinyl LP"))        { formatSet.add("Sound Recording"); formatSet.add("LP");  pool="music_recordings"; }
            else if (format.equals("Vinyl EP"))        { formatSet.add("Sound Recording"); formatSet.add("LP");  pool="music_recordings"; }
            else if (format.equals("Flash Card"))        { formatSet.add("Visual Materials"); }

            else                                       { System.err.println("No Format Found: "+ id + " : " + parts[20]); showAllParts(parts); }
            addResult(result, "format_f_stored", formatSet);
            addResult(result, "pool_f_stored", pool);
        }
        //video genre
        Set<String> genreSet = new LinkedHashSet<String>();
        if (is_video)
        {
          String genre = parts[28];
          if (genre.equals("Adventure; Comedy"))              { genreSet.add("Action/Adventure"); genreSet.add("Comedy"); }
          else if (genre.equals("Animation"))                 { genreSet.add("Animation"); }
          else if (genre.equals("Biography"))                 { genreSet.add("Biography"); }
          else if (genre.equals("Comedy"))                    { genreSet.add("Comedy"); }
          else if (genre.equals("Dance Film"))                { genreSet.add("Music/Musical"); }
          else if (genre.equals("Documentary Film"))          { genreSet.add("Documentary"); }
          else if (genre.equals("Drama"))                     { genreSet.add("Drama"); }
          else if (genre.equals("Historical Drama"))          { genreSet.add("Historical"); }
          else if (genre.equals("Historical Re-Envisioning")) { genreSet.add("Historical"); }
          else if (genre.equals("History"))                   { genreSet.add("Historical"); }
          else if (genre.equals("Humor; Drama"))              { genreSet.add("Drama");  genreSet.add("Comedy"); }
          else if (genre.equals("Juvenile Film"))             { genreSet.add("Children/Family"); }
          else if (genre.equals("Legal drama"))               { genreSet.add("Drama"); }
          else if (genre.equals("Promotional Film"))          { genreSet.add("Documentary"); }
          else if (genre.equals("Thriller"))                  { genreSet.add("Crime/Mystery"); }
          else if (genre.equals("Western"))                   { genreSet.add("Western"); }
          addResult(result, "video_genre_tsearch_stored", genreSet);
          
          if (parts[27].length() > 0)
          {
              addResult(result, "video_run_time_tsearch_stored", parts[27]);
          }

        }

        
        // title_display title_text  title_sort_facet, subtitle_text subtitle_display column 1
        String titleSort = "";
        if (!parts[2].equals("")) 
        {
            String[] title = DataUtil.cleanData(parts[2]).split(":", 2);
            title[0] = DataUtil.cleanData(title[0]);
            addResult(result, "title_tsearch_stored", title[0]);
            if (title.length > 1 && title[1].length() > 0)
            {
                title[1] = DataUtil.cleanData(title[1]);
                addResult(result, "title_sub_tsearch_stored", title[1]);
            }
            titleSort = parts[2];
            titleSort = titleSort.toLowerCase();
            titleSort = titleSort.replaceAll("[^-a-z0-9: ]", " ");
            titleSort = titleSort.replaceAll("[ ]?:[ ]?", ":");
            titleSort = titleSort.replaceAll("[ ][ ]+", " ").trim();
            titleSort = titleSort.replaceAll("^(the |a |le )", "");
            addResult(result, "title_ssort_stored", titleSort);
        }

        if (!parts[3].equals("")) 
        {
            addResult(result, "title_alternate_tsearch_stored", parts[3].trim());
        }
        
        // Author Stuff 3,4  5,6  7,8  9,10  11
        Collection<String> authors = new LinkedHashSet<String>();
        StringBuilder author = new StringBuilder();
        StringBuilder responsibility = new StringBuilder();
        if (!parts[6].isEmpty())
        {
            author.append(parts[6]);
            if (!parts[7].isEmpty())
            {
                author.append(", ").append(parts[7]);
                responsibility.append(parts[7]).append(" ");
            }
            responsibility.append(parts[6]);
            if (!parts[8].isEmpty())
            {
                author.append(", ").append(parts[8]);
            }
            if (!parts[9].isEmpty())
            {
                responsibility.append(", ").append(parts[9]);
            }
            authors.add(author.toString()); 
            author.setLength(0);
        }

        if (!parts[10].isEmpty())
        {
            author.append(parts[10]);
            responsibility.append("; ");
            if (!parts[11].isEmpty())
            {
                author.append(", ").append(parts[11]);
                responsibility.append(parts[11]).append(" ");
            }
            responsibility.append(parts[10]);
            if (!parts[12].isEmpty())
            {
                author.append(", ").append(parts[12]);
            }
            if (!parts[13].isEmpty())
            {
                responsibility.append(", ").append(parts[13]);
            }
            authors.add(author.toString()); 
            author.setLength(0);
        }
        addResult(result, "author_tsearch_stored", authors);
        addResult(result, "author_facet_f_stored", authors);

        if (responsibility.length() > 0)
        {
            addResult(result, "responsibility_statement_tsearch_stored", responsibility.toString());
        }
        String firstAuthor = null;
        String sortableFirstAuthor = "";
        try {
            firstAuthor = authors.iterator().next();
            sortableFirstAuthor = firstAuthor.toLowerCase().replaceAll("[^-a-z0-9 ]", " ").replaceAll("[ ][ ]+", " ");
            addResult(result, "author_ssort_stored", sortableFirstAuthor.trim());
        }
        catch (NoSuchElementException nsee)
        {
            // eat it.
        }

        // published info
        if (!parts[22].isEmpty())
        {
            StringBuilder published = new StringBuilder();
            if (!parts[20].isEmpty())  { published.append(parts[20]).append(", "); }
            if (!parts[21].isEmpty())  { published.append(parts[21]).append(", "); }
            if (!parts[22].isEmpty())  { published.append(parts[22]); }
            if (!parts[19].isEmpty())  { published.append(" : ").append(parts[19]); }
            if (year != null)
            {
                if (year.matches("^[0-9][0-9][0-9][0-9]$"))
                {
                    published.append(", ").append(year);
                }
            }
            addResult(result, "published_tsearch_stored", published.toString());
        }

        // series_title
        if (!parts[23].isEmpty())
        {
            addResult(result, "title_series_tsearch_stored", parts[23]);
        }
        
        // contents  -> description_display
        if (!parts[42].isEmpty())
        {
            String[] contents = parts[42].split(";[ ]?");
            Collection<String> contentsList = Arrays.asList(contents);
            addResult(result, "title_notes_tsearch_stored", contentsList);
        }

        // vol, num -> part_text  part_display
        if (!parts[24].isEmpty() || !parts[25].isEmpty())
        {
           String part = String.format("%s%s%s%s%s", (!parts[24].isEmpty() ? "v." : ""), parts[24], 
                                                     (!parts[24].isEmpty() && !parts[25].isEmpty() ? ", " : ""),
                                                     (!parts[25].isEmpty() ? "no." : ""), parts[25]);
           addResult(result, "title_part_tsearch_stored", part);
        }
        
        // issn -> issn_text issn_display
        if (!parts[31].isEmpty())
        {
            addResult(result, "issn_e_stored", parts[31]);
        }

        // oclc -> oclc_text oclc_display
        if (!oclcNum.isEmpty())
        {
            String[] oclcNums = oclcNum.split(",[ ]?(n/a, )?");
            List<String> oclcNumList = Arrays.asList(oclcNums);
            addResult(result, "oclc_e_stored", oclcNumList);
            if (oclcNumList.size() == 1)
            {
                addFieldsFromMarcRecord(result, id, oclcNum);
            }
            else
            {
                for (String oclc : oclcNumList)
                {
                    addSelectedFieldsFromMarcRecord(result, id, oclc);
                }
            }
        }
        
        // LCSH1 -> subject_facet subject_text
        Collection<String> subjects = new LinkedHashSet<String>();

        if (!parts[36].isEmpty())
        {
            subjects.add(fixSubject(parts[36]));
        }

        if (!parts[37].isEmpty())
        {
            subjects.add(fixSubject(parts[37]));
        }

        if (!parts[38].isEmpty())
        {
            subjects.add(fixSubject(parts[38]));
        }

        // language1 -> language_facet
        Collection<String> languages = new LinkedHashSet<String>();
        if (!parts[39].isEmpty())
        {
            languages.add(parts[39]);
        }

        // language2 -> language_facet
        if (!parts[40].isEmpty())
        {
            languages.add(parts[40]);
        }
        addResult(result, "language_f_stored", languages);

        // edition -> edition_display
        if (!parts[4].isEmpty())
        {
            addResult(result, "edition_a", parts[4]);
        }
        
        // # keywords -> keyword_text
        Collection<String> keywords = new LinkedHashSet<String>();
        if (!parts[43].isEmpty())
        {
            addResult(result, "keywordathon_tsearch", parts[43]);
            String words[] = parts[43].split(";[ ]?");
            for (String word : words)
            {
                if (word.contains("--"))
                {
                    subjects.add(word);
                }
                else
                {
                    keywords.add(word);
                }
            }
        }
        if (keywords.size() > 0)
        {
            addResult(result, "subject_tsearchf_stored", keywords);
        }
        addResult(result, "subject_tsearchf_stored", subjects);
        
        // ISBN column 29 and/or 30
        String isbnStr[] = (parts[29] + ","+ parts[30]).split(",");
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

        if (isbnSet.size() > 0) 
        {
            addResult(result, "isbn_isbn_stored", isbnSet);
        }

        // LC call number
        if (!parts[0].isEmpty())
        {
            String lcnum = parts[0].trim();
            handleLCNum(result, lcnum, id, firstAuthor, year);
        }
        
        if (!result.containsKey("work_title2_key_ssort_stored"))
        {
        	String worktitlekey = titleSort + "/" + sortableFirstAuthor + "/" + formatSet.iterator().next();
        	addResult(result, "work_title2_key_ssort_stored", worktitlekey );
        }
        if (!result.containsKey("work_title3_key_ssort_stored") && is_video)
        {
        	String worktitlekey = titleSort + "/" + sortableFirstAuthor + "/" + formatSet.iterator().next();
        	addResult(result, "work_title3_key_ssort_stored", worktitlekey );
        }
        return(result);
    }
    
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

    private void addSelectedFieldsFromMarcRecord(Map<String, Map<String, String>> result, String id, String oclcNum)
    {
        Map<String, String> fieldsFromMarcRecord = this.marcRecordMap.get(oclcNum);
        if (fieldsFromMarcRecord != null && !fieldsFromMarcRecord.isEmpty())
        {
            boolean headerAdded = false;
            for (String field : fieldsFromMarcRecord.values())
            {
                String parts[] = field.split(" = ", 2);
                if (parts.length == 2 && parts[0].equals("full_title_tsearchf_stored"))
                {
                    if (!headerAdded)
                    {
                        super.addResult(result, "description_tsearch_stored", "Consists of Multiple Items:");
                        headerAdded = true;
                    }
                    super.addResult(result, "description_tsearch_stored", parts[1]);
                }
                if (parts.length == 2 && (parts[0].startsWith("author") || parts[0].startsWith("published")))
                {
                    if (!result.containsKey(parts[0])) 
                        super.addResult(result, parts[0], parts[1]);
                }
                else if (parts.length == 2 && (parts[0].startsWith("subject") || parts[0].startsWith("topic")))
                {
                    super.addResult(result, parts[0], parts[1]);
                }
            }
        }
    }

    private void addFieldsFromMarcRecord(Map<String, Map<String, String>> result, String id, String oclcNum)
    {
        Map<String, String> fieldsFromMarcRecord = this.marcRecordMap.get(oclcNum);
        if (fieldsFromMarcRecord != null && !fieldsFromMarcRecord.isEmpty())
        {
            for (String field : fieldsFromMarcRecord.values())
            {
                String parts[] = field.split(" = ", 2);
                if (parts.length == 2)
                {
                    String key = parts[0];
                    String value = parts[1];
                    if (key.equals("fullrecord"))
                    {
                        value = value.replaceFirst("tag=\"001\">[^<]*<", "tag=\"001\">"+id+"<").replaceFirst("tag=\"003\">[^<]*<", "tag=\"003\">KlugeID<");
                    }
                    if (key.equals("id") || ( (key.contains("sort") || key.contains("shelf") || key.contains("published_date"))&& result.containsKey(key)))
                    	continue;
                    super.addResult(result, key, value);
                }
            }
        }
        
    }

    
}
