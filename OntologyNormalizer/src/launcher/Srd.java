package launcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

public class Srd {

	public static OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
	private static Map<OWLClassExpression, OWLClassExpression> freshClassMap = new HashMap<OWLClassExpression, OWLClassExpression>();
	private static Map<OWLObjectPropertyExpression, OWLObjectPropertyExpression> freshObjPropMap = new HashMap<OWLObjectPropertyExpression, OWLObjectPropertyExpression>();

	// Helpers

	public static <T> List<T> toList(T element1, T element2) {
		List<T> newSet = new ArrayList<T>();
		newSet.add(element1);
		newSet.add(element2);
		return newSet;
	}

	public static <T> Set<T> toSet(T element1) {
		Set<T> newSet = new HashSet<T>();
		newSet.add(element1);
		return newSet;
	}

	public static <T> Set<T> toSet(T element1, T element2) {
		Set<T> newSet = new HashSet<T>();
		newSet.add(element1);
		newSet.add(element2);
		return newSet;
	}

	public static <T> Set<T> toSet(T element1, T element2, T element3) {
		Set<T> newSet = new HashSet<T>();
		newSet.add(element1);
		newSet.add(element2);
		newSet.add(element3);
		return newSet;
	}

	// Retrieving fresh classes

	public static OWLClassExpression getCorrespondingFreshClass(OWLClassExpression classExpression) {
		freshClassMap.putIfAbsent(classExpression, factory.getOWLClass(IRI.create("http://FreshClass" + freshClassMap.size())));
		return freshClassMap.get(classExpression);
	}

	public static OWLObjectPropertyExpression getCorrespondingFreshObjProp(OWLObjectPropertyExpression objPropExp) {
		freshObjPropMap.putIfAbsent(objPropExp, factory.getOWLObjectProperty(IRI.create("http://FreshObjProp" + freshObjPropMap.size())));
		return freshObjPropMap.get(objPropExp);
	}

}
