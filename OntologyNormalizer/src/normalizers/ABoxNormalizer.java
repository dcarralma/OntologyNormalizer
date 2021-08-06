package normalizers;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import launcher.Utils;
import uk.ac.manchester.cs.owl.owlapi.OWLClassAssertionAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDisjointObjectPropertiesAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyAssertionAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

public class ABoxNormalizer {

	public static void normalizeClassAsss(Set<OWLClassAssertionAxiom> classAsss, Set<OWLSubClassOfAxiom> subClassOfAxioms) {
		Set<OWLClassAssertionAxiom> classAssCopy = new HashSet<OWLClassAssertionAxiom>();
		classAssCopy.addAll(classAsss);
		classAsss.clear();

		for (OWLClassAssertionAxiom classAss : classAssCopy) {
			OWLClassExpression classExpr = classAss.getClassExpression();
			if (classExpr.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				classAsss.add(classAss);
			else {
				// C(a) -> { X(a), X sqs C }
				subClassOfAxioms.add(new OWLSubClassOfAxiomImpl(Utils.getCorrespondingFreshClass(classExpr), classExpr, new HashSet<OWLAnnotation>()));
				classAsss.add(new OWLClassAssertionAxiomImpl(classAss.getIndividual(), Utils.getCorrespondingFreshClass(classExpr), new HashSet<OWLAnnotation>()));
			}
		}
	}

	public static void normalizeNegativeObjPropAsss(Set<OWLNegativeObjectPropertyAssertionAxiom> negativeObjPropAssertions, Set<OWLObjectPropertyAssertionAxiom> objPropAss,
		Set<OWLDisjointObjectPropertiesAxiom> disjointPropAx) {
		for (OWLNegativeObjectPropertyAssertionAxiom negativeObjPropAss : negativeObjPropAssertions) {
			// ~ R(a, b) -> { Rx(a, b), Disj(R, Rx) }
			OWLObjectPropertyExpression objProp = negativeObjPropAss.getProperty();
			disjointPropAx.add(new OWLDisjointObjectPropertiesAxiomImpl(Utils.toSet(Utils.getCorrespondingFreshObjProp(objProp), objProp), new HashSet<OWLAnnotation>()));
			objPropAss.add(
				new OWLObjectPropertyAssertionAxiomImpl(negativeObjPropAss.getSubject(), Utils.getCorrespondingFreshObjProp(objProp), negativeObjPropAss.getObject(), new HashSet<OWLAnnotation>()));
		}
	}

}
