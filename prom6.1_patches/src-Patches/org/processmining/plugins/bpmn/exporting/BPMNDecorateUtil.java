package org.processmining.plugins.bpmn.exporting;

import java.awt.Color;

import java.util.Collection;
import java.util.HashMap;

import java.util.Map;
import java.util.Vector;


import org.processmining.models.graphbased.AttributeMap;

import org.processmining.models.graphbased.directed.ContainingDirectedGraphNode;

import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramExt;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramExtFactory;

import org.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.processmining.models.graphbased.directed.bpmn.elements.Artifacts;
import org.processmining.models.graphbased.directed.bpmn.elements.Flow;

import org.processmining.models.graphbased.directed.bpmn.elements.Artifacts.ArtifactType;
import org.processmining.models.graphbased.directed.bpmn.elements.SubProcess;
import org.processmining.models.graphbased.directed.bpmn.elements.Swimlane;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import org.processmining.models.semantics.petrinet.Marking;

import org.processmining.plugins.petrinet.replayfitness.conformance.ConformanceResult;

import org.processmining.plugins.petrinet.replayfitness.performance.PerformanceData;
import org.processmining.plugins.petrinet.replayfitness.performance.PerformanceResult;


public class BPMNDecorateUtil {

	public BPMNDecorateUtil() {

	}

	public static BPMNDiagramExt exportPerformancetoBPMN(
			BPMNDiagram bpmnoriginal,
			PerformanceResult Performanceresult, Map<String, Place> MapArc2Place,
			Petrinet net) {

		// clona bpmn
		BPMNDiagramExt bpmn =BPMNDiagramExtFactory.cloneBPMNDiagram(bpmnoriginal);

		Map<Arc, Integer> maparc = Performanceresult.getMaparc();

		Map<String, Integer> ArchiAttivatiBPMN = new HashMap<String, Integer>();
		Map<String, String> archibpmnwithsyncperformance = new HashMap<String, String>();

		

		// ogni piazza che attraversiamo in performance result conta i token
		// passati sulla pizza
		/*
		 * for(Place p : Performanceresult.keySet()){
		 * 
		 * if(MapArc2Place.containsKey(p.getLabel())){
		 * 
		 * PerformanceData rs= Performanceresult.get(p); int i =
		 * rs.getTokenCount(); ArchiAttivatiBPMN.put(p.getLabel(), i); } }
		 */

		for (Place p : MapArc2Place.values()) {

			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges = p
			.getGraph().getOutEdges(p);
			int count = 0;
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : edges) {
				Arc a = (Arc) edge;
				if (maparc.containsKey(a)) {
					Integer i = maparc.get(a);
					count += i;
				}
			}

			ArchiAttivatiBPMN.put(p.getLabel(), count);

		}

		Map<Activity, String> MapActivity = new HashMap<Activity, String>();

		for (Transition t : net.getTransitions()) {
			if (!t.isInvisible()) {
				String tname = t.getLabel();
				String name = (String) tname.subSequence(0, tname.indexOf("+"));
				Activity activity = null;
				// cerco l'attività bpmn a cui collegare l'artifacts
				for (Activity a : bpmn.getActivities()) {
					if (a.getLabel().equals(name)) {
						activity = a;
						break;
					}
				}
				Place preplace = (Place) t.getGraph().getInEdges(t).iterator()
				.next().getSource();
				// Place postplace = (Place)
				// t.getGraph().getOutEdges(t).iterator().next().getTarget();
				String text = "";
				PerformanceData ps = getPerfResult(preplace, Performanceresult.getList());
				if (ps != null) {
					if (t.getLabel().endsWith("start")) {
						if (ps.getWaitTime() > 0) {
							text = "Activation Time: " + ps.getWaitTime()
							+ "<br/>";
						}
					} else if (t.getLabel().endsWith("complete")) {
						if (ps.getWaitTime() > 0) {
							text = "Execution Time: " + ps.getWaitTime()
							+ "<br/>";

						}
					}
					if (MapActivity.containsKey(activity)) {
						text += MapActivity.get(activity);
					}
					MapActivity.put(activity, text);
				}
			} else {
				if (t.getLabel().endsWith("_join")) {

					// controlla la presenza di sync time e inserisce il souj
					// per ogni ramo parallelo
					addsoujandsynctime(Performanceresult.getList(), t,
							archibpmnwithsyncperformance);
				}
			}

		}

