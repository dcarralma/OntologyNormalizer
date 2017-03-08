package normalizers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;

import launcher.Srd;
import nfOntology.NFOntology;
import uk.ac.manchester.cs.owl.owlapi.OWLDisjointObjectPropertiesAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectAllValuesFromImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectHasSelfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectIntersectionOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectMaxCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectSomeValuesFromImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubObjectPropertyOfAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubPropertyChainAxiomImpl;

public class OntologyNormalizer {

	public static NFOntology normalizeOnt(OWLOntology ontology) throws OWLOntologyCreationException {

		// Filter out non-logical axioms
		Stream<OWLLogicalAxiom> logicalAx = ontology.logicalAxioms();

		// Distribute Axioms
		Set<OWLSubClassOfAxiom> subClassOfAxs = new HashSet<OWLSubClassOfAxiom>();
		Set<OWLSubObjectPropertyOfAxiom> simpleObjPropInclusionAxs = new HashSet<OWLSubObjectPropertyOfAxiom>();
		Set<OWLSubPropertyChainOfAxiom> complexObjPropInclusionAxs = new HashSet<OWLSubPropertyChainOfAxiom>();
		Set<OWLDisjointObjectPropertiesAxiom> disjointObjPropAxs = new HashSet<OWLDisjointObjectPropertiesAxiom>();
		Set<SWRLRule> swrlRules = new HashSet<SWRLRule>();
		Set<OWLClassAssertionAxiom> classAsss = new HashSet<OWLClassAssertionAxiom>();
		Set<OWLObjectPropertyAssertionAxiom> objPropAsss = new HashSet<OWLObjectPropertyAssertionAxiom>();
		Set<OWLNegativeObjectPropertyAssertionAxiom> negativeObjPropAsss = new HashSet<OWLNegativeObjectPropertyAssertionAxiom>();
		Set<OWLSameIndividualAxiom> sameIndsAsss = new HashSet<OWLSameIndividualAxiom>();
		Set<OWLDifferentIndividualsAxiom> differentIndsAsss = new HashSet<OWLDifferentIndividualsAxiom>();
		distributeAndFilterDataAxs(logicalAx, subClassOfAxs, simpleObjPropInclusionAxs, complexObjPropInclusionAxs, disjointObjPropAxs, swrlRules, classAsss, objPropAsss,
			negativeObjPropAsss, differentIndsAsss, sameIndsAsss);

		// Normalize SWRLRules
		SWRLRulesNormalizer.normalizeSWRLRules(swrlRules, subClassOfAxs);

		// Normalize ABoxAxioms
		ABoxNormalizer.normalizeClassAsss(classAsss, subClassOfAxs);
		ABoxNormalizer.normalizeNegativeObjPropAsss(negativeObjPropAsss, objPropAsss, disjointObjPropAxs);

		// Normalize SubClassOfAxioms
		SubClassOfAxNormalizer.normalizeSubClassOfAx(subClassOfAxs, classAsss);

		return (new NFOntology(subClassOfAxs, simpleObjPropInclusionAxs, complexObjPropInclusionAxs, disjointObjPropAxs, swrlRules, classAsss, objPropAsss, sameIndsAsss,
			differentIndsAsss));
	}

