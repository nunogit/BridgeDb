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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.bridgedb.rdf.constants.RdfConstants;
import org.bridgedb.rdf.constants.VoidConstants;
import org.bridgedb.rdf.reader.StatementReader;
import org.bridgedb.utils.BridgeDBException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 *
 * @author Christian
 */
public class MetaDataCollection extends AppendBase implements MetaData {
    
    Map<Resource,ResourceMetaData> resourcesMap = new HashMap<Resource,ResourceMetaData>();
    Set<String> errors = new HashSet<String>();
    Set<Statement> unusedStatements = new HashSet<Statement>();
    MetaDataSpecification metaDataRegistry;
    private final String source;
    
    static final Logger logger = Logger.getLogger(MetaDataCollection.class);

    public MetaDataCollection(String source, Set<Statement> incomingStatements, MetaDataSpecification specification) 
            throws BridgeDBException {
        Set<Statement> statements = new HashSet(incomingStatements);
        this.metaDataRegistry = specification;
        Set<Resource> ids = findIds(statements);
//        Set<Statement> subsetStatements = extractStatementsByPredicate(VoidConstants.SUBSET, statements);
        loadLinkedResources(statements);
        for (Resource id:ids){
            if (!resourcesMap.containsKey(id)){
               ResourceMetaData resourceMetaData =  getResourceMetaData(id, statements);
               if (resourceMetaData == null){
                    logger.warn(id + " has no known rdf:type ");
                    errors.add(id + " has no known rdf:type ");                   
                    throw new IllegalStateException("No type for " + id);               
               }
            }
        }
//        addSubsets(subsetStatements);
        checkAllRemainingAreLinks(statements);
        this.source = source;
    }
    
    public MetaDataCollection (String dataFileName, MetaDataSpecification metaDataRegistry) throws BridgeDBException{
        this(dataFileName, new StatementReader(dataFileName).getVoidStatements(), metaDataRegistry);
    }
        
    private void loadLinkedResources(Set<Statement> statements) throws BridgeDBException{
        HashMap<Resource,Set<Resource>> hierarchy = new HashMap<Resource,Set<Resource>>();
        Set<URI> linkingPredicates = metaDataRegistry.getLinkingPredicates();
        for (Iterator<Statement> iterator = statements.iterator(); iterator.hasNext();) {
            Statement statement = iterator.next();
            if (statement.getPredicate().equals(VoidConstants.SUBSET)){
                Resource subject = statement.getSubject();
                Value objectValue = statement.getObject();
                if (subject.equals(objectValue)){
                    errors.add("Found a subset statement with subject == object " + statement);
                    iterator.remove();                    
                } else if (objectValue instanceof Resource){
                    Resource object = (Resource)objectValue;
                    Set<Resource> supers = hierarchy.get(object);
                    if (supers == null){
                        supers = new HashSet<Resource>();
                    }
                    supers.add(subject);
                    hierarchy.put(object, supers);
                } else {
                    errors.add("Found a subset statement with none Resource object " + statement);
                    iterator.remove();
                }                
            } else if (linkingPredicates.contains(statement.getPredicate())){
                Resource subject = statement.getSubject();
                Value objectValue = statement.getObject();
                if (subject.equals(objectValue)){
                    errors.add("Found a link statement with subject == object " + statement);
                    iterator.remove();                    
                } else if (objectValue instanceof Resource){
                    Resource object = (Resource)objectValue;
                    Set<Resource> supers = hierarchy.get(subject);
                    if (supers == null){
                        supers = new HashSet<Resource>();
                    }
                    supers.add(object);
                    hierarchy.put(subject, supers);
                } else {
                    errors.add("Found a linking statement with none Resource object " + statement);
                    iterator.remove();
                }
            }
        }
        loadLinkedResources (hierarchy, statements);
    }

    private void loadLinkedResources (HashMap<Resource,Set<Resource>> hierarchy, Set<Statement> statements) 
            throws BridgeDBException{
        for (Resource child:hierarchy.keySet()){
            loadLinkedResources(child, hierarchy, statements);
        }            
    }
    
