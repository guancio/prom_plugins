package org.processmining.plugins.bpmn.exporting;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


import javax.swing.JComponent;
import javax.swing.JFileChooser;

import javax.swing.JOptionPane;
import javax.swing.JPanel;



import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;


import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramExt;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.plugins.petrinet.replayfitness.performance.LegendPerfomancePanel;
import org.processmining.plugins.petrinet.replayfitness.performance.PerformanceResult;
import org.processmining.plugins.petrinet.replayfitness.performance.TotalPerformanceResult;
import org.processmining.plugins.petrinet.replayfitness.util.LogViewInteractivePanel;
import org.processmining.plugins.petrinet.replayfitness.util.PetriNetDrawUtil;
import org.processmining.plugins.xpdl.Xpdl;
import org.processmining.plugins.xpdl.converter.BPMN2XPDLConversionExt;



public class BPMNexportPanelPerformance extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3531962030544060794L;
	
	private  ProMJGraphPanel netPNView;
	private  JComponent netBPMNView;
	private BPMNDiagramExt bpmnvisulizated;
	

	private LegendPerfomancePanel legendInteractionPanel;

	private LogViewInteractivePanel logInteractionPanel;

	private UIPluginContext context;

	private Petrinet net;

	private BPMNexportResult export;

	private TabTracePerfPanel tabinteractivepanel;



	public BPMNexportPanelPerformance(UIPluginContext c, Petrinet n,
			XLog log, Progress progress, BPMNexportResult e) {

		context=c;
		net=n;
		export=e;
		init(log);


	}

	

	private void init(XLog log) {
TotalPerformanceResult tovisualize = export.getTotalPerformanceresult();
		
		Petrinet netx = PetrinetFactory.clonePetrinet(net);
		PetriNetDrawUtil.drawperformancenet(netx, tovisualize.getListperformance().get(0).getList(), tovisualize.getListperformance().get(0).getMaparc());
		
		netPNView = ProMJGraphVisualizer.instance().visualizeGraph(context, netx);

		legendInteractionPanel = new LegendPerfomancePanel(netPNView, "Legend");
		netPNView.addViewInteractionPanel(legendInteractionPanel, SwingConstants.NORTH);

		bpmnvisulizated= export.getBPMNtraslate();
		netBPMNView= ProMJGraphVisualizer.instance().visualizeGraph(context, bpmnvisulizated);

		//JComponent logView = new LogViewUI(log);
		
		logInteractionPanel = new LogViewInteractivePanel(netPNView, log);
		netPNView.addViewInteractionPanel(logInteractionPanel, SwingConstants.SOUTH);
		
		tabinteractivepanel = new TabTracePerfPanel(netPNView, "Trace_Sel", tovisualize, this);
		netPNView.addViewInteractionPanel(tabinteractivepanel, SwingConstants.SOUTH);
		
		double size[][] = { { TableLayoutConstants.FILL }, { TableLayoutConstants.FILL,  TableLayoutConstants.FILL} };
		setLayout(new TableLayout(size));
		
		add(netBPMNView, "0, 0");
		
		add(netPNView, "0, 1");

		
	}



	


	public void savefile() {
		// TODO Auto-generated method stub
		JFileChooser saveDialog = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
		        "XPDL", "xpdl");
		saveDialog.setFileFilter(filter);

		saveDialog.setSelectedFile(new File(bpmnvisulizated.getLabel()+"Summary.xpdl")); 
		if (saveDialog.showSaveDialog(context.getGlobalContext().getUI()) == JFileChooser.APPROVE_OPTION) {
			File outFile = saveDialog.getSelectedFile();
			try {
				BufferedWriter outWriter = new BufferedWriter(new FileWriter(outFile));
				Xpdl newxpdl=null;
				try {
					BPMN2XPDLConversionExt xpdlConversion = new BPMN2XPDLConversionExt(bpmnvisulizated);
					newxpdl = xpdlConversion.fills_layout(context);
				
				} catch (Exception e1) {
					
					//e1.printStackTrace();
					
				}
				outWriter.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +newxpdl.exportElement());
				outWriter.flush();
				outWriter.close();
				JOptionPane.showMessageDialog(context.getGlobalContext().getUI(),
						"BPMN has been saved\nto XPDL file!", "BPMN saved.",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}



	public void repainone(PerformanceResult performanceResult) {
		// TODO Auto-generated method stub
		Petrinet netx = PetrinetFactory.clonePetrinet(net);
		
		PetriNetDrawUtil.drawperformancenet(netx, performanceResult.getList(), performanceResult.getMaparc());
		bpmnvisulizated =	BPMNexportUtil.exportPerformancetoBPMN(export.getTraslateBpmnresult(), performanceResult.getList(), performanceResult.getMaparc());
		
		remove(netPNView);
		remove(netBPMNView);
		netPNView = ProMJGraphVisualizer.instance().visualizeGraph(context, netx);
		netPNView.addViewInteractionPanel(legendInteractionPanel, SwingConstants.NORTH);
		netPNView.addViewInteractionPanel(logInteractionPanel, SwingConstants.SOUTH);
		netPNView.addViewInteractionPanel(tabinteractivepanel, SwingConstants.SOUTH);
		
		netBPMNView = ProMJGraphVisualizer.instance().visualizeGraph(context, bpmnvisulizated);
		//add (netPNView, "1, 5, 5, 5");
		//add (netBPMNView, "1, 1, 5, 1");
		add(netBPMNView, "0, 0");
		
		add(netPNView, "0, 1");
		
		revalidate();
		repaint();
	}

}