		for (Activity a : MapActivity.keySet()) {
			String text = MapActivity.get(a);
			String label = "<html>" + text + "</html>";
			ContainingDirectedGraphNode parent = a.getParent();
			Artifacts art = null;
			if (parent instanceof Swimlane) {
				art = bpmn.addArtifacts(label, ArtifactType.TEXTANNOATION,
						a.getParentSwimlane());
				bpmn.addFlowAssociation(art, a,a.getParentSwimlane());
			}
			if (parent instanceof SubProcess) {
				art = bpmn.addArtifacts(label, ArtifactType.TEXTANNOATION,
						a.getParentSubProcess());
				bpmn.addFlowAssociation(art, a,a.getParentSubProcess());
			}
			if (parent == null) {
				art = bpmn.addArtifacts(label, ArtifactType.TEXTANNOATION);
				bpmn.addFlowAssociation(art, a);
			}


		}

		// i sync time sono sempre sulle piazze "arco", quindi cerco l'arco a
		// cui si riferisco i place con sync time ed aggiungo
		// il tooltip all'arco e lo coloro di rosso.
		for (Flow f : bpmn.getFlows()) {
			String from = f.getSource().getLabel();
			String to = f.getTarget().getLabel();
			if (archibpmnwithsyncperformance.containsKey(from + to)) {
				String flowsync = archibpmnwithsyncperformance.get(from + to);
				f.getAttributeMap().remove(AttributeMap.TOOLTIP);

				f.getAttributeMap().put(AttributeMap.TOOLTIP, flowsync);
				f.getAttributeMap().remove(AttributeMap.SHOWLABEL);
				f.getAttributeMap().put(AttributeMap.SHOWLABEL, false);
				f.getAttributeMap().put(AttributeMap.EDGECOLOR, Color.RED);

			}
			if (ArchiAttivatiBPMN.containsKey(from + to)) {
				Integer i = ArchiAttivatiBPMN.get(from + to);
				f.getAttributeMap().remove(AttributeMap.SHOWLABEL);
				f.getAttributeMap().put(AttributeMap.SHOWLABEL, true);
				f.getAttributeMap().put(AttributeMap.LABEL, i.toString());

			}
		}

