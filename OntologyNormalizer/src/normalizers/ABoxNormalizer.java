package normalizers;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import launcher.Utils;

public final class ABoxNormalizer {

	private ABoxNormalizer() {
	}

	public static void normalizeClassAssertions(final Set<OWLClassAssertionAxiom> classAsssertionAxioms,
			final Set<OWLSubClassOfAxiom> subClassOfAxioms) {
		final Set<OWLClassAssertionAxiom> classAssertionsCopy = new HashSet<>();
		classAssertionsCopy.addAll(classAsssertionAxioms);
		classAsssertionAxioms.clear();

		for (final OWLClassAssertionAxiom classAssertion : classAssertionsCopy) {
			final OWLClassExpression classExpr = classAssertion.getClassExpression();
			if (classExpr.isOWLClass()) {
				classAsssertionAxioms.add(classAssertion);
			} else {
				// C(a) -> { X(a), X sqs C }
				final OWLClassExpression freshClass = Utils.getCorrespondingFreshClass(classExpr);
				classAsssertionAxioms
						.add(Utils.factory.getOWLClassAssertionAxiom(freshClass, classAssertion.getIndividual()));
				subClassOfAxioms.add(
						Utils.factory.getOWLSubClassOfAxiom(freshClass, classExpr));
			}
		}
	}

	public static void normalizeNegativeObjPropAsssertions(
			final Set<OWLNegativeObjectPropertyAssertionAxiom> negativeObjPropAssertions,
			final Set<OWLObjectPropertyAssertionAxiom> objPropAss,
			final Set<OWLDisjointObjectPropertiesAxiom> disjointPropAx) {
		for (final OWLNegativeObjectPropertyAssertionAxiom negativeObjPropAss : negativeObjPropAssertions) {
			// ~ R(a, b) -> { Rx(a, b), Disj(R, Rx) }
			final OWLObjectPropertyExpression objProp = negativeObjPropAss.getProperty();
			disjointPropAx.add(Utils.factory.getOWLDisjointObjectPropertiesAxiom(
					Utils.toSet(Utils.getCorrespondingFreshObjProp(objProp), objProp)));
			objPropAss.add(Utils.factory.getOWLObjectPropertyAssertionAxiom(Utils.getCorrespondingFreshObjProp(objProp),
					negativeObjPropAss.getSubject(), negativeObjPropAss.getObject()));
		}
	}

}
