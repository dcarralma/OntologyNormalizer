package nfOntology;

import java.util.HashSet;
import java.util.Set;
import java.io.FileOutputStream;

import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.SWRLRule;

public class NFOntology {

	// A1 sqcap ... sqcap An sqs B1 sqcup ... sqcup Bm, A sqs forall R.B, exists R.Self sqs A, A sqs exists R.Self, A sqs > n R.B, A sqs < n R.B, A sqs {a1} sqcup ... sqcup {an}  
	private Set<OWLSubClassOfAxiom> conjDisjAxs2 = new HashSet<OWLSubClassOfAxiom>();
	private Set<OWLSubClassOfAxiom> univAxs = new HashSet<OWLSubClassOfAxiom>();
	private Set<OWLSubClassOfAxiom> lhsSelfAxs = new HashSet<OWLSubClassOfAxiom>();
	private Set<OWLSubClassOfAxiom> rhsSelfAxs = new HashSet<OWLSubClassOfAxiom>();
	private Set<OWLSubClassOfAxiom> atLeastAxs = new HashSet<OWLSubClassOfAxiom>();
	private Set<OWLSubClassOfAxiom> atMostAxs = new HashSet<OWLSubClassOfAxiom>();
	private Set<OWLSubClassOfAxiom> nominalAxs = new HashSet<OWLSubClassOfAxiom>();
	// R sqs S, R1 o ... o Rn sqs S, R1 sqcap ... sqcap Rn sqs Bot
	private Set<OWLSubObjectPropertyOfAxiom> simpleObjPropInclusionAxs = new HashSet<OWLSubObjectPropertyOfAxiom>();
	private Set<OWLSubPropertyChainOfAxiom> complexObjPropInclusionAxs = new HashSet<OWLSubPropertyChainOfAxiom>();
	private Set<OWLDisjointObjectPropertiesAxiom> disjointObjPropAxs = new HashSet<OWLDisjointObjectPropertiesAxiom>();
	// Rules
	private Set<SWRLRule> swrlRules = new HashSet<SWRLRule>();
	// A(a), R(a, b), ai = aj, ai neq aj
	Set<OWLClassAssertionAxiom> classAsss = new HashSet<OWLClassAssertionAxiom>();
	Set<OWLObjectPropertyAssertionAxiom> objPropAsss = new HashSet<OWLObjectPropertyAssertionAxiom>();
	Set<OWLSameIndividualAxiom> sameIndAsss = new HashSet<OWLSameIndividualAxiom>();
	Set<OWLDifferentIndividualsAxiom> differentIndAsss = new HashSet<OWLDifferentIndividualsAxiom>();

	public NFOntology(Set<OWLSubClassOfAxiom> inputNormalizedSubClassOfAxs, Set<OWLSubObjectPropertyOfAxiom> inputObjPropInclusionAxs,
		Set<OWLSubPropertyChainOfAxiom> inputComplexObjPropInclusionAxs, Set<OWLDisjointObjectPropertiesAxiom> inputDisjointObjPropAxs, Set<SWRLRule> inputSWRLRules,
		Set<OWLClassAssertionAxiom> inputClassAsss, Set<OWLObjectPropertyAssertionAxiom> inputObjPropAsss, Set<OWLSameIndividualAxiom> inputSameIndAsss,
		Set<OWLDifferentIndividualsAxiom> inputDifferentIndAsss) {
		for (OWLSubClassOfAxiom subClassOfAx : inputNormalizedSubClassOfAxs)
			if (!assignSubClassOfAx(subClassOfAx)) {
				System.out.println("WARNING!!! Unrecognized subclass of axiom at assignSubClassOfAxiom at NFDLOntology.java");
				System.out.println(" -> " + subClassOfAx + "\n" + " -> " + subClassOfAx.getAxiomType() + "\n");
			}
		simpleObjPropInclusionAxs.addAll(inputObjPropInclusionAxs);
		complexObjPropInclusionAxs.addAll(inputComplexObjPropInclusionAxs);
		disjointObjPropAxs.addAll(inputDisjointObjPropAxs);
		swrlRules.addAll(inputSWRLRules);
		classAsss.addAll(inputClassAsss);
		objPropAsss.addAll(inputObjPropAsss);
		sameIndAsss.addAll(inputSameIndAsss);
		differentIndAsss.addAll(inputDifferentIndAsss);
	}