    private void loadLinkedResources (Resource start, HashMap<Resource,Set<Resource>> hierarchy, Set<Statement> statements) 
            throws BridgeDBException{
        for (Resource parent:hierarchy.get(start)){
            loadLinkedResources(start, parent, hierarchy, statements);
        }            
    }
    
    private void loadLinkedResources (Resource start, Resource current, HashMap<Resource, Set<Resource>> hierarchy, 
            Set<Statement> statements) throws BridgeDBException{
        if (start.equals(current)){
            throw new BridgeDBException("Illegal circular subset reference including " + hierarchy);
        }
        if (hierarchy.get(current) == null){
           ResourceMetaData resourceMetaData = getResourceMetaData(current, statements);
        } else {
            for (Resource parent:hierarchy.get(current)){
                loadLinkedResources(start, parent, hierarchy, statements);
                ResourceMetaData resourceMetaData = getResourceMetaData(current, statements);
            }
        }
    }
    
    private ResourceMetaData getResourceMetaData (Resource id, Set<Statement> statements) throws BridgeDBException{
        if (resourcesMap.containsKey(id)){
            return resourcesMap.get(id);
        }
        Set<Value> types = findBySubjectPredicate(id, RdfConstants.TYPE_URI, statements);
        ResourceMetaData resourceMetaData = null;
        for (Value type:types){
            if (resourceMetaData == null){
                resourceMetaData = metaDataRegistry.getExistingResourceByType(type, id, this);
            } else if (metaDataRegistry.getExistingResourceByType(type, id, this) != null){
                errors.add(id + " has multiple types " + resourceMetaData.getType() + " and " + type);
                errors.add(id + " will has only been validated as " + resourceMetaData.getType());
            }
        }
        if (resourceMetaData == null){
            for (Value type:getSupersetTypes(id, statements)){
               if (resourceMetaData == null){
                    resourceMetaData = metaDataRegistry.getExistingResourceByType(type, id, this);
                } else if (metaDataRegistry.getExistingResourceByType(type, id, this) != null){
                    errors.add(id + " has multiple super types " + resourceMetaData.getType() + " and " + type);
                    errors.add(id + " will has only been validated as " + resourceMetaData.getType());
                }
            }
        }
        if (resourceMetaData == null){
            for (Value type:types){
                if (resourceMetaData == null){
                    resourceMetaData = metaDataRegistry.getResourceByType(type, id, this);
                } else {
                    errors.add(id + " has multiple unknown types " + resourceMetaData.getType() + " and " + type);
                }
            }
        }
        if (resourceMetaData == null){
            resourceMetaData = metaDataRegistry.getResourceByType(null, id, this);
        }
        resourceMetaData.loadValues(statements, errors);
        addSupersets(resourceMetaData, statements);
        resourcesMap.put(id, resourceMetaData);
        return resourceMetaData;
    }
    
    private Set<Resource> findIds(Set<Statement> statements){
        HashSet<Resource> results = new HashSet<Resource>();
        for (Statement statement: statements) {
            if (statement.getPredicate().equals(RdfConstants.TYPE_URI)){
                 results.add(statement.getSubject());
            }
//            if (statement.getPredicate().equals(VoidConstants.SUBSET)){
//                 results.add(statement.getSubject());
//                 Value object = statement.getObject();
//                 if (object instanceof Resource){
//                     results.add((Resource)object);
//                 }
//            }
        }  
        return results;
    }
    
    private Set<Statement> extractStatementsByPredicate(URI predicate, Set<Statement> statements){
        HashSet<Statement> results = new HashSet<Statement>();
        for (Iterator<Statement> iterator = statements.iterator(); iterator.hasNext();) {
            Statement statement = iterator.next();
            if (statement.getPredicate().equals(predicate)){
                iterator.remove();
                results.add(statement);
            }
        }  
        return results;
    }

