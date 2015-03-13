import java.util.ArrayList;
import org.pathvisio.model.ObjectType;
import org.pathvisio.model.Pathway;
import org.pathvisio.model.PathwayElement;

ArrayList<String[]> phaseSupport(){  // this method must be present with the same signature to allow for phase-specific validation
    	String[] phase1=["demoPhase1","ruleTitle , ruleOrganism, ruleAuthor"]; // Each individual phase is defined as a String Array, where the first  String element is the Phase's name , second is the comma (,) separated list of rules under this Phase
    	String[] phase2=["demoPhase2","ruleDataBaseAnnotation , ruleReferences,ruleTextLabel ,ruleUnattachedLines"];
    	ArrayList<String[]> result=new ArrayList<String[]>();
    	result.add(phase1); // If a phase is to be used by the Validator , then it has to be added in the "result" ArrayList
    	result.add(phase2);
    	return result;
    }

String[] ruleTitle(Pathway pw) { //checks for the "Title" attribute in the "Pathway" tag
    
    	String[] result=null;
    	
		if(pw.getMappInfo().getMapInfoName()==null){
			result= new String[3];						// String Array created to hold the result from rule (may contain 0.)role  1.)Diagnostic message   2.)GraphId  )
			result[0]="error"; 						// zeroth element in the string array must always contain the role (error/warning)
			result[1]="Diagrams should have a title.";  // first element must always contain the Diagnostic message
			//result[2]=pw.getMappInfo().getGraphId();  // the second element must always the contain the graphId
		}
		else System.out.println("title found = "+pw.getMappInfo().getMapInfoName());
    		
		return result; //return the result as null if it passes the rule (i.e nor error/warning)
    }
    
String[] ruleOrganism(Pathway pw) { // checks for the "Organism" attribute in the "Pathway" tag
        
    	String[] result=null;
	
    	if(pw.getMappInfo().getOrganism()==null){
    		result= new String[3];
    		//result[0]="error"; // if the result[0] is not set (left as null), then the default value for role is taken as an "error"
    		result[1]="Diagrams should have an organism.";
		}
    	else System.out.println("organism found = "+pw.getMappInfo().getOrganism());
    		
		return result;
    }
    
String[] ruleAuthor(Pathway pw) { //checks for the "Author" attribute in the "Pathway" tag
        
    	String[] result=null;
	
    	if(pw.getMappInfo().getAuthor()==null){
    		result= ["error","Diagrams should have an author.",null];// the groovy way
    		//result=new String[3];
			//result[0]="warning";
    		//result[1]="Diagrams should have an author.";
		}
    	else System.out.println("author found = "+pw.getMappInfo().getAuthor());
    		
		return result;
    }
    
String[] ruleReferences(Pathway pw) { // this rule checks if a "biopax" tag is present under the "Pathway" tag 
        
    	String[] result=null;
	
    	if(pw.getBiopax()==null ){
			result= new String[3];
			result[0]="error";
			result[1]="Diagrams should have references.";
		}
    	else System.out.println("passed the Biopax rule i.e references rule");
    		
		return result;
    }
    
    
ArrayList<String[]> ruleDataBaseAnnotation(Pathway pw) { //checks every "Xref" tag (under the "DataNode" tag) for non-empty "Database" and "Id" attributes 
        
    	ArrayList<String[]> totalResultForThisRule=null;
    	
    	for(PathwayElement pwe: pw.getDataObjects()){
    		
    		if(pwe.getObjectType()==ObjectType.DATANODE && 
    				( pwe.getXref().getDataSource()== null | pwe.getXref().getDataSource().equals("") |
    						pwe.getXref().getId()== null) | pwe.getXref().getId().equals("")){
    			
    			if(totalResultForThisRule==null){
    				totalResultForThisRule=new ArrayList<String[]>();
    			}
    			
    			//String[] result= new String[3];
				//result[0]= "error";
				//result[1]= "Datanodes should include database annotations.";
				//result[2]= pwe.getGraphId();
				String[] result = ["error","Datanodes should include database annotations.",pwe.getGraphId()];//the groovy way

				totalResultForThisRule.add(result);
    		}
    		
    	}
    	
    	if(totalResultForThisRule==null) System.out.println("All the datanodes include database annotations");
    
    	return totalResultForThisRule;
    }
    
ArrayList<String[]> ruleTextLabel(Pathway pw) { //checks every "DataNode" tag for a "TextLabel" attribute  
        
    	ArrayList<String[]> totalResultForThisRule=null;
    	
    	for(PathwayElement pwe: pw.getDataObjects()){
    		
    		if(pwe.getObjectType()==ObjectType.DATANODE && 
    				( pwe.getTextLabel()==null | pwe.getTextLabel().equals("") ) ){
    			
    			if(totalResultForThisRule==null){
    				totalResultForThisRule=new ArrayList<String[]>();
    			}
    			
    			String[] result= new String[3];
				result[0]= "error";
				result[1]= "DataNodes should have a text label.";
				result[2]= pwe.getGraphId();
				
				totalResultForThisRule.add(result);
    		}
    		
    	}
    	
    	if(totalResultForThisRule==null) System.out.println("All the datanodes have textlabels");
    
    	return totalResultForThisRule;
    }

    
ArrayList<String[]> ruleUnattachedLines(Pathway pw) { //checks every "Line" tag for its first and last "GraphRef" attributes under the "Point" tag (which is under "Graphics" tag)  
        
    	ArrayList<String[]> totalResultForThisRule=null;
    	
    	for(PathwayElement pwe: pw.getDataObjects()){
    		
    		if(pwe.getObjectType()==ObjectType.LINE && 
    				( pwe.getStartGraphRef()==null | pwe.getEndGraphRef()==null 
    						| pwe.getEndGraphRef().equals("") | pwe.getStartGraphRef().equals("") ) ){
    			
    			if(totalResultForThisRule==null){
    				totalResultForThisRule=new ArrayList<String[]>();
    			}
    			
    			String[] result= new String[3];
				result[0]= "error";
				result[1]= "Lines should be attached at both ends.";
				result[2]= pwe.getGraphId();
				
				totalResultForThisRule.add(result);
    		}
    		
    	}
    	
    	if(totalResultForThisRule==null) System.out.println("All the lines are attached");
    
    	return totalResultForThisRule;
    }    
