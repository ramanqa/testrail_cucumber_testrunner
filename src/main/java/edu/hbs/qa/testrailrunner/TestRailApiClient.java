package edu.hbs.qa.testrailrunner;

import edu.hbs.qa.testrailrunner.googlesheetreader.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Call test rail api with proper credentials and params
 * 
 * @author Ramandeep <ramandeepsingh@qainfotch.com>
 */
public class TestRailApiClient {
    Map<String, String> configuration;
    Integer runId;
    Integer projectId;
    
    /**
     * 
     * @param config 
     */
    public TestRailApiClient(Map config){
        configuration = new HashMap();
        configuration.put("username", (String)config.get("username"));
        configuration.put("password", (String)config.get("password"));
        configuration.put("testrail_url", (String)config.get("testrail_url"));
    }
    
    /**
     * 
     * @throws IOException 
     */
    public TestRailApiClient() throws IOException{
        configuration = new HashMap();
        Properties prop = new Properties();
        InputStream in = TestRailApiClient.class.getResourceAsStream("/testrail_credentials.properties");
        prop.load(in);
        in.close();
        configuration.put("username", (String)prop.get("username"));
        configuration.put("password", (String)prop.get("password"));
        configuration.put("testrail_url", (String)prop.get("url"));
        configuration.put("jenkins_job_url", (String)prop.get("jenkins_job_url"));
        
        /** read runId and projectId **/
        Properties testRun = new Properties();
        in = TestRailApiClient.class.getResourceAsStream("/testRun_details.properties");
        testRun.load(in);
        in.close();
        projectId = new Integer(testRun.getProperty("projectId"));
        runId = new Integer(testRun.getProperty("runId"));
        if(System.getProperty("testRunId")!=null){
            runId = new Integer(System.getProperty("testRunId"));
        }
    }
    
    private HttpResponse<JsonNode> get(String url) throws UnirestException, TestRailApiClientException, IOException{
        HttpResponse<JsonNode> response = Unirest.get(configuration.get("testrail_url") + url)
                .header("Content-Type", "application/json")
                .basicAuth(configuration.get("username"), configuration.get("password"))
                .asJson();
        if(response.getStatus() != 200){
            throw new TestRailApiClientException("Incorrect response code: " 
                    + Integer.toString(response.getStatus()) + ", message: " 
                    + response.getStatusText() + ", body: " + isToString(response.getRawBody()));
        }
        return response;
    }
    
    private HttpResponse<JsonNode> post(String url, Map postData) throws UnirestException, TestRailApiClientException, IOException{
        HttpResponse<JsonNode> response = Unirest.post(configuration.get("testrail_url") + url)
                .header("Content-Type", "application/json")
                .basicAuth(configuration.get("username"), configuration.get("password"))
                .body(new JSONObject(postData))                
                .asJson();
        if(response.getStatus() != 200){
            throw new TestRailApiClientException("Incorrect response code: " 
                    + Integer.toString(response.getStatus()) + ", message: " 
                    + response.getStatusText() + ", body: " + isToString(response.getRawBody()));
        }
        return response;
    }
    