    private Set<Value> findBySubjectPredicate(Resource subject, URI predicate, Set<Statement> statements){
        HashSet<Value> values = new HashSet<Value>();         
        for (Statement statement: statements){
            if (statement.getSubject().equals(subject)){
                if (statement.getPredicate().equals(predicate)){
                    values.add(statement.getObject());
                }
            }
        }  
        return values;
    }

    private Set<Resource> findByPredicateObject(URI predicate, Value object, Set<Statement> statements){
        HashSet<Resource> subjects = new HashSet<Resource>();         
        for (Statement statement: statements){
            if (statement.getPredicate().equals(predicate)){
               if (statement.getObject().equals(object)){
                    subjects.add(statement.getSubject());
                }
            }
        }  
        return subjects;
    }

    private void addSupersets(ResourceMetaData child, Set<Statement> statements){
        for (Iterator<Statement> iterator = statements.iterator(); iterator.hasNext();) {
            Statement statement = iterator.next();
            if (statement.getObject().equals(child.id) && statement.getPredicate().equals(VoidConstants.SUBSET)){
                iterator.remove();
                ResourceMetaData parent =  getResourceByID(statement.getSubject());
                if (parent != null){
                    child.addParent(parent, errors);
                } else {
                    logger.error("No parent found for " + statement);
                }
            }
        }                 
    }

    private void addSubsets(Set<Statement> subsetStatements) {
        for (Statement statement: subsetStatements){
            ResourceMetaData parent = resourcesMap.get(statement.getSubject());
            if (parent == null){
                errors.add("No resource found for " + statement.getSubject() + " unable to find parent");                
                throw new IllegalStateException("No resource found for " + statement.getSubject() + " unable to find parent");
            } 
            Value object = statement.getObject();
            if (object instanceof Resource){
                ResourceMetaData child = resourcesMap.get((Resource)object);
                if (child == null){
                    logger.warn("No resource found for " + object + " unable to find child");
                } else {
                    if (parent != null){
                         child.addParent(parent, errors);                           
                    }
                }
            } else {
                errors.add("Unexpected Object in subset statement " + object);                
            }
        }
    }

    private void checkAllRemainingAreLinks(Set<Statement> statements) {
        Set<Value> linkPredicates = getValuesByPredicate(VoidConstants.LINK_PREDICATE);
        for (Statement statement:statements){
            if (linkPredicates == null || !linkPredicates.contains(statement.getPredicate())){
                unusedStatements.add(statement);     
            }
        }
    }


    // ** Retreival Methods
    public Set<Resource> getIds(){
        return resourcesMap.keySet();
    }
    
    public Set<Resource> getIdsOfType(URI type){
        Set<Resource> result = new HashSet<Resource>();
        Set<Resource>  keys =  resourcesMap.keySet();
        for (Resource key: keys){
            ResourceMetaData resourceMetaData = resourcesMap.get(key);
            if (resourceMetaData.getType().equals(type)){
                result.add(key);
            }
        }
        return result;
    }

    public String summary() throws BridgeDBException {
        StringBuilder builder = new StringBuilder();
        for (ResourceMetaData resource:resourcesMap.values()){
            resource.appendSummary(builder, 0);
        }
        return builder.toString();
    }

    // ** MetaData Methods 
    @Override
    public boolean hasRequiredValues() {
        for (ResourceMetaData resouce:resourcesMap.values()){
            if (!resouce.hasRequiredValues()){
                return false;
            }
        }
        return true;
    }

    public boolean hasRequiredValuesOrIsSuperset() {
        for (ResourceMetaData resource:resourcesMap.values()){
            if (!resource.isSuperset()){
                if (!resource.hasRequiredValues()){
                      return false;
                } else {
                }
            }
        }
        return true;
    }

