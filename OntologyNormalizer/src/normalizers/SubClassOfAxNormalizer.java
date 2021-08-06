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

import launcher.Utils;
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

	protected static void normalizeSubClassOfAx(final Set<OWLSubClassOfAxiom> subClassOfAxioms, final Set<OWLClassAssertionAxiom> classAssertionAxioms) {

		final Set<OWLSubClassOfAxiom> newIterationAxioms = new HashSet<OWLSubClassOfAxiom>();
		newIterationAxioms.addAll(subClassOfAxioms);
		boolean modified = true;

		while (modified) {
			modified = false;

			for (final OWLSubClassOfAxiom subClassOfAxiom : subClassOfAxioms) {
				final Set<OWLSubClassOfAxiom> nfRuleAxioms = applySomeNFRule(subClassOfAxiom);
				newIterationAxioms.addAll(nfRuleAxioms);
				if (modified) {

				}
				if (!Utils.toSet(subClassOfAxiom).equals(nfRuleAxioms)) {
					modified = true;
				}
			}

			subClassOfAxioms.clear();
			subClassOfAxioms.addAll(newIterationAxioms);
			newIterationAxioms.clear();
		}

		// {a1} sqcup ... sqcup {an} sqsubseteq D -> { D(a1), ..., D(an) }
		final Set<OWLSubClassOfAxiom> copySubClassOfAxioms = new HashSet<OWLSubClassOfAxiom>();
		copySubClassOfAxioms.addAll(subClassOfAxioms);
		for (final OWLSubClassOfAxiom normalizedSubClassOfAxiom : copySubClassOfAxioms) {
			final OWLClassExpression subClass = normalizedSubClassOfAxiom.getSubClass();
			if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_ONE_OF)) {
				for (final OWLIndividual individual : ((OWLObjectOneOf) subClass).individuals().collect(Collectors.toSet()))
					classAssertionAxioms.add(new OWLClassAssertionAxiomImpl(individual, normalizedSubClassOfAxiom.getSuperClass(), new HashSet<OWLAnnotation>()));
				subClassOfAxioms.remove(normalizedSubClassOfAxiom);
			}
		}
	}

	private static Set<OWLSubClassOfAxiom> applySomeNFRule(final OWLSubClassOfAxiom subClassOfAxiom) {
		final OWLClassExpression subClass = subClassOfAxiom.getSubClass();
		final OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
		// OWLClassExpression freshClass = Srd.factory.getOWLClass(IRI.create(newConceptPref + Integer.toString(++newEntCounter)));

		// Transform to NNF
		if (!subClass.getNNF().equals(subClass))
			return Utils.toSet(new OWLSubClassOfAxiomImpl(subClass.getNNF(), superClass, new HashSet<OWLAnnotation>()));

		if (!superClass.getNNF().equals(superClass))
			return Utils.toSet(new OWLSubClassOfAxiomImpl(subClass, superClass.getNNF(), new HashSet<OWLAnnotation>()));

		// Split Axioms

		// C sqs D -> { C sqs X, X sqs D }
		if (!subClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS) && !superClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {
			final OWLClassExpression subClassFreshClass = Utils.getCorrespondingFreshClass(subClass);
			return Utils.toSet(new OWLSubClassOfAxiomImpl(subClass, subClassFreshClass, new HashSet<OWLAnnotation>()),
					new OWLSubClassOfAxiomImpl(subClassFreshClass, superClass, new HashSet<OWLAnnotation>()));
		}

		// Normalize SubClass

		// A1 cap ... cap C cap ... cap An sqs D -> { C sqs X, A1 cap ... cap X cap ... cap An sqs D }
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF))
			if (superClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {
				final Set<OWLClassExpression> subClassConjuncts = subClass.asConjunctSet();
				final OWLClassExpression nonClassNameClassExpr = retrieveNonOWLClassClassExpression(subClassConjuncts);
				if (nonClassNameClassExpr != null) {
					subClassConjuncts.remove(nonClassNameClassExpr);
					subClassConjuncts.add(Utils.getCorrespondingFreshClass(nonClassNameClassExpr));
					return Utils.toSet(new OWLSubClassOfAxiomImpl(nonClassNameClassExpr, Utils.getCorrespondingFreshClass(nonClassNameClassExpr), new HashSet<OWLAnnotation>()),
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
			final OWLObjectSomeValuesFrom existSubClass = (OWLObjectSomeValuesFrom) subClass;
			return Utils.toSet(new OWLSubClassOfAxiomImpl(existSubClass.getFiller(), new OWLObjectAllValuesFromImpl(existSubClass.getProperty().getInverseProperty(), superClass),
					new HashSet<OWLAnnotation>()));
		}

		// hasValue(R, a) sqs C -> { exists R.{a} sqs C }
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_HAS_VALUE)) {
			final OWLObjectHasValue hasValueSubClass = (OWLObjectHasValue) subClass;
			return Utils.toSet(new OWLSubClassOfAxiomImpl(new OWLObjectSomeValuesFromImpl(hasValueSubClass.getProperty(), new OWLObjectOneOfImpl(hasValueSubClass.getFiller())),
					superClass, new HashSet<OWLAnnotation>()));
		}

		// = n R.C sqs D
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_EXACT_CARDINALITY)) {
			final OWLObjectExactCardinality exactCardSubClass = (OWLObjectExactCardinality) subClass;
			final OWLObjectPropertyExpression objectProperty = exactCardSubClass.getProperty();
			final int cardinality = exactCardSubClass.getCardinality();
			final OWLClassExpression filler = exactCardSubClass.getFiller();
			final OWLObjectMinCardinalityImpl minCardinality = new OWLObjectMinCardinalityImpl(objectProperty, cardinality, filler);
			final OWLObjectMaxCardinalityImpl maxCardinality = new OWLObjectMaxCardinalityImpl(objectProperty, cardinality, filler);
			if (cardinality == 0) {
				// = 0 R.C sqs D -> { <= 0 R.C sqs D }
				return Utils.toSet(new OWLSubClassOfAxiomImpl(maxCardinality, superClass, new HashSet<OWLAnnotation>()));
			} else {
				// = n R.C sqs D -> { >= n R.C sqcap <= n R.D sqs D }
				return Utils.toSet(new OWLSubClassOfAxiomImpl(new OWLObjectIntersectionOfImpl(minCardinality, maxCardinality), superClass, new HashSet<OWLAnnotation>()));
			}
		}

		// >= n R.C sqs D
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_MIN_CARDINALITY)) {
			final OWLObjectMinCardinality minCardSubClass = (OWLObjectMinCardinality) subClass;
			// >= 0 R.C sqs D -> { T sqs D }
			if (minCardSubClass.getCardinality() == 0)
				return Utils.toSet(new OWLSubClassOfAxiomImpl(Utils.factory.getOWLThing(), superClass, new HashSet<OWLAnnotation>()));
			// >= 1 R.C sqs D -> { exists R.C sqs D }
			if (minCardSubClass.getCardinality() == 1)
				return Utils.toSet(new OWLSubClassOfAxiomImpl(new OWLObjectSomeValuesFromImpl(minCardSubClass.getProperty(), minCardSubClass.getFiller()), superClass,
						new HashSet<OWLAnnotation>()));
			// >= n R.C sqs D -> { T sqs ~ (>= n R.C) cup D } with n >= 2
			if (minCardSubClass.getCardinality() >= 2)
				return Utils.toSet(new OWLSubClassOfAxiomImpl(Utils.factory.getOWLThing(), new OWLObjectUnionOfImpl(Utils.toSet(minCardSubClass.getComplementNNF(), superClass).stream()),
						new HashSet<OWLAnnotation>()));
		}

		// E sqs D -> { Top sqs ~ E cup D} if C is of the form forall R.E, <= n R.C or lnot A sqs B
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)
				|| subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_MAX_CARDINALITY)
				|| subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_COMPLEMENT_OF))
			return Utils.toSet(new OWLSubClassOfAxiomImpl(Utils.factory.getOWLThing(), new OWLObjectUnionOfImpl(Utils.toSet(subClass.getComplementNNF(), superClass).stream()),
					new HashSet<OWLAnnotation>()));

		// Normalize SuperClass

		// C sqs lnot D -> { C cap D sqs Bot }
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_COMPLEMENT_OF))
			return Utils.toSet(new OWLSubClassOfAxiomImpl(new OWLObjectIntersectionOfImpl(Utils.toSet(subClass, superClass.getComplementNNF()).stream()), Utils.factory.getOWLNothing(),
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
			final Set<OWLClassExpression> superClassDisjuncts = superClass.asDisjunctSet();
			final OWLClassExpression nonClassNameClassExpr = retrieveNonOWLClassClassExpression(superClassDisjuncts);
			if (nonClassNameClassExpr != null) {
				superClassDisjuncts.remove(nonClassNameClassExpr);
				superClassDisjuncts.add(Utils.getCorrespondingFreshClass(nonClassNameClassExpr));
				return Utils.toSet(new OWLSubClassOfAxiomImpl(subClass, new OWLObjectUnionOfImpl(superClassDisjuncts.stream()), new HashSet<OWLAnnotation>()),
						new OWLSubClassOfAxiomImpl(Utils.getCorrespondingFreshClass(nonClassNameClassExpr), nonClassNameClassExpr, new HashSet<OWLAnnotation>()));
			}
		}

		// C sqs exists R.D -> { C sqs >= 1 R.D }
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
			final OWLObjectSomeValuesFrom existsSuperClass = (OWLObjectSomeValuesFrom) superClass;
			return Utils.toSet(new OWLSubClassOfAxiomImpl(subClass, new OWLObjectMinCardinalityImpl(existsSuperClass.getProperty(), 1, existsSuperClass.getFiller()),
					new HashSet<OWLAnnotation>()));
		}

		// C sqs hasValue(R, a) -> { C sqs exists R.{a} }
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_HAS_VALUE)) {
			final OWLObjectHasValue hasValueSuperClass = (OWLObjectHasValue) superClass;
			return Utils.toSet(new OWLSubClassOfAxiomImpl(subClass,
					new OWLObjectSomeValuesFromImpl(hasValueSuperClass.getProperty(), new OWLObjectOneOfImpl(hasValueSuperClass.getFiller())), new HashSet<OWLAnnotation>()));
		}

		// C sqs forall R.D -> { C sqs forall R.X, X sqs D }
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)) {
			final OWLObjectAllValuesFrom allValuesSuperClass = (OWLObjectAllValuesFrom) superClass;
			if (!allValuesSuperClass.getFiller().getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				return Utils.toSet(
						new OWLSubClassOfAxiomImpl(subClass, new OWLObjectAllValuesFromImpl(allValuesSuperClass.getProperty(), Utils.getCorrespondingFreshClass(allValuesSuperClass)),
								new HashSet<OWLAnnotation>()),
						new OWLSubClassOfAxiomImpl(Utils.getCorrespondingFreshClass(allValuesSuperClass), allValuesSuperClass.getFiller(), new HashSet<OWLAnnotation>()));
		}

		// C sqs = n R.D -> { C sqs >= n R.D, C sqs <= n R.D }
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_EXACT_CARDINALITY)) {
			final OWLObjectExactCardinality exactCardSuperClass = (OWLObjectExactCardinality) superClass;
			final OWLObjectPropertyExpression objectProperty = exactCardSuperClass.getProperty();
			final int cardinality = exactCardSuperClass.getCardinality();
			final OWLClassExpression filler = exactCardSuperClass.getFiller();
			return Utils.toSet(new OWLSubClassOfAxiomImpl(subClass, new OWLObjectMinCardinalityImpl(objectProperty, cardinality, filler), new HashSet<OWLAnnotation>()),
					new OWLSubClassOfAxiomImpl(subClass, new OWLObjectMaxCardinalityImpl(objectProperty, cardinality, filler), new HashSet<OWLAnnotation>()));
		}

		// C sqs >= n R.D
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_MIN_CARDINALITY)) {
			final OWLObjectMinCardinality minCardSuperClass = (OWLObjectMinCardinality) superClass;
			// C sqs >= 0 R.D -> { } ignore min cardinality 0
			if (minCardSuperClass.getCardinality() == 0) {
				return new HashSet<>();
			}
			// C sqs >= n R.D -> { C sqs >= n R.X, X sqs D }
			if (!minCardSuperClass.getFiller().getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				return Utils.toSet(
						new OWLSubClassOfAxiomImpl(subClass,
								new OWLObjectMinCardinalityImpl(minCardSuperClass.getProperty(), minCardSuperClass.getCardinality(),
										Utils.getCorrespondingFreshClass(minCardSuperClass)),
								new HashSet<OWLAnnotation>()),
						new OWLSubClassOfAxiomImpl(Utils.getCorrespondingFreshClass(minCardSuperClass), minCardSuperClass.getFiller(), new HashSet<OWLAnnotation>()));
		}

		// C sqs <= n R.D
		if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_MAX_CARDINALITY)) {
			final OWLObjectMaxCardinality maxCardSuperClass = (OWLObjectMaxCardinality) superClass;
			final OWLClassExpression maxCardSuperClassFiller = maxCardSuperClass.getFiller();
			// C sqs <= 0 R.D -> { C sqcap exists R.D subseteq Bot }
			if (maxCardSuperClass.getCardinality() == 0) {
				final Set<OWLClassExpression> newSubClassConjuncts = new HashSet<OWLClassExpression>();
				newSubClassConjuncts.addAll(subClass.asConjunctSet());
				newSubClassConjuncts.add(new OWLObjectSomeValuesFromImpl(maxCardSuperClass.getProperty(), maxCardSuperClassFiller));
				return Utils.toSet(
						new OWLSubClassOfAxiomImpl(new OWLObjectIntersectionOfImpl(newSubClassConjuncts.stream()), Utils.factory.getOWLNothing(), new HashSet<OWLAnnotation>()));
				// C sqs <= n R.D -> { C sqs <= n R.X, D sqs X }
			} else if (!maxCardSuperClassFiller.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				return Utils.toSet(
						new OWLSubClassOfAxiomImpl(subClass,
								new OWLObjectMaxCardinalityImpl(maxCardSuperClass.getProperty(), maxCardSuperClass.getCardinality(),
										Utils.getCorrespondingFreshClass(maxCardSuperClassFiller)),
								new HashSet<OWLAnnotation>()),
						new OWLSubClassOfAxiomImpl(maxCardSuperClass.getFiller(), Utils.getCorrespondingFreshClass(maxCardSuperClassFiller), new HashSet<OWLAnnotation>()));
		}

		return Utils.toSet(subClassOfAxiom);
	}

	private static OWLClassExpression retrieveNonOWLClassClassExpression(final Set<OWLClassExpression> conceptExpressions) {
		for (final OWLClassExpression conceptExpression : conceptExpressions)
			if (!conceptExpression.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				return conceptExpression;
		return null;
	}
}
