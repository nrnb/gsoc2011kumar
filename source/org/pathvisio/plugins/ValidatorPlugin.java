package org.pathvisio.plugins;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;

import org.codehaus.groovy.control.CompilationFailedException;
import org.jdesktop.swingworker.SwingWorker;
import org.pathvisio.ApplicationEvent;
import org.pathvisio.Engine;
import org.pathvisio.Engine.ApplicationEventListener;
import org.pathvisio.gui.swing.ProgressDialog;
import org.pathvisio.gui.swing.PvDesktop;
import org.pathvisio.model.Pathway;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.plugin.Plugin;
import org.pathvisio.plugins.VPUtility.RuleSetNotSupportedException;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.sbgn.SbgnFormat;
import org.pathvisio.sbgn.SbgnShapes;
import org.pathvisio.util.ProgressKeeper;
import org.pathvisio.view.VPathwayElement;
import org.pathvisio.view.VPathwayListener;
import org.xml.sax.SAXException;
import edu.stanford.ejalbert.BrowserLauncher;
import gov.nih.nci.lmp.mimGpml.MIMFormat;
import groovy.lang.GroovyObject;

/**
 * The plugin class for the Validator
 * 
 */
public class ValidatorPlugin implements Plugin,ActionListener, ApplicationEventListener,
	ItemListener, ComponentListener
{
	PvDesktop desktop;
	private static File  schemaFile; // the file object pointing to the ruleset chosen from the chooser
	private static File exportedPathwayFile; // "Pathway" object is exported in XML format(gpml/mimml/sbgnml) to this file 
	static Engine eng; // "Engine" object reference passed to the plugin will be stored in this 
	static Pathway pth; // "Pathway" object reference
	static SaxonTransformer saxTfr ; 
	private  static MIMFormat mimf;//=new MIMFormat();
	private static SbgnFormat sbgnf;
	private static JFileChooser chooser; // for "Choose Ruleset" button
	private final static JButton valbutton=new JButton("Validate"); // the "Validate" button
	private final static JButton chooseSchema=new JButton("Choose Ruleset"); // "Choose Ruleset" button
	final static JComboBox ewBox = new JComboBox(new String[]{"Errors & Warnings","Errors only","Warnings only"});
	private final static JComboBox phaseBox = new JComboBox(new String[]{VPUtility.phaseLabelInCBox+"All"});
	private final ValidatorHelpAction vhelpAction= new ValidatorHelpAction();// for  help->Validator Help
	static JButton highlightAllButton; 
	final JLabel eLabel=new JLabel("Errors:0",new ImageIcon(getClass().getResource("/error.png")),SwingConstants.CENTER);
	final JLabel wLabel=new JLabel("Warnings:0",new ImageIcon(getClass().getResource("/warning.png")),SwingConstants.CENTER);
	static final JTextField schemaTitleTag= new JTextField(VPUtility.rulesetTitleLabel);
	private static GroovyObject grvyObject;
	static List<Object> globGroovyResult;
	final VPUtility.MyTableModel mytbm=new VPUtility.MyTableModel();
	final JCheckBox svrlOutputChoose= new JCheckBox(" Generate SVRL file",false);
	private GroovyValidator groovyValidator;
	private SchematronValidator schematronValidator;
	private VPRightClickMenu vpRCMenu;
	private ValidatorPlugin vPlugin=this;
	private SAXParser saxParser;
	//private File serializedInfoFile= new File(System.getProperty("user.home"), "GloballyIgnored.ser");
	//private ObjectOutputStream oos;
	
	final JTable jtb = new JTable(mytbm){
		public Class getColumnClass(int column){  
			return getValueAt(0, column).getClass();  
		}  
	};

	private VPathwayListener vpwListener = new VPUtility.VPWListener(jtb);
	final List<String> graphIdsList = new ArrayList<String>();
	List<String> ignoredErrorTypesList, ignoredElements, ignoredSingleError, globallyIgnoredEWType;
	JPopupMenu popup;
	JMenu subMenu4,subMenu5, subMenu6, subMenu8;
	int[] checkedUnchecked;

	public ValidatorPlugin(){
		System.out.println("init callled");
	}

	public void init(PvDesktop desktop) 
	{   
		// save the desktop reference so we can use it later
		this.desktop = desktop;
		eng=desktop.getSwingEngine().getEngine();
		// To listen to the events from the engine (eg. Pathaway-opened event)
		eng.addApplicationEventListener(this);  

		// register Validator help page action in the "Help" menu.
		desktop.registerMenuAction ("Help", vhelpAction);
		
		//register listener for pathway area click event (especially for autosaved pathways)
		if(eng.hasVPathway())
			eng.getActiveVPathway().addVPathwayListener(vpwListener);
		
		createPluginUIAndTheirListeners();

		//code for setting the winx and winy of main window of pathvisio in preference file
		//PreferenceManager.getCurrent().set(GlobalPreference.WIN_X, "0");
		//PreferenceManager.getCurrent().set(GlobalPreference.WIN_Y,"0");

		//initialization code for exportedPathwayFile object
		if(exportedPathwayFile==null){
			//schemaFile=new File("D:\\schematron\\mimschema.sch");
			try {
				//exportedPathwayFile=File.createTempFile("pvv","val");
				exportedPathwayFile=new File(System.getProperty("java.io.tmpdir"), "ValidatorPluginExportedPathway.xml");
				exportedPathwayFile.deleteOnExit();
			} catch (Exception e) {
				System.out.println("Exception in creating current pathway temp file "+e);
				JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
						"Could not generate the export file ","Validator Plugin",JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		}
	}

	public void done() {
		System.out.println("unloading plugin");
		//SaxonTransformer.getSvrlFile().deleteOnExit();
	}

	/**
	 * Almost all the UI related objects are created or assigned with listeners here.
	 */
	private void createPluginUIAndTheirListeners(){

		schemaTitleTag.setEditable(false);
		schemaTitleTag.addComponentListener(this);

		//creating a button for choosing the schema file 
		//chooseSchema=new JButton("Choose Ruleset");
		chooseSchema.setActionCommand("choose");
		chooseSchema.addActionListener(this);

		//creating a jcheckbox ("Highlight All") and set its status from the .pathvisio pref file
		/*boolean jbcinit;
		if(PreferenceManager.getCurrent().getInt(VPUtility.SchemaPreference.CHECK_BOX_STATUS)==1){
			jbcinit=true;
		}else jbcinit=false;*/

		highlightAllButton= new JButton(" Highlight All");
		highlightAllButton.setActionCommand("HighlightAll");
		highlightAllButton.addActionListener(this);
		highlightAllButton.setEnabled(false);//set to false , to enable it only when validate is pressed
		//valbutton.setEnabled(!jbcinit);

		svrlOutputChoose.setActionCommand("svrlOutputChoose");
		svrlOutputChoose.addActionListener(this);
		svrlOutputChoose.setEnabled(false);
		svrlOutputChoose.setToolTipText("Go to Edit->Preferences to change file-name/location");

		//combo-box for message filtering (Errors Vs Warnings)
		//final JComboBox ewBox = new JComboBox(new String[]{"Errors & Warnings","Errors only","Warnings only"});
		ewBox.setActionCommand("ewBox");
		ewBox.addActionListener(this);
		ewBox.setEnabled(false);
		//ewBox.setSelectedIndex(1);

		phaseBox.setActionCommand("phaseBox");
		//phaseBox.addActionListener(this);
		phaseBox.addItemListener(this);
		phaseBox.setEnabled(false);

		//adding labels for warning and error counts
		eLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
		wLabel.setHorizontalTextPosition(SwingConstants.RIGHT);

		//jtable settings
		jtb.setTableHeader(null);
		jtb.setFont(new Font("Verdana", Font.PLAIN, 15));
		//jtb.setGridColor(Color.WHITE);

		mytbm.addColumn("image"); 
		mytbm.addColumn("errors and warnings");

		jtb.getColumn("image").setMaxWidth(23);

		jtb.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent e){
				vpRCMenu.listenToJTableMouseClicks(e);
			}
		});

		jtb.getColumnModel().getColumn(1).setCellRenderer(new TextAreaRenderer(vPlugin));

		//create a scrollpane and adding jtable to the pane
		final JScrollPane scrollPane = new JScrollPane(jtb);
		scrollPane.getViewport().setBackground(Color.WHITE);
		//scrollPane.setOpaque(true);
		//scrollPane.setForeground(Color.white);
		//scrollPane.setColumnHeader(null);

		//code for layout of the components in JPanel goes below
		final JPanel mySideBarPanel = new JPanel (new GridBagLayout());
		//Font f=new Font("Times New Roman", Font.BOLD, 16);
		final GridBagConstraints c = new GridBagConstraints();

		c.weighty = 0.0;c.weightx=0.5;
		c.fill = GridBagConstraints.HORIZONTAL;
		//c.gridwidth = GridBagConstraints.RELATIVE;
		mySideBarPanel.add(eLabel,c);
		mySideBarPanel.add(wLabel,c);

		c.fill=GridBagConstraints.NONE;
		c.gridwidth = GridBagConstraints.REMAINDER;
		mySideBarPanel.add(svrlOutputChoose,c);

		c.fill=GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.RELATIVE;
		mySideBarPanel.add(schemaTitleTag,c);

		//c.fill=GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		mySideBarPanel.add(phaseBox,c);

		//c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		mySideBarPanel.add(scrollPane,c);

		c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.RELATIVE;
		mySideBarPanel.add(highlightAllButton,c);

		c.gridwidth = GridBagConstraints.REMAINDER;
		mySideBarPanel.add(ewBox,c);

		//final JButton valbutton=new JButton("validate");
		valbutton.setActionCommand("validate");
		valbutton.addActionListener(this);

		c.gridwidth=GridBagConstraints.RELATIVE;
		mySideBarPanel.add(valbutton,c);
		mySideBarPanel.add(chooseSchema,c);

		// get a reference to the sidebar and add the jpanel created above, to it
		final JTabbedPane sidebarTabbedPane = desktop.getSideBarTabbedPane();
		sidebarTabbedPane.add("Validator", mySideBarPanel);
		sidebarTabbedPane.setSelectedComponent(mySideBarPanel);

		//adding Validator plugin's preferences in the "preferences" window of edit->preferences
		desktop.getPreferencesDlg().addPanel("Validator", 
				desktop.getPreferencesDlg().builder()
				//.booleanField(VPUtility.SchemaPreference.APPLY_IGNORED_RULES_CHECKBOX, "Apply ignored rules globally")
				.fileField(VPUtility.SchemaPreference.SVRL_FILE, 
						"SVRL report file :", false)
				.build());

	}

	/**
	 * Open a Browser link to the plugin's help page when this action is triggered. 
	 */
	private class ValidatorHelpAction extends AbstractAction
	{
		ValidatorHelpAction()
		{
			// The NAME property of an action is used as 
			// the label of the menu item
			putValue (NAME, "Validator Help");
		}

		/**
		 *  called when the user selects the menu item
		 */
		public void actionPerformed(ActionEvent arg0) 
		{	
			try
			{
				BrowserLauncher bl = new BrowserLauncher(null);
				bl.openURLinBrowser("http://pathvisio.org/wiki/PathwayValidatorHelp");
			}
			catch (Exception ex)
			{
				JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
						"could not launch the page","Validator Plugin",JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}	
		}
	}

	/**
	 * "validatePathway" method calls this method internally. This runs in a separate thread to do the task 
	 */
	void processTask(ProgressKeeper pk, ProgressDialog d, SwingWorker<Object, Object> sw) {

		sw.execute();
		d.setVisible(true);

		try {
			sw.get();
			//ewBox.setEnabled(true);jcb.setEnabled(true);
		} catch (Exception e)//InterruptedException,ExecutionException
		{
			System.out.println("ExecutionException in ValidatorPlugin---"+e.getMessage());
			JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"Validation Exception in Schematron","Validator Plugin",JOptionPane.ERROR_MESSAGE);
			resetUI(false);
			return;
		}
	}

	/**
	 * method to carry out the validation task in background, while a progress bar runs in the foreground   
	 */
	void validatePathway(final SaxonTransformer tempSaxTrnfr,final MIMFormat mimf){

		final ProgressKeeper pk = new ProgressKeeper();
		final ProgressDialog d = new ProgressDialog(desktop.getFrame(),"Validator plugin", pk, false, true);
		SwingWorker<Object, Object> sw = new SwingWorker<Object, Object>() {

			protected Object doInBackground() {
				pk.setTaskName("Validating pathway");

				try{
					schematronValidator.exportAndValidate(tempSaxTrnfr, mimf,
							sbgnf, eng.getActivePathway(), exportedPathwayFile, schemaFile);
				}
				catch (Exception e1) { //changed from ConverterException to catch all the errors
					//System.out.println("Exception in validatepathway method--"+e1.getMessage());
					JOptionPane.showMessageDialog(desktop.getFrame(), 
							"Validation Exception in Schematron","Validator Plugin",JOptionPane.ERROR_MESSAGE);
					e1.printStackTrace();
					resetUI(false);
					return null;
				}
				finally {
					pk.finished();
				}

				return null;
			}
		};

		processTask(pk, d, sw);
	}

	/**
	 * a method which decides , whether to call Schematron or Groovy's print 
	 * method based on the ruleset(schemaFile) selected
	 */
	void printItOnTable(){
		if(!VPUtility.schemaFileType.equalsIgnoreCase("groovy"))
			schematronValidator.printSchematron(eng,graphIdsList, ignoredErrorTypesList,
					globallyIgnoredEWType, ignoredElements,ignoredSingleError);
		else
			groovyValidator.sortGroovyResultsAndPrint(globGroovyResult);
	}
	
	/**
	 * It highlights a particular element in the Pathway corresponding to the String passed to it. 
	 */
	VPathwayElement highlightNode(String gId, Color col){
		PathwayElement pe=pth.getElementById(gId);
		VPathwayElement vpe = null;

		if(pe!=null) {
			vpe=eng.getActiveVPathway().getPathwayElementView(pe);
			vpe.highlight(col);
			VPUtility.prevPwe=vpe;
		}
		else {
			System.out.println("no available graphId @ id: "+gId);
		}
		return vpe;
	}

	/**
	 * this highlights all the nodes present in the graphIdsList. 
	 */
	private void vhighlightAll(){
		for(String s:graphIdsList){
			highlightNode(s,VPUtility.col2);
		}
		eng.getActiveVPathway().redraw();
		VPUtility.prevHighlight=true;
	}

	/**
	 * removes all the JTable rows (i.e removes the validation messages) and enables mouse-clicks on JTable 
	 */
	void clearTableRows(){
		mytbm.setRowCount(0);
		jtb.setEnabled(true);
	}
	
	/**
	 * It resets the state of the UI elements (to default state)
	 * @param resetSchemaTitleAlso flag based on which phaseBox and schemaTitleTag are also reset. 
	 */
	void resetUI(boolean resetSchemaTitleAlso){
		clearTableRows();
		
		if(resetSchemaTitleAlso){
			schemaTitleTag.setText(VPUtility.rulesetTitleLabel);
			VPUtility.schemaString="";
			VPUtility.resetPhaseBox(phaseBox);
		}	
		
		if(eng.hasVPathway())
			eng.getActiveVPathway().resetHighlight();

		if(ignoredErrorTypesList!=null){
			vpRCMenu.clearRightClickStuff();
		}

		ewBox.setEnabled(false);highlightAllButton.setEnabled(false);
		eLabel.setText("Errors:0");wLabel.setText("Warnings:0");
	}

	/**
	 * This method creates the thread in which saxParser and saxTfr are instantiated and also starts the thread. 
	 * Called only once at the start. 
	 * @return Thread object on which join is called later, to make sure that the thread has
	 *  completed its run before proceeding further 
	 */
	private Thread create_SAXTFR_InAThread(){
		//System.out.println("choose pressed for 1st time");
		Thread threadForSax=new Thread(){ 
			public void run(){

				try {
					System.out.println("This thread for saxtranform runs");
					//if(saxParser==null)
					saxParser=SAXParserFactory.newInstance().newSAXParser();
					saxTfr= new SaxonTransformer(saxParser);
					saxTfr.setInputFile(exportedPathwayFile);
					
					PreferenceManager.init();
					SbgnShapes.registerShapes();
					sbgnf = new SbgnFormat(); 
					mimf=new MIMFormat();
					
					System.out.println("This thread for saxtranform completes its run");
				} catch (TransformerConfigurationException e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
							"problem while configuring Saxon Transformer","Validator Plugin",JOptionPane.ERROR_MESSAGE);
					resetUI(true);
				} catch (ParserConfigurationException e) {
					JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
							"problem while configuring SaxParser","Validator Plugin",JOptionPane.ERROR_MESSAGE);
					resetUI(true);
					e.printStackTrace();
				} catch (SAXException e) {
					JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
							"SaxException occured","Validator Plugin",JOptionPane.ERROR_MESSAGE);
					resetUI(true);
					e.printStackTrace();
				}
			}
		};
		threadForSax.start();
		return threadForSax;
	}

	/**
	 * instantiating "Choose Ruleset" button related Objects, its called only the first time choose is clicked
	 * @return null or the Thread Object created in the create_SAXTFR_InAThread method. 
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private Thread chooseButtonInitialisation() throws InterruptedException,IOException,ClassNotFoundException{
		Thread threadForSax=null;

		if(chooser==null){
			chooser=new JFileChooser();
			vpRCMenu=new VPRightClickMenu(vPlugin);
			vpRCMenu.createAndInitialize_RightClickMenuUI();

			threadForSax=create_SAXTFR_InAThread();

			chooser.setDialogTitle("Choose Ruleset");
			chooser.setApproveButtonText("Open");
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.setCurrentDirectory(PreferenceManager.getCurrent().getFile(VPUtility.SchemaPreference.LAST_OPENED_SCHEMA_DIR));

			chooser.addChoosableFileFilter(new FileFilter() {

				public boolean accept(File f) {

					if(f.isDirectory()) return true;

					String ext = f.toString().substring(f.toString().length()-3);

					if(ext.equalsIgnoreCase("sch")||ext.equalsIgnoreCase("ovy")||ext.equalsIgnoreCase("xml")) {
						return true;
					}

					return false;
				}

				public String getDescription() {
					return "Schematron (.sch & .xml) & Groovy (.groovy)";
				}

			});

		}
		return threadForSax;
	}

	
	/**
	 *  "chooseRuleset" method delegates to this method. This does the handling of events related to 
	 *  "Choose Ruleset" button.
	 * @throws RuleNotSupportedException 
	 */
	private void chooseRulesetButtonListener() throws InterruptedException,
		IOException,IllegalAccessException,InstantiationException,
		SAXException,CompilationFailedException,ClassNotFoundException, RuleSetNotSupportedException{
		
		//System.out.println("choose schema button pressed");
		Thread threadForSax=null;
		threadForSax=chooseButtonInitialisation();

		int returnVal = chooser.showOpenDialog(desktop.getFrame());

		//wait for the transformer creation in the thread to complete 
		if(threadForSax!=null){
			threadForSax.join();
			threadForSax=null;
		}
		System.out.println("after thread's join");
		
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			// reset
			//stopping the changes made by changing the state of phasebox to 0
			VPUtility.changeOfSchema=true;
			phaseBox.setSelectedIndex(0);
			VPUtility.changeOfSchema=false;
			ewBox.setSelectedIndex(0);
			
			schemaFile=chooser.getSelectedFile();
			System.out.println("You chose this schematron file: "+schemaFile.getName());
			//System.out.println("schema is of type: "+(VPUtility.schemaFileType=whichSchema(schemaFile)));

			String schemaFileSubString=(schemaFile.toString().substring(schemaFile.toString().length()-3));
			//if the file chosen is of type ".groovy", then do Groovy specific logic
			if(schemaFileSubString.equalsIgnoreCase("ovy") ){
				svrlOutputChoose.setEnabled(false);
				VPUtility.schemaFileType="groovy";
				//phaseBox.setEnabled(false);
				if(groovyValidator==null)
					groovyValidator=new GroovyValidator(vPlugin,eng,phaseBox,graphIdsList);
				grvyObject=groovyValidator.loadGroovy(schemaFile);

			}
			// if the chosen file is of type ".sch" (schema file)
			else {
				svrlOutputChoose.setEnabled(true);
				if(schematronValidator == null) 
					schematronValidator=new SchematronValidator(vPlugin);
				schematronValidator.parseSchemaAndSetValues(saxParser, saxTfr.getTransformer1(), schemaFile,
						desktop.getFrame(), schemaTitleTag, phaseBox);
			}
			// setting/clearing the rightclick related stuff
			vpRCMenu.clearRightClickStuff();
			validateButtonListener();
			PreferenceManager.getCurrent().setFile(VPUtility.SchemaPreference.LAST_OPENED_SCHEMA_DIR, schemaFile);
		}
	}
	
	/**
	 * delegate method which listens directly to the events from "Choose Ruleset" button, its present
	 * especially to avoid passing of the exceptions to the validation phase (validateButtonListener method)
	 */
	private void chooseRuleset(){
		try {
			chooseRulesetButtonListener();
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"problem with the SaxonTranformer","Validator Plugin",JOptionPane.ERROR_MESSAGE);
			resetUI(true);
			e.printStackTrace();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"Ruleset/serialized file not accesible","Validator Plugin",JOptionPane.ERROR_MESSAGE);
			resetUI(true);
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"problem with the Groovy Ruleset","Validator Plugin",JOptionPane.ERROR_MESSAGE);
			resetUI(true);
			e.printStackTrace();
		} catch (InstantiationException e) {
			JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"problem with the Groovy Ruleset","Validator Plugin",JOptionPane.ERROR_MESSAGE);
			resetUI(true);
			e.printStackTrace();
		} catch (SAXException e) {
			JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"problem with the Schematron Ruleset","Validator Plugin",JOptionPane.ERROR_MESSAGE);
			resetUI(true);
			e.printStackTrace();
		}
		catch (CompilationFailedException e) {
			JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"problem while compiling Groovy Ruleset","Validator Plugin",JOptionPane.ERROR_MESSAGE);
			resetUI(true);
			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"problem with deserialization","Validator Plugin",JOptionPane.ERROR_MESSAGE);
			resetUI(true);
			e.printStackTrace();
		}
		catch(VPUtility.RuleSetNotSupportedException e){
			JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"Ruleset: "+e.rulesetType+" is not yet supported." ,"Validator Plugin",JOptionPane.ERROR_MESSAGE);
			resetUI(true);
			e.printStackTrace();
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"problem with the Ruleset","Validator Plugin",JOptionPane.ERROR_MESSAGE);
			resetUI(true);
			e.printStackTrace();
		}
	}


	/**
	 *  a delegate method which does the handling of events related to "Validate" button
	 */
	private void validateButtonListener(){
		//System.out.println("validate button pressed ");
		//initializing the color objects
		if(VPUtility.col1==null){
			VPUtility.col1= new Color(255,0,0);
			VPUtility.col2=new Color(0,0,255);
		}

		// check whether a schema is chosen or not 
		if(schemaFile==null){
			JOptionPane.showMessageDialog(desktop.getFrame(), 
				"Please choose a Ruleset and then press Validate");
			chooseRuleset();
			return;
		}
		
		//check "Choose Ruleset" success
		if(VPUtility.schemaString.equals("")){
			JOptionPane.showMessageDialog(desktop.getFrame(), 
				"cannot validate with the current ruleset, please choose another");
			return;
		}
			

		// check if a pathway is opened
		if(!eng.hasVPathway()){
			JOptionPane.showMessageDialog(desktop.getFrame(), 
			"Please open a Pathway to start validation");
			desktop.getSwingEngine().openPathway();
			return;
		}

		//if the control reaches here, then every thing's fine until choose ruleset
		//reset values (default option), when "Validate" is pressed
		ewBox.setEnabled(true);ewBox.setSelectedIndex(0);
		VPUtility.prevSelect=0;highlightAllButton.setEnabled(true);
		
		if(!VPUtility.schemaFileType.equalsIgnoreCase("groovy")){ 
			validatePathway(saxTfr,mimf);
			//printSchematron();
		}
		else {
			groovyValidator.runGroovy(grvyObject);
		}
	}

	/////////////////////////////////////////////////////////////////////////////////		
	//Listeners for most of of the UI components are below this line:
	/////////////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent e) {
		// Listener method for "Validate" button
		if ("validate".equals(e.getActionCommand())) { // "Validate" button preseed
			validateButtonListener();
		}

		// Listener method for "Choose Ruleset" button
		else if ("choose".equals(e.getActionCommand())) { // "Choose Ruleset" button pressed 
			chooseRuleset();
		}

		// Listener for "Hightlight All" button 
		else if("HighlightAll".equals(e.getActionCommand())){ 
			//serializeIgnoredRules();
			vhighlightAll();
		}

		// Listener for "errors/warnings drop down box"
		else if("ewBox".equals(e.getActionCommand())){ 

			JComboBox cbox = (JComboBox)e.getSource();
			if(VPUtility.prevSelect != cbox.getSelectedIndex()) {
				VPUtility.prevSelect = cbox.getSelectedIndex();
				printItOnTable();
				//System.out.println(cbox.getSelectedItem());
			}
		}

		// Listener for the "Generate SVRL file" checkbox
		else if("svrlOutputChoose".equals(e.getActionCommand())){

			if( ((JCheckBox)e.getSource()).isSelected() ){
				saxTfr.setProduceSvrl(true); 
			}else 
				saxTfr.setProduceSvrl(false);
		}

	}

	/**
	 * Override method for receiving events when a pathway is opened/closed/loaded
	 *	basically to clear the panel and reset values on this event
	 */
	public void applicationEvent(ApplicationEvent e) {

		if( e.getType()==ApplicationEvent.PATHWAY_OPENED || e.getType()==ApplicationEvent.PATHWAY_NEW){
			resetUI(false);
			eng.getActiveVPathway().addVPathwayListener(vpwListener);
			
		}
	}

	// overridden method for ItemListener interface, invoked for change in phasebox selection
	public void itemStateChanged(ItemEvent arg0) {  

		if(!VPUtility.changeOfSchema && arg0.getStateChange()==ItemEvent.SELECTED){
			//phaseBoxSelection=((JComboBox)arg0.getSource()).getSelectedIndex();
			if(!VPUtility.schemaFileType.equalsIgnoreCase("groovy")){
				//donot forget to change the index if the "Phase: " format is changed
				String temp=( (String)arg0.getItem() ).substring(7);

				if(temp.equals("All")){
					saxTfr.getTransformer1().setParameter("phase", "#ALL" );
				}
				else{
					saxTfr.getTransformer1().setParameter("phase", temp );
				}
				//System.out.println("item selected --"+temp );
			}
			if(eng.hasVPathway()) validateButtonListener();
		}
	}

	// the 4 methods below are the method overrides for ComponentListener interface, only the 3rd one below is 
	//actually used here, in order to handle events related to change in component's size
	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub

	}
	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub

	}
	public void componentResized(ComponentEvent e) {
		// TODO Auto-generated method stub
		if(VPUtility.schemaString!=null){
			VPUtility.cutSchemaTitleString(VPUtility.schemaString,schemaTitleTag);
			//System.out.println("no of chars "+schemaTitleTag.get);
		}
	}
	public void componentShown(ComponentEvent e) {
		// TODO Auto-generated method stub

	}

}	