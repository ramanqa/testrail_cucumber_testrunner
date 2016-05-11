package edu.hbs.qa.testrailrunner.googlesheetreader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Ramandeep <ramandeepsingh@qainfotech.com>
 */
public class Registry extends Worksheet{
    
    Map<String,String> index;
    
    public Registry(String id) throws MalformedURLException, IOException {
        super(id, "0");
        parseRegistry();
    }
    
    private void parseRegistry(){
        index = new HashMap();
        String[] rows = content.split("\n");
        for(int rowindex = 1; rowindex<rows.length; rowindex++){
            String[] cells = rows[rowindex].split(",");
            if(!cells[1].isEmpty())
                index.put(cells[1], cells[2]);
        }
    }
    
    public String getGuidBySheetName(String sheetName){
        return index.get(sheetName);
    }
    
    public Set<String> testSuiteList(){
        return index.keySet();
    }
    
}
