package apollo.kotlin.compat

import com.apollographql.apollo.api.Query as ApolloQuery
import com.apollographql.apollo.api.Mutation as ApolloMutation
import com.apollographql.apollo.api.Subscription as ApolloSubscription
import com.apollographql.apollo.api.Fragment as ApolloFragment

interface QueryCompat<D> : ApolloQuery<D>, OperationCompat<D> where D : ApolloQuery.Data {
  interface Data : ApolloQuery.Data
}

interface MutationCompat<D> : ApolloMutation<D>, OperationCompat<D> where  D : ApolloMutation.Data {
  interface Data : ApolloMutation.Data
}

interface SubscriptionCompat<D> : ApolloSubscription<D>, OperationCompat<D> where D : ApolloSubscription.Data {
  interface Data : ApolloSubscription.Data
}

interface FragmentCompat<D> : ApolloFragment<D>, ExecutableCompat<D> where  D : ApolloFragment.Data {
  interface Data : ApolloFragment.Data
}