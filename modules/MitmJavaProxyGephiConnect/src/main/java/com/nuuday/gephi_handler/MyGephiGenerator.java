package com.nuuday.gephi_handler;

import com.nuuday.commons.CommonUtils;
import com.nuuday.commons.LoggerUtils;
import com.nuuday.jetty_handler.MyJettyStarter;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.openide.util.lookup.ServiceProvider;
import org.gephi.io.generator.spi.Generator;
import org.gephi.io.generator.spi.GeneratorUI;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.importer.api.NodeDraft;
import org.gephi.io.processor.spi.Processor;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.project.api.Project;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.Lookup;

/**
 * The starting class of gephi-plugin
 *
 * @author surajmathew
 */
@ServiceProvider(service = Generator.class)
public class MyGephiGenerator implements Generator {

    private static final int MAX_COLOURS = 50;
    private static final ArrayList<Color> COLOR_ARRAY = new ArrayList<Color>(MAX_COLOURS);
    private static final Map<String, Color> DOMAIN_COLOR_MAP = new HashMap<String, Color>();
    private static final String MITM_PROXY_GEPHI_PROJECT_FILE_NAME = "mitm-java-proxy.gephi";

    protected ProgressTicket progress;
    private Workspace workspace = null;
    private Container container = null;
    ImportController importController = null;
    Project project = null;
    ProjectController projectController = null;

    private MyJettyStarter jettyServerStarter = null;

    private File projectFile = null;

    private boolean isProcessCancelled = false;

    static {
        loadColorArray();
    }

