package edu.hbs.qa.testrailrunner;

/**
 *
 * @author Ramandeep <ramandeepsingh@qainfotech.com>
 */
public class BuildFeature {
    
    public static void main(String args[]) throws Exception{
        TestRailApiClient tr = new TestRailApiClient();
        tr.createFeatureFiles();
    }
    
}
