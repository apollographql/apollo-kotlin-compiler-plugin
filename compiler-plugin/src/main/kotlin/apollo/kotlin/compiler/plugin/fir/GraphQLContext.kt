//package apollo.kotlin.compiler.plugin.fir
//
//import apollo.kotlin.compiler.plugin.*
//import apollo.kotlin.compiler.plugin.ap.ApModel
//import apollo.kotlin.compiler.plugin.ap.capitalizeFirstLetter
//import apollo.kotlin.compiler.plugin.fir.service.graphQLService
//import com.apollographql.apollo.ast.GQLExecutableDefinition
//import com.apollographql.apollo.ast.GQLOperationDefinition
//import com.apollographql.apollo.ast.Schema
//import org.jetbrains.kotlin.descriptors.ClassKind
//import org.jetbrains.kotlin.fir.declarations.utils.classId
//import org.jetbrains.kotlin.fir.plugin.createNestedClass
//import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
//import org.jetbrains.kotlin.name.ClassId
//import org.jetbrains.kotlin.name.Name
//
//class GraphQLContext(
//  val models: List<ApModel>,
//  val definition: GQLExecutableDefinition
//)
//
//fun createGraphQLContext(
//  firExtension: ExecutableDeclarationGenerationExtension,
//  schema: Schema,
//  symbol: FirClassSymbol<*>
//): GraphQLContext? {
//  val gqlDefinition = firExtension.session.graphQLService.getExecutableFor(symbol)
//  if (gqlDefinition == null) {
//    return null
//  }
//
//  .associate {
//    val modelKey = Keys.Model(symbol, it, null)
//    val model = firExtension.
//
//    val adapter = firExtension.
//
//    modelKey.adapter = adapter
//
//    Name.identifier(it.name) to model
//  }
//
//  return GraphQLContext(
//    models = models,
//    definition = gqlDefinition
//  )
//}