	private boolean assignSubClassOfAx(OWLSubClassOfAxiom subClassOfAxiom) {
		OWLClassExpression subClass = subClassOfAxiom.getSubClass();
		OWLClassExpression superClass = subClassOfAxiom.getSuperClass();

		// A1 sqcap ... sqcap An sqs B1 sqcup ... sqcup Bm
		boolean isConjDisjAxiom = true;
		for (OWLClassExpression conjunct : subClass.asConjunctSet())
			if (!conjunct.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				isConjDisjAxiom = false;
		for (OWLClassExpression disjunct : superClass.asDisjunctSet())
			if (!disjunct.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				isConjDisjAxiom = false;
		if (isConjDisjAxiom)
			return conjDisjAxs2.add(subClassOfAxiom);

		// A sqs forall R.B
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
			if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)
				&& ((OWLObjectAllValuesFrom) superClass).getFiller().getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				return univAxs.add(subClassOfAxiom);

		// exists R.Self sqs A
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_HAS_SELF))
			if (superClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				return lhsSelfAxs.add(subClassOfAxiom);

		// A sqs exists R.Self
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
			if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_HAS_SELF))
				return rhsSelfAxs.add(subClassOfAxiom);

		// A sqs >= n R.B
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
			if ((superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_MIN_CARDINALITY))
				&& ((OWLObjectMinCardinality) superClass).getFiller().getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				return atLeastAxs.add(subClassOfAxiom);

		// A sqs <= n R.B
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
			if ((superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_MAX_CARDINALITY))
				&& ((OWLObjectMaxCardinality) superClass).getFiller().getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)
				&& ((OWLObjectMaxCardinality) superClass).getCardinality() > 0)
				return atMostAxs.add(subClassOfAxiom);

		// A sqs {a1} sqcup ... {an}
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
			if (superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_ONE_OF))
				return nominalAxs.add(subClassOfAxiom);

		return false;
	}

	public Set<OWLAxiom> getTBoxAxioms() {
		Set<OWLAxiom> tBoxAxioms = new HashSet<OWLAxiom>();
		tBoxAxioms.addAll(conjDisjAxs2);
		tBoxAxioms.addAll(univAxs);
		tBoxAxioms.addAll(lhsSelfAxs);
		tBoxAxioms.addAll(rhsSelfAxs);
		tBoxAxioms.addAll(atLeastAxs);
		tBoxAxioms.addAll(atMostAxs);
		tBoxAxioms.addAll(nominalAxs);
		tBoxAxioms.addAll(simpleObjPropInclusionAxs);
		tBoxAxioms.addAll(complexObjPropInclusionAxs);
		tBoxAxioms.addAll(disjointObjPropAxs);
		tBoxAxioms.addAll(swrlRules);
		return tBoxAxioms;
	}

	public Set<OWLAxiom> getABoxAxioms() {
		Set<OWLAxiom> aBoxAxioms = new HashSet<OWLAxiom>();
		aBoxAxioms.addAll(classAsss);
		aBoxAxioms.addAll(objPropAsss);
		aBoxAxioms.addAll(differentIndAsss);
		aBoxAxioms.addAll(sameIndAsss);
		return aBoxAxioms;
	}

	public Set<OWLAxiom> getAxioms() {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		axioms.addAll(getTBoxAxioms());
		axioms.addAll(getABoxAxioms());
		return axioms;
	}

	public void toFile(String filePath) {
		try {
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology owlAPIOntology = manager.createOntology();
			for (OWLAxiom axiom : getAxioms())
				manager.addAxiom(owlAPIOntology, axiom);
			manager.saveOntology(owlAPIOntology, new FileOutputStream(filePath));
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("WARNING!!! Exception at toFile() at NFOntology.java." + "\n");
			e.printStackTrace();
		}
	}
}
