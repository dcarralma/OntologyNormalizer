package normalizers;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
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
import uk.ac.manchester.cs.owl.owlapi.InternalizedEntities;

public class SubClassOfAxNormalizer {

	protected static void normalizeSubClassOfAx(final Set<OWLSubClassOfAxiom> subClassOfAxioms,
			final Set<OWLClassAssertionAxiom> classAssertionAxioms) {

		final Set<OWLSubClassOfAxiom> newIterationAxioms = new HashSet<>();
		newIterationAxioms.addAll(subClassOfAxioms);
		boolean modified = true;

		while (modified) {
			modified = false;

			for (final OWLSubClassOfAxiom subClassOfAxiom : subClassOfAxioms) {
				final Set<OWLSubClassOfAxiom> nfRuleAxioms = applySomeNFRule(subClassOfAxiom);
				newIterationAxioms.addAll(nfRuleAxioms);

				if (!Utils.toSet(subClassOfAxiom).equals(nfRuleAxioms)) {
					modified = true;
				}
			}

			subClassOfAxioms.clear();
			subClassOfAxioms.addAll(newIterationAxioms);
			newIterationAxioms.clear();
		}

		// {a1} sqcup ... sqcup {an} sqsubseteq D -> { D(a1), ..., D(an) }
		final Set<OWLSubClassOfAxiom> copySubClassOfAxioms = new HashSet<>();
		copySubClassOfAxioms.addAll(subClassOfAxioms);
		for (final OWLSubClassOfAxiom normalizedSubClassOfAxiom : copySubClassOfAxioms) {
			final OWLClassExpression subClass = normalizedSubClassOfAxiom.getSubClass();
			if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_ONE_OF)) {

				((OWLObjectOneOf) subClass).individuals().forEach(individual -> {
					final OWLClassAssertionAxiom owlClassAssertionAxiom = Utils.factory
							.getOWLClassAssertionAxiom(normalizedSubClassOfAxiom.getSuperClass(), individual);
					classAssertionAxioms.add(owlClassAssertionAxiom);
				});

				subClassOfAxioms.remove(normalizedSubClassOfAxiom);
			}
		}
	}

	private static Set<OWLSubClassOfAxiom> applySomeNFRule(final OWLSubClassOfAxiom subClassOfAxiom) {
		final OWLClassExpression subClass = subClassOfAxiom.getSubClass();
		final OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
		// OWLClassExpression freshClass =
		// Srd.factory.getOWLClass(IRI.create(newConceptPref +
		// Integer.toString(++newEntCounter)));

		// Transform to NNF
		if (!subClass.getNNF().equals(subClass))
			return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(subClass.getNNF(), superClass));

		if (!superClass.getNNF().equals(superClass))
			return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(subClass, superClass.getNNF()));

		// Split Axioms
		// C sqs D -> { C sqs X, X sqs D }
		if (!subClass.isOWLClass() && !superClass.isOWLClass()) {
			final OWLClassExpression subClassFreshClass = Utils.getCorrespondingFreshClass(subClass);
			return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(subClass, subClassFreshClass),
					Utils.factory.getOWLSubClassOfAxiom(subClassFreshClass, superClass));
		}

		// Normalize SubClass
		switch (subClass.getClassExpressionType()) {

		/*
		 * A1 cap ... cap C cap ... cap An sqs D -> { C sqs X, A1 cap ... cap X cap ...
		 * cap An sqs D }
		 */
		case OBJECT_INTERSECTION_OF:
			if (superClass.isOWLClass()) {

				final Optional<OWLClassExpression> complexClassExpresstionOptional = retrieveFirstComplexClassExpression(
						subClass.conjunctSet());

				if (complexClassExpresstionOptional.isPresent()) {
					final OWLClassExpression complexClassExpression = complexClassExpresstionOptional.get();

					final Set<OWLClassExpression> subClassConjuncts = subClass.asConjunctSet();
					subClassConjuncts.remove(complexClassExpression);
					subClassConjuncts.add(Utils.getCorrespondingFreshClass(complexClassExpression));
					return Utils.toSet(

							Utils.factory.getOWLSubClassOfAxiom(complexClassExpression,
									Utils.getCorrespondingFreshClass(complexClassExpression)),

							Utils.factory.getOWLSubClassOfAxiom(
									Utils.factory.getOWLObjectIntersectionOf(subClassConjuncts), superClass));
				}
			}
			break;

		// C1 cup ... cup Cn sqs B -> { C1 sqs B, ..., Cn sqs B}
		case OBJECT_UNION_OF:
			return subClass.disjunctSet()
					.map(subClassDisjunct -> Utils.factory.getOWLSubClassOfAxiom(subClassDisjunct, superClass))
					.collect(Collectors.toSet());

		// exists R.C sqs D -> { C sqs forall R-.D }
		case OBJECT_SOME_VALUES_FROM:
			final OWLObjectSomeValuesFrom existSubClass = (OWLObjectSomeValuesFrom) subClass;
			return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(existSubClass.getFiller(), Utils.factory
					.getOWLObjectAllValuesFrom(existSubClass.getProperty().getInverseProperty(), superClass)));

		// hasValue(R, a) sqs C -> { exists R.{a} sqs C }
		case OBJECT_HAS_VALUE:
			final OWLObjectHasValue hasValueSubClass = (OWLObjectHasValue) subClass;
			return Utils.toSet(Utils.factory
					.getOWLSubClassOfAxiom(Utils.factory.getOWLObjectSomeValuesFrom(hasValueSubClass.getProperty(),
							Utils.factory.getOWLObjectOneOf(hasValueSubClass.getFiller())), superClass));

		// = n R.C sqs D
		case OBJECT_EXACT_CARDINALITY:
			final OWLObjectExactCardinality exactCardSubClass = (OWLObjectExactCardinality) subClass;
			final OWLObjectPropertyExpression objectProperty = exactCardSubClass.getProperty();
			final int cardinality = exactCardSubClass.getCardinality();
			final OWLClassExpression filler = exactCardSubClass.getFiller();
			final OWLObjectMinCardinality minCardinality = Utils.factory.getOWLObjectMinCardinality(cardinality,
					objectProperty, filler);
			final OWLObjectMaxCardinality maxCardinality = Utils.factory.getOWLObjectMaxCardinality(cardinality,
					objectProperty, filler);
			if (cardinality == 0) {
				// = 0 R.C sqs D -> { <= 0 R.C sqs D }
				return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(maxCardinality, superClass));
			} else {
				// = n R.C sqs D -> { >= n R.C sqcap <= n R.D sqs D }
				return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(
						Utils.factory.getOWLObjectIntersectionOf(minCardinality, maxCardinality), superClass));
			}

			// >= n R.C sqs D
		case OBJECT_MIN_CARDINALITY:

			final OWLObjectMinCardinality minCardSubClass = (OWLObjectMinCardinality) subClass;
			// >= 0 R.C sqs D -> { T sqs D }
			if (minCardSubClass.getCardinality() == 0) {
				return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(InternalizedEntities.OWL_THING, superClass));
			}
			// >= 1 R.C sqs D -> { exists R.C sqs D }
			if (minCardSubClass.getCardinality() == 1) {
				return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(Utils.factory.getOWLObjectSomeValuesFrom(
						minCardSubClass.getProperty(), minCardSubClass.getFiller()), superClass));
			}
			// >= n R.C sqs D -> { T sqs ~ (>= n R.C) cup D } with n >= 2
			if (minCardSubClass.getCardinality() >= 2) {
				return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(InternalizedEntities.OWL_THING,
						Utils.factory.getOWLObjectUnionOf(minCardSubClass.getComplementNNF(), superClass)));
			}
			break;

		// E sqs D -> { Top sqs ~ E cup D} if C is of the form forall R.E, <= n R.C or
		// lnot A sqs B
		case OBJECT_ALL_VALUES_FROM:
		case OBJECT_MAX_CARDINALITY:
		case OBJECT_COMPLEMENT_OF:
			return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(InternalizedEntities.OWL_THING,
					Utils.factory.getOWLObjectUnionOf(subClass.getComplementNNF(), superClass)));

		default:
			break;
		}

		// Normalize SuperClass
		switch (superClass.getClassExpressionType()) {
		// C sqs lnot D -> { C cap D sqs Bot }
		case OBJECT_COMPLEMENT_OF:
			return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(
					Utils.factory.getOWLObjectIntersectionOf(subClass, superClass.getComplementNNF()),
					InternalizedEntities.OWL_NOTHING));

		// C sqs D1 cap ... cap Dn -> { C sqs D1, ..., C sqs Dn}
		case OBJECT_INTERSECTION_OF:
			// FIXME test
			// for (OWLClassExpression superClassConjunct : superClass.asConjunctSet())
			// normalizedAxioms.add(new OWLSubClassOfAxiomImpl(subClass, superClassConjunct,
			// new HashSet<OWLAnnotation>()));
			// return normalizedAxioms;
			return superClass.conjunctSet()
					.map(superClassConjunct -> Utils.factory.getOWLSubClassOfAxiom(subClass, superClassConjunct))
					.collect(Collectors.toSet());

		// C sqs B1 cup ... cup D cup ... cup Bn -> { C sqs B1 cup ... cap X cup ... cup
		// Bn, X sqs D }
		case OBJECT_UNION_OF:
			final Optional<OWLClassExpression> nonClassNameClassExprOptional = retrieveFirstComplexClassExpression(
					superClass.disjunctSet());
			if (nonClassNameClassExprOptional.isPresent()) {
				final OWLClassExpression nonClassNameClassExpr = nonClassNameClassExprOptional.get();
				final Set<OWLClassExpression> superClassDisjuncts = superClass.asDisjunctSet();
				superClassDisjuncts.remove(nonClassNameClassExpr);
				superClassDisjuncts.add(Utils.getCorrespondingFreshClass(nonClassNameClassExpr));
				return Utils.toSet(
						Utils.factory.getOWLSubClassOfAxiom(subClass,
								Utils.factory.getOWLObjectUnionOf(superClassDisjuncts)),
						Utils.factory.getOWLSubClassOfAxiom(Utils.getCorrespondingFreshClass(nonClassNameClassExpr),
								nonClassNameClassExpr));
			}
			break;

		// C sqs exists R.D -> { C sqs >= 1 R.D }
		case OBJECT_SOME_VALUES_FROM:
			final OWLObjectSomeValuesFrom existsSuperClass = (OWLObjectSomeValuesFrom) superClass;
			return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(subClass, Utils.factory.getOWLObjectMinCardinality(1,
					existsSuperClass.getProperty(), existsSuperClass.getFiller())));

		// C sqs hasValue(R, a) -> { C sqs exists R.{a} }
		case OBJECT_HAS_VALUE:
			final OWLObjectHasValue hasValueSuperClass = (OWLObjectHasValue) superClass;
			return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(subClass,
					Utils.factory.getOWLObjectSomeValuesFrom(hasValueSuperClass.getProperty(),
							Utils.factory.getOWLObjectOneOf(hasValueSuperClass.getFiller()))));

		// C sqs forall R.D -> { C sqs forall R.X, X sqs D }
		case OBJECT_ALL_VALUES_FROM:
			final OWLObjectAllValuesFrom allValuesSuperClass = (OWLObjectAllValuesFrom) superClass;
			if (!allValuesSuperClass.getFiller().isOWLClass()) {
				return Utils.toSet(
						Utils.factory.getOWLSubClassOfAxiom(subClass,
								Utils.factory.getOWLObjectAllValuesFrom(allValuesSuperClass.getProperty(),
										Utils.getCorrespondingFreshClass(allValuesSuperClass))),
						Utils.factory.getOWLSubClassOfAxiom(Utils.getCorrespondingFreshClass(allValuesSuperClass),
								allValuesSuperClass.getFiller()));
			}
			break;

		// C sqs = n R.D -> { C sqs >= n R.D, C sqs <= n R.D }
		case OBJECT_EXACT_CARDINALITY:
			final OWLObjectExactCardinality exactCardSuperClass = (OWLObjectExactCardinality) superClass;
			final OWLObjectPropertyExpression objectProperty = exactCardSuperClass.getProperty();
			final int cardinality = exactCardSuperClass.getCardinality();
			final OWLClassExpression filler = exactCardSuperClass.getFiller();
			return Utils.toSet(
					Utils.factory.getOWLSubClassOfAxiom(subClass,
							Utils.factory.getOWLObjectMinCardinality(cardinality, objectProperty, filler)),
					Utils.factory.getOWLSubClassOfAxiom(subClass,
							Utils.factory.getOWLObjectMaxCardinality(cardinality, objectProperty, filler)));

		// C sqs >= n R.D
		case OBJECT_MIN_CARDINALITY:
			final OWLObjectMinCardinality minCardSuperClass = (OWLObjectMinCardinality) superClass;
			// C sqs >= 0 R.D -> { } ignore min cardinality 0
			if (minCardSuperClass.getCardinality() == 0) {
				return new HashSet<>();
			}
			// C sqs >= n R.D -> { C sqs >= n R.X, X sqs D }
			if (!minCardSuperClass.getFiller().getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(subClass,
						Utils.factory.getOWLObjectMinCardinality(minCardSuperClass.getCardinality(),
								minCardSuperClass.getProperty(), Utils.getCorrespondingFreshClass(minCardSuperClass))),
						Utils.factory.getOWLSubClassOfAxiom(Utils.getCorrespondingFreshClass(minCardSuperClass),
								minCardSuperClass.getFiller()));
			break;

		// C sqs <= n R.D
		case OBJECT_MAX_CARDINALITY:
			final OWLObjectMaxCardinality maxCardSuperClass = (OWLObjectMaxCardinality) superClass;
			final OWLClassExpression maxCardSuperClassFiller = maxCardSuperClass.getFiller();

			// C sqs <= 0 R.D -> { C sqcap exists R.D subseteq Bot }
			if (maxCardSuperClass.getCardinality() == 0) {
				final Set<OWLClassExpression> newSubClassConjuncts = new HashSet<>();
				newSubClassConjuncts.addAll(subClass.asConjunctSet());
				newSubClassConjuncts.add(Utils.factory.getOWLObjectSomeValuesFrom(maxCardSuperClass.getProperty(),
						maxCardSuperClassFiller));

				return Utils.toSet(Utils.factory.getOWLSubClassOfAxiom(
						Utils.factory.getOWLObjectIntersectionOf(newSubClassConjuncts),
						InternalizedEntities.OWL_NOTHING));

				// C sqs <= n R.D -> { C sqs <= n R.X, D sqs X }
			} else if (!maxCardSuperClassFiller.isOWLClass()) {
				return Utils.toSet(
						Utils.factory.getOWLSubClassOfAxiom(subClass,
								Utils.factory.getOWLObjectMaxCardinality(maxCardSuperClass.getCardinality(),
										maxCardSuperClass.getProperty(),
										Utils.getCorrespondingFreshClass(maxCardSuperClassFiller))),
						Utils.factory.getOWLSubClassOfAxiom(maxCardSuperClass.getFiller(),
								Utils.getCorrespondingFreshClass(maxCardSuperClassFiller)));
			}
			break;

		default:
			break;
		}

		return Utils.toSet(subClassOfAxiom);
	}

	private static Optional<OWLClassExpression> retrieveFirstComplexClassExpression(
			final Stream<OWLClassExpression> conceptExpressions) {
		return conceptExpressions.filter(conceptExpression -> !conceptExpression.isOWLClass()).findFirst();
	}
}
