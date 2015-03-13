package org.pathvisio.plugins;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyShell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;

import org.codehaus.groovy.control.CompilationFailedException;
import org.pathvisio.Engine;
import org.pathvisio.model.ObjectType;
import org.pathvisio.model.Pathway;
import org.pathvisio.model.PathwayElement;

/**
 * This class is responsible for validating Pathways against Groovy rulesets. It loads(parses)
 *  the Groovy ruleset and then runs it against the Pathway Object. The whole result is passed
 *  to the sortGroovyResultsAndPrint, which is futher parsed and displayed on the panel.  
 */
public class GroovyValidator {

	private Engine eng;
	private JComboBox phaseBox;
	private List<String> graphIdsList;
	private ValidatorPlugin vPlugin;
	//private ValidatorPlugin.MyTableModel mytbm;
	//private JTable jtb;
	
public GroovyValidator(ValidatorPlugin vPlugin,Engine eng,JComboBox phaseBox,List<String> graphIdsList) {
	// TODO Auto-generated constructor stub
	this.eng=eng;
	this.phaseBox=phaseBox;
	this.graphIdsList=graphIdsList;
	this.vPlugin=vPlugin;
}
	
/**
 * separates the results coming in as mix of ArrayLists and String Arrays; and also filters the results based on 
 * ignored rules and ewbox (Errors and Warnings combo-box) selection 
 * @param tempList the result containing the validation messages after rungGroovy is called
 */
 void sortGroovyResultsAndPrint(List<Object> tempList){
		
		int counter=0;
		String tempSt,graphId,combinedStrings;
		String[] tempArray;
		int[] ijkew={0,0,0,0,0,0};
		
		//clear and reset 
		vPlugin.mytbm.setRowCount(0);
		vPlugin.jtb.setEnabled(true);
		graphIdsList.clear();
		
		eng.getActiveVPathway().resetHighlight();//unhighlight all nodes
	    
		for(Object tempObject:tempList){
	         counter++;
	         
	         if( tempObject instanceof ArrayList){
	         		
	         		System.out.println("Array list detected in the result");
	         		
	         		for(String[] sa: (ArrayList<String[]>)tempObject){
	         		
	         			if(sa[0]==null) sa[0]="Error"; // default role is null, if role is not set
	         			else 
	         				sa[0]=VPUtility.convertToTitleCase(sa[0]);
	         			
	         			graphId=sa[2];
	         			tempSt=sa[0]+" - "+sa[1];
	         			combinedStrings=graphId+"@@"+tempSt;
	         			if(vPlugin.ignoredErrorTypesList.contains(tempSt)||vPlugin.globallyIgnoredEWType.contains(tempSt)||
	         					vPlugin.ignoredElements.contains(graphId)|| vPlugin.ignoredSingleError.contains(combinedStrings)) 
	         				continue;
	         		
	         			printGroovy(tempSt,graphId,ijkew);
	         		}
	         		
	         	}
	         	
	         else {
	         		System.out.println("String Array detected "+counter);
	         		tempArray= (String[])tempObject;
	         		
	         		if(tempArray[0]==null) tempArray[0]="Error";
	         		else 
	         			tempArray[0]=VPUtility.convertToTitleCase(tempArray[0]);
	         		
	         		graphId=tempArray[2];
	         		tempSt=tempArray[0]+" - "+tempArray[1];
	         		combinedStrings=graphId+"@@"+tempSt;
	         		if(vPlugin.ignoredErrorTypesList.contains(tempSt)|| vPlugin.globallyIgnoredEWType.contains(tempSt)||
	         				vPlugin.ignoredElements.contains(graphId)|| vPlugin.ignoredSingleError.contains(combinedStrings) ) 
	         			continue;
	         		
	         		printGroovy(tempSt,graphId,ijkew);
	         }
	      
		 }
		
		if(ijkew[5]>0)
			ValidatorPlugin.highlightAllButton.setEnabled(true);
		else	
			ValidatorPlugin.highlightAllButton.setEnabled(false);
		
		vPlugin.eLabel.setText("Errors:"+ijkew[3]); vPlugin.wLabel.setText("Warnings:"+ijkew[4]);
		
		//vpwTemp.setPctZoom(vpwTemp.getPctZoom());
		eng.getActiveVPathway().redraw();
        
		if( (VPUtility.prevSelect==0 && ijkew[0]!=0) || (VPUtility.prevSelect==1 && ijkew[1]!=0) 
				|| (VPUtility.prevSelect==2 && ijkew[2]!=0) ){ 
			//jta.setText(sbf.toString());
			VPUtility.allIgnored=false;
		}
		else{ 
			switch(VPUtility.prevSelect){
			case 0:
				vPlugin.mytbm.addRow(new Object[]{"","No Errors and Warnings"});
				break;
			case 1:
				vPlugin.mytbm.addRow(new Object[]{VPUtility.eIcon,"No Errors"});
				break;
			case 2:
				vPlugin.mytbm.addRow(new Object[]{VPUtility.wIcon,"No Warnings"});
				break;
			}
			VPUtility.allIgnored=true;
			vPlugin.jtb.setEnabled(false);
		}
		ValidatorPlugin.ewBox.setEnabled(true);//ValidatorPlugin.highlightAllButton.setEnabled(true);
        System.out.println("-----------groovy part end-------------- ");
}
	
