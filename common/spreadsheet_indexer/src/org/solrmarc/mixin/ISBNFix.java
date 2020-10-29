package org.solrmarc.mixin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;

public class ISBNFix
{

    public static void main(String[] args)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        try
        {
            while ((line = reader.readLine()) != null)
            {
                Collection<String> isbns = ISBNNormalizer.filterISBN(Collections.singletonList(line), "both");
                boolean first = true;
                for (String isbn : isbns)
                {
                    if (!first) { System.out.print("\t"); first = false; }
                    System.out.print(isbn);
                }
                System.out.println("");
            }
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
