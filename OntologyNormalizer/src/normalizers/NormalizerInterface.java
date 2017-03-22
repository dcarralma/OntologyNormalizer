package normalizers;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

public interface NormalizerInterface {

	/**
	 * Filters out
	 * <ul>
	 * <li>non-logical axioms</li>
	 * <li>axioms containing datatypes</li>
	 * <li>axioms containing data properties</li>
	 * <li>axioms containing swrl builtin atoms</li>
	 * <li>axioms containing empty oneOf, unionOf or intersectionOf collections</li>
	 * </ul>
	 * 
	 * Normalizes remaining axioms to the following format: A1 sqcap ... sqcap An sqsubseteq B1 sqcup ... sqcup Bm, A sqs forall R.B, A sqs exists R.Self, exists R.Self sqs B, A
	 * sqs >= n R.B, A sqs <= n R.B, A sqs {a1} sqcup ... sqcup {an}, R sqs S, R1 o ... o Rn sqs S, A(a), R(a, b), a1 = ... = an or a1 neq ... neq an where A(i), B are concept
	 * names, R(i), S are (possibly inverse) roles, and a(i), b are named individuals.
	 * 
	 * @param ontology
	 * @return the set of normalized logical axioms of the given ontology
	 */
	Set<OWLLogicalAxiom> filterAndNormalizeAxioms(OWLOntology ontology);

}
