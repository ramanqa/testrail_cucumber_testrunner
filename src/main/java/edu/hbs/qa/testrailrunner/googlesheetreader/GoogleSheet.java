package edu.hbs.qa.testrailrunner.googlesheetreader;

import java.util.Scanner;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ramandeep <ramandeepsingh@qainfotech.com>
 */
public class GoogleSheet {
    
    String id;
    Registry registry;
    Map<String, Worksheet> worksheets;
    
    public GoogleSheet(String sheetId) throws Exception{
        try{
            id = sheetId;
            registry = new Registry(id);
            initWorksheet();
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }
    
    private Worksheet worksheet(String name) throws Exception{
        try{
            String guid = registry.getGuidBySheetName(name);
            return new Worksheet(id, guid);
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }
    
    private void initWorksheet() throws Exception{
        worksheets = new HashMap();
        for(String testSuiteName:registry.testSuiteList()){
            worksheets.put(testSuiteName, worksheet(testSuiteName));
        }
    }
    
    public String exampleTable(String testSuiteName, String testCaseName){
        return (String)((Map)worksheets.get(testSuiteName).testData)
                .get(testCaseName);
    }
    
    
}
