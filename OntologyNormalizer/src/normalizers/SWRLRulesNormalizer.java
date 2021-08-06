package normalizers;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLBuiltInAtom;
import org.semanticweb.owlapi.model.SWRLClassAtom;
import org.semanticweb.owlapi.model.SWRLRule;

import launcher.Utils;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.SWRLClassAtomImpl;
import uk.ac.manchester.cs.owl.owlapi.SWRLRuleImpl;

public class SWRLRulesNormalizer {

	public static void normalizeSWRLRules(Set<SWRLRule> rules, Set<OWLSubClassOfAxiom> subClassOfAxioms) {
		Set<SWRLRule> rulesCopy = new HashSet<SWRLRule>();
		rulesCopy.addAll(rules);
		rules.clear();

		for (SWRLRule rule : rulesCopy) {
			boolean containsBuiltInAtom = false;

			Set<SWRLAtom> body = new HashSet<SWRLAtom>();
			for (SWRLAtom bodyAtom : rule.body().collect(Collectors.toSet()))
				if (bodyAtom instanceof SWRLClassAtom && (bodyAtom.getPredicate() instanceof OWLClass)) {
					OWLClassExpression classExpPred = (OWLClassExpression) bodyAtom.getPredicate();
					subClassOfAxioms.add(new OWLSubClassOfAxiomImpl(classExpPred, Utils.getCorrespondingFreshClass(classExpPred), new HashSet<OWLAnnotation>()));
					body.add(new SWRLClassAtomImpl(Utils.getCorrespondingFreshClass(classExpPred), ((SWRLClassAtom) bodyAtom).getArgument()));
				} else if (bodyAtom instanceof SWRLBuiltInAtom)
					containsBuiltInAtom = true;
				else
					body.add(bodyAtom);

			Set<SWRLAtom> head = new HashSet<SWRLAtom>();
			for (SWRLAtom headAtom : rule.head().collect(Collectors.toSet()))
				if (headAtom instanceof SWRLClassAtom && !(headAtom.getPredicate() instanceof OWLClass)) {
					OWLClassExpression classExpPred = (OWLClassExpression) headAtom.getPredicate();
					subClassOfAxioms.add(new OWLSubClassOfAxiomImpl(Utils.getCorrespondingFreshClass(classExpPred), classExpPred, new HashSet<OWLAnnotation>()));
					head.add(new SWRLClassAtomImpl(Utils.getCorrespondingFreshClass(classExpPred), ((SWRLClassAtom) headAtom).getArgument()));
				} else if (headAtom instanceof SWRLBuiltInAtom)
					containsBuiltInAtom = true;
				else
					head.add(headAtom);

			if (!containsBuiltInAtom)
				rules.add(new SWRLRuleImpl(body, head, new HashSet<OWLAnnotation>()));
		}
	}
}
