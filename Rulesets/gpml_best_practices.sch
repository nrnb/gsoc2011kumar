<?xml version="1.0" encoding="UTF-8"?>
<!-- 

Schematron validation for GPML Best Practices

@author Augustin Luna
@version 3 August 2011
-->
<iso:schema    
  xmlns:iso="http://purl.oclc.org/dsdl/schematron"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  defaultPhase="#ALL"
  schemaVersion="0.1.2">
     
	<iso:ns prefix="gpml" uri="http://genmapp.org/GPML/2010a"/>
	<iso:ns prefix="bp" uri="http://www.biopax.org/release/biopax-level3.owl#"/>
	
	<iso:title>GPML Best Practices</iso:title>

	<!-- Check that the Name attribute is present and not empty -->	
	<iso:pattern name="check-title" id="check-title">
		<iso:rule context="gpml:Pathway">
			<iso:assert test="@Name and not(@Name='')" role="error">Diagrams should have a title.</iso:assert>
		</iso:rule> 
	</iso:pattern> 

	<!-- Check that the Author attribute is present and not empty -->	
	<iso:pattern name="check-author" id="check-author">
		<iso:rule context="gpml:Pathway">
			<iso:assert test="@Author and not(@Author='')" role="error">Diagrams should have an author.</iso:assert>
		</iso:rule> 
	</iso:pattern> 

	<!-- Check that the Organism attribute is present and not empty -->	
	<iso:pattern name="check-organism" id="check-organism">
		<iso:rule context="gpml:Pathway">
			<iso:assert test="@Organism and not(@Organism='')" role="error">Diagrams should have an organism.</iso:assert>
		</iso:rule> 
	</iso:pattern> 

	<!-- Check that for the existence of a Biopax element -->	
	<iso:pattern name="check-pub-xref" id="check-pub-xref">
		<iso:rule context="gpml:Pathway">
			<iso:assert test="gpml:Biopax" role="error">Diagrams should have references.</iso:assert>
		</iso:rule> 
	</iso:pattern> 
	
	<!-- Check that data nodes have Xref elements -->
	<iso:pattern name="check-db-xref" id="check-db-xref">
		<iso:rule context="gpml:DataNode">
			<iso:let name="graph-id" value="@GraphId"/>

			<!-- Neither the Database or ID attributes should be empty -->
			<iso:assert test="gpml:Xref[not(@Database='')] and gpml:Xref[not(@ID='')]"  role="error" diagnostics="graph-id">Datanodes should include database annotations.</iso:assert>
		</iso:rule> 
	</iso:pattern> 

	<!-- Check that data nodes have TextLabel attributes and that they are not empty -->
	<iso:pattern name="check-labels" id="check-labels">
		<iso:rule context="gpml:DataNode">
			<iso:let name="graph-id" value="@GraphId"/>

			<!-- The TextLabel attribute should not be empty -->
			<iso:assert test="@TextLabel and not(@TextLabel='')" role="error" diagnostics="graph-id">DataNodes should have a text label.</iso:assert>
		</iso:rule> 
	</iso:pattern> 

	<!-- Check that all lines are attached at both ends -->
	<iso:pattern name="check-unattached-lines" id="check-unattached-lines">
		<!-- Check "Line" elements -->
		<iso:rule context="gpml:Line">
			<!-- Get the unique identifier for the line -->
			<iso:let name="graph-id" value="@GraphId"/>

			<!-- Get the GraphRefs (these indicate the GraphIds of the objects the line is connected to) of the first and last points for a line -->
			<iso:let name="start-graph-ref" value="gpml:Graphics/gpml:Point[position()=1]/@GraphRef"/>
			<iso:let name="end-graph-ref" value="gpml:Graphics/gpml:Point[last()]/@GraphRef"/>
			
			<!-- Assert that the GraphRef attributes on the first and last points are present and not empty, otherwise output error message and identifier -->
			<iso:assert test="$start-graph-ref and $end-graph-ref and not($start-graph-ref='') and not($end-graph-ref='')" role="error" diagnostics="graph-id">Lines should not be unattached.</iso:assert>
		</iso:rule> 
	</iso:pattern> 
	
	<!-- List of diagnostics and their values used with the rules -->
	<iso:diagnostics>
		<iso:diagnostic id="graph-id"><iso:value-of select="$graph-id"/></iso:diagnostic> 				
	</iso:diagnostics> 
</iso:schema>