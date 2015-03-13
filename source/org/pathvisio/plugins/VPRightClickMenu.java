package org.pathvisio.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.pathvisio.model.PathwayElement;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.view.VPathwayElement;

/**
 * This class is related to the right click related UI and events on the jtable. It also is responsible 
 * for left click events on the jtable
 */
public class VPRightClickMenu implements ActionListener{

	private ValidatorPlugin vplugin;

	VPRightClickMenu(ValidatorPlugin vplugin){
		this.vplugin=vplugin;
	}

	/**
	 * Instantiation and layout of right-click related Objects done here (like JPopupMenu,
	 *  menuItems in it, lists of ignored messages etc. ) 
	 */
	void createAndInitialize_RightClickMenuUI()throws IOException,ClassNotFoundException{
		
		// loading the globally ignored list from the serialized list saved in memory.
		/*		if(serializedInfoFile.exists()){
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedInfoFile));
			globallyIgnoredEWType = (ArrayList<String>) ois.readObject();
			ois.close();
		}
		else 
			//globallyIgnoredEWType = new ArrayList<String>();*/

		String IRList = PreferenceManager.getCurrent().get
			(VPUtility.SchemaPreference.GLOBALLY_IGNORED_RULES);

		if(!IRList.equals("")){
			vplugin.globallyIgnoredEWType = new ArrayList<String>( Arrays.asList(IRList.split( "@@" )));
		/*	for(String s:vplugin.globallyIgnoredEWType) 
				System.out.println("GIL-- "+s);*/
		}else 
			vplugin.globallyIgnoredEWType = new ArrayList<String>();

		vplugin.ignoredErrorTypesList=new ArrayList<String>();
		vplugin.ignoredElements=new ArrayList<String>();
		vplugin.ignoredSingleError=new ArrayList<String>();
		vplugin.checkedUnchecked = new int[4];

		//The 2 icon objects below are used wherever the icon images appear in the UI
		VPUtility.eIcon = new ImageIcon(vplugin.getClass().getResource("/error.png"));
		VPUtility.wIcon = new ImageIcon(vplugin.getClass().getResource("/warning.png"));

		vplugin.popup = new JPopupMenu("filter");// popup named "filter"

		JMenuItem menuItem1237= new VPUtility.CustomMenuItem("Ignore Element");
		menuItem1237.addActionListener(this);
		menuItem1237.setActionCommand("menuItem1");
		vplugin.popup.add(menuItem1237);

		menuItem1237 = new VPUtility.CustomMenuItem("Ignore this Error/Warning");
		menuItem1237.setActionCommand("menuItem2");
		menuItem1237.addActionListener(this);
		vplugin.popup.add(menuItem1237);

		menuItem1237 = new VPUtility.CustomMenuItem("Ignore this Error/Warning Type");
		menuItem1237.setActionCommand("menuItem3");
		menuItem1237.addActionListener(this);
		vplugin.popup.add(menuItem1237);

		menuItem1237 = new VPUtility.CustomMenuItem("Ignore this Error/Warning Type Globally");
		menuItem1237.setActionCommand("menuItem7");
		menuItem1237.addActionListener(this);
		vplugin.popup.add(menuItem1237);
		vplugin.popup.addSeparator();

		ImageIcon img=new ImageIcon(getClass().getResource("/ignore.png"));
		vplugin.subMenu4= new JMenu("Ignored Error/Warning Types");
		vplugin.subMenu4.setIcon(img);

		vplugin.subMenu5= new JMenu("Ignored Elements");
		vplugin.subMenu5.setIcon(img);

		vplugin.subMenu6= new JMenu("Ignored Errors/Warnings");
		vplugin.subMenu6.setIcon(img);

		vplugin.subMenu8= new JMenu("Globally Ignored Error/Warning Types");
		vplugin.subMenu8.setIcon(img);

		JMenuItem subMenuItemOkButton=new VPUtility.CustomMenuItem( "Reconsider (Un-Ignore)");
		subMenuItemOkButton.setActionCommand("subMenu4ReConsider");
		subMenuItemOkButton.addActionListener(this);
		subMenuItemOkButton.setEnabled(false);
		vplugin.subMenu4.add(subMenuItemOkButton);

		subMenuItemOkButton=new VPUtility.CustomMenuItem( "Reconsider (Un-Ignore)");
		subMenuItemOkButton.setActionCommand("subMenu5ReConsider");
		subMenuItemOkButton.addActionListener(this);
		subMenuItemOkButton.setEnabled(false);
		vplugin.subMenu5.add(subMenuItemOkButton);

		subMenuItemOkButton=new VPUtility.CustomMenuItem( "Reconsider (Un-Ignore)");
		subMenuItemOkButton.setActionCommand("subMenu6ReConsider");
		subMenuItemOkButton.addActionListener(this);
		subMenuItemOkButton.setEnabled(false);
		vplugin.subMenu6.add(subMenuItemOkButton);

		subMenuItemOkButton=new VPUtility.CustomMenuItem( "Reconsider (Un-Ignore)");
		subMenuItemOkButton.setActionCommand("subMenu8ReConsider");
		subMenuItemOkButton.addActionListener(this);
		subMenuItemOkButton.setEnabled(false);
		vplugin.subMenu8.add(subMenuItemOkButton);

		vplugin.subMenu4.addSeparator();
		vplugin.subMenu4.setEnabled(false);

		vplugin.subMenu5.addSeparator();
		vplugin.subMenu5.setEnabled(false);

		vplugin.subMenu6.addSeparator();
		vplugin.subMenu6.setEnabled(false);

		vplugin.subMenu8.addSeparator();
		vplugin.subMenu8.setEnabled(false);

		vplugin.popup.add(vplugin.subMenu5);
		vplugin.popup.add(vplugin.subMenu6);
		vplugin.popup.add(vplugin.subMenu4);
		vplugin.popup.add(vplugin.subMenu8);

		repopulateGIEWTSubMenu();
	}

