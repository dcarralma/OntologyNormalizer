package normalizers;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLBuiltInAtom;
import org.semanticweb.owlapi.model.SWRLClassAtom;
import org.semanticweb.owlapi.model.SWRLRule;

import launcher.Utils;

public class SWRLRulesNormalizer {

	public static void normalizeSWRLRules(final Set<SWRLRule> rules, final Set<OWLSubClassOfAxiom> subClassOfAxioms) {
		final Set<SWRLRule> rulesCopy = new HashSet<>();
		rulesCopy.addAll(rules);
		rules.clear();

		for (final SWRLRule rule : rulesCopy) {
			boolean containsBuiltInAtom = false;

			final Set<SWRLAtom> body = new HashSet<>();
			for (final SWRLAtom bodyAtom : rule.body().collect(Collectors.toSet())) {
				if (bodyAtom instanceof SWRLClassAtom && (bodyAtom.getPredicate() instanceof OWLClass)) {
					final OWLClassExpression classExpPred = (OWLClassExpression) bodyAtom.getPredicate();
					final OWLClassExpression freshClass = Utils.getCorrespondingFreshClass(classExpPred);
					subClassOfAxioms.add(Utils.factory.getOWLSubClassOfAxiom(classExpPred,
							freshClass));
					body.add(Utils.factory.getSWRLClassAtom(freshClass,
							((SWRLClassAtom) bodyAtom).getArgument()));
				} else if (bodyAtom instanceof SWRLBuiltInAtom) {
					containsBuiltInAtom = true;
				} else {
					body.add(bodyAtom);
				}
			}

			final Set<SWRLAtom> head = new HashSet<>();
			for (final SWRLAtom headAtom : rule.head().collect(Collectors.toSet())) {
				if (headAtom instanceof SWRLClassAtom && !(headAtom.getPredicate() instanceof OWLClass)) {
					final OWLClassExpression classExpPred = (OWLClassExpression) headAtom.getPredicate();
					final OWLClassExpression freshClass = Utils.getCorrespondingFreshClass(classExpPred);
					subClassOfAxioms.add(Utils.factory
							.getOWLSubClassOfAxiom(freshClass, classExpPred));
					head.add(Utils.factory.getSWRLClassAtom(freshClass,
							((SWRLClassAtom) headAtom).getArgument()));
				} else if (headAtom instanceof SWRLBuiltInAtom) {
					containsBuiltInAtom = true;
				} else {
					head.add(headAtom);
				}
			}

			if (!containsBuiltInAtom) {
				rules.add(Utils.factory.getSWRLRule(body, head));
			} else {
				System.out.println(
						"WARNING!!! Unrecognized built in SWRL rule atom! The following SWRL rule is ignored: ");
				System.out.println(" -> " + rule);
			}
		}
	}
}
