# OntologyNormalizer
Eclipse project implementing a normalization algorithm for OWL ontologies.

The program takes two arguments:
 1. The path of the input ontology file (this must be a valid OWL file).
 2. The path where the normalized ontology will be stored.

Note the following comments:
1. All axioms in the input ontology which are not logical axioms, or contain datatype properties, datatypes or builtin atoms are ignored.
2. All axioms in the normalized ontology are of one of the following forms:
  A1 sqcap ... sqcap An sqsubseteq B1 sqcup ... sqcup Bm
  A sqs forall R.B
  A sqs exists R.Self
  exists R.Self sqs B
  A sqs >= n R.B
  A sqs <= n R.B
  A sqs {a1} sqcup ... sqcup {an}
  R sqs S
  R1 o ... o Rn sqs S
  A(a)
  R(a, b)
  a1 = ... = an
  a1 neq ... neq an
Where A(i), B are concept names; R(i), S are (possibly inverse) roles and a(i), b are named individuals.
3. SWRLRules in the normalized ontology only feature atoms of the form A(t) or R(t, u) where A is a concept name, R is a (possibly inverse) role and t, u are variables or named individuals.