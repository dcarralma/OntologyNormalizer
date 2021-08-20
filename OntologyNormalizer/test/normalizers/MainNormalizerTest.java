package normalizers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;

import launcher.Utils;
import uk.ac.manchester.cs.owl.owlapi.InternalizedEntities;

public class MainNormalizerTest {

	@Test
	public void testNormalizeEquivalentObjectProperties() {

		final OWLObjectProperty p = Utils.factory.getOWLObjectProperty("p");
		final OWLObjectProperty q = Utils.factory.getOWLObjectProperty("q");
		final OWLObjectProperty r = Utils.factory.getOWLObjectProperty("r");
		final OWLEquivalentObjectPropertiesAxiom owlEquivalentObjectPropertiesAxiom = Utils.factory
				.getOWLEquivalentObjectPropertiesAxiom(p, q, r, p.getInverseProperty());

		final Set<OWLSubObjectPropertyOfAxiom> expectedSubObjPropAxioms = new HashSet<>();
		expectedSubObjPropAxioms.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(p, q));
		expectedSubObjPropAxioms.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(p, r));
		expectedSubObjPropAxioms.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(p, p.getInverseProperty()));
		expectedSubObjPropAxioms.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(q, p));
		expectedSubObjPropAxioms.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(q, r));
		expectedSubObjPropAxioms.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(q, p.getInverseProperty()));
		expectedSubObjPropAxioms.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(r, p));
		expectedSubObjPropAxioms.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(r, q));
		expectedSubObjPropAxioms.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(r, p.getInverseProperty()));
		expectedSubObjPropAxioms.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(p.getInverseProperty(), p));
		expectedSubObjPropAxioms.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(p.getInverseProperty(), q));
		expectedSubObjPropAxioms.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(p.getInverseProperty(), r));

		final Set<OWLSubObjectPropertyOfAxiom> subObjPropertiesAxioms = MainNormalizer
				.equivalentObjPropertiesAxiomToSubObjPropertiesAxioms(owlEquivalentObjectPropertiesAxiom);

		assertEquals(expectedSubObjPropAxioms, subObjPropertiesAxioms);
	}

	@Test
	public void testNormalizeEquivalentClasses() {
		final OWLClass a = Utils.factory.getOWLClass("a");
		final OWLClass b = Utils.factory.getOWLClass("b");
		final OWLClass c = Utils.factory.getOWLClass("c");
		final OWLEquivalentClassesAxiom equivalentClassesAxiom = Utils.factory.getOWLEquivalentClassesAxiom(a, b, c);

		final Set<OWLSubClassOfAxiom> expectedSubClassOfAxioms = new HashSet<>();
		expectedSubClassOfAxioms.add(Utils.factory.getOWLSubClassOfAxiom(a, b));
		expectedSubClassOfAxioms.add(Utils.factory.getOWLSubClassOfAxiom(a, c));
		expectedSubClassOfAxioms.add(Utils.factory.getOWLSubClassOfAxiom(b, a));
		expectedSubClassOfAxioms.add(Utils.factory.getOWLSubClassOfAxiom(b, c));
		expectedSubClassOfAxioms.add(Utils.factory.getOWLSubClassOfAxiom(c, a));
		expectedSubClassOfAxioms.add(Utils.factory.getOWLSubClassOfAxiom(c, b));

		final Set<OWLSubClassOfAxiom> subClassOfAxioms = MainNormalizer
				.equivalentClassesAxiomToSubClassOfAxioms(equivalentClassesAxiom);

		assertEquals(expectedSubClassOfAxioms, subClassOfAxioms);
	}

	@Test
	public void testNormalizedDisjointClasses() {
		final OWLClass a = Utils.factory.getOWLClass("a");
		final OWLClass b = Utils.factory.getOWLClass("b");
		final OWLClass c = Utils.factory.getOWLClass("c");
		
		final OWLDisjointClassesAxiom disjointClassesAxiom= Utils.factory.getOWLDisjointClassesAxiom(a, b, c);

		final Set<OWLSubClassOfAxiom> expectedSubClassOfAxioms = new HashSet<>();
		expectedSubClassOfAxioms.add(Utils.factory.getOWLSubClassOfAxiom(Utils.factory.getOWLObjectIntersectionOf(a, b),
				InternalizedEntities.OWL_NOTHING));
		expectedSubClassOfAxioms.add(Utils.factory.getOWLSubClassOfAxiom(Utils.factory.getOWLObjectIntersectionOf(a, c),
				InternalizedEntities.OWL_NOTHING));
		expectedSubClassOfAxioms.add(Utils.factory.getOWLSubClassOfAxiom(Utils.factory.getOWLObjectIntersectionOf(b, c),
				InternalizedEntities.OWL_NOTHING));


		final Set<OWLSubClassOfAxiom> subClassOfAxioms = MainNormalizer
				.disjointClassesAxiomToSubClassOfAxioms(disjointClassesAxiom);
		assertEquals(expectedSubClassOfAxioms, subClassOfAxioms);

		assertEquals(Utils.factory.getOWLObjectIntersectionOf(a, b), Utils.factory.getOWLObjectIntersectionOf(b, a));
	}

}
