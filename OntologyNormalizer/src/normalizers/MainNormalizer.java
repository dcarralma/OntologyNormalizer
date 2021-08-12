package normalizers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.HasOperands;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;

import launcher.Utils;
import uk.ac.manchester.cs.owl.owlapi.InternalizedEntities;

public class MainNormalizer implements NormalizerInterface {

	// Distribute Axioms
	private final Set<OWLSubClassOfAxiom> subClassOfAxs = new HashSet<>();
	private final Set<OWLSubObjectPropertyOfAxiom> simpleObjPropInclusionAxs = new HashSet<>();
	private final Set<OWLSubPropertyChainOfAxiom> complexObjPropInclusionAxs = new HashSet<>();
	private final Set<OWLDisjointObjectPropertiesAxiom> disjointObjPropAxs = new HashSet<>();
	private final Set<SWRLRule> swrlRules = new HashSet<>();
	private final Set<OWLClassAssertionAxiom> classAsss = new HashSet<>();
	private final Set<OWLObjectPropertyAssertionAxiom> objPropAsss = new HashSet<>();
	private final Set<OWLNegativeObjectPropertyAssertionAxiom> negativeObjPropAsss = new HashSet<>();
	private final Set<OWLSameIndividualAxiom> sameIndsAsss = new HashSet<>();
	private final Set<OWLDifferentIndividualsAxiom> differentIndsAsss = new HashSet<>();

	@Override
	public Set<OWLLogicalAxiom> filterAndNormalizeAxioms(final OWLOntology ontology) {

		// Filter out non-logical axioms
		this.distributeAndFilterDataAxs(ontology.logicalAxioms());

		// Normalize SWRLRules
		SWRLRulesNormalizer.normalizeSWRLRules(this.swrlRules, this.subClassOfAxs);

		// Normalize ABoxAxioms
		ABoxNormalizer.normalizeClassAssertions(this.classAsss, this.subClassOfAxs);
		ABoxNormalizer.normalizeNegativeObjPropAsssertions(this.negativeObjPropAsss, this.objPropAsss,
				this.disjointObjPropAxs);

		// Normalize SubClassOfAxioms
		SubClassOfAxNormalizer.normalizeSubClassOfAx(this.subClassOfAxs, this.classAsss);

		return this.collectNormalizedAxioms();
	}

	private Set<OWLLogicalAxiom> collectNormalizedAxioms() {
		final Set<OWLLogicalAxiom> normalizedAxioms = new HashSet<>();
		normalizedAxioms.addAll(this.subClassOfAxs);
		normalizedAxioms.addAll(this.simpleObjPropInclusionAxs);
		normalizedAxioms.addAll(this.complexObjPropInclusionAxs);
		normalizedAxioms.addAll(this.disjointObjPropAxs);
		normalizedAxioms.addAll(this.swrlRules);
		normalizedAxioms.addAll(this.classAsss);
		normalizedAxioms.addAll(this.objPropAsss);
		normalizedAxioms.addAll(this.sameIndsAsss);
		normalizedAxioms.addAll(this.differentIndsAsss);
		return normalizedAxioms;
	}

	private void distributeAndFilterDataAxs(final Stream<OWLLogicalAxiom> logicalAxs) {

		logicalAxs.forEach(logicalAx -> {
			if (containsIllegalExpressions(logicalAx)) {
				System.out.println("WARNING! Eliminate axioms wich contain illegal expressions: " + logicalAx);
				return;
			}
			this.normalizeLogicalAxiom(logicalAx);
		});

	}

