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
import org.semanticweb.owlapi.model.OWLAnnotation;
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
import uk.ac.manchester.cs.owl.owlapi.OWLDisjointObjectPropertiesAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectAllValuesFromImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectHasSelfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectIntersectionOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectMaxCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectSomeValuesFromImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubObjectPropertyOfAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubPropertyChainAxiomImpl;

public class MainNormalizer implements NormalizerInterface {

	// Distribute Axioms
	private final Set<OWLSubClassOfAxiom> subClassOfAxs = new HashSet<OWLSubClassOfAxiom>();
	private final Set<OWLSubObjectPropertyOfAxiom> simpleObjPropInclusionAxs = new HashSet<>();
	private final Set<OWLSubPropertyChainOfAxiom> complexObjPropInclusionAxs = new HashSet<>();
	private final Set<OWLDisjointObjectPropertiesAxiom> disjointObjPropAxs = new HashSet<>();
	private final Set<SWRLRule> swrlRules = new HashSet<>();
	private final Set<OWLClassAssertionAxiom> classAsss = new HashSet<OWLClassAssertionAxiom>();
	private final Set<OWLObjectPropertyAssertionAxiom> objPropAsss = new HashSet<OWLObjectPropertyAssertionAxiom>();
	private final Set<OWLNegativeObjectPropertyAssertionAxiom> negativeObjPropAsss = new HashSet<>();
	private final Set<OWLSameIndividualAxiom> sameIndsAsss = new HashSet<OWLSameIndividualAxiom>();
	private final Set<OWLDifferentIndividualsAxiom> differentIndsAsss = new HashSet<OWLDifferentIndividualsAxiom>();

	@Override
	public Set<OWLLogicalAxiom> filterAndNormalizeAxioms(final OWLOntology ontology) {

		// Filter out non-logical axioms
		this.distributeAndFilterDataAxs(ontology.logicalAxioms());

		// Normalize SWRLRules
		SWRLRulesNormalizer.normalizeSWRLRules(this.swrlRules, this.subClassOfAxs);

		// Normalize ABoxAxioms
		ABoxNormalizer.normalizeClassAsss(this.classAsss, this.subClassOfAxs);
		ABoxNormalizer.normalizeNegativeObjPropAsss(this.negativeObjPropAsss, this.objPropAsss, this.disjointObjPropAxs);

		// Normalize SubClassOfAxioms
		SubClassOfAxNormalizer.normalizeSubClassOfAx(this.subClassOfAxs, this.classAsss);

		return this.collectNormalizedAxioms();
	}

