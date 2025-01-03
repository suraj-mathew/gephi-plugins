package com.nuuday.gephi_handler;

import com.nuuday.commons.CommonUtils;
import com.nuuday.commons.LoggerUtils;
import com.nuuday.mitm_proxy_handler.MyProxyStarter;
import java.awt.Color;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.openide.util.lookup.ServiceProvider;
import org.gephi.io.generator.spi.Generator;
import org.gephi.io.generator.spi.GeneratorUI;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.EdgeMergeStrategy;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.importer.api.NodeDraft;
import org.gephi.io.processor.spi.Processor;
import org.gephi.layout.api.LayoutController;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.project.api.Project;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.utils.progress.ProgressTicket;
import org.jose4j.json.internal.json_simple.parser.ContainerFactory;
import org.openide.util.Lookup;

/**
 *
 * @author surajmathew
 */
@ServiceProvider(service = Generator.class)
public class MyGephiGenerator implements Generator {

    private static final int MAX_COLOURS = 50;
    private static final ArrayList<Color> COLOR_ARRAY = new ArrayList<Color>(MAX_COLOURS);
    private static final Map<String, Color> DOMAIN_COLOR_MAP = new HashMap<String, Color>();

    protected ProgressTicket progress;
    private Workspace workspace = null;
    private Container container = null;
    ImportController importController = null;

    private MyProxyStarter proxyStarter = null;

    private boolean isProcessCancelled = false;

    static {
        loadColorArray();
    }

    @Override
    public void generate(ContainerLoader containerLoader) {

        LoggerUtils.info("----GENERATE-----");

        String currentHeadMessage = null;

        try {
            progress.start();
            
            //start server
            proxyStarter = new MyProxyStarter();
            LoggerUtils.info("----777777-----");
            proxyStarter.startProxy();
            LoggerUtils.info("----8888888888-----");

            LoggerUtils.info("----11111111-----");
            /*containerLoader.setEdgesMergeStrategy(EdgeMergeStrategy.FIRST);
            containerLoader.setAllowAutoNode(false);
            containerLoader.setFillLabelWithId(true);*/
            
            ProjectController projectController = Lookup.getDefault().lookup(ProjectController.class);
Project project = projectController.newProject();
 workspace = projectController.getCurrentWorkspace();
 
 workspace.getWorkspaceMetadata().setTitle("Mitm-Java-proxy-workspace");
  container = Lookup.getDefault().lookup(Container.Factory.class).newContainer();
  container.setSource("Mitm-Java-proxy-container");
            
            LoggerUtils.info("----22222-----");
            
       containerLoader = container.getLoader();
            //workspace = Lookup.getDefault().lookup(ProjectController.class).openNewWorkspace();
            //LoggerUtils.info("----333333333-----"+workspace);
            //workspace.getWorkspaceMetadata().setTitle("Mitm-Java-Proxy-workspace");
            LoggerUtils.info("----444444444-----");
            //workspace.getProject().getProjectMetadata().setTitle("Mitm-Java-Proxy-p");
            LoggerUtils.info("----555555-----");

            while (!isProcessCancelled) {

                currentHeadMessage = CommonUtils.getFirstElementFromMessageQueue();

                if (CommonUtils.isNotNull(currentHeadMessage)) {
                    LoggerUtils.info("---- Got new message from queue -----");
                    try {
                        addUrlDataToGraph(new URI(currentHeadMessage),containerLoader);
                    } catch (URISyntaxException uriSyntaxException) {
                        LoggerUtils.info("###### URISyntaxException for \"" + currentHeadMessage + "\". Hence skipping this url");
                        LoggerUtils.info(uriSyntaxException.getMessage());
                    }
                } else {
                    //adding an extra wait as the queue is empty
                    Thread.sleep(1000);
                }

                Thread.sleep(100);
            }
        } catch (Exception ex) {
            LoggerUtils.severe("###### Failed to start server ########");
            LoggerUtils.severe(ex.getMessage());
        }
    }

    @Override
    public String getName() {
        return "Mitm-Java-Proxy";
    }

    @Override
    public GeneratorUI getUI() {
        return Lookup.getDefault().lookup(GeneratorUI.class);
    }

    @Override
    public boolean cancel() {
        try {
            proxyStarter.stopProxy();
        } catch (Exception ex) {
            LoggerUtils.warn("Exception while stopping proxy server");
            LoggerUtils.warn(ex.getMessage());
        }

        isProcessCancelled = true;
        progress.finish();
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        LoggerUtils.info("======== SETTING PROGRESS TICKET ======");
        this.progress = progressTicket;
    }

    private static void loadColorArray() {
        for (int arrayIndex = 0; arrayIndex < MAX_COLOURS; arrayIndex++) {
            COLOR_ARRAY.add(Color.getHSBColor((float) arrayIndex / (float) MAX_COLOURS, 0.85f, 1f));
        }
    }