    private String isToString(InputStream is) throws IOException{
        int ch;
        StringBuilder sb = new StringBuilder();
        while((ch = is.read()) != -1)
            sb.append((char)ch);
        return sb.toString();
    }
    
    
    /** Test Client API **/
    /**
     * 
     * @param projectId
     * @param runId
     * @return
     * @throws TestRailApiClientException
     * @throws UnirestException
     * @throws IOException 
     */
    private Map getTests(Integer projectId, Integer runId) throws TestRailApiClientException, UnirestException, IOException{
        Map features = new HashMap();
        features.put("projectId", projectId);
        HttpResponse<JsonNode> testRun = get("/index.php?/api/v2/get_run/"+Integer.toString(runId));
        Integer suiteId = testRun.getBody().getObject().getInt("suite_id");
        features.put("suiteId", suiteId);
        
        //** get run configurations **/
        String description = testRun.getBody().getObject().getString("description");
        String[] configRows = description.split("\n");
        Map<String,String> runConfig = new HashMap();
        for(String configRow:configRows){
            String key=configRow.split("=")[0].replaceFirst("!", "");
            String value=configRow.split("=")[1];
            runConfig.put(key, value);
        }
        features.put("runConfig", runConfig);
        
        HttpResponse<JsonNode> testSuite = get("/index.php?/api/v2/get_suite/"+Integer.toString(suiteId));
        String suiteName = testSuite.getBody().getObject().getString("name");
        features.put("suiteName", suiteName);
        
        List featureList = new ArrayList();
        /** get sections **/
        HttpResponse<JsonNode> sections = get("/index.php?/api/v2/get_sections/"
                + projectId + "&suite_id=" + suiteId);        
        /** for each section get test cases in sections **/
        for(int sectionindex=0; sectionindex<sections.getBody().getArray().length(); sectionindex++){
            Map feature = new HashMap();
            JSONObject section = sections.getBody().getArray().getJSONObject(sectionindex);
            feature.put("sectionId", section.getInt("id"));
            feature.put("sectionName", section.getString("name"));
            
            List scenarios = new ArrayList();
            HttpResponse<JsonNode> testCases = get("/index.php?/api/v2/get_cases/"
                    + projectId + "&suite_id=" 
                    + suiteId + "&section_id=" + Integer.toString(section.getInt("id")));
            /** for each test case get scenario**/
            for(int testcaseindex=0; testcaseindex<testCases.getBody().getArray().length(); testcaseindex++){
                Map scenario = new HashMap();
                JSONObject testCase = testCases.getBody().getArray().getJSONObject(testcaseindex);
                scenario.put("testCaseId", testCase.getInt("id"));
                scenario.put("title", testCase.getString("title"));
                String type = "Scenario";
                try{
                    if(testCase.getString("custom_preconds").contains("!type")){
                        if(testCase.getString("custom_preconds").contains("!type=outline")){
                            type = "Scenario Outline";
                        }
                    }
                } catch(Exception e){
                }
                scenario.put("type", type);
                scenario.put("steps", testCase.getString("custom_steps"));
                
                scenarios.add(scenario);
            }
            
            feature.put("scenarios", scenarios);
            featureList.add(feature);
        }
        
        features.put("sections", featureList);
        /** get test case scenario **/
        return features;
    }
    
