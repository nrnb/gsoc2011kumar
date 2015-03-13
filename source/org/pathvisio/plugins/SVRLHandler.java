/*


Open Source Initiative OSI - The MIT License:Licensing
[OSI Approved License]

The MIT License

Copyright (c) 2008 Rick Jelliffe, Topologi Pty. Ltd, Allette Systems 

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

 */

package org.pathvisio.plugins;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
/**
 * SAX handler for svrl xml file
 * @author Christophe lauret
 * @author Willy Ekasalim
 * @version 9 February 2007
 */
import java.util.List;

/**
 * @author Xin Chen
 * @version 20 July 2007
 * SAX Parser class which parses the exported Pathway file to extract the failed 
 * validation messages. Actual class modified to be used in the Validator plugin, 
 */

public final class SVRLHandler extends DefaultHandler {

	// constants --------------------------------------------------------------------------------------

	//chandan 

	/** 
	 * used as a flag to put only the first <svrl:diagnostic-reference> element's value in the output if there are more than one present in the svrl:failed-assert element
	 */
	private int svrlDiagRefCounter=0;
	private String roleAttribute;
	private boolean removePrev;
	//chandan

	/**
	 * Static name for failed assertions.
	 */ 
	private static final String FAILED_ASSERT_ELT = "svrl:failed-assert";

	/**
	 * Static name for simple text
	 */
	private static final String TEXT_ELT = "svrl:text";

	/**
	 * Static name for successful report.
	 */
	private static final String SUCCESSFUL_REPORT_ELT = "svrl:successful-report";

	//chandan
	/**
	 * Static name for diagnostic-reference.
	 */
	private static final String DIAGNOSTIC_REFERENCE_ELT = "svrl:diagnostic-reference";
	//chandan

	/**
	 * Static name for test attribute.
	 * Currently not used because test condition are not showed in the console output
	 */
	private static final String TEST_ATT = "test";

	//chandan
	/**
	 * Static name for diagnostic attribute.
	 */
	//private static final String DIAGNOSTIC_ATT = "diagnostic";
	//chandan

	/**
	 * Static name for location attribute.
	 */
	private static final String LOCATION_ATT = "location";

	// class attributes -------------------------------------------------------------------------------

	/**
	 * StringBuffer to collect text/character data received from characters() callback
	 */
	private StringBuffer chars = new StringBuffer();

	/**
	 * StringBuffer for constructing failed assertion/succesfull message
	 */
	private StringBuffer message = new StringBuffer();

	/**
	 * StringBuffer for storing the diagnostic message
	 */
	private StringBuffer diag_attr=new StringBuffer();
	/**
	 * String to store the element name that are currently being produced
	 */
	private String lastElement;

	/**
	 * An ArrayList to store (String) message of failed assertion found.
	 */
	private final List<String> failedAssertions= new ArrayList<String>();

	/**
	 * An ArrayList to store (String) message of diagnostic-reference found.
	 */
	private final List<String> diagnosticReference=new ArrayList<String>();

	/**
	 * An ArrayList to store (String) message of successful reports found.
	 */
	private final List<String> successfulReports=new ArrayList<String>();

	/***
	 * indicate that the current parsed element is either FAILED_ASSERT_ELT or SUCCESSFUL_REPORT_ELT.
	 */
	private boolean underAssertorReport = false;

	// contructor -------------------------------------------------------------------------------------

	/**
	 * Constructor for SVRLHandler.
	 */
	public SVRLHandler() {
	}

	public List<String> getFailedAssertions() {
		return failedAssertions;
	}

	public List<String> getDiagnosticReference() {
		return diagnosticReference;
	}

	public List<String> getSuccessfulReports() {
		return successfulReports;
	}
	