		return bpmn;
	}

	private static void addsoujandsynctime(
			Map<Place, PerformanceData> performanceresult, Transition t,
			Map<String, String> archibpmnwithsyncperformance) {

		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inflows = t
		.getGraph().getInEdges(t);
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : inflows) {
			Place source = (Place) edge.getSource();
			PerformanceData rs = performanceresult.get(source);
			if (rs.getSynchTime() > 0) {
				String sourjtime = calcolasojourntime(source, performanceresult);
				archibpmnwithsyncperformance.put(source.getLabel(),
						"Sync time: " + String.valueOf(rs.getSynchTime())
						+ "\n Souj time: " + sourjtime);
			} else {
				if (rs.getTime() >= 0) {
					String sourjtime = calcolasojourntime(source,
							performanceresult);
					archibpmnwithsyncperformance.put(source.getLabel(),
							"Souj time: " + sourjtime);
				}

			}

		}

	}

	private static String calcolasojourntime(PetrinetNode p,
			Map<Place, PerformanceData> performanceresult) {

		myFloat soujour = new myFloat();

		recursiveaddsoujourtime(soujour, performanceresult, p, 0);

		return String.valueOf(soujour.getFlo());
	}

	private static void recursiveaddsoujourtime(myFloat soujour,
			Map<Place, PerformanceData> performanceresult, PetrinetNode p, int i) {

		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> flow : p
				.getGraph().getInEdges(p)) {
			PetrinetNode sourcenode = flow.getSource();
			if (sourcenode instanceof Transition) {
				Transition source = (Transition) flow.getSource();
				if (source.getLabel().endsWith("_join")) {
					// prendo solo un ramo ora devo non mi devo fermare al primo
					// fork che incontro ma al successivo
					PetrinetNode newsource = source.getGraph()
					.getInEdges(source).iterator().next().getSource();
					recursiveaddsoujourtime(soujour, performanceresult,
							newsource, i++);
				} else {
					if (!source.getLabel().endsWith("_fork")) {
						recursiveaddsoujourtime(soujour, performanceresult,
								source, i);
					} else {
						// se i è maggiore di 0 significa che sto calcolando un
						// tempo di soggiorno del branch che contiente almeno
						// un altro branch parallelo nel suo interno manca
						// ancora per i cicli
						if (i > 0) {
							recursiveaddsoujourtime(soujour, performanceresult,
									source, i--);
						}

					}

				}
			} else if (sourcenode instanceof Place) {
				Place source = (Place) flow.getSource();
				PerformanceData ps = performanceresult.get(source);
				if (ps != null) {
					if (ps.getTime() > 0) {
						soujour.add(ps.getTime());
					}
					recursiveaddsoujourtime(soujour, performanceresult, source,
							i);
				}

			}

		}

	}

	private static PerformanceData getPerfResult(Place preplace,
			Map<Place, PerformanceData> performanceresult) {
		for (Place p : performanceresult.keySet()) {
			if (p.getLabel().equals(preplace.getLabel())) {
				return performanceresult.get(p);
			}
		}

		return null;
	}

	public static BPMNDiagramExt exportConformancetoBPMN(
			BPMNDiagram bpmnoriginal, Petrinet net, ConformanceResult conformanceresult, Map<String, Place> MapArc2Place) {





		// clona bpmn
		BPMNDiagramExt bpmn = BPMNDiagramExtFactory.cloneBPMNDiagram(bpmnoriginal);



		Marking remaning = conformanceresult.getRemainingMarking();
		Marking missing = conformanceresult.getMissingMarking();
		//Map<Transition, Integer> transnotfit = conformanceresult.getMapTransition();
		Map<Arc, Integer> attivazionearchi = conformanceresult.getMapArc();

		Map<String, Integer> ArchiAttivatiBPMN = new HashMap<String, Integer>();
		Map<String, String> archibpmnwitherrorconformance = new HashMap<String, String>();

		// gli archi che attivo sul bpmn sono gli archi uscenti delle piazze
		// "arco"
		for (Place p : MapArc2Place.values()) {
			int att = 0;
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> egde : p
					.getGraph().getOutEdges(p)) {
				if (attivazionearchi.containsKey(egde)) {
					att += attivazionearchi.get(egde);

				}

			}
			ArchiAttivatiBPMN.put(p.getLabel(), att);

		}

		Map<Activity,Artifacts> mapActiArtic = new HashMap<Activity, Artifacts>();
		// transizioni che nn fittano
		String ret = "<br/>";
		for (Transition t : net.getTransitions()) {
			if (!t.isInvisible()) {
				String tname = t.getLabel();
				String name = (String) tname.subSequence(0, tname.indexOf("+"));
				
				Activity activity = null;
				// cerco l'attività bpmn a cui collegare l'artifacts
				for (Activity a : bpmn.getActivities()) {
					if (a.getLabel().equals(name)) {
						activity = a;
						break;
					}
				}
				String unsoundallert = "";
				for (Place p : remaning.baseSet()) {
					if (p.getLabel().equals(name)) {
						unsoundallert += ret + " Task missing competition\n";
					} else if (p.getLabel().startsWith(name) && !tname.endsWith("start") ) {
						unsoundallert += ret + " Task interrupted executions\n";
					}
				}
				for (Place p : missing.baseSet()) {
					if (p.getLabel().equals(name)) {
						unsoundallert += ret + " Task internal failures";
					}
					if(p.getLabel().endsWith(name)&& tname.endsWith("start")){
						unsoundallert += ret + " Task unsound executions\n";
					}
				}
				if (activity != null && unsoundallert!="") {
					
					
						String label = "<html>"+ unsoundallert + "<html>";
					if(!mapActiArtic.containsKey(activity)){

						
						Artifacts art = null;
						if (activity.getParent() == null) {
							art = bpmn.addArtifacts(label,
									ArtifactType.TEXTANNOATION);
							bpmn.addFlowAssociation(art, activity);

						} else {
							if (activity.getParent() instanceof SubProcess) {
								art = bpmn.addArtifacts(label,
										ArtifactType.TEXTANNOATION,
										activity.getParentSubProcess());
								bpmn.addFlowAssociation(art, activity,activity.getParentSubProcess());	
							} else {
								if (activity.getParent() instanceof Swimlane) {
									art = bpmn.addArtifacts(label,
											ArtifactType.TEXTANNOATION,
											activity.getParentSwimlane());
									bpmn.addFlowAssociation(art, activity,activity.getParentSwimlane());
								}
							}
						}

						mapActiArtic.put(activity, art);
					}else{
						Artifacts art = mapActiArtic.get(activity);
						label+=art.getLabel();
						art.getAttributeMap().remove(AttributeMap.LABEL);
						art.getAttributeMap().put(AttributeMap.LABEL, label);
						

					}

				}

			}

		
			// cerco la transizione del fork
			if (t.getLabel().endsWith("_fork")) {
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> p = t
				.getGraph().getOutEdges(t);
				Vector<String> targ = new Vector<String>();
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : p) {
					Place target = (Place) e.getTarget();
					targ.add(target.getLabel());
				}
				for (String placename : targ) {
					for (Place place : remaning.baseSet()) {
						if (place.getLabel().equals(placename)) {
							System.out.println(ret + " Fork internal failures");
							archibpmnwitherrorconformance.put(place.getLabel(),
							" Fork internal failures");
						}

					}
				}
			}
		}
		// metto gli attraversamenti sugli archi bpmn
		for (Flow f : bpmn.getFlows()) {
			String from = f.getSource().getLabel();
			String to = f.getTarget().getLabel();
			if (ArchiAttivatiBPMN.containsKey(from + to)) {
				Integer i = ArchiAttivatiBPMN.get(from + to);
				if (i > 0) {
					f.getAttributeMap().put(AttributeMap.LABEL, i.toString());
					f.getAttributeMap().put(AttributeMap.TOOLTIP, i.toString());
					f.getAttributeMap().put(AttributeMap.SHOWLABEL, true);
				}

			}
			// metto eventuali errore sul arco di fork
			if (archibpmnwitherrorconformance.containsKey(from + to)) {

				String flowerr = archibpmnwitherrorconformance.get(from + to);
				f.getAttributeMap().remove(AttributeMap.TOOLTIP);

				f.getAttributeMap().put(AttributeMap.TOOLTIP, flowerr);
				f.getAttributeMap().remove(AttributeMap.SHOWLABEL);
				f.getAttributeMap().put(AttributeMap.SHOWLABEL, true);
				f.getAttributeMap().put(AttributeMap.EDGECOLOR, Color.RED);

			}
		}

		return bpmn;

	}

	
}

class myFloat {

	float flo = 0;

	public float getFlo() {
		return flo;
	}

	public void setFlo(float flo) {
		this.flo = flo;
	}

	public void add(float a) {
		flo += a;
	}

	myFloat() {
		flo = 0;
	}

}
