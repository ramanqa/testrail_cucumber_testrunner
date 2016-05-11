package edu.hbs.qa.testrailrunner;

import java.util.Map;
import java.util.HashMap;


/**
 *
 * @author Ramandeep <ramandeepsingh@qainfotech.com>
 */
public class TestDataHandler {
    
    Map<String,String> configuration;
    
    public TestDataHandler(Map config){
        configuration = new HashMap();
        configuration.put("username", (String)config.get("username"));
        configuration.put("password", (String)config.get("password"));
    }
}