 	/**
 	 * 	This prints the mesages on the panel based on ewBOx selection and also highlights the nodes.
 	 * @param tempSt individual validation message without the graph-Id
 	 * @param graphId it is the graph-Id string which is used in highlighting nodes  
 	 * @param ijkew pass the array which is to contain the count for error and warning labels
 	 */
	private void printGroovy(String tempSt,String graphId,int[] ijkew){
		
		VPUtility.prevHighlight=true;
		ImageIcon EWIcon=VPUtility.eIcon;
		ValidatorPlugin.pth=eng.getActivePathway();
        
        if(tempSt.startsWith("Warning")){
        	EWIcon=VPUtility.wIcon; ijkew[4]++;
        }
        else {
        	EWIcon=VPUtility.eIcon; ijkew[3]++;
        }
		
		if(VPUtility.prevSelect==0){
			//System.out.println("prevsel 0");
			vPlugin.mytbm.addRow(new Object[]{EWIcon,++ijkew[0] +".) "+tempSt});
        }
		else if(VPUtility.prevSelect==1 && tempSt.startsWith("Error")){
			//System.out.println("prevsel 1");
			vPlugin.mytbm.addRow(new Object[]{EWIcon,++ijkew[1] +".) "+tempSt});
		}
		else if(VPUtility.prevSelect==2 && tempSt.startsWith("Warning")){
			//System.out.println("prevsel 2");
			vPlugin.mytbm.addRow(new Object[]{EWIcon,++ijkew[2] +".) "+tempSt}); 
		}
		else{
			//System.out.println("not passed"); 
			//make tempSt null , so that only the corresponding nodes are highlighted, when selecting the drop down (E / W / E&W)
			graphId=null;tempSt=null;
			
         }
        if(tempSt!=null){
        	graphIdsList.add(graphId+"");	
        }
		
        if(graphId!=null){
        	if(vPlugin.highlightNode(graphId, VPUtility.col2)!=null) 
        		++ijkew[5];
        }
      
	}
	
	/**
	 * Parser for the Groovy Ruleset, sets the Groups in phaseBox, and also ruleset Title
	 * @param schemaFile The file reference to Groovy ruleset 
	 * @return parsed Groovy Object out of the ruleset
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	 GroovyObject loadGroovy(File schemaFile) throws IOException,InstantiationException,IllegalAccessException{
		
		System.out.println("reached inside loadGroovy method");
		List<String[]> tempArray;//=new ArrayList<String[]>();
		
  	   	GroovyClassLoader loader =  new GroovyClassLoader(getClass().getClassLoader());
  	   	Class<GroovyObject> groovyClass=null;
  	   	GroovyObject groovyObject=null;
  	   
  	   	//try {
  		   groovyClass = loader.parseClass(schemaFile);
  		   VPUtility.schemaString=groovyClass.getSimpleName();
  		   VPUtility.cutSchemaTitleString(VPUtility.schemaString,ValidatorPlugin.schemaTitleTag);
  		   //ValidatorPlugin.schemaTitleTag.setCaretPosition(0);
 		   groovyObject = (GroovyObject) groovyClass.newInstance();
  	   	/*}
  	   	catch (Exception e1) {
  		   System.out.println("Exception @ groovy = "+e1.getMessage());
  		 JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"problem with the Groovy Ruleset","Validator Plugin",JOptionPane.ERROR_MESSAGE);
		 vPlugin.resetUI();  
  		 e1.printStackTrace();
  	   	}*/
  	   	