	/**
	 * delegate method to listen to the mouse-click events from the JTable present in JPanel.
	 */
	void listenToJTableMouseClicks(java.awt.event.MouseEvent e){

		//code to highlight node whether its a left/right click event
		int row=vplugin.jtb.rowAtPoint(e.getPoint());
		if(VPUtility.prevPwe!=null && vplugin.graphIdsList.size()!=0 ){ 
			vplugin.jtb.getSelectionModel().setSelectionInterval(row, row);
			if( ! VPUtility.prevPwe.isHighlighted() ){
				VPUtility.prevHighlight=false; 
			}

			if(VPUtility.prevHighlight){
				VPUtility.prevPwe.highlight(VPUtility.col2);//col2 is blue
			}
			else 
				VPUtility.prevPwe.unhighlight();

			String gId=vplugin.graphIdsList.get(row);
			if(!gId.equals("null") && !gId.equals("")){
				//System.out.println("graphId is "+graphIdsList.get(row));
				VPathwayElement vpe = null;
				if( (vpe=vplugin.highlightNode(gId,VPUtility.col1))!=null ){
					//to focus on the highlighted  element
					ValidatorPlugin.eng.getActiveVPathway().getWrapper()
					.scrollCenterTo((int)vpe.getVBounds().getCenterX(),(int)vpe.getVBounds().getCenterY());
					ValidatorPlugin.eng.getActiveVPathway().redraw();
				}
			} 
			else 
				System.out.println("graphId is null or empty");
			//System.out.println(" Value in the cell clicked :"+jtb.getValueAt(row,col).toString());
		}
		else 
			System.out.println("VPUtility.prevPwe is null or no errors");

		//right click event handling is done here
		if ( e.getButton()== MouseEvent.BUTTON3 || e.isControlDown() ) { //iscontrolDown is for MAC OS
			vplugin.jtb.clearSelection();

			boolean discardFirst2Menus=false;
			String eachCellTip = vplugin.jtb.getToolTipText(e);
			if(eachCellTip!=null) 
				discardFirst2Menus=eachCellTip.equals("----");    			
			//decide which main menuitems (1,2,3) to show and which not to
			if(discardFirst2Menus || VPUtility.allIgnored){
				//System.out.println("inside if d a "+discardFirst2Menus+" "+VPUtility.allIgnored);
				if(discardFirst2Menus) 
					vplugin.jtb.getSelectionModel().setSelectionInterval(row, row);

				//if(popup.getComponent(2).isEnabled())
				for(int MI=0; MI<4 ; MI++){
					if( (MI==2 || MI==3)&& discardFirst2Menus){
						vplugin.popup.getComponent(MI).setEnabled(true); 
					}
					else
						vplugin.popup.getComponent(MI).setEnabled(false); 
				}
			}	
			else {
				//System.out.println("inside else d a "+discardFirst2Menus+" "+VPUtility.allIgnored);
				vplugin.jtb.getSelectionModel().setSelectionInterval(row, row);//to select the row with right click
				//if(!popup.getComponent(2).isEnabled())
				for(int MI=0; MI<4 ; MI++)
					vplugin.popup.getComponent(MI).setEnabled(true); 
			}
			vplugin.popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	/**
	 * removes all the ignored rules (except the global one) from the lists and the pop-up menu
	 */
	void clearRightClickStuff(){
		JMenu jm;
		vplugin.ignoredErrorTypesList.clear();
		vplugin.ignoredElements.clear();
		vplugin.ignoredSingleError.clear();

		//checkedUnchecked= new int[4];
		int index=vplugin.popup.getComponentIndex(vplugin.subMenu5);
		int cuIndex=0;
		for(int i=index;i<index+3;i++){ // removing the 3 submenus 5,6,4
			vplugin.checkedUnchecked[cuIndex++]=0;
			jm=(JMenu)vplugin.popup.getComponent(i);
			jm.setEnabled(false);
			jm.getMenuComponent(0).setEnabled(false);
			String jmText=jm.getText();
			int jmIndex=jmText.indexOf('(');
			if(jmIndex==-1)
				jm.setText(jmText);
			else
				jm.setText(jmText.substring(0,jmIndex-1));

			// clearing the subMenu4's checkboxes
			while(jm.getMenuComponentCount()>2){
				jm.remove(2);
			}
		}
		vplugin.checkedUnchecked[cuIndex]=0;
	}

	/**
	 * This method enables/disables the "Reconsider (Un-Ignore)" button in the sub menu item 
	 * @param jcbmi the JCheckBoxMenuItem that received the check/uncheck event 
	 */
	void checkUncheck(JCheckBoxMenuItem jcbmi){
		//since can not access the parent directly, hence used getInvoker method 
		JMenu subMenu=(JMenu)((JPopupMenu)jcbmi.getParent()).getInvoker();
		int indx = subMenu==vplugin.subMenu4 ? 0 : ((subMenu==vplugin.subMenu5) ? 1 : ((subMenu==vplugin.subMenu6)? 2 : 3 ));

		if (jcbmi.getState())  // increment the corresponding index's value, if its an uncheck
			vplugin.checkedUnchecked[indx]++;
		else 
			vplugin.checkedUnchecked[indx]--; // decrement the corresponding index's value, if its an uncheck 

		if(vplugin.checkedUnchecked[indx]==0) { // if value at index is 0, corresponding "Reconsider" button is disabled
			subMenu.getMenuComponent(0).setEnabled(false);
		}else 
			subMenu.getMenuComponent(0).setEnabled(true);
	}

	/**
	 * all the chosen items from the popup menu (in the Ignored list) are considered back for validation again.
	 * @param subMenu the subMenu from which the items were selected to be reconsidered 
	 * @param ignoredList the list corresponding to the subMenu
	 */
	void reConsiderTheIgnored(JMenu subMenu,List<String> ignoredList){
		int lengthOfIgnored=subMenu.getMenuComponentCount();
		int index=lengthOfIgnored-1;
		String subMenuText=subMenu.getText();
		//System.out.println("total in submenu4 "+subMenu4.getMenuComponentCount());
		while(index>1){
			if( ( (JCheckBoxMenuItem)subMenu.getMenuComponent(index) ).getState() ){
				ignoredList.remove(index-2);//since submenu index has min of 2 (0,1 are not removed)
				subMenu.remove(index);
			}
			index--;
		}

		if(ignoredList.isEmpty()){ 
			subMenu.setEnabled(false);
			subMenu.setText( subMenuText.substring(0,subMenuText.indexOf('(')-1) );
		} 
		else 
			subMenu.setText(subMenuText.substring(0,subMenuText.indexOf('('))+"("+ignoredList.size()+")");

		vplugin.checkedUnchecked=new int[4];// reset all the indices to 0 after the reconsider button's clicked
		subMenu.getMenuComponent(0).setEnabled(false);//disable reconsider button
		vplugin.printItOnTable();// in order to refresh the validation messages
		//serializeIgnoredRules();
		saveGIRules();
	}

	/**
	 * This method adds ignored Errors/Warnings (E/W) chosen from the pop-up menu to the corresponding main 
	 * menuItem and also to the ignored list, so they get picked up for validation.
	 * @param subMenu the main menuItem inside which the ignored E/W text has to be added as a subMenuItem
	 * @param EWMtext E/W text to be added
	 * @param ignList the list to which the ignored rules are added, to be used in reconsidering back the rules. 
	 * @param refreshTable indicates whether to add to the ignList and refresh the messages in the table or not,
	 * set it to false when using this method in a loop.
	 */
	void addToSubMenu(JMenu subMenu,String EWMtext, List<String> ignList, boolean refreshTable){ // Error/Warning message Text : EWMText
		String subMenuText=subMenu.getText();
		if(refreshTable)
			ignList.add(EWMtext);

		if(EWMtext.length()>80) 
			EWMtext = EWMtext.substring(0,78)+"...";

		if(ignList==vplugin.ignoredSingleError)
			EWMtext=EWMtext.replace("@@", " : ");
		else if (ignList==vplugin.ignoredElements) {
			PathwayElement pe=ValidatorPlugin.pth.getElementById(EWMtext);
			if(pe!=null){
				EWMtext=EWMtext+" : "+pe.getObjectType();
			}
			else 
				EWMtext=EWMtext+" : "+"Non-highlightable Component";
			
		}

		JCheckBoxMenuItem subMenuItemCBMI= new JCheckBoxMenuItem(EWMtext);
		subMenuItemCBMI.setUI(new VPUtility.StayOpenCheckBoxMenuItemUI());
		subMenuItemCBMI.setActionCommand("subMenuItemCBMI");
		subMenuItemCBMI.addActionListener(this);
		subMenu.add(subMenuItemCBMI);

		if(ignList.size()==1){ 
			subMenu.setEnabled(true);  
			int indexP=subMenuText.indexOf('(');
			if(indexP==-1)
				subMenu.setText(subMenuText+" ("+1+")");
			else 
				subMenu.setText(subMenuText.substring(0,indexP-1)+" ("+1+")");
		}

		if(refreshTable){
			vplugin.printItOnTable();
			if(ignList.size()>1)
				subMenu.setText(subMenuText.substring(0,subMenuText.indexOf('('))+"("+ignList.size()+")");
			//serializeIgnoredRules();
			saveGIRules();
		}
		//else System.out.println("adding the serialised stuff back to menu");
	}

	/*private void serializeIgnoredRules(){
		try {
			if(oos ==null){
				System.out.println("serialize method called");
				oos=new ObjectOutputStream(new FileOutputStream(serializedInfoFile));
			}
			oos.writeObject(globallyIgnoredEWType);
			oos.flush();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			JOptionPane.showMessageDialog(vPlugin.desktop.getFrame(), 
					"problem with serializing gloabally ignored list","Validator Plugin",JOptionPane.ERROR_MESSAGE);
			e1.printStackTrace();
		}

	}
	 */	

	/**
	 * this stores the "globallyIgnoredEWType" list's current state which can be retrieved on the next run. 
	 */
	private void saveGIRules(){
		StringBuilder result=new StringBuilder("");
		for(String rule:vplugin.globallyIgnoredEWType ){
			result=result.append(rule+"@@"); // "@@" is the delimiter between the rule texts stored in String in the pref file 
		}
		PreferenceManager.getCurrent().set(VPUtility.SchemaPreference.GLOBALLY_IGNORED_RULES, result.toString());
	}

	/**
	 * this populates the "Globally Ignored EW Type" menu (subMenu8) with the list "globallyIgnoredEWType". Its called
	 * only once at the start in this plugin.
	 */
	private void repopulateGIEWTSubMenu(){
		if(vplugin.globallyIgnoredEWType.size()!=0){
			vplugin.subMenu8.setEnabled(true);
			for(String str:vplugin.globallyIgnoredEWType){
				//System.out.println("adding the serialised stuff back to menu");
				addToSubMenu(vplugin.subMenu8, str, vplugin.globallyIgnoredEWType,false);
			}
			vplugin.subMenu8.setText("Globally Ignored Error/Warning Types ("+vplugin.globallyIgnoredEWType.size()+")");
		}
	} 

	/**
	 * invoked when action related to right-click option's meuItems occur 
	 */
	public void actionPerformed(ActionEvent e) {

		if ("menuItem1".equals(e.getActionCommand())) { //Ignore this Element
			//System.out.println("pressed Igonore Element");
			int rowNumberClicked = vplugin.jtb.getSelectedRow() ;
			String graphIdToAdd = vplugin.graphIdsList.get(rowNumberClicked);
			addToSubMenu(vplugin.subMenu5,graphIdToAdd,vplugin.ignoredElements, true);

			/*for(String s : ignoredElements){
				System.out.println(s);
				}*/
		}

		else if ("menuItem2".equals(e.getActionCommand())) {//Ignore this Error
			//System.out.println("pressed Ignore this Error/Warning");
			int rowNumberClicked = vplugin.jtb.getSelectedRow() ;
			String graphIdToAdd = vplugin.graphIdsList.get(rowNumberClicked);

			String valueAtTheRow=(String)vplugin.jtb.getValueAt(vplugin.jtb.getSelectedRow(), 1);
			valueAtTheRow=valueAtTheRow.substring(valueAtTheRow.indexOf('.')+3);

			String combined= graphIdToAdd+"@@"+valueAtTheRow;
			addToSubMenu(vplugin.subMenu6,combined,vplugin.ignoredSingleError,true);
		}

		else if ("menuItem3".equals(e.getActionCommand())) {//Ignore this Error Type
			//System.out.println("Ignore this Error Type pressed");
			//if(ignoredErrorTypesList==null) ignoredErrorTypesList=new ArrayList<String>();
			String valueAtTheRow=(String)vplugin.jtb.getValueAt(vplugin.jtb.getSelectedRow(), 1);
			valueAtTheRow=valueAtTheRow.substring(valueAtTheRow.indexOf('.')+3);
			addToSubMenu(vplugin.subMenu4,valueAtTheRow,vplugin.ignoredErrorTypesList,true);
		}
		else if ("menuItem7".equals(e.getActionCommand())) {//Ignore this Error Type
			//System.out.println("Ignore this Error Type Globally pressed");
			String valueAtTheRow=(String)vplugin.jtb.getValueAt(vplugin.jtb.getSelectedRow(), 1);
			valueAtTheRow=valueAtTheRow.substring(valueAtTheRow.indexOf('.')+3);
			addToSubMenu(vplugin.subMenu8,valueAtTheRow,vplugin.globallyIgnoredEWType,true);
		}
		
		// Listeners for the menuItems (4,5,6)'s popped-up-submenuitems :("Reconsider (Un-Ignore)" button Listeners)) 
		else if ("subMenu5ReConsider".equals(e.getActionCommand())) {
			reConsiderTheIgnored(vplugin.subMenu5,vplugin.ignoredElements);
		}

		else if ("subMenu4ReConsider".equals(e.getActionCommand())) {
			reConsiderTheIgnored(vplugin.subMenu4,vplugin.ignoredErrorTypesList);
		}

		else if ("subMenu6ReConsider".equals(e.getActionCommand())) {
			reConsiderTheIgnored(vplugin.subMenu6,vplugin.ignoredSingleError);
		}
		else if ("subMenu8ReConsider".equals(e.getActionCommand())) {
			reConsiderTheIgnored(vplugin.subMenu8,vplugin.globallyIgnoredEWType);
		}

		// Listener for submenuItems that are dynamically generated in in 4,5,6 menuItems above
		else if("subMenuItemCBMI".equals(e.getActionCommand())){
			JCheckBoxMenuItem jcbmi=(JCheckBoxMenuItem)e.getSource();
			//okButtonED(jcbmi); // this method can be a bit slower for large groups
			checkUncheck(jcbmi); // This must be faster 
		}

	}

}
