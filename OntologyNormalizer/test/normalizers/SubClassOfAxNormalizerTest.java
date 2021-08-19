package normalizers;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import launcher.Utils;

public class SubClassOfAxNormalizerTest {

	@Test
	public void testSubclassOfSomeValuesFromOneOf() {
		final Set<OWLSubClassOfAxiom> subClassOfAxioms = new HashSet<OWLSubClassOfAxiom>();
		final OWLNamedIndividual namedIndividual = Utils.factory.getOWLNamedIndividual("i");
		final OWLSubClassOfAxiom subclassOfSomeValuesFromOneOf = Utils.factory
				.getOWLSubClassOfAxiom(Utils.factory.getOWLClass("A"), Utils.factory.getOWLObjectSomeValuesFrom(
						Utils.factory.getOWLObjectProperty("r"), Utils.factory.getOWLObjectOneOf(namedIndividual)));
		subClassOfAxioms.add(subclassOfSomeValuesFromOneOf);
		final Set<OWLClassAssertionAxiom> classAssertionAxioms = new HashSet<OWLClassAssertionAxiom>();
		SubClassOfAxNormalizer.normalizeSubClassOfAx(subClassOfAxioms, classAssertionAxioms);

		assertEquals(2, subClassOfAxioms.size());
	}

}