  	   	VPUtility.resetPhaseBox(phaseBox);
	   
	   	try{
	   		tempArray=(ArrayList<String[]>)(groovyObject.invokeMethod("phaseSupport", null));
	   	}
	   	catch(Exception e){
	   		System.out.println("phaseSupport method not present"); return groovyObject;
	   	}
	   	
	   	for(String[] phaseArr: tempArray){
	   		phaseBox.addItem(VPUtility.phaseLabelInCBox+phaseArr[0]);
	   		//System.out.println(tempIterator.next());
	   	}

	   	return groovyObject;
	}
	
	 /**
	  * 
	  * @param groovyObject object which contains the parsed rules from the Groovy ruleset (result of loadGroovy)
	  * @throws CompilationFailedException
	  */
	 void runGroovy(GroovyObject groovyObject) throws CompilationFailedException{
		
		System.out.println("--------------groovy---------------");
		List<Object> tempArray=new ArrayList<Object>();
		//phaseBoxSelection=2;
  	     	   
  	   	Pathway argPw= eng.getActivePathway();
  	   	
  	   	//checking every line element for graphId before sending the pathway for validation, generate graphId if graphId is not found
  	   	for(PathwayElement pwe: argPw.getDataObjects()){
  		
  	   		if( pwe.getObjectType()==ObjectType.LINE && ( pwe.getGraphId()=="" | pwe.getGraphId()==null) ){
  	   			pwe.setGeneratedGraphId();
  	   		}
  		}
  	   	
  	   	if(phaseBox.getSelectedIndex()==0){
  	   		//if(argPw!=null){
  	   		/*Object[] args = {argPw};
  	   		tempArray=(ArrayList<Object>)(groovyObject.invokeMethod("main", args));*/
  	   	
  	   		//code for running groovy script from java   
  	   		Binding binding = new Binding();
  	   		binding.setVariable("groovyObject", groovyObject);
  	   		binding.setVariable("tempArray", tempArray);
  	   		binding.setVariable("argPw",argPw );
  	   		GroovyShell shell = new GroovyShell(binding);
  
  	   		//try {
  	   			//running groovy script from a file named GroovyScriptKC.kc
  	   			shell.evaluate(getClass().getResourceAsStream("/GroovyScriptKC.kc"));
  	   		/*} catch (CompilationFailedException e) {
  	   			System.out.println("CompilationFailedException in the groovyshell code");
  	   			JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"Validation Exception in Groovy","Validator Plugin",JOptionPane.ERROR_MESSAGE);
  	   			vPlugin.resetUI();
  	   			e.printStackTrace();
  	   		}*/ 
  	   	}
  	   	
  	   	else { // this code runs only when there are phases present in the groovy rule 
  	   		
  	   		List<String[]> phaseTotal;//=new ArrayList<Object>();
  	   		phaseTotal=(ArrayList<String[]>)(groovyObject.invokeMethod("phaseSupport", null));
  	   		String methodNamesWithCommas=  ((String[])phaseTotal.get(phaseBox.getSelectedIndex()-1))[1];
  	   		String[] methodNamesArray= methodNamesWithCommas.split( ",\\s*" );
  	   		for(String methodName :methodNamesArray)
  	   			tempArray.add(groovyObject.invokeMethod(methodName.trim(), argPw));
    	}
  	   	
  	   	//remove null results from the overall result from the ruleset
  	   	while(tempArray.contains(null)){
  	   		tempArray.remove(null);
  	   	}
  	   	
  	   	ValidatorPlugin.globGroovyResult=tempArray; 	   	
  	   	sortGroovyResultsAndPrint(ValidatorPlugin.globGroovyResult);
 	}

}