    private NodeDraft createOrGetNode(String nodeName, ContainerLoader containerLoader) {
        LoggerUtils.info("NEW NODE NEEDED - " + nodeName);
        NodeDraft nodeDraft = null;
        
        if (!containerLoader.nodeExists(nodeName)) {
            
            nodeDraft = containerLoader.factory().newNodeDraft(nodeName);
            
            if (!DOMAIN_COLOR_MAP.containsKey(nodeName) && !COLOR_ARRAY.isEmpty()) {
                LoggerUtils.info("----ADD to DOMAIN_COLOR_MAP -----");
                DOMAIN_COLOR_MAP.put(nodeName, COLOR_ARRAY.remove(0));
            }
            
            if (DOMAIN_COLOR_MAP.containsKey(nodeName)) {
                LoggerUtils.info("----Set Node color -----" + DOMAIN_COLOR_MAP.get(nodeName));
                nodeDraft.setColor(DOMAIN_COLOR_MAP.get(nodeName));
            } else {
                LoggerUtils.info("----Set Node color AS GREY-----");
                nodeDraft.setColor(Color.LIGHT_GRAY);
            }
            
            nodeDraft.setLabelVisible(true);
            nodeDraft.setLabel(nodeName);
            nodeDraft.setLabelColor(Color.BLACK);
            nodeDraft.setSize(10f);
            nodeDraft.setLabelSize(14f);
            
            containerLoader.addNode(nodeDraft);
        } else {
            LoggerUtils.info("NODE EXIST - " + nodeName);
            nodeDraft = containerLoader.getNode(nodeName);
            nodeDraft.setSize(nodeDraft.getSize() + 1f);
            LoggerUtils.info("NODE - visible?"+nodeDraft.isLabelVisible()+"-size-"+nodeDraft.getSize());
        }
        
        return nodeDraft;
    }
    
    private void addUrlDataToGraph(URI uri,ContainerLoader containerLoader) {
        
        NodeDraft sourceNode;
        NodeDraft destinationNode;
        EdgeDraft edgeDraft;

LoggerUtils.info("----addUrlDataToGraph 11111 -----"+uri.toString());
        String domain = uri.getHost();
        String queryParams = uri.getQuery();
        String path = uri.getPath();
LoggerUtils.info("----addUrlDataToGraph domain -----"+domain);
            sourceNode = createOrGetNode(domain, containerLoader);
       

        for (String pathParam : path.split("/")) {

            if (pathParam.trim().length() == 0) {
                continue;
            }
            
                destinationNode = createOrGetNode(pathParam, containerLoader);
            

            if (!containerLoader.edgeExists(sourceNode.getId(), destinationNode.getId())) {
                LoggerUtils.info("NEW EDGE NEEDED - " + sourceNode.getLabel() + "-" + destinationNode.getLabel());
                edgeDraft = containerLoader.factory().newEdgeDraft(pathParam);

                edgeDraft.setSource(sourceNode);
                edgeDraft.setTarget(destinationNode);
                edgeDraft.setLabel(pathParam);
                edgeDraft.setLabelVisible(true);
                edgeDraft.setLabelColor(Color.BLUE);
                edgeDraft.setLabelSize(14f);
                edgeDraft.setColor(Color.BLACK);
                edgeDraft.setWeight(5f);
                
                containerLoader.addEdge(edgeDraft);
            }

            sourceNode = destinationNode;
        }

        LoggerUtils.info("About to import ---");
        
        if (importController == null) {
            importController = Lookup.getDefault().lookup(ImportController.class);
        }
        
                
        LoggerUtils.info("About to process ---");
        Processor processor = Lookup.getDefault().lookup(Processor.class);
        LoggerUtils.info("processing ---");
       importController.process(container, processor, workspace);
       
       //See if graph is well imported
       LoggerUtils.info("See if graph is well imported ---");
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        DirectedGraph graph = graphModel.getDirectedGraph();
LoggerUtils.info("--> Nodes: " + graph.getNodeCount());
LoggerUtils.info("--> Edges: " + graph.getEdgeCount());
 
//Layout for 1 minute
/*AutoLayout autoLayout = new AutoLayout(1, TimeUnit.MINUTES);
LoggerUtils.info("--> layout 11111: ");
autoLayout.setGraphModel(graphModel);
LoggerUtils.info("--> layout 22222: ");
        YifanHuLayout firstLayout = new YifanHuLayout(null, new StepDisplacement(1f));
        LoggerUtils.info("--> layout 3333: ");
ForceAtlasLayout secondLayout = new ForceAtlasLayout(null);
LoggerUtils.info("--> layout 4444: ");
AutoLayout.DynamicProperty adjustBySizeProperty = AutoLayout.createDynamicProperty("forceAtlas.adjustSizes.name", Boolean.TRUE, 0.1f);//True after 10% of layout time
LoggerUtils.info("--> layout 55555: ");
AutoLayout.DynamicProperty repulsionProperty = AutoLayout.createDynamicProperty("forceAtlas.repulsionStrength.name", Double.parseDouble("500"), 0f);//500 for the complete period
LoggerUtils.info("--> layout 66666: ");
autoLayout.addLayout(firstLayout, 0.5f);
LoggerUtils.info("--> layout 77777: ");
autoLayout.addLayout(secondLayout, 0.5f, new AutoLayout.DynamicProperty[]{adjustBySizeProperty, repulsionProperty});
LoggerUtils.info("--> layout 88888: ");
autoLayout.execute();
*/

LoggerUtils.info("YifanHuLayout ---");
YifanHuLayout firstLayout = new YifanHuLayout(null, new StepDisplacement(1f));
firstLayout.setGraphModel(graphModel);
firstLayout.initAlgo();
firstLayout.resetPropertiesValues();
firstLayout.setOptimalDistance(200f);
 LoggerUtils.info("YifanHuLayout ---222222222");
for (int i = 0; i < 100 && firstLayout.canAlgo(); i++) {
   firstLayout.goAlgo();
}
LoggerUtils.info("YifanHuLayout ---3333333");
firstLayout.endAlgo();
        
        LoggerUtils.info("Done ---");
    }
}