    /**
     * All custom user action needs to be kick started from this overwritten
     * "generate" method
     */
    @Override
    public void generate(ContainerLoader containerLoader) {

        LoggerUtils.info("---- Opening gephi-plugin control -----");

        String currentHeadMessage = null;

        InputStream inputStream = null;
        OutputStream outputStream = null;

        File projectFolder = null;

        try {
            progress.start();

            projectFolder = new File("my-graphs");
            projectFile = new File(projectFolder, MITM_PROXY_GEPHI_PROJECT_FILE_NAME);

            if (!projectFile.exists()) {
                try {
                    projectFolder.mkdirs();

                    LoggerUtils.info("Copy the blank project file to " + projectFile.getAbsolutePath());

                    inputStream = getClass().getClassLoader().getResourceAsStream(MITM_PROXY_GEPHI_PROJECT_FILE_NAME);
                    outputStream = Files.newOutputStream(projectFile.toPath());

                    inputStream.transferTo(outputStream);

                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();

                } catch (IOException ioException) {
                    LoggerUtils.info("IOException while copying project file from jar - " + ioException.getMessage());
                }
            } else {
                LoggerUtils.info("Using the existing project file from " + projectFile.getAbsolutePath());
            }

            //start server
            jettyServerStarter = new MyJettyStarter();

            LoggerUtils.info("Going to start Jetty server ---- ");

            jettyServerStarter.startServer();

            projectController = Lookup.getDefault().lookup(ProjectController.class);
            project = projectController.openProject(projectFile);
            workspace = project.getCurrentWorkspace();

            container = Lookup.getDefault().lookup(Container.Factory.class).newContainer();
            containerLoader = container.getLoader();

            LoggerUtils.info("Waiting to receive urls from Mitm java proxy server ---- ");

            while (!isProcessCancelled) {

                currentHeadMessage = CommonUtils.getFirstElementFromMessageQueue();

                if (CommonUtils.isNotNull(currentHeadMessage)) {
                    try {
                        addUrlDataToGraph(new URI(currentHeadMessage), containerLoader);
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

    /**
     * This method is invoked when the gephi plugin process is stopped
     *
     * @return
     */
    @Override
    public boolean cancel() {
        try {
            LoggerUtils.info("---- Stopping Jetty server -----");
            jettyServerStarter.stopServer();
        } catch (Exception ex) {
            LoggerUtils.warn("Exception while stopping server");
            LoggerUtils.warn(ex.getMessage());
        }

        isProcessCancelled = true;
        progress.finish();
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;
    }

    private static void loadColorArray() {
        for (int arrayIndex = 0; arrayIndex < MAX_COLOURS; arrayIndex++) {
            COLOR_ARRAY.add(Color.getHSBColor((float) arrayIndex / (float) MAX_COLOURS, 0.85f, 1f));
        }
    }

    private NodeDraft createOrGetNode(String nodeName, ContainerLoader containerLoader, boolean isDomainNode) {

        NodeDraft nodeDraft = null;

        if (!containerLoader.nodeExists(nodeName)) {

            nodeDraft = containerLoader.factory().newNodeDraft(nodeName);

            if (isDomainNode && !DOMAIN_COLOR_MAP.containsKey(nodeName) && !COLOR_ARRAY.isEmpty()) {
                DOMAIN_COLOR_MAP.put(nodeName, COLOR_ARRAY.remove(0));
            }

            if (DOMAIN_COLOR_MAP.containsKey(nodeName)) {
                nodeDraft.setColor(DOMAIN_COLOR_MAP.get(nodeName));
            } else {
                nodeDraft.setColor(Color.LIGHT_GRAY);
            }

            nodeDraft.setLabelVisible(true);
            nodeDraft.setLabel(nodeName);
            nodeDraft.setLabelColor(Color.BLACK);
            nodeDraft.setSize(15f);
            nodeDraft.setLabelSize(14f);

            containerLoader.addNode(nodeDraft);
        } else {

            nodeDraft = containerLoader.getNode(nodeName);
            nodeDraft.setSize(10f);

        }

        return nodeDraft;
    }

    private void addUrlDataToGraph(URI uri, ContainerLoader containerLoader) {

        NodeDraft sourceNode;
        NodeDraft destinationNode;
        EdgeDraft edgeDraft;

        String domain = uri.getHost();
        String path = uri.getPath();

        sourceNode = createOrGetNode(domain, containerLoader, true);

        for (String pathParam : path.split("/")) {

            if (pathParam.trim().length() == 0) {
                continue;
            }

            destinationNode = createOrGetNode(pathParam, containerLoader, false);

            if (!containerLoader.edgeExists(sourceNode.getId(), destinationNode.getId())) {

                edgeDraft = containerLoader.factory().newEdgeDraft(pathParam);

                edgeDraft.setSource(sourceNode);
                edgeDraft.setTarget(destinationNode);
                edgeDraft.setLabel(pathParam);
                edgeDraft.setLabelVisible(true);
                edgeDraft.setLabelColor(Color.BLUE);
                edgeDraft.setLabelSize(10f);
                edgeDraft.setColor(Color.BLACK);
                edgeDraft.setWeight(5f);

                containerLoader.addEdge(edgeDraft);
            }

            sourceNode = destinationNode;
        }

        if (importController == null) {
            importController = Lookup.getDefault().lookup(ImportController.class);
        }

        Processor processor = Lookup.getDefault().lookup(Processor.class);
        importController.process(container, processor, workspace);

        //Graph model
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        DirectedGraph graph = graphModel.getDirectedGraph();
        LoggerUtils.info("--> Current Nodes: " + graph.getNodeCount());
        LoggerUtils.info("--> Current Edges: " + graph.getEdgeCount());

        YifanHuLayout firstLayout = new YifanHuLayout(null, new StepDisplacement(1f));
        firstLayout.setGraphModel(graphModel);
        firstLayout.initAlgo();
        firstLayout.resetPropertiesValues();
        firstLayout.setOptimalDistance(200f);

        for (int index = 0; index < 100 && firstLayout.canAlgo(); index++) {
            firstLayout.goAlgo();
        }

        firstLayout.endAlgo();

        projectController.saveProject(project);

        LoggerUtils.info("Gephi project saved ---");
    }
}
