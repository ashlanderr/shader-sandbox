package app

sealed class Result<out X, out R> {
  companion object {
    inline fun <X, R1, R2, R> combine(r1: Result<X, R1>, r2: Result<X, R2>, mapper: (R1, R2) -> R): Result<X, R> {
      return when (r1) {
        is Ok -> when (r2) {
          is Ok -> Ok(mapper(r1.value, r2.value))
          is Err -> r2
        }
        is Err -> r1
      }
    }

    inline fun <X, R1, R2, R3, R> combine(r1: Result<X, R1>, r2: Result<X, R2>, r3: Result<X, R3>, mapper: (R1, R2, R3) -> R): Result<X, R> {
      return when (r1) {
        is Ok -> when (r2) {
          is Ok -> when (r3) {
            is Ok -> Ok(mapper(r1.value, r2.value, r3.value))
            is Err -> r3
          }
          is Err -> r2
        }
        is Err -> r1
      }
    }
  }
}

data class Ok<R>(val value: R) : Result<Nothing, R>()
data class Err<X>(val error: X) : Result<X, Nothing>()

inline fun <X, R1, R2> Result<X, R1>.map(mapper: (R1) -> R2): Result<X, R2> {
  return when (this) {
    is Ok -> Ok(mapper(this.value))
    is Err -> this
  }
}

inline fun <X, R> Result<X, R>.orElse(mapper: (X) -> R): R {
  return when (this) {
    is Ok -> this.value
    is Err -> mapper(this.error)
  }
}

inline fun <X1, X2, R> Result<X1, R>.mapError(mapper: (X1) -> X2): Result<X2, R> {
  return when (this) {
    is Ok -> this
    is Err -> Err(mapper(this.error))
  }
}

inline fun <X, T1, T2, U> Result<X, T1>.combine(another: Result<X, T2>, mapper: (T1, T2) -> U) =
  Result.combine(this, another, mapper)

inline fun <X, R1, R2> Result<X, R1>.flatMap(mapper: (R1) -> Result<X, R2>): Result<X, R2> {
  return when (this) {
    is Ok -> mapper(this.value)
    is Err -> this
  }
}

fun <X, R> List<Result<X, R>>.flatten(): Result<List<X>, List<R>> {
  return this.fold(Ok(emptyList())) { acc, item ->
    when (acc) {
      is Ok -> when (item) {
        is Ok -> Ok(acc.value + item.value)
        is Err -> Err(listOf(item.error))
      }
      is Err -> when (item) {
        is Ok -> acc
        is Err -> Err(acc.error + item.error)
      }
    }
  }
}

fun <K, V, X> Map<K, V>.getOrErr(key: K, err: () -> X): Result<X, V> {
  return when (val value = this[key]) {
    null -> Err(err())
    else -> Ok(value)
  }
}
