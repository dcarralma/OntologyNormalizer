package normalizers;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

			final Set<SWRLAtom> normalizedBody = new HashSet<>();
			final boolean bodyContainsBuiltInAtom = normalizeSWRLRuleConjunction(rule.body(), normalizedBody, true,
					subClassOfAxioms);

			final Set<SWRLAtom> normalizedHead = new HashSet<>();
			final boolean headCotainsBuiltInAtoms = normalizeSWRLRuleConjunction(rule.head(), normalizedHead, false,
					subClassOfAxioms);

			if (!bodyContainsBuiltInAtom && !headCotainsBuiltInAtoms) {
				rules.add(Utils.factory.getSWRLRule(normalizedBody, normalizedHead));
			} else {
				System.out.println(
						"WARNING!!! Unrecognized built in SWRL rule atom! The following SWRL rule is ignored: ");
				System.out.println(" -> " + rule);
			}
		}
	}

	private static boolean normalizeSWRLRuleConjunction(final Stream<SWRLAtom> toNormalize,
			final Set<SWRLAtom> normalized, final boolean isBody, final Set<OWLSubClassOfAxiom> subClassOfAxioms) {
		boolean containsBuiltInAtom = false;

		for (final SWRLAtom atom : toNormalize.collect(Collectors.toSet())) {
			if (atom instanceof SWRLClassAtom && (atom.getPredicate() instanceof OWLClass)) {
				final OWLClassExpression classExpPred = (OWLClassExpression) atom.getPredicate();
				final OWLClassExpression freshClass = Utils.getCorrespondingFreshClass(classExpPred);
				normalized.add(Utils.factory.getSWRLClassAtom(freshClass, ((SWRLClassAtom) atom).getArgument()));

				final OWLSubClassOfAxiom owlSubClassOfAxiom;
				if (isBody) {
					owlSubClassOfAxiom = Utils.factory.getOWLSubClassOfAxiom(classExpPred, freshClass);
				} else {
					owlSubClassOfAxiom = Utils.factory.getOWLSubClassOfAxiom(freshClass, classExpPred);
				}

				subClassOfAxioms.add(owlSubClassOfAxiom);
			} else if (atom instanceof SWRLBuiltInAtom) {
				containsBuiltInAtom = true;
			} else {
				normalized.add(atom);
			}
		}
		return containsBuiltInAtom;
	}

}