	private static void distributeAndFilterDataAxs(Stream<OWLLogicalAxiom> logicalAxs, Set<OWLSubClassOfAxiom> subClassOfAxs,
		Set<OWLSubObjectPropertyOfAxiom> simpleObjPropInclusionAxs, Set<OWLSubPropertyChainOfAxiom> complexObjPropInclusionAxs,
		Set<OWLDisjointObjectPropertiesAxiom> disjointObjPropAxioms, Set<SWRLRule> swrlRules, Set<OWLClassAssertionAxiom> classAsss,
		Set<OWLObjectPropertyAssertionAxiom> objPropAsss, Set<OWLNegativeObjectPropertyAssertionAxiom> negativeObjPropAsss, Set<OWLDifferentIndividualsAxiom> differentIndsAsss,
		Set<OWLSameIndividualAxiom> sameIndsAsss) {

		logicalAxs.forEach(logicalAx -> {
			if (logicalAx.dataPropertiesInSignature().count() == 0 && logicalAx.datatypesInSignature().count() == 0)
				switch (logicalAx.getAxiomType().toString()) {
				// TBox
				case "DisjointClasses":
					subClassOfAxs.addAll(disjointClassesAxToSubClassOfAxs((OWLDisjointClassesAxiom) logicalAx));
					break;

				case "DisjointUnion":
					OWLDisjointUnionAxiom disjointUnionAxiom = (OWLDisjointUnionAxiom) logicalAx;
					subClassOfAxs.addAll(disjointClassesAxToSubClassOfAxs(disjointUnionAxiom.getOWLDisjointClassesAxiom()));
					subClassOfAxs.addAll(equivalentClassesAxToSubClassOfAxs(disjointUnionAxiom.getOWLEquivalentClassesAxiom()));
					break;

				case "InverseFunctionalObjectProperty":
					subClassOfAxs.add(new OWLSubClassOfAxiomImpl(Srd.factory.getOWLThing(),
						new OWLObjectMaxCardinalityImpl(((OWLInverseFunctionalObjectPropertyAxiom) logicalAx).getProperty().getInverseProperty(), 1, Srd.factory.getOWLThing()),
						new HashSet<OWLAnnotation>()));
					break;

				case "IrrefexiveObjectProperty":
					subClassOfAxs.add(new OWLSubClassOfAxiomImpl(new OWLObjectHasSelfImpl(((OWLIrreflexiveObjectPropertyAxiom) logicalAx).getProperty()), Srd.factory.getOWLNothing(),
						new HashSet<OWLAnnotation>()));
					break;

				case "EquivalentClasses":
					subClassOfAxs.addAll(equivalentClassesAxToSubClassOfAxs((OWLEquivalentClassesAxiom) logicalAx));
					break;

				case "FunctionalObjectProperty":
					subClassOfAxs.add(new OWLSubClassOfAxiomImpl(Srd.factory.getOWLThing(),
						new OWLObjectMaxCardinalityImpl(((OWLFunctionalObjectPropertyAxiom) logicalAx).getProperty(), 1, Srd.factory.getOWLThing()), new HashSet<OWLAnnotation>()));
					break;

				case "ObjectPropertyRange":
					OWLObjectPropertyRangeAxiom objPropRangeAxiom = (OWLObjectPropertyRangeAxiom) logicalAx;
					subClassOfAxs.add(new OWLSubClassOfAxiomImpl(Srd.factory.getOWLThing(),
						new OWLObjectAllValuesFromImpl(objPropRangeAxiom.getProperty(), objPropRangeAxiom.getRange()), new HashSet<OWLAnnotation>()));
					break;

				case "ObjectPropertyDomain":
					OWLObjectPropertyDomainAxiom domainObjPropRangeAxiom = (OWLObjectPropertyDomainAxiom) logicalAx;
					subClassOfAxs.add(new OWLSubClassOfAxiomImpl(new OWLObjectSomeValuesFromImpl(domainObjPropRangeAxiom.getProperty(), Srd.factory.getOWLThing()),
						domainObjPropRangeAxiom.getDomain(), new HashSet<OWLAnnotation>()));
					break;

				case "ReflexiveObjectProperty":
					subClassOfAxs.add(new OWLSubClassOfAxiomImpl(Srd.factory.getOWLThing(), new OWLObjectHasSelfImpl(((OWLReflexiveObjectPropertyAxiom) logicalAx).getProperty()),
						new HashSet<OWLAnnotation>()));
					break;

				case "SubClassOf":
					subClassOfAxs.add((OWLSubClassOfAxiom) logicalAx);
					break;

				// RBox
				case "AsymmetricObjectProperty":
					OWLObjectPropertyExpression asymmetricObjProp = ((OWLAsymmetricObjectPropertyAxiom) logicalAx).getProperty();
					disjointObjPropAxioms
						.add(new OWLDisjointObjectPropertiesAxiomImpl(Srd.toSet(asymmetricObjProp, asymmetricObjProp.getInverseProperty()), new HashSet<OWLAnnotation>()));
					break;

				case "DisjointObjectProperties":
					disjointObjPropAxioms.add((OWLDisjointObjectPropertiesAxiom) logicalAx);
					break;

				case "EquivalentObjectProperties":
					Set<OWLObjectPropertyExpression> equivalentObjProps = ((OWLEquivalentObjectPropertiesAxiom) logicalAx).properties().collect(Collectors.toSet());
					for (OWLObjectPropertyExpression equivalentObjPropi : equivalentObjProps)
						for (OWLObjectPropertyExpression equivalentObjPropj : equivalentObjProps)
							if (!equivalentObjPropi.equals(equivalentObjPropj))
								simpleObjPropInclusionAxs.add(new OWLSubObjectPropertyOfAxiomImpl(equivalentObjPropi, equivalentObjPropj, new HashSet<OWLAnnotation>()));
					break;

				case "InverseObjectProperties":
					OWLInverseObjectPropertiesAxiom invObjPropAx = (OWLInverseObjectPropertiesAxiom) logicalAx;
					simpleObjPropInclusionAxs
						.add(new OWLSubObjectPropertyOfAxiomImpl(invObjPropAx.getFirstProperty().getInverseProperty(), invObjPropAx.getSecondProperty(), new HashSet<OWLAnnotation>()));
					simpleObjPropInclusionAxs
						.add(new OWLSubObjectPropertyOfAxiomImpl(invObjPropAx.getSecondProperty().getInverseProperty(), invObjPropAx.getFirstProperty(), new HashSet<OWLAnnotation>()));
					break;

				case "SubObjectPropertyOf":
					simpleObjPropInclusionAxs.add((OWLSubObjectPropertyOfAxiom) logicalAx);
					break;

				case "SymmetricObjectProperty":
					OWLObjectPropertyExpression symmetrictObjProp = ((OWLSymmetricObjectPropertyAxiom) logicalAx).getProperty();
					simpleObjPropInclusionAxs.add(new OWLSubObjectPropertyOfAxiomImpl(symmetrictObjProp.getInverseProperty(), symmetrictObjProp, new HashSet<OWLAnnotation>()));
					break;

				case "SubPropertyChainOf":
					complexObjPropInclusionAxs.add((OWLSubPropertyChainOfAxiom) logicalAx);
					break;

				case "TransitiveObjectProperty":
					OWLObjectPropertyExpression transitiveObjProp = ((OWLTransitiveObjectPropertyAxiom) logicalAx).getProperty();
					complexObjPropInclusionAxs.add(new OWLSubPropertyChainAxiomImpl(Srd.toList(transitiveObjProp, transitiveObjProp), transitiveObjProp, new HashSet<OWLAnnotation>()));
					break;

				// Rules
				case "Rule":
					swrlRules.add((SWRLRule) logicalAx);
					break;

				// ABox
				case "ClassAssertion":
					classAsss.add((OWLClassAssertionAxiom) logicalAx);
					break;

				case "DifferentIndividuals":
					differentIndsAsss.add((OWLDifferentIndividualsAxiom) logicalAx);
					break;

				case "NegativeObjectPropertyAssertion":
					negativeObjPropAsss.add((OWLNegativeObjectPropertyAssertionAxiom) logicalAx);
					break;

				case "ObjectPropertyAssertion":
					objPropAsss.add((OWLObjectPropertyAssertionAxiom) logicalAx);
					break;

				case "SameIndividual":
					sameIndsAsss.add((OWLSameIndividualAxiom) logicalAx);
					break;

				default:
					System.out.println("WARNING!!! Unrecognized type of Logical Axiom at normalizeOntology at MainNormalizer.java.");
					System.out.println(" -> " + logicalAx.getAxiomType().toString());
					System.out.println(" -> " + logicalAx + "\n");
				}
		});

	}

