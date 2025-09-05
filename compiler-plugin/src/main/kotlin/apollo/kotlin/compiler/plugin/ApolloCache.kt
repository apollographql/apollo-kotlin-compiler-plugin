package apollo.kotlin.compiler.plugin

import com.apollographql.apollo.ast.GQLFragmentDefinition
import org.jetbrains.kotlin.name.ClassId
import java.util.concurrent.ConcurrentHashMap

class ScalarInfo(val target: String, val adapter: String)
class ApolloCache(userScalars: Map<String, ScalarInfo>) {
  private class ScalarInfoInternal(val target: ClassId, val adapter: ClassId)
  private val scalars: Map<String, ScalarInfoInternal>
  // TODO does that need to be thread safe?
  private val fragments = ConcurrentHashMap<String, ClassId>()
  private val fragmentDefinitions = ConcurrentHashMap<String, GQLFragmentDefinition>()

  init {
    val builtIns = mapOf(
      "Int" to ScalarInfoInternal(ClassIds.Int, ClassIds.IntAdapter),
      "String" to ScalarInfoInternal(ClassIds.String, ClassIds.StringAdapter),
      "Boolean" to ScalarInfoInternal(ClassIds.Boolean, ClassIds.BooleanAdapter),
      "Float" to ScalarInfoInternal(ClassIds.Double, ClassIds.FloatAdapter),
    )
    scalars = builtIns + userScalars.mapValues {
      ScalarInfoInternal(it.value.target.classId(), it.value.adapter.classId())
    }
  }

  fun rememberFragment(name: String, classId: ClassId, definition: GQLFragmentDefinition) {
    fragments.put(name, classId)
    fragmentDefinitions.put(name, definition)
  }

  fun getFragment(name: String) = fragments.get(name)
  fun getFragmentDefinition(name: String) = fragmentDefinitions.get(name)
  fun getScalar(name: String) = scalars.get(name)?.target ?: error("Apollo: unresolved scalar '$name'")
  fun getScalarAdapter(name: String) = scalars.get(name)?.adapter ?: error("Apollo: unresolved adapter '$name'")
}