	private Set<OWLLogicalAxiom> collectNormalizedAxioms() {
		final Set<OWLLogicalAxiom> normalizedAxioms = new HashSet<OWLLogicalAxiom>();
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
			normalizeLogicalAxiom(logicalAx);
		});

	}

	private void normalizeLogicalAxiom(OWLLogicalAxiom logicalAx) {
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
			this.subClassOfAxs.add(new OWLSubClassOfAxiomImpl(Utils.factory.getOWLThing(),
					new OWLObjectMaxCardinalityImpl(((OWLInverseFunctionalObjectPropertyAxiom) logicalAx)
							.getProperty().getInverseProperty(), 1, Utils.factory.getOWLThing()),
					new HashSet<OWLAnnotation>()));
			break;

		case "IrrefexiveObjectProperty":
			this.subClassOfAxs.add(new OWLSubClassOfAxiomImpl(
					new OWLObjectHasSelfImpl(((OWLIrreflexiveObjectPropertyAxiom) logicalAx).getProperty()),
					Utils.factory.getOWLNothing(), new HashSet<OWLAnnotation>()));
			break;

		case "EquivalentClasses":
			this.subClassOfAxs.addAll(equivalentClassesAxToSubClassOfAxs((OWLEquivalentClassesAxiom) logicalAx));
			break;

		case "FunctionalObjectProperty":
			this.subClassOfAxs.add(new OWLSubClassOfAxiomImpl(Utils.factory.getOWLThing(),
					new OWLObjectMaxCardinalityImpl(((OWLFunctionalObjectPropertyAxiom) logicalAx).getProperty(), 1,
							Utils.factory.getOWLThing()),
					new HashSet<OWLAnnotation>()));
			break;

		case "ObjectPropertyRange":
			final OWLObjectPropertyRangeAxiom objPropRangeAxiom = (OWLObjectPropertyRangeAxiom) logicalAx;
			this.subClassOfAxs.add(new OWLSubClassOfAxiomImpl(Utils.factory.getOWLThing(),
					new OWLObjectAllValuesFromImpl(objPropRangeAxiom.getProperty(), objPropRangeAxiom.getRange()),
					new HashSet<OWLAnnotation>()));
			break;

		case "ObjectPropertyDomain":
			final OWLObjectPropertyDomainAxiom domainObjPropRangeAxiom = (OWLObjectPropertyDomainAxiom) logicalAx;
			this.subClassOfAxs.add(new OWLSubClassOfAxiomImpl(
					new OWLObjectSomeValuesFromImpl(domainObjPropRangeAxiom.getProperty(),
							Utils.factory.getOWLThing()),
					domainObjPropRangeAxiom.getDomain(), new HashSet<OWLAnnotation>()));
			break;

		case "ReflexiveObjectProperty":
			this.subClassOfAxs.add(new OWLSubClassOfAxiomImpl(Utils.factory.getOWLThing(),
					new OWLObjectHasSelfImpl(((OWLReflexiveObjectPropertyAxiom) logicalAx).getProperty()),
					new HashSet<OWLAnnotation>()));
			break;

		case "SubClassOf":
			this.subClassOfAxs.add((OWLSubClassOfAxiom) logicalAx);
			break;

		// RBox
		case "AsymmetricObjectProperty":
			final OWLObjectPropertyExpression asymmetricObjProp = ((OWLAsymmetricObjectPropertyAxiom) logicalAx)
					.getProperty();
			this.disjointObjPropAxs.add(new OWLDisjointObjectPropertiesAxiomImpl(
					Utils.toSet(asymmetricObjProp, asymmetricObjProp.getInverseProperty()),
					new HashSet<OWLAnnotation>()));
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
						this.simpleObjPropInclusionAxs.add(new OWLSubObjectPropertyOfAxiomImpl(equivalentObjPropi,
								equivalentObjPropj, new HashSet<OWLAnnotation>()));
			break;

		case "InverseObjectProperties":
			final OWLInverseObjectPropertiesAxiom invObjPropAx = (OWLInverseObjectPropertiesAxiom) logicalAx;
			this.simpleObjPropInclusionAxs
					.add(new OWLSubObjectPropertyOfAxiomImpl(invObjPropAx.getFirstProperty().getInverseProperty(),
							invObjPropAx.getSecondProperty(), new HashSet<OWLAnnotation>()));
			this.simpleObjPropInclusionAxs
					.add(new OWLSubObjectPropertyOfAxiomImpl(invObjPropAx.getSecondProperty().getInverseProperty(),
							invObjPropAx.getFirstProperty(), new HashSet<OWLAnnotation>()));
			break;

		case "SubObjectPropertyOf":
			this.simpleObjPropInclusionAxs.add((OWLSubObjectPropertyOfAxiom) logicalAx);
			break;

		case "SymmetricObjectProperty":
			final OWLObjectPropertyExpression symmetrictObjProp = ((OWLSymmetricObjectPropertyAxiom) logicalAx)
					.getProperty();
			this.simpleObjPropInclusionAxs.add(new OWLSubObjectPropertyOfAxiomImpl(
					symmetrictObjProp.getInverseProperty(), symmetrictObjProp, new HashSet<OWLAnnotation>()));
			break;

		case "SubPropertyChainOf":
			this.complexObjPropInclusionAxs.add((OWLSubPropertyChainOfAxiom) logicalAx);
			break;

		case "TransitiveObjectProperty":
			final OWLObjectPropertyExpression transitiveObjProp = ((OWLTransitiveObjectPropertyAxiom) logicalAx)
					.getProperty();
			this.complexObjPropInclusionAxs
					.add(new OWLSubPropertyChainAxiomImpl(Arrays.asList(transitiveObjProp, transitiveObjProp),
							transitiveObjProp, new HashSet<OWLAnnotation>()));

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
			System.out.println(" -> " + logicalAx.getAxiomType().toString());
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
		// empty one of: <owl:oneOf
		// rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"/>
		// empty union: <owl:unionOf
		// rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"/>
		// empty intersection: <owl:intersectionOf
		// rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"/>
		final ClassExpressionType[] forbiddenEmptyClassExpressionTypes = { ClassExpressionType.OBJECT_ONE_OF,
				ClassExpressionType.OBJECT_UNION_OF, ClassExpressionType.OBJECT_INTERSECTION_OF };
		final Predicate<OWLClassExpression> containsEmptyExpression = expression -> Arrays
				.asList(forbiddenEmptyClassExpressionTypes).contains(expression.getClassExpressionType())
				&& ((HasOperands<?>) expression).operands().count() == 0;

		final boolean containsData = logicalAx.dataPropertiesInSignature().count() != 0
				|| logicalAx.datatypesInSignature().count() != 0;

		return containsData || logicalAx.nestedClassExpressions().anyMatch(containsEmptyExpression);
	}

	private static Set<OWLSubClassOfAxiom> disjointClassesAxToSubClassOfAxs(
			final OWLDisjointClassesAxiom disjointClassesAxiom) {
		final Set<OWLSubClassOfAxiom> subClassOfAxs = new HashSet<OWLSubClassOfAxiom>();
		final List<OWLClassExpression> disjointClasses = disjointClassesAxiom.classExpressions()
				.collect(Collectors.toList());
		for (int i = 0; i < disjointClasses.size(); i++)
			for (int j = i + 1; j < disjointClasses.size(); j++)
				subClassOfAxs.add(new OWLSubClassOfAxiomImpl(
						new OWLObjectIntersectionOfImpl(
								Utils.toSet(disjointClasses.get(i), disjointClasses.get(j)).stream()),
						Utils.factory.getOWLNothing(), new HashSet<OWLAnnotation>()));
		return subClassOfAxs;
	}

	private static Set<OWLSubClassOfAxiom> equivalentClassesAxToSubClassOfAxs(
			final OWLEquivalentClassesAxiom equivalentClassesAxiom) {
		final Set<OWLSubClassOfAxiom> subClassOfAxs = new HashSet<OWLSubClassOfAxiom>();
		final Set<OWLClassExpression> equivalentClasses = equivalentClassesAxiom.classExpressions()
				.collect(Collectors.toSet());
		for (final OWLClassExpression equivalentClassi : equivalentClasses)
			for (final OWLClassExpression equivalentClassj : equivalentClasses)
				if (!equivalentClassi.equals(equivalentClassj))
					subClassOfAxs.add(new OWLSubClassOfAxiomImpl(equivalentClassi, equivalentClassj,
							new HashSet<OWLAnnotation>()));
		return subClassOfAxs;
	}

}