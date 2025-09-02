package apollo.kotlin.compiler.plugin.fir

import apollo.kotlin.compiler.plugin.ClassIds
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate

object FirPredicates {
  val Query = LookupPredicate.create {
    annotated(ClassIds.Query.asSingleFqName())
  }
  val Mutation = LookupPredicate.create {
    annotated(ClassIds.Mutation.asSingleFqName())
  }
  val Schema = LookupPredicate.create {
    annotated(ClassIds.Schema.asSingleFqName())
  }
  val Subscription = LookupPredicate.create {
    annotated(ClassIds.Subscription.asSingleFqName())
  }
  val Fragment = LookupPredicate.create {
    annotated(ClassIds.Fragment.asSingleFqName())
  }
  val Scalar = LookupPredicate.create {
    annotated(ClassIds.Scalar.asSingleFqName())
  }
}
