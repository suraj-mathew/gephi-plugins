package com.nuuday.gephi_handler;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.gephi.io.generator.plugin.DynamicGraph;

import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.EdgeMergeStrategy;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.importer.api.NodeDraft;
import org.gephi.io.processor.plugin.AppendProcessor;
import org.gephi.project.api.Project;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.ProjectMetaData;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

public class MyGephiBridge {

	private static final int MAX_COLOURS = 50;
	private static final ArrayList<Color> COLOR_ARRAY = new ArrayList<Color>(MAX_COLOURS);
	private static final Map<String,Color> DOMAIN_COLOR_MAP = new HashMap<String,Color>();
	private static final Object lockObject = new Object();

	//TODO - not sure whether this variable can be made static. but going for a try
	private static ProjectController projectController = null;
	private static Project project = null;
	private static File projectFile = null;
        private static Container container = null;
	private static ContainerLoader containerLoader = null;
        
	static {
		loadColorArray();
	}
	
	Workspace workspace = null;
	ImportController importController = null;

	public MyGephiBridge() {
		synchronized (lockObject) {
			if(projectController == null) {
				projectController = Lookup.getDefault().lookup(ProjectController.class);
				projectController.newProject();
				
				project = projectController.getCurrentProject();
				project.getLookup().lookup(ProjectMetaData.class).setAuthor("suraj");
				project.getLookup().lookup(ProjectMetaData.class).setTitle("Mitm-Java-Proxy");
				
				workspace = projectController.newWorkspace(project);
                                workspace.getWorkspaceMetadata().setTitle("Mitm-Java-Proxy-Gephi");
				
				container = Lookup.getDefault().lookup(Container.Factory.class).newContainer();
		                container.setSource("Mitm-Java-Proxy");
                                
                                containerLoader = container.getLoader();
                                containerLoader.setEdgesMergeStrategy(EdgeMergeStrategy.FIRST);
                                containerLoader.setAllowAutoNode(false);
                
				/*File parentFolder = new File("mygraphs"); 
				projectFile = new File(parentFolder,"MitmJavaProxy.gephi");

				projectFile.mkdirs(); 
				try { 
					projectFile.createNewFile(); 
				} catch (IOException e) { // TODO Auto-generated catch block 
					e.printStackTrace(); 
				} 
				projectFile = projectFile.getAbsoluteFile();

				projectController.saveProject(project, projectFile);*/

			}
		}
	}
	
	private static void loadColorArray() {
		for (int arrayIndex = 0; arrayIndex < MAX_COLOURS; arrayIndex++) {
			//colors.add(Color.getHSBColor((float) i / (float) n, 0.6f, 0.75f));
			COLOR_ARRAY.add(Color.getHSBColor((float) arrayIndex / (float) MAX_COLOURS, 0.85f, 1f));
			//colors.add(Color.getHSBColor((float) i / (float) n, 0.65f, 0.8f));
		}
	}
	
	public void addUrlDataToGraph(URI uri) {
		//Generate a new random graph into a container
		
		//id is the full url including path
		//label is path
		
		NodeDraft sourceNode;
		NodeDraft destinationNode;
		EdgeDraft edgeDraft;
		
		String domain = uri.getHost();
		String queryParams = uri.getQuery();
		String path = uri.getPath();
		
		if (!containerLoader.nodeExists(domain)) {
                    System.out.println("NEW NODE NEEDED - "+domain);
			sourceNode = containerLoader.factory().newNodeDraft(domain);
			
			if (!DOMAIN_COLOR_MAP.containsKey(domain) && !DOMAIN_COLOR_MAP.isEmpty()) {
				DOMAIN_COLOR_MAP.put(domain, COLOR_ARRAY.remove(0));
			}

			if (DOMAIN_COLOR_MAP.containsKey(domain)) {
				sourceNode.setColor(DOMAIN_COLOR_MAP.get(domain));
				sourceNode.setLabelVisible(true);
			} else {
				sourceNode.setColor(Color.BLACK);
				sourceNode.setLabelVisible(false);
			}
			
			sourceNode.setLabel(domain);
			
			/*
			 * for (String attrib : n.attributes.keySet()) { Object value =
			 * n.attributes.get(attrib); nd.setValue(attrib, value); }
			 */

			
			sourceNode.setSize(100f);
			
			
			containerLoader.addNode(sourceNode);
		} else {
                    System.out.println("NODE EXIST - "+domain);
			sourceNode = containerLoader.getNode(domain);
		}
		
		for(String pathParam : path.split("/")) {
			
			if(pathParam.trim().length() == 0) {
				continue;
			}
			if (!containerLoader.nodeExists(pathParam)) {
                            System.out.println("NEW DEST NODE FOR EDGE NEEDED - "+pathParam);
				destinationNode = containerLoader.factory().newNodeDraft(pathParam);
				destinationNode.setLabel(pathParam);
				destinationNode.setSize(100f);
				containerLoader.addNode(destinationNode);
			} else {
                            System.out.println("NODE DEST FOR EDGE ALREADY EXISTS - "+pathParam);
				destinationNode = containerLoader.getNode(pathParam);
			}
			
			if(!containerLoader.edgeExists(sourceNode.getId(), destinationNode.getId())) {
                            System.out.println("NEW EDGE NEEDED - "+sourceNode.getLabel()+"-"+destinationNode.getLabel());
				edgeDraft = containerLoader.factory().newEdgeDraft(pathParam);
				
				edgeDraft.setSource(sourceNode);
                                edgeDraft.setTarget(destinationNode);
                                edgeDraft.setLabel(sourceNode.getLabel()+"-"+destinationNode.getLabel());
                                edgeDraft.setLabelVisible(true);
                                
                                containerLoader.addEdge(edgeDraft);
			}
			
			sourceNode = destinationNode;
		}

		container.verify();
                
		//projectController.saveProject(project);
		
		if (importController == null) {
			importController = Lookup.getDefault().lookup(ImportController.class);
		}
		
		//Processor processor = Lookup.getDefault().lookup(Processor.class);
		
                DynamicGraph dynamicGraph = new DynamicGraph();
                dynamicGraph.generate(containerLoader);
		
                importController.process(container, new AppendProcessor(),workspace);
                
		//projectController.saveProject(project);
		
		
	}
	
	public static void main(String[] args) {
		System.out.println("00000");
				
		MyGephiBridge mg = new MyGephiBridge();
		
		
		try {
			System.out.println("1111");
			mg.addUrlDataToGraph(new URI("http://myhost/abc/pqr/efg/123/test"));
			System.out.println("2222");
			mg.addUrlDataToGraph(new URI("http://myhost/abc/pqr/456/xyz/test"));
			System.out.println("3333");
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
