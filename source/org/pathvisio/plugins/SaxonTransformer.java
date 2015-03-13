package org.pathvisio.plugins;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.pathvisio.preferences.PreferenceManager;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Responsible for XSL Transformations of ruleset with SVRL skeleton file and then the resultant 
 *  with the exported pathway file; also does the parsing of the resultant SVRL file
 *  using SAX Parser and the SVRLHandler class
 */
public class SaxonTransformer {

	private File schemaFile, inputFile, svrlFile;
	private SAXParser saxParser;
	private SVRLHandler handler ;
	private TransformerFactory factory;// = new net.sf.saxon.TransformerFactoryImpl();
	private Transformer transformer1;
	private boolean produceSvrl = false;

	public SaxonTransformer(SAXParser saxParser) throws TransformerConfigurationException {
		factory = new net.sf.saxon.TransformerFactoryImpl();
		System.setProperty("javax.xml.transform.TransformerFactory",
				"net.sf.saxon.TransformerFactoryImpl");
		transformer1 = factory
				.newTransformer(new StreamSource(getClass().getResource("/iso_svrl_for_xslt2.xsl").toString()));
		this.saxParser=saxParser;
	}

	public void setProduceSvrl(boolean produceSvrl) {
		this.produceSvrl = produceSvrl;
	}

	public void setschemaFile(File schemaFile) {
		this.schemaFile = schemaFile;
	}

	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}

	public Transformer getTransformer1() {
		return transformer1;
	}

	public SVRLHandler getHandler() {
		return handler;
	}
	
	/*public InputStream[] getFiles() {

		// String isoName = "/XSLs/iso_svrl_for_xslt2.xsl";

		InputStream[] in = {
				getClass().getResourceAsStream("/resources/mimschema.sch"),
				getClass().getResourceAsStream("/resources/example.mimml") };
		return in;
	}*/
	
	/*public URL getUrlToIso() {

		return getClass().getResource("/iso_svrl_for_xslt2.xsl");
		// above line for ECLIPSE build

		// below line for ANT JAR BUILD
		// return getClass().getResource("/XSLs/iso_svrl_for_xslt2.xsl");
	}*/
	
	/**
	 * this does the XSL Transformations on the ruleset and the exported Pathway Object
	 * and then invokes the SAX parser through "parseSVRL" method on the transformation's result.
	 */
	public void produceSvrlAndThenParse() throws 
			ParserConfigurationException, TransformerException, IOException, SAXException {

		// String schemaSystemId = new File(args[0]).toURL().toExternalForm();
		// String inputFileSystemId = new
		// File(args[1]).toURL().toExternalForm();
		
		// InputStream[] in=getFiles();

		Source schemaSource = new StreamSource(schemaFile);
		Source inputSource = new StreamSource(inputFile);

		StringWriter sw1 = new StringWriter();
		Result result1 = new StreamResult(sw1);
		// Result result2 = new
		// StreamResult(r2=File.createTempFile("SVRL_OUTPUT", null));
		// r2.deleteOnExit();

		transformer1.transform(schemaSource, result1);
		System.out.println("xsl cretaed");

		Transformer transformer2 = factory.newTransformer(new StreamSource(
				new StringReader(sw1.toString())));
		StringWriter sw2 = new StringWriter();
		Result result2 = new StreamResult(sw2);
		transformer2.transform(inputSource, result2);
		// to produce the svrl output in a file in the user's preferred directory
		//if(svrlFile==null)
			svrlFile = new File(PreferenceManager.getCurrent()
					.get(VPUtility.SchemaPreference.SVRL_FILE));

		if (produceSvrl) {
			transformer2.transform(inputSource, new StreamResult(svrlFile));
			// produceSvrl=false;
		}
		else {
			svrlFile.delete();
			svrlFile=null;
		}
		
		System.out.println("svrl cretaed");
		// System.out.println(sw2.toString());
		parseSVRL(removeXMLheader(sw2.toString()));
		// printMessages();
	}

	/**
	 * removes the first line in the SVRL if it contains XML header i.e
	 * strips the input string of its XML header
	 * @param svrl the SVRL string for which the XML header is to be removed
	 * @return String without the XML header 
	 */
	private String removeXMLheader(String svrl) {

		int firstLineEnd = svrl.indexOf("\n");
		if (svrl.startsWith("<?xml ") || svrl.startsWith("<?xml ", 1)
				|| svrl.startsWith("<?xml ", 2) // Handle Unicode BOM
				|| svrl.startsWith("<?xml ", 3)) {
			return svrl.substring(firstLineEnd + 1);
		} else
			return svrl;
	}

	/**
	 * parses the input SVRL String using the SVRLHandler, results are put into 
	 *  diagnosticReference
	 * @param svrl resultant SVRL String from the XSL Transformations
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	private void parseSVRL(String svrl) throws IOException, SAXException,
			ParserConfigurationException {

		//if(handler==null) 
			handler = new SVRLHandler();
		
		/*else{
			//reset the arraylists in the handler
			handler.getDiagnosticReference().clear();
			handler.getFailedAssertions().clear();
			handler.getSuccessfulReports().clear();
		}*/
		// System.out.println(this.svrl);
		InputSource is = new InputSource(
				new StringReader(svrl));
		is.setEncoding("UTF-16");
		//if(saxParser==null)saxParser= SAXParserFactory.newInstance().newSAXParser();
		saxParser.parse(is, handler);
	}

	/*private void printMessages() {

		Iterator<String> tempIterator = diagnosticReference.iterator();
		while (tempIterator.hasNext()) {
			// Logger.log.debug(tempIterator.next());
			System.out.println(tempIterator.next());
		}
	}*/
}