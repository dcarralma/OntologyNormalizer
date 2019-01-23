package normalizers;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import launcher.Srd;
import uk.ac.manchester.cs.owl.owlapi.OWLClassAssertionAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectAllValuesFromImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectIntersectionOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectMaxCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectMinCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectOneOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectSomeValuesFromImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectUnionOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

public class SubClassOfAxNormalizer {

	protected static void normalizeSubClassOfAx(Set<OWLSubClassOfAxiom> subClassOfAxioms, Set<OWLClassAssertionAxiom> classAssertionAxioms) {

		Set<OWLSubClassOfAxiom> newIterationAxioms = new HashSet<OWLSubClassOfAxiom>();
		newIterationAxioms.addAll(subClassOfAxioms);
		boolean modified = true;

		while (modified) {
			modified = false;

			for (OWLSubClassOfAxiom subClassOfAxiom : subClassOfAxioms) {
				Set<OWLSubClassOfAxiom> nfRuleAxioms = applySomeNFRule(subClassOfAxiom);
				newIterationAxioms.addAll(nfRuleAxioms);
				if (modified) {

				}
				if (!Srd.toSet(subClassOfAxiom).equals(nfRuleAxioms)) {
					modified = true;
				}
			}

			subClassOfAxioms.clear();
			subClassOfAxioms.addAll(newIterationAxioms);
			newIterationAxioms.clear();
		}

		// {a1} sqcup ... sqcup {an} sqsubseteq D -> { D(a1), ..., D(an) }
		Set<OWLSubClassOfAxiom> copySubClassOfAxioms = new HashSet<OWLSubClassOfAxiom>();
		copySubClassOfAxioms.addAll(subClassOfAxioms);
		for (OWLSubClassOfAxiom normalizedSubClassOfAxiom : copySubClassOfAxioms) {
			OWLClassExpression subClass = normalizedSubClassOfAxiom.getSubClass();
			if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_ONE_OF)) {
				for (OWLIndividual individual : ((OWLObjectOneOf) subClass).individuals().collect(Collectors.toSet()))
					classAssertionAxioms.add(new OWLClassAssertionAxiomImpl(individual, normalizedSubClassOfAxiom.getSuperClass(), new HashSet<OWLAnnotation>()));
				subClassOfAxioms.remove(normalizedSubClassOfAxiom);
			}
		}
	}

	private static Set<OWLSubClassOfAxiom> applySomeNFRule(OWLSubClassOfAxiom subClassOfAxiom) {
		OWLClassExpression subClass = subClassOfAxiom.getSubClass();
		OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
		// OWLClassExpression freshClass = Srd.factory.getOWLClass(IRI.create(newConceptPref + Integer.toString(++newEntCounter)));

		// Transform to NNF
		if (!subClass.getNNF().equals(subClass))
			return Srd.toSet(new OWLSubClassOfAxiomImpl(subClass.getNNF(), superClass, new HashSet<OWLAnnotation>()));

		if (!superClass.getNNF().equals(superClass))
			return Srd.toSet(new OWLSubClassOfAxiomImpl(subClass, superClass.getNNF(), new HashSet<OWLAnnotation>()));

		// Split Axioms

		// C sqs D -> { C sqs X, X sqs D }
		if (!subClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS) && !superClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {
			OWLClassExpression subClassFreshClass = Srd.getCorrespondingFreshClass(subClass);
			return Srd.toSet(new OWLSubClassOfAxiomImpl(subClass, subClassFreshClass, new HashSet<OWLAnnotation>()),
					new OWLSubClassOfAxiomImpl(subClassFreshClass, superClass, new HashSet<OWLAnnotation>()));
		}

		// Normalize SubClass

		// A1 cap ... cap C cap ... cap An sqs D -> { C sqs X, A1 cap ... cap X cap ... cap An sqs D }
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF))
			if (superClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {
				Set<OWLClassExpression> subClassConjuncts = subClass.asConjunctSet();
				OWLClassExpression nonClassNameClassExpr = retrieveNonOWLClassClassExpression(subClassConjuncts);
				if (nonClassNameClassExpr != null) {
					subClassConjuncts.remove(nonClassNameClassExpr);
					subClassConjuncts.add(Srd.getCorrespondingFreshClass(nonClassNameClassExpr));
					return Srd.toSet(new OWLSubClassOfAxiomImpl(nonClassNameClassExpr, Srd.getCorrespondingFreshClass(nonClassNameClassExpr), new HashSet<OWLAnnotation>()),
							new OWLSubClassOfAxiomImpl(new OWLObjectIntersectionOfImpl(subClassConjuncts.stream()), superClass, new HashSet<OWLAnnotation>()));
				}
			}

		// C1 cup ... cup Cn sqs B -> { C1 sqs B, ..., Cn sqs B}
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_UNION_OF)) {
			return subClass.disjunctSet().map(subClassDisjunct -> new OWLSubClassOfAxiomImpl(subClassDisjunct, superClass, new HashSet<OWLAnnotation>()))
					.collect(Collectors.toSet());
		}

		// exists R.C sqs D -> { C sqs forall R-.D }
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
			OWLObjectSomeValuesFrom existSubClass = (OWLObjectSomeValuesFrom) subClass;
			return Srd.toSet(new OWLSubClassOfAxiomImpl(existSubClass.getFiller(), new OWLObjectAllValuesFromImpl(existSubClass.getProperty().getInverseProperty(), superClass),
					new HashSet<OWLAnnotation>()));
		}

		// hasValue(R, a) sqs C -> { exists R.{a} sqs C }
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_HAS_VALUE)) {
			OWLObjectHasValue hasValueSubClass = (OWLObjectHasValue) subClass;
			return Srd.toSet(new OWLSubClassOfAxiomImpl(new OWLObjectSomeValuesFromImpl(hasValueSubClass.getProperty(), new OWLObjectOneOfImpl(hasValueSubClass.getFiller())),
					superClass, new HashSet<OWLAnnotation>()));
		}

		// = n R.C sqs D
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_EXACT_CARDINALITY)) {
			OWLObjectExactCardinality exactCardSubClass = (OWLObjectExactCardinality) subClass;
			OWLObjectPropertyExpression objectProperty = exactCardSubClass.getProperty();
			int cardinality = exactCardSubClass.getCardinality();
			OWLClassExpression filler = exactCardSubClass.getFiller();
			OWLObjectMinCardinalityImpl minCardinality = new OWLObjectMinCardinalityImpl(objectProperty, cardinality, filler);
			OWLObjectMaxCardinalityImpl maxCardinality = new OWLObjectMaxCardinalityImpl(objectProperty, cardinality, filler);
			if (cardinality == 0) {
				// = 0 R.C sqs D -> { <= 0 R.C sqs D }
				return Srd.toSet(new OWLSubClassOfAxiomImpl(maxCardinality, superClass, new HashSet<OWLAnnotation>()));
			} else {
				// = n R.C sqs D -> { >= n R.C sqcap <= n R.D sqs D }
				return Srd.toSet(new OWLSubClassOfAxiomImpl(new OWLObjectIntersectionOfImpl(minCardinality, maxCardinality), superClass, new HashSet<OWLAnnotation>()));
			}
		}

		// >= n R.C sqs D
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_MIN_CARDINALITY)) {
			OWLObjectMinCardinality minCardSubClass = (OWLObjectMinCardinality) subClass;
			// >= 0 R.C sqs D -> { T sqs D }
			if (minCardSubClass.getCardinality() == 0)
				return Srd.toSet(new OWLSubClassOfAxiomImpl(Srd.factory.getOWLThing(), superClass, new HashSet<OWLAnnotation>()));
			// >= 1 R.C sqs D -> { exists R.C sqs D }
			if (minCardSubClass.getCardinality() == 1)
				return Srd.toSet(new OWLSubClassOfAxiomImpl(new OWLObjectSomeValuesFromImpl(minCardSubClass.getProperty(), minCardSubClass.getFiller()), superClass,
						new HashSet<OWLAnnotation>()));
			// >= n R.C sqs D -> { T sqs ~ (>= n R.C) cup D } with n >= 2
			if (minCardSubClass.getCardinality() >= 2)
				return Srd.toSet(new OWLSubClassOfAxiomImpl(Srd.factory.getOWLThing(), new OWLObjectUnionOfImpl(Srd.toSet(minCardSubClass.getComplementNNF(), superClass).stream()),
						new HashSet<OWLAnnotation>()));
		}

		// E sqs D -> { Top sqs ~ E cup D} if C is of the form forall R.E, <= n R.C or lnot A sqs B
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)
				|| subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_MAX_CARDINALITY)
				|| subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_COMPLEMENT_OF))
			return Srd.toSet(new OWLSubClassOfAxiomImpl(Srd.factory.getOWLThing(), new OWLObjectUnionOfImpl(Srd.toSet(subClass.getComplementNNF(), superClass).stream()),
					new HashSet<OWLAnnotation>()));

		// Normalize SuperClass

		// C sqs lnot D -> { C cap D sqs Bot }
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_COMPLEMENT_OF))
			return Srd.toSet(new OWLSubClassOfAxiomImpl(new OWLObjectIntersectionOfImpl(Srd.toSet(subClass, superClass.getComplementNNF()).stream()), Srd.factory.getOWLNothing(),
					new HashSet<OWLAnnotation>()));

		// C sqs D1 cap ... cap Dn -> { C sqs D1, ..., C sqs Dn}
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF)) {
			// FIXME test
			// for (OWLClassExpression superClassConjunct : superClass.asConjunctSet())
			// normalizedAxioms.add(new OWLSubClassOfAxiomImpl(subClass, superClassConjunct, new HashSet<OWLAnnotation>()));
			// return normalizedAxioms;
			return superClass.conjunctSet().map(superClassConjunct -> new OWLSubClassOfAxiomImpl(subClass, superClassConjunct, new HashSet<OWLAnnotation>()))
					.collect(Collectors.toSet());
		}

		// C sqs B1 cup ... cup D cup ... cup Bn -> { C sqs B1 cup ... cap X cup ... cup Bn, X sqs D }
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_UNION_OF)) {
			Set<OWLClassExpression> superClassDisjuncts = superClass.asDisjunctSet();
			OWLClassExpression nonClassNameClassExpr = retrieveNonOWLClassClassExpression(superClassDisjuncts);
			if (nonClassNameClassExpr != null) {
				superClassDisjuncts.remove(nonClassNameClassExpr);
				superClassDisjuncts.add(Srd.getCorrespondingFreshClass(nonClassNameClassExpr));
				return Srd.toSet(new OWLSubClassOfAxiomImpl(subClass, new OWLObjectUnionOfImpl(superClassDisjuncts.stream()), new HashSet<OWLAnnotation>()),
						new OWLSubClassOfAxiomImpl(Srd.getCorrespondingFreshClass(nonClassNameClassExpr), nonClassNameClassExpr, new HashSet<OWLAnnotation>()));
			}
		}

		// C sqs exists R.D -> { C sqs >= 1 R.D }
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
			OWLObjectSomeValuesFrom existsSuperClass = (OWLObjectSomeValuesFrom) superClass;
			return Srd.toSet(new OWLSubClassOfAxiomImpl(subClass, new OWLObjectMinCardinalityImpl(existsSuperClass.getProperty(), 1, existsSuperClass.getFiller()),
					new HashSet<OWLAnnotation>()));
		}

		// C sqs hasValue(R, a) -> { C sqs exists R.{a} }
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_HAS_VALUE)) {
			OWLObjectHasValue hasValueSuperClass = (OWLObjectHasValue) superClass;
			return Srd.toSet(new OWLSubClassOfAxiomImpl(subClass,
					new OWLObjectSomeValuesFromImpl(hasValueSuperClass.getProperty(), new OWLObjectOneOfImpl(hasValueSuperClass.getFiller())), new HashSet<OWLAnnotation>()));
		}

		// C sqs forall R.D -> { C sqs forall R.X, X sqs D }
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)) {
			OWLObjectAllValuesFrom allValuesSuperClass = (OWLObjectAllValuesFrom) superClass;
			if (!allValuesSuperClass.getFiller().getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				return Srd.toSet(
						new OWLSubClassOfAxiomImpl(subClass, new OWLObjectAllValuesFromImpl(allValuesSuperClass.getProperty(), Srd.getCorrespondingFreshClass(allValuesSuperClass)),
								new HashSet<OWLAnnotation>()),
						new OWLSubClassOfAxiomImpl(Srd.getCorrespondingFreshClass(allValuesSuperClass), allValuesSuperClass.getFiller(), new HashSet<OWLAnnotation>()));
		}

		// C sqs = n R.D -> { C sqs >= n R.D, C sqs <= n R.D }
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_EXACT_CARDINALITY)) {
			OWLObjectExactCardinality exactCardSuperClass = (OWLObjectExactCardinality) superClass;
			OWLObjectPropertyExpression objectProperty = exactCardSuperClass.getProperty();
			int cardinality = exactCardSuperClass.getCardinality();
			OWLClassExpression filler = exactCardSuperClass.getFiller();
			return Srd.toSet(new OWLSubClassOfAxiomImpl(subClass, new OWLObjectMinCardinalityImpl(objectProperty, cardinality, filler), new HashSet<OWLAnnotation>()),
					new OWLSubClassOfAxiomImpl(subClass, new OWLObjectMaxCardinalityImpl(objectProperty, cardinality, filler), new HashSet<OWLAnnotation>()));
		}

		// C sqs >= n R.D
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_MIN_CARDINALITY)) {
			OWLObjectMinCardinality minCardSuperClass = (OWLObjectMinCardinality) superClass;
			// C sqs >= 0 R.D -> { } ignore min cardinality 0
			if (minCardSuperClass.getCardinality() == 0) {
				return new HashSet<>();
			}
			// C sqs >= n R.D -> { C sqs >= n R.X, X sqs D }
			if (!minCardSuperClass.getFiller().getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				return Srd.toSet(
						new OWLSubClassOfAxiomImpl(subClass,
								new OWLObjectMinCardinalityImpl(minCardSuperClass.getProperty(), minCardSuperClass.getCardinality(),
										Srd.getCorrespondingFreshClass(minCardSuperClass)),
								new HashSet<OWLAnnotation>()),
						new OWLSubClassOfAxiomImpl(Srd.getCorrespondingFreshClass(minCardSuperClass), minCardSuperClass.getFiller(), new HashSet<OWLAnnotation>()));
		}

		// C sqs <= n R.D
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_MAX_CARDINALITY)) {
			OWLObjectMaxCardinality maxCardSuperClass = (OWLObjectMaxCardinality) superClass;
			OWLClassExpression maxCardSuperClassFiller = maxCardSuperClass.getFiller();
			// C sqs <= 0 R.D -> { C sqcap exists R.D subseteq Bot }
			if (maxCardSuperClass.getCardinality() == 0) {
				Set<OWLClassExpression> newSubClassConjuncts = new HashSet<OWLClassExpression>();
				newSubClassConjuncts.addAll(subClass.asConjunctSet());
				newSubClassConjuncts.add(new OWLObjectSomeValuesFromImpl(maxCardSuperClass.getProperty(), maxCardSuperClassFiller));
				return Srd.toSet(
						new OWLSubClassOfAxiomImpl(new OWLObjectIntersectionOfImpl(newSubClassConjuncts.stream()), Srd.factory.getOWLNothing(), new HashSet<OWLAnnotation>()));
				// C sqs <= n R.D -> { C sqs <= n R.X, D sqs X }
			} else if (!maxCardSuperClassFiller.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				return Srd.toSet(
						new OWLSubClassOfAxiomImpl(subClass,
								new OWLObjectMaxCardinalityImpl(maxCardSuperClass.getProperty(), maxCardSuperClass.getCardinality(),
										Srd.getCorrespondingFreshClass(maxCardSuperClassFiller)),
								new HashSet<OWLAnnotation>()),
						new OWLSubClassOfAxiomImpl(maxCardSuperClass.getFiller(), Srd.getCorrespondingFreshClass(maxCardSuperClassFiller), new HashSet<OWLAnnotation>()));
		}

		return Srd.toSet(subClassOfAxiom);
	}

	private static OWLClassExpression retrieveNonOWLClassClassExpression(Set<OWLClassExpression> conceptExpressions) {
		for (OWLClassExpression conceptExpression : conceptExpressions)
			if (!conceptExpression.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				return conceptExpression;
		return null;
	}
}
