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
package org.bridgedb.sql;

import org.apache.log4j.Logger;
import org.bridgedb.utils.BridgeDBException;
import org.bridgedb.utils.ConfigReader;

/**
 * Methods to generate the SQLAccess for MYSQl and Virtuso
 *
 * Set up in such a way that if the SQL connections fails the Tests will abort with a warning
 * WARNING: Due to the way JUnits work this will not generate a Fauilure, error or skipped count!
 *
 * @author Christian
 */
public abstract class TestSqlFactory {

    static final Logger logger = Logger.getLogger(TestSqlFactory.class);
    
    public static void checkSQLAccess() {
        ConfigReader.useTest();
        try {
            SQLAccess sqlAccess = SqlFactory.createTheSQLAccess();
            sqlAccess.getConnection();
        } catch (BridgeDBException ex) {
            logger.fatal("SKIPPPING tests due to Connection error.");
            System.err.println("**** SKIPPPING tests due to Connection error.");
            System.err.print("To run these test you must have ");
            if (SqlFactory.inSQLMode()){
                System.err.println(" A SQL server running ");
            } else {
                System.err.println(" A Virtuoso server running ");                
            }
            System.err.println("Configuration read from: " + ConfigReader.CONFIG_FILE_NAME + "  " + SqlFactory.configs());
            logger.fatal("Configuration read from: " + ConfigReader.CONFIG_FILE_NAME + "  " + SqlFactory.configs());
           org.junit.Assume.assumeTrue(false);        
         }
    } 

}