	private void normalizeLogicalAxiom(final OWLLogicalAxiom logicalAx) {
		switch (logicalAx.getAxiomType().toString()) {
		// TBox
		case "DisjointClasses":
			this.subClassOfAxs.addAll(disjointClassesAxToSubClassOfAxs((OWLDisjointClassesAxiom) logicalAx));
			break;

		case "DisjointUnion":
			final OWLDisjointUnionAxiom disjointUnionAxiom = (OWLDisjointUnionAxiom) logicalAx;
			this.subClassOfAxs
					.addAll(disjointClassesAxToSubClassOfAxs(disjointUnionAxiom.getOWLDisjointClassesAxiom()));
			this.subClassOfAxs
					.addAll(equivalentClassesAxToSubClassOfAxs(disjointUnionAxiom.getOWLEquivalentClassesAxiom()));
			break;

		case "InverseFunctionalObjectProperty":
			this.subClassOfAxs.add(Utils.factory.getOWLSubClassOfAxiom(InternalizedEntities.OWL_THING,
					Utils.factory.getOWLObjectMaxCardinality(1,
							((OWLInverseFunctionalObjectPropertyAxiom) logicalAx).getProperty().getInverseProperty(),
							InternalizedEntities.OWL_THING)));
			break;

		case "IrrefexiveObjectProperty":
			this.subClassOfAxs.add(Utils.factory.getOWLSubClassOfAxiom(
					Utils.factory.getOWLObjectHasSelf(((OWLIrreflexiveObjectPropertyAxiom) logicalAx).getProperty()),
					InternalizedEntities.OWL_NOTHING));
			break;

		case "EquivalentClasses":
			this.subClassOfAxs.addAll(equivalentClassesAxToSubClassOfAxs((OWLEquivalentClassesAxiom) logicalAx));
			break;

		case "FunctionalObjectProperty":
			this.subClassOfAxs.add(Utils.factory.getOWLSubClassOfAxiom(InternalizedEntities.OWL_THING,
					Utils.factory.getOWLObjectMaxCardinality(1,
							((OWLFunctionalObjectPropertyAxiom) logicalAx).getProperty(),
							InternalizedEntities.OWL_THING)));
			break;

		case "ObjectPropertyRange":
			final OWLObjectPropertyRangeAxiom objPropRangeAxiom = (OWLObjectPropertyRangeAxiom) logicalAx;
			this.subClassOfAxs.add(Utils.factory.getOWLSubClassOfAxiom(InternalizedEntities.OWL_THING, Utils.factory
					.getOWLObjectAllValuesFrom(objPropRangeAxiom.getProperty(), objPropRangeAxiom.getRange())));
			break;

		case "ObjectPropertyDomain":
			final OWLObjectPropertyDomainAxiom domainObjPropRangeAxiom = (OWLObjectPropertyDomainAxiom) logicalAx;
			this.subClassOfAxs.add(Utils.factory.getOWLSubClassOfAxiom(Utils.factory
					.getOWLObjectSomeValuesFrom(domainObjPropRangeAxiom.getProperty(), InternalizedEntities.OWL_THING),
					domainObjPropRangeAxiom.getDomain()));
			break;

		case "ReflexiveObjectProperty":
			this.subClassOfAxs.add(Utils.factory.getOWLSubClassOfAxiom(InternalizedEntities.OWL_THING,
					Utils.factory.getOWLObjectHasSelf(((OWLReflexiveObjectPropertyAxiom) logicalAx).getProperty())));
			break;

		case "SubClassOf":
			this.subClassOfAxs.add((OWLSubClassOfAxiom) logicalAx);
			break;

		// RBox
		case "AsymmetricObjectProperty":
			final OWLObjectPropertyExpression asymmetricObjProp = ((OWLAsymmetricObjectPropertyAxiom) logicalAx)
					.getProperty();
			this.disjointObjPropAxs.add(Utils.factory.getOWLDisjointObjectPropertiesAxiom(asymmetricObjProp,
					asymmetricObjProp.getInverseProperty()));
			break;

		case "DisjointObjectProperties":
			this.disjointObjPropAxs.add((OWLDisjointObjectPropertiesAxiom) logicalAx);
			break;

		case "EquivalentObjectProperties":
			final Set<OWLObjectPropertyExpression> equivalentObjProps = ((OWLEquivalentObjectPropertiesAxiom) logicalAx)
					.properties().collect(Collectors.toSet());
			for (final OWLObjectPropertyExpression equivalentObjPropi : equivalentObjProps)
				for (final OWLObjectPropertyExpression equivalentObjPropj : equivalentObjProps)
					if (!equivalentObjPropi.equals(equivalentObjPropj))
						this.simpleObjPropInclusionAxs.add(
								Utils.factory.getOWLSubObjectPropertyOfAxiom(equivalentObjPropi, equivalentObjPropj));
			break;

		case "InverseObjectProperties":
			final OWLInverseObjectPropertiesAxiom invObjPropAx = (OWLInverseObjectPropertiesAxiom) logicalAx;
			this.simpleObjPropInclusionAxs.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(
					invObjPropAx.getFirstProperty().getInverseProperty(), invObjPropAx.getSecondProperty()));
			this.simpleObjPropInclusionAxs.add(Utils.factory.getOWLSubObjectPropertyOfAxiom(
					invObjPropAx.getSecondProperty().getInverseProperty(), invObjPropAx.getFirstProperty()));
			break;

		case "SubObjectPropertyOf":
			this.simpleObjPropInclusionAxs.add((OWLSubObjectPropertyOfAxiom) logicalAx);
			break;

		case "SymmetricObjectProperty":
			final OWLObjectPropertyExpression symmetrictObjProp = ((OWLSymmetricObjectPropertyAxiom) logicalAx)
					.getProperty();
			this.simpleObjPropInclusionAxs.add(Utils.factory
					.getOWLSubObjectPropertyOfAxiom(symmetrictObjProp.getInverseProperty(), symmetrictObjProp));
			break;

		case "SubPropertyChainOf":
			this.complexObjPropInclusionAxs.add((OWLSubPropertyChainOfAxiom) logicalAx);
			break;

		case "TransitiveObjectProperty":
			final OWLObjectPropertyExpression transitiveObjProp = ((OWLTransitiveObjectPropertyAxiom) logicalAx)
					.getProperty();
			this.complexObjPropInclusionAxs.add(Utils.factory.getOWLSubPropertyChainOfAxiom(
					Arrays.asList(transitiveObjProp, transitiveObjProp), transitiveObjProp));

//				complexObjPropInclusionAxs.add(new OWLSubPropertyChainAxiomImpl(Utils.toList(transitiveObjProp, transitiveObjProp), transitiveObjProp, new HashSet<OWLAnnotation>()));
			break;

		// Rules
		case "Rule":
			this.swrlRules.add((SWRLRule) logicalAx);
			break;

		// ABox
		case "ClassAssertion":
			this.classAsss.add((OWLClassAssertionAxiom) logicalAx);
			break;

		case "DifferentIndividuals":
			this.differentIndsAsss.add((OWLDifferentIndividualsAxiom) logicalAx);
			break;

		case "NegativeObjectPropertyAssertion":
			this.negativeObjPropAsss.add((OWLNegativeObjectPropertyAssertionAxiom) logicalAx);
			break;

		case "ObjectPropertyAssertion":
			this.objPropAsss.add((OWLObjectPropertyAssertionAxiom) logicalAx);
			break;

		case "SameIndividual":
			this.sameIndsAsss.add((OWLSameIndividualAxiom) logicalAx);
			break;

		default:
			System.out.println(
					"WARNING!!! Unrecognized type of Logical Axiom at normalizeOntology at MainNormalizer.java.");
			System.out.println(" -> " + logicalAx.getAxiomType());
			System.out.println(" -> " + logicalAx + "\n");
		}
	}

	/**
	 * Illegal expressions include datatypes and data properties, and empty UnionOf,
	 * IntersectionOf and OneOf expressions. Axioms that contain illegal expressions
	 * will be ignored by the normalizer and will not be found in the output
	 * ontology.
	 * 
	 * @param logicalAx
	 * @return
	 */
	private static boolean containsIllegalExpressions(final OWLLogicalAxiom logicalAx) {
		return containsDataFeatures(logicalAx) || containsIllegalEmptyExpression(logicalAx);
	}

	private static boolean containsIllegalEmptyExpression(final OWLLogicalAxiom logicalAx) {
		// empty one of: <owl:oneOf
		// rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"/>
		// empty union: <owl:unionOf
		// rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"/>
		// empty intersection: <owl:intersectionOf
		// rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"/>
		final List<ClassExpressionType> forbiddenEmptyClassExpressionTypes = Arrays.asList(
				ClassExpressionType.OBJECT_ONE_OF, ClassExpressionType.OBJECT_UNION_OF,
				ClassExpressionType.OBJECT_INTERSECTION_OF);
		final Predicate<OWLClassExpression> containsEmptyExpression = expression -> forbiddenEmptyClassExpressionTypes
				.contains(expression.getClassExpressionType()) && ((HasOperands<?>) expression).operands().count() == 0;

		return logicalAx.nestedClassExpressions().anyMatch(containsEmptyExpression);
	}

	private static boolean containsDataFeatures(final OWLLogicalAxiom logicalAx) {
		return logicalAx.dataPropertiesInSignature().count() != 0 || logicalAx.datatypesInSignature().count() != 0;
	}

	// FIXME what do we do with this?
	/**
	 * Transforms this axiom to an equivalent set of OWLSubClassOfAxiom axioms.
	 * <code>( C1 and C2 ) subClassOf Bottom</code> <br>
	 * For each pair of disjoint classes in the OWLDisjointClassesAxiom, create a
	 * new OWLSubClassOfAxiom axiom.
	 *
	 * @param axiom axiom to normalize to a set of OWLSubClassOfAxiom axioms.
	 */
	public static Set<OWLSubClassOfAxiom> disjointClassesAxToSubClassOfAxs(final OWLDisjointClassesAxiom axiom) {
		final Set<OWLSubClassOfAxiom> subClassOfAxioms = new HashSet<OWLSubClassOfAxiom>();

		for (final OWLDisjointClassesAxiom pairwiseDisjointClassesAxiom : axiom.asPairwiseAxioms()) {
			if (pairwiseDisjointClassesAxiom.classExpressions().count() != 2) {
				throw new RuntimeException("Expected Pairwise disjoint classes axiom: " + pairwiseDisjointClassesAxiom);
			}
			/* Normalize to ( C1 and C2 ) subClassOf Bottom */
			final OWLObjectIntersectionOf intersection = Utils.factory
					.getOWLObjectIntersectionOf(pairwiseDisjointClassesAxiom.classExpressions());
			subClassOfAxioms.add(Utils.factory.getOWLSubClassOfAxiom(intersection, InternalizedEntities.OWL_NOTHING));
		}
		return subClassOfAxioms;
	}

	private static Set<OWLSubClassOfAxiom> equivalentClassesAxToSubClassOfAxs(
			final OWLEquivalentClassesAxiom equivalentClassesAxiom) {
		final Set<OWLSubClassOfAxiom> subClassOfAxs = new HashSet<>();
		final Set<OWLClassExpression> equivalentClasses = equivalentClassesAxiom.classExpressions()
				.collect(Collectors.toSet());
		for (final OWLClassExpression equivalentClassi : equivalentClasses)
			for (final OWLClassExpression equivalentClassj : equivalentClasses)
				if (!equivalentClassi.equals(equivalentClassj))
					subClassOfAxs.add(Utils.factory.getOWLSubClassOfAxiom(equivalentClassi, equivalentClassj));
		return subClassOfAxs;
	}

}