    /**
     * Pull tests from TestRail runId in JIRA and store them as executeable 
     *  feature files. This method also reads test data from google docs and
     *  updates features with test parameterization
     * 
     * @param projectId
     * @param runId
     * @param location
     * @throws TestRailApiClientException
     * @throws UnirestException
     * @throws IOException 
     */
    public void createFeatureFiles(Integer projectId, Integer runId, String location)
            throws TestRailApiClientException, UnirestException, IOException {
        
        Map tests = getTests(projectId, runId);
        String suiteName = (String)tests.get("suiteName");
        
        /** empty features folder **/
        FileUtils.cleanDirectory(new File(location));
        
        /** save each section to a separate feature file **/
        List sections = (ArrayList)tests.get("sections");
        for(Object sectionObject : sections){
            Map section = (HashMap)sectionObject;
            File featureFile = new File(location
                    +"/"+Integer.toString(projectId)
                    +"_"+tests.get("suiteId")
                    +"_"+Integer.toString(runId)
                    +"_"+section.get("sectionId")
                    +".feature");
            featureFile.createNewFile();
            FileWriter writer = new FileWriter(featureFile);
            writer.write("Feature: " + section.get("sectionName"));
            List scenarios = (ArrayList)section.get("scenarios");
            for(Object scenarioObject : scenarios){
                Map scenario = (HashMap)scenarioObject;
                writer.write("\n\n");
                writer.write("  # @testCaseId:" + scenario.get("testCaseId") + "\n");
                writer.write("  " + scenario.get("type") 
                        + ": " + scenario.get("title") 
                        + " - @testCaseId:"+scenario.get("testCaseId")+"\n");
                List<String> gwtSteps = new ArrayList(Arrays.asList(
                        ((String)scenario.get("steps")).split("\n"))
                );
                for(String step:gwtSteps){
                    writer.write("    "+step+"\n");
                }
                
                /** insert test data if its a scenario outline **/
                try{
                    if(((String)scenario.get("type")).equals("Scenario Outline")){
                        String dataFileId = ((String)((Map)tests.get("runConfig"))
                                .get("testDataFile")).trim();
                        GoogleSheet sheet = new GoogleSheet(dataFileId);
                        String testData = sheet.exampleTable(
                                (String)tests.get("suiteName")
                                , (String)scenario.get("title"));
                        writer.write("\n    Examples:\n");
                        writer.write(testData);
                        writer.write("\n");
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            writer.flush();
            writer.close();
        }
    }
    
    /**
     * 
     * @throws TestRailApiClientException
     * @throws UnirestException
     * @throws IOException 
     */
    public void createFeatureFiles()
            throws TestRailApiClientException, UnirestException, IOException {
        createFeatureFiles(projectId, runId, "features/");
    }
    
    
    /**
     * 
     * @throws TestRailApiClientException
     * @throws UnirestException
     * @throws IOException 
     */
    public void pushResult()
        throws TestRailApiClientException, UnirestException, IOException{
        pushResult(projectId, runId, "target/cucumber-report.json");
    }
    
    /**
     * 
     * @param projectId
     * @param runId
     * @param resultFilePath
     * @throws TestRailApiClientException
     * @throws UnirestException
     * @throws IOException 
     */
    public void pushResult(Integer projectId, Integer runId, String resultFilePath)
            throws TestRailApiClientException, UnirestException, IOException{
        String resultsContent = new String(Files.readAllBytes(Paths.get(resultFilePath)));
        JSONArray resultsJson = new JSONArray(resultsContent);
        
        /** deserialize result json file and interprete test outcome **/
        for(int index=0; index<resultsJson.length(); index++){
            JSONObject feature = resultsJson.getJSONObject(index);
            
            Map<Integer, Map> results = new HashMap();

            for(int scenarioIndex=0; scenarioIndex<feature.getJSONArray("elements").length(); scenarioIndex++){
                
                Map result = new HashMap();
                
                JSONObject scenario = feature.getJSONArray("elements").getJSONObject(scenarioIndex);
                Integer testId = 0;
                String gwt = "";                
                List<Integer> stepResults = new ArrayList();
                
                /** extract testId from results (Scenario title)**/                
                String scenarioName = scenario.getString("name");
                testId = new Integer(scenarioName.split("@testCaseId:")[1]);
                
                JSONArray steps = scenario.getJSONArray("steps");
                Integer statusId = 1;

                gwt += "# " + scenario.getString("keyword") + ": " + scenario.getString("name") + " \n";
                
                for(int stepsIndex=0; stepsIndex<steps.length(); stepsIndex++){
                    gwt += "> **" + steps.getJSONObject(stepsIndex).getString("keyword").trim() + "** " + steps.getJSONObject(stepsIndex).getString("name");
                    String stepResult = steps.getJSONObject(stepsIndex).getJSONObject("result").getString("status");
                    gwt += " - **" + stepResult + "** \n";
                    if(stepResult.equals("undefined")){
                        stepResults.add(5);
                        statusId = 5;
                    }else if(stepResult.equals("passed")){
                        stepResults.add(1);
                    }else if(stepResult.equals("failed")){
                        stepResults.add(5);
                        statusId = 5;
                    }
                }
                
                String fileName = feature.getString("uri").replace(".", "-");
                gwt += configuration.get("jenkins_job_url") + "/" 
                        + System.getProperty("buildNumber") 
                        + "/cucumber-html-reports/" 
                        + fileName + ".html\n\n";
                
                if(results.containsKey(testId)){
                    String oldGwt = ((Map)results.get(testId)).get("comments").toString();
                    result.put("comments", oldGwt + "\n" + gwt);
                    Integer oldStatusId = (Integer)((Map)results.get(testId)).get("status_id");
                    result.put("status_id", Math.max(statusId, oldStatusId));
                }else{
                    result.put("comments", gwt);
//                    result.put("step_results", stepResults);
                    result.put("status_id", statusId);
                }
                results.put(testId, result);
            }
            postResultsToTestRail(runId, results);
        }        
    }
    
    private void postResultsToTestRail(Integer runId, Map results) 
            throws UnirestException, TestRailApiClientException, IOException{
        for(Object testId:results.keySet()){
            Integer test_id = (Integer)testId;
            Map<String, String> postFields = new HashMap();
            postFields.put("comment", (String)((Map)results.get(test_id)).get("comments"));
            postFields.put("status_id", ((Map)results.get(test_id)).get("status_id").toString());
            post("/index.php?/api/v2/add_result_for_case/"
                    + runId
                    +"/" + test_id
                    , postFields 
            );
        }
    }
    
}
