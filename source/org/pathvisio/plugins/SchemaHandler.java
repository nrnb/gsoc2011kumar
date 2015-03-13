package org.pathvisio.plugins;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX handler to parse Schematron rulesets chosen using the "Choose Ruleset" button. The 
 * retrieved values are then stored in the corresponding variables to be used by the plugin
 * for setting the ruleset's title, type (GPML/MIM/SBGN) and extracting rule-groups.
 */
public class SchemaHandler extends DefaultHandler {

	private String theTitle; // schema's title
	private String defaultPhase;
	private String type; // schema's type
	private List<String> phases = new ArrayList<String>();
	private StringBuilder chars = new StringBuilder();
	private int iso_ns_counter = 0;// <iso:ns> tag counter

	public void startElement(String uri, String localName, String rawName,
			Attributes attributes) {

		if (rawName.equals("iso:phase")) {
			phases.add(attributes.getValue("id"));
		}
		
		else if (rawName.equals("iso:ns")) {

			iso_ns_counter++;

			if (iso_ns_counter == 1) {
				this.type = attributes.getValue("prefix");
			}

		}

		else if (rawName.equals("iso:title")) { // here getcharacters()
												// necessary to flushout the
												// empty spaces
			this.theTitle = getCharacters();
		}
		
		else if(rawName.equals("iso:schema")){
			this.defaultPhase=attributes.getValue("defaultPhase");
		}

	}

	
	public void endElement(String namespaceURL, String localName, String rawName) {

		if (rawName.equals("iso:title")) {
			this.theTitle = getCharacters();
		}

	}

	public void characters(char[] ch, int start, int length) {
		// print svrl:text text node if the lastElement is svrl:text
		this.chars.append(ch, start, length);
	}

	/**
	 * To retrieve and clear the String in the String Builder "chars"
	 * @return the String accumulated in the "chars" 
	 */
	private String getCharacters() {
		String retstr = this.chars.toString();
		this.chars.setLength(0);
		return retstr;
	}

	// getters and setters are below

	public void setPhases(List<String> phases) {
		this.phases = phases;
	}

	public List<String> getPhases() {
		return phases;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setTheTitle(String theTitle) {
		this.theTitle = theTitle;
	}

	public String getTheTitle() {
		return theTitle;
	}
	
	public String getDefaultPhase() {
		return defaultPhase;
	}


}
