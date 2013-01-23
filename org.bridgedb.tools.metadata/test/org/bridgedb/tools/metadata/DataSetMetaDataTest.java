// BridgeDb,
// An abstraction layer for identifier mapping services, both local and online.
//
// Copyright 2006-2009  BridgeDb developers
// Copyright 2012-2013  Christian Y. A. Brenninkmeijer
// Copyright 2012-2013  OpenPhacts
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.bridgedb.tools.metadata;

import java.util.Set;
import javax.xml.datatype.DatatypeConfigurationException;
import org.bridgedb.rdf.constants.VoidConstants;
import org.bridgedb.tools.metadata.constants.DctermsConstants;
import org.bridgedb.utils.BridgeDBException;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;

/**
 *
 * @author Christian
 */
public class DataSetMetaDataTest extends MetaDataTestBase{
    
    public DataSetMetaDataTest() throws DatatypeConfigurationException, BridgeDBException{        
    }
    
    @Test
    @Ignore
    public void testShowAll() throws BridgeDBException{
        report("ShowAll");
        MetaDataCollection metaData = new MetaDataCollection("loadMayDataSet1()", loadMayDataSet1(), voidRegistry);
        String showAll = metaData.showAll();
        //ystem.out.println(showAll);
    } 
    
    @Test
    public void testHasRequiredValues() throws BridgeDBException{
        report("HasRequiredValues");
        MetaDataCollection metaData = new MetaDataCollection("loadDirectDataSet1()", loadDirectDataSet1(), voidRegistry);
        checkRequiredValues(metaData);
    } 

    @Test
    public void testMissingRequiredValue() throws BridgeDBException{
        report("HasMissingRequiredValues");
        d1LicenseStatement = null;
        MetaDataCollection metaData = new MetaDataCollection("loadDirectDataSet1()", loadDirectDataSet1(), voidRegistry);
        assertFalse(metaData.hasRequiredValues());
    } 

    @Test
    public void testAlternative1MissingRequiredValue() throws BridgeDBException{
        report("HasMissingRequiredValues");
        d1PublishedStatement = null;
        MetaDataCollection metaData = new MetaDataCollection("loadMayDataSet1()", loadMayDataSet1(), voidRegistry);
        checkRequiredValues(metaData);
    } 

    @Test
    public void testAlternativeAllMissingRequiredValue() throws BridgeDBException{
        report("HasMissingRequiredValues");
        d1PublishedStatement = null;
        d1RetreivedOn = null;
        MetaDataCollection metaData = new MetaDataCollection("loadMayDataSet1()", loadMayDataSet1(), voidRegistry);
        assertFalse(metaData.hasRequiredValues());
    } 

    @Test
    public void testTooManyValues() throws BridgeDBException{
        report("TooManyValues");
        Set<Statement> statements = loadDirectDataSet1();
        Statement extra = new StatementImpl(D1_ID, VoidConstants.URI_SPACE_URI, D2_NAME_SPACE_VALUE);
        statements.add(extra);
        MetaDataCollection metaData = new MetaDataCollection("testTooManyValues()", statements, voidRegistry);
        assertFalse(metaData.hasRequiredValues());
        String report = metaData.validityReport(NO_WARNINGS);
        assertThat(report, containsString("ERROR"));
    }
    
    @Test
    public void testHasCorrectTypes() throws BridgeDBException{
        report("HasCorrectTypes");
        MetaDataCollection metaData = new MetaDataCollection("loadMayDataSet1()", loadMayDataSet1(), voidRegistry);
        checkCorrectTypes(metaData);
    }
    
    @Test
    public void testHasCorrectTypesBadDate() throws BridgeDBException{
        report("isHasCorrectTypesBadDate");
        d1ModifiedStatement = new StatementImpl(D1_ID, DctermsConstants.MODIFIED, TITLE);  
        MetaDataCollection metaData = new MetaDataCollection("loadMayDataSet1()", loadMayDataSet1(), voidRegistry);
        assertFalse(metaData.hasCorrectTypes());
    }
 
    @Test
    public void testMustValidityReport() throws BridgeDBException{
        report("MustValidityReport");
        MetaDataCollection metaData = new MetaDataCollection("loadDirectDataSet1()", loadDirectDataSet1(), voidRegistry);
        String report = metaData.validityReport(NO_WARNINGS);
        assertThat(report, not(containsString("ERROR")));
        report = metaData.validityReport(INCLUDE_WARNINGS);
        assertThat(report, not(containsString("ERROR")));
    }

