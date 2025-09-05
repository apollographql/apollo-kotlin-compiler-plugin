# Apollo Kotlin Compiler Plugin

The Apollo Kotlin Compiler Plugin is a Kotlin compiler plugin to work with GraphQL operations.

The Apollo Kotlin Compiler Plugin makes it possible to colocate your GraphQL queries in your `.kt` Kotlin files.

> [!WARNING]
> This project is under construction. 

## Get Started

Add the `com.apollographql.kotlin.compiler.plugin` Gradle plugin to your `build.gradle.kts` files:

```kotlin
plugins {
  id("org.jetbrains.kotlin.jvm").version("2.2.20-RC")
  id("com.apollographql.kotlin.compiler.plugin").version("2.2.20-RC-0.0.0-SNAPSHOT")
}

apolloKotlinCompilerPlugin {
  // Configure your schema
  schemaFile.set(file("schema.graphqls"))
  // Enable Apollo Kotlin compatibility mode to use the generated models with
  // your existing `ApolloClient`
  compat.set(true)
}

dependencies {
  // The client
  implementation("com.apollographql.apollo:apollo-runtime:5.0.0-alpha.2")
  // The Kotlin compiler plugin runtime
  implementation("com.apollographql.kotlin:runtime-compat:2.2.20-RC-0.0.0-SNAPSHOT")
} 
```

To execute an operation, write a `@Query` class:

```kotlin
@Query("""
query GetSessions {
  sessions {
    start
    end
    title
    speakers {
      name
    }
  }
}
  
""")
class GetSessions
```

The compiler plugins automatically makes your class an instance of `Query` and generates nested models:

* `Data`
* `Session`
* `Speaker`

Use them with `ApolloClient` ([documentation](https://www.apollographql.com/docs/kotlin/essentials/queries)):

```kotlin
apolloClient.query(GetScheduleItemsQuery()).toFlow().collect {
  it.data?.sessions?.forEach {
    println(it.title)
  }
}
```

Output:
```
Community Update 2025: Growing in the Open
GraphQL at Meta
How To Use Fragments (They're Not for Re-use!)
Fixing GraphQL's Biggest Mistake in 512 Bytes
...
```

