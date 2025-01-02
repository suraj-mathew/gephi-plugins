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
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.utils.progress.ProgressTicket;
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
    ImportController importController = null;

    private MyProxyStarter proxyStarter = null;

    private boolean isProcessCancelled = false;

    static {
        loadColorArray();
    }

    @Override
    public void generate(ContainerLoader container) {

        LoggerUtils.info("----GENERATE-----");

        String currentHeadMessage = null;

        try {
            LoggerUtils.info("----11111111-----");
            container = null;// Doing the same way as that of a working plugin.
            LoggerUtils.info("----22222-----");
            workspace = Lookup.getDefault().lookup(ProjectController.class).getCurrentWorkspace();
            LoggerUtils.info("----333333333-----");
            workspace.getWorkspaceMetadata().setTitle("Mitm-Java-Proxy-workspace");
            LoggerUtils.info("----444444444-----");
            workspace.getProject().getProjectMetadata().setTitle("Mitm-Java-Proxy");
            LoggerUtils.info("----555555-----");

            progress.start();
            LoggerUtils.info("----6666666666-----");
            //start server
            proxyStarter = new MyProxyStarter();
            LoggerUtils.info("----777777-----");
            proxyStarter.startProxy();
            LoggerUtils.info("----8888888888-----");

            while (!isProcessCancelled) {

                currentHeadMessage = CommonUtils.getFirstElementFromMessageQueue();

                if (CommonUtils.isNotNull(currentHeadMessage)) {
                    try {
                        addUrlDataToGraph(new URI(currentHeadMessage));
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

    private void addUrlDataToGraph(URI uri) {
        
        NodeDraft sourceNode;
        NodeDraft destinationNode;
        EdgeDraft edgeDraft;

        Container container = null;
        ContainerLoader containerLoader = null;

        String domain = uri.getHost();
        String queryParams = uri.getQuery();
        String path = uri.getPath();

        container = Lookup.getDefault().lookup(Container.Factory.class).newContainer();
        container.setSource("Mitm-Java-Proxy-container");

        containerLoader = container.getLoader();
        containerLoader.setEdgesMergeStrategy(EdgeMergeStrategy.FIRST);
        containerLoader.setAllowAutoNode(false);

        if (!containerLoader.nodeExists(domain)) {
            LoggerUtils.info("NEW NODE NEEDED - " + domain);
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
            sourceNode.setSize(100f);

            containerLoader.addNode(sourceNode);
        } else {
            LoggerUtils.info("NODE EXIST - " + domain);
            sourceNode = containerLoader.getNode(domain);
        }

        for (String pathParam : path.split("/")) {

            if (pathParam.trim().length() == 0) {
                continue;
            }
            if (!containerLoader.nodeExists(pathParam)) {
                LoggerUtils.info("NEW DEST NODE FOR EDGE NEEDED - " + pathParam);
                destinationNode = containerLoader.factory().newNodeDraft(pathParam);
                destinationNode.setLabel(pathParam);
                destinationNode.setSize(100f);
                containerLoader.addNode(destinationNode);
            } else {
                LoggerUtils.info("NODE DEST FOR EDGE ALREADY EXISTS - " + pathParam);
                destinationNode = containerLoader.getNode(pathParam);
            }

            if (!containerLoader.edgeExists(sourceNode.getId(), destinationNode.getId())) {
                LoggerUtils.info("NEW EDGE NEEDED - " + sourceNode.getLabel() + "-" + destinationNode.getLabel());
                edgeDraft = containerLoader.factory().newEdgeDraft(pathParam);

                edgeDraft.setSource(sourceNode);
                edgeDraft.setTarget(destinationNode);
                edgeDraft.setLabel(sourceNode.getLabel() + "-" + destinationNode.getLabel());
                edgeDraft.setLabelVisible(true);

                containerLoader.addEdge(edgeDraft);
            }

            sourceNode = destinationNode;
        }

        container.verify();
        
        LoggerUtils.info("About to import ---");
        
        if (importController == null) {
            importController = Lookup.getDefault().lookup(ImportController.class);
        }

        Processor processor = Lookup.getDefault().lookup(Processor.class);
        
        LoggerUtils.info("About to process ---");
        
        importController.process(container,processor,workspace);
        
        LoggerUtils.info("Done ---");
    }
}
