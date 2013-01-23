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
package org.bridgedb.mysql;

import java.util.Date;
import org.bridgedb.IDMapperException;
import org.bridgedb.sql.SQLAccess;
import org.bridgedb.sql.SQLUrlMapper;
import org.bridgedb.sql.TestSqlFactory;
import org.bridgedb.utils.StoreType;
import org.junit.BeforeClass;

/**
 * Runs the URLMapper interface tests over SQLURLMapper class
 * 
 * Creates the mapper, loads in the test data and then runs the tests.
 *
 * @author Christian
 */
//@Ignore
public class URLMapperTest extends org.bridgedb.url.URLMapperTest {
    
    private static final String CREATOR1 = "testCreator";
    private static final String PREDICATE1 = "testMapping";
    private static final long CREATION1 = new Date().getTime();

    @BeforeClass
    public static void setupIDMapper() throws IDMapperException{

        connectionOk = false;
        TestSqlFactory.checkSQLAccess();
        connectionOk = true;
        listener = new SQLUrlMapper(true, StoreType.TEST);
        loadData();
        urlMapper =new SQLUrlMapper(false, StoreType.TEST);
    }
            
}