    @Test
    public void testMissingValidityReport() throws BridgeDBException{
        report("MissingValidityReport");
        d1TitleStatement = null;
        MetaDataCollection metaData = new MetaDataCollection("loadMayDataSet1()", loadMayDataSet1(), voidRegistry);
        assertFalse(metaData.hasRequiredValues());
        String report = metaData.validityReport(NO_WARNINGS);
        assertThat(report, containsString("ERROR"));
    }

    @Test
    public void testGroupValidityReport() throws BridgeDBException{
        report("MissingValidityReport");
        d1ModifiedStatement = null;
        MetaDataCollection metaData = new MetaDataCollection("loadMayDataSet1()", loadMayDataSet1(), voidRegistry);        
        String report = metaData.validityReport(NO_WARNINGS);
        assertThat(report, not(containsString("ERROR")));
        report = metaData.validityReport(INCLUDE_WARNINGS);
        assertThat(report, not(containsString("ERROR")));
        assertThat(report, containsString("WARNING"));
    }

    @Test
    public void testAlternativeValidityReport() throws BridgeDBException{
        report("MissingValidityReport");
        d1ModifiedStatement = null;
        d1RetreivedOn = null;
        MetaDataCollection metaData = new MetaDataCollection("loadMayDataSet1()", loadMayDataSet1(), voidRegistry);        
        String report = metaData.validityReport(NO_WARNINGS);
        assertThat(report, containsString("ERROR"));
    }
    
    @Test
    @Ignore
    public void testAllStatementsUsed() throws BridgeDBException{
        report("AllStatementsUsed");
        MetaDataCollection metaData = new MetaDataCollection("loadMayDataSet1()", loadMayDataSet1(), voidRegistry);
        checkAllStatementsUsed(metaData);
    }
    
    @Test
    public void testNotAllStatementsUsedDifferentPredicate() throws BridgeDBException{
        report("NotAllStatementsUsedDifferentPredicate");
        Set<Statement> data = loadMayDataSet1();
        Statement unusedStatement = 
                new StatementImpl(D1_ID, new URIImpl("http://www.example.org/NotARealURI"), NAME_SPACE_VALUE);
        data.add(unusedStatement);
        MetaDataCollection metaData = 
                new MetaDataCollection("testNotAllStatementsUsedDifferentPredicate()", data, voidRegistry);
        assertFalse(metaData.allStatementsUsed());
    }

    @Test
    public void testNotAllStatementsUsedDifferentResource() throws BridgeDBException{
        report("NotAllStatementsUsedDifferentResource");
        Set<Statement> data = loadMayDataSet1();
        Statement unusedStatement = new StatementImpl(
                new URIImpl("http://www.example.org/NotARealURI"), DctermsConstants.TITLE, NAME_SPACE_VALUE);
        data.add(unusedStatement);
        MetaDataCollection metaData = 
                new MetaDataCollection("testNotAllStatementsUsedDifferentResource()", data, voidRegistry);
        assertFalse(metaData.allStatementsUsed());
    }

    @Test
    public void testGetRDF() throws BridgeDBException{
        report("getRdf");
        Set<Statement> data = loadMayDataSet1();
        MetaDataCollection metaData = new MetaDataCollection("loadMayDataSet1()", data, voidRegistry);
        Set<Statement> rewriteData = metaData.getRDF();
        assertEquals(loadMayDataSet1(), rewriteData);
    }
    
    @Test
    public void testSummary() throws BridgeDBException{
        report("Dataset Summary");
        MetaDataCollection metaData = new MetaDataCollection("loadMayDataSet1()", loadMayDataSet1(), voidRegistry);
        String expected = "(Dataset) http://www.example.com/test/dataset1 OK!\n";
        String summary = metaData.summary();
        assertEquals(expected, summary);
    }

    @Test
    public void testSummaryExtra() throws BridgeDBException{
        report("Dataset Summary Extra");
        Set<Statement> data = loadMayDataSet1();
        data.add(personIdStatement);
        data.add(personNameStatement);
        MetaDataCollection metaData = new MetaDataCollection("testSummaryExtra()", data, voidRegistry);
        String expected = "http://www.example.com/person#Joe has an unspecified type of http://www.example.com/Person\n"
                + "(Dataset) http://www.example.com/test/dataset1 OK!\n";
        String summary = metaData.summary();
        assertEquals(expected, summary);
    }

}