	private static Set<OWLSubClassOfAxiom> disjointClassesAxToSubClassOfAxs(OWLDisjointClassesAxiom disjointClassesAxiom) {
		Set<OWLSubClassOfAxiom> subClassOfAxs = new HashSet<OWLSubClassOfAxiom>();
		List<OWLClassExpression> disjointClasses = disjointClassesAxiom.classExpressions().collect(Collectors.toList());
		for (int i = 0; i < disjointClasses.size(); i++)
			for (int j = i + 1; j < disjointClasses.size(); j++)
				subClassOfAxs.add(new OWLSubClassOfAxiomImpl(new OWLObjectIntersectionOfImpl(Srd.toSet(disjointClasses.get(i), disjointClasses.get(j)).stream()),
					Srd.factory.getOWLNothing(), new HashSet<OWLAnnotation>()));
		return subClassOfAxs;
	}

	private static Set<OWLSubClassOfAxiom> equivalentClassesAxToSubClassOfAxs(OWLEquivalentClassesAxiom equivalentClassesAxiom) {
		Set<OWLSubClassOfAxiom> subClassOfAxs = new HashSet<OWLSubClassOfAxiom>();
		Set<OWLClassExpression> equivalentClasses = equivalentClassesAxiom.classExpressions().collect(Collectors.toSet());
		for (OWLClassExpression equivalentClassi : equivalentClasses)
			for (OWLClassExpression equivalentClassj : equivalentClasses)
				if (!equivalentClassi.equals(equivalentClassj))
					subClassOfAxs.add(new OWLSubClassOfAxiomImpl(equivalentClassi, equivalentClassj, new HashSet<OWLAnnotation>()));
		return subClassOfAxs;
	}
}