    @Override
    public boolean hasCorrectTypes() throws BridgeDBException {
        for (ResourceMetaData resouce:resourcesMap.values()){
            if (!resouce.hasCorrectTypes()){
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean allStatementsUsed() {
        if (!unusedStatements.isEmpty()) {
            return false;
        }
        for (ResourceMetaData resouce:resourcesMap.values()){
            if (!resouce.allStatementsUsed()){
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<Statement> getRDF(){
        HashSet results = new HashSet<Statement>();
        for (ResourceMetaData resouce:resourcesMap.values()){
            results.addAll(resouce.getRDF());
        }
        return results;
     }

   //** AppendBase Methods 
    
    @Override
    void appendShowAll(StringBuilder builder, int tabLevel) {
         Collection<ResourceMetaData> theResources = resourcesMap.values();
         for (ResourceMetaData resouce:theResources){
             resouce.appendShowAll(builder, 0);
         }
         //appendUnusedStatements(builder);
    }
    
    @Override
    void appendUnusedStatements(StringBuilder builder) {
         Collection<ResourceMetaData> theResources = resourcesMap.values();
         for (ResourceMetaData resouce:theResources){
             resouce.appendUnusedStatements(builder);
         }
         for (Statement statement:unusedStatements){
             builder.append(statement);
             newLine(builder);
         }
    }
   
    @Override
    void appendSchema(StringBuilder builder, int tabLevel) {
         Collection<ResourceMetaData> theResources = resourcesMap.values();
         for (ResourceMetaData resouce:theResources){
             resouce.appendSchema(builder, 0);
         }
    }

    @Override
    public String validityReport(boolean includeWarnings) throws BridgeDBException {
         StringBuilder builder = new StringBuilder();
         builder.append("Report for " + source);
         newLine(builder);
         appendValidityReport(builder, CHECK_ALL_PRESENT, includeWarnings, 0);
         return builder.toString();
    }
    
    @Override
    void appendValidityReport(StringBuilder builder, boolean checkAllpresent, boolean includeWarnings, int tabLevel) throws BridgeDBException {
         Collection<ResourceMetaData> theResources = resourcesMap.values();
         for (ResourceMetaData resource:theResources){
             if (!resource.isSuperset()){
                resource.appendValidityReport(builder, checkAllpresent, includeWarnings, 0);
             } else {
                resource.appendParentValidityReport(builder, checkAllpresent, includeWarnings, 0);
             }
         }
         for (String error:errors){
             tab(builder, tabLevel);
             builder.append(error);
             newLine(builder);
         }
    }

    @Override
    public Set<Value> getValuesByPredicate(URI predicate) {
        HashSet<Value> result = null;
        for (Resource id: resourcesMap.keySet()){
            ResourceMetaData rmd = resourcesMap.get(id);
            Set<Value> moreResults = rmd.getValuesByPredicate(predicate);
            if (moreResults != null){
                if (result == null){
                    result = new HashSet<Value>();
                }
                result.addAll(moreResults);
            }
        }
        return result;
    }

     
    ResourceMetaData getResourceByID(Resource id) {
        return resourcesMap.get(id);
    }

    public Set<ResourceMetaData> getResourceMetaDataByType(URI type) {
        HashSet<ResourceMetaData> results = new HashSet<ResourceMetaData>();
        for (ResourceMetaData resultMetaData:resourcesMap.values()){
            if(type.equals(resultMetaData.getType())){
                results.add(resultMetaData);
            }
        }
        return results;
    }

   @Override
    public Set<ResourceMetaData> getResoucresByPredicate(URI predicate) {
        HashSet<ResourceMetaData> result = null;
        for (Resource id: resourcesMap.keySet()){
            ResourceMetaData rmd = resourcesMap.get(id);
            Set<ResourceMetaData> moreResults = rmd.getResoucresByPredicate(predicate);
            if (moreResults != null){
                if (result == null){
                    result = new HashSet<ResourceMetaData>();
                }
                result.addAll(moreResults);
            }
        }
        return result;
    }

    private Set<Value> getSupersetTypes(Resource id, Set<Statement> statements) {
        Set<Value> types = new HashSet<Value>();
        Set<Resource> superResources = findByPredicateObject(VoidConstants.SUBSET, id, statements);
        for (Resource superResource:superResources){
            ResourceMetaData parent = resourcesMap.get(superResource);
            types.add(parent.getType());
        }
        return types;
    }

}
