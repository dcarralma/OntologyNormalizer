package normalizers;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import launcher.Utils;

public class ABoxNormalizer {

	public static void normalizeClassAsss(final Set<OWLClassAssertionAxiom> classAsss,
			final Set<OWLSubClassOfAxiom> subClassOfAxioms) {
		final Set<OWLClassAssertionAxiom> classAssCopy = new HashSet<>();
		classAssCopy.addAll(classAsss);
		classAsss.clear();

		for (final OWLClassAssertionAxiom classAss : classAssCopy) {
			final OWLClassExpression classExpr = classAss.getClassExpression();
			if (classExpr.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				classAsss.add(classAss);
			else {
				// C(a) -> { X(a), X sqs C }
				subClassOfAxioms.add(
						Utils.factory.getOWLSubClassOfAxiom(Utils.getCorrespondingFreshClass(classExpr), classExpr));
				classAsss.add(Utils.factory.getOWLClassAssertionAxiom(Utils.getCorrespondingFreshClass(classExpr),
						classAss.getIndividual()));
			}
		}
	}

	public static void normalizeNegativeObjPropAsss(final Set<OWLNegativeObjectPropertyAssertionAxiom> negativeObjPropAssertions, final Set<OWLObjectPropertyAssertionAxiom> objPropAss,
		final Set<OWLDisjointObjectPropertiesAxiom> disjointPropAx) {
		for (final OWLNegativeObjectPropertyAssertionAxiom negativeObjPropAss : negativeObjPropAssertions) {
			// ~ R(a, b) -> { Rx(a, b), Disj(R, Rx) }
			final OWLObjectPropertyExpression objProp = negativeObjPropAss.getProperty();
			disjointPropAx.add(Utils.factory.getOWLDisjointObjectPropertiesAxiom(
					Utils.toSet(Utils.getCorrespondingFreshObjProp(objProp), objProp)));
			objPropAss.add(
					Utils.factory.getOWLObjectPropertyAssertionAxiom(Utils.getCorrespondingFreshObjProp(objProp),
							negativeObjPropAss.getSubject(), negativeObjPropAss.getObject()));
		}
	}

}
