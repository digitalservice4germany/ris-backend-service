package de.bund.digitalservice.ris.norms.conventions.predicate

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass

class HaveAMethodWithNameLikeClassPrefix(private val classPostfix: String) :
    DescribedPredicate<JavaClass>(
        "have a method named like class prefix where prefix is '$classPostfix'",
    ) {
    override fun test(item: JavaClass): Boolean {
        val expectedMethodName = this.getExpectedMethodName(item)
        val classHasMethod = HaveAMethodWithName(expectedMethodName).test(item)
        val classHasPostfix = item.simpleName.endsWith(this.classPostfix)

        return classHasPostfix && classHasMethod
    }

    fun getExpectedMethodName(item: JavaClass): String {
        val classPrefix = item.simpleName.replace(this.classPostfix, "")
        return classPrefix.replaceFirstChar { it.lowercase() }
    }
}
