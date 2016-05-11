package edu.hbs.qa.testrailrunner.googlesheetreader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author Ramandeep <ramandeepsingh@qainfotech.com>
 */
public class Worksheet {
    
    String spreadsheetId;
    String worksheetId;
    public String content;
    Map<String, String> testData;
    
    public Worksheet(String id, String gid) throws MalformedURLException, IOException {
        spreadsheetId = id;
        worksheetId = gid;
        content = worksheetContent(id, gid);
        parseTestData();
    }
    
    private String worksheetContent(String id, String gid) throws MalformedURLException, IOException {
        return new Scanner(new URL("https://docs.google.com/spreadsheets/d/"
                + id + "/export?format=csv&gid=" + gid).openStream()
                , "UTF-8").useDelimiter("\\A").next();
    }
    
    private void parseTestData(){
        String[] lines = content.split("\n");
        Boolean inTestCase = false;
        Boolean headerReset = false;
        String testCaseName = null;
        testData = new HashMap();
        String testCaseTable = "";
        for(int rowindex=0; rowindex<lines.length; rowindex++){
            String row = lines[rowindex];
            String[] cells = row.split(",");
            if(cells[0].equals("TestCase")){
                inTestCase = true;
                headerReset = true;
                testCaseName = cells[1];
                testCaseTable = "";
            } else if(inTestCase){
                if(headerReset){
                    headerReset = false;
                    testCaseTable = "      |" + row.trim().replaceAll(",", "|") + "|\n";
                } else if(cells[0].isEmpty()){
                    testData.put(testCaseName, testCaseTable);
                    inTestCase = false;
                    testCaseName = null;
                } else {
                    testCaseTable += "      |" + row.trim().replaceAll(",", "|") + "|\n";
                }
                if(rowindex==lines.length-1){
                    testData.put(testCaseName, testCaseTable);
                }
            }
        }
    }
}
