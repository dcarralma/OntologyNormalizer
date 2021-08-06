package launcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class Utils {
	public static OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
	public static OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
	private static Map<OWLClassExpression, OWLClassExpression> freshClassMap = new HashMap<OWLClassExpression, OWLClassExpression>();
	private static Map<OWLObjectPropertyExpression, OWLObjectPropertyExpression> freshObjPropMap = new HashMap<OWLObjectPropertyExpression, OWLObjectPropertyExpression>();


	public static <T> Set<T> toSet(final T... elements) {
		final Set<T> newSet = new HashSet<T>();
		for (final T element : elements) {

			newSet.add(element);
		}
		return newSet;
	}

	// Retrieving fresh classes

	public static OWLClassExpression getCorrespondingFreshClass(final OWLClassExpression classExpression) {
		freshClassMap.putIfAbsent(classExpression,
				factory.getOWLClass(IRI.create("http://FreshClass" + UUID.randomUUID().getLeastSignificantBits())));
		return freshClassMap.get(classExpression);
	}

	public static OWLObjectPropertyExpression getCorrespondingFreshObjProp(
			final OWLObjectPropertyExpression objPropExp) {
		freshObjPropMap.putIfAbsent(objPropExp, factory
				.getOWLObjectProperty(IRI.create("http://FreshObjProp" + UUID.randomUUID().getLeastSignificantBits())));
		return freshObjPropMap.get(objPropExp);
	}

}
