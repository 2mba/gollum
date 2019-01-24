package domain


data class FieldCondition(
    val fieldName: String,
    val predicate: String,
    val value: String
)

fun FieldCondition.validate(): Boolean {
    return when (this.fieldName) {
        "sex" -> this.predicate == "eq"
                && (this.value == "m" || this.value == "f")
        "email" -> {
            return when (this.predicate) {
                "domain" -> true
                "lt", "gt" -> true
                else -> false
            }
        }
        "status" -> {
            return when (this.predicate) {
                "eq", "neq" ->
                    this.value == "свободны"
                            || this.value == "заняты"
                            || this.value == "всё сложно"
                else -> false
            }
        }
        "fname" -> {
            return when (this.predicate) {
                "eq", "any" -> true
                "null" -> this.value == "0" || this.value == "1"
                else -> false
            }
        }
        "sname" -> {
            return when (this.predicate) {
                "eq", "starts" -> true
                "null" -> this.value == "0" || this.value == "1"
                else -> false
            }
        }
        "phone" -> {
            return when (this.predicate) {
                "code" -> this.value.length == 3
                "null" -> this.value == "0" || this.value == "1"
                else -> false
            }
        }
        "country" -> {
            return when (this.predicate) {
                "eq" -> true
                "null" -> this.value == "0" || this.value == "1"
                else -> false
            }
        }
        "city" -> {
            return when (this.predicate) {
                "eq", "any" -> true
                "null" -> this.value == "0" || this.value == "1"
                else -> false
            }
        }
        "birth" -> {
            return when (this.predicate) {
                "lt", "gt", "year" -> {
                    val number = this.value.toLongOrNull() ?: return false
                    return number > 0
                }
                else -> false
            }
        }
//        "interests" -> {
//            return when (this.predicate) {
//                "contains", "any" -> true
//                else -> false
//            }
//        }
//        "likes" -> {
//            return when (this.predicate) {
//                "contains" -> true
//                else -> false
//            }
//        }
        "premium" -> {
            return when (this.predicate) {
                "now" -> {
                    val number = this.value.toLongOrNull() ?: return false
                    return number > 0
                }
                "null" -> this.value == "0" || this.value == "1"
                else -> false
            }
        }
        else -> false
    }
}