	// Handler methods --------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	public void startElement(String uri, String localName, String rawName, Attributes attributes) {
		// detect svrl:failed-assert and svrl:successful-report element
		if (rawName.equals(FAILED_ASSERT_ELT)) {
			svrlDiagRefCounter=0;
			this.roleAttribute=attributes.getValue("role");
			//if the role atrribute is not set, then consider the default as error
			if(this.roleAttribute==null){
				this.roleAttribute="Error";
			}
			else
				roleAttribute=VPUtility.convertToTitleCase(roleAttribute);

			this.message.append("[assert] " + attributes.getValue(LOCATION_ATT));
			this.lastElement = FAILED_ASSERT_ELT;
			underAssertorReport = true;
		} else if (rawName.equals(SUCCESSFUL_REPORT_ELT)) {
			this.message.append("[report] " + attributes.getValue(LOCATION_ATT));
			this.lastElement = SUCCESSFUL_REPORT_ELT;
			underAssertorReport = true;
		} else if (rawName.equals(TEXT_ELT) && underAssertorReport == true) {
			// clean the buffer to start collecting text of svrl:text
			getCharacters();
			this.diag_attr.setLength(0);
		}
		else if (rawName.equals(DIAGNOSTIC_REFERENCE_ELT)) {
			svrlDiagRefCounter+=1;
			/*if(svrlDiagRefCounter==2 && attributes.getValue("diagnostic").equals("inter-vis-id")){
				this.diagnosticReference.remove(this.diagnosticReference.size()-1);
				removePrev=true;
			}*/
			//this.diag_attr.setLength(0);
			//this.diag_attr.append("GraphId="+attributes.getValue(DIAGNOSTIC_ATT));
			getCharacters();
		}
		//chandan
		//System.out.println("the  last processed element--"+ this.lastElement);
		//chandan
	}

	/**
	 * {@inheritDoc}
	 */
	public void endElement(String namespaceURL, String localName, String rawName) {
		String temp=null;
		// reach the end of svrl:text and collect the text data
		if (rawName.equals(TEXT_ELT) && underAssertorReport == true) {
			diag_attr.append(getCharacters());
			this.message.append(" - " + diag_attr );
			//check the last element name to decide where to store the validation message
			if (this.lastElement.equals(FAILED_ASSERT_ELT)) {
				this.failedAssertions.add(getMessage());
			} else {
				this.successfulReports.add(getMessage());       
			}
			underAssertorReport = false;
		}
		//chandan
		else if(rawName.equals(DIAGNOSTIC_REFERENCE_ELT)){
			//this.diagnosticReference.add(diag_attr+":"+getCharacters()+" ");
			if(svrlDiagRefCounter==1){
				//the output containing the graphid and the diagonostic message 
				temp = getCharacters();
				this.diagnosticReference.add(temp+"@@"+this.roleAttribute+" - "+diag_attr);
			}/*else if(svrlDiagRefCounter==2 && removePrev){
				temp = getCharacters();
				this.diagnosticReference.add(temp+"@@"+this.roleAttribute+" - "+diag_attr);
				removePrev=false;
			}*/
			//this.diagnosticReference.add(getCharacters());
			//this.diag_attr.setLength(0);
		}
		//chandan
		else if (rawName.equals(FAILED_ASSERT_ELT)){
			if(svrlDiagRefCounter==0)//temp will go as null in this case
				this.diagnosticReference.add(temp+"@@"+this.roleAttribute+" - "+diag_attr);
		}
		//chandan
		this.lastElement = "";
	}

	/**
	 * {@inheritDoc}
	 */
	public void characters(char[] ch, int start, int length) {
		// print svrl:text text node if the lastElement is svrl:text
		this.chars.append (ch, start, length);
	}

	/**
	 * Return the collected text data so far and clean the buffer
	 * @return collected text data on the buffer
	 */
	private String getCharacters() {
		String retstr = this.chars.toString();
		this.chars.setLength(0);
		return retstr;
	}

	/**
	 * @return the constructed validation message
	 */
	private String getMessage() {
		String retstr = this.message.toString();
		this.message.setLength(0);
		return retstr;
	}

}
